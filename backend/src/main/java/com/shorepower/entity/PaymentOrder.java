package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reservationId;
    private Long userId;
    private BigDecimal amount;
    private String method;
    private String qrCodeUrl;
    private String tradeNo;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
