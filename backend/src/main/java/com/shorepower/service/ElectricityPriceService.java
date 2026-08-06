package com.shorepower.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 分时电价计费服务
 *
 * 计费模型参考王孝坤等关于分时电价下岸电调度的研究成果[2]。
 *
 * 时段划分（可动态配置）：
 *   低谷时段(off-peak)：23:00~06:59，默认 0.45元/kWh
 *   高峰时段(peak)：08:00~10:59 + 18:00~20:59，默认 0.85元/kWh
 *   平段(mid-peak)：其余时段，默认 0.65元/kWh
 *
 * 计费方式：
 *   - 按小时逐段计算，各时段内的能耗按时间比例分配
 *   - 预计费用 = 额定功率 × 55%负载因子 × 时长 × 分时电价
 *   - 实际费用 = 实际能耗 × 分时电价（按各时段占比分配）
 *
 * 价格来源：sys_config 数据库表，运行中可动态修改
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectricityPriceService {

    private final SystemConfigService configService;
    private final Random random = new Random();

    private BigDecimal getConfigPrice(String key, double defaultVal) {
        return configService.getDecimalConfig(key, BigDecimal.valueOf(defaultVal));
    }

    @Cacheable(value = "prices", key = "'realtime'")
    public BigDecimal getRealTimePrice() {
        try {
            return getConfigPrice("electricity.price", 0.65);
        } catch (Exception e) {
            log.error("获取实时电价失败", e);
            return BigDecimal.valueOf(0.65);
        }
    }

    /** 获取预测日期的电价（考虑周末折扣和季节加价） */
    public BigDecimal getPredictedPrice(LocalDate date) {
        try {
            BigDecimal basePrice = getConfigPrice("electricity.price", 0.65);
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek >= 6) {
                basePrice = basePrice.multiply(BigDecimal.valueOf(0.9));
            }
            int month = date.getMonthValue();
            if (month >= 6 && month <= 8) {
                basePrice = basePrice.multiply(BigDecimal.valueOf(1.1));
            } else if (month >= 12 || month <= 2) {
                basePrice = basePrice.multiply(BigDecimal.valueOf(1.05));
            }
            double randomFactor = 0.95 + random.nextDouble() * 0.1;
            basePrice = basePrice.multiply(BigDecimal.valueOf(randomFactor));
            return basePrice.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("获取预测电价失败", e);
            return BigDecimal.valueOf(0.65);
        }
    }

    /** 获取指定时间段的平均电价（简化实现，直接返回基础电价） */
    public BigDecimal getAveragePrice(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return getConfigPrice("electricity.price", 0.65);
        } catch (Exception e) {
            log.error("获取平均电价失败", e);
            return BigDecimal.valueOf(0.65);
        }
    }

    /** 获取三个时段的电价（带缓存，Caffeine 缓存） */
    @Cacheable(value = "prices", key = "'tou'")
    public Map<String, BigDecimal> getTimeOfUsePrices() {
        Map<String, BigDecimal> prices = new HashMap<>();
        prices.put("off-peak", getConfigPrice("electricity.price.off_peak", 0.45));
        prices.put("mid-peak", getConfigPrice("electricity.price.mid_peak", 0.65));
        prices.put("peak", getConfigPrice("electricity.price.peak", 0.85));
        return prices;
    }

    /**
     * 根据小时判断所属时段
     *   低谷：23点~6点（含）
     *   高峰：8~10点、18~20点
     *   平段：其余时间（7点、11~17点、21~22点）
     */
    public String getTimePeriod(int hour) {
        if (hour >= 23 || hour < 7) return "off-peak";
        if (hour >= 8 && hour <= 10) return "peak";
        if (hour >= 18 && hour <= 20) return "peak";
        return "mid-peak";
    }

    /** 获取某小时的电价 */
    public BigDecimal getPriceByHour(int hour) {
        String period = getTimePeriod(hour);
        Map<String, BigDecimal> prices = getTimeOfUsePrices();
        return prices.getOrDefault(period, BigDecimal.valueOf(0.65));
    }

    /**
     * 分时电价计费核心算法
     *
     * 步骤：
     *   1. 按小时遍历使用时段
     *   2. 统计每个时段的秒数（峰/平/谷）
     *   3. 总能耗按各时段秒数比例分配
     *   4. 各时段电费 = 分配能耗 × 对应电价
     *   5. 合计 = 总电度电费
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param totalEnergy 总能耗（kWh）
     * @return 电度电费（不含服务费）
     */
    public BigDecimal calculateTimeOfUseCost(LocalDateTime startTime, LocalDateTime endTime, BigDecimal totalEnergy) {
        long totalSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        if (totalSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalMinutes = BigDecimal.valueOf(totalSeconds)
            .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);

        Map<String, BigDecimal> prices = getTimeOfUsePrices();
        Map<String, BigDecimal> periodSeconds = new HashMap<>();
        periodSeconds.put("peak", BigDecimal.ZERO);
        periodSeconds.put("mid-peak", BigDecimal.ZERO);
        periodSeconds.put("off-peak", BigDecimal.ZERO);

        // 按小时切分，统计每段各属于哪个电价时段
        LocalDateTime cursor = startTime;
        while (cursor.isBefore(endTime)) {
            LocalDateTime nextHour = cursor.withMinute(0).withSecond(0).withNano(0).plusHours(1);
            if (nextHour.isAfter(endTime)) {
                nextHour = endTime;
            }
            long secondsInSlot = java.time.Duration.between(cursor, nextHour).getSeconds();
            String period = getTimePeriod(cursor.getHour());
            periodSeconds.put(period, periodSeconds.get(period).add(BigDecimal.valueOf(secondsInSlot)));
            cursor = nextHour;
        }

        // 按比例分配能耗并计费
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : periodSeconds.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = entry.getValue()
                    .divide(BigDecimal.valueOf(totalSeconds), 6, RoundingMode.HALF_UP);
                BigDecimal energyInPeriod = totalEnergy.multiply(ratio);
                BigDecimal price = prices.getOrDefault(entry.getKey(), BigDecimal.valueOf(0.65));
                totalCost = totalCost.add(energyInPeriod.multiply(price));
            }
        }

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算预计费用（预约创建时使用）
     * 通过 55% 负载因子估算实际功率
     */
    public BigDecimal calculateEstimatedCost(LocalDateTime startTime, LocalDateTime endTime, BigDecimal ratedPower) {
        BigDecimal loadFactor = BigDecimal.valueOf(0.55);
        BigDecimal effectivePower = ratedPower != null ? ratedPower : BigDecimal.valueOf(60);
        effectivePower = effectivePower.multiply(loadFactor);

        long totalSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        if (totalSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalEnergy = effectivePower
            .multiply(BigDecimal.valueOf(totalSeconds))
            .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);

        return calculateTimeOfUseCost(startTime, endTime, totalEnergy);
    }
}
