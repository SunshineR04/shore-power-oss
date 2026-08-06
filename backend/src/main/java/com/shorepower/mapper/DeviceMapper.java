package com.shorepower.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shorepower.entity.Device;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 行级锁查询设备：用于预约创建等需要互斥的设备场景（SELECT ... FOR UPDATE）。
     * 调用方必须在事务内执行，锁在事务提交/回滚时释放。
     */
    @Select("SELECT * FROM device WHERE id = #{id} FOR UPDATE")
    Device selectByIdForUpdate(@Param("id") Long id);
}
