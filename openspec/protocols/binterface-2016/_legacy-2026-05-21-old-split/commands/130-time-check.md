# TIME_CHECK — 时间同步校验

## 1. 协议定位

- 命令名称：TIME_CHECK
- ACK 名称：TIME_CHECK_ACK
- 方向：SC→FSU
- 发起方：SC（监控中心）
- 接收方：FSU（现场监控单元）
- 功能说明：SC 向 FSU 发送标准时间，FSU 返回自身系统时间。仅做时间比对，不修改 FSU 时间。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 1301 |
| 响应 | 1302 |

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 编码 | `request.info.FSUCode` | FSUCode |
| SessionID | String | 协议标准 | 会话 ID | `request.info.SessionID` | Not used in TimeCheckService |
| RequestTime | DateTime | 协议标准 | 请求发送时间 | `request.info.RequestTime` | Not used |
| StandardTime | DateTime | 是 | SC 端标准时间（ISO8601 格式） | `request.info.StandardTime` | StandardTime |

注: 请求无 xmlData。

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `response.info.ResultCode` | ResultCode |
| FSUTime | DateTime | 否 | FSU 端系统时间（成功时返回） | `response.info.FSUTime` | FSUTime |

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request xmlns="http://FSUService.chinatowercom.com">
      <PK_Type>TIME_CHECK</PK_Type>
      <Info>
        <FSUCode>51051243812345</FSUCode>
        <SessionID>SESSION-20260520120000</SessionID>
        <StandardTime>2026-05-20 12:00:00</StandardTime>
      </Info>
    </Request>
  </soap:Body>
</soap:Envelope>
```

### 响应样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response xmlns="http://FSUService.chinatowercom.com">
      <PK_Type>TIME_CHECK_ACK</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <FSUTime>2026-05-20 12:00:05</FSUTime>
      </Info>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- 本命令不引用复杂数据结构，仅使用基本 DateTime 类型

## 7. 成功/失败判断

- 成功: ResultCode=0，且 Info 包含 FSUTime
- 失败: ResultCode ≠ 0
  - 2001: 参数错误（缺少 FSUCode 或 Info 为空）
  - 2003: 缺少 StandardTime 或响应缺少 FSUTime
  - 1002: FSU 未登录或已离线
  - 5001: 服务器内部错误

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Controller | `ScServiceController.java` | ✅ | 间接调用 |
| Handler | `TimeCheckCommandHandler.java` | ✅ 已实现 | — |
| Service | `TimeCheckService.java` | ✅ 已实现 | — |
| Entity | `TimeCheckResult.java` | ✅ | — |
| Test | `TimeCheckServiceTest.java` | ✅ | — |
| Test | `TimeCheckCommandHandlerTest.java` | ✅ | — |

## 9. 真实 FSU 实测差异

- 未在真实 FSU 上执行 TIME_CHECK（LANDING-006 仅验证了 GET_LOGININFO、GET_FSUINFO、GET_FTP）
- 协议标准 StandardTime 为必填，FSU 返回 FSUTime 用于比对

## 10. 验收标准

1. 请求 XML 符合 B接口 2016 格式要求，包含 StandardTime
2. 响应成功时包含 ResultCode=0 和 FSUTime
3. StandardTime 为空时返回 2003
4. FSU 未登录时返回 1002
5. BInterfaceCommand2016 需增加 TIME_CHECK=1301 条目

## 11. 安全边界

- 只读查询，不修改 FSU 时间
- 不修改系统时间
- 不做持久化
- StandardTime 用于比对，不做设置
