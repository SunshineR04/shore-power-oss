package com.shorepower.websocket;

import com.shorepower.entity.Alarm;
import com.shorepower.entity.Device;
import com.shorepower.entity.DeviceData;
import com.shorepower.entity.EnergyConsumption;
import com.shorepower.mapper.DeviceDataMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.EnergyConsumptionMapper;
import com.shorepower.mapper.ReservationMapper;
import com.shorepower.service.AlarmService;
import com.shorepower.service.SystemConfigService;
import com.shorepower.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.mapper.AlarmMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
/**
 * 设备运行数据模拟器
 *
 * 核心职责：通过定时任务模拟7台岸电设备的实时运行数据，替代真实硬件传感器。
 * 每1秒触发一次心跳，但实际数据生成+推送由配置项 device.polling.interval 控制（默认10秒）。
 *
 * 物理模拟模型：
 *   1. 软启动：设备从空闲→使用中时，负载从0渐变到目标值
 *   2. 负载惯性：目标负载随机游走 + 实际负载一阶滞后追踪
 *   3. 热惯性：I²R铜损 + IGBT开关损耗 + 环境温度 → 指数平滑
 *   4. PFC功率因数曲线：轻载0.85→满载0.99，三段分段线性
 *   5. 三相功率公式：P = √3 × U × I × PF
 *
 * WebSocket推送Topic：
 *   - /topic/device-data：设备运行数据（JSON数组，每台设备一个Map）
 *   - /topic/alarm：告警事件（触发即推）
 *   - /topic/device-status：设备状态变更（ONLINE/FAULT/IN_USE）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSimulator {

    private final DeviceMapper deviceMapper;
    private final DeviceDataMapper dataMapper;
    private final EnergyConsumptionMapper energyMapper;
    private final ReservationMapper reservationMapper;
    private final AlarmService alarmService;
    private final AlarmMapper alarmMapper;
    private final SimpMessagingTemplate ws;
    private final SystemConfigService configService;
    private final WeatherService weatherService;
    private final Random rand = new Random();

    /** 上次推送时间戳，用于控制推送间隔。初始化为当前时间，避免首帧 elapsedSeconds 爆炸 */
    private long lastPushTime = System.currentTimeMillis();
    /** 模拟计数器，用于频率正弦波模拟 */
    private long simulationTick = 0;
    /** 记录每台设备上一轮的目标负载因子（0~1） */
    private final Map<Long, Double> lastTargetLoad = new HashMap<>();
    /** 记录每台设备上一轮的实际负载因子（带惯性） */
    private final Map<Long, Double> lastActualLoad = new HashMap<>();
    /** 记录每台设备上一轮是否处于使用中状态（检测状态变化） */
    private final Map<Long, Boolean> lastInUseState = new HashMap<>();
    /** 记录每台设备上一轮的温度值（热惯量计算） */
    private final Map<Long, Double> lastTemperature = new HashMap<>();

    /** √3 ≈ 1.732，三相功率计算用 */
    private static final BigDecimal SQRT3 = BigDecimal.valueOf(1.732);

    /**
     * 定时模拟数据入口
     *
     * 调度策略：@Scheduled(fixedRate=1000) 每秒触发一次，
     * 但通过 configService 读取 device.polling.interval（默认3000ms）做门控。
     * 只有实际经过的 wall-clock 时间达到 interval 时才执行数据生成+推送。
     *
     * 这种"双频"设计的意图：
     *   - 1秒心跳确保时间间隔测量精度
     *   - 3秒推送降低前端渲染负载和WebSocket通信压力
     *   - 推送间隔可在运维中动态调整而无需重启
     *
     * 执行步骤：
     *   1. 遍历所有设备
     *   2. 查询当前正在使用中的设备ID集合
     *   3. 为每台设备生成模拟运行数据（写入device_data表）
     *   4. 构建推送消息Map
     *   5. 对非故障设备执行阈值检查 -> 可能触发告警
     *   6. 通过WebSocket推送给前端
     */
    @Scheduled(fixedRate = 1000)
    public void simulateData() {
        // 从 sys_config 读取推送间隔（默认3000ms），可在运行中修改
        int interval = configService.getIntConfig("device.polling.interval", 3000);
        long now = System.currentTimeMillis();
        // 双频门控：虽然方法每秒触发，但只有距上次推送超过 interval 才真正执行
        if (now - lastPushTime < interval) return;
        // 计算自上次推送以来经过的秒数，用于能耗积分
        double elapsedSeconds = (now - lastPushTime) / 1000.0;
        // 兜底：首帧或异常间隔（重启/时钟回拨）时钳制为一个采集周期，避免能耗积分爆炸
        if (elapsedSeconds > 3600) elapsedSeconds = interval / 1000.0;
        lastPushTime = now;

        // 查全量设备（selectList(null) = 无条件查询全部记录）
        List<Device> allDevices = deviceMapper.selectList(null);
        if (allDevices.isEmpty()) return;

        // 查当前所有正在使用的设备ID（有预约状态为 IN_USE 的记录）
        // 用于在 generatePileData 中区分设备是带载还是空闲
        Set<Long> inUseDeviceIds = new HashSet<>(reservationMapper.findInUseDeviceIds());

        // 构建待推送的数据列表（每台设备一个 Map）
        List<Map<String, Object>> pushList = new ArrayList<>();

        for (Device dev : allDevices) {
            // 判断该设备当前是否处于使用状态（有正在进行的预约）
            boolean inUse = inUseDeviceIds.contains(dev.getId());
            // 生成模拟运行数据（含物理模型计算）
            DeviceData data = generatePileData(dev, inUse, elapsedSeconds);

            // 写入 device_data 表（用于能耗结算和历史趋势查询）
            dataMapper.insert(data);

            // 构建前端需要的字段映射（扁平化 JSON，前端直接使用）
            Map<String, Object> item = new HashMap<>();
            item.put("deviceId", dev.getId());
            item.put("deviceCode", dev.getDeviceCode());
            item.put("deviceName", dev.getDeviceName());
            item.put("deviceType", dev.getDeviceType());
            item.put("voltage", data.getVoltage());
            item.put("currentVal", data.getCurrentVal());
            item.put("power", data.getPower());
            item.put("temperature", data.getTemperature());
            item.put("powerFactor", data.getPowerFactor());
            item.put("frequency", data.getFrequency());
            item.put("energyConsumption", data.getEnergyConsumption());
            item.put("energyCost", data.getEnergyCost());
            item.put("collectTime", data.getCollectTime().toString());
            pushList.add(item);

            // 已故障的设备不再重复检查阈值（等管理员处理完告警恢复 ONLINE 后再检查）
            if (!"FAULT".equals(dev.getStatus())) {
                checkThreshold(dev, data);
            }
        }

        // 一次性推送所有设备的实时数据（JSON 数组），前端拆包后逐个渲染仪表盘
        ws.convertAndSend("/topic/device-data", pushList);
    }

    /**
     * 为单台设备生成模拟运行数据
     *
     * 物理模型详解：
     *
     * 1. 软启动（行117-125）：设备从空闲→使用中时，实际负载从0起步，目标随机为30%-70%额定容量。
     *    IGBT整流器启动时有限流功能，不会瞬时带满负载，模拟了这个特性。
     *
     * 2. 负载惯性模型（行127-137）：
     *    - 目标值每步随机游走 ±1%，限幅20%-100%
     *    - 实际值每步朝目标靠近 8%（一阶滞后，惯性系数0.08）
     *    - 模拟船舶负载不会瞬间跳变，有渐进过程
     *
     * 3. 电压模型（行143-146）：
     *    - 高压设备（≥1000V）：满负荷压降约200V（~2%）
     *    - 低压设备（<1000V）：满负荷压降约15V（~4%）
     *    - 叠加 ±0.5V 随机噪声
     *
     * 4. 电流模型（行149-151）：与负载因子成正比，加 ±2% 噪声
     *
     * 5. PFC功率因数曲线（行154-158）：
     *    - 轻载 <20%：0.85~0.92（IGBT整流器轻载时PF较低）
     *    - 中载 20%~50%：0.92~0.95
     *    - 重载 >50%：0.95~0.99，上限0.99
     *    - 模拟了有源前端整流器的功率因数校正特性
     *
     * 6. 热惯性模型（行165-173）：
     *    - 柜内温度 = 室外温度 + 5°C（机柜温升）
     *    - 铜损温升 = lf² × 35（I²R损耗与负载平方成正比）
     *    - 开关损耗温升 = lf × 8（IGBT开关损耗与负载成正比）
     *    - 热惯量：指数平滑 α=0.08（~12步达到63%变化量）
     *
     * 7. 三相功率公式（行162-163）：P(kW) = √3 × U(V) × I(A) × PF / 1000
     *
     * 8. 空闲状态（行189-206）：有额定电压、无电流、温度缓慢衰减至室温
     *
     * 9. 电网频率（行209）：50Hz基频 + 慢速正弦波动(周期约10分钟) + 白噪声
     */
    private DeviceData generatePileData(Device dev, boolean inUse, double elapsedSeconds) {
        // 兜底：如果 elapsedSeconds <= 0（首次推送时），按10秒算能耗
        if (elapsedSeconds <= 0) elapsedSeconds = 10;
        DeviceData d = new DeviceData();
        d.setDeviceId(dev.getId());
        simulationTick++;  // 全局计数器，用于频率正弦波

        // 设备额定参数，数据库无值则用默认值兜底（380V/200A/130kW）
        BigDecimal ratedV = dev.getRatedVoltage() != null ? dev.getRatedVoltage() : BigDecimal.valueOf(380);
        BigDecimal ratedA = dev.getRatedCurrent() != null ? dev.getRatedCurrent() : BigDecimal.valueOf(200);
        BigDecimal ratedP = dev.getRatedPower() != null ? dev.getRatedPower() : BigDecimal.valueOf(130);

        // 高压设备（≥1000V，如6600V集装箱船）有更大的压降绝对值但比例更小
        boolean isHighVoltage = ratedV.compareTo(BigDecimal.valueOf(1000)) >= 0;

        // 检测状态变化：之前是否在使用
        boolean wasInUse = lastInUseState.getOrDefault(dev.getId(), false);
        lastInUseState.put(dev.getId(), inUse);  // 更新当前状态供下一轮检测

        // 状态从 空闲→使用中 的瞬间，触发软启动：负载从0开始，目标随机为30%~70%
        if (inUse && !wasInUse) {
            lastActualLoad.put(dev.getId(), 0.0);
            lastTargetLoad.put(dev.getId(), 0.3 + rand.nextDouble() * 0.4);
        }

        if (inUse) {
            // ==== 使用中状态：带载模拟 ====
            double target = lastTargetLoad.getOrDefault(dev.getId(), 0.5);
            double actual = lastActualLoad.getOrDefault(dev.getId(), 0.0);

            // 目标负载随机游走：每步 ±1%（模拟船舶用电设备的随机波动）
            target += (rand.nextDouble() - 0.5) * 0.02;
            target = clamp(target, 0.2, 1.0);  // 不超过20%~100%
            // 实际负载朝目标靠近 8%（一阶滞后系统，惯性系数0.08）
            // 模拟负载不能突变（比如启动电机、泵等设备是缓慢加载的）
            actual += (target - actual) * 0.08;

            lastTargetLoad.put(dev.getId(), target);
            lastActualLoad.put(dev.getId(), actual);

            double lf = actual;
            BigDecimal loadFactor = BigDecimal.valueOf(lf);

            // ==== 电压模型 ====
            // 高压设备（10kV级）：满负荷压降约200V（~2%）
            // 低压设备（380V级）：满负荷压降约15V（~4%）
            // + ±0.5V随机噪声模拟电网小波动
            BigDecimal voltageDrop = loadFactor.multiply(isHighVoltage ? BigDecimal.valueOf(200) : BigDecimal.valueOf(15));
            BigDecimal voltage = ratedV.subtract(voltageDrop)
                .add(BigDecimal.valueOf(rand.nextDouble() * 2 - 1))
                .setScale(2, RoundingMode.HALF_UP);

            // ==== 电流模型 ====
            // 电流正比于负载因子（I = I_rated × loadFactor），加 ±2%噪声
            BigDecimal current = ratedA.multiply(loadFactor)
                .add(BigDecimal.valueOf(rand.nextDouble() * ratedA.doubleValue() * 0.04 - ratedA.doubleValue() * 0.02))
                .setScale(2, RoundingMode.HALF_UP);

            // ==== 功率因数(PFC)曲线 ====
            // 模拟IGBT有源前端整流器的功率因数校正特性
            // 轻载(<20%)时PF较低(0.85~0.92)，重载(>50%)时接近1(0.95~0.99)
            double pfDouble;
            if (lf < 0.2) pfDouble = 0.85 + lf * 0.35 + rand.nextDouble() * 0.02;
            else if (lf < 0.5) pfDouble = 0.92 + (lf - 0.2) * 0.1 + rand.nextDouble() * 0.02;
            else pfDouble = 0.95 + (lf - 0.5) * 0.08 + rand.nextDouble() * 0.02;
            pfDouble = Math.min(pfDouble, 0.99);  // PF最高0.99，达不到理想1.0
            BigDecimal pf = BigDecimal.valueOf(pfDouble).setScale(2, RoundingMode.HALF_UP);

            // ==== 三相功率计算 ====
            // 三相交流系统：P(kW) = √3 × U(V) × I(A) × PF / 1000
            BigDecimal power = voltage.multiply(current).multiply(SQRT3).multiply(pf)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            // ==== 热模型（核心：集总参数热模型）====
            // 从OpenWeatherMap获取室外温度（实际部署需配置API Key）
            double outdoor = weatherService.getCurrentAmbient();
            double cabinetBase = outdoor + 5.0;  // 机柜内外温差约5℃
            // 铜损(I²R)与负载的平方成正比（lf² × 35）
            double copperLoss = lf * lf * 35.0;
            // IGBT开关损耗与负载成正比（lf × 8）
            double switchingLoss = lf * 8.0;
            double targetTemp = cabinetBase + copperLoss + switchingLoss + rand.nextDouble() * 2;
            // 热惯量：一阶指数平滑 α=0.08
            // 温度不会瞬间跳变，约12步（36秒）达到63%的变化量
            double prevTemp = lastTemperature.getOrDefault(dev.getId(), cabinetBase);
            double temp = prevTemp * 0.92 + targetTemp * 0.08;
            lastTemperature.put(dev.getId(), temp);
            BigDecimal temperature = BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP);

            d.setVoltage(voltage);
            d.setCurrentVal(current);
            d.setPower(power);
            d.setTemperature(temperature);
            d.setPowerFactor(pf);

            // ==== 能耗计算 ====
            // 每个推送间隔的能耗 = 功率(kW) × 经过时间(h)
            // 常用于电费结算和能耗聚合
            BigDecimal energy = power.multiply(BigDecimal.valueOf(elapsedSeconds / 3600.0))
                .setScale(4, RoundingMode.HALF_UP);
            d.setEnergyConsumption(energy);

            // 电费 = 能耗 × 当前电价（默认0.65元/度，从 sys_config 读取）
            BigDecimal price = configService.getDecimalConfig("electricity.price", BigDecimal.valueOf(0.65));
            d.setEnergyCost(energy.multiply(price).setScale(4, RoundingMode.HALF_UP));
        } else {
            // ==== 空闲状态：有电压、无电流 ====
            // 电压在额定值附近有小波动（±0.5V），电流为0，功率为0
            BigDecimal voltage = ratedV.add(BigDecimal.valueOf(rand.nextDouble() * 2 - 1))
                .setScale(2, RoundingMode.HALF_UP);
            d.setVoltage(voltage);
            d.setCurrentVal(BigDecimal.ZERO.setScale(2));
            d.setPower(BigDecimal.ZERO.setScale(2));

            // 空闲时温度缓慢降至环境温度（衰减系数0.95，更快趋向环境）
            double outdoor = weatherService.getCurrentAmbient();
            double ambient = outdoor + 5.0 + (rand.nextDouble() - 0.5) * 4;
            double prevTemp = lastTemperature.getOrDefault(dev.getId(), ambient);
            double temp = prevTemp * 0.95 + ambient * 0.05;
            lastTemperature.put(dev.getId(), temp);
            d.setTemperature(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP));

            d.setPowerFactor(BigDecimal.valueOf(0.97 + rand.nextDouble() * 0.03).setScale(2, RoundingMode.HALF_UP));
            d.setEnergyConsumption(BigDecimal.ZERO.setScale(4));
            d.setEnergyCost(BigDecimal.ZERO.setScale(4));
        }

        // ==== 电网频率模拟 ====
        // 50Hz基频 + 正弦波慢波动(周期=600步≈10分钟，振幅±0.05Hz) + 白噪声(±0.01Hz)
        double freq = 50.0 + Math.sin(simulationTick * Math.PI * 2.0 / 600.0) * 0.05 + rand.nextDouble() * 0.02;
        d.setFrequency(BigDecimal.valueOf(freq).setScale(2, RoundingMode.HALF_UP));
        // 湿度：均匀分布 40%~70%RH
        d.setHumidity(BigDecimal.valueOf(40 + rand.nextDouble() * 30).setScale(2, RoundingMode.HALF_UP));
        d.setCollectTime(LocalDateTime.now());
        return d;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * 每日凌晨日终结算
     *
     * @Scheduled(cron = "0 0 0 * * *") 每天零点执行
     * 汇总前一天的 device_data 记录，计算每台设备的总能耗、峰值功率、平均功率、运行时长和电费，
     * 写入 energy_consumption 表供历史查询和趋势分析。
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void calculateDailyEnergy() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

        List<Device> devices = deviceMapper.selectList(null);
        for (Device dev : devices) {
            try {
                // 查前一天该设备的所有运行数据
                List<DeviceData> deviceDataList = dataMapper.selectList(
                    new LambdaQueryWrapper<DeviceData>()
                        .eq(DeviceData::getDeviceId, dev.getId())
                        .between(DeviceData::getCollectTime, startOfDay, endOfDay)
                );

                if (!deviceDataList.isEmpty()) {
                    BigDecimal totalEnergy = BigDecimal.ZERO;
                    BigDecimal totalCost = BigDecimal.ZERO;
                    BigDecimal maxPower = BigDecimal.ZERO;
                    BigDecimal totalPower = BigDecimal.ZERO;

                    // 遍历每日所有数据点，累加能耗和电费，找峰值功率
                    for (DeviceData data : deviceDataList) {
                        if (data.getEnergyConsumption() != null) {
                            totalEnergy = totalEnergy.add(data.getEnergyConsumption());
                        }
                        if (data.getEnergyCost() != null) {
                            totalCost = totalCost.add(data.getEnergyCost());
                        }
                        if (data.getPower() != null && data.getPower().compareTo(maxPower) > 0) {
                            maxPower = data.getPower();
                        }
                        if (data.getPower() != null) {
                            totalPower = totalPower.add(data.getPower());
                        }
                    }

                    // 平均功率 = 总功率 / 样本数
                    BigDecimal avgPower = totalPower.divide(BigDecimal.valueOf(deviceDataList.size()), 2, RoundingMode.HALF_UP);
                    // 运行时长 = 样本数 × 每点间隔秒数 / 3600（间隔从 sys_config 读取，与推送间隔一致）
                    double secondsPerPoint = configService.getIntConfig("device.polling.interval", 3000) / 1000.0;
                    BigDecimal runningHours = BigDecimal.valueOf(deviceDataList.size() * secondsPerPoint / 3600.0).setScale(2, RoundingMode.HALF_UP);

                    // 写入 energy_consumption 历史表
                    EnergyConsumption energyConsumption = new EnergyConsumption();
                    energyConsumption.setDeviceId(dev.getId());
                    energyConsumption.setStatDate(yesterday);
                    energyConsumption.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
                    energyConsumption.setPeakPower(maxPower);
                    energyConsumption.setAvgPower(avgPower);
                    energyConsumption.setRunningHours(runningHours);
                    energyConsumption.setEnergyCost(totalCost.setScale(2, RoundingMode.HALF_UP));

                    energyMapper.insert(energyConsumption);
                }
            } catch (Exception e) {
                log.error("汇总设备 {} 日能耗失败", dev.getId(), e);
            }
        }
    }

    /**
     * 重复告警抑制检查
     *
     * 核心逻辑：查询 alarm 表中是否存在 同一设备 + 同一告警类型 + 状态为PENDING 的记录。
     * 如果存在，说明该告警还未被处理，不再重复创建。
     *
     * 为什么需要这个机制：如果一台设备温度持续超标（例如60°C），
     * 每3秒就会触发一次温度报警。不做抑制的话，一天累计约28800条告警记录，
     * 管理员根本看不过来，数据库也会被撑爆。
     *
     * 所以统一类型 + 同一设备的未处理告警只保留一条，处理完（状态改为RESOLVED/IGNORED）后
     * 如果再次超标才允许生成新告警。
     */
    private boolean hasPendingAlarm(Long deviceId, String alarmType) {
        // selectCount 返回符合条件的记录数 >= 0
        // 查询条件：deviceId + alarmType + status=PENDING
        return alarmMapper.selectCount(
            new LambdaQueryWrapper<Alarm>()
                .eq(Alarm::getDeviceId, deviceId)
                .eq(Alarm::getAlarmType, alarmType)
                .eq(Alarm::getStatus, "PENDING")
        ) > 0;
    }

    /**
     * 阈值检测 — 两种告警类型
     *
     * 1. 温度告警：
     *    - 从 sys_config 读取告警阈值，运行时动态可配
     *    - 默认：WARNING=55°C, CRITICAL=65°C
     *    - 超过WARNING但未超过CRITICAL → WARNING级别
     *    - 超过CRITICAL → CRITICAL级别
     *
     * 2. 电压偏差告警：
     *    - 从 sys_config 读取允许偏差比例（默认10%）
     *    - 计算 |实际电压 - 额定电压| / 额定电压
     *    - 超过比例 → WARNING级别（电压只有WARNING，不设CRITICAL）
     *
     * 共同特点：
     *   - 阈值可从数据库动态读取，管理员在系统配置页面修改后即时生效
     *   - 检测前先调用 hasPendingAlarm 做重复告警抑制
     */
    private void checkThreshold(Device dev, DeviceData data) {
        // 从 sys_config 读取温度告警阈值（运行时动态可配，无需重启）
        BigDecimal tempWarning = configService.getDecimalConfig("alarm.temperature.warning", BigDecimal.valueOf(55));
        BigDecimal tempCritical = configService.getDecimalConfig("alarm.temperature.critical", BigDecimal.valueOf(65));

        // === 温度告警 ===
        // 先检查温度是否超过WARNING阈值，再检查是否有未处理的同类型告警
        if (data.getTemperature() != null && data.getTemperature().compareTo(tempWarning) > 0
                && !hasPendingAlarm(dev.getId(), "TEMPERATURE")) {
            // 超过 WARNING 后，再判断是否也超过 CRITICAL，决定告警等级
            createAlarm(dev, "TEMPERATURE",
                    data.getTemperature().compareTo(tempCritical) > 0 ? "CRITICAL" : "WARNING",
                    dev.getDeviceName() + " 温度过高: " + data.getTemperature() + "℃",
                    data.getTemperature().toString(), tempWarning.toString());
        }

        // === 电压偏差告警 ===
        // 需要设备有额定电压字段（ratedVoltage 不为空且>0）
        if (data.getVoltage() != null && dev.getRatedVoltage() != null
                && dev.getRatedVoltage().compareTo(BigDecimal.ZERO) > 0) {
            // 读取允许的电压偏差比例（默认10%）
            BigDecimal voltageRatio = configService.getDecimalConfig("alarm.voltage.ratio", BigDecimal.valueOf(0.1));
            // 计算偏差比例 = |实际电压 - 额定电压| / 额定电压
            BigDecimal deviation = data.getVoltage().subtract(dev.getRatedVoltage()).abs()
                    .divide(dev.getRatedVoltage(), 4, RoundingMode.HALF_UP);
            // 超过阈值且没有未处理的同类告警，才创建新告警
            if (deviation.compareTo(voltageRatio) > 0 && !hasPendingAlarm(dev.getId(), "VOLTAGE")) {
                createAlarm(dev, "VOLTAGE", "WARNING",
                        dev.getDeviceName() + " 电压偏差: " + data.getVoltage() + "V (额定" + dev.getRatedVoltage() + "V)",
                        data.getVoltage().toString(), dev.getRatedVoltage().toString());
            }
        }
    }

    /**
     * 创建告警并推送WebSocket通知
     *
     * 执行步骤：
     *   1. 构建 Alarm 实体并写入数据库
     *   2. 通过 WebSocket 推送到 /topic/alarm（前端弹窗通知）
     *   3. 将对应设备的状态标记为 FAULT（故障）
     *   4. 通过 WebSocket 推送设备状态变更到 /topic/device-status
     */
    private void createAlarm(Device dev, String type, String level, String content, String value, String threshold) {
        // 步骤1：构建告警实体并调用 AlarmService 写入数据库
        Alarm alarm = new Alarm();
        alarm.setDeviceId(dev.getId());
        alarm.setAlarmType(type);
        alarm.setAlarmLevel(level);
        alarm.setAlarmContent(content);
        alarm.setAlarmValue(value);
        alarm.setThresholdValue(threshold);
        alarmService.createAlarm(alarm);

        // 步骤2：WebSocket 推送到 /topic/alarm，前端告警列表和弹窗立即响应
        ws.convertAndSend("/topic/alarm", alarm);

        // 步骤3：设备状态置为 FAULT（故障），阻止重复检测
        dev.setStatus("FAULT");
        deviceMapper.updateById(dev);

        // 步骤4：WebSocket 推送设备状态变更，前端仪表盘立即可见设备变红
        Map<String, Object> deviceStatusUpdate = new HashMap<>();
        deviceStatusUpdate.put("deviceId", dev.getId());
        deviceStatusUpdate.put("status", dev.getStatus());
        ws.convertAndSend("/topic/device-status", deviceStatusUpdate);
    }
}
