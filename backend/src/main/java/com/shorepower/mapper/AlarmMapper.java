package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.entity.Alarm;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface AlarmMapper extends BaseMapper<Alarm> {

    @Select("SELECT a.*, d.device_name, u.real_name as handler_name " +
            "FROM alarm a LEFT JOIN device d ON a.device_id = d.id " +
            "LEFT JOIN sys_user u ON a.handler_id = u.id " +
            "ORDER BY a.alarm_time DESC")
    IPage<Alarm> selectAlarmPage(Page<Alarm> page);

    @Select("SELECT alarm_level as \"alarm_level\", COUNT(*) as \"cnt\" FROM alarm " +
            "WHERE status = 'PENDING' GROUP BY alarm_level")
    List<Map<String, Object>> countByLevel();

    @Select("SELECT COUNT(*) FROM alarm WHERE status = 'PENDING'")
    int countPending();
}
