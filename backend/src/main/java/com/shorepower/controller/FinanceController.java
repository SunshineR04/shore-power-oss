package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    public Result<?> getSummary() {
        return Result.ok(financeService.getSummary());
    }

    @GetMapping("/device-ranking")
    public Result<?> getDeviceRevenueRanking() {
        return Result.ok(financeService.getDeviceRevenueRanking());
    }

    @GetMapping("/user-ranking")
    public Result<?> getUserSpendingRanking() {
        return Result.ok(financeService.getUserSpendingRanking());
    }

    @GetMapping("/daily-trend")
    public Result<?> getDailyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(financeService.getDailyTrend(startDate, endDate));
    }
}
