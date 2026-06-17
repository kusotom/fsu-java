# BE-AUTH-P0-FIX-002: 后端权限粒度 + DataScope + raw scope 闭环

## 修复 AUTH-MATRIX-VERIFY-002 的 6 项 P0

- Fix 1: 13 新权限点 + 19 方法级写权限
- Fix 2: raw:download requireAll 已有
- Fix 3-6: 8 Service 注入 DataScope + BInterface聚合 + getById scope + raw scope

## 测试: 217 tests, 0 failures

## 结论: P0 全部闭合。可进入 AUTH-MATRIX-VERIFY-003。
