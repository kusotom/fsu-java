# B接口2016 协议规范库

本目录是 B接口2016 协议规范库，是后续开发、测试、审计和验收的协议依据。任何 B接口相关代码变更，必须引用本目录中的 SPEC 编号。

## 1. 目的

将《中国铁塔动环监控系统统一互联B接口技术规范（试行）V1.0，2016年9月》逐条整理为工程可引用、可审计、可映射到代码和测试的规范文件。本文档库只描述协议、工程解释、厂商实测和待确认事项，不实现业务代码。

## 2. 协议来源

| 项 | 内容 |
|---|---|
| 主源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 辅助同版文档 | `/home/tom/桌面/FSU/B接口协议/B接口协议2016.docx` |
| WSDL辅助文件 | `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl`；`/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` |
| 主源SHA256 | `eb049933533db97761aa1514cca449dc6f143a686c1425ba4ff087eb38574c1b` |
| 辅助docx SHA256 | `d481a1f0b3d695b3a875b75976490319c4db4278b599b0f2191c5601b3b71bbf` |
| 抽取稿 | `/tmp/binterface2016_docx_extract.md`，3297行，仅作定位索引 |


## 3. 当前协议基线

- B接口2016：当前主开发依据和真实落地标准。
- B接口2024：仅作为未来兼容版本，不作为当前实现主线。
- Emerson 真实设备行为：只进入 `profiles/emerson-2016.md`，不得写成标准2016规则。

## 4. 文件结构

```text
openspec/protocols/binterface-2016/
├── 00-overview.md
├── 01-communication-model.md
├── 02-soap-envelope.md
├── 03-message-structure.md
├── 04-pk-type.md
├── 05-result-code.md
├── 06-data-types.md
├── 07-enums.md
├── 08-error-handling.md
├── 09-security-boundary.md
├── commands/
├── structures/
├── profiles/
└── matrices/
```

旧版非本次规范化产物已移入 `_legacy-2026-05-21-old-split/`，仅作历史参考，不作为当前协议依据。

`matrices/dictionary-code-matrix.md` 是协议 docx 与设备信号字典 xlsx 的全量码表矩阵。它保留原始名称、原始码值、原始含义、来源位置、处理类型和输出文件；`matrices/unknown-dictionary-items.md` 记录码表、设备编码、信号编码、命令码、字段拼写和工程扩展中的待确认项。

## 5. SPEC 编号规则

编号格式：`SPEC-2016-<领域>-<序号>`。示例：

- `SPEC-2016-CMD-GET-DATA-001`
- `SPEC-2016-STRUCT-TSEMAPHORE-001`
- `SPEC-2016-SECURITY-SET-POINT-001`
- `SPEC-2016-PROFILE-EMERSON-001`

每条关键规则必须包含来源位置。规则类型分为：协议规定、工程解释、厂商实测、待确认。只有“协议规定”可作为标准 B接口2016 强约束。

## 6. 引用规则

Claude 编码任务必须在任务说明中引用相关 SPEC 编号。Codex 审计必须检查命令码、ACK码、XML层级、字段大小写、raw message 留痕、安全边界、测试覆盖，以及是否误用2024逻辑。

## 7. 禁止事项

- 禁止把 `profiles/emerson-2016.md` 的厂商实测写成标准2016规定。
- 禁止把 UNKNOWN/待确认事项写成确定规则。
- 禁止把 B接口2024 命令码用于 B接口2016 主线。
- 禁止默认执行 SET_POINT、SET_THRESHOLD、SET_FTP、SET_LOGININFO、SET_FSUREBOOT。
- 禁止测试环境误触真实 FSU。

## 8. 覆盖率摘要

| 项 | 结果 |
|---|---|
| 主源文档页数 | 48页 |
| 抽取稿行数 | 3297行 |
| 原文章节覆盖 | 100% |
| 跳过章节数 | 0 |
| 未解释章节数 | 0 |
| 命令规范文件 | 16个，其中HEARTBEAT为标准未定义/工程心跳解释文件 |
| 数据结构文件 | 9个，含TTime/TGPS补充结构 |
| XML样例索引 | 30个 |
| 字典码表矩阵 | 1114条，含协议常量/枚举/命令码/设备编码/xlsx设备信号字典/工程ResultCode/Emerson差异 |
| 字典待确认项 | 12条 |
| SET类安全边界 | 已全部标记默认禁用 |
