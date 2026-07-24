/**
 * WebSocket 数据同步 composable
 *
 * 核心职责：建立并维护与后端的 WebSocket 连接，接收实时数据推送
 *
 * 连接方式：
 *   使用 STOMP over SockJS（而不是原生 WebSocket），因为后端配置了 .withSockJS()
 *   SockJS 会在浏览器不支持原生 WebSocket 时自动降级为 XHR 轮询/流
 *
 * 安全认证：
 *   在 STOMP CONNECT 帧的 Authorization 头部携带 JWT Token
 *   后端 WebSocketConfig 的 configureClientInboundChannel 会拦截校验
 *
 * 断线重连策略：
 *   1. STOMP 自动重连：配置 reconnectDelay=5000（每5秒尝试一次）
 *   2. 心跳检测：每10秒发送心跳，约30秒无响应则认为断开
 *   3. HTTP 轮询兜底：onDisconnect/onStompError 时启动 setTimeout
 *      - 每隔10秒触发一次 refreshKey++，驱动 Vue 响应式更新
 *      - onConnect 成功时清除轮询定时器
 *      - 前端会同时启动 WebSocket 和轮询，WS 连接成功后轮询自动关闭
 *
 * 返回值：
 *   refreshKey - Vue ref，每次数据更新时自增，
 *   组件中 watch(refreshKey) 即可实现实时刷新
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// STOMP 客户端实例（模块级单例，所有调用 useDataSync 的组件共享一个连接）
let client = null
// 响应式刷新键：每次自增，组件 watch 此值触发数据重新拉取
const refreshKey = ref(0)

export function useDataSync() {
  let pollTimer = null

  /**
   * 启动 STOMP over SockJS 连接
   *
   * 使用 SockJS 而非原生 WebSocket，原因：
   *   后端配置了 .withSockJS()，客户端必须匹配
   *   SockJS 在浏览器不支持 WS 时自动降级为 XHR 流/轮询
   *
   * JWT 通过 STOMP CONNECT 帧的 Authorization 头传递
   */
  function startClient() {
    if (client) return                    // 防止重复创建
    const token = sessionStorage.getItem('token')
    if (!token) return                     // 未登录不连接
    client = new Client({
      // webSocketFactory 而非 brokerURL：SockJS 用户自定义工厂方式
      webSocketFactory: () => new SockJS('/ws'),
      // JWT Token 放在 STOMP CONNECT 头部，后端 WebSocketConfig 拦截校验
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,               // 断线后每5秒尝试重连
      heartbeatIncoming: 10000,            // 接收心跳：10秒
      heartbeatOutgoing: 10000,            // 发送心跳：10秒
      onConnect: () => {
        // 连接成功：清除 HTTP 轮询
        clearInterval(pollTimer)
        pollTimer = null
        // 订阅 /topic/data-sync，后端有数据变更时推送此主题
        client.subscribe('/topic/data-sync', () => {
          refreshKey.value++               // 触发 Vue 响应式更新
        })
      },
      onDisconnect: () => startPolling(),  // 断开 → 启动 HTTP 轮询兜底
      onStompError: () => startPolling()   // 错误 → 启动 HTTP 轮询兜底
    })
    client.activate()
  }

  /**
   * HTTP 轮询兜底
   * 当 WebSocket 断开或出错时启动，每隔10秒触发一次 refreshKey++
   * WebSocket 恢复时（onConnect）自动清除轮询
   */
  function startPolling() {
    if (pollTimer) return                  // 防止重复启动
    pollTimer = setInterval(() => { refreshKey.value++ }, 10000)
  }

  /** 清理函数：关闭 WebSocket 和轮询定时器 */
  function stopClient() {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
    if (client) { client.deactivate(); client = null }
  }

  // 组件挂载时同时启动 WebSocket 和轮询（WS 成功连上后轮询自动关闭）
  onMounted(() => { startClient(); startPolling() })
  // 组件卸载时清理
  onUnmounted(() => stopClient())

  return { refreshKey }
}
