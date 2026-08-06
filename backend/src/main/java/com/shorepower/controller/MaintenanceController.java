package com.shorepower.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shorepower.common.Result;
import com.shorepower.entity.MaintenanceTask;
import com.shorepower.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * 维护调度 API
 *
 * 权限控制：
 *   ADMIN  - 创建/分配/删除/取消任务
 *   OPERATOR - 查看自己的任务(my-tasks)、开始/完成任务
 *   任意认证用户 - 查看全部任务(page)、更新任务(update)
 *
 * 注意：add 和 update 使用 rawBody + ObjectMapper 手动解析 JSON，
 * 而不是直接 @RequestBody MaintenanceTask，是因为前端传入了额外的字段，
 * 手动解析可以 better handle 异常。
 */
@Slf4j
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final ObjectMapper objectMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<?> page(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String priority) {
        return Result.ok(maintenanceService.page(pageNum, pageSize, status, priority));
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<?> myTasks(Authentication auth,
                             @RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String priority) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(maintenanceService.pageByAssignee(userId, pageNum, pageSize, status, priority));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> add(@RequestBody String rawBody) {
        try {
            log.info("创建任务请求体: {}", rawBody);
            MaintenanceTask task = objectMapper.readValue(rawBody, MaintenanceTask.class);
            maintenanceService.create(task);
            return Result.ok();
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.fail("创建失败，请检查输入数据");
        }
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<?> update(Authentication auth, @RequestBody String rawBody) {
        try {
            Long userId = (Long) auth.getPrincipal();
            MaintenanceTask task = objectMapper.readValue(rawBody, MaintenanceTask.class);
            maintenanceService.update(userId, task);
            return Result.ok();
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return Result.fail("更新失败，请检查输入数据");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        maintenanceService.delete(id);
        return Result.ok();
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> assign(Authentication auth, @RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        Long assigneeId = Long.valueOf(body.get("assigneeId").toString());
        Long operatorId = (Long) auth.getPrincipal();
        maintenanceService.assign(taskId, assigneeId, operatorId);
        return Result.ok();
    }

    @PutMapping("/start/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<?> start(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        maintenanceService.start(id, userId);
        return Result.ok();
    }

    @PutMapping("/complete")
    @PreAuthorize("hasRole('OPERATOR')")
    public Result<?> complete(Authentication auth, @RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String remark = (String) body.getOrDefault("remark", "");
        Long userId = (Long) auth.getPrincipal();
        maintenanceService.complete(taskId, userId, remark);
        return Result.ok();
    }

    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> cancel(@PathVariable Long id) {
        maintenanceService.cancel(id);
        return Result.ok();
    }
}
