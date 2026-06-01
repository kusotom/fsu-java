# BIF2016-CONNECTION-003-FIX-006 ResultCode=1 语义清理与 WORKING-MEMORY 同步

## 1. 任务目标
本任务用于修复 FIX-005 后 Codex 指出的 ResultCode=1 语义矛盾、WORKING-MEMORY 未同步、FIX-003/FIX-005 归档内容不完整等问题。

## 2. Codex 上一轮 FAIL 原因
- WORKING-MEMORY 未实际同步 GET_DATA 注册上下文路线、Result UNKNOWN、CONNECTION-005 后置状态。
- FIX-003 / FIX-005 root audit 和 memory 内容仍不完整。
- FAST-REALDATA-003A / 003B 当前有效正文仍保留 ResultCode 1 = FAILURE 等旧口径。
- 精确 grep 剩余 26 行，语义复核仍发现有效文档矛盾口径。

## 3. 本轮修复内容
- WORKING-MEMORY 已实际同步。
- FAST-REALDATA-003A / 003B 当前有效正文已消除 ResultCode 1 = FAILURE 语义矛盾。
- FIX-003 / FIX-005 audit 和 memory 内容已补全。
- 已新增 FIX-006 audit / memory。
- 已更新 README 索引。

## 4. 搜索与清理结果
- 精确 grep 修正前/后已记录。
- 语义 grep 修正前/后已记录。
- 当前有效文档中 Result=1 / ResultCode 1 表示失败的口径已清理。
- 剩余命中如存在，只能是历史审计原文或 FIX 文档中用于说明旧错误口径已修正的引用。

## 5. 当前正确 Result 口径
- B接口2016 EnumResult：FAILURE=0，SUCCESS=1。
- GET_DATA_ACK Code=402 + Result=1 表示协议层 SUCCESS。
- GET_DATA_ACK Code=402 + Result=0 表示协议层 FAILURE。
- 空 DeviceList / valuesReturned=0 表示协议层成功，但 FSU 未返回测点值。
- Result 缺失或非 0/1 表示 UNKNOWN。

## 6. CONNECTION-003 当前状态
- GET_DATA run-once 主线已从 manual_probe/static_config 修正为 login_registration_context。
- registrationContext.fsuIp 实际进入 FsuServiceRequest URL。
- URL 构造只替换 host，scheme/port/path/query 来自模板。
- DeviceList 来自 LOGIN deviceCapabilities。
- Device.Code 使用 capability.deviceCode。
- deviceCode 缺失时显式 fallback。
- manual_probe 仅保留为人工只读诊断。
- 随机 GET_DATA 策略扩展暂停。
- 当前未进入真实 FSU 复测。
- 真实复测另开 BIF2016-CONNECTION-005。

## 7. 安全边界
- 未修改 Java/Vue/SQL。
- 未执行 SET。
- 未启 Scheduler。
- 未执行 DDL/DML。
- 未修改 monitoring_point / realtime_data / alarm_record。
- 未访问真实 FSU。
- 未改变 real-call-enabled 默认值。
- 未进入 BIF2016-CONNECTION-005。

## 8. 下一步
- 下一步为 CODEX-REVIEW-BIF2016-CONNECTION-003-FIX-008。
- 通过后才允许进入 BIF2016-CONNECTION-005。
