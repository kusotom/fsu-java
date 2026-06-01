# AUTH-MATRIX-VERIFY-002 后端 P0 修复后权限矩阵复验

日期：2026-05-29  
范围：BE-AUTH-P0-FIX-001 后的后端 API 权限、raw XML、run-once、SET 拦截、DataScope、AuditLog、前端权限矩阵和前端实时数据状态口径。  
边界：未访问真实 FSU，未执行 SET，未启 Scheduler，未做 FE-P1 页面建设，未修改 B接口2016 主线。

## 结论摘要

AUTH-MATRIX-VERIFY-002 **未通过**。

BE-AUTH-P0-FIX-001 已修复认证链路、401/403、敏感权限 adminLike 绕过和 SET 安全门等基础问题，但前后端权限矩阵仍未形成 P0 安全闭环。主要原因是后端 DataScope 没有覆盖所有资源读取路径，raw XML 下载权限不是 AND 语义，多个按 ID 查询绕过 scope，`/api/b-interface` 聚合读取端点直接 `findAll()` 返回数据，且 `view` 权限仍覆盖 POST/PUT/DELETE 写操作。

因此当前仍存在 P0，不能进入 FE-P1 页面建设，不能把三方授权视为可生产交付。可以并行做三方授权字段模型草案，但不建议开始页面实现。

## 已读取文件清单

- `backend/src/main/java/com/dcim/platform/common/security/RequirePermission.java`
- `backend/src/main/java/com/dcim/platform/common/security/AuthInterceptor.java`
- `backend/src/main/java/com/dcim/platform/common/security/RequestContext.java`
- `backend/src/main/java/com/dcim/platform/common/security/SecurityContextFilter.java`
- `backend/src/main/java/com/dcim/platform/common/security/DataScopeService.java`
- `backend/src/main/java/com/dcim/platform/common/security/Permissions.java`
- `backend/src/main/java/com/dcim/platform/common/security/audit/AuditLogService.java`
- `backend/src/main/java/com/dcim/platform/common/config/SecurityConfig.java`
- `backend/src/main/java/com/dcim/platform/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/dcim/platform/module/auth/service/AuthService.java`
- `backend/src/main/java/com/dcim/platform/module/auth/service/TokenStore.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceMessageLogController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceCallRecordController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterface2016ReadOnlyRunOnceController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFsuStatusController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/log/BInterfaceMessageLogService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterfaceMessageLogQueryService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterfaceCallRecordQueryService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterfaceFsuStatusQueryService.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/SiteController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/FsuDeviceController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/controller/MonitoringPointController.java`
- `backend/src/main/java/com/dcim/platform/module/resource/service/SiteService.java`
- `backend/src/main/java/com/dcim/platform/module/resource/service/FsuDeviceService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/controller/AlarmRecordController.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/RealtimeDataController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/HistoryDataController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/DeviceHeartbeatController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/HistoryDataService.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/DeviceHeartbeatService.java`
- `backend/src/main/java/com/dcim/platform/module/system/controller/UserAccountController.java`
- `backend/src/main/java/com/dcim/platform/module/system/controller/RoleController.java`
- `backend/src/test/java/com/dcim/platform/common/security/SecurityInfrastructureTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/BInterface2016SetCommandSafetyTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/ReadOnlyIntegrationSafetyTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/BInterface2016ReadOnlyRunOnceProbeIntegrationTest.java`
- `database/seed/002_seed_roles_permissions.sql`
- `frontend/src/auth/permissions.ts`
- `frontend/src/auth/routeGuard.ts`
- `frontend/src/auth/access.ts`
- `frontend/src/stores/user.ts`
- `frontend/src/router/index.ts`
- `frontend/src/layouts/BasicLayout.vue`
- `frontend/src/components/auth/PermissionGuard.vue`
- `frontend/src/compat/realtimeSignalFilter.ts`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/views/binterface/MessageLogView.vue`
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
- `frontend/src/views/binterface/PointCard.vue`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/api/bInterface.ts`
- `frontend/src/api/request.ts`

## 验证执行

| 验证项 | 命令或方式 | 结果 |
|---|---|---|
| 指定后端安全/协议安全测试 | `mvn test -Dtest='SecurityInfrastructureTest,BInterface2016SetCommandSafetyTest,ReadOnlyIntegrationSafetyTest,BInterface2016ReadOnlyRunOnceProbeIntegrationTest,*CommandHandlerTest'` | 217 tests, 0 failures |
| 前端构建 | `npm run build` | 通过 |
| 后端全量测试 | `mvn test` | 1450 tests, 4 failures, 24 skipped |
| 缺失测试类核查 | `rg` 检索 `AuthHttpStatusTest/AdminLikeBypassTest/DataScopeIntegrationTest/AuditLogWiringTest` | 未发现这些测试类 |

全量 `mvn test` 的 4 个失败为：

- `CommandResultTest.shouldCreateNotImplementedResult`
- `CommandResultTest.shouldCreateSuccessResult`
- `CommandResultTest.shouldConvertToInfoXml`
- `FsuServiceRpcAdapterTest.shouldContainSoapEncodingNamespace`

这些失败集中在协议 ResultCode 语义和 SOAP ENC 命名空间断言，不是本次鉴权/DataScope/AuditLog 修复直接引入的失败。安全相关指定测试和 `*CommandHandlerTest` 已通过。

## 后端权限矩阵复验

### 通过项

- `SecurityContextFilter` 注册到 `/api/*`，并在 finally 中清理 `RequestContext`。
- `AuthInterceptor` 支持类级和方法级 `@RequirePermission`，方法级优先。
- `GlobalExceptionHandler` 对 `UnauthorizedException` 和 `ForbiddenException` 返回真实 HTTP 401/403。
- `RequestContext.containsSensitivePermission()` 覆盖 raw、run-once 和 5 个 SET 权限。
- `AuthInterceptor.canBypassPermissionCheck()` 限制敏感权限仅 `super_admin` 可绕过，`platform_admin/admin` 不再绕过 raw/run-once/SET。
- SET 安全门默认拒绝 SET 类真实执行，指定安全测试通过。

### 未通过项

1. raw XML 下载权限不是 AND 语义。  
   `BInterfaceMessageLogController.downloadXml()` 注释要求 `raw:view + raw:download`，但注解为 `@RequirePermission({Permissions.PROTOCOL_RAW_VIEW, Permissions.PROTOCOL_RAW_DOWNLOAD})`，而 `RequirePermission.requireAll()` 默认是 `false`。因此持有 `protocol:raw:view` 即可通过下载接口，不需要 `protocol:raw:download`。

2. raw XML 数据范围过滤不闭合。  
   `BInterfaceMessageLogController.query()` 调用 `BInterfaceMessageLogService.query()`，该服务直接按 Repository 查询并返回 Page，没有调用 `DataScopeService`。`getById()` 和 `downloadXml()` 最终依赖 `BInterfaceMessageLogQueryService.getById()`，该方法直接 `repository.findById()`，没有校验当前用户 `fsuScope`。`BInterfaceCallRecordQueryService.getById()` 也存在同类问题。

3. `/api/b-interface` 聚合只读接口绕过 DataScope。  
   `BInterfaceFrontendReadController` 类级有 `fsu:view`，但内部多处直接使用 Repository：`fsuStatusRepo.findAll()`、`alarmRepo.findAll()`、`rtRepo.findAll()`。三方用户只要有 `fsu:view`，就可能通过 `/api/b-interface/fsus`、`/api/b-interface/alarms`、`/api/b-interface/realtime-points` 看到未授权 FSU/告警/实时数据。

4. 多个按 ID 查询绕过 DataScope。  
   `SiteService.getById()`、`BInterfaceFsuStatusQueryService.getById()`、`AlarmRecordService.getById()`、`RealtimeDataService.getById()`、`HistoryDataService.getById()`、`DeviceHeartbeatService.getById()` 等均直接按 ID 查询返回，没有资源级 scope 校验。即使列表过滤正确，直接输入 ID 仍可能越权。

5. 多个服务仍未接入 DataScope。  
   `HistoryDataService.list()`、`DeviceHeartbeatService.list()`、`FsuDeviceService.list()` 等仍是直接 `repository.findAll()`。这些资源在三方授权场景下属于 FSU/站点数据范围，不能默认全量返回。

6. `view` 权限覆盖写接口。  
   `SiteController`、`FsuDeviceController`、`MonitoringPointController`、`UserAccountController`、`RoleController` 等采用类级 `site:view/fsu:view/user:view/role:view`，POST/PUT/DELETE 没有独立写权限。持有查看权限的角色可通过直接 HTTP 调用执行创建、修改、删除。

## raw XML 复验结论

raw XML **未真正完成后端保护**。

- 查看列表和详情需要 `protocol:raw:view`，但 scope 不闭合。
- 下载端点未启用 `requireAll=true`，`protocol:raw:download` 未形成独立强制条件。
- 前端下载按钮有 `protocol:raw:download` 的 `PermissionGuard`，但 `MessageLogView` 从列表中的 `row.rawMessage` 直接生成 Blob 下载，并没有调用后端下载 API；后端下载权限测试因此不能覆盖前端实际下载路径。
- raw XML 审计方法存在，但在 scope 绕过的情况下会记录“允许访问”，无法弥补越权访问。

定级：P0。

## run-once 复验结论

run-once 权限保护 **基本通过**，但审计和命令范围仍有 P1 缺口。

- `/api/b-interface/2016/read-only/**` 类级要求 `protocol:runonce:readonly`。
- 后端 Controller 当前只暴露 `GET_FSUINFO` 和 `GET_DATA probe`，未暴露 SET。
- 前端路由和占位区块要求 `protocol:runonce:readonly`。
- 指定测试使用 fake/stub client，不访问真实 FSU。

缺口：

- 用户要求的只读范围包含 `GET_FSUINFO/GET_DATA/GET_LOGININFO/GET_FTP`，当前后端 REST 只覆盖前两个。
- run-once Controller 只在 service 正常返回后写 `logRunOnce()`，参数校验失败和异常路径未写 RUN_ONCE 审计。

定级：保护面 P0 通过，功能/审计完整性 P1。

## SET 安全复验结论

SET 后端拒绝 **通过**。

- 5 个 B接口2016 高风险 SET 命令在安全门禁默认拒绝。
- `BInterface2016SetCommandSafetyTest` 验证 REST run-once Controller 不暴露 SET 路径。
- 前端未发现可点击执行 `SET_POINT/SET_THRESHOLD/SET_FTP/SET_LOGININFO/SET_FSUREBOOT` 的真实入口。
- `SetCommandSafetyGate` 对多条拒绝路径接入 `logSetCommandBlocked()`。

注意：测试中部分 `AuditLogService(null)` 会导致审计写入静默失败，这是测试覆盖问题，不影响 SET 拦截结论。

## DataScope 三方隔离复验结论

DataScope **未通过最小闭环复验**。

已完成：

- `DataScopeService` 明确无 scope 返回空集合，adminLike 返回不限范围。
- 部分列表服务接入了 `filterByFsuScope/filterByStationScope`。

未完成：

- 关键前端聚合 API 未接入 DataScope。
- raw XML 查询和按 ID 下载未做 scope 校验。
- 多个业务按 ID 查询未做 scope 校验。
- `HistoryDataService`、`DeviceHeartbeatService`、`FsuDeviceService` 等仍直接全量返回。
- `AlarmRecordService` 和 `RealtimeDataService` 用 `String.valueOf(e.getFsuId())` 对比 `fsuScope`，而 scope 语义是 `fsuCode`，可能导致授权用户看不到应有数据，也说明 scope 字段语义未统一。
- 当前仍是内存过滤，分页、总数、排序和性能风险仍存在。

定级：P0。

## AuditLog 审计闭环复验结论

AuditLog **未达到最小闭环证明**。

已完成：

- `AuditLogService` 定义了权限拒绝、raw view、raw download、run-once、SET blocked 5 类事件。
- `AuthInterceptor` 权限拒绝路径写审计。
- raw XML 查看/下载和 run-once 正常返回路径有调用点。
- 审计保存异常不影响主流程。

未完成：

- 缺少 `AuditLogWiringTest` 或等价集成测试证明 JPA 持久化成功。
- 多个现有测试用 `new AuditLogService(null)`，只能证明“不抛异常”，不能证明入库。
- `tenantId` 和 `clientIp` 当前为 null。
- run-once 参数失败/异常路径未写审计。
- raw XML scope 越权访问会被记录为 allowed，不能证明授权正确。
- call-record raw 查看未写 raw view 审计。

定级：P1，因 DataScope/raw P0 未闭合，审计只能算接线存在，不算生产闭环。

## 前端权限矩阵复验

通过项：

- `routeGuard` 明确 `permissions=all-of`、`roles=any-of`、二者同时存在为 AND。
- `PermissionGuard` 与路由语义一致。
- raw XML 路由要求 `protocol:raw:view` 并限定高权限角色。
- raw XML 查看按钮要求 `protocol:raw:view`，下载按钮要求 `protocol:raw:download`。
- run-once 相关页面/区块要求 `protocol:runonce:readonly`。
- `npm run build` 通过。

未通过项：

- 前端业务路由仍大量使用旧权限码，例如 `binterface.fsu.read`、`binterface.realtime.read`、`user.read`、`role.read`；后端 Controller 使用 `fsu:view`、`realtime:view`、`user:view`、`role:view`。前后端权限码未统一，可能导致“前端放行后端拒绝”或“后端有权前端不可见”。
- `AuthService.permList()` 仍返回旧的 `binterface.*`/`user.read` 权限清单，而 `database/seed/002_seed_roles_permissions.sql` 使用新的 `*:view` 权限码，权限来源不一致。
- `BasicLayout` 多数菜单没有按权限过滤；核心业务路由如 `/sites`、`/devices`、`/telemetry/realtime`、`/alarms` 只要求登录，未在前端 meta 中声明对应权限。

定级：前端展示控制 P1；真正安全仍由后端实现，但后端当前仍有 P0。

## 前端状态口径复验

前端状态口径 **基本通过**。

- `BInterfaceRealtimeView` 已区分 API 404、网络异常、401、403、5xx、协议解析失败、FSU 未 ACK/未访问真实设备、FSU ACK 空数据、HTTP 200 空数据、未映射点位。
- `compat/realtimeSignalFilter.ts` 集中保存 8 个 SignalID 白名单，并明确标注为临时兼容逻辑，不是正式点位映射方案。
- `PointCard` 使用 `valueNumber != null` 和 `??`，`0.0` 可正常显示。
- DI 点位没有硬编码 0/1 语义，只提示“DI 编码待现场复核”。
- 未知单位点位显示“单位待确认”。

缺口：

- 后端 `/api/b-interface/realtime-points` 当前返回 `ApiResponse<List<RealtimePointDto>>`，没有返回 `realDeviceAccessed/ackReceived/emptyData/valuesReturned/unmappedCount/parseError` meta；因此 ACK 空数据等状态口径只能靠未来后端字段驱动。

定级：前端 P0 通过，后端数据状态字段 P1。

## 权限矩阵结论

| 资源/能力 | 当前结论 | 级别 |
|---|---|---|
| 后端 API 权限注解 | 基础覆盖比 VERIFY-001 改善，但 view 覆盖写接口 | P0 未闭合 |
| raw XML view | 有权限点，但按 ID/scope 不闭合 | P0 |
| raw XML download | 未强制 `raw:view + raw:download` AND | P0 |
| run-once | 端点权限基本有效，功能范围和失败审计不足 | P1 |
| SET blocked | 默认拒绝仍成立 | 通过 |
| DataScope | 聚合 API、按 ID、部分服务未覆盖 | P0 |
| AuditLog | 接线存在，持久化和全路径覆盖未验证 | P1 |
| 前端 route/guard | raw/run-once 有保护，业务权限码未统一 | P1 |
| 前端状态口径 | 展示口径通过，后端 meta 缺失 | P1 |

## P0 遗留

1. `BInterfaceMessageLogController.downloadXml()` 未设置 `requireAll=true`，raw 下载权限未真正独立生效。
2. raw XML 列表、详情、下载、call-record 详情未做资源级 scope 校验。
3. `/api/b-interface/fsus`、`/api/b-interface/alarms`、`/api/b-interface/realtime-points` 等聚合 API 直接全量查询，绕过 DataScope。
4. 多个按 ID 查询路径未校验 stationScope/fsuScope，可通过 ID 猜测越权。
5. `site:view/fsu:view/user:view/role:view` 类级权限覆盖 POST/PUT/DELETE，查看权限可执行写操作。
6. `HistoryDataService`、`DeviceHeartbeatService`、`FsuDeviceService` 等仍存在未过滤全量查询。

## 是否可以进入下一阶段

| 问题 | 结论 |
|---|---|
| 后端 API 权限矩阵是否通过 | 否 |
| raw XML 是否真正受保护 | 否 |
| run-once 是否真正受保护 | 是，保护面通过；功能和失败审计 P1 |
| SET 是否仍被拒绝 | 是 |
| 三方数据隔离是否通过 | 否 |
| 审计日志闭环是否通过 | 否，只能算接线存在 |
| 是否仍存在 P0 | 是 |
| 是否可以进入 FE-P1 页面建设 | 否 |
| 是否可以开始三方授权页面规划 | 否；仅可做字段模型草案，不应进入页面实现 |

## 建议整改顺序

1. 先修 raw XML 下载权限 AND 语义，并补 HTTP 测试。
2. 把 raw XML、call-record、FSU status、site、alarm、realtime、history、heartbeat、FSU device、monitoring point 的按 ID 查询全部接入资源级 scope 校验。
3. 替换 `/api/b-interface` 聚合 Controller 中的直接 Repository 查询，统一走已 scope 化的 service 或 Repository 层过滤。
4. 拆分 `view/create/update/delete/manage/confirm/download/runonce` 权限点，不能再让 view 权限覆盖写接口。
5. 补齐 `AuthHttpStatusTest`、`AdminLikeBypassTest`、`DataScopeIntegrationTest`、`AuditLogWiringTest`。
6. 统一前后端权限码，建议以 `dashboard:view/site:view/fsu:view/realtime:view/alarm:view/protocol:*` 为准。
7. 将 DataScope 从内存过滤迁移到 Repository/SQL 层，避免分页、总数和性能泄露。
