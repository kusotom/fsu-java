# CODEX-BE-AUTH-P0-001 后端鉴权、权限点、数据隔离和审计复审

日期：2026-05-29  
范围：`BE-AUTH-P0-001` 后端最小鉴权闭环复审  
结论：**不通过复审，仍存在 P0 遗留。**

## 1. 审计范围

本次只做审计复核和小范围修正建议，未修改业务代码，未访问真实 FSU，未执行 SET，未启 Scheduler。

重点核对：

- 认证链路：`SecurityContextFilter`、`TokenStore`、`RequestContext`
- 权限链路：`@RequirePermission`、`AuthInterceptor`、`Permissions`
- 受保护 Controller：raw XML、run-once、用户/角色、告警、实时数据、站点、FSU
- 数据隔离：`DataScopeService` 是否真正接入业务查询
- 审计日志：`AuditLogEntity`、`AuditLogService` 是否真正落到关键动作
- 协议安全边界：只读 run-once、SET 默认拒绝、Scheduler 默认关闭
- 测试覆盖：安全基础设施、SET 安全门、只读白名单、run-once probe、命令处理回归

## 2. 已读取文件清单

核心安全链路：

- `backend/src/main/java/com/dcim/platform/common/security/SecurityConfig.java`
- `backend/src/main/java/com/dcim/platform/common/security/SecurityContextFilter.java`
- `backend/src/main/java/com/dcim/platform/common/security/AuthInterceptor.java`
- `backend/src/main/java/com/dcim/platform/common/security/RequestContext.java`
- `backend/src/main/java/com/dcim/platform/common/security/RequirePermission.java`
- `backend/src/main/java/com/dcim/platform/common/security/Permissions.java`
- `backend/src/main/java/com/dcim/platform/module/auth/service/TokenStore.java`
- `backend/src/main/java/com/dcim/platform/module/auth/service/AuthService.java`
- `backend/src/main/java/com/dcim/platform/common/exception/GlobalExceptionHandler.java`

数据范围与审计：

- `backend/src/main/java/com/dcim/platform/common/security/DataScopeService.java`
- `backend/src/main/java/com/dcim/platform/common/security/audit/AuditLogEntity.java`
- `backend/src/main/java/com/dcim/platform/common/security/audit/AuditLogRepository.java`
- `backend/src/main/java/com/dcim/platform/common/security/audit/AuditLogService.java`

Controller 覆盖：

- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceMessageLogController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceCallRecordController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterface2016ReadOnlyRunOnceController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFsuStatusController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceCommandController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceSessionController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/FtpTransferRecordController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceAuthController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/ActiveAlarmAuditController.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/controller/AlarmRecordController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/RealtimeDataController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/HistoryDataController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/DeviceHeartbeatController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/SiteController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/FsuDeviceController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/MonitoringPointController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/CabinetController.java`
- `backend/src/main/java/com/dcim/platform/module/system/controller/UserAccountController.java`
- `backend/src/main/java/com/dcim/platform/module/system/controller/RoleController.java`

配置与测试：

- `backend/src/main/resources/application.yml`
- `database/seed/001_seed_demo_data.sql`
- `database/seed/002_seed_roles_permissions.sql`
- `backend/src/test/java/com/dcim/platform/common/security/SecurityInfrastructureTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/BInterface2016SetCommandSafetyTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/ReadOnlyIntegrationSafetyTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/BInterface2016ReadOnlyRunOnceProbeIntegrationTest.java`

## 3. 总体结论

`BE-AUTH-P0-001` 已经补上了 Filter、Interceptor、权限常量、部分 Controller 注解、SET 安全测试和审计实体，属于有价值的安全骨架。

但它尚未形成三方授权生产最小闭环。主要原因：

1. 未加 `@RequirePermission` 的敏感 Controller 默认放行；
2. 401/403 当前大概率只是响应体 `code=401/403`，不是 HTTP 状态码；
3. `DataScopeService` 没有被业务 Controller/Service 使用，Token 也没有 tenant/scope；
4. raw XML 下载权限只定义未执行，raw XML 没有 station/fsu scope 过滤；
5. run-once 虽有类级注解，但无审计落库，且 `admin` 角色可绕过显式权限；
6. 审计服务存在但关键动作未接线。

因此：**BE-AUTH-P0-001 不通过复审。**

## 4. 认证链路复审

### 已完成

- `SecurityConfig` 注册 `SecurityContextFilter` 到 `/api/*`，`AuthInterceptor` 拦截 `/api/**`。
- `SecurityContextFilter` 从 `Authorization: Bearer ...` 提取 token，命中 `TokenStore` 后写入 `RequestContext`。
- `SecurityContextFilter` 在 `finally` 中调用 `RequestContext.clear()`，ThreadLocal 清理路径存在。
- `TokenStore` 使用随机 `at-*` / `rt-*` token，伪造 token 不会建立 `RequestContext`。

### 问题

**P0：未认证/无权限没有明确返回真实 HTTP 401/403。**

`GlobalExceptionHandler` 对 `UnauthorizedException` 和 `ForbiddenException` 返回 `ApiResponse.fail(401/403, ...)`，但没有 `ResponseEntity.status(...)` 或 `@ResponseStatus`。在 Spring MVC 中这通常会形成 HTTP 200 + body.code=401/403，不能满足“未认证 → 401；已认证无权限 → 403”的验收语义。

建议：

- `handleUnauthorized` 返回 `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(...)`；
- `handleForbidden` 返回 `ResponseEntity.status(HttpStatus.FORBIDDEN).body(...)`；
- 增加 MockMvc 测试直接断言 HTTP status。

**P1：TokenStore 是 dev/单实例内存实现，生产误用风险明确存在。**

`TokenStore` 注释已经写明生产应替换为 Redis/JWT。当前复审接受其作为最小实现，但必须在生产开关、部署说明或 profile 中阻断误用。

**P0：TokenEntry 不含 tenantId、stationScope、fsuScope，Filter 写入空 scope。**

`SecurityContextFilter` 当前写入 `tenantId=null`、`stationScope=emptySet`、`fsuScope=emptySet`。这导致后续即使接入 `DataScopeService`，三方用户也无法获得真实授权范围。

## 5. 权限注解与拦截器复审

### 已完成

以下端点已加类级 `@RequirePermission`：

- `/api/b-interface/message-logs/**` → `protocol:raw:view`
- `/api/b-interface/call-records/**` → `protocol:raw:view`
- `/api/b-interface/2016/read-only/**` → `protocol:runonce:readonly`
- `/api/system/users/**` → `user:view`
- `/api/system/roles/**` → `role:view`
- `/api/alarms/**` → `alarm:view`
- `/api/telemetry/realtime/**` → `realtime:view`
- `/api/b-interface/**` frontend read → `fsu:view`
- `/api/b-interface/fsu-status/**` → `fsu:view`
- `/api/sites/**` → `site:view`

`AuthInterceptor` 方法级注解优先于类级注解；权限数组默认 OR，`requireAll=true` 时 AND；角色数组也是 OR。

### P0 问题：未注解 Controller 默认裸奔

`AuthInterceptor` 在无注解时直接 `return true`。这意味着任何遗漏注解的 `/api/**` Controller 都会绕过认证和授权。

复审发现以下敏感 Controller 没有 `@RequirePermission`：

| Controller | 路径 | 风险 |
|---|---|---|
| `FsuDeviceController` | `/api/fsu-devices/**` | FSU 设备 CRUD 未受控 |
| `MonitoringPointController` | `/api/monitoring-points/**` | 点位 CRUD 未受控 |
| `CabinetController` | `/api/cabinets/**` | 机柜 CRUD 未受控 |
| `HistoryDataController` | `/api/telemetry/history/**` | 历史遥测数据未受控 |
| `DeviceHeartbeatController` | `/api/telemetry/heartbeats/**` | 心跳数据未受控 |
| `BInterfaceCommandController` | `/api/b-interface/commands/**` | 协议命令表未受控 |
| `BInterfaceSessionController` | `/api/b-interface/sessions/**` | FSU session 未受控 |
| `FtpTransferRecordController` | `/api/b-interface/ftp-records/**` | FTP 记录未受控 |
| `BInterfaceAuthController` | `/api/b-interface/2016/auth/**` | registration context 未受控 |
| `ActiveAlarmAuditController` | `/api/binterface/active-alarm-audit/**` | 主动告警审计数据未受控 |

建议：

- 对所有业务 Controller 强制加 `@RequirePermission`；
- 或把 `AuthInterceptor` 改为默认拒绝，只允许显式 `@AllowAnonymous` 白名单；
- 对 CRUD 方法拆分 view/create/update/delete 权限，不能全部只用 `*:view`。

### P0 问题：adminLike 绕过与角色种子语义冲突

`RequestContext.isAdminLike()` 把 `admin`、`super_admin`、`platform_admin` 都视为可绕过权限检查。  
但 `database/seed/002_seed_roles_permissions.sql` 明确写明 `admin` “不含 raw XML 和 run-once”。当前代码会使 `admin` 即使没有 `protocol:raw:view` / `protocol:runonce:readonly`，仍可访问这些接口。

建议：

- 敏感权限不要被 `adminLike` 通用绕过；
- raw XML、run-once、未来 SET 必须检查显式权限；
- `isAdminLike` 只用于数据范围放行，且限定 `super_admin` / `platform_admin`，不要用于操作权限绕过。

### P1 问题：权限点清单存在新旧口径并存

`Permissions.java` 使用 `dashboard:view`、`fsu:view`、`user:view`、`role:view` 等新口径；但 `AuthService.permList()` 仍返回旧口径 `binterface.*`、`user.read`、`role.read`、`audit.read`。`database/seed/001_seed_demo_data.sql` 也仍包含旧权限点。

建议：

- 登录返回、权限点接口、前端权限常量、种子数据统一到同一权限矩阵；
- 保留旧权限时必须做迁移映射，不能让新旧权限同时驱动鉴权。

## 6. raw XML 权限复审

### 已完成

- `BInterfaceMessageLogController` 类级要求 `protocol:raw:view`；
- `BInterfaceCallRecordController` 类级要求 `protocol:raw:view`；
- `Permissions` 定义了 `protocol:raw:view` 和 `protocol:raw:download`。

### P0/P1 问题

**P0：raw XML 没有数据范围过滤。**

message logs 和 call records 直接按查询服务返回数据，没有按 `tenantId`、`stationScope`、`fsuScope` 过滤。三方用户一旦拥有 raw 权限或通过 adminLike 绕过，就可能看到非授权 FSU 的 raw XML。

**P0：raw XML 下载权限没有实际拦截点。**

后端定义了 `protocol:raw:download`，但没有发现任何 Controller 使用该权限。当前 raw 内容通过查看接口直接返回，前端可以基于已返回 rawMessage 自行下载，因此下载权限在后端没有真正成立。

**P1：raw XML 查看/下载审计方法存在但未接线。**

`AuditLogService.logRawXmlAccess`、`logRawXmlDownload` 没有被 Controller 调用。raw XML 查看和下载不会形成业务审计日志。

结论：**raw XML API 仅对部分查看端点做了非 admin 用户的权限注解保护，但不满足“真正受后端保护”的生产要求。**

## 7. run-once 权限复审

### 已完成

- `/api/b-interface/2016/read-only/**` 类级要求 `protocol:runonce:readonly`。
- 当前 Controller 仅暴露：
  - `POST /fsu-info/run-once` → GET_FSUINFO
  - `POST /get-data/probe` → GET_DATA
- 未发现该 Controller 暴露 SET_POINT、SET_THRESHOLD、SET_FTP、SET_LOGININFO、SET_FSUREBOOT。
- `application.yml` 中 FSU real call、Scheduler、SET safety 均默认关闭。

### 问题

**P0：run-once 审计未接线。**

`AuditLogService.logRunOnce` 存在，但 `BInterface2016ReadOnlyRunOnceController` 未调用。run-once 执行没有进入 `audit_log`。

**P0/P1：adminLike 可绕过 `protocol:runonce:readonly`。**

如果用户是 `admin`，即便不具备 `protocol:runonce:readonly`，仍可能通过 `ctx.isAdminLike()` 绕过权限校验。

**P1：只读命令范围未完全与需求一致。**

需求允许只读命令范围为 GET_FSUINFO、GET_DATA、GET_LOGININFO、GET_FTP。当前真正执行端点只有 GET_FSUINFO、GET_DATA，属于安全但不完整。另 `/api/b-interface/run-once/capabilities` 展示了 GET_THRESHOLD、GET_ACTIVEALARM、FTP_IMAGE_PULL 等规划能力，建议与可执行白名单区分。

结论：**run-once API 有权限注解保护，但由于 adminLike 绕过和审计缺失，不能认定为生产安全闭环。**

## 8. 数据范围过滤复审

### 已完成

- `DataScopeService` 定义了 `filterByFsuScope`、`filterByStationScope`。
- 无 scope 的普通用户按设计应返回空集合。
- admin/super_admin/platform_admin 按设计返回全部。

### P0 问题：DataScopeService 没有被业务查询使用

代码搜索显示 `DataScopeService` 除自身外没有业务调用点。实际业务 Controller 仍直接调用 Repository：

- `BInterfaceFrontendReadController.listFsus()` 使用 `fsuStatusRepo.findAll()`
- `BInterfaceFrontendReadController.getAlarms()` 无 fsuCode 时使用 `alarmRepo.findAll()`
- `BInterfaceFrontendReadController.getRealtimePoints()` 无 fsuCode 时使用 `rtRepo.findAll()`
- message logs、call records、alarm、site、realtime 等 Controller 均未见 scope 过滤

同时 `SecurityContextFilter` 写入空 `stationScope` / `fsuScope`，`TokenStore.TokenEntry` 不含租户和范围信息。

结论：**数据范围过滤不满足三方授权最小闭环。** 当前只是服务类骨架，尚未接入业务数据路径。

后续建议：

1. 短期：所有返回站点、FSU、告警、实时数据、raw XML 的 Controller 接入 `DataScopeService`，并对 path/query 中的 `fsuCode` 做授权校验；
2. 中期：Token 或服务端 session 中写入 `tenantId/stationScope/fsuScope`；
3. 长期：迁移到 Repository/SQL 层过滤，避免先查全量再内存过滤导致分页、总数、排序和性能风险。

## 9. 审计日志复审

### 已完成

- `AuditLogEntity` 字段覆盖 userId、username、tenantId、action、resourceType、resourceId、stationId、fsuCode、permissionCode、allowed、reason、requestTime、clientIp。
- `AuditLogService.save` 捕获异常，审计失败不会中断主流程。
- `AuthInterceptor` 在权限拒绝时调用 `logPermissionDenied`。

### P0/P1 问题

**P0：关键允许动作未写入审计。**

以下方法存在但无调用点：

- `logRawXmlAccess`
- `logRawXmlDownload`
- `logRunOnce`
- `logSetCommandBlocked`

因此 raw XML 查看、raw XML 下载、run-once 执行、SET 类命令拦截未进入统一 `audit_log`。

**P1：审计字段未完整填充。**

`AuditLogService.save` 当前写入 `tenantId=null`、`clientIp=null`。stationId 多数调用也为空。

**P1：生产 DDL 风险。**

`audit_log` 只有 JPA Entity，未发现正式 schema/DDL 文件。dev 环境可依赖 Hibernate update，但生产环境不应依赖自动建表。

结论：**审计日志不满足最小闭环。**

## 10. 协议安全边界复审

### 通过项

- `application.yml` 中 `b-interface.fsu-client.real-call-enabled=false`。
- `b-interface.slow-polling.scheduler-enabled=false`。
- `b-interface.set-command-safety.enabled=false`、`allow-real-call=false`。
- SET 类命令权限只定义了常量，种子数据未给普通角色赋权。
- `BInterface2016SetCommandSafetyTest`、`ReadOnlyIntegrationSafetyTest` 通过。
- 本次没有访问真实 FSU，没有执行 SET，没有启 Scheduler。

### 风险

- `BInterfaceFrontendReadController.getProtocolMatrix()` 仍展示 SET_* 命令规划信息，但只是只读展示，暂不构成可执行入口。
- `BInterfaceFrontendReadController.getRunOnceCapabilities()` 展示的能力与当前只读 run-once 允许命令不完全一致，建议把“可执行”和“规划/协议覆盖”分开。
- 如果未来 SET Controller 仅加 `@RequirePermission(protocol:set:*)`，当前 `adminLike` 绕过会形成高风险漏洞。

## 11. 测试验证

已执行：

```bash
mvn test -Dtest=SecurityInfrastructureTest
```

结果：9 tests，0 failures，0 errors。

已执行：

```bash
mvn test -Dtest='SecurityInfrastructureTest,BInterface2016SetCommandSafetyTest,ReadOnlyIntegrationSafetyTest,BInterface2016ReadOnlyRunOnceProbeIntegrationTest'
```

结果：55 tests，0 failures，0 errors。

额外执行更宽命令处理回归：

```bash
mvn test -Dtest='*CommandHandlerTest'
```

结果：162 tests，6 failures，0 errors。失败集中在：

- `HeartbeatCommandHandlerTest.shouldHandleSuccessfulHeartbeat`
- `HeartbeatCommandHandlerTest.shouldReturnServerTimeInResponse`
- `SendDataCommandHandlerTest.shouldHandleSuccessfulSendData`
- `SendDataCommandHandlerTest.shouldIncludeCountInResponse`
- `SendAlarmCommandHandlerTest.shouldHandleSuccessfulSendAlarm`
- `SendAlarmCommandHandlerTest.shouldIncludeAlarmIdInResponse`

未继续执行全量 `mvn test`，原因是更宽的 `*CommandHandlerTest` 子集已失败，全量测试会被相同失败阻断，不能提供更多鉴权复审信号。

补充说明：`BInterface2016ReadOnlyRunOnceProbeIntegrationTest` 在本地测试过程中写出 raw sample 文件到 `backend/docs/landing/raw-samples/`，这是该测试已有行为；本次未访问真实 FSU。

## 12. 测试覆盖缺口

当前 9 个 `SecurityInfrastructureTest` 主要覆盖 RequestContext、权限常量、Filter 设置/清理上下文、异常对象，不足以证明后端 P0 闭环。

必须补测：

| 编号 | 缺口 | 建议测试 |
|---|---|---|
| T1 | HTTP 401/403 真实状态码 | MockMvc 未登录访问 message-logs 返回 401 |
| T2 | raw XML 无权限拒绝 | 普通 token 访问 message-logs 返回 403 |
| T3 | raw XML view 允许 | `protocol:raw:view` token 可访问 |
| T4 | raw XML download | 无 `protocol:raw:download` 不能下载 |
| T5 | run-once 无权限拒绝 | 无 `protocol:runonce:readonly` 返回 403 |
| T6 | run-once 有权限 | 有权限进入校验路径，但不访问真实 FSU |
| T7 | SET 后端拒绝 | SET 类命令即使有 token 也被安全门拒绝 |
| T8 | 三方 fsuScope | 用户只能查询授权 FSU |
| T9 | 无 scope 用户 | 无 scope 不能看到全部数据 |
| T10 | audit log | raw/run-once/权限拒绝/SET 拦截均落库 |
| T11 | 未注解 Controller | 所有业务 Controller 无 token 默认 401 |

## 13. P0 / P1 / P2 整改建议

### P0

1. **默认拒绝未注解业务接口。** 给所有业务 Controller 增加权限注解，或引入 `@AllowAnonymous` 白名单后默认拒绝。
2. **修正 HTTP 401/403 状态码。** `GlobalExceptionHandler` 改为 `ResponseEntity.status(...)`，补 MockMvc。
3. **接入数据范围过滤。** 至少覆盖站点、FSU、告警、实时数据、raw XML、message logs、call records。
4. **修正 adminLike 绕过。** raw XML、run-once、SET 不允许被 `admin` 通用绕过。
5. **后端实现 raw XML 下载权限。** 单独下载接口使用 `protocol:raw:download`，且不能通过 view 接口返回完整 raw 后绕过。
6. **审计关键动作。** raw view/download、run-once、权限拒绝、SET 拦截全部写 `audit_log`。

### P1

1. Token/会话写入 `tenantId/stationScope/fsuScope`。
2. `AuthService.permList()`、种子数据、前端权限点统一到同一矩阵。
3. 用户/角色 CRUD 拆分 `view/create/update/delete`，不能只用 `user:view` / `role:view`。
4. 为 `audit_log` 增加正式 DDL。
5. 将内存 scope 过滤迁移到 Repository/SQL 层，避免分页总数泄露。

### P2

1. 增加权限矩阵导出与审计页面查询。
2. 增加租户、站点授权、FSU 授权后台管理 API。
3. 增加权限拒绝原因的前端可观测性。

## 14. 最终回答

1. **BE-AUTH-P0-001 是否通过复审：否。**
2. **是否仍存在 P0 遗留：是。** 未注解敏感 Controller 默认放行、HTTP 状态码语义不成立、数据范围未接入、raw 下载权限未执行、审计未闭环。
3. **raw XML API 是否真正受后端保护：否。** 查看端点有 `protocol:raw:view` 注解，但无下载权限执行、无数据范围过滤、无查看/下载审计，且 adminLike 绕过存在。
4. **run-once API 是否真正受后端保护：部分。** 类级注解存在，执行端点未暴露 SET，但 adminLike 绕过和审计缺失使其未达到生产闭环。
5. **数据范围过滤是否满足三方授权最小闭环：否。** `DataScopeService` 未接入业务查询，Token 不含 tenant/scope。
6. **审计日志是否满足最小闭环：否。** Entity/Service 存在，但 raw/run-once/SET 拦截未接线。
7. **是否可以进入 FE-P0-RECTIFY-002：可以并行，但不能把后端视为已通过。** 前端可继续修状态口径和权限矩阵，但生产安全仍阻塞在后端。
8. **是否可以进入 FE/BE 权限矩阵联合验证：暂不建议。** 需先修复后端 P0，否则联合验证会被后端裸接口和状态码问题污染。
9. **是否可以开始 FE-P1 页面建设：不建议。** 三方授权生产阻塞项未解决前，应优先修 BE-AUTH-P0-001 复审 P0。
