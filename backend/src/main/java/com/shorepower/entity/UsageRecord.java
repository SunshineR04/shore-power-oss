package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("usage_record")
public class UsageRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reservationId;
    private Long userId;
    private Long deviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalEnergy;
    private BigDecimal totalCost;
    private Integer rating;
    private String comment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
