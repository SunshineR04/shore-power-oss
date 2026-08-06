package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.EnergyConsumption;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EnergyConsumptionMapper extends BaseMapper<EnergyConsumption> {

    @Select("SELECT device_id, SUM(total_energy) as total_energy, SUM(energy_cost) as total_cost " +
            "FROM energy_consumption " +
            "WHERE stat_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY device_id")
    List<Map<String, Object>> getEnergySummary(LocalDate startDate, LocalDate endDate);

    @Select("SELECT stat_date, AVG(total_energy) as avg_energy, AVG(energy_cost) as avg_cost " +
            "FROM energy_consumption " +
            "WHERE device_id = #{deviceId} AND stat_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY stat_date ORDER BY stat_date")
    List<Map<String, Object>> getDeviceEnergyTrend(Long deviceId, LocalDate startDate, LocalDate endDate);

    @Select("SELECT * FROM energy_consumption " +
            "WHERE device_id = #{deviceId} AND stat_date = #{date}")
    EnergyConsumption getByDeviceAndDate(Long deviceId, LocalDate date);
}
