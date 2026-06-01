# AUTH-MATRIX-VERIFY-001 前后端权限矩阵与三方授权安全闭环验证

## 1. 任务目标

验证 FE-P0-RECTIFY-002 + BE-AUTH-P0-001 之后的权限矩阵是否满足三方授权最小安全闭环。

## 2. 架构判断

涉及前后端三层安全体系:
- 前端: routeGuard (路由) + PermissionGuard (组件) + 菜单过滤 (UI)
- 后端: SecurityContextFilter (Token提取) + AuthInterceptor (权限检查) + DataScopeService (数据范围) + AuditLogService (审计)
- 协议层: BInterface2016ReadOnlyRunOnceController (只读 run-once)

## 3. 协议一致性判断

不涉及 B接口协议修改。验证仅读取代码和执行测试，未修改业务代码。

## 4. 修改前论证

纯验证任务，无代码修改。仅读取前/后端源文件、执行测试、生成审计报告。

## 5. 写入前验证

- 阅读 20+ 个源文件: 所有 Controller、AuthInterceptor、SecurityContextFilter、DataScopeService、AuditLogService、TokenStore、AuthService、种子数据
- 执行 55 个测试: SecurityInfrastructureTest(9) + SetCommandSafetyTest(7) + ReadOnlyIntegrationSafetyTest(23) + RunOnceProbeIntegrationTest(16)
- npm run build 通过

## 6. 实际修改文件

无业务代码修改。

## 7. 核心发现

### 通过 (5/8)
- 前端权限矩阵: routeGuard (all-of permissions + any-of roles + AND) + PermissionGuard + 菜单过滤三层一致
- run-once 保护: 前后端权限一致，仅 GET_FSUINFO + GET_DATA，无 SET 端点
- SET 命令拒绝: 安全门禁 + 前端 PermissionGuard + run-once 不含 SET
- 前端状态口径: 9 态精确分类，meta 缺失优雅降级
- 测试基线: 55 tests 0/0/0，npm run build 通过

### 未通过 (4 项 P0)
1. **9 个 Controller 未加 @RequirePermission**: FsuDevice, MonitoringPoint, Cabinet, HistoryData, DeviceHeartbeat, ActiveAlarmAudit, BInterfaceCommand, BInterfaceSession, FtpTransferRecord
2. **HTTP 401/403 非真实状态码**: GlobalExceptionHandler 返回 HTTP 200 + body code=401/403，前端无法通过 error.response.status 区分
3. **DataScopeService 零调用点**: stationScope/fsuScope 始终为空，数据隔离不成立
4. **审计日志仅有 logPermissionDenied 被调用**: logRawXmlAccess/logRawXmlDownload/logRunOnce/logSetCommandBlocked 无调用点
5. **isAdminLike() 语义冲突**: admin 角色在种子数据中不含 raw/run-once 权限，但 isAdminLike() 允许绕过

## 8. 测试命令和结果

```bash
# 前端
cd frontend && npm run build  # 通过

# 后端
cd backend
mvn test -Dtest="SecurityInfrastructureTest,BInterface2016SetCommandSafetyTest,ReadOnlyIntegrationSafetyTest,BInterface2016ReadOnlyRunOnceProbeIntegrationTest"
# 结果: 55 tests, 0 failures, 0 errors, 0 skipped
```

未执行全量 mvn test (CODEX 报告 162 CommandHandlerTest 有 6 failures)。

## 9. 风险

- **高风险**: 9 个 Controller 默认放行 → 任何已认证用户可访问 FSU 设备/点位/机柜/历史数据 API
- **高风险**: 数据隔离不成立 → 三方用户可看到全部数据
- **中风险**: HTTP 401/403 非真实状态码 → 前端错误处理依赖 error.response.status
- **低风险**: isAdminLike() 语义冲突 → admin 种子数据不含 raw 但可绕过

## 10. 遗留问题

5 项 P0 修复任务: BE-AUTH-P0-001-FIX-001 ~ FIX-005 (详见 follow-up TODO)

## 11. 下一步建议

1. 立即进入 BE-AUTH-P0-001-FIX-001~005 (优先级: FIX-002 → FIX-001 → FIX-005 → FIX-003 → FIX-004)
2. P0 闭合后执行 AUTH-MATRIX-VERIFY-002 重新验证
3. 重新验证通过后方可进入 FE-P1 页面建设
4. 数据隔离闭合后方可规划三方授权页面

## 12. git diff 摘要

无业务代码修改。

## 13. git status 摘要

仅新增审计/任务/记忆文档。
