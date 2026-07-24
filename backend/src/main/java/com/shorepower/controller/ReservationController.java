package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 预约管理 API
 *
 * 对应预约的完整生命周期端点：
 *   create     - 创建预约（5步校验）
 *   confirm    - 确认预约（PENDING → CONFIRMED）
 *   cancel     - 取消预约（PENDING/CONFIRMED → CANCELLED）
 *   start      - 开始使用（CONFIRMED → IN_USE，锁定设备）
 *   end        - 结束使用（IN_USE → PENDING_PAYMENT，能耗结算）
 *   pay        - 发起支付（创建支付单）
 *   pay-callback - 支付回调（PENDING_PAYMENT → COMPLETED）
 *
 * 其他：预约列表、使用记录、设备评分
 */
@RestController
@RequestMapping("/api/user/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReservationService reservationService;

    @PostMapping("/create")
    public Result<?> create(Authentication auth,
                            @RequestParam Long deviceId,
                            @RequestParam(required = false) Long shipId,
                            @RequestParam String startTime,
                            @RequestParam String endTime) {
        Long userId = (Long) auth.getPrincipal();
        LocalDateTime start = LocalDateTime.parse(startTime, FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, FORMATTER);
        return reservationService.createReservation(userId, deviceId, shipId, start, end);
    }

    @GetMapping("/list")
    public Result<?> getList(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.getUserReservations(userId);
    }

    @GetMapping("/detail/{id}")
    public Result<?> getDetail(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return reservationService.getReservationDetail(id, userId, isAdmin);
    }

    @PostMapping("/confirm/{id}")
    public Result<?> confirm(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.confirmReservation(userId, id);
    }

    @PostMapping("/cancel/{id}")
    public Result<?> cancel(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.cancelReservation(userId, id);
    }

    @PostMapping("/start/{id}")
    public Result<?> startUsage(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.startUsage(userId, id);
    }

    @PostMapping("/end/{id}")
    public Result<?> endUsage(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.endUsage(userId, id);
    }

    @PostMapping("/pay")
    public Result<?> pay(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = (Long) auth.getPrincipal();
        Long reservationId = Long.valueOf(body.get("reservationId").toString());
        String method = (String) body.getOrDefault("method", "ALIPAY");
        return reservationService.payBilling(userId, reservationId, method);
    }

    @PostMapping("/pay-callback")
    public Result<?> payCallback(@RequestParam String tradeNo) {
        return reservationService.completePayment(tradeNo);
    }

    @GetMapping("/usage-records")
    public Result<?> getUsageRecords(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.getUserUsageRecords(userId);
    }

    @PostMapping("/rating")
    public Result<?> submitRating(Authentication auth,
                                  @RequestParam Long deviceId,
                                  @RequestParam Integer rating,
                                  @RequestParam(required = false) String comment) {
        Long userId = (Long) auth.getPrincipal();
        return reservationService.submitRating(userId, deviceId, rating, comment);
    }

    @GetMapping("/rating/{deviceId}")
    public Result<?> getDeviceRatings(@PathVariable Long deviceId) {
        return reservationService.getDeviceRatings(deviceId);
    }
}
