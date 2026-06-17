# BE-AUTH-P0-001: 后端鉴权与数据范围补齐

**日期:** 2026-05-29
**来源:** FE-P0-RECTIFY-001 (前端 P0 安全整改)
**状态:** 最小闭环已完成 (12新文件 + 12修改 + 9测试), 待 P1 完整化
**优先级:** P0 (最小闭环完成) / P1 (后续完整化)

---

## 一、背景

FE-ARCH-AUDIT-001 / FE-P0-RECTIFY-001 发现后端鉴权层面存在以下缺失，前端无法独立解决。本次前端已做路由守卫 + 菜单过滤 + 组件权限控制，但安全闭环需要后端配合。

## 二、后端必须补齐

### 1. 登录鉴权
- [ ] JWT 签发与验证
- [ ] Token 刷新机制
- [ ] 会话管理 (登出、过期、并发控制)
- [ ] 当前 `/auth/sms/login` 返回 mock 数据，需接入真实 SMS 或密码登录

### 2. 权限点校验
- [ ] 每个 API 端点检查 `@PreAuthorize` 或拦截器
- [ ] 权限点需与前端对齐：
  - `protocol:raw:view` — 查看原始 XML 报文
  - `protocol:raw:download` — 下载原始 XML 报文
  - `protocol:runonce:readonly` — 只读 run-once
  - 以及现有的 `binterface.*.read` 系列
- [ ] 角色-权限映射：
  - SUPER_ADMIN / PLATFORM_ADMIN / PROTOCOL_DEBUGGER → raw XML + run-once
  - ADMIN → 系统管理 + 全部 B接口查看
  - OPERATOR → B接口查看 (不含 raw XML)
  - VIEWER / READ_ONLY → 只读查看

### 3. raw XML API 权限
- [ ] `/api/b-interface/message-logs/*` — protocol:raw:view
- [ ] `/api/b-interface/call-records/*` — protocol:raw:view (如含 raw body)
- [ ] 报文下载接口 — protocol:raw:download

### 4. run-once API 权限
- [ ] `/api/b-interface/2016/read-only/*` — protocol:runonce:readonly
- [ ] `/api/b-interface/2016/heartbeat/run-once` — protocol:runonce:readonly
- [ ] 仅允许只读命令: GET_FSUINFO, GET_DATA, GET_LOGININFO, GET_FTP
- [ ] SET_POINT, SET_THRESHOLD, SET_FTP, SET_LOGININFO, SET_FSUREBOOT 需额外高风险权限

### 5. 数据隔离
- [ ] **tenantId**: 多租户数据隔离
- [ ] **stationScope**: 用户所属站点范围
- [ ] **fsuScope**: 用户可访问 FSU 范围
- [ ] 站点/FSU/实时数据/告警数据均需按 scope 过滤

### 6. 操作审计
- [ ] run-once 操作记录审计 (操作人、时间、命令、FSU、结果)
- [ ] raw XML 查看/下载记录
- [ ] SET 类命令操作审计
- [ ] 审计日志持久化与查询

## 三、前端已完成的配合工作

- [x] 路由守卫: requiresAuth + permissions + roles 三层校验
- [x] 菜单过滤: raw XML 仅 elevated user 可见，系统管理仅 admin 可见
- [x] 组件权限: PermissionGuard 可对接后端权限码
- [x] Token 传递: request.ts 拦截器自动带 Bearer token
- [x] 401/403 处理: 自动跳转登录页或 403 页面

## 四、不在此次范围

- 大规模后端重构
- Scheduler 启用
- SET 类命令实际执行
- 真实 FSU 连接逻辑修改
