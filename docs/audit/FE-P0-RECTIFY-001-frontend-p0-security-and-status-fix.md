# FE-P0-RECTIFY-001: 前端 P0 安全与状态口径整改 — 审计报告

**日期:** 2026-05-29
**类型:** P0 安全整改
**信息来源:** FE-ARCH-AUDIT-001

---

## 一、修改前论证

FE-ARCH-AUDIT-001 发现前端存在 6 项 P0 风险：
1. raw XML 协议报文无权限控制
2. run-once 功能无权限控制
3. 大部分路由缺少 requiresAuth
4. 实时页硬编码 SignalID 白名单
5. "后端未接入"误报
6. 后端鉴权缺失（前端暂时无法修复）

本次整改只做受控小范围修改，未做大规橫重构。

## 二、修改文件清单

| # | 文件 | 变更类型 | 说明 |
|---|------|----------|------|
| 1 | `types/auth.ts` | 修改 | PlatformRole.code 扩展 3 个新角色 |
| 2 | `stores/user.ts` | 修改 | 新增 isSuperAdmin/isPlatformAdmin/isProtocolDebugger/isElevatedUser |
| 3 | `auth/permissions.ts` | 修改 | 新增 3 个权限点 + PERMISSION_META_LIST |
| 4 | `auth/routeGuard.ts` | 修改 | 新增 meta.roles 检查 |
| 5 | `router/index.ts` | 修改 | 全部路由 requiresAuth + raw XML/run-once permissions+roles |
| 6 | `layouts/BasicLayout.vue` | 修改 | 菜单权限过滤 v-if |
| 7 | `compat/realtimeSignalFilter.ts` | **新增** | 集中封装临时硬编码白名单 |
| 8 | `views/binterface/BInterfaceRealtimeView.vue` | 修改 | 替换硬编码→compat导入 + 状态口径 |
| 9 | `views/binterface/MessageLogView.vue` | 修改 | raw XML查看/下载 + PermissionGuard |
| 10 | `views/binterface/FsuStatusDetailView.vue` | 修改 | GET_FSUINFO run-once PermissionGuard |
| 11 | `views/binterface/BInterfaceSchedulerView.vue` | 修改 | run-once PermissionGuard |
| 12 | `views/binterface/BInterfaceThresholdView.vue` | 修改 | GET/SET_THRESHOLD PermissionGuard |
| 13 | `views/dashboard/DashboardView.vue` | 修改 | 状态口径细分 |
| 14 | `views/binterface/BInterfaceOverviewView.vue` | 修改 | 状态口径细分 |

## 三、权限点清单

### 新增权限点
| 权限码 | 名称 | 风险等级 | 说明 |
|--------|------|----------|------|
| `protocol:raw:view` | 原始XML查看 | high | 查看 SOAP/XML 原始报文 |
| `protocol:raw:download` | 原始XML下载 | high | 下载 SOAP/XML 原始报文 |
| `protocol:runonce:readonly` | Run-once只读 | medium | 只读 FSU 实时采集(仅 GET_*) |

### 新增角色
| 角色码 | 说明 |
|--------|------|
| `super_admin` | 超级管理员，可查看 raw XML |
| `platform_admin` | 平台管理员，可查看 raw XML |
| `protocol_debugger` | 协议调试员，可查看 raw XML + run-once |

## 四、路由守卫说明

### 登录检查
所有布局路由 (20 条) 统一增加 `requiresAuth: true`。未登录重定向到 `/login`。

### 权限检查
- `meta.permissions` 使用 OR 语义：满足任一项即放行
- admin 角色直接放行

### 角色检查 (新增)
- `meta.roles` 使用 OR 语义：满足任一角色即放行
- admin 角色直接放行

### raw XML 路由
- `/b-interface/logs` — requiresAuth + permissions:[protocol:raw:view] + roles:[super_admin,platform_admin,protocol_debugger]
- `/b-interface/calls` — 同上

### run-once 路由
- `/b-interface/fsus/:fsuCode` — 额外加 protocol:runonce:readonly
- `/b-interface/schedulers` — protocol:runonce:readonly
- `/b-interface/thresholds` — 额外加 protocol:runonce:readonly

### 菜单过滤
- 报文日志/调用记录 菜单: `v-if="userStore.isElevatedUser"`
- 系统管理 菜单: `v-if="userStore.isAdmin"`

## 五、实时数据状态口径说明

| 状态 | 触发条件 | 提示信息 |
|------|----------|----------|
| `empty` | HTTP 200 + data为空 | "暂无实时数据" |
| `fsu_ack_empty` | realDeviceAccessed=true + ackReceived=true + emptyData=true | "FSU已ACK但未返回测点值" |
| `fsu_no_ack` | ackReceived=false 或 realDeviceAccessed=false | "FSU通信失败或未访问真实设备" |
| `parse_error` | parseError=true | "协议响应解析失败" |
| `api_error` | 网络不通或API 404/500 | ApiErrorAlert 组件展示 |
| `ok` | 数据正常 | 显示链路状态 + 未映射点位统计 |

## 六、硬编码过滤处理说明

8 个 SignalID 白名单已从 `BInterfaceRealtimeView.vue` 内联代码中提取到 `src/compat/realtimeSignalFilter.ts`：

- 所有导出标记 `@deprecated` + TODO
- 临时白名单仅在此集中封装的模块中存在
- 未扩散到其他页面组件
- 明确标注待后端 `mappingStatus` / `dataSource` / `isDemo` 字段补齐后删除

## 七、验证结果

1. **npm run build**: 通过 (0 errors)
2. **普通用户不能看 raw XML 菜单**: `v-if="userStore.isElevatedUser"` 控制
3. **普通用户不能进 /b-interface/logs**: route guard meta.roles 校验
4. **普通用户不能进 run-once 入口路由**: protocol:runonce:readonly 权限控制
5. **SET 类命令无可点击入口**: SET_THRESHOLD 用 PermissionGuard mode="disable"
6. **实时数据不误报"后端未接入"**: 改用 6 态口径
7. **DI 不硬编码 0/1**: 未修改 DI 相关逻辑

## 八、遗留问题

1. 后端鉴权缺失 (见 BE-AUTH-P0-001)
2. `compat/realtimeSignalFilter.ts` 等待后端字段后删除
3. PermissionGuard 标记的 run-once 功能均为 "规划中"，待实际接入时再完善
4. CallRecordView 和 FtpRecordView 的路由保护待确认后端是否有 raw XML 暴露

## 九、下一步建议

1. 后端执行 BE-AUTH-P0-001 鉴权补齐
2. 后端补齐 realtime API 的 mappingStatus/dataSource/isDemo 字段
3. 前端删除 `compat/realtimeSignalFilter.ts`
4. 前端完善 run-once 功能接入 (需后端权限 API 先就绪)
