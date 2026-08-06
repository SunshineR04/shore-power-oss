package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_rating")
public class DeviceRating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deviceId;
    private Integer rating;
    private String comment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
