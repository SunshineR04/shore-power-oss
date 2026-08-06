package com.shorepower.service;

import com.shorepower.entity.PaymentOrder;
import com.shorepower.entity.Reservation;
import com.shorepower.entity.UsageRecord;
import com.shorepower.mapper.PaymentOrderMapper;
import com.shorepower.mapper.ReservationMapper;
import com.shorepower.mapper.UsageRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReservationService 支付与预约归属逻辑单元测试（只验证归属/状态校验，不依赖数据库）。
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationMapper reservationMapper;
    @Mock private UsageRecordMapper usageRecordMapper;
    @Mock private PaymentOrderMapper paymentOrderMapper;
    @Mock private PaymentService paymentService;
    @Mock private SimpMessagingTemplate ws;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationMapper, null, null, usageRecordMapper, null,
                null, null, ws, null, null, paymentService, paymentOrderMapper);
    }

    private Reservation pendingPaymentReservation(Long id, Long userId) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setDeviceId(1L);
        r.setStatus("PENDING_PAYMENT");
        return r;
    }

    @Test
    void payBilling_rejectsOtherUsersReservation() {
        UsageRecord record = new UsageRecord();
        record.setTotalCost(BigDecimal.TEN);
        when(usageRecordMapper.selectOne(any())).thenReturn(record);
        when(reservationMapper.selectById(99L)).thenReturn(pendingPaymentReservation(99L, 999L));

        var result = service.payBilling(1L, 99L, "ALIPAY");
        assertEquals(500, result.getCode());
        verify(paymentService, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void payBilling_rejectsNonPendingPaymentStatus() {
        UsageRecord record = new UsageRecord();
        record.setTotalCost(BigDecimal.TEN);
        when(usageRecordMapper.selectOne(any())).thenReturn(record);
        Reservation r = pendingPaymentReservation(1L, 1L);
        r.setStatus("IN_USE");
        when(reservationMapper.selectById(1L)).thenReturn(r);

        var result = service.payBilling(1L, 1L, "ALIPAY");
        assertEquals(500, result.getCode());
        verify(paymentService, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    void completePayment_rejectsOtherUsersOrder() {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setUserId(999L);
        order.setTradeNo("PAY123");
        order.setStatus("PENDING");
        when(paymentOrderMapper.selectOne(any())).thenReturn(order);

        var result = service.completePayment(1L, "PAY123");
        assertEquals(500, result.getCode());
        verify(paymentService, never()).processCallback(any());
    }

    @Test
    void completePayment_idempotent_whenAlreadyPaid() {
        PaymentOrder paid = new PaymentOrder();
        paid.setId(1L);
        paid.setUserId(1L);
        paid.setTradeNo("PAY123");
        paid.setStatus("PAID");
        when(paymentOrderMapper.selectOne(any())).thenReturn(paid);

        var result = service.completePayment(1L, "PAY123");
        assertEquals(200, result.getCode());
        verify(paymentService, never()).processCallback(any());
    }
}
