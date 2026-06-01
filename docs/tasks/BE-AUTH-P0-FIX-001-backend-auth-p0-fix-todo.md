# BE-AUTH-P0-FIX-001 后续任务清单

**创建日期**: 2026-05-29
**状态**: P0 已闭合，以下为 P1/P2 建议

---

## 已完成 (本次修复)

- [x] FIX-001: 21 Controller 全量 @RequirePermission 核查
- [x] FIX-002: HTTP 401/403 显式 ResponseEntity
- [x] FIX-005: adminLike 敏感权限绕过治理 (canBypassPermissionCheck)
- [x] FIX-003: DataScopeService 接入 6 Service + TokenStore scope 链路
- [x] FIX-004: 审计日志接入 (raw XML view/download, run-once, SET blocked)
- [x] CommandHandler 162 tests 0 failures (6 预存失效已修复)

---

## P1 建议 (不阻塞 AUTH-MATRIX-VERIFY-002)

### P1-001: scope 持久化
当前 scope 为空集合，admin-like 通过 `isAdminLike()` 绕过。需:
1. `user_account` 表加 `station_scope`/`fsu_scope` 列
2. `AuthService.smsLogin()` 从 DB 读取 scope 传入 TokenStore
3. 管理后台提供 scope 配置 UI

### P1-002: DataScopeService 从内存过滤迁移到 SQL/Repository 层
当前为内存过滤 (`filterByFsuScope()`)，分页场景下总数泄露。建议:
1. Repository 方法增加 `fsuCode IN (:scope)` 参数
2. `Page` 查询使用 Specification 动态过滤

### P1-003: AuditLog 异步写入
当前审计日志同步写入 (`repository.save()`)，高并发下可能阻塞业务。建议:
1. `@Async` + `@EventListener` 异步写入
2. 或使用 `BlockingQueue` + 批量 flush

---

## P2 建议 (不阻塞)

### P2-001: 前后端权限码对齐
前端 `binterface.*` vs 后端 `protocol:*`/`site:*`/`fsu:*` 不一致。建议建立映射表。

### P2-002: admin 角色 seed 数据与 isAdminLike 语义一致性
admin 角色在种子数据中不含 raw/run-once 权限，但 `isAdminLike()` 在某些场景仍绕过。建议文档化差异。

### P2-003: clientIp 审计字段填充
`AuditLogEntity.clientIp` 当前未填充。建议从 `HttpServletRequest.getRemoteAddr()` 获取。

---

## 执行路线

```
AUTH-MATRIX-VERIFY-002 (复验) → P0 闭合确认 → FE-P1 页面建设 / 三方授权页面规划
                                    ↓
                              P1-001 (scope 持久化) → P1-002 (SQL 过滤) → P1-003 (异步审计)
```
