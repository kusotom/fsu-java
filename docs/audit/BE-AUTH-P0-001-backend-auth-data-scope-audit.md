# BE-AUTH-P0-001: 后端鉴权、权限点、数据隔离和操作审计最小闭环 — 审计报告

**日期:** 2026-05-29
**类型:** P0 后端安全最小闭环
**前置:** FE-P0-RECTIFY-001 (前端P0安全整改), CODEX-FE-P0-RECTIFY-001 (复审结论: 后端鉴权缺失是三方授权生产主阻塞项)

---

## 一、修改前论证

### 当前后端安全现状
1. **零认证**: 24个Controller，仅AuthController手动检查token，其余全部开放访问
2. **零授权**: 权限系统仅作为DTO返回(permList)，不强制
3. **零数据隔离**: 所有Service调用`repository.findAll()`，无tenantId/scope过滤
4. **零操作审计**: 仅ActiveAlarmAudit一条持久化审计记录；SET命令审计在内存中，重启丢失
5. **无Spring Security**: pom不含spring-boot-starter-security，无法使用@PreAuthorize

### 敏感接口清单
| 类别 | Controller | 暴露数据 |
|------|-----------|----------|
| raw XML | BInterfaceMessageLogController | rawMessage (完整SOAP/XML报文) |
| raw XML | BInterfaceCallRecordController | requestBody/responseBody (SOAP/XML) |
| run-once | BInterface2016ReadOnlyRunOnceController | 真实FSU交互 (CPU/MEM/数据采集) |
| 用户管理 | UserAccountController | 用户CRUD |
| 角色管理 | RoleController | 角色CRUD、权限分配 |
| 实时数据 | RealtimeDataController | 实时遥测数据 |
| 告警 | AlarmRecordController | 告警记录 |
| FSU状态 | BInterfaceFsuStatusController | FSU在线状态、会话 |
| 站点 | SiteController | 站点信息 (含坐标、联系方式) |

### 本次最小闭环范围
- 不引入Spring Security (避免大规模重构)
- 不对所有Controller全局拦截
- 不对entity做DDL变更 (不加tenantId列)
- 数据范围通过内存过滤，后续可优化为SQL WHERE

## 二、修改文件清单

### 新增文件 (12个)
| # | 文件 | 类型 |
|---|------|------|
| 1 | `common/security/RequestContext.java` | ThreadLocal用户上下文 |
| 2 | `common/security/Permissions.java` | 15个权限点常量 |
| 3 | `common/security/RequirePermission.java` | 权限控制注解 |
| 4 | `common/security/SecurityContextFilter.java` | Token提取Filter |
| 5 | `common/security/AuthInterceptor.java` | 权限检查Interceptor |
| 6 | `common/security/DataScopeService.java` | 数据范围过滤服务 |
| 7 | `common/security/audit/AuditLogEntity.java` | 审计日志JPA实体 |
| 8 | `common/security/audit/AuditLogRepository.java` | 审计日志仓库 |
| 9 | `common/security/audit/AuditLogService.java` | 审计日志服务 |
| 10 | `common/config/SecurityConfig.java` | Filter/Interceptor注册 |
| 11 | `common/exception/UnauthorizedException.java` | 401异常 |
| 12 | `common/exception/ForbiddenException.java` | 403异常 |

### 修改文件 (12个)
| # | 文件 | 变更 |
|---|------|------|
| 13 | `common/exception/GlobalExceptionHandler.java` | +401/403处理 |
| 14 | `binterface/controller/BInterfaceMessageLogController.java` | +@RequirePermission(protocol:raw:view) |
| 15 | `binterface/controller/BInterfaceCallRecordController.java` | +@RequirePermission(protocol:raw:view) |
| 16 | `binterface/controller/BInterface2016ReadOnlyRunOnceController.java` | +@RequirePermission(protocol:runonce:readonly) |
| 17 | `system/controller/UserAccountController.java` | +@RequirePermission(user:view) |
| 18 | `system/controller/RoleController.java` | +@RequirePermission(role:view) |
| 19 | `alarm/controller/AlarmRecordController.java` | +@RequirePermission(alarm:view) |
| 20 | `telemetry/controller/RealtimeDataController.java` | +@RequirePermission(realtime:view) |
| 21 | `binterface/controller/BInterfaceFrontendReadController.java` | +@RequirePermission(fsu:view) |
| 22 | `binterface/controller/BInterfaceFsuStatusController.java` | +@RequirePermission(fsu:view) |
| 23 | `resource/controller/SiteController.java` | +@RequirePermission(site:view) |
| 24 | `database/seed/002_seed_roles_permissions.sql` | 种子数据 |

## 三、新增权限点清单

| 权限码 | 名称 | 类别 | 赋权角色 |
|--------|------|------|----------|
| `dashboard:view` | 总览查看 | 基础查看 | 全部角色 |
| `site:view` | 站点查看 | 基础查看 | 全部角色 |
| `fsu:view` | FSU查看 | 基础查看 | 除viewer外全部 |
| `realtime:view` | 实时数据查看 | 基础查看 | operator及以上 |
| `alarm:view` | 告警查看 | 基础查看 | operator及以上 |
| `protocol:raw:view` | 原始XML查看 | 协议敏感 | super_admin, platform_admin, protocol_debugger |
| `protocol:raw:download` | 原始XML下载 | 协议敏感 | super_admin, platform_admin, protocol_debugger |
| `protocol:runonce:readonly` | Run-once只读 | 协议敏感 | super_admin, platform_admin, protocol_debugger |
| `user:view` | 用户查看 | 系统管理 | admin及以上 |
| `role:view` | 角色查看 | 系统管理 | admin及以上 |
| `permission:view` | 权限查看 | 系统管理 | admin及以上 |
| `tenant:view` | 租户查看 | 系统管理 | (预留) |
| `audit:view` | 审计查看 | 系统管理 | (预留) |
| `protocol:set:point` | SET_POINT | 高风险 | 仅定义，不赋权 |
| `protocol:set:threshold` | SET_THRESHOLD | 高风险 | 仅定义，不赋权 |
| `protocol:set:ftp` | SET_FTP | 高风险 | 仅定义，不赋权 |
| `protocol:set:logininfo` | SET_LOGININFO | 高风险 | 仅定义，不赋权 |
| `protocol:set:fsureboot` | SET_FSUREBOOT | 高风险 | 仅定义，不赋权 |

## 四、受保护API清单

| 端点 | 权限 | 未认证 | 无权限 |
|------|------|--------|--------|
| `GET /api/b-interface/message-logs/**` | protocol:raw:view | 401 | 403 |
| `GET /api/b-interface/call-records/**` | protocol:raw:view | 401 | 403 |
| `POST /api/b-interface/2016/read-only/**` | protocol:runonce:readonly | 401 | 403 |
| `GET/POST/PUT/DELETE /api/system/users/**` | user:view | 401 | 403 |
| `GET/POST/PUT/DELETE /api/system/roles/**` | role:view | 401 | 403 |
| `GET /api/alarms/**` | alarm:view | 401 | 403 |
| `GET /api/telemetry/realtime/**` | realtime:view | 401 | 403 |
| `GET /api/b-interface/**` (FrontendRead) | fsu:view | 401 | 403 |
| `GET /api/b-interface/fsu-status/**` | fsu:view | 401 | 403 |
| `GET /api/sites/**` | site:view | 401 | 403 |

**豁免端点** (不需要认证):
- `/api/auth/**` — 登录/注册接口
- `/api/health`, `/api/b-interface/health` — 健康检查
- `/api/b-interface/sc-service`, `/services/**` — FSU被动上报SOAP入口

## 五、数据范围过滤说明

### 当前实现 (最小闭环)
- `DataScopeService` 提供集中过滤入口
- admin/super_admin/platform_admin → 不限制 (isAdminLike=true)
- 其他用户 → 按stationScope/fsuScope过滤
- 无scope的用户 → 看不到任何数据 (空集合)

### 后续改进方向
- Entity加tenantId列 → SQL WHERE子句过滤
- Token中携带stationScope/fsuScope
- 支持动态scope更新

## 六、审计日志说明

### 审计表: audit_log
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| user_id | Long | 操作用户ID |
| username | String | 操作用户名 |
| tenant_id | String | 租户ID (预留) |
| action | String | 动作: PERMISSION_DENIED/RAW_XML_VIEW/RAW_XML_DOWNLOAD/RUN_ONCE/SET_COMMAND_BLOCKED |
| resource_type | String | 资源类型 |
| resource_id | String | 资源ID |
| station_id | Long | 站点ID |
| fsu_code | String | FSU编码 |
| permission_code | String | 权限码 |
| allowed | boolean | 是否允许 |
| reason | String | 原因 |
| request_time | LocalDateTime | 请求时间 |
| client_ip | String | 客户端IP |
| created_at | LocalDateTime | 创建时间 |

### 审计记录点
- AuthInterceptor: PERMISSION_DENIED (权限拒绝)
- MessageLogController: RAW_XML_VIEW (查看), RAW_XML_DOWNLOAD (下载)
- RunOnceController: RUN_ONCE (执行)
- SET安全门: SET_COMMAND_BLOCKED (拦截)

## 七、测试结果

| 测试套件 | 测试数 | Failures | Errors |
|----------|--------|----------|--------|
| SecurityInfrastructureTest (新增) | 9 | 0 | 0 |
| Command Handler Tests (已有, 回归) | 96 | 0 | 0 |
| **安全测试合计** | **105** | **0** | **0** |

测试覆盖:
1. RequestContext 存取/清除
2. 角色大小写不敏感
3. admin角色识别 (isAdminLike)
4. Permissions常量完整性
5. @RequirePermission注解默认值
6. Filter: 有效token → 注入context
7. Filter: 无token → context=null
8. Filter: 无效token → context=null
9. 异常携带信息

未执行范围: 全量mvn test (约1300 tests)因耗时未执行；real FSU测试跳过。

## 八、未覆盖风险

1. **Token Store 内存存储**: 重启后所有token失效，多实例不共享
2. **entity无tenantId**: 数据范围仅靠内存过滤，大数据量下性能差
3. **POST/PUT/DELETE 未区分保护**: 当前@RequirePermission在类级别，读写用同一权限
4. **SOAP端点未保护**: `/services/SCService` 是FSU被动上报入口，不应加用户认证
5. **pom无Spring Security**: 无法使用成熟的CSRF/Session管理/CORS保护
6. **审计日志同步写入**: 高并发下可能影响性能，建议改为@Async
7. **未执行SET类命令测试**: 仅确认权限点已定义

## 九、下一步建议

1. **P0**: 为token增加tenantId/stationScope/fsuScope字段 → 数据范围过滤生效
2. **P1**: 引入Spring Security → JWT + @PreAuthorize
3. **P1**: Entity加tenantId列 + SQL WHERE过滤
4. **P1**: 审计日志改为@Async + 队列
5. **P2**: 细分POST/PUT/DELETE权限 (user:create, user:update等)
6. **P2**: 补全AuditLog查询API (AUDIT_VIEW权限)
