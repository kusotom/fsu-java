# LANDING-007 B接口2016 DeviceID GET_DATA 真实点位查询

## 任务目标
核对 B接口2016 GET_DATA 协议结构，使用真实 DeviceID 执行点位查询，尝试获取 SPID/SignalID/Value。

## 架构判断
- 新增: Landing007GetDataDeviceList 测试类 (手动 wiring)
- 不修改: 业务代码, 通信层, Scheduler, SET 体系

## 协议核对
2016 GET_DATA 请求结构 (经验证的维谛 eStoneII 格式):
- PK_Type: Name+Code, Code=401 (GET_DATA), Code=402 (GET_DATA_ACK)
- Info: FsuId + FsuCode + DeviceList
- DeviceList 在 Info 内 (非 xmlData)
- Device 有 Id/Code 属性, TSemaphore 可选
- DeviceID=全9 为通配

## 执行结果
4 种策略 × 真实 FSU:

| 策略 | 结果 |
|------|------|
| DeviceList (4 DeviceIDs) | Result=1, DeviceList 空 |
| DeviceList + TSemaphore | Result=1, DeviceList 空 |
| 单设备 (51051241820004) | Result=1, DeviceList 空 |
| 通配 (DeviceID=全9) | Result=1, DeviceList 空 |

- GET_DATA_ACK Code=402 确认协议正确
- FsuId/FsuCode 回显正确
- 所有响应一致: 1034 bytes RPC, 410 bytes doc
- FSU 当前无任何测量点位数据

## 候选点位表
未生成 — FSU 未返回 SPID/SignalID/Value。

## 测试
```
mvn test -Dtest='Landing007GetDataDeviceList' -DrealFsuTest.enabled=true → BUILD SUCCESS
mvn test → 1164 tests, 0 failures (默认不访问真实 FSU)
```

## 遗留问题
1. FSU 当前无监控点位数据 — 需管理方确认: 设备是否已实际配置监控模块和传感器
2. SPID/SignalID 映射表仍未知 — 即使有数据也无法解读
3. TSemaphore ID 候选值 (来自 Python 探测) 需与管理方确认
