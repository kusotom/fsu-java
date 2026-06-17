# LANDING-008 Spec：B接口2016 被动上报接收联调准备

> 规格版本：1.0
> 创建日期：2026-05-20（SPEC-INIT-001）
> 对应计划：`openspec/plans/landing-008-passive-reporting-readiness-plan.md`

---

## 1. 背景

LANDING-006 确认真实 FSU 使用 **B接口2016 码表**。使用 2016 Code 成功完成以下出站查询：

- `GET_LOGININFO` (Code=1501) → ACK 1502，获取 4 个 DeviceID
- `GET_FSUINFO` (Code=1701) → ACK 1702，获取 CPU 14.95% / MEM 62.84%
- `GET_FTP` (Code=1601) → ACK 1602，获取 FTP 配置
- `GET_DATA` (Code=401) → ACK 402，Result=1

LANDING-007 使用正确协议格式和真实 DeviceID 执行 GET_DATA (Code=401) 查询，FSU 返回 GET_DATA_ACK (Code=402) + Result=1，但 **Values / DeviceList 为空** — FSU 当前无测量点位数据可返回。

因此，获取真实监控数据的可行路径是将平台配置为 **FSU 数据上报的接收端 (SC)**，由 FSU 通过 LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM 主动推送数据到平台 SCService。

平台侧 SCService 被动上报链路在 BIF-P0~P1 阶段已实现（LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM 四个命令的 Handler + Service 均就绪），LANDING-008（只读文档任务）已完成代码链路核对。本次 SPEC-INIT-001 在已有 LANDING-008 基础上创建正式的 OpenSpec 规格和执行计划。

## 2. 目标

1. 核对平台 SCService 接收入口（Controller / WSDL / SOAP 解析）
2. 核对 LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM 完整代码链路
3. 明确 FSU 侧 SC 平台地址配置（IP:Port/路径/协议版本）
4. 明确原始上报报文保存策略（BInterfaceMessageLogService 设计）
5. 明确未映射 DeviceID / SPID 处理策略
6. 输出被动上报联调指南和检查清单

## 3. 非目标

以下明确 **不在本次范围内**：

- 不执行 SET 类命令
- 不启用 Scheduler
- 不主动访问真实 FSU（`192.168.100.100`）
- 不修改 alarm_record 状态机
- 不生成正式 seed SQL
- 不新增 demo 数据
- 不大范围重构 B接口主链路
- 不修改 Java 生产代码（SPEC-INIT-001 为纯文档任务）

## 4. 协议依据

| 项目 | 值 |
|------|-----|
| 协议版本 | B接口 2016 |
| SOAP | 1.1, RPC/encoded style, operation=invoke |
| 入站方向 | FSU → SCService (被动上报) |
| 入站命令 | LOGIN(101), HEARTBEAT, SEND_DATA, SEND_ALARM(501) |
| PK_Type 格式 | 请求 Name+Code，ACK 纯文本（与请求同名 PK_Type） |
| WSDL | `src/main/resources/wsdl/sc-service.wsdl` |

## 5. 验收标准

| # | 验收项 | 状态 |
|---|--------|------|
| 1 | 已明确 FSU → SCService 被动上报链路 | ✅ (LANDING-008 已完成) |
| 2 | 已明确 SCService 对外配置地址格式 | ✅ |
| 3 | 已明确 2016 LOGIN / HEARTBEAT / SEND_ALARM 支持状态 | ✅ |
| 4 | 如协议无 SEND_DATA，必须明确说明 | ✅ (2016 有 SEND_DATA，当前代码已支持) |
| 5 | 已明确原始报文保存策略 | ✅ (BInterfaceMessageLogService 已实现，ScServiceController 已接入) |
| 6 | 已明确未映射 DeviceID / SPID 处理策略 | ✅ (SPID 已写入 alarm_record，未映射项记录在原始报文中) |
| 7 | 已明确 FSU 侧配置清单 | ✅ |
| 8 | 未执行 SET | ✅ |
| 9 | 未启用 Scheduler | ✅ |
| 10 | 未主动访问真实 FSU | ✅ |
| 11 | memory 已写入 | ✅ (原有 LANDING-008 memory + 本次 SPEC-INIT-001 memory) |
| 12 | spec 已创建 | ← 本文件 |
| 13 | plan 已创建 | ← 对应 plan 文件 |

## 6. SCService 链路核对结果

### 6.1 入口

| 项目 | 值 |
|------|-----|
| Controller | `ScServiceController` (`POST /api/b-interface/sc-service`) |
| WSDL 声明路径 | `/services/SCService` (与实际 Controller 路径不一致) |
| WSDL 文件 | `src/main/resources/wsdl/sc-service.wsdl` |
| SOAP 解析 | `SoapMessageHandler.parse()` — 双向兼容 RPC invoke 和 document-style |
| SOAP 响应构建 | `SoapMessageHandler.buildResponse()` — document-style 格式 |

### 6.2 分发

| 项目 | 值 |
|------|-----|
| 分发器 | `CommandDispatcher` (自动发现 18 个 `CommandHandler` Bean) |
| 分发逻辑 | `handlerMap.get(pkType)` — 基于 BInterfacePkType 枚举 |
| 未匹配回退 | `UnknownCommandHandler` |

### 6.3 命令链路

| 命令 | 2016 Code | Handler | Service | 状态 |
|------|-----------|---------|---------|------|
| LOGIN | 101 | LoginCommandHandler | LoginService.login() | ✅ 就绪 |
| HEARTBEAT | — | HeartbeatCommandHandler | LoginService (复用) | ✅ 就绪 |
| SEND_DATA | — | SendDataCommandHandler | SendDataService.processData() | ✅ 就绪 |
| SEND_ALARM | 501 | SendAlarmCommandHandler | SendAlarmService.processAlarms() | ✅ 就绪 |

## 7. 风险

| # | 风险 | 可能性 | 影响 | 缓解 |
|---|------|--------|------|------|
| 1 | 2016 协议中 SEND_DATA 是否存在需核对 — **已确认：存在**，LANDING-003 已收到 SEND_DATA 结构响应 | 低 | 中 | 已核对 |
| 2 | 真实 FSU 可能只上报告警，不上报实时数据 | 中 | 高 | 等 FSU 配置 SC 后观察实际行为 |
| 3 | SC 平台地址需要现场网络可达 | 中 | 高 | 确认 192.168.100.123 与 FSU 同网段 |
| 4 | 防火墙和端口可能阻塞 | 中 | 高 | 确认 FSU → SC 8080 端口可达 |
| 5 | 未映射 DeviceID / SPID 仍需管理方点位表确认 | 高 | 中 | 暂记录在原始报文中供事后解析 |
| 6 | ACK 格式无 Name+Code（SC 入站 ACK 使用纯文本 PK_Type），FSU 可能期望 Name+Code | 低 | 中 | 联调时验证 |
| 7 | WSDL 路径 `/services/SCService` vs Controller `/api/b-interface/sc-service` 不一致 | 中 | 中 | FSU 配置时需使用实际 Controller 路径 |
| 8 | ~~BInterfaceMessageLogService 未实现~~ | ~~高~~ | ~~高~~ | ✅ **已实现**: try-catch 安全保存，不阻塞 ACK |
| 9 | SEND_DATA 不处理 DeviceID（仅按 SignalID 匹配），同 SignalID 不同 DeviceID 数据会混淆 | 中 | 中 | 待确认真实 FSU SEND_DATA 格式 |

## 8. 安全边界

| 边界 | 本次状态 |
|------|----------|
| 真实 FSU 访问 | **不访问** |
| Scheduler | **不启用** |
| SET 命令 | **不执行** |
| alarm_record 修改 | **不修改** |
| 正式 seed SQL | **不生成** |
| Java 生产代码 | **不修改** |
| Codex 复审 | **暂缓** |

## 9. 已输出文档

| 文件 | 说明 | 状态 |
|------|------|------|
| `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 被动上报集成方案 (12 节) | ✅ LANDING-008 |
| `docs/landing/FSU-SC-CONFIGURATION-GUIDE.md` | FSU 侧 SC 配置指南 | ✅ LANDING-008 |
| `docs/landing/PASSIVE-REPORTING-CHECKLIST.md` | 联调检查清单 (12 节) | ✅ LANDING-008 |
| `docs/audit/LANDING-008-passive-reporting-readiness.md` | 审计报告 | ✅ LANDING-008 |
| `docs/memory/2026-05-20-LANDING-008-passive-reporting-readiness.md` | 工程记忆 | ✅ LANDING-008 |
| `openspec/specs/landing-008-passive-reporting-readiness.md` | OpenSpec 规格 | ← 本文件 |
| `openspec/plans/landing-008-passive-reporting-readiness-plan.md` | 执行计划 | ← 对应 plan |

## 10. 已知遗留问题

1. `BInterfaceMessageLogService` ✅ 已实现（报文日志，2026-05-20 LANDING-008 实施阶段）
2. `alarm_record.spid` ✅ 已补齐（Entity + SendAlarmService，schema DDL 待同步）
3. `monitoring_point` 点位表待管理方提供数据后导入
4. GET_DATA (Code=401) DeviceList 返回空 — FSU 当前无测量点位数据
5. SCService Controller 路径与 WSDL 不一致
6. Codex 集中复审暂缓（累积项：BIF-P4-FIX-001 ~ LANDING-008）
