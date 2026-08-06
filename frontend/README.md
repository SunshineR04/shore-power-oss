# 前端（长江岸电码头智能运维系统）

基于 Vue 3 + Vite + Element Plus 的用户端/运维端/管理端界面。

## 技术栈

- Vue 3.5（Composition API）
- Vite 8
- Element Plus
- Pinia
- Vue Router 4
- Axios
- ECharts（按需引入）
- STOMP.js + SockJS（实时数据）

## 常用命令

```bash
npm ci          # 安装依赖（锁定版本）
npm run dev     # 开发服务，端口 3000（/api 与 /ws 代理到 8088）
npm run typecheck  # vue-tsc 类型检查
npm test        # Vitest 单元测试
npm run build   # 生产构建（先类型检查）
```

## 环境要求

- Node.js 20.19+（Vite 8 要求）
- 后端服务运行在 `http://localhost:8088`（开发时由 Vite 代理，无需额外配置）

## 说明

- 生产部署时 `/api` 与 `/ws` 需要由反向代理（Nginx 等）转发到后端服务。
- 登录凭证保存在 `sessionStorage`（演示用途；生产建议改用 HttpOnly Cookie）。
- 支付为模拟流程，点击“我已支付”即完成订单，不涉及真实资金。
