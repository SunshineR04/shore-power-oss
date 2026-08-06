package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("energy_consumption")
public class EnergyConsumption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private LocalDate statDate;
    private BigDecimal totalEnergy;
    private BigDecimal peakPower;
    private BigDecimal avgPower;
    private BigDecimal runningHours;
    private BigDecimal energyCost;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
