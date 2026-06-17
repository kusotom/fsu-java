# LANDING-015-FIX-001: 补齐真实 FSU run-once 测试

## 1. 时间
2026-05-21

## 2. 问题

Landing015RealFsuIntegrationTest.java 未创建，现场无法执行 run-once。

## 3. 修复

新增测试文件，@Disabled 默认，1216 tests 0/0/6。
