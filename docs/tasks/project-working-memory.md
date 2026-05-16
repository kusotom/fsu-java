# 项目工作记忆

## 当前项目

- 项目名称：dcim-platform（机房动环监控平台）
- 协议基础：《中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0，2016 年 9 月》
- 技术栈：Java 17 + Spring Boot 3.2.5 + PostgreSQL 16 + Vue 3 + TypeScript + Vite 5 + Element Plus
- 当前阶段：INIT 系列（工程基础）
- 项目路径：`C:\Users\测试\Desktop\动环项目\dcim-platform`

## 任务状态

| 任务 | 状态 | 日期 |
|------|------|------|
| INIT-001 | 完成 | 2026-05-10 |
| INIT-001-FIX-001 | 完成 | 2026-05-10 |
| ENV-001 | 完成（环境未就绪） | 2026-05-10 |
| INIT-002 | 完成 | 2026-05-10 |
| INIT-003 | 完成 | 2026-05-10 |
| INIT-004 | 完成 | 2026-05-10 |
| ENV-002 | 完成（环境未就绪） | 2026-05-10 |
| ENV-003 | 完成（平台限制已确认） | 2026-05-10 |
| ENV-004 | 完成（环境未就绪） | 2026-05-10 |
| **INIT-005** | **待开始** | - |

## 已完成内容汇总

### 后端（Java Spring Boot）

```
backend/src/main/java/com/dcim/platform/
├── DcimPlatformApplication.java
├── common/
│   ├── api/HealthController.java           # GET /api/health
│   ├── config/SwaggerConfig.java           # Swagger/OpenAPI 3
│   ├── exception/GlobalExceptionHandler.java
│   └── response/ApiResponse.java           # 统一响应 {code, message, data, timestamp}
└── module/
    ├── resource/                           # 资源管理（17个文件）
    │   ├── entity/    SiteEntity, CabinetEntity, FsuDeviceEntity, MonitoringPointEntity
    │   ├── repository/ 4 个 Repository
    │   ├── service/    4 个 Service
    │   └── controller/ 4 个 Controller → /api/sites, /api/cabinets, /api/fsu-devices, /api/monitoring-points
    ├── telemetry/                          # 数据遥测（12个文件）
    │   ├── entity/    RealtimeDataEntity, HistoryDataEntity, DeviceHeartbeatEntity
    │   ├── repository/ 3 个 Repository
    │   ├── service/    3 个 Service
    │   └── controller/ 3 个 Controller → /api/telemetry/realtime, /api/telemetry/history, /api/telemetry/heartbeats
    ├── alarm/                              # 告警管理（4个文件）
    │   ├── entity/    AlarmRecordEntity
    │   ├── repository/ AlarmRecordRepository
    │   ├── service/   AlarmRecordService
    │   └── controller/ AlarmRecordController → /api/alarms
    ├── binterface/                         # B接口模块（原有 + 新增共22个新文件）
    │   ├── entity/    BInterfaceCommandEntity, BInterfaceSessionEntity, BInterfaceFsuStatusEntity, BInterfaceCallRecordEntity
    │   ├── repository/ 6 个 Repository
    │   ├── service/   6 个 Service（全部只读）
    │   ├── controller/ 6 个 Controller → /api/b-interface/*
    │   ├── command/   CommandDispatcher + 4 Handler（INIT-001 占位）
    │   ├── ftp/       FtpConfigModel, FtpTransferRecordEntity（INIT-001）
    │   ├── log/       BInterfaceMessageLogEntity, Repository, Service（INIT-001）
    │   ├── model/     BInterfaceMessage, BInterfacePkType
    │   ├── service/sc/ ScServiceController, ScServiceHandler（INIT-001 占位）
    │   ├── service/fsu/ FsuServiceClient, FsuServiceClientStub（INIT-001 占位）
    │   ├── soap/      SoapMessageHandler（INIT-001 占位）
    │   ├── xml/       XmlDataModel（INIT-001 占位）
    │   └── wsdl/      ScServiceWsdlController, FsuServiceWsdlTemplate（INIT-001 占位）
    └── system/                             # 系统管理（9个文件）
        ├── entity/    UserAccountEntity, RoleEntity, UserRoleEntity
        ├── repository/ 2 个 Repository
        ├── service/    2 个 Service
        └── controller/ 2 个 Controller → /api/system/users, /api/system/roles
```

**总计：63 个新文件，22 个 REST API 端点，覆盖 17 张核心表。**
无 @ManyToOne/@OneToMany（全部 Long 外键）。B接口模块全部只读。

### 前端（Vue 3 + TypeScript）

```
frontend/src/
├── api/
│   ├── request.ts          # Axios 封装（baseURL=/api, 15s 超时）
│   ├── health.ts           # 1 函数
│   ├── resource.ts         # 20 函数
│   ├── telemetry.ts        # 8 函数
│   ├── alarm.ts            # 3 函数
│   ├── bInterface.ts       # 11 函数
│   └── system.ts           # 10 函数
├── components/
│   ├── PageHeader.vue      # 页面标题 + 描述
│   └── SearchPanel.vue     # 搜索面板骨架
├── types/index.ts          # 17 个 TypeScript 接口
├── layouts/BasicLayout.vue # 左侧菜单 + 顶栏 + 实时时钟
├── router/index.ts         # 15 条路由
└── views/
    ├── dashboard/DashboardView.vue
    ├── resource/            # SiteManagement, CabinetManagement, FsuDeviceManagement, PointManagement（CRUD）
    ├── telemetry/           # RealtimeData, HistoryData（只读）
    ├── alarm/AlarmCenterView.vue
    ├── binterface/          # Overview, FsuStatus, MessageLog, CommandMatrix, CallRecord, FtpRecord（只读）
    └── system/UserManagementView.vue
```

**6 个 API 封装文件，53 个函数。15 个页面全部增强。`npm run build` 通过。**

### 数据库

```
database/
├── schema/001_init_schema.sql    # 17 张表完整 DDL + 12 UK + 19索引 + 触发器
├── seed/001_seed_demo_data.sql   # 1站点/1机柜/1FSU/5点位/3用户/15命令
└── README.md

deploy/
└── docker-compose.yml            # PostgreSQL 16, 端口5432, dcim/dcim123456
```

### 文档

```
docs/
├── architecture/   01-overview.md, 02-module-boundary.md, 03-b-interface-architecture.md
├── database/       core-model-design.md, table-list.md, index-and-constraint-design.md
├── protocol/       b-interface-2016-summary.md, b-interface-command-map.md
├── api/            README.md
├── acceptance/     INIT-001-acceptance.md
├── tasks/          00-project-planning-master.md, INIT-001-foundation.md
│                   project-working-memory.md, task-history.md
└── reports/        INIT-001 ~ INIT-004 + FIX-001 + ENV-001 ~ ENV-004（共 9 份）
```

## 安全边界

- SET_FSUREBOOT `safe_enabled = FALSE`（数据库级），B接口无启用/执行入口（前端+后端级）
- FTP 仅数据模型，无真实连接
- B接口全部只读（GET），无 POST/PUT/DELETE
- 无 @ManyToOne/@OneToMany（全部 Long 外键）
- 不实现真实协议、不连接真实 FSU、不做 UDP/TCP/MQTT/DSC/RDS

---

## 新系统启动指南

将此目录完整复制到新系统后，按以下顺序操作：

### 前提：安装基础工具

| 工具 | 版本 | 下载 |
|------|------|------|
| Java JDK | 17+ | https://adoptium.net/download/ (Temurin 17 LTS) |
| Maven | 3.8+ | https://maven.apache.org/download.cgi |
| Node | 18+ | https://nodejs.org/ |
| Docker Desktop | 最新 | https://www.docker.com/products/docker-desktop/ |

### 一键启动流程

```powershell
# 0. 进入项目目录
cd dcim-platform

# 1. 启动 PostgreSQL
cd deploy
docker compose up -d postgres
docker compose ps                                # 确认容器 dcim-postgres 运行中

# 2. 初始化数据库
cd ..
psql -h localhost -U postgres -c "CREATE USER dcim WITH PASSWORD 'dcim123456';"  2>&1
psql -h localhost -U postgres -c "CREATE DATABASE dcim_platform OWNER dcim;"     2>&1
# 如果提示 already exists 可忽略
psql -h localhost -U dcim -d dcim_platform -f database/schema/001_init_schema.sql
psql -h localhost -U dcim -d dcim_platform -f database/seed/001_seed_demo_data.sql
psql -h localhost -U dcim -d dcim_platform -c "\dt"                               # 期望 17 张表

# 3. 编译并启动后端
cd backend
mvn clean package -DskipTests
mvn spring-boot:run                              # 监听 http://localhost:8080

# 4. 验证后端（另开终端）
curl http://localhost:8080/api/health
curl http://localhost:8080/api/b-interface/health
# Swagger: http://localhost:8080/swagger-ui/index.html

# 5. 启动前端（另开终端）
cd frontend
npm install
npm run dev                                      # 监听 http://localhost:5173
```

### 预期验证结果

| 端点 | 预期返回 |
|------|----------|
| `GET /api/health` | `{"code":0, "data":{"service":"dcim-platform-backend","status":"UP"}}` |
| `GET /api/b-interface/health` | `{"code":0, "data":{"bInterfaceVersion":"B接口 2016...","scServiceStatus":"STUB"}}` |
| `GET /api/sites` | `{"code":0, "data":[...]}` |
| Swagger UI | OpenAPI 3 文档页面 |

### 后续任务

**INIT-005：模拟采集闭环** — 需要后端和数据库运行后执行：
1. 后端模拟数据生成器
2. realtime_data upsert + history_data 批量写入
3. 模拟告警生成
4. 前端 Dashboard 动态数据
