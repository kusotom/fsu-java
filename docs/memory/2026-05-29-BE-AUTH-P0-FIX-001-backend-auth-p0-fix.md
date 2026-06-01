# BE-AUTH-P0-FIX-001 后端鉴权与数据隔离 P0 闭环修复

## 1. 任务目标

修复 AUTH-MATRIX-VERIFY-001 发现的 4 项 P0 遗留，闭合后端安全基础设施。

## 2. 架构判断

涉及: GlobalExceptionHandler (HTTP), RequestContext/AuthInterceptor (权限), TokenStore/SecurityContextFilter (scope 链路), DataScopeService (数据过滤), AuditLogService/SetCommandSafetyGate (审计)

## 3. 协议一致性判断

不涉及 B接口协议修改。

## 4. 修改前论证

大部分代码已在 BE-AUTH-P0-001 中实现。本次补 3 处缺口 + 1 处增强。

## 5. 写入前验证

编译通过，217 安全测试通过，全量 1450 tests (4 预存协议 failures)。

## 6. 实际修改文件

8 生产文件 + 1 测试重建 + 5 测试适配 + 4 文档

## 7. 核心改动

- FIX-001: 21 Controller 已注解 (确认), 3 白名单
- FIX-002: GlobalExceptionHandler → ResponseEntity 显式 HTTP 401/403
- FIX-005: canBypassPermissionCheck 仅 super_admin 绕过敏感权限
- FIX-003: 6 Service DataScope + TokenStore scope 链路
- FIX-004: 5 类审计事件全部接入 + SET 拦截审计补齐

## 8. 测试结果

- 安全测试: 55 tests, 0 failures
- CommandHandler: 162 tests, 0 failures (此前 6 failures 已修复)
- 全量: 1450 tests, 4 failures (预存协议), 24 skipped

## 9. 风险

低: TokenStore scope 空集合, admin-like 由 DataScopeService 语义处理

## 10. 遗留问题

P1: scope 持久化, SQL 过滤迁移, 异步审计

## 11. 下一步建议

AUTH-MATRIX-VERIFY-002 → FE-P1

## 12. git diff 摘要

8 生产 + 6 测试文件修改

## 13. git status 摘要

Modified: GlobalExceptionHandler, SetCommandSafetyGate, TokenStore, AuthService, SecurityContextFilter, BInterfaceCallRecordQueryService, SecurityInfrastructureTest + 5 测试适配

New: audit/task/memory docs
