# AUTH-MATRIX-VERIFY-002 后续整改 TODO

日期：2026-05-29  
复验结论：未通过，仍存在 P0。P0 修复前不进入 FE-P1 页面建设。

## P0

### AUTH-MATRIX-VERIFY-002-P0-001 raw XML 下载权限改为 AND

- 问题描述：`BInterfaceMessageLogController.downloadXml()` 注释要求 `raw:view + raw:download`，但 `@RequirePermission` 默认 OR，导致只有 `protocol:raw:view` 也可下载。
- 影响范围：raw XML 下载、三方协议数据泄露。
- 建议修改文件：`backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceMessageLogController.java`、`backend/src/test/java/**/AuthHttpStatusTest.java` 或新增等价测试。
- 验收标准：无 token 返回 401；只有 `protocol:raw:view` 返回 403；同时拥有 `protocol:raw:view` 和 `protocol:raw:download` 才返回 200。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是，raw XML 边界。

### AUTH-MATRIX-VERIFY-002-P0-002 raw XML 与 call-record 按资源 scope 校验

- 问题描述：message-log/call-record 的 list/detail/download 未形成统一 DataScope；按 ID 查询可绕过列表过滤。
- 影响范围：raw XML 查看、下载、FSUService 调用记录。
- 建议修改文件：`BInterfaceMessageLogController.java`、`BInterfaceMessageLogService.java`、`BInterfaceMessageLogQueryService.java`、`BInterfaceCallRecordQueryService.java`。
- 验收标准：三方用户只能查询/查看/下载 `fsuScope` 内 raw XML；无 scope 三方用户列表为空且按 ID 返回 403 或 404；越权访问写入拒绝审计。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是，raw XML 边界。

### AUTH-MATRIX-VERIFY-002-P0-003 修复 `/api/b-interface` 聚合 API 绕过 DataScope

- 问题描述：`BInterfaceFrontendReadController` 直接 `findAll()` 读取 FSU、告警、实时数据，绕过 DataScope。
- 影响范围：`/api/b-interface/fsus`、`/api/b-interface/alarms`、`/api/b-interface/realtime-points`、FSU 详情、设备列表、未映射点位。
- 建议修改文件：`BInterfaceFrontendReadController.java`，必要时新增 scope 化 service。
- 验收标准：THIRD_PARTY_OPS 只能看到授权 `stationScope/fsuScope` 内的 FSU、告警、实时数据；无 scope 返回空；按 fsuCode 参数越权返回 403 或空。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是，三方数据隔离。

### AUTH-MATRIX-VERIFY-002-P0-004 所有按 ID 查询加入资源级 scope 校验

- 问题描述：多个 service 的 `getById()` 直接返回实体，列表过滤无法阻止直接 ID 越权。
- 影响范围：sites、fsu-status、realtime、history、heartbeats、alarms、raw XML、call-records。
- 建议修改文件：`SiteService.java`、`BInterfaceFsuStatusQueryService.java`、`AlarmRecordService.java`、`RealtimeDataService.java`、`HistoryDataService.java`、`DeviceHeartbeatService.java`、`BInterfaceMessageLogQueryService.java`、`BInterfaceCallRecordQueryService.java`。
- 验收标准：授权外 ID 返回 403 或 404；补充覆盖每类资源的 DataScope 集成测试。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是，三方数据隔离。

### AUTH-MATRIX-VERIFY-002-P0-005 拆分写操作权限，禁止 view 权限执行写接口

- 问题描述：`site:view/fsu:view/user:view/role:view` 类级权限覆盖 POST/PUT/DELETE，查看用户可直接调用写接口。
- 影响范围：站点、FSU、点位、机柜、用户、角色。
- 建议修改文件：`SiteController.java`、`FsuDeviceController.java`、`MonitoringPointController.java`、`CabinetController.java`、`UserAccountController.java`、`RoleController.java`、`Permissions.java`、`database/seed/002_seed_roles_permissions.sql`。
- 验收标准：新增并使用 `*:create/*:update/*:delete/*:manage` 权限；THIRD_PARTY_OPS 的 view 权限不能 POST/PUT/DELETE；系统用户/角色修改只有授权管理员可执行。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是，操作权限边界。

### AUTH-MATRIX-VERIFY-002-P0-006 补齐 DataScope 集成测试

- 问题描述：用户要求的 `DataScopeIntegrationTest` 不存在，现有测试不能证明三方用户只看授权范围。
- 影响范围：所有三方数据隔离判断。
- 建议修改文件：新增 `backend/src/test/java/com/dcim/platform/common/security/DataScopeIntegrationTest.java`。
- 验收标准：覆盖无 scope、单 fsuScope、单 stationScope、越权 ID、聚合 API、raw XML 下载、分页场景。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是。

## P1

### AUTH-MATRIX-VERIFY-002-P1-001 补齐 HTTP 鉴权状态测试

- 问题描述：用户要求的 `AuthHttpStatusTest` 不存在。
- 影响范围：401/403 语义回归风险。
- 建议修改文件：新增 `backend/src/test/java/com/dcim/platform/common/security/AuthHttpStatusTest.java`。
- 验收标准：未认证敏感 API 返回 HTTP 401；无权限返回 HTTP 403；不再只依赖 `ApiResponse.code`。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

### AUTH-MATRIX-VERIFY-002-P1-002 补齐 adminLike 绕过测试

- 问题描述：用户要求的 `AdminLikeBypassTest` 不存在。
- 影响范围：平台管理员是否绕过 raw/run-once/SET 的回归风险。
- 建议修改文件：新增 `backend/src/test/java/com/dcim/platform/common/security/AdminLikeBypassTest.java`。
- 验收标准：`platform_admin/admin` 可绕过普通 view 权限但不能绕过 raw/run-once/SET；`super_admin` 行为明确。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是。

### AUTH-MATRIX-VERIFY-002-P1-003 补齐 AuditLog 持久化测试

- 问题描述：用户要求的 `AuditLogWiringTest` 不存在；现有测试多用 `AuditLogService(null)`。
- 影响范围：权限拒绝、raw、run-once、SET blocked 审计闭环。
- 建议修改文件：新增 `backend/src/test/java/com/dcim/platform/common/security/audit/AuditLogWiringTest.java`。
- 验收标准：JPA repository 持久化成功；字段包含 userId、username、action、resourceType、resourceId、fsuCode、permissionCode、allowed、reason、requestTime；审计失败不影响主流程。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是。

### AUTH-MATRIX-VERIFY-002-P1-004 run-once 失败路径审计

- 问题描述：run-once Controller 只在 service 返回后写审计，参数校验失败和异常路径未记录 RUN_ONCE 审计。
- 影响范围：GET_FSUINFO/GET_DATA probe 审计。
- 建议修改文件：`BInterface2016ReadOnlyRunOnceController.java`、对应测试。
- 验收标准：成功、业务失败、参数失败、异常均有审计，`allowed/success/reason` 清晰。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是。

### AUTH-MATRIX-VERIFY-002-P1-005 前后端权限码统一

- 问题描述：前端使用 `binterface.*`/`user.read`，后端使用 `fsu:view`/`user:view`，`AuthService.permList()` 与 seed SQL 也不一致。
- 影响范围：菜单、路由、按钮、后端 API 权限。
- 建议修改文件：`frontend/src/auth/permissions.ts`、`frontend/src/router/index.ts`、`frontend/src/layouts/BasicLayout.vue`、`backend/src/main/java/com/dcim/platform/module/auth/service/AuthService.java`、`database/seed/002_seed_roles_permissions.sql`。
- 验收标准：登录返回的 permissions 可同时驱动前端路由和后端 API；不再出现旧新权限码混用。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

### AUTH-MATRIX-VERIFY-002-P1-006 后端实时数据 meta 字段补齐

- 问题描述：前端已支持 ACK 空数据等状态，但 `/api/b-interface/realtime-points` 未返回 `realDeviceAccessed/ackReceived/emptyData/valuesReturned/unmappedCount/parseError` meta。
- 影响范围：实时数据状态口径。
- 建议修改文件：`BInterfaceFrontendReadController.java`、相关 DTO/service。
- 验收标准：前端能真实区分 API 不存在、HTTP 200 空数据、FSU ACK 空数据、FSU 通信失败、解析失败、未映射。
- 是否涉及后端：是。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

## P2

### AUTH-MATRIX-VERIFY-002-P2-001 DataScope 下沉到 Repository/SQL 层

- 问题描述：当前 DataScope 为内存过滤，可能泄露分页总数/排序信息，且大数据量性能不可控。
- 影响范围：所有列表查询。
- 建议修改文件：各 Repository、service、entity schema。
- 验收标准：tenantId/stationId/fsuCode 在 SQL WHERE 层过滤；分页总数基于授权范围计算。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

### AUTH-MATRIX-VERIFY-002-P2-002 完善租户与授权模型

- 问题描述：Token 中 tenantId 仍为 null，登录链路没有真实 stationScope/fsuScope 分配。
- 影响范围：三方授权商业化。
- 建议修改文件：用户/角色/租户/站点授权相关 entity、repository、service、seed。
- 验收标准：登录后 token 或 RequestContext 含 tenantId、stationScope、fsuScope；三方管理员只能管理本租户授权范围。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

## P3

### AUTH-MATRIX-VERIFY-002-P3-001 三方授权页面规划

- 问题描述：后端 P0 未闭合前不应实现页面，但可以保留信息架构草案。
- 影响范围：用户管理、角色管理、权限点、租户、站点授权、FSU 授权、操作审计。
- 建议修改文件：后续 FE-P1/FE-P2 设计文档。
- 验收标准：只输出页面结构和字段草案，不接真实权限 API 实现。
- 是否涉及后端：否。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。
