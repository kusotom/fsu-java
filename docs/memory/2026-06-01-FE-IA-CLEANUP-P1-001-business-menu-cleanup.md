# 2026-06-01 FE-IA-CLEANUP-P1-001 普通业务菜单收敛

## 任务目标

根据最新产品口径收敛前端菜单与信息架构，让普通业务导航只保留监控中心、站点监控、三方授权和系统设置。点位治理、协议诊断、系统审计不再暴露给普通监控用户和三方用户。

## 审计结论摘要

FE-IA-CLEANUP-P1-001 已完成，未发现新增 P0。普通侧边栏已剥离资产与点位、设备管理、机柜管理、点位字典、点位映射、未映射点位、协议诊断和系统审计入口。相关页面和后端能力未删除，作为内部能力通过角色约束保留。

## 修改内容

- `BasicLayout.vue`：普通菜单调整为监控中心、站点监控、三方授权、系统设置。
- `router/index.ts`：点位治理、设备/机柜、协议诊断路由保留但增加 elevated/admin 角色限制；三方授权路由增加管理角色限制。
- `user.ts`：新增 `isTenantAdmin`，并让 `admin / super_admin / platform_admin` 对齐管理角色口径；增加新旧权限码别名兼容。
- `routeGuard.ts`：权限判断改用 `userStore.hasPermission()`，兼容 `fsu:view` 与旧 `binterface.fsu.read` 等权限码。
- 新增 `SiteAuthorizationView.vue`、`FsuAuthorizationView.vue`，补齐三方授权菜单结构，当前为只读空态。

## 权限与安全边界

- 普通用户和三方用户不可见点位治理、协议诊断、系统审计菜单。
- 直接访问内部治理路由时，非 elevated/admin 角色进入 `/403`。
- raw XML 仍受 `protocol:raw:view` 与 elevated 角色控制。
- run-once 仍受 `protocol:runonce:readonly` 与 elevated 角色控制。
- FSU 详情页不再因页面级 run-once 权限阻塞普通查看，但 run-once 区块仍受 `PermissionGuard` 控制。
- 未修改后端审计、DataScope、SET 安全门。
- 未访问真实 FSU，未启 Scheduler。

## 验证结果

- `mvn -q -DskipTests compile` 在项目根目录执行失败，原因是根目录无 `pom.xml`。
- `cd backend && mvn -q -DskipTests compile`：通过。
- `cd backend && mvn test -Dtest='*Security*Test,*DataScope*Test'`：通过，20 tests。
- `cd frontend && npm run build`：通过，保留既有 Rollup/CSS/chunk warning。

## 后续优先级

1. `FE-IA-CLEANUP-P1-002`：将设备、机柜、点位概览归并到 FSU 详情页。
2. `FE-AUTH-P1-001`：站点授权、FSU 授权页面接入真实后端 API。
3. `FE-INTERNAL-TOOLS-P1-001`：规划内部工具入口，仅 elevated/admin 可见。

## 输出

- [审计报告](../audit/FE-IA-CLEANUP-P1-001-business-menu-cleanup.md)
- [后续 TODO](../tasks/FE-IA-CLEANUP-P1-001-follow-up-todo.md)

## 最终结论

FE-IA-CLEANUP-P1-001 完成，普通业务前端已剥离资产与点位、设备管理、机柜管理、点位治理和系统审计入口；普通业务导航仅保留监控中心、站点监控、三方授权和系统设置。点位治理与系统审计能力作为内部能力保留，不再暴露给普通监控用户和三方用户。
