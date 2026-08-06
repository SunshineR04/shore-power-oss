package com.shorepower.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WebSocket 订阅主题 ACL 单元测试：
 * 覆盖运维主题角色限制、个性化主题本人限制、非法主题格式。
 */
class TopicAccessPolicyTest {

    // ---------- 运维类全局主题：仅 ADMIN/OPERATOR ----------

    @Test
    void staffTopics_adminAllowed() {
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/device-data", 1L, "ADMIN"));
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/alarm", 1L, "ADMIN"));
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/device-status", 1L, "ADMIN"));
    }

    @Test
    void staffTopics_operatorAllowed() {
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/device-data", 2L, "OPERATOR"));
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/alarm-resolved", 2L, "OPERATOR"));
    }

    @Test
    void staffTopics_userRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/device-data", 3L, "USER"));
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/alarm", 3L, "USER"));
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/device-status", 3L, "USER"));
    }

    // ---------- 个性化主题：仅本人 ----------

    @Test
    void personalTopic_ownAllowed() {
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/notification/100", 100L, "USER"));
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/maintenance-assigned/5", 5L, "OPERATOR"));
    }

    @Test
    void personalTopic_otherUserRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/notification/100", 99L, "USER"));
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/maintenance-assigned/5", 6L, "OPERATOR"));
    }

    @Test
    void personalTopic_missingSessionUserRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/notification/100", null, "USER"));
    }

    @Test
    void personalTopic_invalidFormatRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/notification/abc", 1L, "USER"));
        assertThrows(IllegalArgumentException.class,
            () -> TopicAccessPolicy.checkSubscription("/topic/notification/", 1L, "USER"));
    }

    // ---------- 普通主题：任意已认证用户可订阅 ----------

    @Test
    void dataSyncTopic_anyAuthenticatedUserAllowed() {
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/data-sync", 3L, "USER"));
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription("/topic/data-sync", 1L, "ADMIN"));
    }

    @Test
    void nullDestination_allowed() {
        assertDoesNotThrow(() -> TopicAccessPolicy.checkSubscription(null, 1L, "USER"));
    }
}
