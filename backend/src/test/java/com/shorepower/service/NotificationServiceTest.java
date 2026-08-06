package com.shorepower.service;

import com.shorepower.common.BusinessException;
import com.shorepower.entity.Notification;
import com.shorepower.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * NotificationService 单元测试：已读操作必须限定本人（防 IDOR）。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private SimpMessagingTemplate ws;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationMapper, ws);
    }

    @Test
    void markRead_ownNotification_succeeds() {
        // 条件更新命中（本人通知）
        when(notificationMapper.update(any(), any())).thenReturn(1);
        notificationService.markRead(1L, 100L);
    }

    @Test
    void markRead_otherUsersNotification_throws() {
        // 条件更新未命中（非本人通知）→ 抛业务异常
        when(notificationMapper.update(any(), any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> notificationService.markRead(2L, 100L));
    }
}
