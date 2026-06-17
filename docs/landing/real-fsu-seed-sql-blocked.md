# TASK-001 阻塞报告：缺少真实点位表

> 日期: 2026-05-21

## 结论：无法生成正式 seed SQL

原因：项目内**不存在**真实 DeviceID/SPID/SignalID 点位数据。

## 已检查的位置

| 路径 | 状态 |
|------|------|
| `docs/landing/candidate-points/` | 空目录 |
| `docs/landing/REAL-STD-SP-DIC-TEMPLATE.csv` | 仅表头 |
| `docs/landing/REAL-SUCONFIGINSTANCE-TEMPLATE.csv` | 仅表头 |
| `docs/landing/REAL-STD-SP-CONFIG-OPTION-TEMPLATE.csv` | 仅表头 |
| `docs/landing/REAL-POINT-TABLE-MAPPING.md` | 仅字段映射文档 |
| `docs/audit/LANDING-001-*.md` | 模型补强(无实际数据) |
| `docs/audit/LANDING-002-*.md` | 配置模板(无实际数据) |

## 当前已知

| 信息 | 值 | 来源 |
|------|-----|------|
| FSUID/FsuCode | 51051243812345 | LANDING-006 GET_LOGININFO |
| DeviceID | 5 个已知 | LANDING-006 |
| SPID/SignalID | **未知** | GET_DATA 返回空 |
| 点位名称/类型/单位 | **未知** | 管理方未提供 |
| 门限值 | **未知** | GET_THRESHOLD 未获取 |

## 已生成模板

`docs/landing/real-fsu-point-seed-template.csv` — 待管理方填写后导入。

## 需要管理方提供

1. 完整 DeviceID → SPID/SignalID 映射表
2. 每个 SignalID 的: 名称、类型(AI/DI/DO)、单位、量程
3. 告警门限值 (如有)
4. 或直接提供 B接口2024 附录的 SUConfigInstance.xml / StdSPDic.xml 文件
