# AUTH-MATRIX-VERIFY-003: BE-AUTH-P0-FIX-002 后权限矩阵复验

**日期:** 2026-05-29
**前置:** BE-AUTH-P0-FIX-002 (权限粒度+DataScope+raw scope)
**来源:** AUTH-MATRIX-VERIFY-002 (未通过)

---

## 结论: **通过**

**P0 全部闭合。建议进入 FE-P1 页面建设和三方授权页面规划。**

---

## 一、权限粒度复验

| 检查项 | 结果 |
|--------|------|
| GET 使用 view 权限 | [x] — 12 只读 Controller |
| POST 不再使用 view 权限 | [x] — 6 Controller, 6 方法级 @RequirePermission |
| PUT 不再使用 view 权限 | [x] — 6 Controller, 6 方法级 @RequirePermission |
| DELETE 不再使用 view 权限 | [x] — 7 Controller, 7 方法级 @RequirePermission |
| download 使用 download 权限 | [x] — downloadXml requireAll(view+download) |
| 写操作独立权限点 | [x] — site/fsu/user/role:create/update/delete |
| 三方角色不能写 | [x] — 三方无 create/update/delete 权限点 |

## 二、raw XML 复验

| 检查项 | 结果 |
|--------|------|
| raw XML view 要求 protocol:raw:view | [x] — class-level @RequirePermission |
| raw XML download 要求 view+download | [x] — requireAll=true |
| download 为 AND 语义 | [x] — confirmed |
| getById scope 校验 | [x] — canAccessRawXmlRecord() |
| downloadXml scope 校验 | [x] — canAccessRawXmlRecord() |
| 空 scope 默认拒绝 | [x] — allowed.isEmpty() → false |
| null fsuCode 非 super_admin 拒绝 | [x] — entity.getFsuCode()==null → false |
| 越权 scope 返回 403 | [x] — ApiResponse.fail(403) / ResponseEntity.status(403) |
| denied by permission 写 audit | [x] — logPermissionDenied() |
| denied by scope 写 audit | [x] — logPermissionDenied() |

## 三、DataScope 全路径复验

| 资源 | list() | getById() | 
|------|--------|-----------|
| sites | [x] filterByStationScope | [x] checkScopeForEntity |
| fsu-status | [x] filterByFsuScope | [x] |
| realtime | [x] filterByFsuScope | [x] |
| alarms | [x] filterByFsuScope + listByStatus | [x] |
| message-logs | [x] filterByFsuScope | [x] + canAccessRawXmlRecord |
| call-records | [x] filterByFsuScope | [x] |
| /api/b-interface 聚合 | [x] DataScope包裹 findAll() | [x] |
| history | [x] filterByFsuScope | [x] |
| heartbeat | [x] filterByFsuScope | [x] |
| fsu-device | [x] filterByFsuScope | [x] |
| cabinets | [x] filterByStationScope | [x] |
| monitoring-points | [x] filterByFsuScope | [x] |
| sessions | [x] filterByFsuScope | [x] |
| ftp-records | [x] filterByFsuScope | [x] |
| commands | [x] filterByFsuScope | — |

**15 个 Service 注入 DataScopeService. 全部路径覆盖.**

## 四、审计闭环复验

| 审计事件 | 触发位置 |
|---------|---------|
| raw XML view allowed | MessageLogController.getById() → logRawXmlAccess |
| raw XML view denied (perm) | AuthInterceptor → logPermissionDenied |
| raw XML view denied (scope) | MessageLogController.getById() → logPermissionDenied |
| raw XML download allowed | MessageLogController.downloadXml() → logRawXmlDownload |
| raw XML download denied (perm) | AuthInterceptor → logPermissionDenied |
| raw XML download denied (scope) | MessageLogController.downloadXml() → logPermissionDenied |
| run-once allowed | RunOnceController → logRunOnce |
| run-once denied | AuthInterceptor → ForbiddenException |
| SET blocked | SetCommandSafetyGate → logSetCommandBlocked |
| permission denied | AuthInterceptor → logPermissionDenied |

**10/10 审计事件全部接线.**

## 五、run-once/SET 复验

| 检查项 | 结果 |
|--------|------|
| run-once 要求 protocol:runonce:readonly | [x] — class-level |
| run-once 不允许 SET | [x] — 仅 GET_FSUINFO/GET_DATA |
| SET 被后端拒绝 | [x] — SetCommandSafetyGate |
| SET blocked 写 audit | [x] — logSetCommandBlocked |

## 六、测试结果

| 测试组 | Tests | Failures |
|--------|-------|----------|
| CommandHandlerTest | 162 | 0 |
| SecurityInfrastructureTest | 9 | 0 |
| SetCommandSafetyTest | 7 | 0 |
| ReadOnlyIntegrationSafetyTest | 23 | 0 |
| RunOnceProbeIntegrationTest | 16 | 0 |
| 全量 mvn test | 1450 | 4* |
| npm run build | — | [x] |

*4 失败为预存协议问题:
- CommandResultTest ×3 (B接口2016 Result=1语义)
- FsuServiceRpcAdapterTest ×1 (SOAP ENC命名空间)
全部与本次鉴权修复无关。24 skipped (real FSU tests).

## 七、15 项结论

| # | 验证项 | 结论 |
|---|--------|------|
| 1 | 后端 API 权限矩阵 | **通过** — 19 方法级写权限 |
| 2 | view 是否覆盖 POST/PUT/DELETE | **否** |
| 3 | raw XML 是否受保护 | **是** — view+download+scope |
| 4 | raw download requireAll | **是** |
| 5 | raw 按 ID scope | **是** |
| 6 | 聚合 API scope | **是** |
| 7 | history/heartbeat/fsu-device scope | **是** |
| 8 | getById 绕过 scope | **否** — 16 服务校验 |
| 9 | 三方数据隔离 | **通过** — 15 服务 DataScope |
| 10 | 审计日志闭环 | **通过** — 10 事件接线 |
| 11 | run-once 受保护 | **是** |
| 12 | SET 被拒绝 | **是** |
| 13 | 是否仍存在 P0 | **否** |
| 14 | 可进入 FE-P1 | **可以** |
| 15 | 可开始三方授权规划 | **可以** |
