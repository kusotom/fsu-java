# BIF2016-P0-001 GET_DATA DeviceList/TSemaphore 闭环

## 1. 时间
2026-05-21

## 2. 实现

BInterface2016GetDataService: 2016 GET_DATA (Code=401, legacy-2016)
- Info 构造: FsuId+FsuCode+DeviceList(Device Id/Code + 可选 TSemaphore)
- 响应解析: GET_DATA_ACK Code=402, Values→DeviceList→TSemaphore
- 空 DeviceList: success=true, emptyData=true
- 入库: 匹配 monitoring_point → realtime_data upsert
- 未匹配: unmapped 清单

## 3. 测试

1279 tests, 0/0/9. 新增 Bif2016P0001RealFsuGetDataIntegrationTest (@Tag real-fsu)
