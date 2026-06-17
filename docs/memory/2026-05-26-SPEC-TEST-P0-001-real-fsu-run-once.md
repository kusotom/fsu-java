# 2026-05-26 SPEC-TEST-P0-001 真实 FSU 只读 Run-Once

## 任务

按用户要求测试真实FSU。新增默认禁用的 `BInterface2016ReadOnlyRealFsuRunOnceTest`，执行B接口2016只读命令GET_DATA、GET_LOGININFO、GET_FTP、GET_FSUINFO、GET_THRESHOLD，保存raw request/response；另补跑 `Landing015RealFsuIntegrationTest` 验证服务层主动读取FSU状态。

## 安全边界

- 未执行SET_POINT、SET_THRESHOLD、SET_LOGININFO、SET_FTP、SET_FSUREBOOT。
- 未启动Scheduler。
- 未通过REST run-once入口绕过安全门。
- 未写业务数据库。
- 测试类使用 `@Tag("real-fsu")`、`@EnabledIfSystemProperty` 和 `Assumptions.assumeTrue` 双保险。

## 执行命令

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest' -DrealFsuTest.enabled=true
```

## 结果

Raw级run-once通过：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。  
服务层主动状态读取通过：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

| 命令 | 请求/ACK | 真实结果 |
|---|---|---|
| GET_DATA | 401/402 | `Result=1`，`DeviceList`为空 |
| GET_LOGININFO | 1501/1502 | 返回SCIP和4个Device，缺少`Result` |
| GET_FTP | 1601/1602 | `Result=1`，返回FTP账号字段，展示必须脱敏 |
| GET_FSUINFO | 1701/1702 | 返回CPU/MEM，但`Result=0` |
| GET_THRESHOLD | 1901/1902 | `Result=1`，返回请求Device，未返回TThreshold明细 |

## Raw 样本

已保存到 `backend/docs/landing/raw-samples/`，文件名前缀：

- `SPEC-TEST-P0-001-GET_DATA-51051243812345-20260526-230101-*`
- `SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-230101-*`
- `SPEC-TEST-P0-001-GET_DATA-51051243812345-20260526-232711-*`
- `SPEC-TEST-P0-001-GET_LOGININFO-51051243812345-20260526-232711-*`
- `SPEC-TEST-P0-001-GET_FTP-51051243812345-20260526-232711-*`
- `SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-232711-*`
- `SPEC-TEST-P0-001-GET_THRESHOLD-51051243812345-20260526-232711-*`

## 结论

真实FSU可达，FSUService RPC路径可用。GET_FSUINFO的`Result=0`不得写成标准B接口2016成功语义，已作为Emerson profile差异写入 `[SPEC-2016-PROFILE-EMERSON-004]`。GET_LOGININFO缺少`Result`已写入 `[SPEC-2016-PROFILE-EMERSON-005]`。

## 2026-05-26 23:41 状态读取复测

用户要求单独测试读取FSU实时状态。已执行：

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest#getFsuInfo_readOnly_statusQuery' -DrealFsuTest.enabled=true
mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true
```

两项均通过。raw样本前缀：`SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-234127-*`。本次实时状态：`CPUUsage=18.94`，`MEMUsage=55.37`，服务层解析结果 `success=true`、`realDeviceAccessed=true`、`statusUpdated=true`。未执行SET，未启动Scheduler，未写真实业务数据库。

## 2026-05-26 23:44 传感器状态复测

用户说明需要电压、电流、温湿度等传感器状态。已执行：

```bash
mvn test -Dtest='Landing007GetDataDeviceList' -DrealFsuTest.enabled=true
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest#getData_readOnly_knownSensorStatus' -DrealFsuTest.enabled=true
```

两项均通过。`Landing007GetDataDeviceList`验证4种策略：全DeviceList、显式Id、单设备、全9通配；只有显式Id策略返回TSemaphore。合规timestamp raw样本前缀：`SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260526-234420-*`。

本次返回8个TSemaphore，`MeasuredVal`均为`0`或`0.0`，`Status=0`。由于缺少厂家点位表，当前只能确认测点Id和值，不能可靠标注哪个Id对应电压、电流、温度或湿度；该结论写入 `[SPEC-2016-PROFILE-EMERSON-006]`。

## 2026-05-27 20:34 蓄电池总电压读取

用户要求读取蓄电池电压。已执行只读GET_DATA：

```bash
mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest#getData_readOnly_knownSensorStatus' -DrealFsuTest.enabled=true
```

测试通过：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。raw样本前缀：`SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-*`。

本次真实返回：`GET_DATA_ACK / 402`，`Result=1`。蓄电池组设备 `51051240700002` 下：

| TSemaphore Id | 字典名称 | 单位 | MeasuredVal | Status |
|---|---|---|---:|---:|
| 0407102001 | D类机房/蓄电池组/总电压 | V | 53.9 | 0 |
| 0407107001 | D类机房/蓄电池组/后半组电压 | 待确认 | 0.0 | 0 |

`0407102001` 对应字典矩阵 `SPEC-2016-DICT-XLSX-SIGNAL-0256`。未执行SET，未启动Scheduler，未写业务数据库。
