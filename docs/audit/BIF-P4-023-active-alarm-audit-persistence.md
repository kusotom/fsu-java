# BIF-P4-023: 活动告警审计结果持久化

## 1. 时间

2026-05-20

## 2. Codex 复审状态

**Codex 复审暂缓。**

- BIF-P4-FIX-001 / BIF-P4-021 / BIF-P4-022 Codex 复审暂缓；
- 本阶段基于 Claude 当前修复结果继续推进；
- 后续仍需 Codex 集中复审。

## 3. 目标

将 BIF-P4-021 Scheduler 和 BIF-P4-022 查询 API 当前只保存在内存（volatile）中的审计结果，落到数据库持久化体系中，使审计结果在应用重启后仍可查询。

## 4. 边界

| 边界 | 说明 |
|------|------|
| 只持久化审计结果 | 不修改 alarm_record |
| 不触发真实 FSU | 默认不访问 192.168.100.100 |
| 不启用 Scheduler | active-alarm-audit.scheduler-enabled=false |
| 不执行 SET | 不调用任何 SET_ 命令 |
| 不做前端 | 不新增 Vue 页面 |
| API 只读 | /latest /history 不触发新审计 |

## 5. 实现方案

### 5.1 架构图

```
GET /api/binterface/active-alarm-audit/status   (保持兼容)
GET /api/binterface/active-alarm-audit/latest    (新增)
GET /api/binterface/active-alarm-audit/history   (新增)
    |
    +-- ActiveAlarmAuditController
         |
         +-- ActiveAlarmAuditStatusService (memory优先 → DB回退)
         |    +-- dataSource: "memory" / "database" / "none"
         |
         +-- ActiveAlarmAuditRecordService (新增)
              +-- ActiveAlarmAuditRecordRepository (扩展)
                   +-- findTopByOrderByRunAtDesc()
                   +-- findTopByFsuCodeOrderByRunAtDesc()
                   +-- findByFsuCodeOrderByRunAtDesc(Pageable)
                   +-- findAllByOrderByRunAtDesc(Pageable)

Scheduler 链路:
ActiveAlarmAuditScheduler
    → ActiveAlarmAuditRecordService.save()
        → ActiveAlarmAuditRecordRepository.save()
```

### 5.2 Entity 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| suid | VARCHAR(128) | FSU 站点标识 |
| resultJson | TEXT | 审计结果 JSON 摘要 |

### 5.3 Repository 新增方法

- `findTopByOrderByRunAtDesc()` — 最近一次记录
- `findTopByFsuCodeOrderByRunAtDesc(String)` — 指定 FSU 最近记录
- `findByFsuCodeOrderByRunAtDesc(String, Pageable)` — 分页历史
- `findAllByOrderByRunAtDesc(Pageable)` — 全量分页

### 5.4 API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/binterface/active-alarm-audit/status` | GET | 保持兼容，新增 dataSource 字段 |
| `/api/binterface/active-alarm-audit/latest` | GET | 查询最近一次持久化审计记录 |
| `/api/binterface/active-alarm-audit/latest?fsuCode=xxx` | GET | 查询指定 FSU 最近记录 |
| `/api/binterface/active-alarm-audit/history?fsuCode=xxx&page=0&size=20` | GET | 分页历史查询（size 上限 100） |

## 6. 安全边界确认

- [x] 不访问真实 FSU
- [x] 不启用 Scheduler (默认 false)
- [x] 不执行 SET 命令
- [x] 不修改 alarm_record
- [x] 新增 API 只读
- [x] API 查询不触发 auditByQueryingFsu()
- [x] Scheduler 委托 Service 层持久化

## 7. 测试基线

- 全量: 1131 tests, 0 failures, 0 errors, 5 skipped
- ActiveAlarmAudit*: 44 tests, 0 failures
- BInterface*: 116 tests, 0 failures

## 8. 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `entity/ActiveAlarmAuditRecordEntity.java` | 修改 | 新增 suid、resultJson |
| `repository/ActiveAlarmAuditRecordRepository.java` | 修改 | 新增 4 个查询方法 |
| `service/ActiveAlarmAuditRecordService.java` | 新增 | 持久化 + 查询 Service |
| `controller/ActiveAlarmAuditController.java` | 修改 | 新增 /latest、/history |
| `dto/ActiveAlarmAuditRecordResponse.java` | 新增 | 审计记录 DTO |
| `dto/ActiveAlarmAuditStatusResponse.java` | 修改 | 新增 dataSource |
| `service/ActiveAlarmAuditScheduler.java` | 修改 | 委托 Service 持久化 |
| `service/ActiveAlarmAuditStatusService.java` | 修改 | DB 回退查询 |

## 9. 遗留问题

1. alarm_record 仍缺少 serialNo/deviceId 字段
2. FsuDeviceEntity 仍缺少 serviceUrl 字段
3. resultJson 为轻量摘要，未包含完整 DiffItem 明细
