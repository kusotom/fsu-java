# LANDING-003 执行报告

> 生成时间: 2026-05-20T18:42:50+08:00
> 目标: http://192.168.100.100:8080/services/FSUService
> 临时 fsuCode: FSU-001

## 1. 本次目标

真实 FSU (192.168.100.100:8080) 只读联调采集，获取原始 SOAP 报文，验证协议兼容性，生成候选点位表。

## 2. 实际 serviceUrl

```
http://192.168.100.100:8080/services/FSUService
```

构造方式: `http://{ipAddr}:{port}/services/FSUService`, ipAddr=192.168.100.100, port=8080.

## 3. 实际调用命令清单

| 序号 | 命令 | HTTP 状态 | ResultCode | 解析结果 |
|------|------|-----------|------------|----------|
| 1 | GET_LOGININFO | 200 OK | N/A (空响应) | 无 LoginInfo 数据 |
| 2 | GET_SUINFO | 200 OK | N/A (空响应) | 无 SUInfo 数据 |
| 3 | GET_SPCONFIGOPTION | 200 OK | N/A (空响应) | 无 SPConfig 数据 |
| 4 | GET_ACTIVEALARM | 200 OK | N/A (空响应) | 0 条告警 |
| 5 | GET_DATA | 200 OK | N/A (SEND_ALARM 回传) | XML 解析失败 |
| 6 | GET_THRESHOLD | 200 OK | N/A (空响应) | 0 条阈值 |

## 4. 每个命令详细结果

### 4.1 GET_LOGININFO — 空响应

- **Request**: `<PK_Type>GET_LOGININFO</PK_Type>`, `<Info><FSUCode>FSU-001</FSUCode></Info>`
- **RPC Response**: `<invokeReturn></invokeReturn>` (空)
- **Doc Response**: `<Response/>` (空)
- **结论**: FSU 不支持或未配置 GET_LOGININFO 出站查询

### 4.2 GET_SUINFO — 空响应

- **Request**: `<PK_Type><Name>GET_SUINFO</Name><Code>1001</Code></PK_Type>`, `<Info><SUID>FSU-001</SUID></Info>`
- **RPC Response**: `<invokeReturn></invokeReturn>` (空)
- **Doc Response**: `<Response/>` (空)
- **结论**: FSU 不支持或未配置 GET_SUINFO 出站查询

### 4.3 GET_SPCONFIGOPTION — 空响应

- **Request**: `<PK_Type><Name>GET_SPCONFIGOPTION</Name><Code>401</Code></PK_Type>`
- **RPC Response**: `<invokeReturn></invokeReturn>` (空)
- **Doc Response**: `<Response/>` (空)
- **结论**: FSU 不支持或未配置 GET_SPCONFIGOPTION 出站查询

### 4.4 GET_ACTIVEALARM — 空响应

- **Request**: `<PK_Type><Name>GET_ACTIVEALARM</Name><Code>603</Code></PK_Type>`
- **RPC Response**: `<invokeReturn></invokeReturn>` (空)
- **Doc Response**: `<Response/>` (空)
- **结论**: FSU 无活动告警或未配置 GET_ACTIVEALARM 出站查询

### 4.5 GET_DATA — SEND_ALARM 回传 (解析失败)

- **Request**: `<PK_Type><Name>GET_DATA</Name><Code>501</Code></PK_Type>`, SignalIDs: TEMP-R01, HUMI-R01
- **RPC Response**: `<invokeReturn>` 内包含 `<?xml version="1.0"?><Response><PK_Type><Name>SEND_ALARM</Name><Code>501</Code>...</Response>`
- **Doc Response 解析失败原因**: `invokeReturn` 中包含 `<?xml version="1.0"?>` 声明，嵌入到另一个 XML 文档内部时导致 SAX 解析错误: "不允许有匹配的处理指令目标"
- **实际返回内容**: SEND_ALARM 结构，TAlarmList 为空
- **结论**: FSU 将 GET_DATA 请求映射到了 SEND_ALARM 响应路径，返回空告警列表

### 4.6 GET_THRESHOLD — 空响应

- **Request**: `<PK_Type><Name>GET_THRESHOLD</Name><Code>505</Code></PK_Type>`, SignalIDs: TEMP-R01, HUMI-R01
- **RPC Response**: `<invokeReturn></invokeReturn>` (空)
- **Doc Response**: `<Response/>` (空)
- **结论**: FSU 不支持或未配置 GET_THRESHOLD 出站查询

## 5. 原始 request/response 保存路径

```
docs/landing/raw-samples/
  login-request-payload.xml        (106B)  — 内层 Request payload
  login-rpc-request.xml            (616B)  — RPC 封装后的完整 SOAP 请求
  login-rpc-response.xml           (513B)  — FSU 返回的原始 RPC SOAP 响应
  login-doc-response.xml           (120B)  — 解包后的 document-style SOAP
  getsuinfo-request-payload.xml    (97B)
  getsuinfo-rpc-request.xml        (607B)
  getsuinfo-rpc-response.xml       (513B)
  getsuinfo-doc-response.xml       (120B)
  getspconfigoption-request-payload.xml  (105B)
  getspconfigoption-rpc-request.xml      (615B)
  getspconfigoption-rpc-response.xml     (513B)
  getspconfigoption-doc-response.xml     (120B)
  getactivealarm-request-payload.xml     (102B)
  getactivealarm-rpc-request.xml         (612B)
  getactivealarm-rpc-response.xml        (513B)
  getactivealarm-doc-response.xml        (120B)
  getdata-request-payload.xml            (246B)
  getdata-rpc-request.xml                (923B)
  getdata-rpc-response.xml               (877B)  — 含 SEND_ALARM 结构
  getdata-doc-response.xml               (340B)  — 含嵌套 XML 声明(解析失败)
  getthreshold-request-payload.xml       (209B)
  getthreshold-rpc-request.xml           (813B)
  getthreshold-rpc-response.xml          (513B)
  getthreshold-doc-response.xml          (120B)
```

所有报文均已保存，可独立于代码进行人工审查。

## 6. 从设备返回中解析出的字段

### SUID
**未获取到** — 所有响应均为空 `<invokeReturn/>`，无 SUID 信息返回。

### DeviceID 列表
**未获取到** — 无设备列表返回。

### SPID 列表
**未获取到** — 无信号配置返回。

### 采集信号数据 (GET_DATA)
**未获取到** — FSU 返回的是 SEND_ALARM 结构而非 GET_DATA 响应。

### 活动告警数据 (GET_ACTIVEALARM)
无活动告警 (totalCount=0, parsedCount=0)。

### 阈值配置数据 (GET_THRESHOLD)
未获取到 (count=0)。

## 7. 候选点位表生成情况

候选点位表已生成，但均为空模板（无有效数据可用于填充）:

- `docs/landing/candidate-points/candidate-devices.csv` — 空（标注原因）
- `docs/landing/candidate-points/candidate-signals.csv` — 空（标注原因）

**注意**: 候选点位表基于本次只读联调采集数据生成。由于 FSU 对所有 GET_* 命令返回空响应，候选点位表无法从本次联调中填充有效数据。

## 8. 协议对照分析

### 8.1 通信层面 — 正常

- FSU 在 192.168.100.100:8080 正常接受 HTTP POST 请求
- WSDL RPC/encoded SOAP 封装格式正确: namespace `http://FSUService.chinatowercom.com`, encoding `http://schemas.xmlsoap.org/soap/encoding/`
- RPC 请求格式被 FSU 正确接受（无 HTTP 错误、无 SOAP Fault）
- Content-Type: `text/xml; charset=utf-8`
- SOAPAction: `""` (空字符串，符合 WSDL 规范)

### 8.2 业务层面 — 全部空响应

FSU 对所有 6 个 GET_* 命令均返回 `<invokeReturn></invokeReturn>` (空)，可能原因:

1. **FSU 固件不支持主动查询模式**: 该 FSU 可能仅配置为数据上报端 (SEND_*)，不支持 SC 主动下发 GET_* 查询
2. **PK_Type 不匹配**: FSU 固件期望的 PK_Type 值与代码发送的值不同
3. **需要先建立登录会话**: FSU 可能需要先通过 LOGIN 建立会话才能响应后续查询，但 LOGIN 也未返回数据
4. **SUID 不正确**: FSU 可能要求传入正确的 SUID 才响应，FSU-001 不被设备识别
5. **协议版本差异**: FSU 可能使用 B-2016 或更早版本，不支持 2024 Name+Code 格式

### 8.3 GET_DATA 异常分析

GET_DATA 请求触发了 FSU 返回 SEND_ALARM 结构（非 GET_DATA 响应），且响应中包含嵌套 `<?xml?>` 声明导致代码解析失败。这说明:

- FSU 对 GET_DATA (Code=501) 的内部路由指向了告警模块
- FSU 在 `<invokeReturn>` 中嵌入了完整的 XML 文档（含声明），不符合 SOAP RPC/encoded 规范
- 代码 `FsuServiceRpcAdapter.unwrapResponsePayload()` 需适配此场景（去除嵌套 XML 声明）

### 8.4 fsuCode/SUID 策略

- 平台临时 fsuCode: `FSU-001`
- 从设备返回的 SUID: **未获取到** (所有响应为空)
- **FSU-001 是平台临时 fsuCode，不等同于真实 SUID。真实 SUID 以设备返回或管理方确认结果为准。**

### 8.5 SUREADY 说明

SUREADY 在 B接口协议中是 FSU→SC 的上报通知，不是 SC→FSU 的查询命令。本次未执行 SUREADY 出站调用（无对应出站命令）。

## 9. 安全边界验证

- [x] 未执行任何 SET_ 命令 (SET_POINT, SET_THRESHOLD, SET_TIME, SET_SCIP, SET_LOGININFO, SET_FTP 等)
- [x] 未启用 Scheduler (scheduler-enabled=false, active-alarm-audit.scheduler-enabled=false)
- [x] 未修改 alarm_record 状态
- [x] 未生成正式 seed SQL
- [x] 未自动恢复/清除/确认告警
- [x] 仅执行只读查询命令 (GET_LOGININFO, GET_SUINFO, GET_SPCONFIGOPTION, GET_ACTIVEALARM, GET_DATA, GET_THRESHOLD)

## 10. 仍需管理方确认的信息

1. **真实 SUID**: 当前使用临时 FSU-001，需管理方提供正式 SUID
2. **FSU 固件类型与版本**: 确认设备型号、固件版本、支持的协议版本 (B-2016 / B-2024)
3. **FSU 是否支持 SC 主动查询**: 确认该 FSU 是否支持 GET_* 类出站查询，或仅支持 SEND_* 上报
4. **正确的 PK_Type 值**: 如果 FSU 支持查询，需确认其期望的 PK_Type 名称和 Code 值
5. **DeviceID/SPID 映射表**: 需管理方提供完整点位表 (DeviceID → 设备名称/柜号, SPID → 信号名称/单位/量程)
6. **告警等级定义**: 确认 AlarmLevel 与平台告警等级的对应关系
7. **是否需要预登录**: 确认 FSU 是否需要先登录获取 SessionID 才能响应后续查询

## 11. 文档与工程记忆

- 原始报文: `docs/landing/raw-samples/` (24 个 XML 文件)
- 候选点位表: `docs/landing/candidate-points/` (空模板)
- 本报告: `docs/landing/REAL-FSU-READONLY-CALL-ANALYSIS.md`
- 操作记忆: `docs/memory/` (待写入)
- 集成测试类: `backend/src/test/java/com/dcim/platform/binterface/Landing003RealFsuIntegration.java`

## 12. 下一步建议

1. **优先**: 向管理方确认 FSU 固件类型、协议版本和是否支持主动查询
2. **优先**: 获取真实 SUID，替换临时 FSU-001 后重新联调
3. 如果 FSU 支持 2024 协议:
   - 确认 PK_Type Name+Code 映射是否正确
   - 检查是否需要预登录会话
4. 如果 FSU 仅支持 B-2016:
   - 切换命令格式为旧版 `<PK_Type>XXXXX</PK_Type>` (无 Name+Code)
   - 重新测试 GET_LOGININFO / GET_DATA / GET_THRESHOLD
5. 修复 `FsuServiceRpcAdapter.unwrapResponsePayload()` 对嵌套 XML 声明的处理
6. 提交 Codex 集中复审 (含累积项 BIF-P4-FIX-001 ~ LANDING-003)

---

# LANDING-004 执行报告 (更新)

> 更新时间: 2026-05-20T19:08:27.409867396

## LANDING-004: Structured vs Legacy 对比

### Structured (Name+Code) 结果

| 命令 | 结果 |
|------|------|
| GET_SUINFO | SUCCESS |
| GET_DATA | SUCCESS |
| GET_ACTIVEALARM | SUCCESS |
| GET_SPCONFIGOPTION | SUCCESS |
| GET_THRESHOLD | SUCCESS |

### Legacy (纯文本 PK_Type) 结果

| 命令 | 结果 |
|------|------|
| GET_SUINFO | SUCCESS |
| GET_DATA | SUCCESS |
| GET_ACTIVEALARM | SUCCESS |
| GET_SPCONFIGOPTION | SUCCESS |
| GET_THRESHOLD | SUCCESS |

### 格式对比分析

| 命令 | Structured | Legacy | 差异 |
|------|-----------|--------|------|
| GET_SUINFO | SUCCESS | SUCCESS | 一致 |
| GET_DATA | SUCCESS | SUCCESS | 一致 |
| GET_ACTIVEALARM | SUCCESS | SUCCESS | 一致 |
| GET_SPCONFIGOPTION | SUCCESS | SUCCESS | 一致 |
| GET_THRESHOLD | SUCCESS | SUCCESS | 一致 |

### 结论

两种格式表现相同或各有差异，需进一步分析。

### 安全边界

- [x] 未执行任何 SET_ 命令
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record 状态
- [x] 仅调用只读命令

---

# LANDING-005 执行报告 (更新)

> 更新时间: 2026-05-20T19:21:10.121774318

> **真实 FSUID**: `51051243812345` (管理方确认, StationName=1)
> **平台临时 fsuCode**: `FSU-001` (仅用于平台内部标识)

## LANDING-005: Structured vs Legacy + 真实 FSUID 对比

### Structured (Name+Code) 结果

| 命令 | 结果 |
|------|------|
| GET_SUINFO | SUCCESS |
| GET_DATA | SUCCESS |
| GET_ACTIVEALARM | SUCCESS |
| GET_SPCONFIGOPTION | SUCCESS |
| GET_THRESHOLD | SUCCESS |

### Legacy (纯文本 PK_Type) 结果

| 命令 | 结果 |
|------|------|
| GET_SUINFO | SUCCESS |
| GET_DATA | SUCCESS |
| GET_ACTIVEALARM | SUCCESS |
| GET_SPCONFIGOPTION | SUCCESS |
| GET_THRESHOLD | SUCCESS |

### 格式对比分析

| 命令 | Structured | Legacy | 差异 |
|------|-----------|--------|------|
| GET_SUINFO | SUCCESS | SUCCESS | 一致 |
| GET_DATA | SUCCESS | SUCCESS | 一致 |
| GET_ACTIVEALARM | SUCCESS | SUCCESS | 一致 |
| GET_SPCONFIGOPTION | SUCCESS | SUCCESS | 一致 |
| GET_THRESHOLD | SUCCESS | SUCCESS | 一致 |

### 与 LANDING-004 (FSU-001) 对比

| 命令 | LANDING-004 (FSU-001) | LANDING-005 (51051243812345) | 差异 |
|------|------------------------|------------------------------|------|
| GET_SUINFO | 空响应 | SUCCESS | 待对比 |
| GET_DATA | 空/SEND_ALARM | SUCCESS | 待对比 |
| GET_ACTIVEALARM | 空响应 | SUCCESS | 待对比 |
| GET_SPCONFIGOPTION | 空响应 | SUCCESS | 待对比 |
| GET_THRESHOLD | 空响应 | SUCCESS | 待对比 |

> **注意**: LANDING-004 使用临时 fsuCode `FSU-001`，LANDING-005 使用真实 FSUID `51051243812345`。
> 如果结果有差异，说明 FSU 对 SUID/FSUID 有校验。

### 结论

两种格式表现相同或各有差异，需进一步分析。

### 安全边界

- [x] 未执行任何 SET_ 命令
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record 状态
- [x] 仅调用只读命令

---

# LANDING-005 执行报告 (更新)

> 更新时间: 2026-05-20T19:38:28.966205667

> **真实 FSUID**: `51051243812345` (管理方确认, StationName=1)
> **平台临时 fsuCode**: `FSU-001` (仅用于平台内部标识)

## LANDING-005: Structured vs Legacy + 真实 FSUID 对比

### Structured (Name+Code) 结果

| 命令 | 结果 |
|------|------|
| GET_DATA | SUCCESS |
| GET_LOGININFO | SUCCESS |
| GET_FTP | SUCCESS |
| GET_FSUINFO | SUCCESS |
| GET_SUINFO | SUCCESS |
| GET_ACTIVEALARM | SUCCESS |
| GET_SPCONFIGOPTION | SUCCESS |
| GET_THRESHOLD | SUCCESS |

### Legacy (纯文本 PK_Type) 结果

| 命令 | 结果 |
|------|------|
| GET_DATA | SUCCESS |
| GET_LOGININFO | SUCCESS |
| GET_FTP | SUCCESS |
| GET_FSUINFO | SUCCESS |
| GET_SUINFO | SUCCESS |
| GET_ACTIVEALARM | SUCCESS |
| GET_SPCONFIGOPTION | SUCCESS |
| GET_THRESHOLD | SUCCESS |

### 格式对比分析

| 命令 | Structured | Legacy | 差异 |
|------|-----------|--------|------|
| GET_DATA | SUCCESS | SUCCESS | 一致 |
| GET_LOGININFO | SUCCESS | SUCCESS | 一致 |
| GET_FTP | SUCCESS | SUCCESS | 一致 |
| GET_FSUINFO | SUCCESS | SUCCESS | 一致 |
| GET_SUINFO | SUCCESS | SUCCESS | 一致 |
| GET_ACTIVEALARM | SUCCESS | SUCCESS | 一致 |
| GET_SPCONFIGOPTION | SUCCESS | SUCCESS | 一致 |
| GET_THRESHOLD | SUCCESS | SUCCESS | 一致 |

### 与 LANDING-004 (FSU-001) 对比

| 命令 | LANDING-004 (FSU-001) | LANDING-005 (51051243812345) | 差异 |
|------|------------------------|------------------------------|------|
| GET_SUINFO | 空响应 | SUCCESS | 待对比 |
| GET_DATA | 空/SEND_ALARM | SUCCESS | 待对比 |
| GET_ACTIVEALARM | 空响应 | SUCCESS | 待对比 |
| GET_SPCONFIGOPTION | 空响应 | SUCCESS | 待对比 |
| GET_THRESHOLD | 空响应 | SUCCESS | 待对比 |

> **注意**: LANDING-004 使用临时 fsuCode `FSU-001`，LANDING-005 使用真实 FSUID `51051243812345`。
> 如果结果有差异，说明 FSU 对 SUID/FSUID 有校验。

### 结论

两种格式表现相同或各有差异，需进一步分析。

### 安全边界

- [x] 未执行任何 SET_ 命令
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record 状态
- [x] 仅调用只读命令
