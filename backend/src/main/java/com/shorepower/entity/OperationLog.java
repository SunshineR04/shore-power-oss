package com.shorepower.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long operatorId;
    private String operationType;
    private String operationContent;
    private String operationResult;
    private String ipAddress;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String operatorName;
}
