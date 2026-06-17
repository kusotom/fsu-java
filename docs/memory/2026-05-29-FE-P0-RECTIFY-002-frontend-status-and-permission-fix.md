# FE-P0-RECTIFY-002 前端P0状态口径与权限语义小修

## 1. 任务目标

修复 CODEX-FE-P0-RECTIFY-001 复审指出的前端 P0 遗留问题:
- "后端/接口未接入" 误报
- API 不存在分类不准确
- 实时数据 meta 缺失
- 权限 roles/permissions OR/AND 语义不明确
- dist 构建产物处理

## 2. 架构判断

仅涉及前端展示层 (`frontend/src/`)：
- 权限层: `auth/routeGuard.ts`, `auth/access.ts`, `components/auth/PermissionGuard.vue`
- 视图层: `views/binterface/BInterfaceRealtimeView.vue`, `views/telemetry/RealtimeDataView.vue`
- 不涉及后端、协议层、数据库、B接口主线

## 3. 协议一致性判断

不涉及 B接口协议修改，仅前端 UI 展示优化。

## 4. 修改前论证

- **状态口径**: 当前 BInterfaceRealtimeView 只有 5 种状态，缺少 `api_404`/`network_error`/`http_401`/`http_403`/`http_5xx` 精确分类；RealtimeDataView 完全无状态显示
- **meta**: 当前只内部使用 `parseError`/`realDeviceAccessed`/`ackReceived`/`emptyData`/`unmappedCount`，缺 5 个字段，且无 UI 展示
- **权限语义**: 当前 permissions 使用 `some()` (any-of)，无法表达"同时需要多个权限"；PermissionGuard 的 permissions+roles 组合逻辑不清晰
- **影响面**: 仅前端 5 个源文件 + dist 构建产物，风险极低

## 5. 写入前验证

- 代码审查确认所有 PermissionGuard 调用点: 7 个组件 9 处调用，仅 1 处多权限
- 路由权限审查: 14 个路由有 permissions，2 个多权限路由，2 个 permissions+roles 路由
- admin 用户 `isAdmin` 绕过所有检查，不受 all-of 变更影响
- meta 缺失降级逻辑确认不会导致 `undefined` 错误

## 6. 实际修改文件

| 文件 | 操作 |
|------|------|
| `frontend/src/auth/routeGuard.ts` | 修改 — permissions all-of, 注释 |
| `frontend/src/components/auth/PermissionGuard.vue` | 修改 — permissions all-of, AND语义 |
| `frontend/src/auth/access.ts` | 修改 — 补充语义注释 |
| `frontend/src/views/binterface/BInterfaceRealtimeView.vue` | 修改 — 6新alert+meta面板+错误分类 |
| `frontend/src/views/telemetry/RealtimeDataView.vue` | 修改 — 6alert+错误分类 |
| `frontend/dist/*` | 构建刷新 |
| `docs/audit/FE-P0-RECTIFY-002-frontend-status-and-permission-fix.md` | 新增 |
| `docs/memory/2026-05-29-FE-P0-RECTIFY-002-frontend-status-and-permission-fix.md` | 新增 |
| `docs/memory/README.md` | 更新索引 |
| `docs/memory/WORKING-MEMORY.md` | 更新 |

## 7. 核心改动

1. **状态口径 9 态精确分类**: 404→未接入, network→不可达, 401→未登录, 403→无权限, 5xx→服务异常, 200空→无数据, ACK空→FSU无测点, parseError→解析失败, unmapped→未映射
2. **meta 10 字段补齐**: realDeviceAccessed, ackReceived, emptyData, valuesReturned, unmappedCount, lastCollectTime, lastGetDataTime, parseError, errorCode, errorMessage。缺失时优雅降级"未提供"
3. **权限语义统一**: permissions=all-of, roles=any-of, permissions+roles=AND。isAdmin 绕过所有检查
4. **dist 保留**: 项目规范为提交 dist

## 8. 测试命令和结果

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/frontend
npm run build
```

结果: **通过** (vue-tsc --noEmit + vite build, 0 errors)

前端无 `test` 脚本，未执行自动化测试。

## 9. 风险

- **低风险**: 权限从 any-of 改为 all-of 可能让部分用户失去某些页面访问权。但实际影响仅 2 个路由 (FSU详情/门限) 和 1 个 SET guard，这些页面本就应同时要求两个权限
- **低风险**: admin 用户完全不受影响
- **无风险**: 状态显示和 meta 补齐仅影响 UI，不影响数据流

## 10. 遗留问题

- BE-AUTH-P0-001: 后端鉴权和数据范围保护仍缺失
- 后端 `/b-interface/realtime-points` 需返回 meta 字段前端才能真实驱动状态展示
- 建议进入 AUTH-MATRIX-VERIFY-001 权限矩阵验证

## 11. 下一步建议

1. AUTH-MATRIX-VERIFY-001: 权限矩阵逐路由验证
2. BE-AUTH-P0-001: 后端鉴权闭环
3. 后端 `/b-interface/realtime-points` meta 字段补齐

## 12. git diff 摘要

- 5 个前端源文件修改
- dist 构建产物刷新 (~60 文件)
- 无后端 Java 代码修改
- 无 SQL/DDL 修改

## 13. git status 摘要

- Modified: `frontend/src/auth/routeGuard.ts`, `frontend/src/auth/access.ts`, `frontend/src/components/auth/PermissionGuard.vue`, `frontend/src/views/binterface/BInterfaceRealtimeView.vue`, `frontend/src/views/telemetry/RealtimeDataView.vue`
- Modified: `frontend/dist/*` (构建产物)
- New: `docs/audit/FE-P0-RECTIFY-002-*.md`, `docs/memory/2026-05-29-FE-P0-RECTIFY-002-*.md`
- Modified: `docs/memory/README.md`, `docs/memory/WORKING-MEMORY.md`
