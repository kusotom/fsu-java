# LANDING-011 Spec：FSU → SCService 被动上报真实联调

> 规格版本：1.0
> 创建日期：2026-05-20
> 对应计划：`openspec/plans/landing-011-passive-reporting-real-call-plan.md`

---

## 1. 背景

LANDING-006 确认真实 FSU 使用 B接口2016 码表。LANDING-007 确认 GET_DATA 路由正确但无测量数据。LANDING-008~010 补齐了 SCService 入站报文日志、spid 写入、DDL 对齐、查询增强和清理能力。

现场现在可以配置 FSU 上报地址，需要启动平台侧的被动上报接收入口，等待 FSU 主动推送 LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM。

## 2. 目标

1. 核对平台 SCService 实际对外访问地址（不能猜，以代码为准）
2. 输出给现场配置 FSU 上报目标的准确 URL
3. 启动平台后等待 FSU 主动上报
4. 接收并保存 LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM 原始报文
5. 通过 BInterfaceMessageLog 查询确认入站报文
6. 检查 SEND_ALARM 是否写入 serialNo / deviceId / spid
7. 检查 SEND_DATA 是否能解析 DeviceID / SPID / Value
8. 生成真实被动上报联调报告

## 3. 非目标

- 不执行 SET_POINT / SET_THRESHOLD / SET_TIME / SET_SCIP / 任何 SET_
- 不启用 Scheduler
- 不主动轮询真实 FSU (不调用 GET_* 出站命令)
- 不自动清理报文日志
- 不修改 alarm_record 状态机
- 不生成正式 seed SQL
- 不大范围修改 Java 代码（除非发现 SCService 路径必须兼容）

## 4. 验收标准

| # | 验收项 |
|---|--------|
| 1 | SCService 实际暴露地址已核对（基于代码，非猜测） |
| 2 | FSU 侧配置 URL 已明确 |
| 3 | 已收到并保存 FSU 主动上报原始 SOAP 报文（至少 1 种命令类型） |
| 4 | BInterfaceMessageLog 查询可检索到入站报文 |
| 5 | SEND_ALARM 入站后 alarm_record.serialNo/deviceId/spid 写入验证 |
| 6 | 未执行 SET |
| 7 | 未启用 Scheduler |
| 8 | 未映射 DeviceID/SPID 已记录 |
| 9 | memory + audit + real-call-report 已写入 |

## 5. 安全边界

| 边界 | 状态 |
|------|------|
| SET 命令 | **禁止** — 所有 SET_* 安全开关保持 false |
| Scheduler | **禁止** — scheduler-enabled: false |
| 主动 GET_* | **禁止** — 不调用 FSUService |
| alarm_record 状态机 | **不修改** — 仅观察 FSU 上报驱动写入 |
| seed SQL | **不生成** |
| 自动清理 | **不执行** |

## 6. 风险

| # | 风险 | 可能性 | 缓解 |
|---|------|--------|------|
| 1 | FSU 未配置 SC 上报地址 | 高 | 输出精确的 FSU 配置指令给现场 |
| 2 | 防火墙/网络不通 | 中 | 先确认网络可达性 |
| 3 | FSU 不上报某些命令（如 SEND_DATA） | 中 | 报告记录缺失项 |
| 4 | WSDL 路径 /services/SCService 无 POST 映射 | **已确认** | 使用实际 POST 路径 /api/b-interface/sc-service |
| 5 | SEND_DATA 报文格式与当前解析逻辑不匹配 | 中 | 记录原始报文供后续分析 |
| 6 | FSU 配置后需要重启 | 中 | 告知现场配置后重启 FSU |

## 7. 回滚策略

- 平台侧无需回滚（被动接收，无主动操作）
- 如需停止接收，关闭平台或通知现场恢复原 SC 地址即可
- 恢复原 SC 地址后 FSU 恢复原上报目标
