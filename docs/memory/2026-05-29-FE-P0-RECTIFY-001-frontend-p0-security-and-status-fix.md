# FE-P0-RECTIFY-001: 前端 P0 安全与状态口径整改

## 任务目标
对前端执行 P0 安全整改：raw XML 权限加固、run-once 权限加固、路由公开问题修复、实时页硬编码过滤整改、"后端未接入"误报修正。

## 架构判断
纯前端安全整改，未涉及真实 FSU 调用、SET 命令、Scheduler。路由守卫 + 菜单过滤 + 组件 PermissionGuard 三层防护。

## 协议一致性判断
不影响 B接口 2016 协议层。状态口径定义基于 B接口 ACK 流程 (ackReceived / realDeviceAccessed / parseError)。

## 修改前论证
FE-ARCH-AUDIT-001 发现 6 项 P0 风险。整改遵循最小变更原则，不重构页面。

## 实际修改文件
1. `types/auth.ts` — PlatformRole.code 扩展
2. `stores/user.ts` — 新增 4 个 computed
3. `auth/permissions.ts` — 新增 3 个权限点
4. `auth/routeGuard.ts` — 新增 roles 检查
5. `router/index.ts` — 20 条路由加 requiresAuth + 6 条加 permissions/roles
6. `layouts/BasicLayout.vue` — 菜单 v-if 权限过滤
7. `compat/realtimeSignalFilter.ts` — 新文件，集中封装临时白名单
8. `views/binterface/BInterfaceRealtimeView.vue` — 替换硬编码 + 状态口径
9. `views/binterface/MessageLogView.vue` — raw XML 查看/下载 + PermissionGuard
10. `views/binterface/FsuStatusDetailView.vue` — PermissionGuard
11. `views/binterface/BInterfaceSchedulerView.vue` — PermissionGuard
12. `views/binterface/BInterfaceThresholdView.vue` — PermissionGuard
13. `views/dashboard/DashboardView.vue` — 状态口径
14. `views/binterface/BInterfaceOverviewView.vue` — 状态口径

## 核心改动
- 路由守卫: requiresAuth + permissions + roles 三层检查
- 菜单过滤: raw XML = isElevatedUser, 系统管理 = isAdmin
- 状态口径: connected / not_connected / partial 三元 + 6 种实时数据状态
- 硬编码白名单: 集中封装到 compat/，全员 @deprecated + TODO

## 测试命令和结果
- `npm run build`: 通过 (0 errors)

## 风险
- 路由 requiresAuth 要求所有页面登录后才能访问 (之前 /dashboard 等可匿名)
- `isElevatedUser` 依赖后端返回新角色码；如后端尚未支持，这些用户退化为普通用户
- compat/ 文件为临时方案，需后续清理

## 遗留问题
- 后端鉴权缺失 (BE-AUTH-P0-001)
- compat/realtimeSignalFilter.ts 待后端字段后删除
- run-once 功能均为"规划中"占位

## 下一步建议
1. 后端执行 BE-AUTH-P0-001
2. 后端补齐 realtime API 字段
3. 前端删除 compat 层
