# 快慢数据通道矩阵

本矩阵基于 B接口2016 原文第 5.1 节通信模型和各命令章节整理。

## 协议定义

| 类别 | 协议原文位置 | 通信方向 | 接口角色 | 典型内容 | 结论 |
|---|---|---|---|---|---|
| 慢数据 | 抽取稿 L186-L190 | SC -> FSU | SC 为客户端，FSU 为服务端 | 温湿度、电压、电流、电量、频率、开关状态等由 SC 轮询获取的数据 | `GET_DATA` 属于慢数据查询通道 |
| 慢数据文件 | 抽取稿 L198 | SC -> FSU FTP | SC 为 FTP 客户端，FSU 为 FTP 服务端 | 视频图像文件 | 使用 FTP，不等同于 WebService GET_DATA |
| 快数据 | 抽取稿 L201 | FSU -> SC | FSU 为客户端，SC 为服务端 | 告警、状态切换等设备事件主动上报 | `SEND_ALARM` 属于快数据上报通道 |
| 注册 / 会话 | 抽取稿 L489-L492 | FSU -> SC | FSU 为客户端，SC 为服务端 | LOGIN 注册、注册确认、重连注册 | 不属于测点数据通道，但为后续通信前置上下文 |

## 命令分类

| 命令 | 协议方向 | 快/慢数据类别 | 是否依赖注册 | 是否有频率限制 | 当前实现状态 | 备注 |
|---|---|---|---|---|---|---|
| LOGIN | FSU -> SC | 注册 / 会话 | 否，本身建立注册态 | 同一 FSU 两次注册最小间隔不小于 120 秒 | 部分实现 | 当前代码保存入站 raw，并更新 session/status；未见显式 120 秒节流 |
| LOGOUT | FSU -> SC | 注册 / 会话 | 是 | 未见独立频率限制 | 未实现 | 登出成功后 FSU 主动拆除隧道 |
| SEND_DATA | FSU -> SC | 非当前 2016 标准命令；如工程兼容存在，应归为快数据上报 | 是 | 需按工程兼容项另行定义 | 未作为标准 2016 主线 | 当前 2016 命令矩阵未列为标准 PK_Type |
| SEND_ALARM | FSU -> SC | 快数据 | 是 | 原文未给出固定轮询频率；由告警事件触发 | 部分实现 | FSU 主动上报告警，SC 返回 ACK |
| GET_DATA | SC -> FSU | 慢数据 | 是，通常需要 LOGIN 后的 FsuIP/DeviceList/在线状态 | 原文未给出 120 秒限制；由 SC 轮询策略控制 | 已实现/真实返回 TSemaphore | 虽获取“实时测点值”，但通信模型归入 SC 轮询慢数据 |
| GET_HISDATA | SC -> FSU | 慢数据 / 历史数据 | 是 | 原文说明轮询周期 1 小时，一个轮询周期只取 1 个点 | 未实现 | 历史数据查询 |
| GET_LOGININFO | SC -> FSU | 慢通道配置读取 / 注册信息查询 | 是 | 未见 120 秒限制 | 部分实现 | 敏感只读配置，需脱敏 |
| GET_FSUINFO | SC -> FSU | 慢通道状态查询 / 心跳替代 | 是 | 可作为 SC 心跳实现，频率由实现策略控制 | 已实现/部分实现 | 标准 2016 原文未定义独立 HEARTBEAT PK_Type |
| GET_FTP | SC -> FSU | 慢通道配置读取 | 是 | 未见 120 秒限制 | 部分实现 | 获取 FTP 凭据，需脱敏 |
| GET_THRESHOLD | SC -> FSU | 慢通道门限读取 | 是 | 未见 120 秒限制 | 部分实现 | 只读门限查询 |
| SET_POINT | SC -> FSU | 慢通道控制类写命令 | 是 | 非轮询命令；受安全门禁 | 安全禁用 | 高风险，禁止默认真实执行 |
| SET_THRESHOLD | SC -> FSU | 慢通道配置类写命令 | 是 | 非轮询命令；受安全门禁 | 安全禁用/部分实现 | 高风险 |
| SET_FTP | SC -> FSU | 慢通道配置类写命令 | 是 | 非轮询命令；受安全门禁 | 安全禁用 | 高风险 |
| SET_FSUREBOOT | SC -> FSU | 慢通道控制类命令 | 是 | 非轮询命令；受安全门禁 | 安全禁用 | 远程重启高风险 |

## 120 秒注册间隔解释

| 问题 | 结论 |
|---|---|
| “客户端”是否指 FSU | 是。在 LOGIN 流程中 FSU 向 SC 发起注册，因此客户端为 FSU，服务端为 SC。 |
| “服务端”是否指 SC | 是。SC 接收 LOGIN 并返回 LOGIN_ACK。 |
| FsuIP 是否用于 SC 后续访问 FSUService | 是。当前工程的 registrationContext 从最新 LOGIN raw message 解析 FsuIP 和 DeviceList，GET_DATA run-once 可用该 FsuIP 替换 endpoint host。 |
| 120 秒是否限制 GET_DATA 频率 | 否。原文限制的是同一 FSU 两次 LOGIN 注册的最小间隔，不是 GET_DATA 慢数据查询的频率限制。 |
| GET_DATA 是否必须在注册确认后执行 | 是。通信模型要求注册成功后进行数据交流；工程上还依赖 registrationContext 的 FsuIP、DeviceList 和在线状态。 |
| 120 秒重复注册是否可能间接影响 GET_DATA | 可能。若重复 LOGIN 覆盖 session/status/registrationContext，GET_DATA 目标 endpoint 或设备能力可能受影响。该影响属于实现和现场状态风险，不是 GET_DATA 协议频率规则。 |

## 本项目当前结论

`GET_DATA` 当前归属为慢数据查询通道，走 SC -> FSU 的 `FSUService` 出站调用；`SEND_ALARM` 等 FSU 主动事件上报归属快数据通道，走 FSU -> SC 的 `SCService` 入站路径。

2026-05-28 只读 `b_interface_message_log` 显示目标 FSU `51051243812345` 存在大量 32/33 秒间隔的重复 `LOGIN`，低于 120 秒协议要求；但同表未记录 GET_DATA 出站成功/失败，`b_interface_call_record` 也未记录该 FSU 的 GET_DATA 调用记录。因此，当前只能判定“重复注册是中等相关风险”，不能证明 timeout/502 主要由快慢数据通道混用或注册节流直接导致。
