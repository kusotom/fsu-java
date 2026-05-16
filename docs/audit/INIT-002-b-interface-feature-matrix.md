# INIT-002 B接口协议功能覆盖矩阵

> 协议基准：《中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0，2016 年 9 月》

## 图例
- ✅ 已实现 — 代码完成，可工作
- ⚠️ 部分实现 — 骨架/模型/枚举完成，但核心逻辑缺失
- ❌ 未实现 — 无相关代码或仅为空占位
- ❓ 存疑 — 未确认协议定义，需对照原文

---

## 核心命令功能矩阵

| # | 协议功能 | MsgType | 方向 | 实现状态 | 代码位置 | 测试位置 | 缺口说明 | 优先级 | 阻塞后续 | 建议任务 |
|---|---------|---------|------|---------|---------|---------|---------|--------|---------|---------|
| 1 | **LOGIN / 注册** | LOGIN | FSU→SC | ❌ 未实现 | `LoginCommandHandler.java` (空) | 无 | Handler 为空，无 SOAP 解析，无 FSU 认证 | P0 | 是 | BIF-P1-002 |
| 2 | **HEARTBEAT / 心跳** | HEARTBEAT | FSU→SC | ❌ 未实现 | `HeartbeatCommandHandler.java` (空) | 无 | Handler 为空，无超时判定，无状态更新 | P0 | 是 | BIF-P1-003 |
| 3 | **SEND_DATA / 上报实时数据** | SEND_DATA | FSU→SC | ❌ 未实现 | 无对应代码 | 无 | BInterfacePkType 中缺少 SEND_DATA 枚举 | P0 | 是 | BIF-P0-001 |
| 4 | **GET_DATA / 获取实时数据** | GET_DATA | SC→FSU | ❌ 未实现 | `GetDataCommandHandler.java` (空) | 无 | Handler 为空，无 SOAP 客户端调用 | P0 | 是 | BIF-P1-005 |
| 5 | **SEND_ALARM / 告警上报** | SEND_ALARM | FSU→SC | ❌ 未实现 | `SendAlarmCommandHandler.java` (空) | 无 | Handler 为空，无告警入库 | P0 | 是 | BIF-P1-004 |
| 6 | **GET_THRESHOLD / 获取门限** | GET_THRESHOLD | SC→FSU | ❌ 未实现 | 无 | 无 | 仅枚举已定义 | P1 | 否 | BIF-P1-001 |
| 7 | **SET_THRESHOLD / 设置门限** | SET_THRESHOLD | SC→FSU | ❌ 未实现 | 无 | 无 | 仅枚举已定义 | P1 | 否 | BIF-P1-001 |
| 8 | **SET_POINT / 遥控遥调** | SET_POINT | SC→FSU | ❌ 未实现 | 无 | 无 | 仅枚举已定义 | P1 | 否 | BIF-P1-002 |
| 9 | **GET_FTP / 获取 FTP 参数** | GET_FTP | SC→FSU | ❌ 未实现 | 无 | 无 | 仅枚举已定义 | P1 | 否 | BIF-P1-003 |
| 10 | **SET_FTP / 设置 FTP 参数** | SET_FTP | SC→FSU | ❓ 存疑 | 无 | 无 | BInterfacePkType 中无 SET_FTP 枚举 | P1 | 否 | BIF-P0-001 |
| 11 | **GET_LOGININFO / 获取登录信息** | GET_LOGININFO | SC→FSU | ❌ 未实现 | 无 | 无 | 仅枚举已定义 | P2 | 否 | BIF-P1-004 |
| 12 | **TIME_CHECK / 时间同步** | TIME_CHECK | SC→FSU | ❌ 未实现 | 无 | 无 | 仅枚举已定义 | P2 | 否 | BIF-P1-004 |
| 13 | **设备状态管理** | — | 双向 | ❌ 未实现 | `BInterfaceFsuStatusEntity` (仅模型) | 无 | 状态未驱动（无登入/登出/心跳变更） | P1 | 是 | BIF-P1-002 |

---

## 扩展功能矩阵

| # | 功能 | 实现状态 | 代码位置 | 缺口说明 | 优先级 |
|---|------|---------|---------|---------|--------|
| 14 | **监控点管理** | ✅ 已实现 | `monitoring_point` 表 + Entity/Repo/Service/Controller | CRUD 已完成 | — |
| 15 | **信号量管理** | ✅ 已实现 | `monitoring_point` 表包含 point_type(AI/DI/DO/PI) | 信号量通过监控点位实现 | — |
| 16 | **告警恢复** | ❌ 未实现 | 无 | `alarm_record` 表有 clear_time/cleared_by 但无恢复逻辑 | P1 |
| 17 | **告警确认** | ❌ 未实现 | 无 | `alarm_record` 表有 confirm_time/confirm_user_id 但无确认流程 | P2 |
| 18 | **错误码 / ResultCode** | ❌ 未实现 | 无 | 无统一错误码体系 | P1 |
| 19 | **XML 样例** | ⚠️ 部分实现 | `database/seed/` 含 1 条 HEARTBEAT SOAP 样例 | 仅 1 条，缺其他命令样例 | P0 |
| 20 | **SOAPAction** | ❌ 未实现 | 无 | 无 SOAPAction 定义 | P0 |
| 21 | **WSDL binding** | ❌ 未实现 | `FsuServiceWsdlTemplate.java` + `ScServiceWsdlController.java` | 两个 WSDL 均为占位 | P0 |
| 22 | **日志留痕** | ⚠️ 部分实现 | `b_interface_message_log` 表 + Entity + Repo + Controller | 表/模型已就绪，写入逻辑未实现 | P0 |
| 23 | **测试覆盖** | ❌ 未实现 | `DcimPlatformApplicationTests.java` (空) | 仅 1 个占位测试类 | P0 |
| 24 | **FTP 图片传输** | ❌ 未实现 | 无 | FTP 客户端未实现 | P1 |
| 25 | **SC 服务端认证** | ❌ 未实现 | 无 | 无 FSU 身份验证逻辑 | P1 |
| 26 | **GET_HISTORY_DATA** | ❌ 未实现 | 无 | 仅枚举已定义 | P2 |
| 27 | **GET_FSUINFO** | ❌ 未实现 | 无 | 仅枚举已定义 | P2 |
| 28 | **SET_DATA** | ❌ 未实现 | 无 | 仅枚举已定义 | P2 |
| 29 | **SET_FSUREBOOT** | ❌ 安全禁用 | 无 | 枚举已定义，safe_enabled=FALSE | P2 |

---

## 各层实现状态

### SOAP/XML 协议栈

| 层 | 实现状态 | 文件 | 说明 |
|---|---------|------|------|
| SOAP Envelope 组装 | ❌ | `SoapMessageHandler.buildRequest()` | 返回 null |
| SOAP Envelope 解析 | ❌ | `SoapMessageHandler.parseResponse()` | 返回 null |
| SOAP Fault 处理 | ❌ | 无 | 未实现 |
| 命名空间管理 | ❌ | 无 | 未实现 |
| XML Serialization | ❌ | `XmlDataModel.toXml()` | 返回 null |
| XML Deserialization | ❌ | `XmlDataModel.fromXml()` | 返回 null |
| XSD 校验 | ❌ | 无 | 未实现 |

### SCService 服务端

| 层 | 实现状态 | 文件 | 说明 |
|---|---------|------|------|
| HTTP 接收端点 | ✅ | `ScServiceController` | POST 端点已注册 |
| SOAP/XML 解析 | ❌ | — | 依赖 SoapMessageHandler |
| PK_Type 路由 | ❌ | `CommandDispatcher.dispatch()` | 返回 null |
| 命令处理 | ❌ | 5 个 Handler | 全部空类 |
| 报文日志 | ❌ | `BInterfaceMessageLogService` | 写入未实现 |

### FSUService 客户端

| 层 | 实现状态 | 文件 | 说明 |
|---|---------|------|------|
| 接口定义 | ✅ | `FsuServiceClient` | 5 个方法签名 |
| WSDL 动态代理 | ❌ | — | 未实现 |
| HTTP 调用 | ❌ | `FsuServiceClientStub` | 全部空方法 |
| 超时/重试 | ❌ | — | 未实现 |
| 调用记录 | ❌ | — | 未写入 b_interface_call_record |

### WSDL

| 定义 | 实现状态 | 文件 | 说明 |
|-----|---------|------|------|
| SCService WSDL | ❌ | `ScServiceWsdlController` | 返回占位字符串 |
| FSUService WSDL | ❌ | `FsuServiceWsdlTemplate` | 返回占位字符串 |

---

## 优先级结构

| 级别 | 含义 | 功能数 |
|------|------|--------|
| **P0** | 必须优先完成，阻塞其他任务 | 9 |
| **P1** | 核心功能，需尽快完成 | 12 |
| **P2** | 重要功能，可在核心功能后完成 | 6 |
| **P3** | 增强/优化功能 | 2 |

### P0 功能清单（阻塞项）
1. SOAP/XML 解析与构造
2. WSDL 定义（SCService + FSUService）
3. LOGIN 登录认证
4. HEARTBEAT 心跳处理
5. SEND_DATA 命令码补全
6. SEND_ALARM 告警处理
7. GET_DATA 数据轮询
8. XML 样例 / Fixtures
9. 测试基线建立
