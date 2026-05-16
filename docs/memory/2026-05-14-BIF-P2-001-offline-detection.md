---
name: BIF-P2-001-offline-detection
description: 完成离线检测/FSU 在线状态维护，新建 OfflineDetectionService/OfflineDetectionResult，扩展 Repository，新增 20 测试
metadata:
  type: project
---

# BIF-P2-001：离线检测 / FSU 在线状态维护

> 对应阶段：BIF-P2-001
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：OfflineDetectionService（新建）/ OfflineDetectionResult（新建）/ BInterfaceFsuStatusRepository（扩展 1 方法）/ OfflineDetectionServiceTest（20 测试）

**关联记忆：** [[BIF-P1-005-main-flow-integration]]

---

## 任务目标

基于 lastHeartbeat 时间戳，定时扫描所有 onlineStatus=ONLINE 的 FSU，超时则置为 OFFLINE，清除 ACTIVE Session，递增 heartbeatMissCount。默认关闭调度，仅提供服务层实现。

## 规则加载确认

- 已读取 AGENTS.md / CLAUDE.md / docs/PROJECT_ENGINEERING_RULES.md
- 已读取 docs/memory/README.md 及前序审计/记忆
- 已确认本阶段不涉及调度框架集成、不做前端变更、不涉及慢数据通道命令

## 总体架构判断

离线检测位于服务层，是快数据通道主链路的旁路维护任务。它读取 BInterfaceFsuStatusEntity 的 onlineStatus 和 lastHeartbeat 字段，复用 LoginService.clearSession() 的离线清理逻辑。

## 协议一致性判断

离线检测基于 B接口协议已有的 lastHeartbeat 字段和 onlineStatus 状态字段，不扩展协议字段，不自造状态值。超时阈值由配置控制，与协议不冲突。

## 修改前论证

**Why 不直接使用 @Scheduled？**
离线检测的调度策略（是否启用、间隔多久）应由运维决定。本阶段提供服务层实现，调度集成留待后续或由具体部署环境决定。enabled=false 为默认安全值。

**Why 新建 OfflineDetectionService 而非扩展 LoginService？**
LoginService 职责为登录/Session 管理，离线检测是批量的定时任务，关注点不同。新建服务符合单一职责原则，也便于独立测试。

**Why 在 Repository 新增 findByOnlineStatus？**
当前 BInterfaceFsuStatusRepository 仅有按 fsuId/fsuCode 查询，无按 onlineStatus 批量查询。Spring Data JPA 自动实现该查询方法。

## 写入前验证

| 验证项 | 结果 |
|--------|------|
| BInterfaceFsuStatusEntity 有 onlineStatus 字段 | 是，String 类型 |
| BInterfaceFsuStatusEntity 有 lastHeartbeat 字段 | 是，LocalDateTime 类型 |
| BInterfaceFsuStatusEntity 有 heartbeatMissCount 字段 | 是，Integer 类型 |
| BInterfaceFsuStatusRepository 可扩展 findByOnlineStatus | 是，Spring Data JPA 自动实现 |
| LoginService.clearSession() 可复用 | 是 |
| 200 测试模式可用 | 是，BaseStub 模式 |

## 实际修改文件

### 新增文件
| 文件 | 说明 |
|------|------|
| `.../binterface/service/OfflineDetectionResult.java` | 扫描结果模型 |
| `.../binterface/service/OfflineDetectionService.java` | 离线检测服务 |
| `.../binterface/OfflineDetectionServiceTest.java` | 20 测试 |
| `docs/audit/BIF-P2-001-offline-detection.md` | 审计文档 |
| `docs/memory/2026-05-14-BIF-P2-001-offline-detection.md` | 操作记忆 |

### 修改文件
| 文件 | 说明 |
|------|------|
| `.../binterface/repository/BInterfaceFsuStatusRepository.java` | 新增 findByOnlineStatus 方法 |
| `src/main/resources/application.yml` | 新增 b-interface.offline-detection 配置 |
| `.../binterface/LoginServiceTest.java` | StubFsuStatusRepository 新增 findByOnlineStatus |
| `.../binterface/BInterfaceMainFlowIntegrationTest.java` | StubFsuStatusRepository 新增 findByOnlineStatus |
| `docs/memory/README.md` | 新增索引条目 |

## 核心改动

### BInterfaceFsuStatusRepository
新增 `List<BInterfaceFsuStatusEntity> findByOnlineStatus(String onlineStatus)` 方法（Spring Data JPA 自动实现）。

### OfflineDetectionService.scanOfflineFsu()
```
findByOnlineStatus("ONLINE") → 空则返回 empty()
逐项检查 lastHeartbeat < now - timeoutSecs
  超时 → setOffline(status): session→LOGOUT, onlineStatus=OFFLINE, loginStatus=LOGOUT, missCount++
  未超时 → 加入 onlineFsuCodes
返回 OfflineDetectionResult
```

### OfflineDetectionResult
- 含 scannedCount / offlineCount / onlineCount / errorCount
- 提供 offlineFsuCodes / onlineFsuCodes 列表
- Factory: completed / completedWithErrors / empty

### application.yml
```yaml
b-interface:
  offline-detection:
    enabled: false
    heartbeat-timeout-seconds: 300
    scan-interval-seconds: 60
```

## 测试命令和结果

```bash
# 新建测试
mvn test -Dtest="com.dcim.platform.binterface.OfflineDetectionServiceTest"
# Tests run: 20, Failures: 0, Errors: 0, Skipped: 0

# 全量 B 接口测试
mvn test -Dtest="com.dcim.platform.binterface.**"
# Tests run: 379, Failures: 0, Errors: 0, Skipped: 0
```

## 风险

| 风险 | 等级 | 说明 |
|------|------|------|
| 未集成调度框架 | 低 | 默认禁用，需运维确认后开启 |
| 测试未覆盖并发场景 | 低 | 与 HEARTBEAT 同时执行可能存在竞态 |
| 无 OFFLINE 告警通知 | 低 | 可后续补充 |

## 遗留问题

1. 未集成 @Scheduled 调度 — 由 enabled=false 控制
2. 无 OFFLINE 告警/通知推送
3. 无前端 OFFLINE 展示区分

## 下一步建议

1. BIF-P2-002：慢数据通道 — GET_DATA / GET_THRESHOLD / SET_THRESHOLD 等 SC→FSU 命令
2. 按需开启调度：b-interface.offline-detection.enabled=true + @EnableScheduling

## Git diff 摘要

### BInterfaceFsuStatusRepository.java
- 新增 `findByOnlineStatus(String onlineStatus)` 方法声明

### OfflineDetectionService.java
- scanOfflineFsu(): 查询 ONLINE → 逐项比较 → setOffline
- setOffline(): 清除 session → 更新状态 → 递增 missCount

### OfflineDetectionResult.java
- completed / completedWithErrors / empty 工厂方法
- scannedCount / offlineCount / onlineCount / errorCount

### application.yml
- 新增 b-interface.offline-detection.{enabled, heartbeat-timeout-seconds, scan-interval-seconds}
