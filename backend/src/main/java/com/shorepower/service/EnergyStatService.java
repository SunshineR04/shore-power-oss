package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shorepower.entity.Device;
import com.shorepower.entity.DeviceData;
import com.shorepower.entity.EnergyConsumption;
import com.shorepower.entity.EnergyStat;
import com.shorepower.mapper.DeviceDataMapper;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.EnergyConsumptionMapper;
import com.shorepower.mapper.EnergyStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * 能耗统计服务
 *
 * 职责：
 *   1. 应用启动时初始化30天历史能耗数据（从 device_data 或模拟生成）
 *   2. 每5分钟聚合当日设备数据到 energy_stat 表（upsert 策略）
 *
 * 注意：当前系统以 usage_record 为能耗数据主源（见 AGENTS.md），
 * energy_stat 和 EnergyStatService 的作用是为前端提供兜底的聚合数据。
 * 前端能耗分析主要调 EnergyOptimizationService（从 usage_record 取数据）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyStatService {

    private final DeviceMapper deviceMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final EnergyConsumptionMapper energyConsumptionMapper;
    private final EnergyStatMapper energyStatMapper;
    private final SystemConfigService configService;
    private final ElectricityPriceService electricityPriceService;

    /**
     * 应用启动时初始化历史能耗数据（仅 dev 环境）
     *
     * @EventListener(ApplicationReadyEvent.class) 在 Spring 容器完全启动后执行一次
     * 如果 energy_stat 表已存在数据则跳过，否则为每个设备生成最近31天的能耗记录
     * 已有真实 device_data 的设备使用真实数据，无数据的设备用模拟数据填充
     * 生产环境（prod profile）不执行：模拟数据会污染真实统计。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Profile("dev")
    public void initHistoricalData() {
        Long count = energyStatMapper.selectCount(null);
        if (count != null && count > 0) {
            log.info("[EnergyStat] Historical data already exists ({} records), skipping init", count);
            return;
        }

        log.info("[EnergyStat] Generating 30-day historical energy data from device_data...");
        List<Device> devices = deviceMapper.selectList(null);
        LocalDate today = LocalDate.now();

        for (int dayOffset = 30; dayOffset >= 0; dayOffset--) {
            LocalDate date = today.minusDays(dayOffset);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);

            for (Device dev : devices) {
                List<DeviceData> dataList = deviceDataMapper.selectList(
                    new LambdaQueryWrapper<DeviceData>()
                        .eq(DeviceData::getDeviceId, dev.getId())
                        .between(DeviceData::getCollectTime, dayStart, dayEnd)
                );

                BigDecimal totalEnergy = BigDecimal.ZERO;
                BigDecimal totalCost = BigDecimal.ZERO;
                BigDecimal maxPower = BigDecimal.ZERO;
                BigDecimal totalPower = BigDecimal.ZERO;

                for (DeviceData d : dataList) {
                    if (d.getEnergyConsumption() != null) totalEnergy = totalEnergy.add(d.getEnergyConsumption());
                    if (d.getEnergyCost() != null) totalCost = totalCost.add(d.getEnergyCost());
                    if (d.getPower() != null && d.getPower().compareTo(maxPower) > 0) maxPower = d.getPower();
                    if (d.getPower() != null) totalPower = totalPower.add(d.getPower());
                }

                if (totalEnergy.compareTo(BigDecimal.ZERO) == 0 && dataList.isEmpty()) {
                    BigDecimal ratedPower = dev.getRatedPower() != null ? dev.getRatedPower() : BigDecimal.valueOf(100);
                    totalEnergy = ratedPower.multiply(BigDecimal.valueOf(8 + Math.random() * 8)).setScale(2, RoundingMode.HALF_UP);
                    totalCost = electricityPriceService.calculateTimeOfUseCost(dayStart, dayEnd, totalEnergy);
                    maxPower = ratedPower;
                    totalPower = ratedPower;
                }

                BigDecimal avgPower = totalPower;
                if (!dataList.isEmpty()) {
                    avgPower = totalPower.divide(BigDecimal.valueOf(dataList.size()), 2, RoundingMode.HALF_UP);
                }

                BigDecimal runningHours = BigDecimal.valueOf(dataList.size() * 10.0 / 3600.0).setScale(2, RoundingMode.HALF_UP);
                if (dataList.isEmpty()) {
                    runningHours = BigDecimal.valueOf(8 + Math.random() * 8).setScale(2, RoundingMode.HALF_UP);
                }

                EnergyStat stat = new EnergyStat();
                stat.setDeviceId(dev.getId());
                stat.setStatDate(date);
                stat.setStatType("DAILY");
                stat.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
                stat.setPeakPower(maxPower.setScale(2, RoundingMode.HALF_UP));
                stat.setAvgPower(avgPower.setScale(2, RoundingMode.HALF_UP));
                stat.setRunningHours(runningHours);
                stat.setEnergyCost(totalCost.setScale(2, RoundingMode.HALF_UP));
                energyStatMapper.insert(stat);
            }
        }
        log.info("[EnergyStat] Generated {} daily records from device_data for {} devices", 31 * devices.size(), devices.size());
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void aggregateEnergy() {
        List<Device> devices = deviceMapper.selectList(null);
        if (devices.isEmpty()) return;

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();

        for (Device dev : devices) {
            List<DeviceData> todayData = deviceDataMapper.selectList(
                new LambdaQueryWrapper<DeviceData>()
                    .eq(DeviceData::getDeviceId, dev.getId())
                    .ge(DeviceData::getCollectTime, dayStart)
            );
            if (todayData.isEmpty()) continue;

            BigDecimal totalEnergy = todayData.stream()
                .map(DeviceData::getEnergyConsumption)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCost = electricityPriceService.calculateTimeOfUseCost(dayStart, LocalDateTime.now(), totalEnergy);
            BigDecimal maxPower = todayData.stream()
                .map(DeviceData::getPower)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            BigDecimal totalPower = todayData.stream()
                .map(DeviceData::getPower)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal runningHours = BigDecimal.valueOf(todayData.size() * 10.0 / 3600.0).setScale(2, RoundingMode.HALF_UP);

            LambdaQueryWrapper<EnergyStat> wrapper = new LambdaQueryWrapper<EnergyStat>()
                .eq(EnergyStat::getDeviceId, dev.getId())
                .eq(EnergyStat::getStatDate, today)
                .eq(EnergyStat::getStatType, "DAILY");
            EnergyStat existing = energyStatMapper.selectOne(wrapper);

            if (existing != null) {
                existing.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
                existing.setEnergyCost(totalCost.setScale(2, RoundingMode.HALF_UP));
                existing.setPeakPower(maxPower.setScale(2, RoundingMode.HALF_UP));
                existing.setAvgPower(totalPower.divide(BigDecimal.valueOf(todayData.size()), 2, RoundingMode.HALF_UP));
                existing.setRunningHours(runningHours);
                energyStatMapper.updateById(existing);
            } else {
                EnergyStat stat = new EnergyStat();
                stat.setDeviceId(dev.getId());
                stat.setStatDate(today);
                stat.setStatType("DAILY");
                stat.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
                stat.setEnergyCost(totalCost.setScale(2, RoundingMode.HALF_UP));
                stat.setPeakPower(maxPower.setScale(2, RoundingMode.HALF_UP));
                stat.setAvgPower(totalPower.divide(BigDecimal.valueOf(todayData.size()), 2, RoundingMode.HALF_UP));
                stat.setRunningHours(runningHours);
                energyStatMapper.insert(stat);
            }
        }
    }
}
