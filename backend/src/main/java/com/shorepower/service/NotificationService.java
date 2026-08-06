package com.shorepower.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shorepower.common.BusinessException;
import com.shorepower.entity.Notification;
import com.shorepower.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知服务
 *
 * 双通道通知机制：
 *   1. 持久化：写入 notification 表（供历史查询和未读计数）
 *   2. 实时推送：通过 WebSocket 推送到 /topic/notification/{userId}
 *
 * 通知类型（refType）：
 *   MAINTENANCE - 维护任务指派
 *   扩展：可支持 ALARM、PAYMENT、SYSTEM 等类型
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate ws;

    /**
     * 创建通知（持久化 + WebSocket 推送）
     *
     * @param userId  接收通知的用户ID
     * @param title   通知标题
     * @param content 通知内容
     * @param refType 关联业务类型（如 MAINTENANCE）
     * @param refId   关联业务ID（如维护任务ID）
     */
    public void create(Long userId, String title, String content, String refType, Long refId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);

        ws.convertAndSend("/topic/notification/" + userId, Map.of(
                "id", n.getId(),
                "title", title,
                "content", content,
                "refType", refType,
                "refId", refId,
                "isRead", 0
        ));
    }

    public Page<Notification> listByUser(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public int countUnread(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper).intValue();
    }

    /**
     * 标记单条通知为已读（仅限本人通知，防止越权修改他人通知）
     */
    public void markRead(Long userId, Long id) {
        Notification update = new Notification();
        update.setIsRead(1);
        int rows = notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
            .eq(Notification::getId, id)
            .eq(Notification::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException("通知不存在");
        }
    }

    public void markAllRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getIsRead, 0);
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, wrapper);
    }
}
