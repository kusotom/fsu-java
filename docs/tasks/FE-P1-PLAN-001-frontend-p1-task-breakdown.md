# FE-P1-PLAN-001: 前端 P1 任务分解

**日期:** 2026-05-29
**来源:** FE-P1-PLAN-001 三方授权与业务页面 P1 规划

---

## P1A: 三方授权基础 (第1批, 优先)

### FE-P1-TENANT-001: 租户管理
- **前端**: 1 新页面 `TenantManagementView.vue`, 1 新路由 `/system/tenants`
- **后端**: 1 新 Entity `TenantEntity`, 1 新 Controller `TenantController`, 1 新 Service
- **权限**: `tenant:view`, `tenant:manage`
- **测试**: TenantControllerTest, TenantManagementView 功能验证
- **依赖**: 无

### FE-P1-SCOPE-001: 站点/FSU授权
- **前端**: 增强 `UserManagementView.vue` (授权范围 tab), 1 新组件 `ScopeTransfer.vue`
- **后端**: 1 新 Controller `ScopeController`, 增强 `TokenStore` (持久化 scope)
- **权限**: `tenant:manage` (复用)
- **测试**: ScopeControllerTest, 前端 scope 分配交互
- **依赖**: TENANT-001 (租户存在才有 scope 分配)

### FE-P1-AUDIT-001: 审计查询
- **前端**: 1 新页面 `AuditLogView.vue`, 1 新路由 `/system/audit`
- **后端**: 1 新 Controller `AuditLogController` (基于已有 `AuditLogEntity`)
- **权限**: `audit:view`
- **测试**: AuditLogControllerTest, 筛选/分页交互
- **依赖**: 无 (AuditLog 已写入数据)

---

## P1B: 业务闭环 (第2批, 次优先)

### FE-P1-MAPPING-001: 点位映射
- **前端**: 增强 `BInterfaceRealtimeView.vue`, 1 新页面 `MappingManagementView.vue`
- **后端**: 1 新 Entity `SignalMappingEntity`, 1 新 Controller `MappingController`
- **权限**: `fsu:view`, `fsu:manage`
- **测试**: MappingControllerTest, 映射创建/删除交互
- **依赖**: SCOPE-001 (scope 决定了哪些 FSU 点位可映射)
- **额外**: 完成后可删除 `compat/realtimeSignalFilter.ts`

### FE-P1-FSU-DETAIL-001: FSU详情增强
- **前端**: 增强 `FsuStatusDetailView.vue`, 2 新组件 `SignalTrendChart.vue` + `AlarmTimeline.vue`
- **后端**: 增强 `/api/b-interface/fsus/{fsuCode}` 返回信号详情
- **权限**: `fsu:view` (已有)
- **测试**: FsuDetailEnhancementTest
- **依赖**: MAPPING-001 (信号数据需要映射才能展示)

### FE-P1-ALARM-001: 告警闭环
- **前端**: 增强 `AlarmCenterView.vue` + `BInterfaceAlarmView.vue`
- **后端**: 1 新 Controller `AlarmOperationController`
- **权限**: `alarm:view`, `alarm:confirm`, `alarm:clear`
- **测试**: AlarmOperationTest, 告警确认/清除交互
- **依赖**: 无 (告警数据已存在)

---

## 实现顺序

```
P0修复完成 → TENANT-001 → SCOPE-001 → AUDIT-001 (P1A 完成)
                                    ↓
            MAPPING-001 → FSU-DETAIL-001 (P1B-1)
            ALARM-001 (P1B-2, 可并行)
```

---

## 各任务文件清单

### TENANT-001
```
新增:
  frontend/src/views/system/TenantManagementView.vue
  backend/src/main/java/.../tenant/entity/TenantEntity.java
  backend/src/main/java/.../tenant/service/TenantService.java
  backend/src/main/java/.../tenant/controller/TenantController.java
  backend/src/main/java/.../tenant/repository/TenantRepository.java
  backend/src/test/java/.../tenant/TenantControllerTest.java
  database/seed/003_seed_tenants.sql
修改:
  frontend/src/router/index.ts (+ /system/tenants)
  frontend/src/layouts/BasicLayout.vue (+ 菜单项)
  frontend/src/api/system.ts (+ tenant APIs)
  frontend/src/auth/permissions.ts (+ tenant:view, tenant:manage)
  backend/database/seed/002_seed_roles_permissions.sql (+ tenant权限)
```

### SCOPE-001
```
新增:
  frontend/src/components/system/ScopeTransfer.vue
  backend/src/main/java/.../system/controller/ScopeController.java
  backend/src/test/java/.../system/ScopeControllerTest.java
修改:
  frontend/src/views/system/UserManagementView.vue (+ 授权 tab)
  backend/src/main/java/.../auth/service/TokenStore.java (+ scope持久化)
  backend/src/main/java/.../security/SecurityContextFilter.java (+ scope注入)
```

### AUDIT-001
```
新增:
  frontend/src/views/system/AuditLogView.vue
  backend/src/main/java/.../security/audit/AuditLogController.java
  backend/src/test/java/.../security/audit/AuditLogControllerTest.java
修改:
  frontend/src/router/index.ts (+ /system/audit)
  frontend/src/layouts/BasicLayout.vue (+ 菜单项)
  frontend/src/api/system.ts (+ audit APIs)
```

### MAPPING-001
```
新增:
  frontend/src/views/resource/MappingManagementView.vue
  backend/src/main/java/.../binterface/entity/SignalMappingEntity.java
  backend/src/main/java/.../binterface/controller/MappingController.java
修改:
  frontend/src/views/binterface/BInterfaceRealtimeView.vue (+ 未映射 tab)
  frontend/src/compat/realtimeSignalFilter.ts (标记可删除)
```

### FSU-DETAIL-001
```
新增:
  frontend/src/components/binterface/SignalTrendChart.vue
  frontend/src/components/binterface/AlarmTimeline.vue
修改:
  frontend/src/views/binterface/FsuStatusDetailView.vue (+ 设备树/趋势/告警)
  backend/.../BInterfaceFrontendReadController.java (+ 增强)
```

### ALARM-001
```
新增:
  backend/src/main/java/.../alarm/controller/AlarmOperationController.java
  backend/src/test/java/.../alarm/AlarmOperationTest.java
修改:
  frontend/src/views/alarm/AlarmCenterView.vue (+ 操作列)
  frontend/src/views/binterface/BInterfaceAlarmView.vue (+ 操作列)
  frontend/src/auth/permissions.ts (+ alarm:confirm, alarm:clear)
```
