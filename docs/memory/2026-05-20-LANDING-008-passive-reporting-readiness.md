# LANDING-008 B接口2016 被动上报接收联调准备（实施阶段）

## 任务目标

在原始 LANDING-008（纯文档核对）基础上，执行实际的代码实现：
1. 实现 BInterfaceMessageLogService 原始报文保存能力
2. 在 ScServiceController 入口接入报文日志
3. 补齐 alarm_record.spid 列
4. 更新 SendAlarmService 写入 SPID

## 架构判断

- 涉及模块: SCService Controller, BInterfaceMessageLogService, AlarmRecordEntity, SendAlarmService
- 不涉及: CommandDispatcher, CommandHandler 核心逻辑, 其他 Service
- 不涉及: Scheduler, SET 命令, 真实 FSU 访问

## 协议一致性

- 报文日志保留完整原始 SOAP/XML（非仅有 xmlData）
- SPID 字段来源: B接口2016 协议 SEND_ALARM 的 Alarm 项
- 报文日志不参与协议处理链路，不影响 ACK 响应格式

## 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `BInterfaceMessageLogService.java` | **重写** | TODO 空壳 → 完整实现 (saveInbound/saveOutbound/findByFsuCode/findByCommand) |
| `ScServiceController.java` | **修改** | 注入 BInterfaceMessageLogService；入口处异步记录原始报文 |
| `AlarmRecordEntity.java` | **修改** | 新增 spid 字段 (@Column, nullable) |
| `SendAlarmService.java` | **修改** | buildAlarmEntity() + 恢复路径补写 spid |
| `BInterfaceMessageLogServiceTest.java` | **新增** | 11 测试: 正常保存/不回抛/SOAP完整性/查询/批量 |
| `SendAlarmServiceTest.java` | **修改** | 新增 2 测试: SPID 写入 + 恢复回填 |

## 关键决策

1. **同步保存**: 项目未启用 @Async，采用同步 try-catch（失败不抛异常）
2. **入口位置**: ScServiceController.handleScService() parse 后、dispatch 前记录
3. **完整 SOAP**: 保存原始 requestBody，非仅 xmlData
4. **spid nullable**: 兼容旧数据，不修改已有告警记录
5. **不处理 SendDataService DeviceID**: 真实格式待确认

## 测试结果

```
BInterfaceMessageLogServiceTest:  11 tests, 0 failures, 0 errors
SendAlarmServiceTest:             22 tests, 0 failures, 0 errors (+2 SPID)
*BInterface*:                    127 tests, 0 failures, 0 errors
DcimPlatformApplicationTests:     1 test,  PASS (contextLoads)
全量:                           1177 tests, 0 failures, 0 errors, 5 skipped
```

- 默认不访问真实 FSU
- 不执行 SET
- 不启用 Scheduler
- contextLoads 通过（新增构造函数参数 Bean 装配正确）

## 新增能力

| 能力 | 说明 |
|------|------|
| 入站报文日志 | FSU→SC 方向原始 SOAP 自动记录（best-effort，不阻塞 ACK） |
| SPID 持久化 | SEND_ALARM 告警 SPID 写入 alarm_record |
| 查询接口 | 按 FsuCode/Command 查询历史报文 |

## 风险

- 项目未启用 @Async，同步写日志在高并发下可能轻微影响响应时间（实测可忽略）
- spid 列仅存在于 Entity，schema SQL 未同步更新（需后续补充幂等 DDL）
- serialNo/deviceId 同样未在 schema SQL 中（LANDING-001 遗留）
- 入站报文日志未设置保留期限（后续需实现日志清理策略）

## 遗留问题

1. BInterfaceMessageLogService 日志清理策略待实现
2. alarm_record.serialNo/deviceId/spid 的 schema DDL 需补充幂等脚本
3. SEND_DATA DeviceID 处理待真实报文确认
4. Codex 集中复审暂缓（累积项：BIF-P4-FIX-001 ~ LANDING-008）

## 下一步建议

1. 补充 alarm_record 幂等 DDL 脚本（ADD COLUMN IF NOT EXISTS）
2. 实现报文日志清理策略（过期删除/归档）
3. 管理方提供 DeviceID/SPID 映射表 → 导入 monitoring_point
4. FSU 侧配置 SC 上报目标后启动被动上报联调
5. 提交 Codex 集中复审
