# CODEX-FE-P0-RECTIFY-001 前端 P0 权限与状态口径复审

复审日期：2026-05-29

复审对象：`FE-P0-RECTIFY-001` 前端 P0 安全与状态口径整改。

复审边界：
- 本次只做审计复核和文档输出。
- 未修改前端业务代码、后端业务代码、数据库 schema 或配置开关。
- 未访问真实 FSU。
- 未执行 SET_POINT / SET_THRESHOLD / SET_FTP / SET_LOGININFO / SET_FSUREBOOT。
- 未启用 Scheduler。

## 1. 结论摘要

`FE-P0-RECTIFY-001` **未完全通过复审**。

已通过的部分：
- `npm run build` 通过。
- 布局下业务路由基本补齐 `requiresAuth`。
- raw XML 页面路由加了 `protocol:raw:view` 和高权限角色限制。
- raw XML 查看、下载按钮加了 `PermissionGuard`。
- run-once 相关 UI 块加了 `protocol:runonce:readonly` 保护。
- 普通实时数据页没有调用真实 GET_DATA probe。
- 8 个 SignalID 白名单已集中到 `frontend/src/compat/realtimeSignalFilter.ts`，没有继续散落在页面组件中。
- `PointCard` 仍能显示 `0.0`，DI 未硬编码 0/1 语义，未知单位仍显示“单位待确认”。
- SET 类命令未发现可点击执行入口。

未通过原因：
- 仍存在多处“后端/接口未接入”误报，其中部分后端端点已经存在。
- 实时数据状态 taxonomy 只在前端预留了 meta 解析，当前后端 `/b-interface/realtime-points` 实际返回 `ApiResponse<List<RealtimePointDto>>`，没有 `realDeviceAccessed / ackReceived / emptyData / parseError / unmappedCount` meta，前端不能真实区分全部要求状态。
- `routeGuard` 和 `PermissionGuard` 的多权限数组均采用 OR 语义。对于 `['binterface.fsu.read', 'protocol:runonce:readonly']` 这类 meta，不能表达“同时需要两个权限”，FE-P0 报告中“普通用户不能进 run-once 入口路由”的说法被放大了。当前 run-once 卡片内部有 guard，因此暂无可点击执行风险，但策略语义不严谨。
- 后端鉴权、数据范围、raw XML API 保护仍缺失，三方授权生产仍被阻塞。

最终判断：
- FE-P0-RECTIFY-001：**不通过完整复审，需补一个前端 P0 小修任务**。
- P0 遗留：**存在**，主要是状态口径误报和后端鉴权阻塞。
- 是否可以进入 BE-AUTH-P0-001：**可以，而且应立即进入**。
- 是否可以暂缓 FE-P1 页面建设：**可以，且建议暂缓**，先完成后端鉴权和前端 P0 残余口径修复。
- 后端鉴权缺失是否仍为三方授权生产阻塞项：**是，仍是生产阻塞项**。

## 2. 已读取文件清单

前端重点文件：
- `frontend/src/types/auth.ts`
- `frontend/src/stores/user.ts`
- `frontend/src/auth/permissions.ts`
- `frontend/src/auth/routeGuard.ts`
- `frontend/src/auth/access.ts`
- `frontend/src/router/index.ts`
- `frontend/src/layouts/BasicLayout.vue`
- `frontend/src/components/auth/PermissionGuard.vue`
- `frontend/src/compat/realtimeSignalFilter.ts`
- `frontend/src/api/bInterface.ts`
- `frontend/src/api/request.ts`
- `frontend/src/components/common/ApiErrorAlert.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/views/binterface/MessageLogView.vue`
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
- `frontend/src/views/binterface/BInterfaceSchedulerView.vue`
- `frontend/src/views/binterface/BInterfaceThresholdView.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/binterface/BInterfaceOverviewView.vue`
- `frontend/src/views/binterface/PointCard.vue`
- `frontend/src/views/binterface/BInterfaceFtpLoginInfoView.vue`
- `frontend/src/views/binterface/BInterfaceFtpImageView.vue`
- `frontend/package.json`

后端辅助对照文件：
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterface2016ReadOnlyRunOnceController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceMessageLogController.java`
- `backend/src/main/java/com/dcim/platform/module/auth/controller/AuthController.java`
- `backend/src/main/java/com/dcim/platform/module/auth/service/AuthService.java`
- `backend/pom.xml`

已读取前置文档：
- `docs/audit/FE-P0-RECTIFY-001-frontend-p0-security-and-status-fix.md`
- `docs/memory/2026-05-29-FE-P0-RECTIFY-001-frontend-p0-security-and-status-fix.md`

## 3. 权限模型复审

### 3.1 通过项

- `PlatformRole.code` 已扩展 `super_admin`、`platform_admin`、`protocol_debugger`。
- `stores/user.ts` 增加 `isSuperAdmin`、`isPlatformAdmin`、`isProtocolDebugger`、`isElevatedUser`。
- `permissions.ts` 增加：
  - `protocol:raw:view`
  - `protocol:raw:download`
  - `protocol:runonce:readonly`
- `routeGuard.ts` 增加 `meta.roles` 检查。
- `router/index.ts` 的业务子路由已补 `requiresAuth: true`。
- `/b-interface/logs`、`/b-interface/calls` 增加 `protocol:raw:view` 和高权限角色。
- `/b-interface/schedulers` 增加 `protocol:runonce:readonly`。

### 3.2 发现的问题

#### P1-权限语义：多权限数组采用 OR 语义，不能表达“同时需要”

证据：
- `frontend/src/auth/routeGuard.ts:25-29` 使用 `required.some(...)`。
- `frontend/src/components/auth/PermissionGuard.vue:29-33` 使用 `hasAnyPermission(...)`。
- `frontend/src/router/index.ts:24` FSU 详情页配置 `['binterface.fsu.read', 'protocol:runonce:readonly']`。
- `frontend/src/router/index.ts:27` 门限页配置 `['binterface.threshold.read', 'protocol:runonce:readonly']`。

影响：
- 拥有 `binterface.fsu.read` 但没有 `protocol:runonce:readonly` 的用户仍能进入 FSU 详情页。
- 当前页面内 run-once 卡片被 `PermissionGuard` 包住，因此没有直接执行风险。
- 但 FE-P0 报告中“普通用户不能进 run-once 入口路由”的结论不严格。

建议：
- 明确 route meta 支持 `permissionsAny` 和 `permissionsAll`。
- 普通详情页只要求读权限，run-once 卡片和未来按钮要求 `permissionsAll: ['protocol:runonce:readonly']`。
- 不要把“页面查看权限”和“协议操作权限”混在同一个 OR 数组里。

#### P1-权限点口径：角色管理权限编码仍不完全一致

证据：
- `frontend/src/router/index.ts:37` 角色管理路由使用 `role.read`。
- `frontend/src/auth/permissions.ts:51` 常量中有 `ROLE_READ: 'role.read'`。
- `frontend/src/auth/permissions.ts:95` 元数据列表只有 `role.manage`，没有 `role.read`。
- `frontend/src/views/system/PermissionManagementView.vue:91` 仍展示 `role.manage`。

影响：
- 后端如果只返回 `role.read`，权限页元数据展示和角色规划页口径不一致。
- 后续后端权限点表落地时容易出现菜单可见但权限页不展示的错配。

建议：
- `PERMISSION_META_LIST` 补齐 `role.read / role.create / role.update / role.delete`，或路由统一改为 `role.manage`。

## 4. raw XML 复审

### 4.1 通过项

- `/b-interface/logs` 路由已要求 `requiresAuth + protocol:raw:view + roles`。
- `BasicLayout.vue` 只在 `userStore.isElevatedUser` 时展示“B接口报文日志”和“FSUService调用记录”菜单。
- `MessageLogView.vue` 的“原始XML”按钮使用 `PermissionGuard :permissions="['protocol:raw:view']"`。
- `MessageLogView.vue` 的“下载”按钮使用 `PermissionGuard :permissions="['protocol:raw:download']"`。
- 普通用户和三方用户如果没有新角色和权限，不能通过菜单进入 raw XML 页面；直接输入 URL 会被 route guard 拦到 `/403`。

### 4.2 残余风险

#### P0-后端阻塞：raw XML API 仍无后端鉴权

证据：
- `BInterfaceMessageLogController.java:47-57` 直接返回 `BInterfaceMessageLogEntity`。
- `BInterfaceMessageLogController.java:72-86` 分页 query 也返回 `Page<BInterfaceMessageLogEntity>`。
- `BInterfaceMessageLogController.java:99-110` cleanup 接口无权限保护。
- 后端检索未发现 `SecurityFilterChain / OncePerRequestFilter / HandlerInterceptor / @PreAuthorize`。

影响：
- 前端 route guard 不能阻止直接 API 调用。
- raw XML 真实安全边界仍未闭合。

结论：
- 前端 raw XML 展示控制基本通过。
- 生产安全仍必须依赖 `BE-AUTH-P0-001`，否则三方授权不可上线。

## 5. run-once 复审

### 5.1 通过项

- `FsuStatusDetailView.vue:40-45` 的 GET_FSUINFO run-once 区块受 `protocol:runonce:readonly` 保护。
- `BInterfaceSchedulerView.vue:58-67` 的 run-once / run-due 占位区块受 `protocol:runonce:readonly` 保护。
- `BInterfaceThresholdView.vue:53-57` 的 GET_THRESHOLD run-once 占位区块受 `protocol:runonce:readonly` 保护。
- `BInterfaceRealtimeView.vue` 只调用：
  - `getMonitoringPoints()`
  - `getBInterfaceRealtimePoints({ fsuCode })`
  - `getBInterfaceFsus({ fsuCode })`
- `rg` 结果确认 `runGetDataProbe` 和 `runFsuInfoReadOnly` 仅在 `api/bInterface.ts` 导出，未被页面调用。
- SET_POINT / SET_THRESHOLD / SET_FTP / SET_LOGININFO / SET_FSUREBOOT 未发现可点击执行入口。

### 5.2 发现的问题

#### P1-run-once 范围：页面占位文案包含非要求命令

证据：
- `BInterfaceSchedulerView.vue:61-64` 展示 `GET_DATA run-once`、`GET_FSUINFO run-once`、`FTP 图片 run-once`、`一致性审计 run-once`。
- 后端 `BInterfaceFrontendReadController.java:168` capabilities 包含 `GET_THRESHOLD`、`GET_ACTIVEALARM`、`FTP_IMAGE_PULL`。

影响：
- 用户要求的只读命令范围是 `GET_FSUINFO / GET_DATA / GET_LOGININFO / GET_FTP`。
- 当前只是占位，不是可点击执行入口，因此不是 P0。
- 但后续实现时要防止 scope 扩散。

建议：
- run-once 工作台明确拆分：
  - P0 安全允许范围：`GET_FSUINFO / GET_DATA / GET_LOGININFO / GET_FTP`
  - 其他能力：标记“未来评估，不在当前授权范围”

## 6. 实时数据状态口径复审

### 6.1 已完成项

- 页面不再把所有异常统一显示为“后端未接入”。
- 已有前端状态枚举：`ok / empty / fsu_no_ack / fsu_ack_empty / parse_error / api_error`。
- HTTP 200 且 data 为空时显示“暂无实时数据”。
- `meta.realDeviceAccessed=false` 或 `ackReceived=false` 时显示“FSU 通信失败或未访问真实设备”。
- `meta.ackReceived && meta.emptyData` 时显示“FSU 已 ACK，但未返回测点值”。
- `meta.parseError` 时显示“协议响应解析失败”。
- 可展示 `unmappedCount` 或按 `mappingStatus==='unmapped'` 统计。

### 6.2 未完成项

#### P0-状态口径：API 不存在没有明确分类

证据：
- `BInterfaceRealtimeView.vue:100-103` catch 中只对网络错误设置 `api_error`，404/500 仍只落到 `ApiErrorAlert` 的 `LOAD_FAILED`。
- `ApiErrorAlert` 只展示传入的 `error.code/message`，没有把 404 归类为“后端未接入/API 不存在”。

影响：
- 还不能满足“API 不存在 → 后端未接入”的明确口径。

#### P1-状态口径：ACK 空数据、真实访问、解析失败依赖后端 meta，但当前 API 未返回 meta

证据：
- `BInterfaceRealtimeView.vue:92-97` 从 `rtRes.meta || rtRes.data.meta` 读取 meta。
- `BInterfaceFrontendReadController.java:197-213` 当前 `/realtime-points` 返回 `ApiResponse<List<RealtimePointDto>>`，未返回 meta。

影响：
- 前端代码有状态分支，但当前真实 API 无法驱动这些分支。
- 现阶段仍不能真实区分 `realDeviceAccessed / ackReceived / valuesReturned / emptyData / parseError`。

建议：
- 后端补 `RealtimePointPageDto` 或 `RealtimeDataStatusDto`，包含：
  - `apiStatus`
  - `realDeviceAccessed`
  - `ackReceived`
  - `valuesReturned`
  - `emptyData`
  - `parseError`
  - `unmappedCount`
  - `lastGetDataTime`
- 前端把 404/501/404-like 错误明确映射为“后端未接入”。

## 7. 硬编码过滤复审

### 7.1 通过项

- 8 个 SignalID 白名单只存在于 `frontend/src/compat/realtimeSignalFilter.ts:17-26`。
- 该文件顶部明确标注：
  - `FE-P0-RECTIFY-001: 实时数据临时兼容过滤层`
  - `WARNING: 这是临时保护逻辑，不是正式点位映射方案`
  - `@deprecated`
  - 待后端 `mappingStatus / dataSource / isDemo` 字段补齐后删除
- 页面组件不再直接散写 8 个 SignalID。

### 7.2 残余问题

#### P1-兼容债务：`fsuId === 2` 仍存在于 compat

证据：
- `realtimeSignalFilter.ts:31-41` 当前逻辑仍为 `point.fsuId === 2 && HARDCODED_REALTIME_SIGNAL_IDS.has(...)`。

影响：
- 白名单已集中封装，降低扩散风险。
- 但这仍不是正式点位映射方案，不能作为三方授权或多 FSU 数据隔离基础。

结论：
- “白名单不散落”通过。
- “彻底消除硬编码过滤”未完成，保留为 P1，待后端 mapping 字段补齐后删除 compat。

## 8. 展示逻辑复审

通过：
- `PointCard.vue:49` 使用 `props.rt?.valueNumber != null` 判断数值，`0.0` 不会被误判为空。
- `PointCard.vue:15` 使用 `rt.valueNumber ?? rt.value` 展示，`0.0` 可正常显示。
- `PointCard.vue:47` 仅通过 `pointType === 'DI'` 判断 DI 类型。
- `PointCard.vue:29-30` 显示“DI 编码待现场复核”，未硬编码 0/1 语义。
- `PointCard.vue:9` 和 `PointCard.vue:18` 对未知单位显示“单位待确认”。

## 9. “后端未接入”误报复审

该项 **未通过**。

仍有误报或口径不准确：
- `FsuStatusDetailView.vue:42-43` 显示“后端 REST 接口未接入”，但后端已有 `/api/b-interface/2016/read-only/fsu-info/run-once`。
- `BInterfaceSchedulerView.vue:36-37` 显示“调度配置接口未接入”，但后端已有 `/api/b-interface/schedulers/configs`。
- `BInterfaceSchedulerView.vue:53-54` 显示“调度运行记录接口未接入”，但后端已有 `/api/b-interface/schedulers/runs`。
- `BInterfaceThresholdView.vue:39-40` 显示“B接口门限接口未接入”，但后端已有 `/api/b-interface/thresholds`，当前是空数据或占位，不是未接入。
- `frontend/src/api/auth.ts:8` 仍保留“后端接口未接入，所有方法目前返回 404”的 TODO，但后端已有 `/api/auth/*`。

扩展发现：
- `BInterfaceFtpLoginInfoView.vue`、`BInterfaceFtpImageView.vue` 也仍存在已接入端点被文案标为“接口未接入”的情况。

整改建议：
- “端点存在但返回空 list/map”统一显示“暂无数据”或“只读占位”。
- “真实调用被配置关闭”显示“真实调用已禁用”。
- “确实 404/501”才显示“后端未接入/API 不存在”。

## 10. SET 类命令入口复审

通过：
- 未发现 `SET_POINT / SET_THRESHOLD / SET_FTP / SET_LOGININFO / SET_FSUREBOOT` 的可点击执行按钮。
- `BInterfaceThresholdView.vue:60-64` 的 SET_THRESHOLD 只是规划卡片，且在无 SET 权限时用 `PermissionGuard mode="disable"` 包住。
- `BInterfaceOverviewView.vue:16` 明确显示 `SET_FSUREBOOT` 默认禁用，不可通过界面启用。
- `CommandMatrixView.vue` 和 `BInterfaceProtocolAuditView.vue` 仅展示矩阵/策略，不提供执行。

注意项：
- `BInterfaceFtpLoginInfoView.vue:79-88` 的 SET_FTP / SET_LOGININFO 规划卡片未加 PermissionGuard，但没有按钮、没有 API 调用，不构成 P0。
- 建议后续统一隐藏或禁用所有 SET 规划卡片，避免普通三方用户误解。

## 11. 构建验证

执行命令：

```bash
npm run build
```

结果：
- `vue-tsc --noEmit` 通过。
- `vite build` 通过。
- 输出 `✓ built in 7.37s`。

警告：
- Rollup 对 `@vueuse/core` 的 `#__PURE__` 注释提示，第三方包构建警告，不影响本次结果。
- `index` chunk 超过 500 kB，属于性能优化提示，不是 P0。

测试：
- `frontend/package.json` 未定义 `test` 脚本，因此没有可执行的前端测试命令。

## 12. 分项复审结论

| 复审项 | 结论 |
|---|---|
| 权限模型 | 部分通过。路由和角色已补，但多权限 OR 语义不严谨，权限点口径仍有不一致 |
| raw XML | 前端展示控制通过；后端 API 鉴权缺失仍为 P0 阻塞 |
| run-once | 当前无可点击执行风险；页面内 guard 通过；route meta 口径需修正 |
| 实时数据状态 | 部分通过。前端有分支，但 API 不存在分类和后端 meta 未落地 |
| 硬编码过滤 | 白名单集中通过；`fsuId===2` 仍是 P1 兼容债务 |
| 0.0 / DI / 单位 | 通过 |
| SET 类入口 | 未发现可点击执行入口，P0 通过 |
| 构建 | 通过 |

## 13. P0 / P1 遗留

### P0 遗留

1. 后端鉴权、数据范围、raw XML API 保护仍缺失，三方授权生产阻塞。
2. 前端仍有“后端/接口未接入”误报，且部分属于已存在端点。
3. 实时数据对“API 不存在”的状态没有明确分类，404/500 仍落到泛化 `LOAD_FAILED`。

### P1 遗留

1. `routeGuard`/`PermissionGuard` 多权限数组使用 OR 语义，需补 `permissionsAll`。
2. `/realtime-points` 后端未返回状态 meta，前端 ACK 空数据等状态无法真实驱动。
3. `compat/realtimeSignalFilter.ts` 仍包含 `fsuId === 2` 临时兼容逻辑。
4. run-once 能力清单和页面占位包含当前授权范围之外的能力，需在后续实现前收敛。
5. SET_FTP / SET_LOGININFO 规划卡片建议统一加 PermissionGuard 或隐藏。
6. `role.read` / `role.manage` 权限点口径仍需统一。

## 14. 最终回答

1. FE-P0-RECTIFY-001 是否通过复审：**未完全通过**。构建通过，前端权限展示控制有明显改进，但状态口径误报和后端安全边界仍未闭合。
2. 是否存在 P0 遗留：**存在**。后端鉴权/raw XML API 保护仍是 P0；前端“未接入”误报和 API 不存在分类也仍需 P0 小修。
3. 是否可以进入 BE-AUTH-P0-001：**可以，并建议立即进入**。这是三方授权生产的主阻塞项。
4. 是否可以暂缓 FE-P1 页面建设：**可以，建议暂缓**。先完成 BE-AUTH-P0-001 和前端 P0 残余口径修复。
5. 后端鉴权缺失是否仍为三方授权生产阻塞项：**是**。前端 guard 只能做展示和导航控制，不能作为真实数据隔离或操作拦截。
