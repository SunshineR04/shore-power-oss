package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.common.BusinessException;
import com.shorepower.entity.MaintenanceTask;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.DeviceMapper;
import com.shorepower.mapper.MaintenanceTaskMapper;
import com.shorepower.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * 维护调度服务
 *
 * 管理维护任务的完整生命周期：
 *   PENDING(待处理) ──assign──→ ASSIGNED(已指派) ──start──→ IN_PROGRESS(执行中) ──complete──→ COMPLETED(已完成)
 *      │
 *      └──cancel──→ CANCELLED(已取消)
 *
 * 核心特性：
 *   - 显式白名单状态转换校验（非法转换直接抛异常）
 *   - 双通道通知：DB持久化(NotificationService) + WebSocket实时推送
 *   - 只有任务指派人才能 start/complete，只有 ADMIN 能 cancel
 *   - 任务创建时可自动指派（如果传了 assigneeId）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceTaskMapper taskMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final DeviceMapper deviceMapper;
    private final SysUserMapper userMapper;
    private final SimpMessagingTemplate ws;

    /** 显式状态转换白名单，非法转换直接拒绝 */
    private static final Set<String> VALID_TRANSITIONS = Set.of(
            "PENDING->ASSIGNED", "PENDING->CANCELLED",
            "ASSIGNED->IN_PROGRESS",
            "IN_PROGRESS->COMPLETED"
    );

    private void validateTransition(String current, String target) {
        String key = current + "->" + target;
        if (!VALID_TRANSITIONS.contains(key)) {
            throw new RuntimeException("非法状态转换: " + current + " → " + target);
        }
    }

    /**
     * 分页查询全部任务（管理员端）
     *
     * @param status 可选的状态筛选（PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CANCELLED）
     * @param priority 可选的优先级筛选（LOW/MEDIUM/HIGH/URGENT）
     * @return 分页结果，每条记录附带 assigneeName 和 deviceName（通过 enrichTask 填充）
     */
    public Page<MaintenanceTask> page(int pageNum, int pageSize, String status, String priority) {
        LambdaQueryWrapper<MaintenanceTask> wrapper = new LambdaQueryWrapper<>();
        // 可选筛选条件：状态、优先级
        if (StringUtils.hasText(status)) wrapper.eq(MaintenanceTask::getStatus, status);
        if (StringUtils.hasText(priority)) wrapper.eq(MaintenanceTask::getPriority, priority);
        wrapper.orderByDesc(MaintenanceTask::getCreateTime);  // 最新创建的排最前
        Page<MaintenanceTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 填充额外显示字段（指派人员姓名、关联设备名称）
        enrichTasks(page.getRecords());
        return page;
    }

    /**
     * 分页查询指派给当前运维人员的任务（运维人员端）
     *
     * @param userId 当前登录的运维人员ID（从JWT中提取）
     */
    public Page<MaintenanceTask> pageByAssignee(Long userId, int pageNum, int pageSize, String status, String priority) {
        LambdaQueryWrapper<MaintenanceTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaintenanceTask::getAssigneeId, userId);  // 只查指派给当前用户的任务
        if (StringUtils.hasText(status)) wrapper.eq(MaintenanceTask::getStatus, status);
        if (StringUtils.hasText(priority)) wrapper.eq(MaintenanceTask::getPriority, priority);
        wrapper.orderByDesc(MaintenanceTask::getCreateTime);
        Page<MaintenanceTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        enrichTasks(page.getRecords());
        return page;
    }

    /**
     * 创建维护任务（初始状态为 PENDING）
     *
     * 如果创建时指定了 assigneeId 且用户存在，则自动执行指派：
     *   - 状态直接提升为 ASSIGNED
     *   - 在 Notification 表中创建持久化通知
     *   - 通过 WebSocket 推送到 /topic/maintenance-assigned/{assigneeId}
     *
     * @Transactional 保证创建和通知的原子性
     */
    @Transactional
    public void create(MaintenanceTask task) {
        try {
            // 初始状态设为 PENDING（待处理）
            task.setStatus("PENDING");
            taskMapper.insert(task);

            // 如果创建时直接指定了指派人，则自动完成指派流程
            if (task.getAssigneeId() != null) {
                SysUser assignee = userService.getById(task.getAssigneeId());
                if (assignee != null) {
                    // 状态自动升级为 ASSIGNED（已指派）
                    task.setStatus("ASSIGNED");
                    taskMapper.updateById(task);
                    // 双通道通知：① 写入 notification 表
                    String title = "新维护任务: " + task.getTaskTitle();
                    String content = "管理员指派了一项任务给您，请及时处理";
                    notificationService.create(task.getAssigneeId(), title, content, "MAINTENANCE", task.getId());
                    // ② WebSocket 推送到该运维人员专属频道
                    ws.convertAndSend("/topic/maintenance-assigned/" + task.getAssigneeId(), Map.of(
                            "taskId", task.getId(), "title", task.getTaskTitle(), "status", "ASSIGNED"
                    ));
                }
            }
        } catch (Exception e) {
            log.error("创建维护任务失败", e);
            throw new BusinessException("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务信息（仅 PENDING 和 ASSIGNED 状态可编辑）
     * 权限：ADMIN 可编辑任意任务；OPERATOR 仅可编辑指派给自己的任务，
     * 且不可修改 assigneeId（防改派越权）。
     */
    @Transactional
    public void update(Long operatorId, MaintenanceTask task) {
        try {
            MaintenanceTask existing = taskMapper.selectById(task.getId());
            if (existing == null) throw new BusinessException("任务不存在");
            // 状态校验：只有 PENDING 和 ASSIGNED 可编辑
            if (!"PENDING".equals(existing.getStatus()) && !"ASSIGNED".equals(existing.getStatus())) {
                throw new BusinessException("仅待处理或已指派状态的任务可编辑");
            }
            // 权限校验：OPERATOR 只能编辑指派给自己的任务
            SysUser operator = userService.getById(operatorId);
            boolean isAdmin = operator != null && "ADMIN".equals(operator.getRole());
            if (!isAdmin && !operatorId.equals(existing.getAssigneeId())) {
                throw new BusinessException("仅可编辑指派给自己的任务");
            }
            // 逐个字段更新（不改变原有字段以外的值）
            existing.setTaskTitle(task.getTaskTitle());
            existing.setTaskType(task.getTaskType());
            existing.setPriority(task.getPriority());
            existing.setDeviceId(task.getDeviceId());
            existing.setTaskContent(task.getTaskContent());
            existing.setPlanStartTime(task.getPlanStartTime());
            existing.setPlanEndTime(task.getPlanEndTime());
            // OPERATOR 不可修改指派人；ADMIN 可改
            if (isAdmin) {
                existing.setAssigneeId(task.getAssigneeId());
            }
            taskMapper.updateById(existing);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新维护任务失败", e);
            throw new BusinessException("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务（仅 PENDING 或 CANCELLED 状态可删除）
     * ASSIGNED/IN_PROGRESS/COMPLETED 状态不允许删除（防止数据丢失）
     */
    public void delete(Long id) {
        MaintenanceTask task = taskMapper.selectById(id);
        if (task == null) return;
        if (!"PENDING".equals(task.getStatus()) && !"CANCELLED".equals(task.getStatus())) {
            throw new BusinessException("仅待处理或已取消状态的任务可删除");
        }
        taskMapper.deleteById(id);
    }

    /**
     * 指派任务：PENDING → ASSIGNED
     *
     * 双通道通知：
     *   1. NotificationService.create() → 写入 notification 表（持久化，支持历史查询）
     *   2. SimpMessagingTemplate → WebSocket 推送 /topic/maintenance-assigned/{assigneeId}
     *
     * @param taskId 任务ID
     * @param assigneeId 被指派的运维人员ID
     * @param operatorId 操作的管理员ID（用于通知内容显示操作人姓名）
     */
    @Transactional
    public void assign(Long taskId, Long assigneeId, Long operatorId) {
        MaintenanceTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        validateTransition(task.getStatus(), "ASSIGNED");  // 必须是 PENDING → ASSIGNED

        SysUser operator = userService.getById(operatorId);
        SysUser assignee = userService.getById(assigneeId);
        if (assignee == null) throw new BusinessException("指派人不存在");

        task.setAssigneeId(assigneeId);
        task.setStatus("ASSIGNED");
        taskMapper.updateById(task);

        // 通道1：持久化通知
        String title = "新维护任务: " + task.getTaskTitle();
        String content = (operator != null ? operator.getRealName() : "管理员") + " 指派了一项任务给您，请及时处理";
        notificationService.create(assigneeId, title, content, "MAINTENANCE", taskId);
        // 通道2：WebSocket 实时推送
        ws.convertAndSend("/topic/maintenance-assigned/" + assigneeId, Map.of(
                "taskId", taskId, "title", task.getTaskTitle(), "status", "ASSIGNED"
        ));
    }

    /**
     * 开始执行任务：ASSIGNED → IN_PROGRESS
     * 校验：只有被指派的运维人员才能开始任务
     * 记录 actualStartTime（实际开始时间）
     */
    @Transactional
    public void start(Long taskId, Long userId) {
        MaintenanceTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        validateTransition(task.getStatus(), "IN_PROGRESS");
        // 权限校验：只有指派人可以开始
        if (!userId.equals(task.getAssigneeId())) {
            throw new BusinessException("仅指派人可开始任务");
        }

        task.setStatus("IN_PROGRESS");
        task.setActualStartTime(LocalDateTime.now());  // 记录任务开始执行的时间
        taskMapper.updateById(task);
    }

    /**
     * 完成任务：IN_PROGRESS → COMPLETED
     * 校验：只有被指派的运维人员才能完成任务
     * 记录 actualEndTime 和 completionRemark（处理备注，如"已更换故障模块"）
     */
    @Transactional
    public void complete(Long taskId, Long userId, String remark) {
        MaintenanceTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        validateTransition(task.getStatus(), "COMPLETED");
        if (!userId.equals(task.getAssigneeId())) {
            throw new BusinessException("仅指派人可完成任务");
        }

        task.setStatus("COMPLETED");
        task.setActualEndTime(LocalDateTime.now());
        task.setCompletionRemark(remark);  // 填写处理备注，便于后续追溯
        taskMapper.updateById(task);
    }

    /**
     * 取消任务：PENDING → CANCELLED
     * 只有管理员有权限调用此接口（Controller 层通过 @PreAuthorize("hasRole('ADMIN')") 控制）
     */
    @Transactional
    public void cancel(Long taskId) {
        MaintenanceTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        validateTransition(task.getStatus(), "CANCELLED");
        task.setStatus("CANCELLED");
        taskMapper.updateById(task);
    }

    /**
     * 批量填充额外显示字段
     *
     * MaintenanceTask 实体中 assigneeName 和 deviceName 用 @TableField(exist = false) 标记，
     * 不存入数据库，仅在查询时通过关联表动态填充，避免数据冗余。
     */
    private void enrichTasks(List<MaintenanceTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return;

        Set<Long> assigneeIds = new HashSet<>();
        Set<Long> deviceIds = new HashSet<>();
        for (MaintenanceTask t : tasks) {
            if (t.getAssigneeId() != null) assigneeIds.add(t.getAssigneeId());
            if (t.getDeviceId() != null) deviceIds.add(t.getDeviceId());
        }

        Map<Long, SysUser> userMap = new HashMap<>();
        if (!assigneeIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(assigneeIds)) {
                userMap.put(u.getId(), u);
            }
        }

        Map<Long, String> deviceNameMap = new HashMap<>();
        if (!deviceIds.isEmpty()) {
            for (var d : deviceMapper.selectBatchIds(deviceIds)) {
                deviceNameMap.put(d.getId(), d.getDeviceName());
            }
        }

        for (MaintenanceTask t : tasks) {
            if (t.getAssigneeId() != null) {
                SysUser user = userMap.get(t.getAssigneeId());
                if (user != null) t.setAssigneeName(user.getRealName());
            }
            if (t.getDeviceId() != null) {
                String deviceName = deviceNameMap.get(t.getDeviceId());
                if (deviceName != null) t.setDeviceName(deviceName);
            }
        }
    }
}
