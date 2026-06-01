# SPEC-DICT-P0-001 B接口2016 字典码表全量分析审计记录

## 1. 任务目标

基于 B接口2016 主源 docx 与《中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx》，补齐协议码表、设备编码、信号编码、工程兼容码值、Emerson 厂商实测差异的可审计矩阵。

## 2. 来源文件

| 文件 | 用途 | 读取结果 |
|---|---|---|
| `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` | B接口2016 主协议来源 | 已读取并按抽取稿定位 |
| `/tmp/binterface2016_docx_extract.md` | docx 抽取稿行号定位 | 已用于表格和样例定位 |
| `/home/tom/桌面/中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx` | 设备类型编码、设备信号字典、说明工作表 | 已读取 3 个工作表 |
| 当前工程源码与测试 | ResultCode 工程兼容码值来源 | 仅作工程解释来源 |
| `openspec/protocols/binterface-2016/profiles/emerson-2016.md` | Emerson 实测差异来源 | 仅作厂商实测来源 |

## 3. 输出文件

| 文件 | 操作 | 说明 |
|---|---|---|
| `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md` | 新增 | 1114 条字典码表记录，含 SPEC 编号、原始名称、原始码值、原始含义、原始位置、处理类型 |
| `openspec/protocols/binterface-2016/matrices/unknown-dictionary-items.md` | 新增 | 12 条码表和信号字典待确认项 |
| `openspec/protocols/binterface-2016/README.md` | 更新 | 增加字典码表矩阵入口和覆盖摘要 |

## 4. 覆盖摘要

| 类别 | 行数 |
|---|---:|
| 协议常量定义 | 33 |
| 协议枚举 | 55 |
| 设备编码表 | 161 |
| 报文类型定义 / PK_Type | 30 |
| LOGIN 相关枚举与厂商字典 | 60 |
| FTP 图片规则 | 7 |
| 告警文本枚举 | 6 |
| 工程 ResultCode | 13 |
| Emerson 实测非标准差异 | 3 |
| xlsx 说明 | 6 |
| xlsx 信号 ID 规则 | 7 |
| xlsx 局站类型编码 | 12 |
| xlsx 设备 / 系统类型编码 | 39 |
| xlsx 设备编码 | 152 |
| xlsx 设备信号字典 | 484 |
| 合计 | 1114 |

## 5. 待确认摘要

| 编号 | 问题 | 影响 |
|---|---|---|
| DICT-UNKNOWN-001 | `EVENT_LENGTH` 在协议常量表重复出现 | 常量唯一性与代码生成 |
| DICT-UNKNOWN-002 | 表7 室外配电设备 C类局站编码原文为 `229`，与 B类局站重复 | 设备编码解析与字典导入 |
| DICT-UNKNOWN-003 | `EnumDeviceType` 预留范围与设备编码表定义冲突 | 设备类型字典一致性 |
| DICT-UNKNOWN-004 | `GET_HISDATA_ACK` 原文方向为 `SC—>FSU` | 历史数据响应路由 |
| DICT-UNKNOWN-006 | `ResultCode` 不是 2016 docx 标准字段 | 协议判断与测试断言 |
| DICT-UNKNOWN-DUP-SIGNAL-0316005001 | xlsx 中同一信号 ID 多次出现 | SignalID 唯一键策略 |

完整清单见 `openspec/protocols/binterface-2016/matrices/unknown-dictionary-items.md`。

## 6. 安全边界

本任务未修改 Java 生产代码、SQL schema、前端页面。未访问真实 FSU，未启动 Scheduler，未执行 `SET_POINT`、`SET_THRESHOLD`、`SET_LOGININFO`、`SET_FTP`、`SET_FSUREBOOT`。

## 7. 结论

字典码表矩阵已将标准协议、工程解释、厂商实测分离记录。后续涉及命令码、ACK 码、PK_Type、Result、ResultCode、设备编码、SignalID、告警级别、信号类型和点位映射的开发与审计，必须引用 `dictionary-code-matrix.md` 中对应 SPEC 编号。
