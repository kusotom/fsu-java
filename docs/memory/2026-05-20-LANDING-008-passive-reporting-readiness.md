# LANDING-008 B接口2016 被动上报接收联调准备

## 任务目标
核对 SCService 接收入口 + LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM 代码链路, 输出被动上报集成方案、FSU 配置指南和联调检查清单。

## 架构判断
- 涉及模块: SCService Controller, CommandDispatcher, Handlers, Services, Repositories
- 本阶段范围: 代码链路核对 + 文档, 无代码修改
- 不涉及: FSU 出站 (FSUService), SET 命令, Scheduler

## 协议一致性
- 2016 被动上报命令: LOGIN(101), SEND_ALARM(501) 已处理
- SC 入站 ACK 使用纯文本 PK_Type (非 Name+Code), 使用请求同名 PK_Type (非 `_ACK` 后缀)
- SC 入站 SOAP 解析双向兼容 RPC invoke 和 document-style 格式

## 链路核对结果

| 命令 | Handler | Service | 状态 |
|------|---------|---------|------|
| LOGIN | LoginCommandHandler | LoginService | ✅ 就绪 |
| HEARTBEAT | HeartbeatCommandHandler | LoginService (复用) | ✅ 就绪 |
| SEND_DATA | SendDataCommandHandler | SendDataService | ✅ 就绪 |
| SEND_ALARM | SendAlarmCommandHandler | SendAlarmService | ✅ 就绪 |

## 发现的问题

| # | 问题 | 建议 |
|---|------|------|
| 1 | WSDL 路径 `/services/SCService` vs Controller `/api/b-interface/sc-service` | 配置 FSU 时用实际路径 |
| 2 | BInterfaceMessageLogService 为 TODO 占位 | 下一阶段实现报文日志 |
| 3 | SPID 已提取但未写入 alarm_record | alarm_record 缺 spid 列, 待补 |
| 4 | SEND_DATA 不处理 DeviceID | 待确认真实 FSU 格式 |

## 新增文件 (4 个文档)

| 文件 | 说明 |
|------|------|
| `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 集成方案 (12 节) |
| `docs/landing/FSU-SC-CONFIGURATION-GUIDE.md` | FSU 配置指南 |
| `docs/landing/PASSIVE-REPORTING-CHECKLIST.md` | 联调检查清单 (12 节) |
| `docs/landing/REAL-FSU-READONLY-CALL-ANALYSIS.md` | 分析报告 (更新) |

## 修改文件

业务代码: **0 修改** (纯文档任务)

## 测试

```
mvn test → 1164 tests, 0 failures, 0 errors, 5 skipped
默认不访问真实 FSU, 不执行 SET, 不启用 Scheduler
```

## 风险
- SCService 路径不一致可能导致 FSU 配置错误
- 报文日志缺失导致联调时无法回溯原始 SOAP
- 未映射 DeviceID/SPID 的 SEND_DATA 数据会静默丢弃

## 遗留问题
1. BInterfaceMessageLogService 待实现
2. alarm_record.spid 列待补充
3. monitoring_point 点位表待管理方提供数据后导入

## 下一步建议
1. 实现 BInterfaceMessageLogService 报文日志
2. 管理方提供为 monitoring_point 导入做准备
3. FSU 侧配置 SC 上报目标地址后启动联调
