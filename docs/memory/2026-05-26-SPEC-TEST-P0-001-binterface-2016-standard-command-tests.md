# 2026-05-26 SPEC-TEST-P0-001 B接口2016 标准主命令测试建设

## 任务

基于 SPEC-P0-001 新建的 B接口2016 标准协议库，新增主命令测试脚本，优先用测试暴露当前平台与标准2016的差异，不大规模修改业务代码。

## 变更

- 新增 `BInterface2016StandardCommandMatrixTest`
- 新增 `BInterface2016ResultSemanticsTest`
- 新增 `BInterface2016XmlSampleReplayTest`
- 新增 `BInterface2016ScServiceCommandTest`
- 新增 `BInterface2016FsuServiceReadOnlyCommandTest`
- 新增 `BInterface2016SetCommandSafetyTest`
- 新增 `BInterface2016EmersonProfileCompatibilityTest`
- 新增 `BInterface2016StandardTestSupport`
- 新增 standard/emerson fixture 目录
- 新增审计报告：`docs/audit/SPEC-TEST-P0-001-binterface-2016-standard-command-tests.md`

## 验证

执行：

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/backend
mvn test -Dtest='*BInterface2016*'
```

结果：编译通过，测试失败。  
汇总：`Tests run: 110, Failures: 6, Errors: 0, Skipped: 0`。

## 暴露差异

- `PkTypeDescriptor` 仍用2024码表校验2016 Name+Code，导致 `GET_DATA/401` 被判不一致。
- `CommandResult.success()` 仍输出 `ResultCode=0`，而非标准2016 `Result=1`。
- SCService ACK路径未输出 `LOGIN_ACK/102` 的标准Name+Code结构。
- `GET_LOGININFO`、`GET_FTP`、`GET_THRESHOLD`、`TIME_CHECK` 出站只读服务未显式使用 `legacy-2016`。
- SET安全门未识别 `SET_LOGININFO`。
- `SET_THRESHOLD`、`SET_LOGININFO` 未标记高风险。

## 安全边界

- 未修改生产业务代码、SQL、前端。
- 未执行 SET 类命令。
- 未启用真实 FSU 测试。
- 未启动 Scheduler。
- 运行 `*BInterface2016*` 时既有 run-once probe 测试写入了 `backend/docs/landing/raw-samples/*20260526-224049.xml`，属于既有测试副作用。

## 下一步

按失败测试逐项修复标准2016路径：先分离2016/2024 PK_Type校验，再迁移标准ACK Result语义，最后补齐只读出站服务和SET安全门。
