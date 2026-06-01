# 审计：LANDING-015-FIX-001 补齐真实 FSU run-once 测试入口

## 1. 日期
2026-05-21

## 2. 问题

LANDING-015 报告引用了 Landing015RealFsuIntegrationTest 但文件未创建。

## 3. 修复

新增 `Landing015RealFsuIntegrationTest.java`:
- @Disabled 默认
- @Tag("real-fsu")
- @EnabledIfSystemProperty(realFsuTest.enabled=true)
- 手动 wiring (不依赖 Spring)
- 仅执行 GET_FSUINFO (Code=1701)

## 4. 测试

1216 tests, 0/0/6 (+1 skipped).

## 5. 现场执行

```bash
mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true
```
