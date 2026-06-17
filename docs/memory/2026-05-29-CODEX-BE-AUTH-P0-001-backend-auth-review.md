# 2026-05-29 CODEX-BE-AUTH-P0-001 后端鉴权、权限点、数据隔离和审计复审

## 本次审计目标

复审 `BE-AUTH-P0-001` 后端最小鉴权闭环，确认 Filter + Interceptor + `@RequirePermission` + `DataScopeService` + `AuditLogService` 是否真正覆盖 raw XML、run-once、SET 安全边界、三方数据隔离和审计日志。

本次只做审计和文档输出，未修改业务代码，未访问真实 FSU，未执行 SET，未启 Scheduler。

## 审计结论摘要

`BE-AUTH-P0-001` **不通过复审**。

已形成安全骨架：

- `SecurityContextFilter` 可从 Bearer token 建立 `RequestContext`，请求结束后清理 ThreadLocal。
- `AuthInterceptor` 能读取类级/方法级 `@RequirePermission`。
- raw XML、run-once、用户/角色、告警、实时、站点、FSU 部分 Controller 已加权限注解。
- SET 安全门和只读白名单相关定向测试通过。
- `AuditLogEntity` 和 `AuditLogService` 已存在。

但未形成生产最小闭环：

- 未注解的敏感 Controller 默认放行。
- HTTP 401/403 可能只是响应体 code，不是真实 HTTP 状态。
- `DataScopeService` 未被业务查询使用，Token 不含 tenant/scope。
- raw XML 下载权限只定义未执行，raw XML 无数据范围过滤。
- run-once 有注解但缺审计，且 `admin` 可通过 adminLike 绕过显式权限。
- 审计服务关键动作方法未接线。

## 发现的关键问题

P0：

1. `AuthInterceptor` 对无 `@RequirePermission` 的接口直接放行，`FsuDeviceController`、`MonitoringPointController`、`CabinetController`、`HistoryDataController`、`DeviceHeartbeatController`、`BInterfaceCommandController`、`BInterfaceSessionController`、`FtpTransferRecordController`、`BInterfaceAuthController`、`ActiveAlarmAuditController` 等敏感接口未受保护。
2. `GlobalExceptionHandler` 返回 `ApiResponse.fail(401/403, ...)`，没有 `ResponseEntity.status(...)` 或 `@ResponseStatus`，需要 MockMvc 确认并修成真实 HTTP 401/403。
3. `DataScopeService` 没有业务调用点；`SecurityContextFilter` 写入 `tenantId=null`、空 station/fsu scope；三方数据隔离不成立。
4. raw XML 查看端点只有 `protocol:raw:view`，无下载权限执行点，无 scope 过滤，无 raw view/download 审计。
5. `RequestContext.isAdminLike()` 包含 `admin`，与种子数据中 `admin` 不含 raw/run-once 的语义冲突，可绕过敏感权限。
6. `AuditLogService.logRawXmlAccess/logRawXmlDownload/logRunOnce/logSetCommandBlocked` 没有调用点。

P1：

1. `AuthService.permList()` 仍返回旧权限点 `binterface.*`、`user.read`、`role.read`，与 `Permissions.java` 新口径不一致。
2. 用户/角色 Controller 的 create/update/delete 仍只由 `user:view` / `role:view` 类级权限保护。
3. `audit_log` 仅有 JPA Entity，未发现正式 schema DDL。
4. 当前 DataScope 即使接入也属于内存过滤，后续应迁移到 Repository/SQL 层。

## 角色管理和三方授权结论

角色和权限常量已补，但三方授权基础不够：

- 权限点新旧口径并存；
- 登录 token 不含 `tenantId/stationScope/fsuScope`；
- 后端未按站点/FSU 对业务数据做强制过滤；
- 无 scope 的三方用户不能默认看到全部数据这一设计尚未被业务路径验证；
- raw XML、告警、实时数据、站点、FSU 查询均未形成统一数据隔离闭环。

后端鉴权缺失仍是三方授权生产阻塞项。

## 验证记录

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

已执行：

```bash
mvn test -Dtest='*CommandHandlerTest'
```

结果：162 tests，6 failures，0 errors。失败在 Heartbeat/SEND_DATA/SEND_ALARM 成功路径断言。因该子集已失败，未继续跑全量 `mvn test`。

说明：`BInterface2016ReadOnlyRunOnceProbeIntegrationTest` 会按既有逻辑写出本地 raw sample 到 `backend/docs/landing/raw-samples/`，本次未访问真实 FSU。

## 后续优先级

第一优先级：

1. 后端默认拒绝未注解业务接口，或补齐全部业务 Controller 权限注解。
2. 修正真实 HTTP 401/403 状态码。
3. 接入数据范围过滤，覆盖站点、FSU、告警、实时数据、raw XML。
4. raw XML 下载使用 `protocol:raw:download` 独立后端权限。
5. raw XML、run-once、权限拒绝、SET 拦截全部落审计。
6. 移除 `admin` 对 raw/run-once/SET 的通用绕过。

第二优先级：

1. 统一前后端权限点和种子数据。
2. Token 或服务端 session 写入 tenant/scope。
3. 为用户/角色写操作拆分权限。
4. 增加 `audit_log` 正式 DDL。

第三优先级：

1. 把内存 scope 过滤迁移到 Repository/SQL 层。
2. 再进入 FE/BE 权限矩阵联合验证。
3. 后端 P0 修复后再开始 FE-P1 页面建设。

## 未修改代码说明

本次仅新增审计报告和工程记忆，并更新 memory 索引。未修改 Java/Vue 业务代码，未启 Scheduler，未执行 SET，未访问真实 FSU。

## 下一步建议

先执行 `BE-AUTH-P0-001-FIX-001`：修默认放行、HTTP 401/403、DataScope 接入、raw 下载权限和审计接线。修完后再做 FE/BE 权限矩阵联合验证；FE-P1 页面建设应暂缓。
