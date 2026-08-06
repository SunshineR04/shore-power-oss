修复监控面板"已断开、无实时参数"问题。

## 根因（已确认）
1. STOMP 订阅被拦截器拒绝且前端 onStompError/onDisconnect 完全静默（后端日志每 3 秒一次拒绝事件，reconnectDelay=3000 吻合）——无法定位拒绝原因。
2. WS 认证与 HTTP 不对称：HTTP JwtAuthFilter 校验用户状态+token_version+DB 角色；WS CONNECT 只验签名+过期，ACL 用 token 里的 role claim。
3. 断线轮询兜底失效：startStatusPolling 从未调用；静默轮询启动后 WS 恢复不停止。
4. alarm 页 onDisconnect/onStompError 定时器泄漏（不清旧值，轮询堆积）。
5. 无参数第二层原因：非 dev 启动时模拟器不运行 + device_data 空 → 轮询 INNER JOIN 无数据。

## 实施步骤
1. WebSocketConfig：CONNECT 校验对齐 HTTP（用户存在/status==1/token_version 匹配，角色以 DB 当前值为准），所有拒绝路径 log.warn 明确原因。
2. 前端 monitor.vue / alarm/index.vue / useDataSync.js：onStompError 打印 frame.headers.message，onDisconnect 打印原因。
3. monitor.vue：断线时调用 startStatusPolling()；WS 恢复时完整停止轮询；轮询间隔统一读配置。
4. alarm/index.vue：启动轮询前先 clearInterval 旧值（幂等）。
5. WebSocketConfig.configureMessageBroker：显式 taskScheduler + 心跳 10s 与前端对齐。
6. 后端：提取 CONNECT 认证校验为可测组件并补单测；README 增"WS 断线排障"。
7. 验证：dev 启动前后端，浏览器实测（admin 登录→实时+参数更新；停后端→已断开+控制台明确错误；恢复→自动重连）；后端 38+ 前端 10 测试全绿；提交推送触发 CI。