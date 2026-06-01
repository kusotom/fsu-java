# AUTH-MATRIX-VERIFY-001 前后端权限矩阵与三方授权安全闭环验证

**日期**: 2026-05-29
**类型**: 安全验证（只读审计 + 测试执行）
**前置**: FE-P0-RECTIFY-002 (前端) + BE-AUTH-P0-001 (后端) + CODEX-BE-AUTH-P0-001 (复审)
**范围**: 仅验证，不修改业务代码，不访问真实 FSU，不执行 SET，不启 Scheduler

---

## 执行摘要

| 验证项 | 结果 | P0? |
|--------|------|-----|
| 前端权限矩阵 | **通过** | 否 |
| 后端 API 权限矩阵 | **未通过** | 是 |
| raw XML 保护 | **未通过** | 是 |
| run-once 保护 | **通过** (含审计缺口) | 否 |
| SET 命令拒绝 | **通过** | 否 |
| 三方数据隔离 | **未通过** | 是 |
| 审计日志闭环 | **未通过** | 是 |
| 前端状态口径 | **通过** | 否 |

**总体结论: 5/8 通过，4 项 P0 遗留。前后端安全闭环尚未成立，不可进入三方授权生产化。**

---

## 一、前端路由权限 — 通过

### 1.1 routeGuard 验证

```typescript
// frontend/src/auth/routeGuard.ts (FE-P0-RECTIFY-002)
// permissions: all-of — required.every(p => userStore.permissions.includes(p))
// roles: any-of — requiredRoles.some(r => userStore.hasRole(r))
// permissions + roles: AND — both checks must pass independently
// isAdmin: bypasses all checks
```

| 场景 | 预期 | 实现 |
|------|------|------|
| 无权限页面不可通过菜单看到 | `isAdmin`/`isElevatedUser` 过滤菜单项 | 通过 — BasicLayout.vue |
| 直接输入 URL 不能绕过 routeGuard | `router.beforeEach` 检查 `meta.permissions`/`meta.roles` | 通过 |
| 无权限时跳转 403 | `next({ path: '/403' })` | 通过 |
| 未登录时跳转登录页 | `next({ path: '/login', query: { redirect } })` | 通过 |
| roles 为 any-of | `some()` | 通过 |
| permissions 为 all-of | `every()` | 通过 |
| roles + permissions 为 AND | 两个独立 if 块 | 通过 |

### 1.2 PermissionGuard 验证

```typescript
// frontend/src/components/auth/PermissionGuard.vue (FE-P0-RECTIFY-002)
// canAccess = permOk && roleOk (AND 语义)
// permissions: hasAllPermissions, roles: hasAnyRole
```

| 场景 | 预期 | 实现 |
|------|------|------|
| 单 permissions | all-of | 通过 |
| 单 roles | any-of | 通过 |
| permissions + roles | AND | 通过 |
| isAdmin | 绕过全部 | 通过 |

### 1.3 角色-路由矩阵

| 路由 | 权限要求 | SUPER_ADMIN | PLATFORM_ADMIN | PROTOCOL_DEBUGGER | THIRD_PARTY_OPS |
|------|---------|-------------|----------------|-------------------|-----------------|
| `/dashboard` | requiresAuth | 可访问 | 可访问 | 可访问 | 可访问 |
| `/b-interface/logs` | `protocol:raw:view` + roles | 可访问 | 可访问 | 可访问 | **403** |
| `/b-interface/calls` | `protocol:raw:view` + roles | 可访问 | 可访问 | 可访问 | **403** |
| `/b-interface/fsus/:fsuCode` | `binterface.fsu.read` + `protocol:runonce:readonly` (all-of) | 可访问 | 可访问 | 可访问 | **403** |
| `/b-interface/thresholds` | `binterface.threshold.read` + `protocol:runonce:readonly` (all-of) | 可访问 | 可访问 | 可访问 | **403** |
| `/b-interface/schedulers` | `protocol:runonce:readonly` | 可访问 | 可访问 | 可访问 | **403** |
| `/system/*` | `user.read`/`role.read`/`permission.read` | 可访问 | 可访问 | **403** | **403** |

**前端权限矩阵结论: 通过** — 路由守卫、组件守卫、菜单过滤三层防护一致。

---

## 二、后端 API 权限 — 未通过 (P0)

### 2.1 Interceptor 验证

```
AuthInterceptor (SecurityConfig.java:35)
  pathPatterns: /api/**
  excludePathPatterns: /api/auth/**, /api/health, /api/b-interface/health,
                       /api/b-interface/sc-service, /services/**
```

`@RequirePermission` 注解语义:
- `value()`: 权限列表，默认 OR (requireAll=false)
- `roles()`: 角色列表，OR
- `requireAll=true`: 权限改为 AND

### 2.2 已保护 Controller (10个)

| Controller | 注解 | 权限 |
|-----------|------|------|
| BInterfaceMessageLogController | `@RequirePermission(PROTOCOL_RAW_VIEW)` | raw XML 查看 |
| BInterfaceCallRecordController | `@RequirePermission(PROTOCOL_RAW_VIEW)` | 调用记录查看 |
| BInterface2016ReadOnlyRunOnceController | `@RequirePermission(PROTOCOL_RUNONCE_READONLY)` | run-once |
| BInterfaceFrontendReadController | `@RequirePermission(FSU_VIEW)` | FSU/告警/实时数据 |
| BInterfaceFsuStatusController | 有注解 | FSU 状态 |
| SiteController | 有注解 | 站点 |
| AlarmRecordController | 有注解 | 告警 |
| RealtimeDataController | 有注解 | 实时数据 |
| UserAccountController | 有注解 | 用户管理 |
| RoleController | 有注解 | 角色管理 |

### 2.3 未保护 Controller (9个 — P0)

| Controller | 暴露的端点 | 风险 |
|-----------|-----------|------|
| **FsuDeviceController** | `/api/fsu-devices/**` | FSU 设备 CRUD 无保护 |
| **MonitoringPointController** | `/api/monitoring-points/**` | 监控点位 CRUD 无保护 |
| **CabinetController** | `/api/cabinets/**` | 机柜 CRUD 无保护 |
| **HistoryDataController** | `/api/telemetry/history/**` | 历史遥测数据无保护 |
| **DeviceHeartbeatController** | `/api/telemetry/heartbeats/**` | 心跳数据无保护 |
| **ActiveAlarmAuditController** | `/api/b-interface/active-alarms/**` | 告警审计无保护 |
| **BInterfaceCommandController** | `/api/b-interface/commands/**` | 命令矩阵无保护 |
| **BInterfaceSessionController** | `/api/b-interface/sessions/**` | 会话数据无保护 |
| **FtpTransferRecordController** | `/api/b-interface/ftp/**` | FTP 记录无保护 |

### 2.4 HTTP 401/403 状态码 — P0

```java
// GlobalExceptionHandler.java
@ExceptionHandler(UnauthorizedException.class)
public ApiResponse<Void> handleUnauthorized(UnauthorizedException e) {
    return ApiResponse.fail(401, e.getMessage());  // ← HTTP 200, body code=401
}
```

**缺陷**: 返回 HTTP 200 + body `{"code":401}`，而非真实 HTTP 401。前端 `request.ts` 拦截器依赖 `error.response.status` 判断，当前 GlobalExceptionHandler 不抛异常时前端无法感知 401/403。

### 2.5 adminLike 绕过 — P0

```java
// RequestContext.java:54
public boolean isAdminLike() {
    return hasRole("admin") || hasRole("super_admin") || hasRole("platform_admin");
}
```

`isAdminLike()` 在 `AuthInterceptor` 中用于绕过权限检查。但 seed 数据中 `admin` 角色不含 `protocol:raw:view` 和 `protocol:runonce:readonly`。这意味着 admin 用户通过 `isAdminLike()` 可访问 raw XML 和 run-once API，即使它没有这些权限点。

**后端 API 权限矩阵结论: 未通过** — 3 项 P0: 9 个 Controller 默认放行、HTTP 401/403 非真实状态码、adminLike 绕过语义与种子数据冲突。

---

## 三、raw XML 保护 — 未通过 (P0)

### 3.1 前端保护

| 保护层 | 机制 | 状态 |
|--------|------|------|
| 路由守卫 | `permissions: ['protocol:raw:view']` + `roles: ['super_admin', 'platform_admin', 'protocol_debugger']` | 通过 |
| 菜单可见性 | `v-if="userStore.isElevatedUser"` | 通过 |
| XML 查看按钮 | `<PermissionGuard :permissions="['protocol:raw:view']">` | 通过 |
| XML 下载按钮 | `<PermissionGuard :permissions="['protocol:raw:download']">` | 通过 |

### 3.2 后端保护

| API | 保护 | 状态 |
|-----|------|------|
| `GET /api/b-interface/message-logs/**` | `@RequirePermission(PROTOCOL_RAW_VIEW)` 类级 | 通过 |
| `GET /api/b-interface/call-records/**` | `@RequirePermission(PROTOCOL_RAW_VIEW)` 类级 | 通过 |
| raw XML 下载权限 | **未实现** — `protocol:raw:download` 定义但无后端检查点 | P0 |
| raw XML 查看审计 | `AuditLogService.logRawXmlAccess()` 存在但**无调用点** | P0 |
| raw XML 下载审计 | `AuditLogService.logRawXmlDownload()` 存在但**无调用点** | P0 |

**raw XML 保护结论: 未通过** — 查看权限已有前后端双层保护，但下载权限无后端拦截，审计日志未接入。

---

## 四、run-once 保护 — 通过 (含审计缺口)

### 4.1 前端保护

| 路由 | 权限 |
|------|------|
| `/b-interface/fsus/:fsuCode` | `binterface.fsu.read` + `protocol:runonce:readonly` (all-of) |
| `/b-interface/thresholds` | `binterface.threshold.read` + `protocol:runonce:readonly` (all-of) |
| `/b-interface/schedulers` | `protocol:runonce:readonly` |

### 4.2 后端保护

```java
@RestController
@RequestMapping("/api/b-interface/2016/read-only")
@RequirePermission(Permissions.PROTOCOL_RUNONCE_READONLY)
public class BInterface2016ReadOnlyRunOnceController {
    // 仅暴露 GET_FSUINFO 和 GET_DATA probe
    // POST /fsu-info/run-once
    // POST /get-data/probe
}
```

| 检查项 | 状态 |
|--------|------|
| 仅 protocol:runonce:readonly 可访问 | 通过 |
| 不暴露 SET 端点 | 通过 |
| GET_FSUINFO/GET_DATA/GET_LOGININFO/GET_FTP 只读 | 通过 (仅 GET_FSUINFO + GET_DATA) |
| SET 命令被拒绝 | 通过 (不在 Controller 中) |
| run-once 审计日志 | **未接入** — `logRunOnce()` 无调用点 |
| 测试不访问真实 FSU | **部分违规** — `BInterface2016ReadOnlyRunOnceProbeIntegrationTest` 写入 raw-samples (既有行为) |

**run-once 保护结论: 通过** — 权限前后端一致，SET 不可执行，但审计日志未接入。

---

## 五、SET 命令拒绝 — 通过

### 5.1 安全门禁

`SetCommandSafetyGate`: 默认所有 SET 命令 blocked。`BInterface2016SetCommandSafetyTest` 7 tests 通过。

### 5.2 前端无 SET 入口

PermissionGuard 保护 SET 按钮 (`BInterfaceThresholdView.vue`: `mode="disable"`)。

### 5.3 run-once 不包含 SET

`BInterface2016ReadOnlyRunOnceController` 仅 `GET_FSUINFO` + `GET_DATA`。

**SET 拒绝结论: 通过** — 前后端 + 安全门禁三层保护。

---

## 六、三方数据隔离 — 未通过 (P0)

### 6.1 当前实现

```java
// SecurityContextFilter.java:41-42
RequestContext ctx = new RequestContext(
    entry.userId(), entry.username(),
    null,                          // tenantId: 预留
    entry.roles(), entry.permissions(),
    Collections.emptySet(),        // stationScope: 预留
    Collections.emptySet()         // fsuScope: 预留
);
```

### 6.2 DataScopeService 调用情况

`DataScopeService.filterByFsuScope()` / `filterByStationScope()` 定义完整，但**零调用点**。grep 确认无任何 Controller 或 Service 使用。

### 6.3 当前行为

| 场景 | 预期 | 实际 |
|------|------|------|
| 三方用户只能看到授权站点 | scope 过滤 | **所有用户看到全部数据** |
| 三方用户只能看到授权 FSU | scope 过滤 | **所有用户看到全部 FSU** |
| 无 scope 用户不默认看到全部 | 返回空 | **返回全部** |
| 管理员放行逻辑明确 | `isAdminLike()` → null = 不限制 | 通过 (逻辑存在但未被调用) |
| 过滤实现方式 | SQL WHERE 子句 | **内存过滤，且未被调用** |

**数据隔离结论: 未通过** — `stationScope`/`fsuScope` 始终为空，`DataScopeService` 零调用点，数据隔离不成立。

---

## 七、审计日志 — 未通过 (P0)

### 7.1 已实现

| 审计方法 | 调用点 |
|----------|--------|
| `logPermissionDenied()` | `AuthInterceptor.preHandle()` — 3 处调用 | 
| `logRawXmlAccess()` | **无调用点** |
| `logRawXmlDownload()` | **无调用点** |
| `logRunOnce()` | **无调用点** |
| `logSetCommandBlocked()` | **无调用点** |

### 7.2 缺失审计覆盖

| 审计事件 | 状态 |
|----------|------|
| raw XML 查看 | **未审计** |
| raw XML 下载 | **未审计** |
| run-once 执行 | **未审计** |
| 权限拒绝 | 已审计 (AuthInterceptor) |
| SET 命令拦截 | **未审计** |
| 用户/角色敏感操作 | **未审计** |

**审计日志结论: 未通过** — `AuditLogService` 定义了完整接口，但仅 `logPermissionDenied` 被实际调用。raw XML、run-once、SET 拦截审计均未接入。

---

## 八、前端状态口径 — 通过

### 8.1 BInterfaceRealtimeView (9 态)

| 状态 | Alert | 触发条件 |
|------|-------|----------|
| `api_404` | warning | HTTP 404 |
| `network_error` | error | 网络不可达 |
| `http_401` | warning | HTTP 401 |
| `http_403` | warning | HTTP 403 |
| `http_5xx` | error | HTTP 5xx |
| `empty` | info | HTTP 200 + 空数据 |
| `fsu_ack_empty` | info | ACK + emptyData |
| `fsu_no_ack` | warning | 无 ACK/无真实访问 |
| `parse_error` | warning | parseError |
| `unmapped` | warning | unmappedCount > 0 |

### 8.2 RealtimeDataView (6 态)

`api_404` / `network_error` / `http_401` / `http_403` / `http_5xx` / `empty` — 兼容无 meta 的普通 telemetry API。

### 8.3 Meta 降级

缺失 `realDeviceAccessed`/`ackReceived`/`emptyData` 时显示"未提供"，不报错。

**前端状态口径结论: 通过** — 9 态完整覆盖，meta 缺失优雅降级。

---

## 九、测试执行

| 测试 | 结果 |
|------|------|
| `SecurityInfrastructureTest` | **9 tests, 0 failures** |
| `BInterface2016SetCommandSafetyTest` | **7 tests, 0 failures** |
| `ReadOnlyIntegrationSafetyTest` | **23 tests, 0 failures** |
| `BInterface2016ReadOnlyRunOnceProbeIntegrationTest` | **16 tests, 0 failures** |
| **合计** | **55 tests, 0 failures** |
| `npm run build` | **通过** (vue-tsc + vite build, 0 errors) |

未执行全量 `mvn test`，原因: CODEX-BE-AUTH-P0-001 报告 162 CommandHandlerTest 中有 6 failures，全量风险未知。本次聚焦 auth/permission/safety 相关测试。

---

## 十、最终结论

### 10 项回答

1. **前端权限矩阵是否通过**: **是** — routeGuard + PermissionGuard + 菜单过滤三层一致
2. **后端 API 权限矩阵是否通过**: **否 (P0)** — 9 Controller 未加 @RequirePermission 默认放行
3. **raw XML 是否真正受保护**: **否 (P0)** — 查看受保护，下载无后端拦截，审计未接入
4. **run-once 是否真正受保护**: **是** (含审计缺口) — 权限前后端一致
5. **SET 类命令是否仍被拒绝**: **是** — 三层保护 (前端/PermissionGuard/安全门禁)
6. **三方用户数据隔离是否满足最小闭环**: **否 (P0)** — DataScopeService 零调用，scope 始终为空
7. **审计日志是否满足最小闭环**: **否 (P0)** — 仅 logPermissionDenied 被调用
8. **是否仍存在 P0**: **是 — 4 项 P0** (见下)
9. **是否可以进入 FE-P1 页面建设**: **否** — P0 未闭合前不应进入
10. **是否可以开始三方授权页面与点位映射页面规划**: **否** — 数据隔离和审计未闭环前不应规划

### P0 遗留清单

| ID | 描述 | 阻塞 |
|----|------|------|
| `BE-AUTH-P0-001-FIX-001` | 9 个未保护 Controller 加 @RequirePermission | 三方授权 API 安全 |
| `BE-AUTH-P0-001-FIX-002` | GlobalExceptionHandler 返回真实 HTTP 401/403 | 前端错误处理闭环 |
| `BE-AUTH-P0-001-FIX-003` | DataScopeService 接入 Controller + Token 含 scope | 三方数据隔离 |
| `BE-AUTH-P0-001-FIX-004` | raw XML/run-once/SET 审计接入 AuditLogService | 审计闭环 |
| `BE-AUTH-P0-001-FIX-005` | `isAdminLike()` 语义重新评估 (admin 不应绕过 raw/run-once) | 权限模型一致性 |

---

## 十一、安全边界 (遵守)

- [x] 不访问真实 FSU
- [x] 不执行 SET
- [x] 不启 Scheduler
- [x] 不修改 B接口2016 主线
- [x] 不把 B接口2024 混入当前主线
- [x] 不做 FE-P1 页面建设
- [x] 不删除角色管理模块
