package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.Ship;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface ShipMapper extends BaseMapper<Ship> {

    @Select("SELECT s.id AS \"id\", s.ship_name AS \"shipName\", s.ship_type AS \"shipType\", " +
            "s.mmsi AS \"mmsi\", s.imo AS \"imo\", s.nationality AS \"nationality\", " +
            "s.tonnage AS \"tonnage\", s.length AS \"length\", s.width AS \"width\", " +
            "s.draft AS \"draft\", s.status AS \"status\", s.remark AS \"remark\", " +
            "DATE_FORMAT(s.create_time, '%Y-%m-%d %H:%i:%s') AS \"createTime\", DATE_FORMAT(s.update_time, '%Y-%m-%d %H:%i:%s') AS \"updateTime\", " +
            "s.user_id AS \"userId\" " +
            "FROM ship s WHERE s.user_id = #{userId} ORDER BY s.create_time DESC")
    List<Map<String, Object>> getUserShips(Long userId);

    @Select("SELECT s.id AS \"id\", s.ship_name AS \"shipName\", s.ship_type AS \"shipType\", " +
            "s.mmsi AS \"mmsi\", s.imo AS \"imo\", s.nationality AS \"nationality\", " +
            "s.tonnage AS \"tonnage\", s.length AS \"length\", s.width AS \"width\", " +
            "s.draft AS \"draft\", s.status AS \"status\", s.remark AS \"remark\", " +
            "DATE_FORMAT(s.create_time, '%Y-%m-%d %H:%i:%s') AS \"createTime\", DATE_FORMAT(s.update_time, '%Y-%m-%d %H:%i:%s') AS \"updateTime\", " +
            "s.user_id AS \"userId\" " +
            "FROM ship s WHERE s.id = #{id}")
    Map<String, Object> getShipDetail(Long id);
}
