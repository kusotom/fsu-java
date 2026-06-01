# FE-P1-LAYOUT-001: 前端菜单与路由重组

**日期:** 2026-05-29
**状态:** 已完成

---

## 一、变更摘要

将前端从"B接口管理后台"重组为"动力环境监控平台"6 模块结构。

## 二、修改文件 (2)

| 文件 | 变更 |
|------|------|
| `layouts/BasicLayout.vue` | 侧边栏重写 — 6 模块菜单 + 新 token 配色 |
| `router/index.ts` | 路由重组 — 模块分组 + 旧路由保留 |

## 三、新菜单结构

```
监控驾驶舱         /dashboard
站点监控           /sites
实时数据           /telemetry/realtime
告警中心           /alarms
━━━━━━━━━━━━━━━━━━━━━━
资产与点位 ▼
  FSU 管理         /b-interface/fsus
  设备管理         /devices
  机柜管理         /cabinets
  点位字典         /points
━━━━━━━━━━━━━━━━━━━━━━
三方授权 ▼          (v-if=isAdmin)
  用户管理         /system/users
  角色管理         /system/roles
  权限点           /system/permissions
━━━━━━━━━━━━━━━━━━━━━━
协议诊断 ▼          (v-if=isElevatedUser)
  协议概览         /b-interface
  raw XML          /b-interface/logs
  协议矩阵         /b-interface/commands
  通信记录         /b-interface/calls
  FTP 记录         /b-interface/ftp
  只读 run-once    /b-interface/schedulers
━━━━━━━━━━━━━━━━━━━━━━
系统审计 ▼          (v-if=isAdmin)
  raw 访问记录     (placeholder)
  通信审计         (placeholder)
━━━━━━━━━━━━━━━━━━━━━━
安全设置           /profile/security
```

## 四、旧路由保留

所有旧路径保持可用，无破坏性变更。仅新增一个 redirect:
- `/b-interface/overview` → `/b-interface`

## 五、权限验证

| 检查项 | 结果 |
|--------|------|
| 三方用户不可见协议诊断 | [x] — `v-if="userStore.isElevatedUser"` |
| elevatedUser 可见协议诊断 | [x] |
| raw XML 路由权限未放松 | [x] — permissions + roles 仍存在 |
| run-once 路由权限未放松 | [x] |
| routeGuard 未破坏 | [x] — 未修改 |
| PermissionGuard 未破坏 | [x] — 未修改 |
| 用户/角色/权限入口保留 | [x] — 移至三方授权模块 |
| npm run build | [x] — 通过 |

## 六、结论

菜单重组完成。协议诊断下沉到独立模块(仅 elevatedUser 可见)。三方授权成为独立模块(仅 admin 可见)。B接口页面从主入口降级为诊断辅助。
