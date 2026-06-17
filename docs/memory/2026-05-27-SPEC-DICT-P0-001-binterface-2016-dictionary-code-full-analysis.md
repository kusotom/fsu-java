# 2026-05-27 SPEC-DICT-P0-001 B接口2016 字典码表全量分析

## 任务目标

补齐 B接口2016 协议主源 docx 与设备信号字典 xlsx 的全量码表矩阵，确保命令码、ACK 码、PK_Type、Result、工程 ResultCode、设备编码、信号编码、告警级别、信号类型和 Emerson 厂商实测差异都有来源、SPEC 编号和处理类型。

## 输入来源

| 来源 | 结果 |
|---|---|
| `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` | 已读取，使用 `/tmp/binterface2016_docx_extract.md` 定位表格和样例行号 |
| `/home/tom/桌面/中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx` | 已读取 3 个工作表：说明、设备类型编码表、设备信号字典表 |
| 当前工程源码与测试 | 仅提取工程 `ResultCode` 兼容码值 |
| `profiles/emerson-2016.md` | 仅提取 Emerson 厂商实测差异 |

## 输出

| 文件 | 说明 |
|---|---|
| `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md` | 1114 条全量字典码表记录 |
| `openspec/protocols/binterface-2016/matrices/unknown-dictionary-items.md` | 12 条码表与信号字典待确认项 |
| `openspec/protocols/binterface-2016/README.md` | 增加字典码表矩阵说明和覆盖摘要 |
| `docs/audit/SPEC-DICT-P0-001-binterface-2016-dictionary-code-full-analysis.md` | 审计闭环记录 |

## 关键结论

- xlsx `设备信号字典表` 已展开为 484 条 SignalID 记录，每条保留局站类型、设备类型、逻辑分类、必测项、测试方法、信号标准名、信号类型、告警态、正常态、信号说明、信号解释、告警级别、告警延时、存储周期和阀值字段中出现的原文内容。
- xlsx `设备类型编码表` 已展开为 12 条局站类型编码、39 条设备 / 系统类型编码、152 条按局站展开的设备编码、7 条信号 ID 规则。
- xlsx `说明` 工作表已保留 6 行原文说明。
- 2016 docx 标准仍以 `Result` 为结果字段，`ResultCode` 只登记为工程解释，不作为标准 2016 强约束。
- Emerson 实测差异只登记为厂商实测，不写入 standard-2016 规则。

## 待确认

主要待确认项包括：`EVENT_LENGTH` 重复、设备编码 `229` 重复、`EnumDeviceType` 预留范围与设备编码表冲突、`GET_HISDATA_ACK` 方向疑似冲突、`Version` / `Vervion` 写法不一致、`PaSCword` 大小写异常、`DEVICEICODE_LEN` 与 `DEVICECODE_LEN` 不一致、xlsx `0316005001` 信号 ID 重复。

## 安全边界

本任务只修改协议文档、矩阵、审计与记忆文件。未修改 Java 生产代码、SQL schema、前端页面。未访问真实 FSU，未启动 Scheduler，未执行 SET 类命令。
