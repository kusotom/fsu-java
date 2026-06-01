# BIF2016-CONNECTION-003-FIX-003 runGetDataProbe 主链路测试与 Result 口径修复

## 1. 任务目标
本任务用于修复 BIF2016-CONNECTION-003 / FIX-002 后仍存在的问题：runGetDataProbe 主链路测试缺失、Result 缺失时 statusText 不符合要求、README/WORKING-MEMORY 未同步、旧文档 Result=1 错误口径残留。

## 2. Codex 上一轮 FAIL 原因
- 测试没有覆盖 BInterface2016ReadOnlyRunOnceService.runGetDataProbe 主链路。
- 未 capture 最终 FsuServiceRequest。
- Result 缺失时未按 UNKNOWN 处理。
- README / WORKING-MEMORY 未同步。
- 旧文档仍存在 Result=1 错误口径。

## 3. 修复内容
- 新增 integration test 直接调用 BInterface2016ReadOnlyRunOnceService.runGetDataProbe。
- 使用 fake registrationContextService。
- 使用 fake FSU client。
- capture 最终 FsuServiceRequest。
- actualServiceUrl 已进入最终请求。
- URL 构造只替换 host，scheme / port / path / query 来自模板。
- Device.Code 使用 capability.deviceCode。
- deviceCode 缺失时显式 fallback。
- Result=1 按 B接口2016 EnumResult 解释为 SUCCESS。
- Result=0 按 B接口2016 EnumResult 解释为 FAILURE。
- Result 缺失或非 0/1 按 UNKNOWN 处理。
- manual_probe 仅保留人工只读诊断。

## 4. 测试结果
- ReadOnlyRunOnce：33 tests，0 failures。
- RegistrationContext：30 tests，0 failures。
- 测试使用 fake client / mock context，不访问真实 FSU。
- 未执行真实 FSU 测试。

## 5. 当前正确 Result 口径
- B接口2016 EnumResult：FAILURE=0，SUCCESS=1。
- GET_DATA_ACK Code=402 + Result=1 表示协议层 SUCCESS。
- GET_DATA_ACK Code=402 + Result=0 表示协议层 FAILURE。
- 空 DeviceList / valuesReturned=0 表示协议层成功，但 FSU 未返回测点值。
- Result 缺失或非 0/1 表示 UNKNOWN。

## 6. CONNECTION-003 当前状态
- GET_DATA run-once 主线已从 manual_probe/static_config 修正为 login_registration_context。
- registrationContext.fsuIp 实际进入 FsuServiceRequest URL。
- DeviceList 来自 LOGIN deviceCapabilities。
- Device.Code 使用 capability.deviceCode，缺失时显式 fallback。
- manual_probe 仅保留为人工只读诊断。
- 随机 GET_DATA 策略扩展暂停。

## 7. 安全边界
- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未执行 DDL/DML。
- 未修改 monitoring_point / realtime_data / alarm_record。
- 未改变 real-call-enabled 默认值。
- 未进入 BIF2016-CONNECTION-005。

## 8. 下一步
- 后续真实只读复测必须另开 BIF2016-CONNECTION-005。
- 进入 CONNECTION-005 前必须通过 Codex 审计。
