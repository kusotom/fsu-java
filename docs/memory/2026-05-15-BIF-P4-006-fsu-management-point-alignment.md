---
name: bif-p4-006-fsu-management-point-alignment
description: FSU管理协调/点位对齐审计 — 固化BIF-P4-005-C联调结果, 输出FSU协调清单/点位对齐清单/补录建议
metadata:
  type: project
---

## 任务编号
BIF-P4-006

## 任务名称
FSU 管理协调 / 点位对齐

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 规则加载确认
- [x] AGENTS.md, CLAUDE.md, docs/PROJECT_ENGINEERING_RULES.md
- [x] B接口协议 2016 唯一性确认
- [x] 禁止自造 SignalID/DeviceID/FSUCode

## 任务目标
基于 BIF-P4-005-C 联调成功结果，整理 FSUCode/SignalID/MonitoringPoint 对应关系，形成运维协调清单。

## 架构判断
纯审计/文档，不修改代码，不访问设备。

## 关键发现

1. FsuDeviceEntity: 2 条记录 (FSU-001 demo + 51051243812345 真实)
2. MonitoringPoint (fsu_id=2): 仅 2 个占位点位 (TEMP-R01, HUMI-R01)
3. GET_DATA/GET_THRESHOLD ResultCode=0 但信号为空 → SignalID 为占位值，FSU 侧不存在
4. GET_LOGININFO 无数据 → FSUCode 未在 FSU 侧注册
5. 门限/FTP 均未配置

## 输出文档
- `docs/audit/BIF-P4-006-fsu-management-point-alignment.md` — 完整审计报告

## 实际新增文件
| 文件 | 说明 |
|------|------|
| `docs/audit/BIF-P4-006-fsu-management-point-alignment.md` | FSU 管理协调/点位对齐审计 |
| `docs/memory/2026-05-15-BIF-P4-006-fsu-management-point-alignment.md` | 操作记忆 |

## 是否修改业务代码
否

## 是否涉及数据库
否（仅读取现状，未写入）

## 是否涉及前端
否

## 是否涉及 DSC/RDS
否

## 是否访问真实设备
否

## 是否执行 SET 类命令
否

## 是否启用 Scheduler
否

## 是否写入真实凭证
否

## 遗留问题
1. FSU 侧 FSUCode 待确认
2. SignalID 全部为占位值，待 FSU 管理方提供真实 ID
3. 门限/FTP/点位模板均待 FSU 管理方提供

## 建议下一步
BIF-P4-007（收到 FSU 管理方点位信息后）：生成 seed SQL，更新 monitoring_point
