package com.shorepower.config;

import com.shorepower.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket（STOMP over SockJS）配置
 *
 * 架构说明：
 *   - 使用 STOMP 协议在 WebSocket 之上实现发布-订阅模式
 *   - 后端使用 SockJS 作为 fallback 传输层（浏览器不支持原生 WebSocket 时自动降级为轮询）
 *   - 消息代理（SimpleBroker）处理 /topic/** 前缀的消息，实现服务端推送
 *
 * 安全机制：
 *   HTTP 握手的 /ws 端点在 SecurityConfig 中配置为 permitAll（允许建立连接），
 *   但 STOMP CONNECT 帧的 Authorization 头部会在 configureClientInboundChannel 中被拦截校验 JWT。
 *   这是 Spring WebSocket 安全的标准模式——HTTP 层面先握手，STOMP 协议层再做认证。
 *
 * 订阅主题（Topic）：
 *   /topic/device-data     - 设备实时运行数据（DataSimulator 推送）
 *   /topic/alarm           - 告警事件（DataSimulator 推送）
 *   /topic/device-status   - 设备状态变更（DataSimulator + ReservationService 推送）
 *   /topic/data-sync       - 数据同步通知（如支付完成）
 *   /topic/notification/{userId} - 用户个性化通知
 *   /topic/maintenance-assigned/{userId} - 维护任务指派通知
 *
 * 前端使用 SockJS 客户端连接 ws://{host}:8088/ws（由 Vite 代理到后端 8088 端口）
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final List<String> allowedOrigins;

    public WebSocketConfig(JwtUtil jwtUtil,
                           @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}") List<String> allowedOrigins) {
        this.jwtUtil = jwtUtil;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 配置消息代理
     *   - enableSimpleBroker("/topic")：服务端向 /topic/** 推送消息，客户端直接订阅接收
     *   - setApplicationDestinationPrefixes("/app")：客户端发送消息用 /app/** 前缀，
     *     由 @MessageMapping 注解的方法处理（本系统未使用客户端发送功能）
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点：/ws
     *
     * 前端通过 SockJS 连接 http://{host}:8088/ws，
     * 后端自动协商最佳传输方式（原生 WebSocket → XHR 流 → XHR 轮询）
     * withSockJS() 必须在 setAllowedOriginPatterns 之后调用
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins.toArray(new String[0])).withSockJS();
    }

    /**
     * 配置入站通道拦截器——在 STOMP CONNECT 帧到达时校验 JWT
     *
     * 注意：HTTP 握手阶段（/ws）是公开的（SecurityConfig 中 permitAll），
     * 但 STOMP CONNECT 阶段会在 protocol 层面做 JWT 校验。
     * 这是 Spring WebSocket 安全的标准模式。
     *
     * 校验失败抛 IllegalArgumentException → STOMP 连接被拒绝（前端收到 ERROR 帧）
     * 校验成功将 userId 和 username 存入 session 属性，供后续 @MessageMapping 使用
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                // 包装 STOMP 消息以访问协议头
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                // 只在 CONNECT 帧（首次建立连接）时做认证
                // 后续的消息复用已建立的 session，不再重复校验
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 从 STOMP 头部提取 Authorization 字段
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("未提供有效的认证Token");
                    }
                    String token = authHeader.substring(7);
                    // 校验 JWT 签名和过期时间
                    if (!jwtUtil.validateToken(token)) {
                        throw new IllegalArgumentException("Token无效或已过期");
                    }
                    // 将用户信息存入 session，后续处理器可以直接获取
                    accessor.getSessionAttributes().put("userId", jwtUtil.getUserId(token));
                    accessor.getSessionAttributes().put("username", jwtUtil.getUsername(token));
                }

                // 订阅鉴权：用户只能订阅自己的个性化主题，防止越权窃听他人通知
                // /topic/notification/{userId}、/topic/maintenance-assigned/{userId} 中的 userId
                // 必须与当前会话 userId 一致，否则拒绝订阅
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String dest = accessor.getDestination();
                    if (dest != null
                            && (dest.startsWith("/topic/notification/")
                            || dest.startsWith("/topic/maintenance-assigned/"))) {
                        Long sessionUserId = (Long) accessor.getSessionAttributes().get("userId");
                        String[] parts = dest.split("/");
                        try {
                            Long targetUserId = Long.valueOf(parts[parts.length - 1]);
                            if (sessionUserId == null || !sessionUserId.equals(targetUserId)) {
                                throw new IllegalArgumentException("无权订阅此主题");
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("非法的订阅主题");
                        }
                    }
                }
                return message;
            }
        });
    }
}
