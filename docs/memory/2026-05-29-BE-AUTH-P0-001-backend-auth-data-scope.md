# BE-AUTH-P0-001: 后端鉴权、权限点、数据隔离和操作审计最小闭环

## 任务目标
建立后端最小安全闭环：认证→权限→数据范围→审计。不做Spring Security大重构，不引入新依赖。

## 架构判断
纯后端安全基础设施，不修改B接口2016协议主线、不访问真实FSU、不执行SET、不启Scheduler。

## 修改前论证
后端当前零安全。24个Controller全部开放，所有Service调用findAll()无数据过滤。前端guard不是安全边界。本次采用Filter+Interceptor+注解的最小方案。

## 修改文件清单

### 新增 (12个)
1. `common/security/RequestContext.java` — ThreadLocal用户上下文
2. `common/security/Permissions.java` — 15+5个权限点常量
3. `common/security/RequirePermission.java` — 权限注解
4. `common/security/SecurityContextFilter.java` — Token提取Filter
5. `common/security/AuthInterceptor.java` — 权限检查Interceptor
6. `common/security/DataScopeService.java` — 数据范围过滤
7. `common/security/audit/AuditLogEntity.java` — 审计日志实体
8. `common/security/audit/AuditLogRepository.java` — 审计日志仓库
9. `common/security/audit/AuditLogService.java` — 审计日志服务
10. `common/config/SecurityConfig.java` — Filter/Interceptor注册
11. `common/exception/UnauthorizedException.java` — 401异常
12. `common/exception/ForbiddenException.java` — 403异常

### 修改 (12个)
13. `common/exception/GlobalExceptionHandler.java` — +401/403处理
14-23. 10个Controller — +@RequirePermission
24. `database/seed/002_seed_roles_permissions.sql` — 6角色+权限初始化

### 测试 (1个)
25. `common/security/SecurityInfrastructureTest.java` — 9 tests

## 核心改动
- Filter: Authorization Bearer token → TokenStore.validate() → RequestContext
- Interceptor: @RequirePermission注解 → 权限/角色检查 → 401/403
- DataScope: admin不限制，其他用户按scope过滤，无scope→空
- AuditLog: JPA持久化PERMISSION_DENIED/RAW_XML/RUN_ONCE/SET_BLOCKED

## 测试结果
- SecurityInfrastructureTest: 9 tests, 0/0/0
- Command handler回归: 96 tests, 0/0/0
- 合计: 105 tests passed

## 风险
- Token存储仍在内存(重启丢失)
- Entity无tenantId列(内存过滤性能差)
- 未加Spring Security(无CSRF/CORS保护)
- POST/PUT/DELETE与GET共用同一权限点

## 遗留问题
- 数据范围过滤依赖token中携带scope，当前token不含此字段
- 审计日志同步写入(建议@Async)
- SOAP端点(/services/SCService)不加用户认证(被动上报需要)

## 下一步建议
P0: Token加scope字段使数据范围生效
P1: Spring Security + JWT + @PreAuthorize
P2: Entity加tenantId + SQL WHERE过滤
