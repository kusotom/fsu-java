# FE-P0-RECTIFY-002 前端P0状态口径与权限语义小修

**日期**: 2026-05-29
**类型**: 前端 P0 修复
**父任务**: CODEX-FE-P0-RECTIFY-001 复审遗留问题
**范围**: 仅前端，不访问真实 FSU，不执行 SET，不启 Scheduler

## 结论

**FE-P0-RECTIFY-002 已完成。**

- 前端状态口径误报: **已闭合** — 9 种状态精确区分
- 权限 OR/AND 语义: **已闭合** — permissions=all-of, roles=any-of, 两者=AND
- 实时 meta 缺失: **已处理** — 10 字段补齐，缺失时优雅降级为"未提供"
- 仍依赖 BE-AUTH-P0-001 后端安全闭环: **是** — 前端仅展示控制，真实安全由后端校验
- 建议进入 AUTH-MATRIX-VERIFY-001: **是** — 权限矩阵验证

## 一、状态口径修复

### 修改文件

1. `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
2. `frontend/src/views/telemetry/RealtimeDataView.vue`

### 状态分类 (9 种)

| 状态 | 触发条件 | Alert 类型 |
|------|----------|-----------|
| `api_404` | HTTP 404 | warning — 后端未接入 / API 不存在 |
| `network_error` | 无响应 (e.request && !e.response) | error — 网络异常或后端不可达 |
| `http_401` | HTTP 401 | warning — 未登录或登录失效 |
| `http_403` | HTTP 403 | warning — 无权限访问 |
| `http_5xx` | HTTP 5xx | error — 后端服务异常 |
| `empty` | HTTP 200 + data 为空 | info — 暂无实时数据 |
| `fsu_ack_empty` | ACK + emptyData=true | info — FSU 已 ACK，但未返回测点值 |
| `fsu_no_ack` | ackReceived=false / realDeviceAccessed=false | warning — FSU 通信失败 |
| `parse_error` | parseError=true | warning — 协议响应解析失败 |
| `unmapped` | unmappedCount > 0 | warning — 存在未映射点位 |
| `ok` | 正常 | 无 alert (显示链路状态) |

### BInterfaceRealtimeView 具体改动

- 模板新增 `api_404`、`network_error`、`http_401`、`http_403`、`http_5xx`、`unmapped` 6 个 alert banner
- 新增 `el-descriptions` meta 信息面板，展示 realDeviceAccessed/ackReceived/emptyData/valuesReturned/unmappedCount/lastCollectTime/lastGetDataTime/parseError
- `loadData()` 中:
  - catch 块从简单的 network-vs-HTTP 二分改为精确识别 404/401/403/5xx/network
  - meta 字段补全：lastCollectTime、lastGetDataTime、errorCode、errorMessage、valuesReturned
  - meta 缺失时优雅降级（显示"未提供"），不报错
  - metaVisible 仅在后端返回 meta 字段时才展示面板

### RealtimeDataView 具体改动

- 模板新增 6 个 alert banner (`api_404`/`network_error`/`http_401`/`http_403`/`http_5xx`/`empty`)
- `onMounted` 从单行 catch-all 改为结构化错误处理，精确区分 404/401/403/5xx/network
- 兼容无 meta 的普通 telemetry API，仅使用 HTTP 状态码判断
- 数据提取兼容多重响应格式 (`res`/`res.data`/`res.data.data`)

## 二、实时 meta 补齐

### 展示字段

| 字段 | 来源 | 缺失时降级 |
|------|------|-----------|
| realDeviceAccessed | meta.realDeviceAccessed | "未提供" |
| ackReceived | meta.ackReceived | "未提供" |
| emptyData | meta.emptyData | "未提供" |
| valuesReturned | meta.valuesReturned / rtData.length | 数据数组长度 |
| unmappedCount | meta.unmappedCount / 过滤 unmapped | 0 |
| lastCollectTime | meta.lastCollectTime | "未提供" |
| lastGetDataTime | meta.lastGetDataTime | "未提供" |
| parseError | meta.parseError | "否" |
| errorCode | meta.errorCode | 不展示 |
| errorMessage | meta.errorMessage | 不展示 |

### 兼容性

- 支持 camelCase (`realDeviceAccessed`) 和 snake_case (`real_device_accessed`) 两种后端返回格式
- meta 面板仅在检测到 meta 字段时才显示，避免对旧版后端产生 UI 干扰
- `RealtimeDataView.vue` 完全兼容无 meta 的传统 telemetry API

## 三、权限 OR/AND 语义

### 修改文件

1. `frontend/src/auth/routeGuard.ts`
2. `frontend/src/components/auth/PermissionGuard.vue`
3. `frontend/src/auth/access.ts`

### 统一语义

| 维度 | 语义 | 实现 |
|------|------|------|
| permissions | **all-of** | `required.every(p => userStore.permissions.includes(p))` |
| roles | **any-of** | `requiredRoles.some(r => userStore.hasRole(r))` |
| permissions + roles | **AND** | 两者独立检查，都通过才放行 |
| isAdmin | **全部绕过** | admin 用户不做权限检查 |

### routeGuard 改动

- `required.some(p => ...)` → `required.every(p => ...)` (all-of)
- roles 保持 `some()` (any-of)
- 添加 JSDoc 注释明确语义

### PermissionGuard 改动

- `hasAnyPermission` → `hasAllPermissions`
- 逻辑从 `if/else if` 链改为独立检查 + AND 合并
- `permOk && roleOk`：两者都指定时，两者都必须满足
- 添加 JSDoc 注释

### 受影响路由清单

| 路由 | 权限 | 影响 |
|------|------|------|
| `/b-interface/fsus/:fsuCode` | `binterface.fsu.read` + `protocol:runonce:readonly` | 现在需要**同时**拥有两个权限 |
| `/b-interface/thresholds` | `binterface.threshold.read` + `protocol:runonce:readonly` | 现在需要**同时**拥有两个权限 |
| `/b-interface/logs` | `protocol:raw:view` + roles `[super_admin, platform_admin, protocol_debugger]` | 需要权限 AND 角色 |
| `/b-interface/calls` | `protocol:raw:view` + roles `[super_admin, platform_admin, protocol_debugger]` | 需要权限 AND 角色 |
| 所有单权限路由 | 无变化 | 单权限数组 all-of 等价于 any-of |

### 受影响 PermissionGuard 调用点

| 组件 | 权限 | 影响 |
|------|------|------|
| `BInterfaceThresholdView.vue` SET guard | `set_threshold.plan` + `set_threshold.execute` | 现在需要**同时**拥有两个权限 |
| 所有单权限 PermissionGuard | 无变化 | 单权限 all-of 等价于 any-of |

### 安全验证

- raw XML 路由 (`/b-interface/logs`, `/b-interface/calls`) 同时要求 `protocol:raw:view` 权限 AND `super_admin/platform_admin/protocol_debugger` 角色
- run-once 页面 (`/b-interface/fsus/:fsuCode`, `/b-interface/thresholds`, `/b-interface/schedulers`) 均要求 `protocol:runonce:readonly`
- 系统管理菜单仅 admin 可见 (`v-if="userStore.isAdmin"`)
- 报文日志/调用记录菜单仅 elevatedUser 可见 (`v-if="userStore.isElevatedUser"`)
- 普通用户（viewer）无法访问 raw XML 或 run-once 页面

## 四、dist 构建产物

`frontend/dist/` 已被 git 追踪（项目初始化时纳入），项目 `.gitignore` 未排除 dist。本次 `npm run build` 刷新了以下构建产物:

- `frontend/dist/index.html` — 入口文件 hash 引用更新
- `frontend/dist/assets/*.js` / `*.css` — 代码分块 hash 更新（正常构建行为）

**结论**: 保留 dist 变更，理由:
1. 项目当前规范为提交 dist（git 已追踪，无排除规则）
2. 构建产物非手工业务修改
3. 如需变更规范，应单独讨论并添加 `.gitignore` 规则

## 五、验证

- `npm run build`: **通过** (vue-tsc --noEmit + vite build, 0 errors)
- 前端无 `test` 脚本，未执行自动化测试

## 六、遗留问题

- **BE-AUTH-P0-001**: 后端鉴权、数据范围、raw XML API 保护仍缺失，前端权限仅展示控制，真实安全由后端校验
- **实时 meta 后端返回**: `/b-interface/realtime-points` 后端需返回 `realDeviceAccessed/ackReceived/valuesReturned/emptyData/parseError/unmappedCount` 等 meta 字段，前端才能真实驱动状态展示
- **权限矩阵验证**: 建议进入 AUTH-MATRIX-VERIFY-001，系统验证各角色在各路由/组件的实际权限表现
- `role.read` 与 `role.manage` 权限点口径不一致（P1 遗留）

## 七、安全边界 (遵守)

- [x] 不访问真实 FSU
- [x] 不执行 SET
- [x] 不启 Scheduler
- [x] 不做 FE-P1 页面建设
- [x] 不删除角色管理模块
- [x] 不改 B接口2016 主线

## 八、实际修改文件

| 文件 | 操作 | 改动内容 |
|------|------|----------|
| `frontend/src/auth/routeGuard.ts` | 修改 | permissions `some()`→`every()`, 注释 |
| `frontend/src/components/auth/PermissionGuard.vue` | 修改 | `hasAnyPermission`→`hasAllPermissions`, AND语义 |
| `frontend/src/auth/access.ts` | 修改 | 补充语义注释 |
| `frontend/src/views/binterface/BInterfaceRealtimeView.vue` | 修改 | 6 新 alert + meta 面板 + 错误分类 |
| `frontend/src/views/telemetry/RealtimeDataView.vue` | 修改 | 6 alert + 错误分类 + 结构化错误处理 |
| `frontend/dist/*` | 构建刷新 | npm run build 产物 |
