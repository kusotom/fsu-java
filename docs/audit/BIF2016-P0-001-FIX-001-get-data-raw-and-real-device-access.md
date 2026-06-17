# BIF2016-P0-001-FIX-001: realDeviceAccessed 透传修复

## 1. 日期
2026-05-21

## 2. 根因

`BInterface2016GetDataResult.successEmpty()` 未接受 `realDeviceAccessed` 参数，默认为 false。

## 3. 修复

`successEmpty()` 新增参数: (fsuCode, fsuId, realDeviceAccessed, rawRequest, rawResponse)
Service 空数据路径透传 `realCall`。

## 4. 测试

1279 tests, 0/0/9. `shouldReturnSuccessForEmptyAck` 断言 `isRealDeviceAccessed()=true`.
