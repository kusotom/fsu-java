---
name: bif-p4-007-a-fsu-signal-collection-template
description: FSU点位采集模板/运维协调包 — CSV模板, 补录规则, FSU管理方资料请求清单, 禁止自造SignalID说明
metadata:
  type: project
---

## 任务编号
BIF-P4-007-A

## 任务名称
FSU 点位资料采集模板 / 运维协调包

## 操作时间
2026-05-15

## 任务目标
生成给 FSU 管理方使用的点位采集模板和运维协调说明。不修改代码，不生成 seed SQL。

## 输出内容
- CSV 模板：14 列（fsu_code ~ remark）
- FSU 资料请求清单：设备信息 + 通信参数 + 点位清单
- 平台补录规则：SignalID=pointCode, 禁止自造
- BIF-P4-007-B 启动条件：6 项全部等待 FSU 管理方

## 实际新增文件
| 文件 | 说明 |
|------|------|
| `docs/audit/BIF-P4-007-A-fsu-signal-collection-template.md` | 采集模板/运维协调包 |
| `docs/memory/2026-05-15-BIF-P4-007-A-fsu-signal-collection-template.md` | 操作记忆 |

## 是否修改业务代码
否

## 是否访问真实设备
否

## 是否生成 seed SQL
否（等待真实数据）

## 是否执行 SET 类命令
否

## 遗留问题
同 BIF-P4-006：FSUCode/SignalID/门限/点位模板均待 FSU 管理方

## 建议下一步
- 等待点位表: BIF-P4-007-B (生成 seed SQL)
- 并行推进: BIF-P4-008 (SCService 入站 RPC 兼容评估)
