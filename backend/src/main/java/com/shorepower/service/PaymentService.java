package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.entity.Device;
import com.shorepower.entity.PaymentOrder;
import com.shorepower.entity.Reservation;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.PaymentOrderMapper;
import com.shorepower.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final ReservationMapper reservationMapper;
    private final DeviceMapper deviceMapper;

    public PaymentOrder createOrder(Long reservationId, Long userId, BigDecimal amount, String method) {
        PaymentOrder order = new PaymentOrder();
        order.setReservationId(reservationId);
        order.setUserId(userId);
        order.setAmount(amount);
        order.setMethod(method);
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());

        String orderNo = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        order.setTradeNo("PAY" + orderNo);

        // 生成模拟支付二维码（实际集成时替换为支付宝/微信SDK调用）
        order.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + order.getTradeNo());

        paymentOrderMapper.insert(order);
        log.info("创建支付单: tradeNo={}, amount={}, method={}", order.getTradeNo(), amount, method);
        return order;
    }

    public PaymentOrder getByTradeNo(String tradeNo) {
        return paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getTradeNo, tradeNo)
        );
    }

    public Map<String, Object> getOrderInfo(String tradeNo) {
        PaymentOrder order = getByTradeNo(tradeNo);
        if (order == null) return null;

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("tradeNo", order.getTradeNo());
        info.put("amount", order.getAmount());
        info.put("method", order.getMethod());
        info.put("status", order.getStatus());
        info.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : null);
        info.put("userId", order.getUserId());

        // 关联预约和设备信息
        Reservation reservation = reservationMapper.selectById(order.getReservationId());
        if (reservation != null) {
            info.put("reservationNo", reservation.getReservationNo());
            info.put("startTime", reservation.getStartTime() != null ? reservation.getStartTime().toString() : null);
            info.put("endTime", reservation.getEndTime() != null ? reservation.getEndTime().toString() : null);
            Device device = deviceMapper.selectById(reservation.getDeviceId());
            info.put("deviceName", device != null ? device.getDeviceName() : "未知设备");
        }
        return info;
    }

    public boolean processCallback(String tradeNo) {
        PaymentOrder order = getByTradeNo(tradeNo);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            return false;
        }
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        paymentOrderMapper.updateById(order);
        log.info("支付回调成功: tradeNo={}", tradeNo);
        return true;
    }
}
