package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.PaymentService;
import com.shorepower.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ReservationService reservationService;

    @GetMapping("/info/{tradeNo}")
    public Result<?> getInfo(Authentication auth, @PathVariable String tradeNo) {
        Long userId = (Long) auth.getPrincipal();
        Map<String, Object> info = paymentService.getOrderInfo(tradeNo);
        if (info == null) return Result.fail("订单不存在");
        if (!userId.equals(info.get("userId"))) return Result.fail("无权查看此订单");
        return Result.ok(info);
    }

    /**
     * 模拟支付回调
     *
     * 演示环境：必须登录，且只能完成本人支付订单（校验订单 userId == 当前用户）。
     * 幂等：已 PAID 的订单重复回调直接返回成功。
     * 生产环境：应改为支付网关回调（来源 IP + 签名 + 金额校验），本接口不应暴露。
     */
    @PostMapping("/callback")
    public Result<?> callback(Authentication auth, @RequestParam String tradeNo) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.completePayment(userId, tradeNo);
    }
}
