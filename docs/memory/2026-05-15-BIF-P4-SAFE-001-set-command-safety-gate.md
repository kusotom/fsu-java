---
name: bif-p4-safe-001-set-command-safety-gate
description: SET类命令统一安全门禁 — Properties+Gate+Decision, 8+5命令全覆盖, 默认全false, 32新测试, 991全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-SAFE-001
## 任务名称 SET 类命令统一安全门禁配置
## 操作时间 2026-05-15
## 核心改动
- SetCommandRiskLevel: LOW/MEDIUM/HIGH/CRITICAL
- SetCommandSafetyProperties: @ConfigurationProperties, 8条命令逐条配置
- SetCommandSafetyDecision: Builder模式安全判定结果
- SetCommandSafetyGate: 统一门禁服务 (normalize→config→check)
- application.yml: +set-command-safety 配置块 (全部默认false)

## 纳入门禁的命令
2024标准(8): SET_SCIP/SET_SCHEMECONFIG/SET_FACTORYCONFIG/SET_SPCONFIGOPTION/SET_RMCTRLCMD/SET_SUFTP/SET_TIME/SET_SUREBOOT
旧兼容(5): SET_THRESHOLD/SET_POINT/SET_FTP/SET_FSUREBOOT/TIME_CHECK

## 测试 32/32 | 全量 991 tests, 0 failures
## 未访问真实设备, 未真实执行任何SET命令
