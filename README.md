# 长江岸电码头智能运维系统

> Intelligent Yangtze River Shore Power Wharf O&M System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Vue 3](https://img.shields.io/badge/Vue-3.5.32-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

针对长江流域岸电设施运维场景，设计并实现的一套集**设备监控、能耗管理、运维工单、预约调度、支付结算、报警通知**于一体的智能化管理系统。

## 功能特性

### 用户端
- 岸电设备查询、预约、用能结算
- 船舶信息管理、用电记录
- 订单支付（模拟支付，仅用于演示）
- 个人中心、设备详情

### 运维端
- 故障告警与处理工单
- 设备监控、维保任务
- 通知中心、消息推送
- 实时数据看板（ECharts）

### 管理端
- 用户/角色管理
- 设备台账、类型配置
- 系统参数配置
- 财务/能耗统计

### 智能化模块
- 基于时序分析的能耗预测（`TimeSeriesForecast`）
- 动态电价策略（峰谷平，`ElectricityPriceService`）
- 能耗优化建议（`EnergyOptimizationService`）
- 实时数据模拟与推送（WebSocket + STOMP）

## 技术栈

### 后端 `backend/`

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.5 |
| 语言 | Java 21 |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库迁移 | Flyway |
| 安全 | Spring Security + JJWT 0.12.5 |
| 缓存 | Caffeine |
| 实时通信 | WebSocket + STOMP |
| 工具 | Hutool 5.8.27、Lombok |
| API 文档 | springdoc-openapi 2.6.0（Swagger UI） |

### 前端 `frontend/`

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.5.32（Composition API） |
| 构建 | Vite 8.0.4 |
| UI | Element Plus 2.13.7 |
| 状态 | Pinia 3.0.4 |
| 路由 | Vue Router 4.6.4 |
| HTTP | Axios |
| 图表 | ECharts 6.0 |
| 实时 | STOMP.js + SockJS |
| 类型 | TypeScript 6.0 |

## 项目结构

```
shore-power-system/
├── backend/                  Spring Boot 后端
│   ├── src/main/java/com/shorepower/
│   │   ├── config/           配置类（Security/Jackson/Cache/WebSocket…）
│   │   ├── controller/       REST 控制器
│   │   ├── service/          业务服务
│   │   ├── mapper/           MyBatis-Plus Mapper
│   │   ├── entity/           实体类
│   │   ├── dto/              数据传输对象
│   │   ├── common/           通用组件（异常处理、统一返回）
│   │   ├── security/         JWT 鉴权
│   │   ├── task/             定时任务
│   │   ├── utils/            工具类（时序预测等）
│   │   └── websocket/        WebSocket 数据模拟器
│   └── src/main/resources/
│       ├── application.yml       主配置
│       ├── application-dev.yml   开发环境
│       ├── application-prod.yml  生产环境
│       ├── db/migration/         Flyway 迁移脚本（V1~V7）
│       └── mapper/              MyBatis XML
│
├── frontend/                 Vue 3 前端
│   ├── src/
│   │   ├── api/              接口封装
│   │   ├── composables/      组合式函数
│   │   ├── layout/           布局组件
│   │   ├── router/           路由（含 RBAC 守卫）
│   │   ├── store/            Pinia 状态
│   │   ├── types/            TS 类型定义
│   │   ├── utils/            工具（axios 封装）
│   │   └── views/            页面（user/operator/admin 三角色域）
│   └── vite.config.js
│
├── .env.example              环境变量模板
└── .gitignore
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+（或使用提交的 Maven Wrapper）
- MySQL 8.0+
- Node.js 20.19+（Vite 8 要求）

### 1. 初始化数据库

```sql
CREATE DATABASE shorepower DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Flyway 会在后端首次启动时自动执行迁移脚本并写入种子数据。

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，设置 JWT_SECRET 和 DB_PASSWORD
```

> ⚠️ Spring Boot 不会自动读取项目根目录的 `.env` 文件。请通过 shell / 容器 / CI 将环境变量注入后端进程：
>
> ```bash
> export JWT_SECRET="$(openssl rand -base64 48)"
> export DB_PASSWORD="你的数据库密码"
> ```
>
> 缺少 `JWT_SECRET` 或密钥短于 32 字节时，后端会**拒绝启动**。

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

- 端口：8088
- Swagger UI：http://localhost:8088/swagger-ui.html
- 健康检查：http://localhost:8088/actuator/health

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 端口：3000（Vite）
- 访问：http://localhost:3000

## 默认账号

种子数据创建的演示账号（密码均为 `123456`）：

| 角色 | 账号 |
|------|------|
| 管理员 | `admin` |
| 运维人员 | `op1` |
| 普通用户 | `user1` |

> ⚠️ 这些账号仅用于本地演示，**生产环境必须删除种子数据或修改默认密码**。

## 模拟支付说明

> ⚠️ 本项目为演示项目，支付流程为**模拟实现**：点击“我已支付”即完成订单，不涉及真实资金。
> 生产环境必须接入支付宝/微信支付并实现签名校验、金额核对与幂等回调，且不应暴露模拟回调接口。

## 测试

```bash
# 后端（单元测试，Mockito 隔离，无需数据库）
cd backend && ./mvnw test

# 前端（Vitest 单元测试）
cd frontend && npm test
```

CI（GitHub Actions，见 `.github/workflows/ci.yml`）会执行：

- 后端：Java 21 + MySQL 8 集成环境下的 `mvn verify`
- 前端：`npm ci` → `typecheck` → `test` → `build` → `npm audit`（高危及以上失败）

## 模拟器与模拟数据说明

- 设备数据模拟器（`DataSimulator`）与能耗历史填充（`EnergyStatService`）**仅 dev profile 启用**，
  生产 profile 不会写入任何模拟数据。
- 演示账号与模拟支付仅用于本地演示，生产环境必须删除种子数据并接入真实支付网关。

## WebSocket 断线排障

监控面板右上角显示"已断开"或无实时参数时，按以下顺序排查：

1. **打开浏览器控制台（F12 → Console）**：页面会在断线时打印 `[monitor] STOMP 错误: <原因>`。
   - `Token无效或已过期` / `Token已失效，请重新登录` → 重新登录后刷新页面。
   - `账号已被禁用` → 账号被禁用，联系管理员。
   - `无权订阅此主题` → 当前账号角色无权查看运维数据（设备/告警主题仅 ADMIN/OPERATOR）。
   - 无错误但持续"已断开" → 检查 Network 面板 `/ws/...` 握手状态码（403 表示来源域名不在 CORS 白名单，见下方）。
2. **确认后端为 dev profile 启动**：启动日志应显示 `The following profiles are active: dev`。
   非 dev 环境模拟器不运行，设备无实时数据（设备状态仍显示"在线"，但无参数）。
3. **确认访问来源在白名单**：`cors.allowed-origins` 默认仅允许 `http://localhost:3000,http://localhost:5173`；
   使用其他端口/局域网 IP/域名访问时需在环境变量 `CORS_ORIGINS` 中加入对应来源。
4. **确认心跳未超时**：前端与后端心跳均为 10s（已显式配置），一般不会误断。

## 数据库迁移

使用 Flyway，脚本位于 `backend/src/main/resources/db/migration/`：

| 版本 | 说明 |
|------|------|
| V1 | 初始化核心表结构 |
| V2 | 种子数据 |
| V3 | 通知模块 |
| V4 | 船舶电气规格 |
| V5 | 天气配置 |
| V6 | 支付订单 |
| V7 | 唯一索引（防并发注册） |

## API 文档

启动后端后访问：
- Swagger UI：http://localhost:8088/swagger-ui.html
- OpenAPI JSON：http://localhost:8088/v3/api-docs

## 作者

**SunshineR04** — [GitHub](https://github.com/SunshineR04)

## License

[MIT](LICENSE)
