# AUTH-MATRIX-VERIFY-002 后端 P0 修复后权限矩阵复验

## 本次目标

复验 BE-AUTH-P0-FIX-001 后前后端权限矩阵是否形成 P0 安全闭环，重点确认后端 API 权限、raw XML view/download、run-once、SET 拦截、DataScope 三方隔离、AuditLog 审计，以及前端权限矩阵和实时数据状态口径。

## 审计结论摘要

AUTH-MATRIX-VERIFY-002 未通过。仍存在 P0，不可进入 FE-P1 页面建设，也不可将三方授权页面规划作为可实施阶段推进。

已通过的部分：

- `SecurityContextFilter` + `AuthInterceptor` + `@RequirePermission` 基础链路存在。
- HTTP 401/403 已由 `ResponseEntity` 保证。
- adminLike 不再绕过 raw/run-once/SET 敏感权限，只有 `super_admin` 可绕过。
- run-once 端点要求 `protocol:runonce:readonly`，当前 REST 未暴露 SET。
- SET 安全门默认拒绝仍成立。
- 前端 raw/run-once 路由和按钮有 guard，实时状态口径和 `0.0` 展示通过。

## 发现的关键问题

P0 遗留：

- raw XML 下载端点未设置 `requireAll=true`，`protocol:raw:download` 未形成独立强制条件。
- message-log/call-record 的按 ID 查看和下载未做 `fsuScope` 校验。
- `/api/b-interface` 聚合读取 Controller 仍直接 `findAll()`，绕过 DataScope。
- 多个业务资源 `getById()` 可绕过列表过滤直接返回实体。
- `HistoryDataService`、`DeviceHeartbeatService`、`FsuDeviceService` 等仍存在未过滤全量查询。
- `site:view/fsu:view/user:view/role:view` 类级权限覆盖 POST/PUT/DELETE，查看权限可执行写操作。

P1 遗留：

- 缺少用户要求的 `AuthHttpStatusTest`、`AdminLikeBypassTest`、`DataScopeIntegrationTest`、`AuditLogWiringTest`。
- AuditLog 接线存在，但 JPA 持久化和全路径审计未被测试证明。
- run-once 失败路径未写 RUN_ONCE 审计。
- 前后端权限码不统一：前端仍有 `binterface.*`/`user.read`，后端使用 `*:view`。
- 后端实时数据 API 未返回前端所需 meta 字段。

## 角色管理和三方授权结论

当前不能认为三方授权最小闭环成立。

虽然 seed SQL 定义了 `super_admin/platform_admin/protocol_debugger/admin/operator/viewer`，但：

- `tenantId` 仍未进入 RequestContext。
- 登录链路默认 `stationScope/fsuScope` 为空，没有真实租户授权来源。
- 部分 scope 过滤未接入或语义不一致。
- view 权限可写资源，操作权限边界不足。

## 验证结果

- 指定后端测试：`mvn test -Dtest='SecurityInfrastructureTest,BInterface2016SetCommandSafetyTest,ReadOnlyIntegrationSafetyTest,BInterface2016ReadOnlyRunOnceProbeIntegrationTest,*CommandHandlerTest'`，217 tests，0 failures。
- 前端构建：`npm run build` 通过。
- 全量后端测试：`mvn test`，1450 tests，4 failures，24 skipped。4 个失败集中在 `CommandResultTest` 和 `FsuServiceRpcAdapterTest`，属于协议 ResultCode/SOAP ENC 旧问题，不是本次鉴权修复直接失败。
- 未访问真实 FSU，未执行 SET，未启 Scheduler。run-once probe 测试使用 fake/stub client。

## 后续优先级

1. P0：修 raw XML 下载 AND 权限和 raw scope 校验。
2. P0：修 `/api/b-interface` 聚合 API 和所有按 ID 查询的 DataScope。
3. P0：拆分写权限，禁止 view 权限写资源。
4. P0：补 DataScope 集成测试。
5. P1：补 HTTP 状态、adminLike、AuditLog wiring 测试。
6. P1：统一前后端权限码。
7. P1：补实时数据 meta。

## 未修改代码说明

本次按用户要求执行复验和文档输出，未修改业务代码。仅更新审计报告、整改 TODO 和 memory 文件。运行测试和前端 build 产生的 `target/`、`dist/`、raw sample 等工作区变化保留，不做回滚。

## 下一步建议

先执行 `BE-AUTH-P0-FIX-002` 或等价后端 P0 修复任务，修完后再执行 `AUTH-MATRIX-VERIFY-003`。P0 全部闭合前，暂缓 FE-P1 页面建设和三方授权页面实现。
