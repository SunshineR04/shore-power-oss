package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/info/{tradeNo}")
    public Result<?> getInfo(Authentication auth, @PathVariable String tradeNo) {
        Long userId = (Long) auth.getPrincipal();
        Map<String, Object> info = paymentService.getOrderInfo(tradeNo);
        if (info == null) return Result.fail("订单不存在");
        if (!userId.equals(info.get("userId"))) return Result.fail("无权查看此订单");
        return Result.ok(info);
    }

    /**
     * 支付回调
     *
     * ⚠️ 安全提示：当前为模拟支付流程，仅凭 tradeNo 完成订单，无签名/来源校验。
     * 生产环境必须接入真实支付网关（支付宝 RSA2 / 微信支付 V3 签名），
     * 校验签名 + 金额 + 商户号后再更新订单状态，禁止直接信任 tradeNo 参数。
     */
    @PostMapping("/callback")
    public Result<?> callback(@RequestParam String tradeNo) {
        boolean ok = paymentService.processCallback(tradeNo);
        return ok ? Result.ok("支付成功") : Result.fail("支付回调处理失败");
    }
}
