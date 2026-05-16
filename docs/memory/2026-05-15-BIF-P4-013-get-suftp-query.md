---
name: bif-p4-013-get-suftp-query
description: GET_SUFTP查询闭环 — 2024标准FTP查询, Service+Handler+Stub+脱敏, 11新测试, 934全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-013
## 任务名称 GET_SUFTP 查询闭环
## 操作时间 2026-05-15

## 核心改动
- GetSuFtpResult: 脱敏结果模型
- GetSuFtpService: SC→FSU FTP查询
- GetSuFtpCommandHandler: SUID提取+登录校验
- BInterfacePkType: 19值 (+GET_SUFTP)
- StubFsuServiceClient: +GET_SUFTP stub (802 PK_Type格式)

## 测试 11/11 | 全量 934 tests, 0 failures
## 未访问真实设备, 未实现 SET_SUFTP
