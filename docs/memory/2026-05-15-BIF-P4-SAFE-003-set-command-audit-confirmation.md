---
name: bif-p4-safe-003-set-command-audit-confirmation
description: SET命令审计日志与confirmationToken — Token生成/验证/失效, 审计record+service, SHA-256哈希, 内存实现, 20新测试, 1020全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-SAFE-003
## 任务名称 SET 命令审计日志与 confirmationToken 机制
## 操作时间 2026-05-15
## 核心改动
- ConfirmationToken: token模型(SHA-256 hash, 命令/SUID绑定, TTL, 单次使用)
- ConfirmationTokenService: 生成/验证/markUsed/失效(过期/已用/命令不匹配/SUID不匹配)
- SetCommandAuditRecord: Builder模式审计记录(不含明文token/密码)
- SetCommandAuditService: 内存审计(rejected/dryRun/allowed)
- SetCommandAuditIntegrationTest: 10 tests (审计+token联合)
- ConfirmationTokenServiceTest: 10 tests (生成/验证/过期/已用/不匹配/null)

## 安全策略
- Token: UUID生成 → SHA-256 hash存储 → 明文仅返回一次
- 审计: 不记录明文token、不记录密码、失败不影响业务
- 内存实现, 生产需替换持久化

## 测试 20/20 | 全量 1020 tests, 0 failures
## 未访问真实设备, 未保存明文token, 未执行SET类命令
