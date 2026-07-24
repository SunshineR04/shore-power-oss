package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alarm")
public class Alarm {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String alarmType;
    private String alarmLevel;
    private String alarmContent;
    private String alarmValue;
    private String thresholdValue;
    private String status;
    private Long handlerId;
    private LocalDateTime handleTime;
    private String handleRemark;
    private LocalDateTime alarmTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String handlerName;
}
