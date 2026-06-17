# SPEC-TEST-P0-001 真实 FSU 只读 Run-Once 记录

日期：2026-05-26  
执行范围：B接口2016真实FSU只读命令，覆盖GET_DATA、GET_LOGININFO、GET_FTP、GET_FSUINFO、GET_THRESHOLD，并补跑GET_FSUINFO服务层主动状态读取。  
安全边界：未执行SET命令，未启动Scheduler，未写业务数据库，未访问REST run-once入口。

## 1. 执行命令

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest' -DrealFsuTest.enabled=true
```

## 2. 目标设备

| 项 | 值 |
|---|---|
| FSUCode | 51051243812345 |
| ServiceUrl | http://192.168.100.100:8080/services/FSUService |
| 协议路径 | B接口2016 FSUService RPC |
| 测试类 | `backend/src/test/java/com/dcim/platform/binterface/BInterface2016ReadOnlyRealFsuRunOnceTest.java` |

## 3. 执行结果

| 命令 | 请求Code | ACK | Result | 结果 |
|---|---:|---|---|---|
| GET_DATA | 401 | GET_DATA_ACK / 402 | 1 | 通过；设备响应成功，DeviceList为空 |
| GET_LOGININFO | 1501 | GET_LOGININFO_ACK / 1502 | 缺失 | 通过只读观测；返回SCIP和4个Device，缺Result属Emerson差异 |
| GET_FTP | 1601 | GET_FTP_ACK / 1602 | 1 | 通过；返回FTP账号字段，raw样本含敏感字段，后续展示必须脱敏 |
| GET_FSUINFO | 1701 | GET_FSUINFO_ACK / 1702 | 0 | 通过只读观测；返回CPU/MEM，但Result与标准2016成功语义不一致 |
| GET_THRESHOLD | 1901 | GET_THRESHOLD_ACK / 1902 | 1 | 通过；返回请求Device节点，未返回TThreshold明细 |

Raw级run-once结果：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

服务层主动状态读取：

```bash
mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true
```

结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；`BInterface2016GetFsuInfoService` 实际访问真实FSU，返回 `cpu=22.85`、`mem=55.36`、`realDeviceAccessed=true`、`statusUpdated=true`。该测试使用stub repository，不写真实业务数据库。

## 4. Raw 样本

| 命令 | 文件 |
|---|---|
| GET_DATA | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA-51051243812345-20260526-230101-request.xml` |
| GET_DATA | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA-51051243812345-20260526-230101-response.xml` |
| GET_DATA | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA-51051243812345-20260526-230101-response-unwrapped.xml` |
| GET_FSUINFO | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-230101-request.xml` |
| GET_FSUINFO | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-230101-response.xml` |
| GET_FSUINFO | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-230101-response-unwrapped.xml` |
| GET_DATA | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA-51051243812345-20260526-232711-response-unwrapped.xml` |
| GET_LOGININFO | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_LOGININFO-51051243812345-20260526-232711-response-unwrapped.xml` |
| GET_FTP | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FTP-51051243812345-20260526-232711-response-unwrapped.xml` |
| GET_FSUINFO | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-232711-response-unwrapped.xml` |
| GET_THRESHOLD | `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_THRESHOLD-51051243812345-20260526-232711-response-unwrapped.xml` |

## 5. 关键实测

GET_DATA返回：

- `PK_Type.Name=GET_DATA_ACK`
- `Code=402`
- `Result=1`
- `Values/DeviceList`为空

GET_LOGININFO返回：

- `PK_Type.Name=GET_LOGININFO_ACK`
- `Code=1502`
- `DeviceList`包含4个Device
- 缺少`Result`

GET_FTP返回：

- `PK_Type.Name=GET_FTP_ACK`
- `Code=1602`
- `Result=1`
- 返回`UserName`和`Password`字段，后续报告和前端必须脱敏

GET_FSUINFO返回：

- `PK_Type.Name=GET_FSUINFO_ACK`
- `Code=1702`
- `CPUUsage=15.61`
- `MEMUsage=55.30`
- `Result=0`

GET_THRESHOLD返回：

- `PK_Type.Name=GET_THRESHOLD_ACK`
- `Code=1902`
- `Result=1`
- `Values/DeviceList`包含请求Device，未返回TThreshold明细

## 6. 协议结论

标准B接口2016仍按 `EnumResult: FAILURE=0, SUCCESS=1` 判定；本次 `GET_FSUINFO Result=0` 和 `GET_LOGININFO` 缺少`Result`只能作为Emerson真实设备profile差异，已写入 `openspec/protocols/binterface-2016/profiles/emerson-2016.md` 的 `[SPEC-2016-PROFILE-EMERSON-004]`、`[SPEC-2016-PROFILE-EMERSON-005]`。

后续不能把该差异写入 `profiles/standard-2016.md`，也不能用它覆盖标准2016离线协议测试。

## 7. 2026-05-26 23:41 FSU实时状态单项复测

按用户要求单独复测读取FSU实时状态：

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest#getFsuInfo_readOnly_statusQuery' -DrealFsuTest.enabled=true
mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true
```

结果：

| 层级 | 结果 |
|---|---|
| raw SOAP | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |
| 服务层 | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |

raw样本：

- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-234127-request.xml`
- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-234127-response.xml`
- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-234127-response-unwrapped.xml`

实测状态：

- raw响应：`GET_FSUINFO_ACK / 1702`
- raw响应：`CPUUsage=18.94`
- raw响应：`MEMUsage=55.37`
- raw响应：`Result=0`，仍按Emerson差异处理
- 服务层结果：`success=true`、`cpuUsage=18.94`、`memUsage=55.37`、`realDeviceAccessed=true`、`statusUpdated=true`

安全边界：未执行SET，未启动Scheduler，服务层测试使用stub repository，不写真实业务数据库。

## 8. 2026-05-26 23:44 传感器状态单项复测

按用户要求单独复测电压、电流、温湿度等传感器/测点实时状态。协议命令为GET_DATA：

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest#getData_readOnly_knownSensorStatus' -DrealFsuTest.enabled=true
```

结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

raw样本：

- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260526-234420-request.xml`
- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260526-234420-response.xml`
- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260526-234420-response-unwrapped.xml`

返回测点：

| DeviceID | Type | TSemaphore Id | MeasuredVal | SetupVal | Status |
|---|---:|---|---:|---:|---:|
| 51051241820004 | 2 | 0418002001 | 0 | 0 | 0 |
| 51051241830004 | 2 | 0418004001 | 0 | 0 | 0 |
| 51051241830004 | 2 | 0418007001 | 0 | 0 | 0 |
| 51051241830004 | 3 | 0418101001 | 0.0 | 0 | 0 |
| 51051241830004 | 3 | 0418102001 | 0.0 | 0 | 0 |
| 51051241840004 | 2 | 0418001001 | 0 | 0 | 0 |
| 51051240700002 | 3 | 0407102001 | 0.0 | 0 | 0 |
| 51051240700002 | 3 | 0407107001 | 0.0 | 0 | 0 |

结论：真实FSU可通过GET_DATA读取传感器状态结构和值，但当前所有返回值均为0或0.0。由于缺少厂家点位表，不能可靠标注这些Id分别对应电压、电流、温度或湿度；该结论已写入 `[SPEC-2016-PROFILE-EMERSON-006]`。

## 9. 2026-05-27 20:34 蓄电池总电压单项读取

按用户要求读取蓄电池电压。协议命令仍为GET_DATA，只读访问真实FSU：

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest#getData_readOnly_knownSensorStatus' -DrealFsuTest.enabled=true
```

结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

raw样本：

- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-request.xml`
- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-response.xml`
- `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-response-unwrapped.xml`

蓄电池组返回测点：

| DeviceID | Type | TSemaphore Id | 字典名称 | 单位 | MeasuredVal | SetupVal | Status |
|---|---:|---|---|---|---:|---:|---:|
| 51051240700002 | 3 | 0407102001 | D类机房/蓄电池组/总电压 | V | 53.9 | 0 | 0 |
| 51051240700002 | 3 | 0407107001 | D类机房/蓄电池组/后半组电压 | 未在字典行给出 | 0.0 | 0 | 0 |

结论：本次真实FSU返回 `GET_DATA_ACK / 402`、`Result=1`。依据 `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md` 中 `SPEC-2016-DICT-XLSX-SIGNAL-0256`，`0407102001` 为 D类机房蓄电池组总电压，单位V，本次实测值为 `53.9V`。安全边界：未执行SET，未启动Scheduler，未写业务数据库。
