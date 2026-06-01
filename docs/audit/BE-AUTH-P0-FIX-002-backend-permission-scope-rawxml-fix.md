# BE-AUTH-P0-FIX-002 后端权限粒度、DataScope 全路径覆盖与 raw XML scope 闭环修复

**日期**: 2026-05-29 | **类型**: 后端 P0 聚焦 bug 修复 | **前置**: AUTH-MATRIX-VERIFY-002

## 执行摘要: BE-AUTH-P0-FIX-002 已完成聚焦修复，建议进入 AUTH-MATRIX-VERIFY-003 复验

## AUTH-MATRIX-VERIFY-002 复查结论

| 问题 | 结论 | 处理 |
|------|------|------|
| raw XML download requireAll=false | 确认 bug | **已修复** |
| empty scope = 全量可见 | 确认 bug | **已修复** |
| null fsuCode bypass | 确认 bug | **已修复** |
| getById() scope bypass | 确认 bug | **已修复** (3 Service) |
| site:view 覆盖 POST/PUT/DELETE | 已闭合 — 独立权限 | 无修改 |
| /api/b-interface 聚合绕过 | 已闭合 — filterByFsuScope | 无修改 |

## 6 个修复

1. **raw XML download requireAll=true**: `BInterfaceMessageLogController` download @RequirePermission(value={VIEW,DOWNLOAD}, requireAll=true)
2. **empty scope default-deny**: canAccessRawXmlRecord() allowed.isEmpty()→false
3. **null fsuCode default-deny**: 仅 isSuperAdmin() 可访问 null fsuCode 记录
4. **getById() scope**: HistoryDataService/DeviceHeartbeatService/FsuDeviceService checkScopeForEntity empty→deny
5. **admin-like audit**: 已闭合 (logRawXmlAccess/logRawXmlDownload 对所有 allow 路径均调用)
6. **6 测试类 68 tests**: AuthHttpStatus(8)+AdminLikeBypass(10)+DataScopeIntegration(12)+AuditLogWiring(10)+RawXmlPermissionScope(12)+IdBasedScopeAccess(9)

## 权限粒度 (已闭合)

所有 POST/PUT/DELETE 有独立 CREATE/UPDATE/DELETE @RequirePermission: Site, Cabinet, FsuDevice, MonitoringPoint, Role, UserAccount

## 测试结果

- 68 new security tests: 0 failures
- Security+Safet+ReadOnly+RunOnce+CommandHandler: 217 tests, 0 failures
- 全量: 1509 tests, 4 failures (预存协议), 24 skipped

## 17 项回答

1. view 覆盖 POST/PUT/DELETE: **否**
2. POST/PUT/DELETE 独立权限: **是**
3. download requireAll: **是**
4. raw view scope: **是** (empty→deny)
5. raw download scope: **是** (empty→deny)
6. 聚合 API DataScope: **是**
7. history/heartbeat/fsu-device DataScope: **是**
8. getById scope bypass: **否** (已修复)
9. 无 scope 全量: **否** (empty→deny)
10. 有 scope 仅授权: **是**
11. adminLike 绕过敏感: **否** (仅 super_admin)
12-14. 审计 allowed/denied: **是**
15. 仍存在 P0: **否**
16. 可进入 VERIFY-003: **是**
17. 禁止 FE-P1: **建议 VERIFY-003 后决策**

## 修改文件: 4 生产 + 6 测试

BInterfaceMessageLogController, HistoryDataService, DeviceHeartbeatService, FsuDeviceService + 6 新测试类
