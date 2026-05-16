# BIF-P2-001：离线检测 / FSU 在线状态维护

> 对应阶段：BIF-P2-001
> 完成日期：2026-05-14
> 影响范围：OfflineDetectionService（新建 20 测试）

---

## 一、本阶段目标

基于 lastHeartbeat 时间戳，定时扫描所有 onlineStatus=ONLINE 的 FSU，将其与心跳超时阈值比较，超时则置为 OFFLINE。

本阶段不涉及调度框架集成（定时扫描由 @Scheduled 或外部调度触发，默认关闭），不做前端变更，不涉及慢数据通道命令。

---

## 二、当前架构

### 2.1 改动范围

```
OfflineDetectionService
  ├── findByOnlineStatus("ONLINE") → List<BInterfaceFsuStatusEntity>
  ├── 逐项检查 lastHeartbeat < now - timeoutSecs
  │     ├── 超时 → setOffline(status)
  │     │          ├── clearSession (ACTIVE → LOGOUT)
  │     │          ├── onlineStatus=OFFLINE, loginStatus=LOGOUT
  │     │          └── heartbeatMissCount++
  │     └── 未超时 → 加入 onlineFsuCodes
  └── 返回 OfflineDetectionResult
```

### 2.2 涉及模块

| 模块 | 作用 | 影响类型 |
|------|------|---------|
| b-interface service | OfflineDetectionService（新建） | 新增 |
| b-interface service | OfflineDetectionResult（新建） | 新增 |
| b-interface repository | BInterfaceFsuStatusRepository.findByOnlineStatus | 扩展 1 方法 |
| config | application.yml b-interface.offline-detection | 新增配置 |

---

## 三、测试覆盖

### 3.1 测试类

| 测试类 | 位置 | 测试数 | 说明 |
|--------|------|--------|------|
| OfflineDetectionServiceTest | binterface/ | 20 | 离线检测服务单元测试 |

### 3.2 测试明细

| 序号 | 测试方法 | 类别 | 验证点 |
|------|---------|------|--------|
| 1 | shouldReturnEmptyWhenNoOnlineFsu | 基础 | 无 ONLINE FSU 时返回空 |
| 2 | shouldKeepOnlineFsuWithinTimeout | 基础 | 心跳未超时保持 ONLINE |
| 3 | shouldSetOfflineWhenHeartbeatExceedsTimeout | 基础 | 心跳超时置为 OFFLINE |
| 4 | shouldChangeOnlineStatusToOffline | 状态变化 | onlineStatus=OFFLINE, loginStatus=LOGOUT |
| 5 | shouldClearSessionOnOffline | 状态变化 | ACTIVE session 被清除 |
| 6 | shouldUpdateSessionStatusToLogout | 状态变化 | Session 状态更新为 LOGOUT，含 logoutTime |
| 7 | shouldIncrementHeartbeatMissCount | 状态变化 | heartbeatMissCount=0→1 |
| 8 | shouldIncrementExistingHeartbeatMissCount | 状态变化 | heartbeatMissCount=3→4 |
| 9 | shouldHandleMixedOnlineAndOffline | 混合 | 3 个 FSU 部分超时 |
| 10 | shouldHandleAllOffline | 混合 | 所有 FSU 超时 |
| 11 | shouldHandleAllOnlineWithinTimeout | 混合 | 所有 FSU 在线 |
| 12 | shouldSetOfflineWhenLastHeartbeatIsNull | 边界 | lastHeartbeat 为 null 也超时 |
| 13 | shouldHandleExactTimeoutBoundary | 边界 | 刚好等于超时边界不置离线 |
| 14 | shouldHandleOneSecondPastTimeout | 边界 | 超过 1 秒置离线 |
| 15 | shouldHandleCustomTimeout | 边界 | 自定义超时秒数 |
| 16 | shouldNotFailWhenSessionNotFound | 边界 | ONLINE 但无 ACTIVE session 不抛异常 |
| 17 | shouldNotFailWhenNoFsuStatusesExist | 边界 | 无任何状态记录不抛异常 |
| 18 | shouldNotFailWhenOfflineStatusExists | 边界 | 已有 OFFLINE 状态不被扫描 |
| 19 | resultShouldContainCorrectOfflineAndOnlineCounts | 输出 | scanned/offline/online 计数正确 |
| 20 | toStringShouldContainKeyFields | 输出 | toString 含关键字段 |

---

## 四、配置

```yaml
b-interface:
  offline-detection:
    enabled: false            # 默认关闭，需确认后开启
    heartbeat-timeout-seconds: 300  # 5 分钟无心跳视为离线
    scan-interval-seconds: 60      # 推荐扫描间隔
```

---

## 五、全量测试结果

```
Tests run: 379, Failures: 0, Errors: 0, Skipped: 0
```

较 BIF-P1-005 新增 20 测试，所有存量测试全部通过。

---

## 六、未覆盖风险

| 风险 | 说明 | 优先级 |
|------|------|--------|
| 未集成 Scheduled 调度 | 当前 enabled=false，需后续确认是否启用 @Scheduled | 低 |
| 非线程安全扫描 | 若与 HEARTBEAT 同时执行可能竞态 | 低（单实例） |
| 无告警通知 | OFFLINE 后不产生告警 | 低（P3 可做） |

---

## 七、后续建议

1. BIF-P2-002：慢数据通道 — GET_DATA / GET_THRESHOLD / SET_THRESHOLD 等 SC→FSU 命令
2. 按需开启调度：b-interface.offline-detection.enabled=true + @EnableScheduling
