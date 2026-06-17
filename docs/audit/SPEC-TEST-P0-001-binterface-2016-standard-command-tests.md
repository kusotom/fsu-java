# SPEC-TEST-P0-001 B接口2016 标准主命令测试建设

日期：2026-05-26  
执行者：Codex  
范围：只新增测试、fixture、审计/记忆文档；未修改 Java 生产代码、SQL、前端。

## 1. 目标

基于 `openspec/protocols/binterface-2016/` 中 SPEC-P0-001 重建后的标准协议库，新增 B接口2016 主命令协议测试，用测试暴露当前实现与标准 2016 的差异。

## 2. 新增测试

| 文件 | 覆盖内容 |
|---|---|
| `BInterface2016StandardCommandMatrixTest.java` | 2016 Code/ACK Code、HEARTBEAT无标准码、2016/2024冲突码、Result字段 |
| `BInterface2016ResultSemanticsTest.java` | `Result=1`成功、`Result=0`失败、`ResultCode`不覆盖标准2016、GET_DATA空DeviceList分类 |
| `BInterface2016XmlSampleReplayTest.java` | standard fixture 回放、SOAP解析、PK_Type Name+Code、Result字段 |
| `BInterface2016ScServiceCommandTest.java` | LOGIN/LOGOUT/SEND_ALARM入站解析、TIME_CHECK方向说明、ACK标准格式断言 |
| `BInterface2016FsuServiceReadOnlyCommandTest.java` | GET_DATA/GET_LOGININFO/GET_FTP/GET_FSUINFO/GET_THRESHOLD/TIME_CHECK出站标准构造与只读服务请求检查 |
| `BInterface2016SetCommandSafetyTest.java` | SET_POINT/SET_THRESHOLD/SET_LOGININFO/SET_FTP/SET_FSUREBOOT默认禁用、Scheduler/REST/run-once边界、离线XML构造 |
| `BInterface2016EmersonProfileCompatibilityTest.java` | standard-2016 与 emerson-2016 profile/fixture 隔离 |
| `BInterface2016StandardTestSupport.java` | 测试共享的2016命令矩阵与XML工具 |

## 3. 新增 fixture

| 目录 | 内容 |
|---|---|
| `backend/src/test/resources/fixtures/b_interface_2016_standard/` | 18个标准2016样本，覆盖9个主命令成功/失败或异常样本 |
| `backend/src/test/resources/fixtures/b_interface_2016_emerson/` | Emerson GET_DATA `Result=1 + DeviceList空` profile样本 |

## 4. 执行结果

命令：

```bash
mvn test -Dtest='*BInterface2016*'
```

结果：编译通过，测试执行失败。  
汇总：`Tests run: 110, Failures: 6, Errors: 0, Skipped: 0`。

## 5. 暴露的协议差异

| 测试 | 差异 |
|---|---|
| `BInterface2016StandardCommandMatrixTest.parsedNameCodeShouldBeValidatedAgainst2016MatrixNot2024Matrix` | `PkTypeDescriptor.fromNameCode("GET_DATA", 401)` 当前按2024码表判定不一致，2016标准应按GET_DATA=401一致处理 |
| `BInterface2016StandardCommandMatrixTest.commandResultFor2016StandardShouldUseResultNotResultCode` | `CommandResult.success()` 当前输出`ResultCode=0`，标准2016应输出`Result=1` |
| `BInterface2016ScServiceCommandTest.scServiceAckXmlShouldUseAckPkTypeNameCodeAndResultField` | 当前SCService ACK构造路径未输出`LOGIN_ACK/102`的Name+Code结构，且仍依赖`ResultCode` |
| `BInterface2016FsuServiceReadOnlyCommandTest.currentReadOnlyServicesShouldRequestLegacy2016PkTypeFormat` | `GET_LOGININFO`、`GET_FTP`、`GET_THRESHOLD`、`TIME_CHECK`当前`pkTypeFormat=null`，真实HTTP路径默认会落到2024/兼容映射，而不是2016 Name+Code |
| `BInterface2016SetCommandSafetyTest.every2016SetCommandShouldBeRecognizedByTheSafetyGate` | `SET_LOGININFO`未进入统一SET安全门识别范围 |
| `BInterface2016SetCommandSafetyTest.all2016SetCommandsShouldBeMarkedHighRisk` | `SET_THRESHOLD`、`SET_LOGININFO`未标记为高风险 |

## 6. 安全记录

- 未修改生产业务代码。
- 未执行任何 SET_POINT / SET_THRESHOLD / SET_LOGININFO / SET_FTP / SET_FSUREBOOT 真实调用。
- 未启用真实 FSU 测试开关。
- 未启动 Scheduler。
- 本次 Maven 命令运行了既有 `BInterface2016ReadOnlyRunOnceProbeIntegrationTest`，该既有测试写入了 `backend/docs/landing/raw-samples/*20260526-224049.xml` raw sample 文件；这是测试副作用，不是本次新增真实FSU访问。

## 7. 后续建议

1. 先修复 2016 标准路径的 `PK_Type Name+Code` 解析：2016码表与2024码表必须分层。
2. 将标准2016 ACK构造从`ResultCode`迁移到`Result=1/0`，并补充工程兼容映射。
3. 为 `GET_LOGININFO`、`GET_FTP`、`GET_THRESHOLD`、`TIME_CHECK` 出站只读服务显式设置 `pkTypeFormat="legacy-2016"` 或等价标准2016路径。
4. 将 `SET_LOGININFO` 纳入 SET 安全门，并将 `SET_THRESHOLD`、`SET_LOGININFO` 标记高风险。
