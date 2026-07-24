package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ship")
public class Ship {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String shipName;
    private String shipType;
    private String mmsi;
    private String imo;
    private String nationality;
    private BigDecimal tonnage;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal draft;
    private BigDecimal ratedVoltage;
    private BigDecimal ratedPower;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
