# INIT-002 B接口协议实现现状审计

## 审计信息

- **审计时间：** 2026-05-13
- **审计范围：** fsu-platform-java 全项目
- **审计类型：** 只读审计
- **协议基准：** 《中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0，2016 年 9 月》

---

## 一、项目结构总览

```
fsu-platform-java/
├── backend/                          # Java Spring Boot 3.2.5
│   ├── pom.xml
│   └── src/main/java/com/dcim/platform/
│       ├── DcimPlatformApplication.java
│       ├── common/                   # 公共模块（ApiResponse, Swagger, GlobalException, Health）
│       └── module/
│           ├── binterface/           # B接口核心模块 — 22 个文件
│           ├── resource/             # 资源管理（站点/机柜/FSU/点位）— 17 个文件
│           ├── telemetry/            # 遥测数据（实时/历史/心跳）— 12 个文件
│           ├── alarm/                # 告警管理 — 4 个文件
│           └── system/               # 系统管理（用户/角色）— 9 个文件
├── frontend/                         # Vue 3 + TypeScript + Element Plus
│   └── src/
│       ├── api/                      # 6 个 API 封装文件（53 函数）
│       ├── components/               # 2 个共享组件
│       ├── layouts/                  # BasicLayout 布局
│       ├── router/                   # 15 条路由
│       ├── types/                    # 17 个 TS 接口
│       └── views/                    # 15 个页面
├── database/
│   ├── schema/001_init_schema.sql    # 17 张表 DDL
│   └── seed/001_seed_demo_data.sql   # 演示数据
├── deploy/docker-compose.yml         # PostgreSQL 16
├── docs/                             # 架构、协议、任务、验收文档
└── tools/                            # 模拟器/协议测试（均为空占位）
```

**当前状态：** INIT 系列已完成 INIT-001 ~ INIT-004。后端、前端、数据库骨架搭建完成，**B接口协议尚未实现**。

---

## 二、B接口模块实现盘点

### 2.1 SCService 服务端（快数据通道：FSU → SC）

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `ScServiceController.java` | ❌ STUB | `POST /api/b-interface/sc-service` 接收 String，返回占位字符串 |
| `ScServiceHandler.java` | ❌ STUB | 空类，3 个 TODO 注释 |

**缺口：**
- 无 SOAP/XML 报文解析
- 无 PK_Type 路由分发
- 无命令处理器调用
- 无认证/鉴权
- 无报文日志写入

### 2.2 FSUService 客户端（慢数据通道：SC → FSU）

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `FsuServiceClient.java` | ✅ INTERFACE | 定义 5 个方法签名（login/heartbeat/getData/setData/reboot） |
| `FsuServiceClientStub.java` | ❌ STUB | 5 个空方法实现，全部返回 null |

**缺口：**
- 无真实 SOAP 客户端
- 无 HTTP 连接池
- 无 WSDL 动态代理
- 无超时/重试机制
- 无调用记录写入

### 2.3 命令分发与处理

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `CommandDispatcher.java` | ❌ STUB | `dispatch()` 返回 null |
| `LoginCommandHandler.java` | ❌ STUB | 空类 |
| `HeartbeatCommandHandler.java` | ❌ STUB | 空类 |
| `GetDataCommandHandler.java` | ❌ STUB | 空类 |
| `SendAlarmCommandHandler.java` | ❌ STUB | 空类 |

**缺口：**
- 命令码→Handler 映射未实现
- 每个 Handler 的业务逻辑为 0
- 无 FSU 认证逻辑
- 无心跳超时判定
- 无数据采集/入库逻辑
- 无告警处理逻辑

### 2.4 SOAP/XML 层

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `SoapMessageHandler.java` | ❌ STUB | `buildRequest()` 返回 null，`parseResponse()` 返回 null |
| `XmlDataModel.java` | ❌ STUB | `toXml()` 返回 null，`fromXml()` 返回 null |

**缺口：**
- 无 SOAP Envelope/Header/Body 组装
- 无命名空间管理
- 无 XML 序列化/反序列化
- 无 xsd 校验
- 无 SOAP Fault 处理

### 2.5 WSDL

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `FsuServiceWsdlTemplate.java` | ❌ STUB | `getTemplate()` 返回占位字符串 |
| `ScServiceWsdlController.java` | ❌ STUB | `GET /api/b-interface/wsdl/sc-service` 返回占位字符串 |

**缺口：**
- 无完整 WSDL 定义
- 无 binding/port/operation 定义
- 无 message/part 定义
- 两个 WSDL 均未实现

### 2.6 报文日志

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `BInterfaceMessageLogEntity.java` | ✅ ENTITY | JPA 实体，字段完整 |
| `BInterfaceMessageLogRepository.java` | ✅ REPO | Spring Data JPA，含按 fsu/command 查询 |
| `BInterfaceMessageLogService.java` | ❌ STUB | 空类，无日志写入实现 |
| `BInterfaceMessageLogController.java` | ✅ CONTROLLER | 只读 `GET list/{id}` |

**缺口：**
- 日志写入未实现（无 SOAP 报文不会产生日志）
- 无日志清理策略
- 无日志查询过滤（按时间/命令/FSU）

### 2.7 FTP 模块

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `FtpConfigModel.java` | ✅ MODEL | 配置模型（host/port/username/password/passiveMode/basePath） |
| `FtpTransferRecordEntity.java` | ✅ ENTITY | JPA 实体，字段完整 |
| `FtpTransferRecordController.java` | ✅ CONTROLLER | 只读 `GET list/{id}` |

**缺口：**
- 无真实 FTP 客户端
- 无文件上传/下载实现
- 无图片处理逻辑
- 无传输进度跟踪

### 2.8 命令码枚举

| 文件 | 状态 | 实际实现内容 |
|------|------|-------------|
| `BInterfacePkType.java` | ✅ DEFINED | 14 个命令 + UNKNOWN，共 15 个枚举值 |

**定义的命令码：** LOGIN, HEARTBEAT, GET_DATA, SEND_ALARM, GET_HISTORY_DATA, SET_POINT, GET_THRESHOLD, SET_THRESHOLD, TIME_CHECK, GET_FTP, GET_LOGININFO, GET_FSUINFO, SET_DATA, SET_FSUREBOOT, UNKNOWN

**缺口：**
- 缺少 `SEND_DATA` 命令码（FSU 主动上报实时数据，区别于 SEND_ALARM）
- 部分命令码命名与协议原文可能存在差异（需对照协议文档确认）

### 2.9 数据库 B接口相关表

| 表 | 状态 | 说明 |
|----|------|------|
| `b_interface_command` | ✅ 已创建 | 15 条命令定义，含 implemented/safe_enabled |
| `b_interface_message_log` | ✅ 已创建 | 含 direction/command_code/pk_type/info/xml_data/raw_message |
| `b_interface_session` | ✅ 已创建 | 含 session_id(UK)/auth_token/last_active_time |
| `b_interface_fsu_status` | ✅ 已创建 | 含 login_status/online_status/last_heartbeat |
| `b_interface_call_record` | ✅ 已创建 | 含 request/response/duration_ms/retry_count |
| `ftp_transfer_record` | ✅ 已创建 | 含 file_type/direction/transfer_status/checksum |
| `device_heartbeat` | ✅ 已创建 | 含 fsu_code/heartbeat_time/status_info |

### 2.10 前端 B接口页面

| 页面 | 状态 | 说明 |
|------|------|------|
| `BInterfaceOverviewView` | ✅ 占位卡片 | 显示版本/SCService/FSUService 状态，安全策略提示 |
| `FsuStatusView` | ✅ 表格 | 只读 FSU 状态列表（从 API 拉取） |
| `MessageLogView` | ✅ 表格 | 只读报文日志列表 |
| `CommandMatrixView` | ✅ 表格 | 只读命令清单列表 |
| `CallRecordView` | ✅ 表格 | 只读调用记录列表 |
| `FtpRecordView` | ✅ 表格 | 只读 FTP 记录列表 |

**前端均为只读展示**，无任何 POST/PUT/DELETE 操作，SET_FSUREBOOT 已做安全隔离。

---

## 三、已实现功能汇总

| 类别 | 具体内容 | 位置 |
|------|---------|------|
| ✅ 项目骨架 | Spring Boot + Vue 3 + PostgreSQL 工程 | 全项目 |
| ✅ 公共模块 | ApiResponse、GlobalExceptionHandler、Swagger、Health | common/ |
| ✅ 数据模型 | 17 张表 DDL、15 个 JPA Entity、17 个 TS 接口 | database/ + backend/ + frontend/ |
| ✅ CRUD API | 22 个 REST 端点（resource/telemetry/alarm/system 模块） | 各 module/controller/ |
| ✅ B接口枚举 | BInterfacePkType 14 种命令 + UNKNOWN | binterface/model/ |
| ✅ B接口只读查询 | 6 个 B接口 Controller（list/get） | binterface/controller/ |
| ✅ 前端页面骨架 | 15 个页面全部搭建 | frontend/src/views/ |
| ✅ API 封装 | 53 个前端 API 函数 | frontend/src/api/ |
| ✅ 安全边界 | SET_FSUREBOOT 禁用、B接口全部只读、无真实连接 | 各层 |
| ✅ 数据库 | 17 表 DDL + 种子数据 | database/ |
| ✅ 文档 | 架构/协议/任务/验收/报告 全套 | docs/ |

---

## 四、部分实现功能

| 类别 | 具体内容 | 位置 | 缺口 |
|------|---------|------|------|
| ⚠️ 命令码定义 | BInterfacePkType 枚举 | binterface/model/ | 缺少 SEND_DATA，未验证与协议一致性 |
| ⚠️ 报文日志模型 | Entity + Repository + Controller | binterface/log/ | 无日志写入逻辑 |
| ⚠️ FTP 模块 | 配置模型 + Entity + Controller | binterface/ftp/ | 无 FTP 客户端实现 |
| ⚠️ 种子 SOAP 样例 | HEARTBEAT SOAP 报文示例 | database/seed/ | 仅 1 条样例，无其他命令 |

---

## 五、未实现功能

| 功能 | 优先级 | 阻塞依赖 | 说明 |
|------|--------|---------|------|
| SOAP/XML 解析 | P0 | 无 | SoapMessageHandler / XmlDataModel 为空 |
| WSDL 定义 | P0 | 无 | 两个 WSDL 均为占位 |
| LOGIN 登录认证 | P0 | SOAP 解析 | LoginCommandHandler 为空 |
| HEARTBEAT 心跳 | P0 | SOAP 解析 | HeartbeatCommandHandler 为空 |
| SEND_ALARM 告警上报 | P0 | SOAP 解析 | SendAlarmCommandHandler 为空 |
| GET_DATA 数据轮询 | P0 | SOAP 解析 | GetDataCommandHandler 为空 |
| 设备状态管理 | P1 | LOGIN 认证 | 无状态变更逻辑 |
| 告警恢复 | P1 | SEND_ALARM | 告警清除流程 |
| 门限管理 | P1 | 无 | GET_THRESHOLD / SET_THRESHOLD |
| 遥控遥调 | P1 | 无 | SET_POINT |
| FTP 图片/文件 | P1 | FTP 客户端 | 文件/图片传输 |
| 时间同步 | P1 | 无 | TIME_CHECK |
| 错误码/ResultCode | P1 | SOAP 解析 | 统一错误码体系 |
| 单元测试 | P0 | 所有模块 | 仅 1 个占位测试类 |
| 集成测试 | P1 | 测试框架 | 无 |
| XML 样例/Fixtures | P0 | 无 | 仅 1 条 HEARTBEAT 样例 |

---

## 六、主要缺口与风险等级

| # | 缺口 | 风险等级 | 影响范围 |
|---|------|---------|---------|
| 1 | **B接口协议实现为 0%** | 🚨 P0 | 项目核心功能全部缺失 |
| 2 | **无 WSDL 定义** | 🚨 P0 | FSU 无法获取接口描述 |
| 3 | **无 SOAP/XML 处理** | 🚨 P0 | 所有 B接口命令无法收发 |
| 4 | **无单元测试/集成测试** | 🚨 P0 | 无法验证任何功能 |
| 5 | **无 SEND_DATA 命令码** | ⚠️ P1 | 协议 2016 包含此命令 |
| 6 | **无 XML 样例/fixtures** | ⚠️ P1 | 无法进行协议测试 |
| 7 | **无认证鉴权** | ⚠️ P1 | 安全性依赖后续实现 |
| 8 | **后端未编译/启动验证** | ⚠️ P1 | 存在未知编译错误 |
| 9 | **DTO/VO 层缺失** | 🔵 P2 | Entity 直接暴露为 API 响应 |
| 10 | **分页未实现** | 🔵 P3 | 大数据量查询性能问题 |

---

## 七、B接口功能完成度估算

| 维度 | 完成度 | 说明 |
|------|--------|------|
| 工程骨架 | 100% | 项目、模块、实体、API 端点 |
| 数据模型 | 90% | 17 表 DDL + 15 Entity（缺 SEND_DATA 相关字段？） |
| 前端展示 | 70% | 15 页骨架（无真实数据联动） |
| SOAP/XML 协议栈 | 0% | SoapMessageHandler + XmlDataModel 为空 |
| WSDL | 0% | 两个 WSDL 均为占位 |
| B接口命令处理 | 0% | 5 个 Handler 全部为空 |
| 测试覆盖 | 0% | 仅有 1 个空测试类 |
| 协议 Fixtures | 5% | 仅 1 条 HEARTBEAT SOAP 样例 |
| 安全机制 | 30% | 基础安全边界（禁用重启、只读），无认证鉴权 |
| 文档 | 60% | 架构/协议概述/命令映射（缺协议实现细节） |

**整体 B接口协议实现完成度：约 8%**（95% 为骨架/数据模型，协议处理逻辑几乎为零）
