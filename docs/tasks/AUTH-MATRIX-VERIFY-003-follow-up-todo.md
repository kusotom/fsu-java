# AUTH-MATRIX-VERIFY-003: 复验后续任务

**日期:** 2026-05-29
**状态:** 复验通过，P0 全部闭合

## 后续任务

### P1: 补全测试类
- AuditLogWiringTest (broken by linter, needs proper implementation)
- AuthHttpStatusTest
- AdminLikeBypassTest
- DataScopeIntegrationTest
- RawXmlPermissionScopeTest
- IdBasedScopeAccessTest

### P1: 4 个预存协议失败
- CommandResultTest ×3: B接口2016 Result=1 语义
- FsuServiceRpcAdapterTest ×1: SOAP ENC 命名空间

### P1: DataScope SQL 化
- 当前为内存过滤，建议迁移到 JPA Criteria/Specification

### P2: 全量 AUTH-MATRIX-VERIFY-003 端到端测试
- 含真实 HTTP 状态码验证 (MockMvc)

### 可以开始
- FE-P1 页面建设
- 三方授权页面规划
