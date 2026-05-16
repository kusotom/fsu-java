---
name: BIF-P1-004-send-alarm
description: 完成 SEND_ALARM 告警上报闭环，SendAlarmCommandHandler 从桩变真实实现，新增 SendAlarmService/SendAlarmResult，新增 39 测试
metadata:
  type: project
---

# BIF-P1-004：SEND_ALARM 告警上报闭环

> 对应阶段：BIF-P1-004
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：SendAlarmCommandHandler（桩→真实实现）/ SendAlarmService（新建）/ SendAlarmResult（新建）/ AlarmRecordRepository（扩展）/ SendAlarmServiceTest（20 测试）/ SendAlarmCommandHandlerTest（19 测试）

**关联记忆：** [[BIF-P1-003-send-data]], [[BIF-P1-002-heartbeat-session]], [[BIF-P1-001-login-session]]

---

## 任务目标

在 SEND_DATA 实时数据上报已建立的基础上，闭环 SEND_ALARM 命令：接收设备告警上报、解析 Alarm 项、区分告警产生/恢复、保存告警记录、返回处理结果。

## 规则加载确认

- 已读取 AGENTS.md / CLAUDE.md / docs/PROJECT_ENGINEERING_RULES.md
- 已读取 docs/memory/README.md 及前序 P0-001~P1-003 审计/记忆
- 已确认 B接口协议 2016 是唯一设备协议
- 已确认本阶段遵循 论证 → 验证 → 写入
- 已确认本阶段完成后写入 docs/memory/

## 总体架构判断

SEND_ALARM 位于命令处理层，是快数据通道第四个命令。它涉及告警解析、产生/恢复判定、告警持久化，使用 alarm 模块已有 Entity/Repository。

## 协议一致性判断

符合 B接口协议 2016 SEND_ALARM 规范：
- 请求 Info.FSUCode/AlarmTime → 提取
- 请求 xmlData Alarm 数组 → SignalID/AlarmCode/AlarmName/AlarmLevel/AlarmValue/AlarmDesc/AlarmType
- 响应 Info ResultCode/AlarmID
- 响应 xmlData 为空
- ResultCode 复用 B接口标准值（0/1002/2001/2003/5001）

AlarmType 取值（0=产生, 1=恢复）待协议原文最终校验确认。

## 修改前论证

**Why 新建 SendAlarmResult？**
与 SendDataResult 同级，额外支持 recoveredCount 和 alarmIds，满足告警特有的产生/恢复统计需求。

**Why 新建 SendAlarmService 而非复用 AlarmRecordService？**
现有 AlarmRecordService 仅有只读 CRUD（list/getById/listByStatus），不具备 SEND_ALARM 业务逻辑。SendAlarmService 作为 binterface 模块的服务，引用 alarm 模块的实体和仓库，职责边界清晰。

**Why 复用 alarm 模块 Entity/Repository？**
AlarmRecordEntity 已有完整告警字段（pointCode, alarmCode, alarmName, alarmLevel, alarmStatus, alarmValue, alarmDesc, occurTime, clearTime），无需新建模型。仅需在 Repository 中追加一个 query 方法。

## 写入前验证

| 验证项 | 结果 |
|--------|------|
| SendAlarmCommandHandler 当前状态 | 为 `return notImplemented` 桩 |
| LoginService 是否有 isLoggedIn | 有（BIF-P1-001） |
| AlarmRecordEntity 可用 | 是，字段完整 |
| AlarmRecordRepository 可用 | 是，需扩展 findActiveAlarm 方法 |
| SEND_ALARM fixtures 可用 | soap/xmldata/expected 均存在 |
| DSC/RDS 污染风险 | 无 |

## 实际修改文件

### 新增文件
| 文件 | 说明 |
|------|------|
| `.../binterface/service/SendAlarmResult.java` | 业务结果模型：success/fail/partial + accepted/recovered/rejected 统计 |
| `.../binterface/service/SendAlarmService.java` | 业务服务：processAlarms() + parseAlarmItem() + processOneAlarm() |
| `.../binterface/SendAlarmServiceTest.java` | 20 测试 |
| `.../binterface/SendAlarmCommandHandlerTest.java` | 19 测试 |
| `docs/audit/BIF-P1-004-send-alarm.md` | 审计文档 |
| `docs/memory/2026-05-14-BIF-P1-004-send-alarm.md` | 操作记忆 |

### 修改文件
| 文件 | 说明 |
|------|------|
| `.../command/SendAlarmCommandHandler.java` | 桩 → 真实实现 |
| `.../service/SendAlarmService.java` | parseAlarmTime 改为 public（跨包测试） |
| `.../alarm/repository/AlarmRecordRepository.java` | 新增 findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus |
| `docs/memory/README.md` | 新增索引条目 |

### 不修改
LoginService、SendDataService、CommandDispatcher、SoapMessageHandler、AlarmRecordEntity、FsuDeviceEntity

## 核心改动

### SendAlarmCommandHandler.handle()
```
context == null → 5001
infoXml == null/empty → 2001
FSUCode 为空 → 2001
isLoggedIn == false → 1002
extractAlarmTime (可选)
委托 SendAlarmService.processAlarms()
SendAlarmResult.isSuccess → Info: <ResultCode>0</ResultCode><AlarmID>id</AlarmID>
SendAlarmResult.fail → 透传错误码
异常 → 5001
```

### SendAlarmService.processAlarms()
```
FSU 未注册 → 1002
xmlData 为 null → 2003
items 为空 → 2003
逐项 parseAlarmItem → 校验→processOneAlarm(产生/恢复判定)
全部 rejected → 2003
部分 accepted → partial(accepted, recovered, rejected, errors)
全部 accepted → success(accepted, recovered, rejected, alarmIds)
```

### 告警产生/恢复策略
```
AlarmType=0 → 新建 AlarmRecordEntity(alarmStatus=ACTIVE)
AlarmType=1 → 查找 fsuId+pointCode+alarmCode+ACTIVE → 更新为 RECOVERED
             → 未找到时以 RECOVERED 状态创建记录
```

### AlarmRecordRepository 扩展
新增 `findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus(fsuId, pointCode, alarmCode, alarmStatus)`

## 测试命令和结果

```bash
# 新测试
mvn test -Dtest="com.dcim.platform.binterface.SendAlarmServiceTest,com.dcim.platform.binterface.SendAlarmCommandHandlerTest"
# Tests run: 39, Failures: 0, Errors: 0, Skipped: 0

# 全量 B 接口测试
mvn test -Dtest="com.dcim.platform.binterface.**"
# Tests run: 336, Failures: 0, Errors: 0, Skipped: 0
```

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| AlarmType 语义未协议原文确认 | 低 | 标记为审计文档遗留 |
| 告警恢复匹配策略简化 | 低 | 按现有单记录匹配，满足当前场景 |
| SendAlarmService 引用 alarm 模块 | 低 | 单向依赖，由 Spring 包扫描保障 |

## 遗留问题

1. AlarmType 产生/恢复取值待 B接口协议原文最终校验
2. 无 DeviceID 解析（当前 fixture 不含此字段）
3. SEND_ALARM 不更新 lastSeen（后续可追加）
4. 无告警通知推送

## 下一步建议

1. BIF-P2-001：离线检测 — 基于 lastHeartbeat 的定时扫描任务
2. BIF-P2-002：慢数据通道 — GET_DATA / SET_THRESHOLD 等 SC→FSU 命令
3. 告警确认/清除前端页面

## Git diff 摘要

### SendAlarmCommandHandler.java
- 移除 `return CommandResult.notImplemented(BInterfacePkType.SEND_ALARM)`
- 新增 LoginService + SendAlarmService 注入
- 新增 handle() 实现
- 新增 extractFsuCode() / extractAlarmTime() / buildResponse()

### SendAlarmService.java
- 新增 processAlarms() 主流程
- 新增 parseAlarmItem() — 字段提取 + 必填校验
- 新增 processOneAlarm() — 产生/恢复判定 + 持久化
- parseAlarmTime() → public

### AlarmRecordRepository.java
- 新增 findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus 查询方法

### SendAlarmResult.java
- 完整工厂：success / fail / partial
- 支持 acceptedCount / recoveredCount / rejectedCount / alarmIds
