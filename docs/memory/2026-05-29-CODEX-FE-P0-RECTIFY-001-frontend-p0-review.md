# 2026-05-29 CODEX-FE-P0-RECTIFY-001 前端 P0 权限与状态口径复审记忆

## 1. 本次目标

复审 `FE-P0-RECTIFY-001` 是否真正修复 FE-ARCH-AUDIT-001 发现的前端 P0 风险：
- raw XML 暴露。
- run-once 缺权限。
- 路由公开。
- 实时页硬编码过滤。
- “后端未接入”误报。
- SET 类命令前端入口风险。

本次只做审计复核和文档输出，未修改业务代码，未访问真实 FSU，未执行 SET，未启 Scheduler。

## 2. 复审结论

`FE-P0-RECTIFY-001` 未完全通过复审。

已通过：
- `npm run build` 通过。
- 布局下业务路由基本补齐 `requiresAuth`。
- raw XML 路由和按钮增加前端权限控制。
- run-once 占位区块增加 `protocol:runonce:readonly`。
- 普通实时数据页没有调用真实 GET_DATA probe。
- 8 个 SignalID 白名单集中到 `compat/realtimeSignalFilter.ts`。
- `PointCard` 仍正确展示 `0.0`，DI 未硬编码 0/1，未知单位显示“单位待确认”。
- 未发现 SET 类可点击执行入口。

未通过：
- 多处“后端/接口未接入”误报仍存在。
- 实时数据状态分支依赖后端 meta，但当前 `/b-interface/realtime-points` 未返回 meta，不能真实区分 ACK 空数据、未访问真实设备、解析失败等状态。
- `routeGuard` 和 `PermissionGuard` 多权限数组使用 OR 语义，不能表达“同时需要”。
- 后端鉴权、数据范围和 raw XML API 保护仍缺失，三方授权生产仍被阻塞。

## 3. 关键发现

P0 遗留：
- `BInterfaceMessageLogController` 仍直接返回 `BInterfaceMessageLogEntity`，包含 raw XML，且无后端权限拦截。
- `FsuStatusDetailView.vue`、`BInterfaceSchedulerView.vue`、`BInterfaceThresholdView.vue`、`api/auth.ts` 仍有已接入端点被标为“接口未接入”的口径问题。
- `BInterfaceRealtimeView.vue` 对 API 404/500 没有明确区分“API 不存在/后端未接入”，仍泛化为 `LOAD_FAILED`。

P1 遗留：
- 多权限数组 OR 语义需拆成 `permissionsAny` / `permissionsAll`。
- `compat/realtimeSignalFilter.ts` 仍含 `fsuId === 2`，只可作为临时兼容。
- 后端 `/realtime-points` 需返回 `realDeviceAccessed / ackReceived / valuesReturned / emptyData / parseError / unmappedCount`。
- `role.read` 与 `role.manage` 权限点口径不一致。
- SET_FTP / SET_LOGININFO 规划卡片建议加 PermissionGuard 或对普通用户隐藏。

## 4. 构建验证

执行：

```bash
npm run build
```

结果：
- `vue-tsc --noEmit` 通过。
- `vite build` 通过，`✓ built in 7.37s`。
- 仅有第三方包 Rollup 注释 warning 和 chunk size warning。

前端无 `test` 脚本，未执行测试。

## 5. 后续建议

1. 立即进入 `BE-AUTH-P0-001`，补后端 token、permission、data scope、raw XML API 权限。
2. 前端补一个 P0 小修：
   - 修正所有已接入端点的“未接入”误报。
   - 404/501 映射为“后端未接入/API 不存在”。
   - 明确“暂无数据/安全禁用/只读占位/真实调用未开启”口径。
3. 暂缓 FE-P1 页面建设，先闭合 P0 安全边界和状态口径。
4. 后端补 realtime status meta 后，再删除 `compat/realtimeSignalFilter.ts`。

## 6. 输出文件

- `docs/audit/CODEX-FE-P0-RECTIFY-001-frontend-p0-review.md`
- `docs/memory/2026-05-29-CODEX-FE-P0-RECTIFY-001-frontend-p0-review.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`
