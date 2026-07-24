package com.shorepower.service;

import com.shorepower.entity.Device;
import com.shorepower.entity.DeviceData;
import com.shorepower.entity.EnergyConsumption;
import com.shorepower.entity.UsageRecord;
import com.shorepower.mapper.DeviceDataMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.EnergyConsumptionMapper;
import com.shorepower.mapper.UsageRecordMapper;
import com.shorepower.utils.TimeSeriesForecast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 能耗分析与优化服务
 *
 * 核心功能：
 *   1. 能耗模式分析：统计指定周期内的能耗总和、峰值、日均值
 *   2. 负载均衡建议：遍历所有设备，计算利用率，识别过载/低负载/空闲设备
 *   3. 能耗预测：基于 Holt-Winters 算法预测未来能耗趋势
 *   4. 环保指标计算：CO₂减排量、标准煤节省量、等效植树量
 *
 * 数据源优先级（按准确度由高到低）：
 *   1. usage_record（用户实际使用记录，单数据源）
 *   2. energy_consumption（日终聚合表）
 *   3. device_data（实时模拟数据，兜底）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyOptimizationService {

    private final DeviceMapper deviceMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final EnergyConsumptionMapper energyConsumptionMapper;
    private final UsageRecordMapper usageRecordMapper;
    private final ElectricityPriceService electricityPriceService;
    private final SystemConfigService configService;

    /**
     * 分析能耗模式
     *
     * 统计指定设备在指定天数内的：
     *   - 总能耗/总费用
     *   - 峰值能耗
     *   - 日均能耗/日均费用
     *   - 能耗模式分类（趋势分析 + 工作日/周末对比）
     */
    public Map<String, Object> analyzeEnergyPatterns(Long deviceId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return Map.of("error", "设备不存在");
        }

        List<Map<String, Object>> dailyData = loadDailyData(deviceId, startDate, endDate);

        BigDecimal totalEnergy = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal maxEnergy = BigDecimal.ZERO;

        for (Map<String, Object> data : dailyData) {
            BigDecimal energy = (BigDecimal) data.get("totalEnergy");
            BigDecimal cost = (BigDecimal) data.get("totalCost");
            totalEnergy = totalEnergy.add(energy);
            totalCost = totalCost.add(cost);
            if (energy.compareTo(maxEnergy) > 0) {
                maxEnergy = energy;
            }
        }

        long dataDays = dailyData.size();
        BigDecimal avgDailyEnergy = dataDays > 0 ? totalEnergy.divide(BigDecimal.valueOf(dataDays), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgDailyCost = dataDays > 0 ? totalCost.divide(BigDecimal.valueOf(dataDays), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<String> patterns = analyzePatterns(dailyData);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceName", device.getDeviceName());
        result.put("totalEnergy", totalEnergy.setScale(2, RoundingMode.HALF_UP));
        result.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
        result.put("peakPower", maxEnergy);
        result.put("dailyAvgEnergy", avgDailyEnergy);
        result.put("dailyAvgCost", avgDailyCost);
        result.put("dailyData", dailyData);
        result.put("patternAnalysis", Map.of(
            "trend", patterns.isEmpty() ? "stable" : (patterns.contains("能耗呈上升趋势") ? "increasing" : patterns.contains("能耗呈下降趋势") ? "decreasing" : "stable"),
            "weekdayWeekendDiff", patterns.size() > 1 ? patterns.get(0) : "数据不足"
        ));
        return result;
    }

    private List<Map<String, Object>> loadDailyData(Long deviceId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> dailyData = aggregateUsageRecordByDate(deviceId, startDate, endDate);
        if (!dailyData.isEmpty()) return dailyData;

        List<EnergyConsumption> energyList = energyConsumptionMapper.selectList(
            new LambdaQueryWrapper<EnergyConsumption>()
                .eq(EnergyConsumption::getDeviceId, deviceId)
                .between(EnergyConsumption::getStatDate, startDate, endDate)
                .orderByAsc(EnergyConsumption::getStatDate)
        );

        if (!energyList.isEmpty()) {
            for (EnergyConsumption energy : energyList) {
                dailyData.add(Map.of(
                    "date", energy.getStatDate(),
                    "totalEnergy", energy.getTotalEnergy(),
                    "totalCost", energy.getEnergyCost(),
                    "peakPower", energy.getPeakPower(),
                    "avgPower", energy.getAvgPower()
                ));
            }
            return dailyData;
        }

        return aggregateDeviceData(deviceId, startDate, endDate);
    }

    private List<Map<String, Object>> aggregateUsageRecordByDate(Long deviceId, LocalDate startDate, LocalDate endDate) {
        List<UsageRecord> records = usageRecordMapper.selectList(
            new LambdaQueryWrapper<UsageRecord>()
                .eq(UsageRecord::getDeviceId, deviceId)
                .ge(UsageRecord::getEndTime, startDate.atStartOfDay())
                .lt(UsageRecord::getEndTime, endDate.plusDays(1).atStartOfDay())
                .isNotNull(UsageRecord::getTotalEnergy)
                .orderByAsc(UsageRecord::getEndTime)
        );
        if (records.isEmpty()) return new ArrayList<>();

        Map<LocalDate, List<UsageRecord>> byDate = records.stream()
            .collect(Collectors.groupingBy(r -> r.getEndTime().toLocalDate()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<UsageRecord>> entry : byDate.entrySet()) {
            BigDecimal energy = entry.getValue().stream()
                .map(r -> r.getTotalEnergy() != null ? r.getTotalEnergy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = entry.getValue().stream()
                .map(r -> r.getTotalCost() != null ? r.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal peakPower = entry.getValue().stream()
                .map(r -> r.getTotalEnergy() != null ? r.getTotalEnergy() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            result.add(Map.of(
                "date", entry.getKey(),
                "totalEnergy", energy.setScale(2, RoundingMode.HALF_UP),
                "totalCost", cost.setScale(2, RoundingMode.HALF_UP),
                "peakPower", peakPower,
                "avgPower", energy.divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP)
            ));
        }
        return result;
    }

    /**
     * 从 device_data 表实时聚合能耗数据（三级兜底）
     */
    private List<Map<String, Object>> aggregateDeviceData(Long deviceId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

            List<DeviceData> dataList = deviceDataMapper.selectList(
                new LambdaQueryWrapper<DeviceData>()
                    .eq(DeviceData::getDeviceId, deviceId)
                    .between(DeviceData::getCollectTime, dayStart, dayEnd)
            );

            if (!dataList.isEmpty()) {
                BigDecimal totalEnergy = BigDecimal.ZERO;
                BigDecimal totalCost = BigDecimal.ZERO;
                BigDecimal maxPower = BigDecimal.ZERO;
                BigDecimal totalPower = BigDecimal.ZERO;

                for (DeviceData data : dataList) {
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

                BigDecimal avgPower = totalPower.divide(BigDecimal.valueOf(dataList.size()), 2, RoundingMode.HALF_UP);

                result.add(Map.of(
                    "date", currentDate,
                    "totalEnergy", totalEnergy.setScale(2, RoundingMode.HALF_UP),
                    "totalCost", totalCost.setScale(2, RoundingMode.HALF_UP),
                    "peakPower", maxPower,
                    "avgPower", avgPower
                ));
            }

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    /**
     * 生成负载均衡建议
     *
     * 针对每台设备：
     *   1. 读取 utilization.window.{deviceType} 配置窗口时长（默认24小时）
     *   2. 统计窗口内 usage_record 的实际使用时长（不是预约时长，是实际插电时长）
     *   3. 计算负载因子 = 实际使用时长 / 窗口时长
     *   4. 读取阈值（按设备类型区分）：
     *      - threshold.overload.{type} 默认 0.8（过载）
     *      - threshold.underload.{type} 默认 0.3（低负载）
     *   5. 状态分类：过载/正常/低负载/空闲
     *   6. 过载设备尝试匹配同类型的低负载设备，生成转移建议
     *
     * 返回值按负载因子降序排列（过载最严重的排第一）
     */
    public List<Map<String, Object>> generateLoadBalancingSuggestions() {
        // 查所有设备
        List<Device> allDevices = deviceMapper.selectList(null);

        List<Map<String, Object>> items = new ArrayList<>();

        for (Device device : allDevices) {
            // 读取该类型设备的窗口时长（默认24小时），可从 sys_config 动态配置
            int windowHours = configService.getIntConfig("utilization.window." + device.getDeviceType(), 24);
            LocalDateTime since = LocalDateTime.now().minusHours(windowHours);
            BigDecimal windowH = BigDecimal.valueOf(windowHours);

            // 从 usage_record 查窗口内的实际使用记录（有 endTime 和 totalEnergy 的才算有效使用）
            List<UsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<UsageRecord>()
                    .eq(UsageRecord::getDeviceId, device.getId())
                    .ge(UsageRecord::getEndTime, since)
                    .isNotNull(UsageRecord::getTotalEnergy)
            );

            long usageCount = records.size();
            long totalSeconds = 0;
            BigDecimal totalEnergy = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;

            // 统计窗口内累计使用时长（秒）、累计能耗、累计电费
            for (UsageRecord r : records) {
                if (r.getEndTime() != null && r.getStartTime() != null) {
                    totalSeconds += Duration.between(r.getStartTime(), r.getEndTime()).getSeconds();
                }
                if (r.getTotalEnergy() != null) totalEnergy = totalEnergy.add(r.getTotalEnergy());
                if (r.getTotalCost() != null) totalCost = totalCost.add(r.getTotalCost());
            }

            BigDecimal ratedPower = device.getRatedPower() != null ? device.getRatedPower() : BigDecimal.valueOf(100);
            // 总使用时长（小时）
            BigDecimal totalHours = BigDecimal.valueOf(totalSeconds).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
            // 负载因子 = 总使用时长 / 窗口时长，上限1.0（不能超过100%）
            double loadFactor = totalSeconds > 0 ? Math.min(totalHours.divide(windowH, 4, RoundingMode.HALF_UP).doubleValue(), 1.0) : 0;
            // 平均功率 = 总能耗(kWh) / 总时长(h)
            BigDecimal avgPower = totalSeconds > 0
                ? totalEnergy.multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(totalSeconds), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            // 读取动态阈值（按设备类型：同类设备可能有不同的阈值配置）
            BigDecimal overloadTh = configService.getDecimalConfig("threshold.overload." + device.getDeviceType(), BigDecimal.valueOf(0.8));
            BigDecimal underloadTh = configService.getDecimalConfig("threshold.underload." + device.getDeviceType(), BigDecimal.valueOf(0.3));

            // 构建该设备的负载信息
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deviceId", device.getId());
            item.put("deviceName", device.getDeviceName());
            item.put("deviceType", device.getDeviceType());
            item.put("loadFactor", loadFactor);
            item.put("currentPower", avgPower);
            item.put("ratedPower", ratedPower);
            item.put("usageCount", usageCount);
            item.put("totalHours", totalHours);
            item.put("totalEnergy", totalEnergy.setScale(2, RoundingMode.HALF_UP));
            item.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));

            // 四档状态分类
            if (usageCount > 0 && loadFactor > overloadTh.doubleValue()) {
                item.put("status", "overload");
                // 尝试找同类型的低负载设备做转移建议
                item.put("suggestion", getOptimizationSuggestion(items, device, loadFactor));
            } else if (usageCount > 0 && loadFactor < underloadTh.doubleValue()) {
                item.put("status", "underload");
                item.put("suggestion", "设备利用率偏低，建议引导用户预约该设备");
            } else if (usageCount == 0) {
                item.put("status", "idle");
                item.put("suggestion", "过去" + windowHours + "小时内未使用");
            } else {
                item.put("status", "normal");
                item.put("suggestion", "设备利用率正常");
            }

            items.add(item);
        }

        // 第二遍遍历：给那些没找到同类型低负载设备的过载设备补全通用建议
        for (Map<String, Object> item : items) {
            if ("overload".equals(item.get("status")) && item.get("suggestion") == null) {
                item.put("suggestion", "设备使用频繁，建议增加备用设备或调整使用时段");
            }
        }

        // 按负载因子降序排列（过载最严重的排第一）
        items.sort((a, b) -> Double.compare((Double) b.get("loadFactor"), (Double) a.get("loadFactor")));
        return items;
    }

    /**
     * 为过载设备生成量化转移建议
     *
     * 逻辑：遍历已有设备列表，找第一个同类型且低负载的设备
     * 转移比例 = min((当前负载率 - 0.7) / 当前负载率, 0.5)，最多转移50%
     * 目标是让过载设备降到约70%利用率
     */
    private String getOptimizationSuggestion(List<Map<String, Object>> existing, Device device, double loadFactor) {
        String deviceType = device.getDeviceType();
        for (Map<String, Object> other : existing) {
            // 只匹配同类型的低负载设备
            if ("underload".equals(other.get("status")) && deviceType.equals(other.get("deviceType"))) {
                double otherLoad = (Double) other.get("loadFactor");
                // 转移比例 = (lf - 0.7) / lf，上限50%
                double transfer = Math.min((loadFactor - 0.7) / loadFactor, 0.5);
                return String.format("负载率 %.0f%%，建议将约 %.0f%% 预约转移至 %s（利用率 %.0f%%）",
                    loadFactor * 100, transfer * 100, other.get("deviceName"), otherLoad * 100);
            }
        }
        return null;
    }

    /**
     * 预测未来能耗
     *
     * 流程：
     *   1. 加载最近30天历史能耗数据（从 usage_record 取）
     *   2. 过滤空值和零值
     *   3. 调用 TimeSeriesForecast.forecast() 执行 Holt-Winters 预测
     *   4. 对预测结果做节假日修正：
     *      - 检查 sys_config 中是否有 holiday.YYYY-MM-DD 配置
     *      - 无条件则周末按 0.8 系数折扣
     *   5. 应用预测电价（getPredictedPrice 考虑周末/季节因素）
     *   6. 汇总总预测能耗和费用
     */
    public Map<String, Object> predictEnergyConsumption(Long deviceId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        // 步骤1：加载历史能耗数据
        List<Map<String, Object>> dailyData = loadDailyData(deviceId, startDate, endDate);

        if (dailyData.isEmpty()) {
            return Map.of("error", "无历史能耗数据");
        }

        // 步骤2：提取能耗时间序列（过滤空值和零值——零值数据会打乱Holt-Winters的季节性）
        List<BigDecimal> energyHistory = dailyData.stream()
            .map(d -> (BigDecimal) d.get("totalEnergy"))
            .filter(e -> e != null && e.compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        // 步骤3：Holt-Winters 时序预测
        List<TimeSeriesForecast.Point> forecastPoints = TimeSeriesForecast.forecast(energyHistory, days);
        if (forecastPoints.isEmpty()) {
            return Map.of("error", "历史能耗数据不足以预测");
        }

        // 步骤4~6：逐个预测日计算
        List<Map<String, Object>> predictions = new ArrayList<>();
        BigDecimal totalPredictedEnergy = BigDecimal.ZERO;
        BigDecimal totalPredictedCost = BigDecimal.ZERO;

        for (int i = 0; i < days; i++) {
            LocalDate predictDate = endDate.plusDays(i + 1);
            TimeSeriesForecast.Point pt = forecastPoints.get(i);

            // 步骤4：节假日修正
            // 先从 sys_config 查 holiday.YYYY-MM-DD 配置（重大节日如春节自定义系数）
            BigDecimal ratio = configService.getDecimalConfig("holiday." + predictDate.toString(), BigDecimal.ZERO);
            if (ratio.compareTo(BigDecimal.ZERO) <= 0) {
                // 没有配置则按周末 0.8、工作日 1.0
                ratio = predictDate.getDayOfWeek().getValue() >= 6
                    ? BigDecimal.valueOf(0.8) : BigDecimal.ONE;
            }

            // 点预测值 × 节假日系数
            double energyVal = pt.forecast * ratio.doubleValue();
            double lowerVal = pt.lower * ratio.doubleValue();
            double upperVal = pt.upper * ratio.doubleValue();

            // 步骤5：预测电价（考虑周末9折、夏季1.1倍、冬季1.05倍）
            BigDecimal predictedEnergy = BigDecimal.valueOf(Math.max(energyVal, 0)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal electricityPrice = electricityPriceService.getPredictedPrice(predictDate);
            BigDecimal predictedCost = predictedEnergy.multiply(electricityPrice).setScale(2, RoundingMode.HALF_UP);

            predictions.add(Map.of(
                "date", predictDate,
                "predictedEnergy", predictedEnergy,
                "predictedCost", predictedCost,
                "predictedPower", predictedEnergy,
                "price", electricityPrice,
                "lowerBound", BigDecimal.valueOf(Math.max(lowerVal, 0)).setScale(2, RoundingMode.HALF_UP),
                "upperBound", BigDecimal.valueOf(Math.max(upperVal, 0)).setScale(2, RoundingMode.HALF_UP)
            ));

            totalPredictedEnergy = totalPredictedEnergy.add(predictedEnergy);
            totalPredictedCost = totalPredictedCost.add(predictedCost);
        }

        // 历史日均能耗（用于对比展示）
        BigDecimal avgDailyEnergy = energyHistory.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(energyHistory.size()), 2, RoundingMode.HALF_UP);

        return Map.of(
            "avgDailyEnergy", avgDailyEnergy,
            "totalPredictedEnergy", totalPredictedEnergy,
            "totalPredictedCost", totalPredictedCost,
            "predictions", predictions
        );
    }

    /**
     * 计算环保指标（CO₂减排量、标准煤节省、等效植树）
     *
     * 折算系数来源：
     *   - 0.785 kgCO₂/kWh：全国电网排放因子（kgCO₂每度电）
     *   - 0.1229 kgce/kWh：标准煤折算系数（kg标准煤每度电）
     *   - 18 kgCO₂/棵·年：一棵成年树年均CO₂吸收量
     *
     * 参考标准：GB/T 2589-2020《综合能耗计算通则》
     */
    public Map<String, Object> calculateEnvironmentalMetrics(BigDecimal totalEnergy) {
        // 环保折算系数（参照国家发改委《综合能耗计算通则》GB/T 2589-2020）
        BigDecimal co2Factor = new BigDecimal("0.785");     // 每kWh减排CO₂千克
        BigDecimal coalFactor = new BigDecimal("0.1229");   // 每kWh节省标准煤千克
        BigDecimal treeFactor = new BigDecimal("18");       // 每棵树年均吸收CO₂千克

        // CO₂减排量 = 总能耗(kWh) × 0.785 kg/kWh
        BigDecimal co2Reduction = totalEnergy.multiply(co2Factor).setScale(2, RoundingMode.HALF_UP);
        // 标准煤节省 = 总能耗(kWh) × 0.1229 kgce/kWh
        BigDecimal coalSaved = totalEnergy.multiply(coalFactor).setScale(2, RoundingMode.HALF_UP);
        // 等效植树量 = CO₂减排量(kg) / 18 kg/棵·年（取整）
        BigDecimal treeEquivalent = co2Reduction.divide(treeFactor, 0, RoundingMode.HALF_UP);

        return Map.of(
            "totalEnergy", totalEnergy.setScale(2, RoundingMode.HALF_UP),
            "co2Reduction", co2Reduction,
            "co2Unit", "kg",
            "coalSaved", coalSaved,
            "coalUnit", "kgce",
            "treeEquivalent", treeEquivalent,
            "treeUnit", "棵"
        );
    }

    /**
     * 计算能耗成本（简化实现：使用平均电价）
     */
    public BigDecimal calculateEnergyCost(BigDecimal energy, LocalDateTime startTime, LocalDateTime endTime) {
        BigDecimal avgPrice = electricityPriceService.getAveragePrice(startTime, endTime);
        return energy.multiply(avgPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 分析能耗模式
     *
     * 两种模式分析：
     *   1. 工作日 vs 周末能耗对比（阈值1.2x，超过则判定为"明显偏高"）
     *   2. 能耗趋势：比较最近7天 vs 最早7天的均值（阈值1.1x）
     */
    private List<String> analyzePatterns(List<Map<String, Object>> dailyData) {
        List<String> patterns = new ArrayList<>();

        if (dailyData.size() < 7) {
            patterns.add("数据不足，无法分析模式");
            return patterns;
        }

        // 分析工作日vs周末模式
        List<BigDecimal> weekdayEnergy = new ArrayList<>();
        List<BigDecimal> weekendEnergy = new ArrayList<>();

        for (Map<String, Object> data : dailyData) {
            LocalDate date = (LocalDate) data.get("date");
            BigDecimal energy = (BigDecimal) data.get("totalEnergy");
            if (date.getDayOfWeek().getValue() <= 5) {
                weekdayEnergy.add(energy);
            } else {
                weekendEnergy.add(energy);
            }
        }

        if (!weekdayEnergy.isEmpty() && !weekendEnergy.isEmpty()) {
            BigDecimal avgWeekday = weekdayEnergy.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(weekdayEnergy.size()), 2, RoundingMode.HALF_UP);
            BigDecimal avgWeekend = weekendEnergy.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(weekendEnergy.size()), 2, RoundingMode.HALF_UP);

            if (avgWeekday.compareTo(avgWeekend.multiply(BigDecimal.valueOf(1.2))) > 0) {
                patterns.add("工作日能耗明显高于周末");
            } else if (avgWeekend.compareTo(avgWeekday.multiply(BigDecimal.valueOf(1.2))) > 0) {
                patterns.add("周末能耗明显高于工作日");
            } else {
                patterns.add("工作日和周末能耗较为均衡");
            }
        }

        // 分析能耗趋???
        List<BigDecimal> energyValues = dailyData.stream()
            .map(d -> (BigDecimal) d.get("totalEnergy"))
            .collect(Collectors.toList());

        if (energyValues.size() >= 7) {
            BigDecimal firstWeekAvg = energyValues.subList(0, 7).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
            BigDecimal lastWeekAvg = energyValues.subList(Math.max(0, energyValues.size() - 7), energyValues.size()).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

            if (lastWeekAvg.compareTo(firstWeekAvg.multiply(BigDecimal.valueOf(1.1))) > 0) {
                patterns.add("能耗呈上升趋势");
            } else if (firstWeekAvg.compareTo(lastWeekAvg.multiply(BigDecimal.valueOf(1.1))) > 0) {
                patterns.add("能耗呈下降趋势");
            } else {
                patterns.add("能耗较为稳定");
            }
        }

        return patterns;
    }
}
