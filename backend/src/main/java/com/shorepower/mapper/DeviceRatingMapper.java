package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.DeviceRating;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface DeviceRatingMapper extends BaseMapper<DeviceRating> {

    @Select("SELECT r.id, r.user_id, r.device_id, r.rating, r.comment, " +
            "DATE_FORMAT(r.create_time, '%Y-%m-%d %H:%i:%s') AS create_time, " +
            "d.device_name, d.device_code " +
            "FROM device_rating r LEFT JOIN device d ON r.device_id = d.id " +
            "WHERE r.user_id = #{userId} ORDER BY r.create_time DESC")
    List<Map<String, Object>> getUserRatings(Long userId);

    @Select("SELECT AVG(rating) as avg_rating, COUNT(*) as count " +
            "FROM device_rating WHERE device_id = #{deviceId}")
    Map<String, Object> getDeviceRatingStats(Long deviceId);
}