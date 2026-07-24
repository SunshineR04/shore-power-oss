package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.EnergyStat;

import java.util.List;
import java.util.Map;

public interface EnergyStatMapper extends BaseMapper<EnergyStat> {

    List<Map<String, Object>> getEnergyTrend(String statType, String startDate, String endDate);

    List<Map<String, Object>> getEnergyByDevice(String startDate, String endDate);

    List<Map<String, Object>> getComparisonTrend(String statType, String startDate, String endDate);

    List<Map<String, Object>> getUsageRecordTrend(String startDate, String endDate);

    List<Map<String, Object>> getUsageRecordByDevice(String startDate, String endDate);

    List<Map<String, Object>> getTodayTrend();

    List<Map<String, Object>> getTodayByDevice();
}
