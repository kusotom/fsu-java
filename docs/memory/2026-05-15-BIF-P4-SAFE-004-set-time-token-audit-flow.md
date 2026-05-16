---
name: bif-p4-safe-004-set-time-token-audit-flow
description: SET_TIME token+audit主流程集成 — executeWithSafety完整样板, Token验证→Gate判定→审计→执行→token消耗, 10新测试, 1030全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-SAFE-004
## 任务名称 SET_TIME confirmationToken + Audit 主流程集成
## 操作时间 2026-05-15
## 核心改动
- SetTimeExecutionRequest: 参数对象(suid/targetTime/token/requestedBy/source...)
- SetTimeService.executeWithSafety(): 完整安全流程
  1. Token验证 → 失败→审计+拒绝
  2. Gate判定 → rejected/dryRun/allowed
  3. 审计记录(rejected/dryRun/allowed)
  4. executeDirect()绕过门禁直接执行
  5. 成功→消耗token
- executeDirect(): 内部方法，绕过门禁
- 旧execute()兼容不变

## token策略
- 验证: SHA-256 hash查表, 过期/已用/命令不匹配/SUID不匹配→拒绝
- 消耗: dry-run不消耗, rejected不消耗, allowed+执行成功才消耗

## 测试 10/10 | 全量 1030 tests, 0 failures
## 未访问真实设备, 未保存明文token, SET_TIME样板完成
