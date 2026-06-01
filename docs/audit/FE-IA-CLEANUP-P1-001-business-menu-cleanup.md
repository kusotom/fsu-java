# FE-IA-CLEANUP-P1-001 普通业务菜单与信息架构收敛报告

## 1. 任务背景

近期前端普通侧边栏仍展示资产与点位、设备管理、机柜管理、点位治理、协议诊断和系统审计类入口。这些页面包含 `SPID`、`SignalID`、`GET_LOGININFO`、`missing_signal_mapping`、raw XML、run-once、审计记录等内部字段，不适合作为普通监控用户和三方用户的日常业务导航。

本任务只收敛前端菜单和路由可见性，不删除后端字典、映射、unmapped observation、审计、DataScope、raw XML、run-once 或 SET 阻断能力。

## 2. 产品口径变更

普通业务导航收敛为：

```text
监控中心
- 监控驾驶舱
- 站点实时数据
- 告警中心

站点监控
- 站点列表
- FSU 管理

三方授权
- 用户管理
- 角色管理
- 权限管理
- 站点授权
- FSU 授权

系统设置
- 安全设置
```

其中 `三方授权` 仅对 `super_admin / platform_admin / admin` 等管理角色可见。普通监控用户和三方用户不显示点位治理、协议诊断和系统审计菜单。

## 3. 修改前菜单结构

修改前 `BasicLayout.vue` 侧边栏结构：

```text
监控驾驶舱
站点监控
- 站点监控
- 实时数据
告警中心
资产与点位
- FSU 管理
- 设备管理
- 机柜管理
- 点位字典
- 点位映射
- 未映射点位
三方授权
- 用户管理
- 角色管理
- 权限点
协议诊断
- 协议概览
- raw XML
- 协议矩阵
- 通信记录
- FTP 记录
- 只读 run-once
系统审计
- raw 访问记录
- 通信审计
系统设置
- 安全设置
```

## 4. 修改后菜单结构

修改后 `BasicLayout.vue` 普通侧边栏结构：

```text
监控中心
- 监控驾驶舱
- 站点实时数据
- 告警中心

站点监控
- 站点列表
- FSU 管理

三方授权
- 用户管理
- 角色管理
- 权限管理
- 站点授权
- FSU 授权

系统设置
- 安全设置
```

## 5. 被移除的普通业务菜单

以下入口已从普通侧边栏移除：

- `资产与点位`
- `设备管理`
- `机柜管理`
- `点位字典`
- `点位映射`
- `未映射点位`
- `协议诊断`
- `系统审计`
- `登录审计`
- `操作审计`
- `协议审计`

说明：页面文件和后端能力未删除，仅从普通导航剥离，并对直接 URL 增加角色约束。

## 6. 保留的普通业务菜单

- `监控驾驶舱`
- `站点实时数据`
- `告警中心`
- `站点列表`
- `FSU 管理`
- `用户管理`、`角色管理`、`权限管理`、`站点授权`、`FSU 授权`，仅管理角色可见
- `安全设置`

## 7. 点位治理页面隐藏或迁移策略

`/points`、`/mapping`、`/unmapped`、`/devices`、`/cabinets` 路由保留，但普通菜单不再展示。这些路由新增 `roles: ELEVATED_ROLES`，仅 `super_admin / platform_admin / protocol_debugger / admin` 可通过直接 URL 访问。

后续设备、机柜信息应逐步并入 `FSU 详情页` 的设备列表、机柜信息和点位概览 Tab，不再作为普通业务独立菜单。

## 8. 系统审计页面隐藏或迁移策略

本次删除普通侧边栏中的 `系统审计` 分组，不删除后端审计日志、权限拒绝审计、raw XML 审计、run-once 审计和 SET 阻断审计。

raw XML 和通信记录仍作为内部协议诊断路由保留，并受 `protocol:raw:view` 与 elevated 角色控制。普通监控用户和三方用户不能从菜单看到，也不能通过普通角色直接访问。

## 9. FSU 管理是否仍正常

`FSU 管理` 继续保留，位置从 `资产与点位` 移到 `站点监控` 下。

`/b-interface/fsus` 路由保留，权限调整为当前后端权限码 `fsu:view`。同时在前端用户 store 增加旧权限兼容，`binterface.fsu.read` 仍可兼容识别为 `fsu:view`。

`/b-interface/fsus/:fsuCode` 路由保留为 FSU 详情页，并去掉页面级 `protocol:runonce:readonly` 要求，避免普通 FSU 详情被协议调试权限阻塞。页面内 GET_FSUINFO run-once 区块仍由 `PermissionGuard` 使用 `protocol:runonce:readonly` 控制。

## 10. 设备/机柜信息后续归并策略

本任务不删除 `FsuDeviceManagementView.vue` 和 `CabinetManagementView.vue`。后续建议：

- 设备列表迁入 `FsuStatusDetailView.vue` 的设备列表 Tab。
- 机柜信息迁入 FSU 详情的机柜信息 Tab。
- 点位概览迁入 FSU 详情的点位概览 Tab。
- 独立设备/机柜页面保留为内部管理工具，不进入普通菜单。

## 11. 权限影响分析

本次前端权限变化：

- 新增 `userStore.isTenantAdmin`，用于 `三方授权` 菜单显示。
- `userStore.isAdmin` 兼容 `admin / super_admin / platform_admin`，与后端 admin-like 管理角色口径对齐。
- `routeGuard` 改用 `userStore.hasPermission()`，支持权限别名兼容。
- 新增权限别名：`fsu:view -> binterface.fsu.read`、`realtime:view -> binterface.realtime.read`、`alarm:view -> binterface.alarm.read`、`user:view -> user.read` 等，避免新旧权限码导致误拒绝。
- 三方授权路由增加 `TENANT_ADMIN_ROLES` 约束。
- 协议诊断和点位治理路由增加 `ELEVATED_ROLES` 约束。

前端权限仍只负责展示和路由控制，真实数据隔离和操作拦截仍由后端实现。

## 12. 三方用户可见性检查

普通监控用户和三方用户：

- 不显示 `三方授权`。
- 不显示 `资产与点位`。
- 不显示 `设备管理`、`机柜管理`。
- 不显示 `点位字典`、`点位映射`、`未映射点位`。
- 不显示 `协议诊断`。
- 不显示 `系统审计`。

直接访问内部治理路由时，若角色不属于 `ELEVATED_ROLES` 或 `TENANT_ADMIN_ROLES`，会进入 `/403`。

## 13. raw XML / run-once / SET 安全边界检查

- raw XML 路由仍要求 `protocol:raw:view` 和 elevated 角色。
- run-once 路由仍要求 `protocol:runonce:readonly` 和 elevated 角色。
- FSU 详情页中的 run-once 区块仍由 `PermissionGuard` 控制。
- 本次未新增任何 SET 按钮或 SET 执行入口。
- 后端 SET 安全门未修改。
- 后端 DataScope 未修改。
- 未访问真实 FSU，未启 Scheduler。

## 14. 修改文件清单

前端代码：

- `frontend/src/layouts/BasicLayout.vue`
- `frontend/src/router/index.ts`
- `frontend/src/stores/user.ts`
- `frontend/src/auth/routeGuard.ts`
- `frontend/src/views/system/SiteAuthorizationView.vue`
- `frontend/src/views/system/FsuAuthorizationView.vue`

文档：

- `docs/audit/FE-IA-CLEANUP-P1-001-business-menu-cleanup.md`
- `docs/tasks/FE-IA-CLEANUP-P1-001-follow-up-todo.md`
- `docs/memory/2026-06-01-FE-IA-CLEANUP-P1-001-business-menu-cleanup.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`

## 15. 测试结果

| 命令 | 结果 |
|---|---|
| `mvn -q -DskipTests compile`，在项目根目录执行 | 失败，根目录无 `pom.xml`，属于执行目录问题 |
| `mvn -q -DskipTests compile`，在 `backend/` 执行 | 通过 |
| `mvn test -Dtest='*Security*Test,*DataScope*Test'`，在 `backend/` 执行 | 通过，20 tests |
| `npm run build`，在 `frontend/` 执行 | 通过 |

`npm run build` 保留既有 warning：Rollup pure annotation、CSS `//` 注释、chunk size 警告；未出现本次新增错误。

## 16. P0/P1/P2 问题清单

### P0

未发现新增 P0。

### P1

- `FE-IA-CLEANUP-P1-002`：将设备列表、机柜信息、点位概览正式归并到 FSU 详情页 Tab。
- `FE-AUTH-P1-001`：三方授权的站点授权、FSU 授权页面接入后端租户与范围授权 API。
- `FE-INTERNAL-TOOLS-P1-001`：为点位治理和协议诊断建立独立内部工具入口，且只对 elevated/admin 角色开放。

### P2

- `FE-PERMISSION-COPY-P2-001`：统一前端权限点页面中的旧 `binterface.*.read` 与后端 `*:view` 权限码展示。
- `FE-DASHBOARD-MENU-P2-001`：Dashboard 增加跳转到站点、FSU、告警的快捷入口。

## 17. 后续建议

1. 接入真实三方授权 API 前，站点授权和 FSU 授权页面保持只读空态，不做前端伪数据。
2. 将内部点位治理页面从普通菜单永久剥离，后续只作为内部工具或协议诊断入口。
3. 将系统审计设计为独立内部管理入口，仅 `super_admin / platform_admin` 可见。
4. 持续以后端鉴权、DataScope 和审计作为真实安全边界，前端只做展示控制。

## 18. 最终结论

FE-IA-CLEANUP-P1-001 完成，普通业务前端已剥离资产与点位、设备管理、机柜管理、点位治理和系统审计入口；普通业务导航仅保留监控中心、站点监控、三方授权和系统设置。点位治理与系统审计能力作为内部能力保留，不再暴露给普通监控用户和三方用户。
