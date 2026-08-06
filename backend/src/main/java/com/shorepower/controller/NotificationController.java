package com.shorepower.controller;

import com.shorepower.common.Result;
import com.shorepower.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知管理 API
 *
 * 功能：
 *   list          - 获取用户的通知列表（分页）
 *   unread-count  - 未读通知数量（用于前端角标）
 *   read/{id}     - 标记单条通知为已读
 *   read-all      - 全部标记已读
 *
 * 通知由后端主动创建（维护任务指派时由 MaintenanceService 调用 NotificationService.create()）
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<?> list(Authentication auth,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(notificationService.listByUser(userId, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(notificationService.countUnread(userId));
    }

    @PutMapping("/read/{id}")
    public Result<?> markRead(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markRead(userId, id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<?> markAllRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAllRead(userId);
        return Result.ok();
    }
}
