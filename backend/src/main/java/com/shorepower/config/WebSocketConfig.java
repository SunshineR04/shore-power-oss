package com.shorepower.config;

import com.shorepower.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket（STOMP over SockJS）配置
 *
 * 安全机制：
 *   HTTP 握手的 /ws 端点在 SecurityConfig 中 permitAll，
 *   但 STOMP CONNECT 帧的 Authorization 头由 SessionAuthValidator 校验
 *   （与 HTTP JwtAuthFilter 完全对齐：用户状态/token_version/数据库角色）。
 *   订阅鉴权由 TopicAccessPolicy 执行。
 *
 * 心跳：SimpleBroker 显式配置 10s 心跳，与前端 heartbeatIncoming/Outgoing 一致，
 *   避免心跳超时导致的误断连。
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final SessionAuthValidator sessionAuthValidator;
    private final List<String> allowedOrigins;

    public WebSocketConfig(SessionAuthValidator sessionAuthValidator,
                           @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}") List<String> allowedOrigins) {
        this.sessionAuthValidator = sessionAuthValidator;
        this.allowedOrigins = allowedOrigins;
    }

    /** 心跳任务调度器：SimpleBroker 心跳与前端 10s 心跳对齐（同时被 @Scheduled 定时任务复用） */
    @Bean
    public TaskScheduler heartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-scheduler-");
        return scheduler;
    }

    /**
     * 配置消息代理
     *   - enableSimpleBroker("/topic")：服务端向 /topic 推送，客户端直接订阅
     *   - 心跳 10s/10s：与前端 STOMP 心跳参数一致
     *   - setApplicationDestinationPrefixes("/app")：预留客户端发送前缀
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
              .setHeartbeatValue(new long[]{10_000, 10_000})
              .setTaskScheduler(heartbeatTaskScheduler());
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点：/ws（SockJS）
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins.toArray(new String[0])).withSockJS();
    }

    /**
     * 入站通道拦截器：
     *   - CONNECT：SessionAuthValidator 校验 JWT（与 HTTP 对齐），
     *     用户信息（含数据库最新角色）写入 session attributes
     *   - SUBSCRIBE：TopicAccessPolicy 按角色/归属校验主题
     *
     * 拒绝时记录明确原因（warn），前端 ERROR 帧可读。
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    try {
                        SysUser user = sessionAuthValidator.authenticate(authHeader);
                        // 以数据库当前角色为准（不信任 token claim）
                        accessor.getSessionAttributes().put("userId", user.getId());
                        accessor.getSessionAttributes().put("username", user.getUsername());
                        accessor.getSessionAttributes().put("role", user.getRole());
                    } catch (IllegalArgumentException e) {
                        log.warn("WS CONNECT 拒绝: {} (remote={})", e.getMessage(), accessor.getSessionId());
                        throw e;
                    }
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Long sessionUserId = (Long) accessor.getSessionAttributes().get("userId");
                    String role = (String) accessor.getSessionAttributes().get("role");
                    try {
                        TopicAccessPolicy.checkSubscription(accessor.getDestination(), sessionUserId, role);
                    } catch (IllegalArgumentException e) {
                        log.warn("WS SUBSCRIBE 拒绝: {} (session={}, dest={})",
                                e.getMessage(), accessor.getSessionId(), accessor.getDestination());
                        throw e;
                    }
                }
                return message;
            }
        });
    }
}
