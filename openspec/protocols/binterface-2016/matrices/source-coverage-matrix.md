# B接口2016 原始文档覆盖矩阵

| 原文章节 | 原文标题 | 原文位置 | 是否已阅读 | 是否已拆分 | 输出规范文件 | 处理结果 | 备注 |
|---|---|---|---|---|---|---|---|
| 封面 | 标准名称/版本/发布单位 | 抽取稿 L5-L21；封面 | 是 | 是 | `README.md, 00-overview.md` | 已整理为背景说明 | 无 |
| 目录 | 1-10章目录 | 抽取稿 L23-L85；目录页 | 是 | 是 | `matrices/source-coverage-matrix.md` | 已整理为覆盖索引 | 无 |
| 前言 | 制定目的和适用参考 | 抽取稿 L87-L91 | 是 | 是 | `README.md, 00-overview.md` | 已整理为规范库目的 | 无 |
| 第1章 | 范围 | 抽取稿 L93-L101；目录页4 | 是 | 是 | `00-overview.md` | 已整理为SPEC-2016-OVERVIEW-001 | 图像相邻说明已读 |
| 第2章 | 规范性引用文件 | 抽取稿 L103-L116；目录页4 | 是 | 是 | `00-overview.md, matrices/unknown-and-ambiguous-items.md` | 已整理为SPEC-2016-OVERVIEW-002 | 外部引用文件不在本docx全文内 |
| 第3章 | 定义 | 抽取稿 L118-L166；目录页4-5 | 是 | 是 | `00-overview.md, structures/common-fields.md` | 已整理为术语和公共字段 | 无 |
| 第4章 | 接口 | 抽取稿 L168-L177；目录页5 | 是 | 是 | `01-communication-model.md` | 已整理为接口定位 | 接口定义图已读，图中无可抽取新增字段 |
| 第5.1章 | B接口互联/接口方式/接入双方要求 | 抽取稿 L179-L201；目录页6 | 是 | 是 | `01-communication-model.md` | 已整理为通信模型 | 无 |
| 第5.2.1章 | 报文原则/WSDL定义 | 抽取稿 L203-L220；目录页6 | 是 | 是 | `02-soap-envelope.md` | 已整理为SOAP/WSDL边界 | WSDL正文来自辅助文件 |
| 第5.2.2章 | 基本报文格式定义 | 抽取稿 L222-L234；表1 | 是 | 是 | `03-message-structure.md` | 已整理为SPEC-2016-MSG-001 | 无 |
| 第5.2.3章 | 对象模型 | 抽取稿 L236-L246 | 是 | 是 | `03-message-structure.md` | 已整理为对象模型说明 | 图像已读，未形成代码字段 |
| 第5.2.4章 | 基本定义/编码/TAlarm说明/数据类型 | 抽取稿 L248-L442；表2-4 | 是 | 是 | `06-data-types.md, structures/common-fields.md, structures/t-alarm.md` | 已整理为字段/结构/数据类型 | 若干大小写差异进入UNKNOWN |
| 第5.2.5章 | 连接建立过程 | 抽取稿 L444-L479 | 是 | 是 | `01-communication-model.md, commands/010-login.md, commands/020-logout.md` | 已整理为连接和会话规则 | Visio流程图文字已抽取并阅读 |
| 第5.2.6章 | 数据流方式 | 抽取稿 L481-L642 | 是 | 是 | `01-communication-model.md, commands/*.md` | 已整理为命令方向和处理规则 | 图示均已按相邻文字整理 |
| 第5.2.7章 | 常量定义 | 抽取稿 L655-L698；表5 | 是 | 是 | `06-data-types.md, structures/common-fields.md` | 已整理为常量和字段长度 | EVENT_LENGTH重复进入UNKNOWN |
| 第5.2.8章 | 枚举定义 | 抽取稿 L700-L846；表6 | 是 | 是 | `07-enums.md` | 已整理为枚举表 | 无 |
| 第5.2.9章 | 设备编码表 | 抽取稿 L848-L859；表7 | 是 | 是 | `07-enums.md, structures/t-signal.md` | 已整理为设备编码枚举 | 外部完整字典仍需引用统一信号字典 |
| 第5.2.10章 | 数据结构定义 | 抽取稿 L861-L892；表8 | 是 | 是 | `structures/*.md` | 已整理为结构文件 | TGPS已进入common/未实现清单 |
| 第5.2.11章 | 报文类型定义 | 抽取稿 L894-L931；表9 | 是 | 是 | `04-pk-type.md, matrices/command-code-matrix.md` | 已整理为命令码矩阵 | HEARTBEAT不在表9 |
| 第5.2.12.1 | LOGIN/LOGIN_ACK | 抽取稿 L938-L1125；表10-11；XML样例1-2 | 是 | 是 | `commands/010-login.md` | 已整理为命令规范 | Version/Vervion进入UNKNOWN |
| 第5.2.12.2 | LOGOUT/LOGOUT_ACK | 抽取稿 L1127-L1205；表12-13；XML样例3-4 | 是 | 是 | `commands/020-logout.md` | 已整理为命令规范 | Info.FsuId与空Info样例冲突进入UNKNOWN |
| 第5.2.12.3 | SEND_ALARM/SEND_ALARM_ACK | 抽取稿 L1215-L1382；表14-15；XML样例5-6 | 是 | 是 | `commands/060-send-alarm.md` | 已整理为命令规范 | 无 |
| 第5.2.12.4 | GET_DATA/GET_DATA_ACK | 抽取稿 L1390-L1558；表16-17；XML样例7-8 | 是 | 是 | `commands/040-get-data.md` | 已整理为命令规范 | FsuID/FsuId差异进入UNKNOWN |
| 第5.2.12.5 | GET_HISDATA/GET_HISDATA_ACK | 抽取稿 L1571-L1748；表18-19；XML样例9-10 | 是 | 是 | `commands/050-get-hisdata.md` | 已整理为命令规范 | 无 |
| 第5.2.12.6 | SET_POINT/SET_POINT_ACK | 抽取稿 L1761-L1951；表20-21；XML样例11-12 | 是 | 是 | `commands/070-set-point.md, 09-security-boundary.md` | 已整理为命令规范和安全边界 | 默认禁用 |
| 第5.2.12.7 | GET_THRESHOLD/GET_THRESHOLD_ACK | 抽取稿 L1975-L2145；表22-23；XML样例13-14 | 是 | 是 | `commands/150-get-threshold.md` | 已整理为命令规范 | 无 |
| 第5.2.12.8 | SET_THRESHOLD/SET_THRESHOLD_ACK | 抽取稿 L2159-L2364；表24-25；XML样例15-16 | 是 | 是 | `commands/160-set-threshold.md, 09-security-boundary.md` | 已整理为命令规范和安全边界 | 默认禁用 |
| 第5.2.12.9 | GET_LOGININFO/GET_LOGININFO_ACK | 抽取稿 L2366-L2500；表26-27；XML样例17-18 | 是 | 是 | `commands/090-get-logininfo.md` | 已整理为命令规范 | 敏感字段需脱敏 |
| 第5.2.12.10 | SET_LOGININFO/SET_LOGININFO_ACK | 抽取稿 L2502-L2640；表28-29；XML样例19-20 | 是 | 是 | `commands/100-set-logininfo.md, 09-security-boundary.md` | 已整理为命令规范和安全边界 | 命令名空格进入UNKNOWN |
| 第5.2.12.11 | GET_FTP/GET_FTP_ACK | 抽取稿 L2642-L2737；表30-31；XML样例21-22 | 是 | 是 | `commands/110-get-ftp.md` | 已整理为命令规范 | 敏感字段需脱敏 |
| 第5.2.12.12 | SET_FTP/SET_FTP_ACK | 抽取稿 L2750-L2849；表32-33；XML样例23-24 | 是 | 是 | `commands/120-set-ftp.md, 09-security-boundary.md` | 已整理为命令规范和安全边界 | 重复PK_Type进入UNKNOWN |
| 第5.2.12.13 | TIME_CHECK/TIME_CHECK_ACK | 抽取稿 L2868-L2983；表34-35；XML样例25-26 | 是 | 是 | `commands/080-time-check.md` | 已整理为命令规范 | 时间更新需审计 |
| 第5.2.12.14 | GET_FSUINFO/GET_FSUINFO_ACK | 抽取稿 L2985-L3102；表36-37；XML样例27-28 | 是 | 是 | `commands/130-get-fsuinfo.md, structures/t-fsu-status.md` | 已整理为命令和结构 | 也作为SC心跳依据 |
| 第5.2.12.15 | SET_FSUREBOOT/SET_FSUREBOOT_ACK | 抽取稿 L3104-L3208；表38-39；XML样例29-30 | 是 | 是 | `commands/140-set-fsureboot.md, 09-security-boundary.md` | 已整理为命令规范和安全边界 | 默认禁用 |
| 第6章 | FTP接口能力 | 抽取稿 L3210-L3226；目录页42 | 是 | 是 | `commands/110-get-ftp.md, commands/120-set-ftp.md, matrices/not-implemented-but-specified.md` | 已整理为FTP能力 | 图片文件规则纳入未实现清单 |
| 第7章 | FSU初始化能力 | 抽取稿 L3228-L3241；目录页43 | 是 | 是 | `matrices/not-implemented-but-specified.md` | 已阅读，当前暂不实现 | init_list.cvs原文拼写保留 |
| 第8章 | FSU自动升级能力 | 抽取稿 L3243-L3248；目录页43 | 是 | 是 | `commands/140-set-fsureboot.md, matrices/not-implemented-but-specified.md` | 已阅读，归入重启/升级未实现能力 | 默认禁用 |
| 第9章 | SC心跳功能 | 抽取稿 L3250-L3254；目录页43 | 是 | 是 | `commands/030-heartbeat.md, commands/130-get-fsuinfo.md` | 已整理为心跳边界 | 未定义HEARTBEAT码进入UNKNOWN |
| 第10章 | 门限值配置/告警回差/延时/注册模式 | 抽取稿 L3256-L3297；目录页43 | 是 | 是 | `commands/010-login.md, commands/150-get-threshold.md, commands/160-set-threshold.md, matrices/not-implemented-but-specified.md` | 已整理为门限/告警/注册模式规则 | 外部信号字典参数不在docx内，进入UNKNOWN |

## 覆盖结论

| 项 | 结果 |
|---|---|
| 原文章节/处理单元数 | 38 |
| 已阅读 | 38 |
| 已拆分 | 38 |
| 跳过章节数 | 0 |
| 未解释章节数 | 0 |
| 覆盖率 | 100% |
