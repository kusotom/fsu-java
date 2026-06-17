# AUTH-MATRIX-VERIFY-001 后续修复任务清单

**创建日期**: 2026-05-29
**父任务**: AUTH-MATRIX-VERIFY-001
**状态**: TODO

---

## P0 任务 (阻塞三方授权生产化)

### BE-AUTH-P0-001-FIX-001: 补齐未保护 Controller 的 @RequirePermission

**描述**: 9 个 Controller 未加 `@RequirePermission`，任何已认证用户均可访问。

**受影响 Controller**:
- `FsuDeviceController` → 建议 `@RequirePermission(Permissions.FSU_VIEW)`
- `MonitoringPointController` → 建议 `@RequirePermission(Permissions.FSU_VIEW)`
- `CabinetController` → 建议 `@RequirePermission(Permissions.SITE_VIEW)`
- `HistoryDataController` → 建议 `@RequirePermission(Permissions.REALTIME_VIEW)`
- `DeviceHeartbeatController` → 建议 `@RequirePermission(Permissions.FSU_VIEW)`
- `ActiveAlarmAuditController` → 建议 `@RequirePermission(Permissions.ALARM_VIEW)`
- `BInterfaceCommandController` → 建议 `@RequirePermission(Permissions.FSU_VIEW)`
- `BInterfaceSessionController` → 建议 `@RequirePermission(Permissions.PROTOCOL_RAW_VIEW)`
- `FtpTransferRecordController` → 建议 `@RequirePermission(Permissions.FSU_VIEW)`

**验证**: 每个 Controller 写一个测试确认未认证返回 401，无权限返回 403。

---

### BE-AUTH-P0-001-FIX-002: 修复 HTTP 401/403 真实状态码

**描述**: `GlobalExceptionHandler` 当前返回 HTTP 200 + body `{"code":401/403}`，前端无法通过 `error.response.status` 区分。

**方案**:
```java
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(401).body(ApiResponse.fail(401, e.getMessage()));
}

@ExceptionHandler(ForbiddenException.class)
public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e) {
    return ResponseEntity.status(403).body(ApiResponse.fail(403, e.getMessage()));
}
```

**验证**: `SecurityInfrastructureTest` 补充 HTTP status code 断言。

---

### BE-AUTH-P0-001-FIX-003: DataScopeService 接入 + Token 含 scope

**描述**: `DataScopeService` 定义完整但零调用点，`SecurityContextFilter` 中 scope 始终为空。

**方案**:
1. `TokenStore.TokenEntry` 增加 `stationScope`/`fsuScope` 字段
2. `AuthService.smsLogin()` 从 `UserAccountEntity` 或 `RoleEntity` 读取 scope
3. 关键 Controller (`SiteController`, `FsuDeviceController`, `AlarmRecordController`, `RealtimeDataController`) 调用 `DataScopeService.filter*()` 过滤返回数据
4. 为 `THIRD_PARTY_OPS` 角色配置有限 scope

**验证**: 构造两个用户不同 scope，确认 API 返回各自数据。

---

### BE-AUTH-P0-001-FIX-004: 审计日志接入

**描述**: `AuditLogService.logRawXmlAccess/logRawXmlDownload/logRunOnce/logSetCommandBlocked` 无调用点。

**接入点**:
1. `BInterfaceMessageLogController.getById()` → `auditLogService.logRawXmlAccess(id, fsuCode)`
2. `BInterfaceMessageLogController` 下载端点 (需新增) → `auditLogService.logRawXmlDownload()`
3. `BInterface2016ReadOnlyRunOnceController` 两个 POST 端点 → `auditLogService.logRunOnce()`
4. `SetCommandSafetyGate` 拦截点 → `auditLogService.logSetCommandBlocked()`

**验证**: 执行各操作后检查 `audit_log` 表有对应记录。

---

### BE-AUTH-P0-001-FIX-005: adminLike 语义重新评估

**描述**: `isAdminLike()` 允许 admin/super_admin/platform_admin 绕过所有权限检查。但 seed 数据中 admin 不含 raw/run-once 权限，存在语义冲突。

**方案 (二选一)**:
- **A (推荐)**: 缩小 `isAdminLike()` 为仅 `super_admin` 和 `platform_admin`，admin 走正常权限检查
- **B**: 保留 `isAdminLike()` 但给 admin 角色在 seed 数据中补充 raw/run-once 权限

**验证**: admin 用户尝试访问 `/api/b-interface/message-logs/1`，确认行为符合预期。

---

## P1 任务 (不阻塞但需在 FE-P1 前完成)

### AUTH-P0-001-P1-001: 前后端权限点对齐

前端的 `permissions.ts` 使用 `binterface.*` 格式，后端的 `Permissions.java` 使用 `protocol:*`/`site:*`/`fsu:*` 格式，两者不一致。需要建立映射表或统一命名。

### AUTH-P0-001-P1-002: run-once 测试不写 raw-samples

`BInterface2016ReadOnlyRunOnceProbeIntegrationTest` 在 `mvn test` 期间写入 `docs/landing/raw-samples/`。应加 `@Disabled` 或改为 `@Tag("real-fsu")`。

---

## 执行顺序

```
BE-AUTH-P0-001-FIX-002 (HTTP status) → FIX-001 (Controller 保护) → FIX-005 (adminLike)
    ↓
BE-AUTH-P0-001-FIX-003 (DataScope) → FIX-004 (审计接入)
    ↓
AUTH-MATRIX-VERIFY-002 (重新验证)
    ↓
FE-P1 页面建设 (仅在 P0 全部闭合后)
```
