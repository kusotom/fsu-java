# BIF2016-CONNECTION-003-FIX-003 审计 — runGetDataProbe 主链路测试与 Result 口径修复

## 1. 审计范围
本审计覆盖 BIF2016-CONNECTION-003-FIX-003 任务的执行结果：runGetDataProbe 主链路测试、Result 口径修正、README/WORKING-MEMORY 同步。

## 2. 协议一致性
- B接口2016 EnumResult 确认：FAILURE=0，SUCCESS=1。
- GET_DATA_ACK Code=402 + Result=1 表示协议层 SUCCESS。
- GET_DATA_ACK Code=402 + Result=0 表示协议层 FAILURE。
- 空 DeviceList / valuesReturned=0 表示协议层成功但 FSU 未返回测点值。
- 与 openspec 03-constants.md 相反的记载已标记为以协议原文为准。

## 3. 修复内容核验
- 新增 integration test 直接调用 BInterface2016ReadOnlyRunOnceService.runGetDataProbe。
- 使用 fake registrationContextService + fake FSU client。
- capture 最终 FsuServiceRequest。
- actualServiceUrl 已进入最终请求。
- URL 构造只替换 host，scheme / port / path / query 来自模板。
- Device.Code 使用 capability.deviceCode，缺失时显式 fallback。
- Result=1→SUCCESS, Result=0→FAILURE, 缺失/非0/1→UNKNOWN。
- manual_probe 仅保留为人工只读诊断。

## 4. 测试基线
- ReadOnlyRunOnce：33 tests，0 failures。
- RegistrationContext：30 tests，0 failures。
- 测试使用 fake client / mock context，不访问真实 FSU。

## 5. 安全边界
- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未执行 DDL/DML。
- 未修改 monitoring_point / realtime_data / alarm_record。
- 未改变 real-call-enabled 默认值。

## 6. 审计结论
PASS — 修复内容与 memory 记录一致，协议口径正确，安全边界完整。
