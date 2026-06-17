# LANDING-015 Spec：B接口2016 GET_FSUINFO 心跳轮询与状态刷新

> 规格版本：1.0 | 创建日期：2026-05-21

## 1. 背景

协议基线已调整为 B接口2016 为主开发依据。现有 GetSuInfoService 使用 2024 Code=1001 调用 FSUService。需要新增 2016 GET_FSUINFO (Code=1701) 的主动轮询能力。

## 2. 目标

- 新增 BInterface2016GetFsuInfoService 使用 2016 Code=1701 + pkTypeFormat=legacy-2016
- 解析 GET_FSUINFO_ACK (Code=1702)，提取 CPUUsage/MEMUsage
- 更新 b_interface_fsu_status.last_heartbeat 和 online_status
- run-once 手动触发，不启用 Scheduler

## 3. 非目标

不执行 SET，不启用 Scheduler，不修改 GetSuInfoService 2024 逻辑，不默认访问真实 FSU

## 4. 验收标准

请求使用 Code=1701, pkTypeFormat=legacy-2016；解析 CPUUsage/MEMUsage；更新 last_heartbeat；测试全量 0/0/5。

## 5. 安全边界

SET/Scheduler: 禁止。真实 FSU: @Disabled 默认, 显式开关。
