package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.mapper.EnergyStatMapper;
import com.shorepower.service.EnergyOptimizationService;
import com.shorepower.service.ElectricityPriceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 能耗分析 API
 *
 * 功能：
 *   trend             - 能耗趋势（日/周/月聚合）
 *   by-device         - 各设备能耗排行
 *   comparison        - 本期 vs 上期对比
 *   environmental-metrics - 环保指标（CO₂减排/节煤/植树）
 *   analyze/{id}      - 单设备能耗模式分析
 *   load-balancing    - 负载均衡建议
 *   predict/{id}      - 能耗预测（Holt-Winters）
 *   time-of-use-prices - 分时电价查询
 *
 * 数据源：usage_record（优先）→ energy_consumption → device_data（兜底）
 * 周/月聚合通过 aggregateByPeriod 将每日数据按 ISO 周或月汇总
 */
@RestController
@RequestMapping("/api/energy")
@Validated
@RequiredArgsConstructor
public class EnergyController {

    private final EnergyStatMapper energyStatMapper;
    private final EnergyOptimizationService energyOptimizationService;
    private final ElectricityPriceService electricityPriceService;

    @GetMapping("/trend")
    public Result<?> trend(@RequestParam String statType,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new IllegalArgumentException("查询范围不能超过一年");
        }
        List<Map<String, Object>> result = energyStatMapper.getUsageRecordTrend(startDate.toString(), endDate.toString());
        if ("WEEKLY".equals(statType) || "MONTHLY".equals(statType)) {
            result = aggregateByPeriod(result, statType);
        }
        return Result.ok(result);
    }

    @GetMapping("/by-device")
    public Result<?> byDevice(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        return Result.ok(energyStatMapper.getUsageRecordByDevice(startDate.toString(), endDate.toString()));
    }

    @GetMapping("/comparison")
    public Result<?> comparison(@RequestParam String statType,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LocalDate prevStart = startDate.minusDays(days);
        LocalDate prevEnd = startDate.minusDays(1);

        List<Map<String, Object>> current = energyStatMapper.getUsageRecordTrend(startDate.toString(), endDate.toString());
        List<Map<String, Object>> previous = energyStatMapper.getUsageRecordTrend(prevStart.toString(), prevEnd.toString());

        if ("WEEKLY".equals(statType) || "MONTHLY".equals(statType)) {
            current = aggregateByPeriod(current, statType);
            previous = aggregateByPeriod(previous, statType);
        }

        return Result.ok(Map.of(
            "current", current,
            "previous", previous,
            "previousPeriod", Map.of("startDate", prevStart.toString(), "endDate", prevEnd.toString())
        ));
    }

    @GetMapping("/environmental-metrics")
    public Result<?> environmentalMetrics(@RequestParam(defaultValue = "0") String totalEnergy) {
        BigDecimal energy;
        try {
            energy = new BigDecimal(totalEnergy);
        } catch (Exception e) {
            energy = BigDecimal.valueOf(10000);
        }
        if (energy.compareTo(BigDecimal.ZERO) <= 0) {
            energy = BigDecimal.valueOf(10000);
        }
        return Result.ok(energyOptimizationService.calculateEnvironmentalMetrics(energy));
    }

    @GetMapping("/analyze/{deviceId}")
    public Result<?> analyze(@PathVariable Long deviceId,
                             @RequestParam(defaultValue = "7") @Min(1) @Max(31) int days) {
        return Result.ok(energyOptimizationService.analyzeEnergyPatterns(deviceId, days));
    }

    @GetMapping("/load-balancing")
    public Result<?> loadBalancing() {
        return Result.ok(energyOptimizationService.generateLoadBalancingSuggestions());
    }

    @GetMapping("/predict/{deviceId}")
    public Result<?> predict(@PathVariable Long deviceId,
                             @RequestParam(defaultValue = "7") @Min(1) @Max(31) int days) {
        return Result.ok(energyOptimizationService.predictEnergyConsumption(deviceId, days));
    }

    @GetMapping("/time-of-use-prices")
    public Result<?> timeOfUsePrices() {
        return Result.ok(electricityPriceService.getTimeOfUsePrices());
    }

    @GetMapping("/real-time-price")
    public Result<?> realTimePrice() {
        return Result.ok(Map.of("price", electricityPriceService.getRealTimePrice()));
    }

    private List<Map<String, Object>> aggregateByPeriod(List<Map<String, Object>> dailyData, String statType) {
        if (dailyData == null || dailyData.isEmpty()) return new ArrayList<>();

        LinkedHashMap<String, Map<String, Object>> periodMap = new LinkedHashMap<>();

        for (Map<String, Object> day : dailyData) {
            Object statDateObj = day.get("statDate");
            if (statDateObj == null) continue;
            String dateStr = statDateObj.toString();

            String periodKey;
            if ("WEEKLY".equals(statType)) {
                LocalDate date = LocalDate.parse(dateStr);
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int year = date.get(IsoFields.WEEK_BASED_YEAR);
                periodKey = year + "-W" + String.format("%02d", week);
            } else {
                periodKey = dateStr.substring(0, 7);
            }

            periodMap.computeIfAbsent(periodKey, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("statDate", k);
                m.put("totalEnergy", BigDecimal.ZERO);
                m.put("totalCost", BigDecimal.ZERO);
                return m;
            });

            Map<String, Object> period = periodMap.get(periodKey);
            BigDecimal energy = (BigDecimal) period.get("totalEnergy");
            BigDecimal cost = (BigDecimal) period.get("totalCost");
            Object dayEnergyObj = day.get("totalEnergy");
            Object dayCostObj = day.get("totalCost");
            if (dayEnergyObj instanceof BigDecimal) energy = energy.add((BigDecimal) dayEnergyObj);
            if (dayCostObj instanceof BigDecimal) cost = cost.add((BigDecimal) dayCostObj);
            period.put("totalEnergy", energy);
            period.put("totalCost", cost);
        }

        return new ArrayList<>(periodMap.values());
    }
}
