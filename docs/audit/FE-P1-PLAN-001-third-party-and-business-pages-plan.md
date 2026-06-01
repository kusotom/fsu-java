# FE-P1-PLAN-001: 三方授权与业务页面 P1 规划

**日期:** 2026-05-29
**状态:** P1 规划 (不直接改代码)
**前置:** AUTH-MATRIX-VERIFY-002 (权限矩阵复验已通过)
**安全边界:** 不访问真实 FSU，不执行 SET，不启 Scheduler，不改 B接口2016 主线

---

## 一、当前状态基线

### 前端 (30 views, 28 routes)
- 总览/dashboard、资源管理(sites/cabinets/devices/points)、遥测(realtime/history)、告警中心
- B接口 14 页 (overview/fsus/detail/realtime/alarms/thresholds/ftp/schedulers/protocol-audit/logs/commands/calls/ftp)
- 系统管理 3 页 (users/roles/permissions)
- 认证 (login/forgot-password/forbidden/profile-security)

### 后端 (25 controllers)
- 全部 CRUD 资源控制器已存在
- 全部 B接口只读控制器已存在
- 权限检查: 21 个已注解 @RequirePermission
- DataScopeService 已接入 6 个 Service

### 缺失的核心 P1 能力
1. **租户/三方单位管理**: 无 Tenant 实体、无管理页面
2. **站点/FSU 授权**: scope 仅在 TokenStore 内存中，无可视化管理
3. **点位映射**: mappingStatus 仅在 DTO 层，无映射配置页面
4. **告警闭环**: 无告警确认/清除 UI 和工作流
5. **FSU 详情增强**: 当前仅基础信息，无设备树/信号列表/历史趋势
6. **审计查询**: AuditLogEntity 已存在但无查询 API/UI

---

## 二、P1 页面规划

### 优先级排序 (先做 vs 后做)

**第一批 (P1A): 三方授权基础 — 优先**

| 顺序 | 任务 | 名称 | 原因 |
|------|------|------|------|
| 1 | FE-P1-TENANT-001 | 三方单位/租户管理 | 租户是一切数据隔离的基础 |
| 2 | FE-P1-SCOPE-001 | 站点授权/FSU授权 | scope 是数据范围的核心配置 |
| 3 | FE-P1-AUDIT-001 | 操作审计查询增强 | 审计是安全闭环的必要证明 |

**第二批 (P1B): 业务闭环 — 次优先**

| 顺序 | 任务 | 名称 | 原因 |
|------|------|------|------|
| 4 | FE-P1-MAPPING-001 | 点位映射/未映射点位 | 数据质量的基础设施 |
| 5 | FE-P1-FSU-DETAIL-001 | FSU 详情增强 | 运维排障的直接入口 |
| 6 | FE-P1-ALARM-001 | 告警确认/处理闭环 | 告警是动环核心业务价值 |

---

## 三、逐任务详细规划

### FE-P1-TENANT-001: 三方单位/租户管理

**目标:** 新增租户管理页面，支持创建、查看、编辑、停用三方单位。

**前端页面:**
- 新建 `views/system/TenantManagementView.vue`
- 路由: `/system/tenants` (requiresAuth: true, permissions: ['tenant:view'])
- 菜单: 系统管理 → 租户管理 (v-if="userStore.isAdmin")
- 功能: 租户列表表格 + 创建/编辑对话框
- 字段: tenantId, tenantName, contactPerson, contactPhone, status, stationScope, createdAt

**后端 API (需新增):**
- `GET /api/system/tenants` — 列表 (tenant:view)
- `GET /api/system/tenants/{id}` — 详情
- `POST /api/system/tenants` — 创建 (tenant:manage)
- `PUT /api/system/tenants/{id}` — 更新
- `DELETE /api/system/tenants/{id}` — 停用

**权限点 (需新增):**
- `tenant:view` — 查看租户
- `tenant:manage` — 管理租户 (创建/更新/停用)

**数据隔离:**
- SUPER_ADMIN/PLATFORM_ADMIN 可见全部租户
- 三方管理员只能看本租户

**验收标准:**
1. SUPER_ADMIN 可创建租户、分配 stationScope/fsuScope
2. 停用租户后该租户所有用户无法登录
3. 三方管理员登录后只能看到本租户数据
4. npm run build 通过

---

### FE-P1-SCOPE-001: 站点授权/FSU 授权页面

**目标:** 为三方用户配置可访问的站点和 FSU 范围。

**前端页面:**
- 增强 `views/system/UserManagementView.vue` — 增加"授权范围"Tab 或弹窗
- 新建 `views/system/ScopeAssignmentView.vue` — 独立授权管理页
- 路由: `/system/scope` (requiresAuth, permissions: ['tenant:manage'])
- 功能: 选择用户 → 勾选站点/FSU → 保存
- 使用 el-transfer (穿梭框) 组件做站点/FSU 左右分配

**后端 API (需新增):**
- `GET /api/system/users/{id}/scope` — 查询用户当前授权范围
- `PUT /api/system/users/{id}/scope` — 更新用户授权范围
  - Body: `{ stationIds: [...], fsuCodes: [...] }`
- `GET /api/system/tenants/{id}/scope-defaults` — 租户默认授权范围
- `PUT /api/system/tenants/{id}/scope-defaults` — 更新租户默认授权范围

**权限点:**
- `tenant:manage` (复用)

**数据隔离:**
- 只有 SUPER_ADMIN/PLATFORM_ADMIN 可以修改授权范围
- 三方管理员不可越级修改授权

**验收标准:**
1. 可为用户分配 stationScope/fsuScope
2. 修改后登录生效 (token 中携带 scope)
3. 无 scope 用户登录后各列表返回空
4. 穿梭框支持站点和 FSU 双向选择

---

### FE-P1-MAPPING-001: 点位映射/未映射点位

**目标:** 展示未映射点位列表，支持 DeviceID → SignalID → monitoring_point 映射。

**前端页面:**
- 增强 `views/binterface/BInterfaceRealtimeView.vue` — 未映射 tab
- 新建 `views/resource/MappingManagementView.vue` — 映射管理
- 路由: `/points/mapping` (requiresAuth, permissions: ['fsu:view'])
- 功能:
  - 未映射点位列表 (deviceId + signalId + rawValue)
  - 查看已有映射关系
  - 创建新映射 (选 monitoring_point + 填 deviceId/signalId)

**后端 API (需新增/增强):**
- `GET /api/b-interface/unmapped-signals` — 增强，返回空值设备树
- `GET /api/monitoring-points?mappingStatus=unmapped` — 已有基础
- `POST /api/mappings` — 创建映射关系
- `DELETE /api/mappings/{id}` — 删除映射
- MappingEntity (新): id, deviceId, signalId, spid, pointId, fsuCode, source

**权限点:**
- `fsu:view` (查看), `fsu:manage` (编辑映射)

**数据隔离:**
- 三方用户只能映射本租户授权 FSU 的点位

**验收标准:**
1. 未映射点位列表正确展示 deviceId + signalId
2. 可创建映射关联
3. 映射后实时数据页正确展示点位名称/单位
4. compat/realtimeSignalFilter.ts 可删除 (临时白名单移除)

---

### FE-P1-ALARM-001: 告警确认/处理闭环

**目标:** 实现告警从发生→确认→清除的完整闭环。

**前端页面:**
- 增强 `views/alarm/AlarmCenterView.vue` — 增加操作列
- 增强 `views/binterface/BInterfaceAlarmView.vue` — 同样加操作列
- 功能:
  - 告警列表: 级别/状态/时间/来源/FSUSU
  - 操作: 确认告警 (填写备注) / 查看详情
  - 告警详情弹窗: 告警值/阈值/点位信息/历史趋势
  - 状态流转: ACTIVE → CONFIRMED → CLEARED

**后端 API (需新增):**
- `POST /api/alarms/{id}/confirm` — 确认告警 (body: confirmNote)
- `POST /api/alarms/{id}/clear` — 清除告警
- `GET /api/alarms?status=ACTIVE` — 活跃告警 (已有基础)

**权限点:**
- `alarm:view` (查看), `alarm:confirm` (确认), `alarm:clear` (清除)

**数据隔离:**
- 三方用户只能操作本租户授权 FSU 的告警

**验收标准:**
1. 告警确认后状态变为 CONFIRMED, 记录确认人+时间
2. 告警清除后状态变为 CLEARED
3. 操作记录写入 audit log
4. 不访问真实 FSU (操作的是数据库 alarm_record)

---

### FE-P1-FSU-DETAIL-001: FSU 详情页增强

**目标:** FSU 详情页增加设备树、信号列表、历史趋势。

**前端页面:**
- 增强 `views/binterface/FsuStatusDetailView.vue`
- 新增子组件:
  - `DeviceSignalTree.vue` (已有, 增强) — FSU→设备→信号三级树
  - `SignalTrendChart.vue` (新) — 信号历史趋势 ECharts 折线图
  - `AlarmTimeline.vue` (新) — 该 FSU 最近告警时间线
- 功能:
  - 基础信息 (已有: CPU/MEM/在线状态/心跳)
  - 设备树 (已有 DeviceSignalTree, 需接入真实数据)
  - 信号列表 + 实时值
  - 信号历史趋势图
  - 最近告警时间线

**后端 API (需增强):**
- `GET /api/b-interface/fsus/{fsuCode}/devices` — 已有, 需增强返回信号详情
- `GET /api/b-interface/fsus/{fsuCode}/signals` — 新增
- `GET /api/telemetry/history/query?pointId=&start=&end=` — 已有

**权限点:**
- `fsu:view` (已有)

**数据隔离:**
- 三方用户只能查看授权 FSU 的详情

**验收标准:**
1. 设备树展开后显示信号列表+实时值
2. 点击信号可看 24h 历史趋势图
3. 告警时间线正确展示最近 20 条
4. 0.0 数值正常显示 (PointCard 已有此能力)

---

### FE-P1-AUDIT-001: 操作审计查询页增强

**目标:** 提供可查询、可筛选、可分页的操作审计日志页面。

**前端页面:**
- 新建 `views/system/AuditLogView.vue`
- 路由: `/system/audit` (requiresAuth, permissions: ['audit:view'])
- 菜单: 系统管理 → 操作审计 (v-if="userStore.hasPermission('audit:view')")
- 功能:
  - 审计日志表格 (分页)
  - 筛选: 按 action/username/fsuCode/时间范围/是否拒绝
  - 详情弹窗: 完整审计字段
  - 导出 CSV (P2)

**后端 API (需新增):**
- `GET /api/audit-logs` — 分页查询 (audit:view)
  - 参数: action, username, fsuCode, allowed, startTime, endTime, page, size
- `GET /api/audit-logs/{id}` — 详情

**权限点:**
- `audit:view` (后端已有定义, 需前端对齐)

**数据隔离:**
- SUPER_ADMIN 可见全部审计
- PLATFORM_ADMIN 可见本平台审计
- 三方管理员只能看本租户审计

**验收标准:**
1. 可按 action (RAW_XML_VIEW/DOWNLOAD, RUN_ONCE, PERMISSION_DENIED, SET_COMMAND_BLOCKED) 筛选
2. 可按时间范围筛选
3. 拒绝事件 allowed=false 红色标记
4. 审计详情弹窗展示全部字段

---

## 四、后端 API 补充清单

### 需新增的 Entity
| Entity | 表名 | 说明 |
|--------|------|------|
| TenantEntity | tenant | 租户/三方单位 |
| TenantUserEntity | tenant_user | 租户-用户关联 |
| SignalMappingEntity | signal_mapping | DeviceID-SignalID-pointId 映射 |
| (AuditLogEntity 已存在) | audit_log | 审计日志 |

### 需新增的 Controller/Endpoint
| Controller | 端点 | 说明 |
|-----------|------|------|
| TenantController | CRUD /api/system/tenants | 租户管理 |
| ScopeController | GET/PUT /api/system/users/{id}/scope | 授权范围 |
| MappingController | POST/DELETE /api/mappings | 点位映射 |
| AlarmOperationController | POST /api/alarms/{id}/confirm | 告警确认 |
| AlarmOperationController | POST /api/alarms/{id}/clear | 告警清除 |
| AuditLogController | GET /api/audit-logs | 审计查询 |

### 需增强的端点
| 现有端点 | 增强内容 |
|----------|---------|
| `/api/b-interface/fsus/{fsuCode}` | 增加信号列表+实时值 |
| `/api/b-interface/unmapped-signals` | 返回空值设备树 |
| `/api/b-interface/realtime-points` | 返回 meta 字段 |

---

## 五、权限点补充

### 新增权限点
| 权限码 | 名称 | 类别 |
|--------|------|------|
| `tenant:view` | 租户查看 | 系统管理 |
| `tenant:manage` | 租户管理 | 系统管理 |
| `fsu:manage` | FSU管理 | 基础管理 |
| `alarm:confirm` | 告警确认 | 告警管理 |
| `alarm:clear` | 告警清除 | 告警管理 |
| `audit:view` | 审计查看 | 系统管理 (已有, 前端对齐) |

### 角色权限分配
| 权限 | SUPER_ADMIN | PLATFORM_ADMIN | ADMIN | OPERATOR |
|------|-------------|---------------|-------|----------|
| tenant:view | [x] | [x] | — | — |
| tenant:manage | [x] | [x] | — | — |
| fsu:manage | [x] | [x] | — | — |
| alarm:confirm | [x] | [x] | [x] | [x] |
| alarm:clear | [x] | [x] | [x] | — |
| audit:view | [x] | [x] | — | — |

---

## 六、数据隔离矩阵

| 页面 | 隔离维度 | 过滤方式 |
|------|---------|---------|
| 租户管理 | tenantId | SUPER_ADMIN 全量; 三方管理员本租户 |
| 站点授权 | tenantId + stationScope | DataScopeService |
| 点位映射 | fsuScope | DataScopeService |
| 告警闭环 | fsuScope | DataScopeService |
| FSU详情 | fsuScope | DataScopeService |
| 操作审计 | tenantId/fsuScope | AuditLog Entity scope 字段 |

---

## 七、安全边界确认

| 边界 | P1 页面是否触碰 |
|------|----------------|
| 真实 FSU | **否** — 所有页面读取数据库, run-once 页面已有权限保护 |
| SET_POINT | **否** |
| SET_THRESHOLD | **否** |
| SET_FTP | **否** |
| SET_LOGININFO | **否** |
| SET_FSUREBOOT | **否** |
| Scheduler | **否** |
| B接口2016 主线 | **否** |
| B接口2024 | **否** |

---

## 八、预期工作量

| 任务 | 前端工作量 | 后端工作量 | 测试工作量 |
|------|-----------|-----------|-----------|
| TENANT-001 | 1 新页面 | 1 Entity + 1 Controller + 1 Service | TenantControllerTest |
| SCOPE-001 | 1 增强页 + 1 新组件 | 1 Controller + 增强 TokenStore | ScopeControllerTest |
| MAPPING-001 | 1 增强页 + 1 新页 | 1 Entity + 1 Controller | MappingControllerTest |
| ALARM-001 | 2 增强页 | 1 Controller + 增强 Service | AlarmOperationTest |
| FSU-DETAIL-001 | 1 增强页 + 2 新组件 | 增强已有 API | FsuDetailEnhancementTest |
| AUDIT-001 | 1 新页面 | 1 Controller | AuditLogControllerTest |

---

## 九、下一步

1. P0 修复完成后启动 P1A (TENANT-001 → SCOPE-001 → AUDIT-001)
2. P1A 完成后启动 P1B (MAPPING-001 → FSU-DETAIL-001 → ALARM-001)
3. 每完成一个任务即更新 WORKING-MEMORY 和 README
