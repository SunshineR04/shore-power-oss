package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * 告警管理 API
 *
 * 功能：
 *   page           - 分页查询告警（支持按状态/等级/设备筛选）
 *   handle         - 处理告警（填写处置备注，更新告警状态）
 *   pending-count  - 待处理告警数量（用于前端角标提示）
 *   level-stats    - 各告警等级统计（用于饼图展示）
 */
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String level,
                          @RequestParam(required = false) Long deviceId) {
        return Result.ok(alarmService.page(pageNum, pageSize, status, level, deviceId));
    }

    @PutMapping("/handle/{id}")
    public Result<?> handle(@PathVariable Long id,
                            @RequestBody Map<String, String> body,
                            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        alarmService.handle(id, userId, body.get("status"), body.get("remark"));
        return Result.ok();
    }

    @GetMapping("/pending-count")
    public Result<?> pendingCount() {
        return Result.ok(alarmService.countPending());
    }

    @GetMapping("/level-stats")
    public Result<?> levelStats() {
        return Result.ok(alarmService.countByLevel());
    }
}
