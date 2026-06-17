# 工程记忆：LANDING-015 B接口2016 GET_FSUINFO 心跳轮询

## 1. 时间
2026-05-21

## 2. 实现

BInterface2016GetFsuInfoService: 2016 Code=1701 GET_FSUINFO 主动轮询。
严格校验 ACK=1702, Result=0 后才更新 fsu_status.last_heartbeat。
CPU/MEM 写入 statusDetail 不新增表字段。

## 3. 测试

1215 tests, 0/0/5. 2024 GetSuInfoService 不受影响。
