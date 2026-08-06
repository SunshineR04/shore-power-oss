package com.shorepower.config;

import java.util.List;

/**
 * WebSocket 订阅主题访问控制策略（独立于 STOMP 通道，便于单元测试）。
 *
 * 规则：
 *   - 运维类全局主题（设备遥测/告警/状态）：仅 ADMIN / OPERATOR
 *   - 个性化主题 /topic/notification/{userId}、/topic/maintenance-assigned/{userId}：
 *     仅允许订阅者本人（sessionUserId 必须与目标 userId 一致）
 *   - /topic/data-sync 等其余主题：任意已认证用户可订阅（用于页面刷新信号）
 */
public class TopicAccessPolicy {

    /** 需要 ADMIN/OPERATOR 角色的全局主题前缀 */
    private static final List<String> STAFF_ONLY_PREFIXES = List.of(
        "/topic/device-data", "/topic/alarm", "/topic/alarm-resolved", "/topic/device-status"
    );

    /** 个性化主题前缀：要求目标 userId == 会话 userId */
    private static final List<String> PERSONAL_PREFIXES = List.of(
        "/topic/notification/", "/topic/maintenance-assigned/"
    );

    private TopicAccessPolicy() {
    }

    /**
     * 校验订阅是否被允许。
     *
     * @param destination   订阅主题，如 /topic/alarm
     * @param sessionUserId 当前会话用户 ID（CONNECT 时写入）
     * @param role          当前会话角色（ADMIN/OPERATOR/USER）
     * @throws IllegalArgumentException 无权订阅时抛出，由 STOMP 层转换为 ERROR 帧
     */
    public static void checkSubscription(String destination, Long sessionUserId, String role) {
        if (destination == null) {
            return;
        }
        boolean staff = "ADMIN".equals(role) || "OPERATOR".equals(role);
        boolean staffOnly = STAFF_ONLY_PREFIXES.stream().anyMatch(destination::startsWith);
        if (staffOnly && !staff) {
            throw new IllegalArgumentException("无权订阅此主题");
        }
        boolean personal = PERSONAL_PREFIXES.stream().anyMatch(destination::startsWith);
        if (personal) {
            if (sessionUserId == null) {
                throw new IllegalArgumentException("无权订阅此主题");
            }
            String[] parts = destination.split("/");
            try {
                Long targetUserId = Long.valueOf(parts[parts.length - 1]);
                if (!sessionUserId.equals(targetUserId)) {
                    throw new IllegalArgumentException("无权订阅此主题");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("非法的订阅主题");
            }
        }
    }
}
