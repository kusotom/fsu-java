# SPEC-P0-001 执行报告

## 1. 本次目标

完整读取B接口2016原始协议文档，建立工程可引用、可审计、可映射到代码和测试的规范库。范围仅限协议文档、OpenSpec、审计和记忆，不修改Java、SQL、前端业务代码，不访问真实FSU，不执行SET。

## 2. 协议源文档

| 文件 | 用途 | 是否完整读取 |
|---|---|---|
| `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` | 主源docx | 是 |
| `/home/tom/桌面/FSU/B接口协议/B接口协议2016.docx` | 辅助同版docx | 是，文本一致性已核对 |
| `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl；/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` | WSDL附件辅助 | 是 |

## 3. 全文覆盖结果

| 项 | 结果 |
|---|---|
| 原文章节/处理单元数 | 38 |
| 已阅读章节数 | 38 |
| 已拆分章节数 | 38 |
| 跳过章节数 | 0 |
| 未解释章节数 | 0 |
| 覆盖率 | 100% |

详见 `openspec/protocols/binterface-2016/matrices/source-coverage-matrix.md`。

## 4. 新增 / 修改文件清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `openspec/protocols/binterface-2016/README.md` | 重写 | 规范库入口、引用规则、覆盖摘要 |
| `openspec/protocols/binterface-2016/00-*.md` ~ `09-*.md` | 新增/重写 | 总览、通信、SOAP、报文、PK_Type、Result、数据类型、枚举、错误、安全 |
| `openspec/protocols/binterface-2016/commands/*.md` | 新增/重写 | 16个命令文件，含HEARTBEAT标准未定义边界 |
| `openspec/protocols/binterface-2016/structures/*.md` | 新增 | 9个结构文件，含TTime/TGPS补充结构 |
| `openspec/protocols/binterface-2016/profiles/*.md` | 新增/重写 | standard/emerson/future-2024分离 |
| `openspec/protocols/binterface-2016/matrices/*.md` | 新增/重写 | 8个矩阵文件 |
| `openspec/protocols/binterface-2016/_legacy-2026-05-21-old-split/` | 新增归档 | 旧拆分稿保留为历史参考，不作当前依据 |
| `docs/audit/SPEC-P0-001-binterface-2016-full-protocol-specification.md` | 新增 | 本执行报告 |
| `docs/memory/2026-05-26-SPEC-P0-001-binterface-2016-full-protocol-specification.md` | 新增 | 操作记忆 |
| `docs/memory/README.md` / `WORKING-MEMORY.md` | 更新 | 索引和当前记忆 |

## 5. 命令覆盖结果

| 命令 | 是否有规范文件 | SPEC编号数量 | 当前实现状态 |
|---|---|---:|---|
| LOGIN | 是 | 5 | 部分实现 |
| LOGOUT | 是 | 3 | 未实现 |
| HEARTBEAT | 是 | 4 | 工程存在/标准未定义 |
| GET_DATA | 是 | 5 | 部分实现 |
| GET_HISDATA | 是 | 4 | 未实现 |
| SEND_ALARM | 是 | 4 | 部分实现 |
| SET_POINT | 是 | 4 | 安全禁用 |
| TIME_CHECK | 是 | 3 | 部分实现 |
| GET_LOGININFO | 是 | 3 | 部分实现 |
| SET_LOGININFO | 是 | 4 | 未实现/安全禁用 |
| GET_FTP | 是 | 3 | 部分实现 |
| SET_FTP | 是 | 4 | 未实现/安全禁用 |
| GET_FSUINFO | 是 | 3 | 已实现/部分实现 |
| SET_FSUREBOOT | 是 | 4 | 安全禁用 |
| GET_THRESHOLD | 是 | 3 | 部分实现 |
| SET_THRESHOLD | 是 | 4 | 安全禁用 |

## 6. 数据结构覆盖结果

| 数据结构 | 是否已整理 | 规范文件 |
|---|---|---|
| TAlarm | 是 | `structures/t-alarm.md` |
| TSemaphore | 是 | `structures/t-semaphore.md` |
| TThreshold | 是 | `structures/t-threshold.md` |
| TFSUStatus | 是 | `structures/t-fsu-status.md` |
| TDevice | 是 | `structures/t-device.md` |
| TSignal | 是 | `structures/t-signal.md` |
| TTime | 是 | `structures/t-time.md` |
| TGPS | 是 | `structures/t-gps.md` |
| Common Fields | 是 | `structures/common-fields.md` |

## 7. 矩阵文件结果

| 矩阵文件 | 是否生成 | 说明 |
|---|---|---|
| source-coverage-matrix.md | 是 | 全文覆盖证明 |
| command-code-matrix.md | 是 | 命令码/ACK码全集 |
| field-mapping-matrix.md | 是 | 协议字段到平台字段占位映射 |
| xml-sample-index.md | 是 | 30个XML样例索引 |
| implementation-status-matrix.md | 是 | 当前实现状态和缺口 |
| test-coverage-matrix.md | 是 | 应测点矩阵 |
| not-implemented-but-specified.md | 是 | 已规定但未实现清单 |
| unknown-and-ambiguous-items.md | 是 | 15项UNKNOWN/待确认 |

## 8. UNKNOWN / 待确认事项摘要

| 编号 | 问题 | 影响 |
|---|---|---|
| UNKNOWN-2016-001 | WSDL正文不在docx正文展开 | SOAP/WSDL实现 |
| UNKNOWN-2016-004 | Version/Vervion拼写差异 | LOGIN解析 |
| UNKNOWN-2016-005 | LOGOUT表格FsuId与空Info样例冲突 | LOGOUT实现 |
| UNKNOWN-2016-010 | SET_FTP样例重复PK_Type | SET_FTP构造 |
| UNKNOWN-2016-011 | HEARTBEAT无独立PK_Type | 心跳实现 |
| UNKNOWN-2016-015 | TGPS Lag拼写和FSUID类型差异 | GPS扩展 |

## 9. 当前未实现但协议已规定事项摘要

| 协议内容 | 当前状态 | 建议 |
|---|---|---|
| LOGOUT | 未实现 | 实现会话登出和状态转换 |
| GET_HISDATA | 未实现 | 先补fixture和时间范围限制 |
| SET_POINT | 安全禁用 | 仅dry-run+授权+审计后评估 |
| SET_LOGININFO / SET_FTP / SET_THRESHOLD | 安全禁用 | 保持默认禁用，需审批和审计 |
| SET_FSUREBOOT / 自动升级 | 安全禁用/未实现 | 禁止真实执行，单独运维专项 |
| FTP图片获取 | 未完整实现 | 补FTP只读流程和文件限制 |

## 10. 安全边界摘要

所有SET类默认禁用。SET_POINT为遥控遥调高风险；SET_THRESHOLD为门限修改高风险；SET_FTP为配置/凭据修改高风险；SET_LOGININFO为注册网络配置高风险；SET_FSUREBOOT为远程重启高风险。后续实现必须显式授权、二次确认、操作审计、保存raw报文、禁止默认调度执行、禁止测试环境误触真实设备。

## 11. 标准2016与Emerson 2016差异摘要

标准2016只依据docx原文：Result=1成功、0失败；GET_DATA=401/402；SEND_ALARM=501/502；HEARTBEAT无独立命令码。Emerson实测仅在`profiles/emerson-2016.md`记录：真实设备采用2016码表，GET_DATA返回ACK=402 Result=1但DeviceList为空，GET_FSUINFO可返回CPU/MEM，LOGIN DeviceList存在厂商兼容解析需求。

## 12. 自检结果

| 检查项 | 结果 |
|---|---|
| 是否全文阅读 | 是 |
| 是否存在跳过章节 | 否 |
| 是否所有命令覆盖 | 是 |
| 是否所有SET类命令标记高风险 | 是 |
| 是否生成UNKNOWN清单 | 是 |
| 是否生成未实现清单 | 是 |
| SPEC定义是否唯一 | 是，136个定义，0重复 |
| 是否修改业务代码 | 否 |
| 是否访问真实FSU | 否 |

## 13. 遗留问题

WSDL附件与主docx正文需长期双校验；统一信号字典中的门限、回差、延时具体参数不在本docx内；HEARTBEAT工程实现需后续审计并标注非标准2016；TGPS字段Lag需厂家/协议确认。

## 14. 下一步建议

1. 以本规范库为基准，对现有BInterfaceCommand2016、BInterfacePkType、XML parser/builder做一次Codex审计。
2. 为30个XML样例建立fixture，并按`test-coverage-matrix.md`补测试。
3. 针对Emerson Profile单独建立真实raw样本索引，不混入标准2016规范。
