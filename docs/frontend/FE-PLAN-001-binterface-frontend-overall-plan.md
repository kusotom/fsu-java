# FE-PLAN-001：B接口2016 前端适配总体规划

> 日期: 2026-05-21 | 审计类型: 只读规划 | 修改: 0

## 1. 前端技术栈

| 项目 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue | 3.4 |
| 语言 | TypeScript | 5.3 |
| 构建 | Vite | 5.1 |
| UI 库 | Element Plus | 2.5 |
| 路由 | Vue Router | 4.3 (hash) |
| HTTP | Axios | 1.6 |
| 状态管理 | **无** (Pinia/Vuex 均未安装) |
| 图表库 | **无** |
| 权限系统 | **无** |

## 2. 当前项目结构

```
frontend/src/
├── api/           (7 文件: alarm/bInterface/health/request/resource/system/telemetry)
├── components/     (2: PageHeader, SearchPanel)
├── layouts/        (1: BasicLayout)
├── router/         (1: index.ts, 15 routes)
├── types/          (1: index.ts, 完整 Entity 类型)
├── views/
│   ├── alarm/      (1: AlarmCenterView)
│   ├── binterface/ (5: Overview/Status/Log/Command/Ftp/CallRecord)
│   ├── dashboard/  (1: DashboardView)
│   ├── resource/   (4: Site/Cabinet/FsuDevice/Point)
│   ├── system/     (1: UserManagement)
│   └── telemetry/  (2: RealtimeData/HistoryData)
├── App.vue
└── main.ts
```

## 3. 已有页面 (15 routes)

| 页面 | 路由 | 状态 | API 接入 |
|------|------|:--:|:--:|
| 总览 Dashboard | /dashboard | ✅ | ⚠️ 基础 |
| 站点管理 | /sites | ✅ | ✅ |
| 机柜管理 | /cabinets | ✅ | ✅ |
| FSU设备管理 | /devices | ✅ | ✅ |
| 监控点位 | /points | ✅ | ✅ |
| 实时数据 | /telemetry/realtime | ✅ | ✅ |
| 历史数据 | /telemetry/history | ✅ | ✅ |
| 告警中心 | /alarms | ✅ | ✅ |
| B接口总览 | /b-interface | ✅ | ✅ |
| FSU注册状态 | /b-interface/fsus | ✅ | ✅ |
| 报文日志 | /b-interface/logs | ✅ | ✅ |
| 协议命令矩阵 | /b-interface/commands | ✅ | ✅ |
| FSUService调用记录 | /b-interface/calls | ✅ | ✅ |
| FTP记录 | /b-interface/ftp | ✅ | ✅ |
| 用户管理 | /system/users | ✅ | ✅ |

## 4. 缺失页面

| 页面 | 优先级 |
|------|--------|
| 门限管理 (查看/设置规划) | P0 |
| 点位映射 / unmapped 清单 | P0 |
| 调度任务配置页 | P1 |
| 角色管理 | P1 |
| 操作审计日志 | P1 |
| SET 操作规划页 | P1 |
| FTP 图片查看 | P2 |
| FSU 重启规划 | P2 |
| 自动升级规划 | P3 |

## 5. API Client 现状

- Axios instance: `/api` baseURL, 15s timeout
- 响应拦截: `response.data` 解包, 无统一错误处理
- 类型定义: `ApiResponse<T>` 对齐后端 (code, message, data, timestamp)
- **缺口**: 无 error interceptor 统一处理, 无 token 管理, 无 audit_id 展示

## 6. B接口菜单规划

```
📊 总览
📡 设备管理
  ├── FSU 列表
  ├── FSU 详情 (含 GET_FSUINFO CPU/MEM)
  └── 点位管理
📈 数据监控
  ├── 实时数据 (按 DeviceID→SignalID 三层)
  ├── 历史数据
  └── 未映射点位清单
🚨 告警管理
  ├── 当前告警
  ├── 历史告警
  └── 活动告警一致性审计
📏 门限管理
  ├── 门限查看
  └── 门限设置 (规划)
🔧 B接口运维
  ├── 协议总览
  ├── 报文日志
  ├── FSU 注册状态
  ├── 调度任务 (规划)
  └── FTP 记录
🖼 FTP 图片 (规划)
⚙ 系统管理
  ├── 用户管理
  └── 角色管理 (规划)
```

## 7. 权限系统规划

| 角色 | 权限 |
|------|------|
| 只读用户 | binterface.read, telemetry.read, alarm.read |
| 运维用户 | + binterface.dry_run, binterface.mock, threshold.read |
| 管理员 | + set_*.execute, scheduler.manage, user.manage, role.manage |

**本阶段只规划，不实现。**

## 8. 高风险操作策略

SET_POINT/SET_THRESHOLD/SET_FTP/SET_FSUREBOOT: 按钮默认 hidden/disabled, 需后端 confirmation_token + 白名单。

## 9. 通用组件规划

| 组件 | 用途 | 优先级 |
|------|------|--------|
| StatusBadge | FSU 在线/离线/告警状态 | P0 |
| EmptyState | 数据为空/接口未接入 | P0 |
| DataTable | 统一样式表格 | P0 |
| DeviceSignalTree | 三层点位树 | P0 |
| ApiNotImplemented | 接口未接入提示 | P1 |
| RefreshButton | 手动刷新 | P1 |
| ErrorCodeTag | 错误码展示 | P2 |
| AuditIdLink | 审计ID链接 | P2 |
| SensitiveValue | 脱敏展示 | P2 |
| RiskConfirmDialog | 高风险确认 | P2 |

## 10. FE-TODO 执行顺序

| 编号 | 任务 | 优先级 |
|------|------|--------|
| FE-INFRA-001 | 工程基础 (Pinia + ECharts) | P0 |
| FE-INFRA-002 | API Client 增强 (error interceptor) | P0 |
| FE-INFRA-004 | 通用组件库 | P0 |
| FE-TODO-001 | FSU 总览与状态页增强 | P0 |
| FE-TODO-002 | 实时数据 DeviceID→SignalID 三层展示 | P0 |
| FE-TODO-003 | 告警页增强 (serialNo/deviceId/spid) | P0 |
| FE-TODO-004A | 门限只读页 | P0 |
| FE-TODO-007A | FTP/注册参数只读页 | P1 |
| FE-TODO-009A | FTP 图片只读页 | P2 |
| FE-TODO-010 | 调度配置页 | P1 |
| FE-TODO-011 | 协议覆盖矩阵/审计页 | P1 |
| FE-AUTH | 权限系统 | P2 |
| FE-RISK | 高风险操作页 | P2 |

## 11. 安全确认

- 未修改业务代码
- 未接入真实高风险操作
- 未保存敏感字段
- 未访问真实 FSU
