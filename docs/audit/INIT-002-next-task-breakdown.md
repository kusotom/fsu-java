# INIT-002 后续任务拆分

> 基于审计结果生成的后续开发任务清单。

---

## 任务执行路线图

```
INIT-003 ─── B接口协议样例与 fixtures 规范化
   │
INIT-004 ─── B接口测试基线建立
   │
INIT-005 ─── DSC/RDS 遗留内容归档计划（跨项目）
   │
   ├─── BIF-P0-001 ─── 命令码映射表补全
   │
   ├─── BIF-P0-002 ─── SOAP/WSDL 入口完整性修复
   │
   ├─── BIF-P0-003 ─── XMLData 解析与构造统一层
   │
   ├─── BIF-P0-004 ─── 设备注册 LOGIN 闭环（P0）
   │
   ├─── BIF-P0-005 ─── 心跳 HEARTBEAT 闭环（P0）
   │
   ├─── BIF-P0-006 ─── 实时数据 SEND_DATA / GET_DATA 闭环（P0）
   │
   ├─── BIF-P0-007 ─── 告警 SEND_ALARM 闭环（P0）
   │
   ├─── BIF-P1-001 ─── 门限 GET_THRESHOLD / SET_THRESHOLD
   │
   ├─── BIF-P1-002 ─── 遥控遥调 SET_POINT
   │
   ├─── BIF-P1-003 ─── FTP 参数与图片流程
   │
   ├─── BIF-P1-004 ─── 时间同步 TIME_CHECK 与登录信息 GET_LOGININFO
   │
   ├─── BIF-P1-005 ─── 错误码与 ResultCode 统一处理
   │
   └─── BIF-P1-006 ─── 前端协议覆盖矩阵页面
```

---

## INIT-003：B接口协议样例与 Fixtures 规范化

**优先级：** P0
**前置条件：** 无
**预计文件数：** 8-12 个

### 目标
创建标准化的 B接口协议 XML 样例和测试 fixtures，作为后续测试和实现的协议依据。

### 任务项

| # | 任务 | 说明 | 输出 |
|---|------|------|------|
| 1 | 通读 B接口协议 2016 原文 | 提取所有命令的 Request/Response 格式 | 协议提取笔记 |
| 2 | 创建 LOGIN XML 样例 | FSU→SC 登录请求/成功/失败 | `fixtures/binterface/login/` |
| 3 | 创建 HEARTBEAT XML 样例 | FSU→SC 心跳请求/响应 | `fixtures/binterface/heartbeat/` |
| 4 | 创建 SEND_DATA XML 样例 | FSU→SC 实时数据上报 | `fixtures/binterface/send-data/` |
| 5 | 创建 GET_DATA XML 样例 | SC→FSU 查询/返回数据 | `fixtures/binterface/get-data/` |
| 6 | 创建 SEND_ALARM XML 样例 | FSU→SC 告警上报/恢复 | `fixtures/binterface/send-alarm/` |
| 7 | 创建其他命令 XML 样例 | 门限、遥控、FTP、时间同步、登录信息 | `fixtures/binterface/other/` |
| 8 | 创建 Schema/XSD 定义 | xmlData 的 XML Schema | `fixtures/schema/` |
| 9 | 整理 SOAPAction 映射表 | 各命令对应的 SOAPAction | `docs/protocol/soap-action-map.md` |
| 10 | 确认并补全命令码 | 对照协议原文确认 BInterfacePkType | 更新 `BInterfacePkType.java` |

### 涉及的文件

```
新增:
  fixtures/binterface/login/request.xml
  fixtures/binterface/login/response-success.xml
  fixtures/binterface/login/response-fail.xml
  fixtures/binterface/heartbeat/request.xml
  fixtures/binterface/heartbeat/response.xml
  fixtures/binterface/send-data/request.xml
  fixtures/binterface/send-data/response.xml
  fixtures/binterface/get-data/request.xml
  fixtures/binterface/get-data/response.xml
  fixtures/binterface/send-alarm/request.xml
  fixtures/binterface/send-alarm/response.xml
  fixtures/binterface/other/get-threshold-request.xml
  fixtures/binterface/other/set-threshold-request.xml
  fixtures/binterface/other/set-point-request.xml
  fixtures/binterface/other/get-ftp-request.xml
  fixtures/binterface/other/time-check-request.xml
  fixtures/binterface/other/get-logininfo-request.xml
  fixtures/schema/b-interface.xsd
  docs/protocol/soap-action-map.md

修改:
  backend/.../binterface/model/BInterfacePkType.java（补全 SEND_DATA 等）
```

---

## INIT-004：B接口测试基线建立

**优先级：** P0
**前置条件：** INIT-003（需要 XML 样例作为测试输入）
**预计文件数：** 10-15 个

### 目标
建立 B接口模块的单元测试和集成测试基线，确保后续开发可验证。

### 任务项

| # | 任务 | 说明 | 输出 |
|---|------|------|------|
| 1 | 搭建测试基础框架 | Spring Boot Test 配置，测试数据库 | `src/test/java/.../binterface/` 目录 |
| 2 | SoapMessageHandler 测试 | SOAP Envelope 组装/解析单元测试 | `SoapMessageHandlerTest.java` |
| 3 | XmlDataModel 测试 | XML 序列化/反序列化测试 | `XmlDataModelTest.java` |
| 4 | CommandDispatcher 测试 | 命令分发路由测试 | `CommandDispatcherTest.java` |
| 5 | LoginCommandHandler 测试 | LOGIN 处理逻辑测试 | `LoginCommandHandlerTest.java` |
| 6 | HeartbeatCommandHandler 测试 | HEARTBEAT 处理逻辑测试 | `HeartbeatCommandHandlerTest.java` |
| 7 | SendAlarmCommandHandler 测试 | SEND_ALARM 处理逻辑测试 | `SendAlarmCommandHandlerTest.java` |
| 8 | GetDataCommandHandler 测试 | GET_DATA 处理逻辑测试 | `GetDataCommandHandlerTest.java` |
| 9 | SOAP 请求/响应集成测试 | 完整 SOAP 报文收发测试 | `SoapIntegrationTest.java` |
| 10 | 数据库 Repository 测试 | 各 B接口 Repository 查询测试 | `*RepositoryTest.java` |
| 11 | 测试覆盖率检查 | 确保测试覆盖核心路径 | 覆盖率报告 |

### 涉及的文件

```
新增:
  backend/src/test/java/.../binterface/soap/SoapMessageHandlerTest.java
  backend/src/test/java/.../binterface/xml/XmlDataModelTest.java
  backend/src/test/java/.../binterface/command/CommandDispatcherTest.java
  backend/src/test/java/.../binterface/command/LoginCommandHandlerTest.java
  backend/src/test/java/.../binterface/command/HeartbeatCommandHandlerTest.java
  backend/src/test/java/.../binterface/command/SendAlarmCommandHandlerTest.java
  backend/src/test/java/.../binterface/command/GetDataCommandHandlerTest.java
  backend/src/test/java/.../binterface/soap/SoapIntegrationTest.java
  backend/src/test/resources/test-b-interface-samples/（从 fixtures 复制）
```

---

## INIT-005：DSC/RDS 遗留内容归档计划（跨项目）

**优先级：** P1
**前置条件：** 无
**预计文件数：** 2-3 个

### 背景
fsu-python 项目中存在大量 DSC/RDS 实现。本项目必须确保这些内容不会被引入。但 fsu-python 的 DSC/RDS 协议分析成果（如报文解码、字段映射）可能在理解 FSU 设备行为时有参考价值。

### 任务项

| # | 任务 | 说明 | 输出 |
|---|------|------|------|
| 1 | 创建 docs/research/ 目录 | 用于存放跨项目参考文档 | `docs/research/` 目录 |
| 2 | 创建跨项目引用说明 | 说明 fsu-python 的 DSC/RDS 内容仅供研究参考 | `docs/research/fsu-python-dsc-rds-reference.md` |
| 3 | 检查 fsu-platform-java 无 DSC/RDS 引入 | 确认主业务代码无 DSC/RDS | 审计输出 |
| 4 | 更新 AGENTS.md/CLAUDE.md/CODEX.md | 确保规则有效 | 一致性检查 |

### 涉及的文件

```
新增:
  docs/research/README.md
  docs/research/fsu-python-dsc-rds-reference.md
```

---

## BIF-P0-001：命令码映射表补全

**优先级：** P0
**前置条件：** INIT-003（需要协议原文阅读)
**预计文件数：** 2-3 个

### 任务
1. 对照 B接口协议 2016 原文，逐条确认 BInterfacePkType 枚举的完整性
2. 补全缺失的 `SEND_DATA` 等命令码
3. 更新 `docs/protocol/b-interface-command-map.md`

### 涉及的文件

```
修改:
  backend/.../binterface/model/BInterfacePkType.java
  docs/protocol/b-interface-command-map.md
```

---

## BIF-P0-002：SOAP/WSDL 入口完整性修复

**优先级：** P0
**前置条件：** INIT-003（需要 WSDL 标准格式）
**预计文件数：** 3-4 个

### 任务
1. 基于 B接口协议 2016 和 WSDL 协议目录，实现 ScService WSDL 定义
2. 基于 B接口协议 2016 和 WSDL 协议目录，实现 FsuService WSDL 模板
3. 确保 WSDL 包含所有命令的 binding/port/operation

### 涉及的文件

```
修改:
  backend/.../binterface/wsdl/FsuServiceWsdlTemplate.java
  backend/.../binterface/wsdl/ScServiceWsdlController.java

新增或参考:
  WSDL协议/（外部目录，参考使用）
```

---

## BIF-P0-003：XMLData 解析与构造统一层

**优先级：** P0
**前置条件：** INIT-003（需要 XML 样例作为实现依据）
**预计文件数：** 5-8 个

### 任务
1. 实现 SoapMessageHandler 的 SOAP Envelope 组装和解析
2. 实现 XmlDataModel 的 XML 序列化和反序列化
3. 创建各命令对应的 xmlData DTO 或 Map 结构
4. 实现 SOAPAction 处理
5. 实现 SOAP Fault 解析

### 涉及的文件

```
重写:
  backend/.../binterface/soap/SoapMessageHandler.java
  backend/.../binterface/xml/XmlDataModel.java
  backend/.../binterface/model/BInterfaceMessage.java

新增:
  backend/.../binterface/xml/XmlDataParser.java（统一解析层）
  backend/.../binterface/xml/dto/（各命令 DTO）
```

---

## BIF-P0-004：设备注册 LOGIN 闭环

**优先级：** P0
**前置条件：** BIF-P0-003（XMLData 解析）
**预计文件数：** 3-5 个

### 任务
1. 实现 LoginCommandHandler 的业务逻辑
2. FSU 身份验证（FSUCode + 密码/Token）
3. 会话管理（BInterfaceSessionEntity 写入）
4. FSU 状态更新（BInterfaceFsuStatusEntity 更新）
5. 报文日志写入

---

## BIF-P0-005：心跳 HEARTBEAT 闭环

**优先级：** P0
**前置条件：** BIF-P0-003（XMLData 解析）
**预计文件数：** 3-5 个

### 任务
1. 实现 HeartbeatCommandHandler 的业务逻辑
2. 心跳时间记录（BInterfaceFsuStatusEntity.lastHeartbeat 更新）
3. 心跳超时判定
4. 心跳漏报计数（heartbeatMissCount）
5. 报文日志写入

---

## BIF-P0-006：实时数据 SEND_DATA / GET_DATA 闭环

**优先级：** P0
**前置条件：** BIF-P0-003 + BIF-P0-004（XMLData 解析 + 登录状态）
**预计文件数：** 5-8 个

### 任务
1. SEND_DATA：接收 FSU 主动上报的实时数据
2. 实时数据更新（realtime_data upsert）
3. 历史数据归档（history_data 写入）
4. GET_DATA：SC 向 FSU 轮询监控数据
5. FsuServiceClient 完整实现（基于 WSDL + SOAP）

---

## BIF-P0-007：告警 SEND_ALARM 闭环

**优先级：** P0
**前置条件：** BIF-P0-003（XMLData 解析）
**预计文件数：** 3-5 个

### 任务
1. 实现 SendAlarmCommandHandler 的业务逻辑
2. 告警记录入库（alarm_record 写入）
3. 告警级别判定和标记
4. 告警恢复处理（cleared_by/clear_time 更新）
5. 报文日志写入

---

## BIF-P1-001：门限 GET_THRESHOLD / SET_THRESHOLD

**优先级：** P1
**前置条件：** BIF-P0-003 + BIF-P0-006
**预计文件数：** 2-4 个

### 任务
1. SC 从 FSU 获取告警阈值配置
2. SC 向 FSU 下发告警阈值配置
3. 阈值变更记录

---

## BIF-P1-002：遥控遥调 SET_POINT

**优先级：** P1
**前置条件：** BIF-P0-003 + BIF-P0-004
**预计文件数：** 2-4 个

### 任务
1. SC 向 FSU 下发遥控遥调指令
2. 指令执行结果反馈
3. 操作日志记录

---

## BIF-P1-003：FTP 参数与图片流程

**优先级：** P1
**前置条件：** BIF-P0-003
**预计文件数：** 3-5 个

### 任务
1. 实现 FTP 客户端连接管理
2. GET_FTP：获取 FTP 文件列表和配置参数
3. SET_FTP：设置 FTP 参数（安全限制）
4. 图片/文件传输记录写入

---

## BIF-P1-004：时间同步 TIME_CHECK 与登录信息 GET_LOGININFO

**优先级：** P1
**前置条件：** BIF-P0-003
**预计文件数：** 2-3 个

### 任务
1. TIME_CHECK：SC 向 FSU 下发标准时间
2. GET_LOGININFO：SC 查询 FSU 登录状态

---

## BIF-P1-005：错误码与 ResultCode 统一处理

**优先级：** P1
**前置条件：** BIF-P0-003
**预计文件数：** 2-3 个

### 任务
1. 定义 B接口 ResultCode 枚举（对照协议 2016）
2. 实现统一错误码映射
3. SOAP Fault 与 ResultCode 的联动

---

## BIF-P1-006：前端协议覆盖矩阵页面

**优先级：** P1
**前置条件：** BIF-P0-001 ~ BIF-P0-007（后端 B接口实现基本完成）
**预计文件数：** 1-3 个

### 任务
1. CommandMatrixView 从静态表格升级为动态覆盖矩阵
2. 按 FSU 设备维度展示各命令的实现状态
3. 命令测试入口（仅限安全命令，排除 SET_FSUREBOOT）

---

## 优先级汇总

### P0（必须优先完成）

| 任务 | 预计文件数 | 依赖 |
|------|-----------|------|
| INIT-003：B接口协议样例与 Fixtures | 8-12 | 无 |
| INIT-004：B接口测试基线建立 | 10-15 | INIT-003 |
| BIF-P0-001：命令码映射表补全 | 2-3 | INIT-003 |
| BIF-P0-002：SOAP/WSDL 入口 | 3-4 | INIT-003 |
| BIF-P0-003：XMLData 解析与构造 | 5-8 | INIT-003 |
| BIF-P0-004：LOGIN 闭环 | 3-5 | BIF-P0-003 |
| BIF-P0-005：HEARTBEAT 闭环 | 3-5 | BIF-P0-003 |
| BIF-P0-006：SEND_DATA/GET_DATA 闭环 | 5-8 | BIF-P0-003 |
| BIF-P0-007：SEND_ALARM 闭环 | 3-5 | BIF-P0-003 |

### P1（核心功能）

| 任务 | 预计文件数 | 依赖 |
|------|-----------|------|
| INIT-005：DSC/RDS 归档计划 | 2-3 | 无 |
| BIF-P1-001：门限管理 | 2-4 | BIF-P0-003 |
| BIF-P1-002：遥控遥调 | 2-4 | BIF-P0-003 |
| BIF-P1-003：FTP 参数与图片 | 3-5 | BIF-P0-003 |
| BIF-P1-004：时间同步/登录信息 | 2-3 | BIF-P0-003 |
| BIF-P1-005：错误码/ResultCode | 2-3 | BIF-P0-003 |
| BIF-P1-006：前端覆盖矩阵 | 1-3 | BIF-P0 系列 |

### P2（后续增强）

| 功能 | 说明 |
|------|------|
| GET_HISTORY_DATA | 历史数据查询 |
| GET_FSUINFO | FSU 信息查询 |
| SET_DATA | 参数设置 |
| SET_FSUREBOOT | 远程重启（保持禁用） |
| 告警确认流程 | 告警确认/忽略 |
| 分页实现 | 大数据量查询 |

---

## 建议执行顺序

```
Phase 1 — 协议基础准备
  INIT-003 → BIF-P0-001 → BIF-P0-002 → BIF-P0-003

Phase 2 — P0 命令闭环 + 测试
  (BIF-P0-004 ~ BIF-P0-007) 并行 + INIT-004

Phase 3 — P1 命令实现
  BIF-P1-001 ~ BIF-P1-006

Phase 4 — 安全与归档
  INIT-005 + 后续增强
```
