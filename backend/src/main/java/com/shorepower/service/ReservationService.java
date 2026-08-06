package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.common.Result;
import com.shorepower.entity.Device;
import com.shorepower.entity.DeviceData;
import com.shorepower.entity.DeviceRating;
import com.shorepower.entity.PaymentOrder;
import com.shorepower.entity.Reservation;
import com.shorepower.entity.SysUser;
import com.shorepower.entity.UsageRecord;
import com.shorepower.mapper.DeviceDataMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.DeviceRatingMapper;
import com.shorepower.mapper.PaymentOrderMapper;

import com.shorepower.mapper.ReservationMapper;
import com.shorepower.mapper.SysUserMapper;
import com.shorepower.mapper.UsageRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约管理核心服务
 *
 * 负责预约的完整生命周期管理：创建→确认→使用→结算→支付→完成
 *
 * 状态流转图：
 *   PENDING ──confirm──→ CONFIRMED ──start──→ IN_USE ──end──→ PENDING_PAYMENT ──pay──→ COMPLETED
 *      │                      │
 *      └──cancel──→ CANCELLED └──cancel──→ CANCELLED
 *
 * 核心功能：
 *   1. 创建预约（5步校验：设备存在→设备在线→时间冲突→去重→时间合理性）
 *   2. 船舶-充电桩兼容性匹配（精确电压匹配 / 类型映射兜底）
 *   3. 使用开始/结束（锁定/释放设备、能耗结算）
 *   4. 分时电价计费 + 最低保底费用
 *   5. 支付流程（创建支付单 → 支付回调 → 完成）
 *   6. 使用评价
 *
 * 数据流向：预约记录 → 使用记录(usage_record) → 设备数据聚合(结算) → 支付单
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final UsageRecordMapper usageRecordMapper;
    private final DeviceRatingMapper deviceRatingMapper;
    private final SysUserMapper userMapper;
    private final ElectricityPriceService electricityPriceService;
    private final SimpMessagingTemplate ws;
    private final ShipService shipService;
    private final DeviceTypeService deviceTypeService;
    private final PaymentService paymentService;
    private final PaymentOrderMapper paymentOrderMapper;

    /** 每笔订单固定服务费 5 元 */
    private static final BigDecimal SERVICE_FEE = BigDecimal.valueOf(5);
    /** 最低负载因子 30%：用于计算保底能耗 */
    private static final BigDecimal MIN_LOAD_FACTOR = BigDecimal.valueOf(0.30);
    /** 最低计费时长 30 分钟：防止超短使用时费用过低 */
    private static final int MIN_MINUTES = 30;

    /**
     * 创建预约 — 5步顺序校验
     *
     * 校验链必须按以下顺序执行（不可颠倒，原因见各步注释）：
     *   V1. 设备存在性检查 —— 最基础的检查，后面步骤都依赖 device
     *   V2. 设备状态必须 ONLINE —— 已故障/使用中的设备不可预约
     *   V3. 时间段冲突检测 —— SQL 区间重叠公式，排除自身预约
     *   V4. 同一用户同一设备不可有未完成订单 —— 防止重复预约
     *   V5. 时间合理性 —— startTime ≥ 当前时间，endTime > startTime
     *
     * 船舶-充电桩匹配（两层兜底）：
     *   Tier 1: 有精确电气数据（电压/功率）→ 精确比对
     *   Tier 2: 只有船舶类型 → 查预定义的 type→pileType 映射表
     *   Tier N: 没有船舶信息 → 跳过匹配
     *
     * 预计费用 = 额定功率 × 55%负载因子 × 使用时长 × 分时电价
     *
     * 并发安全：方法在事务内先对设备行加排他锁（SELECT ... FOR UPDATE），
     * 使同一设备的并发预约串行化，配合 V8 的时间段冲突检查杜绝重叠预约。
     */
    @Transactional
    public Result<Reservation> createReservation(Long userId, Long deviceId, Long shipId, LocalDateTime startTime, LocalDateTime endTime) {
        // V1: 设备必须存在（行级锁：同一设备的并发创建在此串行）
        Device device = deviceMapper.selectByIdForUpdate(deviceId);
        if (device == null) {
            return Result.fail("设备不存在");
        }
        // V2: 设备必须处于 ONLINE 状态（故障/使用中的设备不可预约）
        if (!"ONLINE".equals(device.getStatus())) {
            return Result.fail("设备当前不可用");
        }

        // V3: 时间冲突检测 —— 查 reservation 表中相同设备+重叠时段+活跃状态的其他用户预约
        // SQL 区间重叠条件：existing.start < new.end AND existing.end > new.start
        // 排除自身（userId）的预约，避免与自己的预约冲突
        long conflict = reservationMapper.countConflict(deviceId, userId,
            startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (conflict > 0) {
            return Result.fail("该时段已有其他用户预约，请选择其他时间");
        }

        // V4: 同一用户在同一设备上不能有未完成的活跃预约
        // 活跃状态：PENDING/CONFIRMED/IN_USE/PENDING_PAYMENT
        // 目的是防止用户在同一台设备上堆积多个预约后不处理
        long userActive = reservationMapper.countUserActiveReservations(deviceId, userId);
        if (userActive > 0) {
            return Result.fail("您已有该设备的预约，请先完成或取消现有预约后再预约");
        }

        // V5a: 开始时间不能早于当前时间（不能预约过去的时间段）
        if (startTime.isBefore(LocalDateTime.now())) {
            return Result.fail("预约开始时间不能早于当前时间");
        }
        // V5b: 结束时间必须严格晚于开始时间（至少1分钟）
        if (!endTime.isAfter(startTime)) {
            return Result.fail("结束时间必须晚于开始时间");
        }

        // === 船舶-充电桩兼容性匹配（双层兜底） ===
        if (shipId != null) {
            com.shorepower.entity.Ship ship = shipService.getById(shipId);
            if (ship == null) {
                return Result.fail("船舶不存在");
            }
            // 归属校验：只能使用自己的船舶预约
            if (!ship.getUserId().equals(userId)) {
                return Result.fail("无权使用此船舶");
            }
            if (ship.getRatedVoltage() != null && ship.getRatedPower() != null) {
                // 电压必须完全匹配（如380V配380V，6600V配6600V）
                if (device.getRatedVoltage() != null && ship.getRatedVoltage().compareTo(device.getRatedVoltage()) != 0) {
                    return Result.fail("船舶电压 " + ship.getRatedVoltage() + "V 与设备电压 " + device.getRatedVoltage() + "V 不匹配");
                }
                // 船舶额定功率不能超过设备额定功率
                if (device.getRatedPower() != null && ship.getRatedPower().compareTo(device.getRatedPower()) > 0) {
                    return Result.fail("船舶功率 " + ship.getRatedPower() + "kW 超过设备额定功率 " + device.getRatedPower() + "kW");
                }
            } else if (ship.getShipType() != null) {
                // Tier2: 船舶无精确参数但有类型 → 查类型映射表
                Map<String, Object> types = deviceTypeService.getDeviceTypes();
                Map<String, List<String>> shipToPileMap = (Map<String, List<String>>) types.get("shipToPileMap");
                List<String> compatiblePiles = shipToPileMap.get(ship.getShipType());
                // 如果映射表不存在该类型或设备不在兼容列表中，拒绝
                if (compatiblePiles != null && !compatiblePiles.contains(device.getDeviceType())) {
                    return Result.fail("该船舶类型不适用于此充电桩");
                }
            }
        }

        // === 计算预计费用 ===
        // 用额定功率 × 55%负载因子 估算实际功率，调 ElectricityPriceService 按分时电价算费
        BigDecimal estimatedPower = device.getRatedPower() != null ? device.getRatedPower() : BigDecimal.valueOf(60);
        BigDecimal estimatedCost = electricityPriceService.calculateEstimatedCost(startTime, endTime, estimatedPower);

        // === 创建预约记录 ===
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setDeviceId(deviceId);
        reservation.setShipId(shipId);
        // 预约编号格式：RES + 时间戳13位 + 3位随机数（防碰撞）
        reservation.setReservationNo("RES" + System.currentTimeMillis() + (int)(Math.random()*900+100));
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus("PENDING");  // 初始状态：待确认
        reservation.setEstimatedCost(estimatedCost);
        reservationMapper.insert(reservation);

        return Result.ok(reservation);
    }

    public Result<List<Map<String, Object>>> getUserReservations(Long userId) {
        List<Map<String, Object>> list = reservationMapper.getUserReservations(userId);
        return Result.ok(list);
    }

    public Result<Map<String, Object>> getReservationDetail(Long id, Long userId, boolean isAdmin) {
        Map<String, Object> detail = reservationMapper.getReservationDetail(id);
        if (detail == null) {
            return Result.fail("预约不存在");
        }
        // IDOR 防护：非管理员只能查看本人的预约详情
        Object ownerId = detail.get("userId");
        if (!isAdmin && (ownerId == null || !ownerId.toString().equals(userId.toString()))) {
            return Result.fail("无权查看此预约");
        }
        return Result.ok(detail);
    }

    /**
     * 确认预约：PENDING → CONFIRMED
     */
    public Result<?> confirmReservation(Long userId, Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return Result.fail("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            return Result.fail("无权操作此预约");
        }
        reservation.setStatus("CONFIRMED");
        reservationMapper.updateById(reservation);
        return Result.ok("预约已确认");
    }

    /**
     * 取消预约：PENDING/CONFIRMED → CANCELLED
     * 使用中（IN_USE）不允许取消
     */
    public Result<?> cancelReservation(Long userId, Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return Result.fail("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            return Result.fail("无权操作此预约");
        }
        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            return Result.fail("当前状态不允许取消");
        }
        reservation.setStatus("CANCELLED");
        reservationMapper.updateById(reservation);
        return Result.ok("预约已取消");
    }

    /**
     * 开始使用：CONFIRMED → IN_USE
     *
     * 执行内容：
     *   1. 更新预约状态为使用中
     *   2. 锁定设备（标记为 IN_USE），并推送到 /topic/device-status
     *   3. 创建使用记录（usage_record），记录开始时间
     *
     * 注意：使用开始后，DataSimulator 会检测到设备状态变化，
     * 在 generatePileData 中触发软启动模拟（负载从0开始）
     *
     * @Transactional 保证预约状态、设备状态、使用记录三者一致
     */
    @Transactional
    public Result<?> startUsage(Long userId, Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return Result.fail("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            return Result.fail("无权操作此预约");
        }
        if (!"CONFIRMED".equals(reservation.getStatus())) {
            return Result.fail("请先确认预约");
        }

        // 步骤1：预约状态 → IN_USE
        reservation.setStatus("IN_USE");
        reservationMapper.updateById(reservation);

        // 步骤2：设备锁定（状态 → IN_USE），防止其他人预约
        Device device = deviceMapper.selectById(reservation.getDeviceId());
        if (device != null) {
            device.setStatus("IN_USE");
            deviceMapper.updateById(device);
            // 推送设备状态变更，前端仪表盘立即反映设备正被使用
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("deviceId", device.getId());
            statusUpdate.put("status", "IN_USE");
            ws.convertAndSend("/topic/device-status", statusUpdate);
        }

        // 步骤3：创建使用记录（记录使用开始时间，结束时再回填能耗数据）
        UsageRecord record = new UsageRecord();
        record.setReservationId(id);
        record.setUserId(reservation.getUserId());
        record.setDeviceId(reservation.getDeviceId());
        record.setStartTime(LocalDateTime.now());
        usageRecordMapper.insert(record);

        return Result.ok("已开始使用");
    }

    /**
     * 结束使用：IN_USE → PENDING_PAYMENT
     *
     * 结算流程：
     *   1. 从 device_data 表聚合使用时段的实际能耗
     *   2. 保底处理：如果实际能耗 < 额定功率×30%×30分钟，按保底值结算
     *      原因：模拟数据有随机波动，偶尔能耗异常偏低，保底防止码头运营亏损
     *      （真实电表数据不会有此问题）
     *   3. 电度电费 = 分时电价计算(能耗)
     *   4. 总费用 = 电度电费 + 5元服务费
     *   5. 更新使用记录、预约状态、释放设备
     *
     * @Transactional 保证结算过程的原子性
     */
    @Transactional
    public Result<Map<String, Object>> endUsage(Long userId, Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return Result.fail("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            return Result.fail("无权操作此预约");
        }
        if (!"IN_USE".equals(reservation.getStatus())) {
            return Result.fail("当前未在使用的状态");
        }

        // 查使用记录（预约创建时在 startUsage 中生成的）
        UsageRecord record = usageRecordMapper.selectOne(
            new LambdaQueryWrapper<UsageRecord>()
                .eq(UsageRecord::getReservationId, id)
        );

        if (record == null) {
            return Result.fail("使用记录不存在");
        }

        LocalDateTime endTime = LocalDateTime.now();
        record.setEndTime(endTime);

        // === 步骤1：从 device_data 表聚合使用时段内的实际能耗 ===
        List<DeviceData> dataList = deviceDataMapper.selectList(
            new LambdaQueryWrapper<DeviceData>()
                .eq(DeviceData::getDeviceId, reservation.getDeviceId())
                .between(DeviceData::getCollectTime, record.getStartTime(), endTime)
        );

        // 累加所有数据点的能耗值（energyConsumption 字段是每个推送周期的增量能耗 kWh）
        BigDecimal totalEnergy = BigDecimal.ZERO;
        for (DeviceData data : dataList) {
            if (data.getEnergyConsumption() != null) {
                totalEnergy = totalEnergy.add(data.getEnergyConsumption());
            }
        }

        // 获取设备额定功率
        Device device = deviceMapper.selectById(reservation.getDeviceId());
        BigDecimal ratedPower = device != null && device.getRatedPower() != null
                ? device.getRatedPower() : BigDecimal.valueOf(100);

        // === 步骤2：保底处理 ===
        // 保底能耗 = 额定功率 × 30%(最小负载因子) × 30分钟(最小使用时长)
        // 取 max(实际能耗, 保底能耗) 作为计费依据
        BigDecimal minEnergy = ratedPower.multiply(MIN_LOAD_FACTOR)
                .multiply(BigDecimal.valueOf(MIN_MINUTES))
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        totalEnergy = totalEnergy.max(minEnergy);

        // === 步骤3：分时电价计算电度电费 ===
        // 调用 ElectricityPriceService 按峰/平/谷时段分配能耗，各时段价格不同
        BigDecimal energyCost = electricityPriceService.calculateTimeOfUseCost(record.getStartTime(), endTime, totalEnergy);

        // === 步骤4：总费用 = 电度电费 + 固定服务费(5元) ===
        BigDecimal totalCost = energyCost.add(SERVICE_FEE).setScale(2, RoundingMode.HALF_UP);

        // 回填使用记录的实际能耗和总费用
        record.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
        record.setTotalCost(totalCost);
        usageRecordMapper.updateById(record);

        // === 步骤5：预约状态 → PENDING_PAYMENT（待支付） ===
        reservation.setStatus("PENDING_PAYMENT");
        reservation.setActualCost(totalCost);
        reservationMapper.updateById(reservation);

        // === 释放设备：状态恢复为 ONLINE ===
        if (device != null) {
            device.setStatus("ONLINE");
            deviceMapper.updateById(device);
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("deviceId", device.getId());
            statusUpdate.put("status", "ONLINE");
            ws.convertAndSend("/topic/device-status", statusUpdate);
        }

        // === 构建账单返回给前端 ===
        long usageSeconds = Duration.between(record.getStartTime(), endTime).getSeconds();

        Map<String, Object> billing = new HashMap<>();
        billing.put("reservationNo", reservation.getReservationNo());
        billing.put("deviceName", device != null ? device.getDeviceName() : "");
        billing.put("startTime", record.getStartTime());
        billing.put("endTime", endTime);
        billing.put("usageMinutes", usageSeconds / 60);
        billing.put("totalEnergy", totalEnergy.setScale(2, RoundingMode.HALF_UP));
        billing.put("energyCost", energyCost.setScale(2, RoundingMode.HALF_UP));
        billing.put("serviceFee", SERVICE_FEE);
        billing.put("totalCost", totalCost);
        billing.put("prices", electricityPriceService.getTimeOfUsePrices());
        billing.put("usageRecordId", record.getId());

        return Result.ok(billing);
    }

    /**
     * 发起支付：创建支付单（生成模拟二维码）
     *
     * @param userId 付款用户ID
     * @param reservationId 预约ID
     * @param method 支付方式（ALIPAY/WECHAT，默认支付宝）
     * @return tradeNo + 模拟二维码URL + 金额
     */
    @Transactional
    public Result<?> payBilling(Long userId, Long reservationId, String method) {
        // 查使用记录确认费用已结算
        UsageRecord record = usageRecordMapper.selectOne(
            new LambdaQueryWrapper<UsageRecord>().eq(UsageRecord::getReservationId, reservationId)
        );
        if (record == null || record.getTotalCost() == null) {
            return Result.fail("费用信息不存在");
        }

        // 预约必须在 PENDING_PAYMENT（待支付）状态，且属于当前用户
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null || !"PENDING_PAYMENT".equals(reservation.getStatus())) {
            return Result.fail("无需支付");
        }
        if (!reservation.getUserId().equals(userId)) {
            return Result.fail("无权操作此预约");
        }

        // 通过 PaymentService 创建支付单（模拟：生成 tradeNo + 模拟二维码）
        String payMethod = (method != null) ? method : "ALIPAY";
        PaymentOrder order = paymentService.createOrder(reservationId, userId, record.getTotalCost(), payMethod);

        // 金额一致性校验：支付单金额必须与结算金额一致
        if (order.getAmount().compareTo(record.getTotalCost()) != 0) {
            return Result.fail("支付金额不一致，请刷新后重试");
        }

        // 返回支付信息给前端（前端展示二维码供用户扫码）
        return Result.ok(Map.of(
            "tradeNo", order.getTradeNo(),
            "qrCodeUrl", order.getQrCodeUrl(),
            "amount", order.getAmount()
        ));
    }

    /**
     * 支付回调：PENDING_PAYMENT → COMPLETED
     *
     * 演示环境由前端“我已支付”按钮调用（必须登录且只能操作本人订单）。
     * 生产接入真实支付网关时应增加签名、商户号、金额校验。
     *
     * 幂等性：订单已被标记 PAID 后，重复回调直接返回成功，不重复处理业务。
     */
    @Transactional
    public Result<?> completePayment(Long userId, String tradeNo) {
        // 查找对应的支付单
        PaymentOrder order = paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getTradeNo, tradeNo)
        );
        if (order == null) return Result.fail("支付单不存在");
        // 归属校验：只能完成本人订单
        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作此订单");
        }
        // 已支付（幂等）：直接返回成功
        if ("PAID".equals(order.getStatus())) {
            return Result.ok("支付成功");
        }

        // 步骤1：处理支付回调（校验支付单状态，标记为已支付）
        boolean paid = paymentService.processCallback(tradeNo);
        if (!paid) {
            return Result.fail("支付失败");
        }

        // 步骤2：查找预约，更新状态为 COMPLETED
        Reservation reservation = reservationMapper.selectById(order.getReservationId());
        if (reservation != null && "PENDING_PAYMENT".equals(reservation.getStatus())) {
            reservation.setStatus("COMPLETED");
            reservationMapper.updateById(reservation);
        }

        // 步骤3：查找使用记录（用于构造推送消息）
        UsageRecord record = usageRecordMapper.selectOne(
            new LambdaQueryWrapper<UsageRecord>().eq(UsageRecord::getReservationId, order.getReservationId())
        );

        // 步骤4：推送 WebSocket 通知，前端收到后刷新预约列表和仪表盘数据
        Map<String, Object> syncMsg = new HashMap<>();
        syncMsg.put("type", "PAYMENT");
        syncMsg.put("deviceId", reservation != null ? reservation.getDeviceId() : null);
        syncMsg.put("reservationId", order.getReservationId());
        syncMsg.put("amount", order.getAmount());
        syncMsg.put("timestamp", LocalDateTime.now().toString());
        ws.convertAndSend("/topic/data-sync", syncMsg);

        return Result.ok("支付成功");
    }

    public Result<List<Map<String, Object>>> getUserUsageRecords(Long userId) {
        return Result.ok(usageRecordMapper.getUserUsageRecords(userId));
    }

    @Transactional
    public Result<?> submitRating(Long userId, Long deviceId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            return Result.fail("评分必须在1-5之间");
        }
        if (deviceId == null) {
            return Result.fail("设备不能为空");
        }

        // 评分资格校验：必须在该设备上有已完成（COMPLETED）的预约
        long completed = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getDeviceId, deviceId)
                .eq(Reservation::getStatus, "COMPLETED")
        );
        if (completed == 0) {
            return Result.fail("仅完成使用的设备可评价");
        }
        // 防重复评分：同一用户同一设备仅允许一条评价
        long rated = deviceRatingMapper.selectCount(
            new LambdaQueryWrapper<DeviceRating>()
                .eq(DeviceRating::getUserId, userId)
                .eq(DeviceRating::getDeviceId, deviceId)
        );
        if (rated > 0) {
            return Result.fail("您已评价过该设备");
        }

        DeviceRating dr = new DeviceRating();
        dr.setUserId(userId);
        dr.setDeviceId(deviceId);
        dr.setRating(rating);
        dr.setComment(comment);
        deviceRatingMapper.insert(dr);

        // 同时更新usage_record中的评价
        UsageRecord record = usageRecordMapper.selectOne(
            new LambdaQueryWrapper<UsageRecord>()
                .eq(UsageRecord::getUserId, userId)
                .eq(UsageRecord::getDeviceId, deviceId)
                .isNull(UsageRecord::getRating)
                .orderByDesc(UsageRecord::getCreateTime)
                .last("LIMIT 1")
        );
        if (record != null) {
            record.setRating(rating);
            record.setComment(comment);
            usageRecordMapper.updateById(record);
        }

        return Result.ok("评价成功");
    }

    public Result<List<Map<String, Object>>> getDeviceRatings(Long deviceId) {
        List<DeviceRating> ratings = deviceRatingMapper.selectList(
            new LambdaQueryWrapper<DeviceRating>()
                .eq(DeviceRating::getDeviceId, deviceId)
                .orderByDesc(DeviceRating::getCreateTime)
        );

        // 批量加载用户，避免 N+1 查询
        List<Long> userIds = ratings.stream()
            .map(DeviceRating::getUserId)
            .filter(u -> u != null)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty()
            ? new HashMap<>()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DeviceRating r : ratings) {
            SysUser user = userMap.get(r.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("rating", r.getRating());
            item.put("comment", r.getComment());
            item.put("userName", user != null ? user.getRealName() : "匿名用户");
            item.put("createTime", r.getCreateTime());
            result.add(item);
        }
        return Result.ok(result);
    }
}
