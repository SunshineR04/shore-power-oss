package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("device_data")
public class DeviceData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private BigDecimal voltage;
    private BigDecimal currentVal;
    private BigDecimal power;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal powerFactor;
    private BigDecimal frequency;
    private BigDecimal energyConsumption;
    private BigDecimal energyCost;
    private LocalDateTime collectTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
