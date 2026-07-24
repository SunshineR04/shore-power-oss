package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.UsageRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UsageRecordMapper extends BaseMapper<UsageRecord> {

    @Select("SELECT u.id AS \"id\", u.reservation_id AS \"reservationId\", u.user_id AS \"userId\", " +
            "u.device_id AS \"deviceId\", DATE_FORMAT(u.start_time, '%Y-%m-%d %H:%i:%s') AS \"startTime\", DATE_FORMAT(u.end_time, '%Y-%m-%d %H:%i:%s') AS \"endTime\", " +
            "u.total_energy AS \"totalEnergy\", u.total_cost AS \"totalCost\", " +
            "u.rating AS \"rating\", u.comment AS \"comment\", DATE_FORMAT(u.create_time, '%Y-%m-%d %H:%i:%s') AS \"createTime\", " +
            "d.device_name AS \"deviceName\", d.device_code AS \"deviceCode\" " +
            "FROM usage_record u LEFT JOIN device d ON u.device_id = d.id " +
            "INNER JOIN reservation r ON u.reservation_id = r.id AND r.status = 'COMPLETED' " +
            "WHERE u.user_id = #{userId} ORDER BY u.create_time DESC")
    List<Map<String, Object>> getUserUsageRecords(Long userId);

    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM usage_record WHERE start_time >= #{start} AND start_time < #{end}")
    BigDecimal sumCostByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM usage_record WHERE start_time >= #{start} AND start_time < #{end}")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT device_id AS \"deviceId\", COALESCE(SUM(total_cost), 0) AS \"totalCost\", COUNT(*) AS \"cnt\" " +
            "FROM usage_record WHERE total_cost IS NOT NULL GROUP BY device_id")
    List<Map<String, Object>> revenueGroupByDevice();

    @Select("SELECT user_id AS \"userId\", COALESCE(SUM(total_cost), 0) AS \"totalCost\", COUNT(*) AS \"cnt\" " +
            "FROM usage_record WHERE total_cost IS NOT NULL GROUP BY user_id")
    List<Map<String, Object>> spendingGroupByUser();

    @Select("SELECT DATE(start_time) AS \"date\", COALESCE(SUM(total_cost), 0) AS \"spent\", COUNT(*) AS \"usageCount\" " +
            "FROM usage_record WHERE start_time >= #{start} AND start_time < #{end} GROUP BY DATE(start_time) ORDER BY DATE(start_time)")
    List<Map<String, Object>> dailyTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
