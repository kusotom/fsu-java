# 协议已整理但当前未实现清单

| SPEC 编号 | 协议内容 | 所属命令 / 章节 | 当前实现状态 | 不实现原因 | 后续建议 |
|---|---|---|---|---|---|
| SPEC-2016-CMD-LOGOUT-001 | LOGOUT/LOGOUT_ACK | LOGOUT | 未实现 | 非当前主链路 | 实现会话登出和状态转换 |
| SPEC-2016-CMD-GET-HISDATA-001 | 历史数据查询 | GET_HISDATA | 未实现 | 缺真实需求/样本 | 先建立fixture和时间范围限制 |
| SPEC-2016-CMD-SET-POINT-001 | 遥控遥调写值 | SET_POINT | 安全禁用 | 控制类高风险 | 仅dry-run+授权+审计后评估 |
| SPEC-2016-CMD-SET-LOGININFO-001 | 设置注册/IPSec/SCIP信息 | SET_LOGININFO | 未实现/安全禁用 | 写网络注册配置高风险 | 严禁默认实现，需厂家确认 |
| SPEC-2016-CMD-SET-FTP-001 | 设置FTP账号密码 | SET_FTP | 安全禁用 | 写敏感配置高风险 | 仅授权审计后实现 |
| SPEC-2016-CMD-SET-FSUREBOOT-001 | 远程重启FSU | SET_FSUREBOOT | 安全禁用 | 会导致设备重启 | 禁止真实执行，先设计审批流 |
| SPEC-2016-CMD-GET-THRESHOLD-001 | 门限查询 | GET_THRESHOLD | 部分实现 | 需按2016复核 | 补fixture和真实只读验证 |
| SPEC-2016-CMD-SET-THRESHOLD-001 | 设置门限 | SET_THRESHOLD | 安全禁用/部分实现 | 写设备门限高风险 | 保持默认禁用 |
| SPEC-2016-COMM-001 | FTP图片文件获取 | 第6章FTP接口能力 | 未完整实现 | 缺FTP真实账号/文件样本 | 建立只读FTP下载流程和大小限制 |
| SPEC-2016-CMD-SET-FSUREBOOT-003 | FSU自动升级 | 第8章自动升级 | 未实现 | 升级/重启高风险 | 进入运维专项 |
| SPEC-2016-OVERVIEW-002 | 统一信号字典/门限外部参数 | 第10章门限配置 | 部分实现 | 外部字典不在本docx全文内 | 引用标准字典基线补齐 |
| SPEC-2016-STRUCT-TGPS-001 | TGPS结构 | 表8数据结构定义 | 未实现 | 当前命令未引用 | 列入未来扩展 |
| SPEC-2016-CMD-HEARTBEAT-001 | 独立HEARTBEAT命令码 | 第9章SC心跳 | 标准未定义/工程存在 | 原文无PK_Type | 工程HEARTBEAT需标注非标准 |
