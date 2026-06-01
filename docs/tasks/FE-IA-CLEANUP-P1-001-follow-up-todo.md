# FE-IA-CLEANUP-P1-001 后续 TODO

## P0

无新增 P0。

## P1

### FE-IA-CLEANUP-P1-002 FSU 详情页承载设备/机柜/点位信息

- 问题描述：设备管理、机柜管理已从普通菜单剥离，但设备/机柜信息仍需要在 FSU 详情中形成业务查看闭环。
- 影响范围：FSU 详情页、设备列表、机柜信息、点位概览。
- 建议修改文件：`frontend/src/views/binterface/FsuStatusDetailView.vue`、`frontend/src/api/bInterface.ts`、必要 DTO/normalizer。
- 验收标准：FSU 详情页具备基础信息、运行状态、设备列表、机柜信息、点位概览、通信记录 Tab；普通用户无需进入独立设备/机柜菜单。
- 是否涉及后端：可能。
- 是否涉及权限：是，保留 `fsu:view` 和 DataScope。
- 是否涉及协议安全边界：否。

### FE-AUTH-P1-001 三方授权页面接入后端 API

- 问题描述：站点授权、FSU 授权页面当前为 IA 占位，尚未接入真实租户和范围授权接口。
- 影响范围：三方授权模块。
- 建议修改文件：`frontend/src/views/system/SiteAuthorizationView.vue`、`frontend/src/views/system/FsuAuthorizationView.vue`、`frontend/src/api/system.ts`。
- 验收标准：页面展示 tenant、stationScope、fsuScope，支持按后端权限返回的范围只读查看；写操作需另行设计权限和审计。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

### FE-INTERNAL-TOOLS-P1-001 内部工具入口规划

- 问题描述：点位治理、协议诊断、系统审计已从普通菜单移除，但内部管理员仍需要受控入口。
- 影响范围：点位字典、点位映射、未映射点位、协议诊断、系统审计。
- 建议修改文件：前端内部工具路由和独立 Layout，具体待规划。
- 验收标准：内部工具入口只对 elevated/admin 角色可见；三方用户和普通监控用户不可见；raw XML、run-once、审计仍有后端鉴权。
- 是否涉及后端：可能。
- 是否涉及权限：是。
- 是否涉及协议安全边界：是。

## P2

### FE-PERMISSION-COPY-P2-001 权限码展示文案统一

- 问题描述：部分权限管理页面仍展示旧 `binterface.*.read` 权限码，与后端 `fsu:view/realtime:view/alarm:view` 口径不完全一致。
- 影响范围：权限管理页、用户管理页角色权限矩阵。
- 建议修改文件：`frontend/src/auth/permissions.ts`、`frontend/src/views/system/PermissionManagementView.vue`、`frontend/src/views/system/UserManagementView.vue`。
- 验收标准：展示层以当前后端权限码为主，旧权限码仅标注为兼容别名。
- 是否涉及后端：否。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

### FE-DASHBOARD-MENU-P2-001 监控中心快捷跳转优化

- 问题描述：菜单收敛后，Dashboard 可增加站点、FSU、告警的业务快捷入口，减少跨菜单跳转。
- 影响范围：监控驾驶舱。
- 建议修改文件：`frontend/src/views/dashboard/DashboardView.vue`。
- 验收标准：快捷入口只指向普通业务页面，不暴露协议诊断、点位治理或审计入口。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

## P3

### FE-IA-CLEANUP-P3-001 菜单可用性回归截图

- 问题描述：本次已通过 build 和权限测试，但未用浏览器截图做多角色视觉回归。
- 影响范围：前端侧边栏。
- 建议修改文件：无。
- 验收标准：使用 super_admin、operator/viewer、三方用户样例分别截图确认菜单差异。
- 是否涉及后端：否。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。
