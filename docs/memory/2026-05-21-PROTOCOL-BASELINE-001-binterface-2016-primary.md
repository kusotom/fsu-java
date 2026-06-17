# 工程记忆：PROTOCOL-BASELINE-001 B接口2016 确认为主开发依据

## 1. 时间
2026-05-21

## 2. 决策

B接口2016 = 当前主开发依据
B接口2024 = 未来升级兼容层

## 3. 后续路线

LANDING-015: 2016 GET_FSUINFO 轮询
LANDING-016: 2016 GET_DATA 轮询
LANDING-017: 2016 SEND_ALARM 事件接收
LANDING-018: DeviceID/SPID 映射导入

不再等待 FSU 主动 HEARTBEAT/SEND_DATA。

## 4. 修改

4 文件 (rules + memory)。Java 0 修改。
