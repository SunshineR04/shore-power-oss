package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shorepower.entity.PaymentOrder;
import com.shorepower.mapper.PaymentOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PaymentService 单元测试：待支付单复用、回调幂等。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentOrderMapper paymentOrderMapper;
    @Mock private ReservationService reservationService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentOrderMapper, null, null);
    }

    private PaymentOrder pendingOrder(Long id, String tradeNo, BigDecimal amount) {
        PaymentOrder o = new PaymentOrder();
        o.setId(id);
        o.setTradeNo(tradeNo);
        o.setAmount(amount);
        o.setStatus("PENDING");
        return o;
    }

    @Test
    void createOrder_reusesExistingPendingOrder() {
        PaymentOrder existing = pendingOrder(1L, "PAY123", BigDecimal.TEN);
        when(paymentOrderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        PaymentOrder result = paymentService.createOrder(9L, 2L, BigDecimal.TEN, "ALIPAY");

        assertEquals(existing, result);
        verify(paymentOrderMapper, never()).insert(any());
    }

    @Test
    void createOrder_createsNewOrder_whenNonePending() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        PaymentOrder result = paymentService.createOrder(9L, 2L, BigDecimal.TEN, "ALIPAY");

        assertNotNull(result.getTradeNo());
        assertEquals("PENDING", result.getStatus());
        assertEquals(2L, result.getUserId());
        verify(paymentOrderMapper).insert(any());
    }

    @Test
    void processCallback_markPaid_onlyWhenPending() {
        PaymentOrder order = pendingOrder(1L, "PAY123", BigDecimal.TEN);
        when(paymentOrderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        // 条件更新命中
        when(paymentOrderMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        assertTrue(paymentService.processCallback("PAY123"));
        verify(paymentOrderMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void processCallback_idempotent_whenAlreadyPaid() {
        PaymentOrder paid = pendingOrder(1L, "PAY123", BigDecimal.TEN);
        paid.setStatus("PAID");
        when(paymentOrderMapper.selectOne(any(Wrapper.class))).thenReturn(paid);

        assertFalse(paymentService.processCallback("PAY123"));
        verify(paymentOrderMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void processCallback_unknownTradeNo_returnsFalse() {
        when(paymentOrderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        assertFalse(paymentService.processCallback("NOPE"));
    }
}
