# BIF2016-P1-002: 统一 2016 PK_Type 枚举和 alias 边界

## 1. 日期
2026-05-21

## 2. 审计结论

### legacy-2016 路径已安全

代码审计确认 `RealHttpFsuServiceClient` 的 legacy-2016 路径直接调用 `BInterfaceCommand2016.codeFor()`，不经过 AliasMapper。无需修改生产逻辑。

### BInterfacePkType 补齐

38 → 44 项。新增 18 个 2016 枚举项（含 LOGIN_ACK/LOGOUT/GET_HISDATA 等），保留 GET_HISTORY_DATA 为旧别名。

### 测试

- BInterfacePkTypeTest 扩展至 18 tests (含 30 项 2016 完整性 + Code 交叉验证)
- 1266 tests, 0/0/6

## 3. 修改

BInterfacePkType.java (补18项) + test (扩展)。生产逻辑 0 修改。
