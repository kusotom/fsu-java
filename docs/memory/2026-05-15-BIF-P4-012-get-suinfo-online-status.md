---
name: bif-p4-012-get-suinfo-online-status
description: GET_SUINFO在线状态闭环 — 2024标准心跳命令实现, Service+Handler+Stub, 11新测试, 923全量回归
metadata:
  type: project
---

## 任务编号
BIF-P4-012

## 任务名称
GET_SUINFO 在线状态闭环

## 操作时间
2026-05-15

## 核心改动
- GetSuInfoResult: 结果模型
- GetSuInfoService: SC→FSU查询, 响应解析, 状态更新
- GetSuInfoCommandHandler: SUID提取(FSUCode兼容), 登录校验
- BInterfacePkType: +GET_SUINFO
- StubFsuServiceClient: +GET_SUINFO stub (2024 PK_Type格式)

## 测试结果
- GetSuInfoServiceTest: 11/11 ✅
- 全量回归: 923 tests, 0 failures

## 是否访问真实设备
否

## 建议下一步
BIF-P4-013: 实现 GET_SUFTP / SET_TIME 等其他 2024 命令
