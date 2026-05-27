# OpenSpec Project Context — FSU 动环监控平台

> 本文件为 OpenSpec + Superpowers 规格驱动层提供项目级上下文。
> 最后更新：2026-05-20（SPEC-INIT-001 OpenSpec + Superpowers 工作流初始化）

---

## 1. 项目概述

**项目名称**: FSU-JAVA / 机房动环监控

**FSU-JAVA** 是机房动环监控平台（FSU — Field Supervision Unit），基于 Java 17 + Spring Boot 3.2.5 构建。平台通过 B接口协议 2016/2024 与 FSU 设备进行 SOAP/XMLData 通信，实现设备注册、心跳、数据采集、告警上报、门限管理、遥控遥调等功能。

## 2. 项目位置

```
项目根: /home/tom/桌面/FSU/fsu-platform-java/
```

## 3. 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.5 |
| 持久化 | JPA / Hibernate |
| 数据库 | PostgreSQL 16 (Docker, 容器名 `dcim-postgres`, 库 `dcim`, 用户 `dcim/dcim123456`) |
| 测试 | JUnit 5 + Maven Surefire |
| 构建 | Maven |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |
| 协议 | B接口 2016 / 2024, SOAP 1.1, WSDL 1.1, RPC/encoded style |
| 规格驱动 | OpenSpec v1.3.1 |
| 工作流 | Superpowers (brainstorming → writing-plans → test-driven-development → executing-plans → verification → code-review → finishing) |

## 4. 协议概要

### 4.1 B接口协议

B接口协议 2016 是本项目设备上报、采集、交互、管理的唯一协议标准。

**快数据通道 (SCService)**：
- WSDL 端点：`/services/SCService`
- Controller 路径：`POST /api/b-interface/sc-service`
- 方向：FSU → 平台（入站，被动上报）
- 命令：LOGIN(101), HEARTBEAT, SEND_DATA, SEND_ALARM(501)

**慢数据通道 (FSUService)**：
- WSDL 端点：`/services/FSUService`
- 方向：平台 → FSU（出站，主动查询）
- 命令：GET_DATA(401), GET_THRESHOLD, GET_ACTIVEALARM(603), GET_LOGININFO(1501), GET_FSUINFO(1701), GET_FTP(1601), GET_SPCONFIGOPTION, TIME_CHECK, SET_THRESHOLD, SET_TIME, SET_POINT 等

### 4.2 关键约定

- SOAP 1.1, RPC/encoded style, `operation=invoke`
- PK_Type 支持两种格式：Name+Code (2024 结构化) 和纯文本 (legacy)
- 2016 码表：三格式支持 (structured / legacy-text / legacy-2016)
- ACK 响应使用与请求同名的 PK_Type（非 `_ACK` 后缀）
- SC 入站 ACK 使用纯文本 PK_Type
- FSU 出站查询使用 Name+Code 格式
- 所有设备上报、采集、交互必须以 B接口 SOAP/XMLData 为准

### 4.3 协议基线 (PROTOCOL-BASELINE-001, 2026-05-21)

| 版本 | 定位 | 说明 |
|------|------|------|
| **B接口2016** | **当前主开发依据 / 真实落地协议** | FSU 51051243812345 已验证 |
| B接口2024 | 未来升级版本 / 可选兼容层 | 保留实现，标注为兼容层 |

2016 关键码表: LOGIN=101, GET_DATA=401→402, GET_LOGININFO=1501→1502, GET_FSUINFO=1701→1702, GET_FTP=1601→1602, SEND_ALARM=501。

## 5. 当前真实设备信息

| 项目 | 值 |
|------|-----|
| **FSUService 地址** | `http://192.168.100.100:8080/services/FSUService` |
| **FSUID** | `51051243812345` |
| **FsuCode** | `51051243812345` |
| **StationName** | `1` |
| **SCIP** (FSU 侧配置) | `192.168.100.123` |
| **协议版本** | B接口2016 码表（已确认） |

### 已获取 DeviceID

| # | DeviceID |
|---|----------|
| 1 | 51051241820004 |
| 2 | 51051241830004 |
| 3 | 51051241840004 |
| 4 | 51051240700002 |

### 已获取监测数据

| 指标 | 值 | 来源 |
|------|-----|------|
| CPU | 14.95% | GET_FSUINFO (Code=1701) |
| MEM | 62.84% | GET_FSUINFO (Code=1701) |

## 6. 当前协议结论

### 6.1 协议版本确认

**真实 FSU 使用 B接口2016 码表。2024 码表不适配该设备。**

证据：
1. SEND_ALARM=501（与 2016 一致，2024 中=601）
2. GET_LOGININFO=1501 返回 GET_LOGININFO_ACK=1502
3. GET_FSUINFO=1701 返回 GET_FSUINFO_ACK=1702
4. GET_FTP=1601 返回 GET_FTP_ACK=1602
5. 所有 2016 Code 均收到正确的 ACK 响应
6. PK_Type 使用 Name+Code 格式（FSU 接受结构化格式但使用 2016 码值）

### 6.2 GET_DATA 状态

- GET_DATA (Code=401) 路由正确
- GET_DATA_ACK (Code=402) Result=1（FSU 接受请求）
- **当前 Values / DeviceList 为空** — FSU 当前无测量点位数据
- 4 种策略（全设备/指定信号/单设备/通配）均返回空 DeviceList

### 6.3 下一步方向

**当前 GET_DATA 无法获取实时测量数据。下一步应准备 FSU → SCService 被动上报接收链路**，由 FSU 通过 LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM 主动推送数据到平台。

## 7. 项目架构

```
Controller / Handler
  → Application Service / Orchestrator
    → Domain Service
      → Repository / Adapter
        → Protocol / XML / SOAP 工具层
```

## 8. 当前安全配置

```yaml
real-call-enabled: false            # 默认不访问真实 FSU
scheduler-enabled: false            # 默认不启用 Scheduler
set-threshold.enabled: false
offline-detection.enabled: false
active-alarm-audit.scheduler-enabled: false
set-command-safety.scheduler-forbidden: true
```

## 9. 已完成阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| RULES-FIX-001 | 统一 Claude 规则加载路径 | ✅ |
| BIF-P4-FIX-001 | BIF-P4-020 Codex 审计阻断修复 | ✅ |
| BIF-P4-021 | Scheduler 定时审计 | ✅ |
| BIF-P4-022 | 审计状态查询 API | ✅ |
| BIF-P4-023 | 审计持久化 | ✅ |
| LANDING-001 | 真实点位表模型补强 (serialNo/deviceId/serviceUrl) | ✅ |
| LANDING-002 | B接口2024 附录标准化配置模板 | ✅ |
| LANDING-003 | 真实 FSU 只读联调采集 (6 命令, 24 报文) | ✅ |
| LANDING-004 | 嵌套 XML 声明容错 + PK_Type 双格式 | ✅ |
| LANDING-005 | 真实 FSUID (51051243812345) 重试 | ✅ |
| LANDING-006 | **B接口2016 码表兼容** — 首次获取真实数据 (CPU/MEM/4 DeviceIDs) | ✅ |
| LANDING-007 | GET_DATA 真实 DeviceID 查询 (4 策略, 确认空响应) | ✅ |
| LANDING-008 | 被动上报接收联调准备 (SCService 链路核对, 文档) | ✅ |
| OPENSPEC-001 | OpenSpec 规格驱动层接入 | ✅ |
| OPENSPEC-CLI-001 | OpenSpec CLI v1.3.1 安装与校验 | ✅ |
| **SPEC-INIT-001** | **OpenSpec + Superpowers 工作流初始化** | **← 当前** |

## 10. 测试基线

以最新 memory/audit 记录为准。最近一次全量测试记录（LANDING-008）为：

```
1164 tests, 0 failures, 0 errors, 5 skipped
contextLoads: PASS
```

## 11. 安全边界

| 边界 | 状态 | 说明 |
|------|------|------|
| 真实 FSU 访问 | **默认关闭** | `@Tag("real-fsu")` + `@EnabledIfSystemProperty`，默认 mvn test 不访问 192.168.100.100 |
| Scheduler | **默认关闭** | `scheduler-enabled: false`，禁止自动启动定时任务 |
| SET 命令 | **默认禁止** | 必须经过安全门禁 + confirmationToken + 审计 + dry-run |
| alarm_record 写入 | **默认只读** | 活动告警审计类任务只读，禁止自动修改/恢复/清除告警 |
| 真实联调 | **必须显式开启** | `realFsuTest.enabled=true`, `real-call-enabled=true`, 不参与默认 mvn test |
| 候选点位表 | **不能直接作为正式 seed SQL** | 需管理方确认后导入 |
| Codex 复审 | **暂缓** | 累积项 BIF-P4-FIX-001 ~ LANDING-008，待集中复审，不能写成已通过 |

## 12. 禁止事项

- 禁止重新引入 DSC / RDS 作为业务主协议
- 禁止绕过 B接口 XML/SOAP 的私有采集协议
- 禁止使用临时 JSON 结构代替 B接口 XMLData
- 禁止自造 `MsgType`、`SignalID`、`DeviceID`、`FsuCode`、`ResultCode`
- 禁止在未论证的情况下扩展协议字段
- 禁止在主业务代码中混入 legacy/research 协议逻辑
- 禁止绕过安全门禁执行 SET 命令
- 禁止默认测试访问真实 FSU
- 禁止不写 spec / plan 直接编码
- 禁止不写 memory / audit 直接进入下一阶段

## 13. OpenSpec + Superpowers 工作流

### 13.1 工作流定义

本项目采用 **OpenSpec + Superpowers** 规格驱动开发工作流：

```
Spec（意图、边界、验收标准）
  → Plan（执行步骤）
    → 论证（基于 Spec/Plan）
      → TDD 实现（先测试、后代码）
        → 验证（verification-before-completion）
          → Code Review（requesting-code-review）
            → Memory 写入（执行结果）
              → Audit 写入（审计结论）
                → 下一阶段
```

### 13.2 文件对应关系

| 文件类型 | 路径 | 含义 | 生命周期 |
|----------|------|------|----------|
| **spec** | `openspec/specs/` | 意图、边界、验收标准 | 任务开始前创建，完成后归档 |
| **plan** | `openspec/plans/` | 执行步骤、验证命令 | 任务开始前创建，执行时参考 |
| **change** | `openspec/changes/` | 变更提案（需用户确认） | 强制场景必须先创建 change |
| **memory** | `docs/memory/` | 执行结果、关键决策 | 每次任务完成必须写入 |
| **audit** | `docs/audit/` | 审计结论、协议一致性 | 涉及协议/安全/联调时必须写入 |
| **archive** | `openspec/archive/` | 已完成并归档的 change | change 完成后移入 |

### 13.3 Claude 执行顺序

每次启动后按以下顺序加载：

```
1. docs/rules/CLAUDE_PROJECT_RULES.md     ← 最高规则（不可覆盖）
2. docs/rules/CLAUDE_OPENSPEC_RULES.md    ← 规格驱动层规则
3. openspec/project.md                    ← 项目上下文（本文件）
4. openspec/README.md                     ← OpenSpec 使用说明
5. docs/memory/README.md                  ← 记忆索引
6. docs/memory/WORKING-MEMORY.md          ← 当前工作记忆
7. docs/audit/                            ← 相关审计文档
8. openspec/specs/                        ← 已有能力规格
9. openspec/plans/                        ← 执行计划（如有）
10. openspec/changes/                     ← 进行中的变更（如有）
```

### 13.4 强制规则

- **每个任务必须先有 spec** — 明确意图、边界、验收标准
- **每个任务必须先有 plan** — 明确执行步骤
- **Claude 执行前必须读取对应 spec / plan**
- **完成后必须写入 docs/memory** — 执行结果和关键决策
- **Codex 审计时必须读取 spec / plan / memory / audit**
- **spec 是意图**，plan 是步骤，memory 是结果，audit 是结论

## 14. 规则文件索引

| 文件 | 用途 |
|------|------|
| `docs/rules/CLAUDE_PROJECT_RULES.md` | 最高优先级规则（21节） |
| `docs/rules/CLAUDE_OPENSPEC_RULES.md` | OpenSpec 规格驱动层规则（11节） |
| `openspec/project.md` | 项目 OpenSpec 上下文（本文件） |
| `openspec/README.md` | OpenSpec 目录使用说明 |
| `docs/memory/README.md` | 工程记忆索引 |
| `docs/memory/WORKING-MEMORY.md` | 当前工作记忆 |
| `docs/audit/` | 审计文档 |

## 15. 当前阻塞与遗留

1. FSU 不支持 GET_* 出站查询（所有 GET_* 返回空响应）
2. GET_DATA (Code=401) 返回空 DeviceList — FSU 当前无测量点位数据
3. 管理方未提供 DeviceID/SPID 点位表
4. `BInterfaceMessageLogService` 待实现（报文日志，当前为 TODO 占位）
5. `alarm_record` 缺少 `spid` 列（SPID 已提取但未入库）
6. `monitoring_point` 点位表待管理方提供数据后导入
7. SCService Controller 路径 `/api/b-interface/sc-service` 与 WSDL `/services/SCService` 不一致
8. SEND_DATA 不处理 DeviceID（仅按 SignalID 匹配），真实 FSU SEND_DATA 格式待确认
9. Codex 集中复审暂缓（累积项：BIF-P4-FIX-001 ~ LANDING-008），**不能写成已通过**
