# BE-AUTH-P0-FIX-001 后端鉴权与数据隔离 P0 闭环修复

**日期**: 2026-05-29
**类型**: 后端 P0 安全修复
**前置**: AUTH-MATRIX-VERIFY-001 (验证发现 4 项 P0) + CODEX-BE-AUTH-P0-001 (复审)
**范围**: 仅后端，不访问真实 FSU，不执行 SET，不启 Scheduler

---

## 执行摘要

**BE-AUTH-P0-FIX-001 已完成后端 P0 修复。建议进入 AUTH-MATRIX-VERIFY-002 复验。**

| 修复项 | 发现状态 | 修复后状态 |
|--------|---------|-----------|
| FIX-001: Controller 默认放行 | 已闭合 (21/21 已注解) | **通过** |
| FIX-002: HTTP 401/403 状态码 | 已有 @ResponseStatus | **增强** — ResponseEntity 显式保证 |
| FIX-005: adminLike 敏感权限绕过 | 已实现 isSuperAdmin + canBypassPermissionCheck | **通过** |
| FIX-003: DataScopeService 接入 | 5 个 Service 已接入 | **补齐** — TokenStore scope 链路 + CallRecord |
| FIX-004: 审计日志接入 | raw XML/run-once 已接入 | **补齐** — SET 拦截审计 |

本次新增修改: GlobalExceptionHandler (ResponseEntity), SetCommandSafetyGate (AuditLog), TokenStore (scope), AuthService (scope), SecurityContextFilter (scope), BInterfaceCallRecordQueryService (DataScope)

## FIX-001: Controller 默认放行 — 已闭合

21 个业务 Controller 全部有 `@RequirePermission` 注解。3 个白名单 Controller (Health/Auth/BInterfaceHealth) 由 SecurityConfig 排除路径:

```
excludePathPatterns: /api/auth/**, /api/health, /api/b-interface/health,
                     /api/b-interface/sc-service, /services/**
```

## FIX-002: HTTP 401/403 — 已增强

`GlobalExceptionHandler` 返回 `ResponseEntity<ApiResponse<Void>>` 显式设置 HTTP 状态码:
- UnauthorizedException → HTTP 401
- ForbiddenException → HTTP 403
- MethodArgumentNotValidException → HTTP 400
- RuntimeException/Exception → HTTP 500

## FIX-005: adminLike 敏感权限绕过 — 已闭合

`RequestContext.SENSITIVE_PERMISSIONS`: 8 个敏感权限点。`canBypassPermissionCheck()`:
- super_admin → 绕过全部
- admin/platform_admin + 敏感权限 → 不允许绕过
- admin/platform_admin + 普通权限 → 允许绕过

PLATFORM_ADMIN 不自动拥有 protocol:raw:download / protocol:runonce:readonly / protocol:set:*

## FIX-003: DataScopeService 接入 — 已补齐

6 个 Service 接入 DataScopeService: BInterfaceFsuStatusQueryService, RealtimeDataService, BInterfaceMessageLogQueryService, AlarmRecordService, SiteService, BInterfaceCallRecordQueryService

TokenStore → AuthService → SecurityContextFilter → RequestContext scope 链路完整。admin-like 用户通过 `isAdminLike()` 返回 null (全部可见)。无 scope 的三方用户返回空数据。

## FIX-004: 审计日志接入 — 已补齐

5 类审计事件全部接入: raw XML view (BInterfaceMessageLogController), raw XML download (BInterfaceMessageLogController), run-once (BInterface2016ReadOnlyRunOnceController), SET blocked (SetCommandSafetyGate 6 拦截点), permission denied (AuthInterceptor)

## 测试结果

| 测试 | 结果 |
|------|------|
| SecurityInfrastructureTest | 9 tests, 0 failures |
| SetCommandSafetyTest | 7 tests, 0 failures |
| ReadOnlyIntegrationSafetyTest | 23 tests, 0 failures |
| RunOnceProbeIntegrationTest | 16 tests, 0 failures |
| CommandHandlerTest (全部) | 162 tests, 0 failures |
| 安全相关小计 | 217 tests, 0 failures |
| 全量 mvn test | 1450 tests, 4 failures (预存), 24 skipped |

全量 4 failures 为预存 BInterface 协议问题 (ResultCode 1/0 + SOAP encoding)，与本次鉴权修复无关。

## 10 项结论

1. 9 个默认放行 Controller 是否全部处理: **是**
2. 401/403 是否闭合: **是**
3. adminLike 是否仍可绕过 raw/run-once/SET: **否** — 仅 super_admin
4. raw XML view/download 是否分别受控: **是**
5. DataScopeService 是否已真实接入: **是** — 6 Service + TokenStore 链路
6. 审计是否写入 audit log: **是** — 5 类事件全部
7. CommandHandler 失败是否修复: **是** — 162 tests 0 failures
8. 是否可以执行 AUTH-MATRIX-VERIFY-002: **是**
9. 是否仍存在 P0: **否** — AUTH-MATRIX-VERIFY-001 的 4 项 P0 全部闭合
10. 是否仍禁止进入 FE-P1: **P0 已闭合** — 建议 VERIFY-002 复验后决策

## 安全边界

- [x] 不访问真实 FSU
- [x] 不执行 SET
- [x] 不启 Scheduler
- [x] 不改 B接口2016 主线
- [x] 不把 B接口2024 混入当前主线
- [x] 不做 FE-P1 页面建设
- [x] 不删除角色管理模块
