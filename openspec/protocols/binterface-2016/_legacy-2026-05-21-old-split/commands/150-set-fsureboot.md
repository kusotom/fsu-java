# SET_FSUREBOOT — 远程重启 FSU

## 1. 协议定位

- 命令名称：SET_FSUREBOOT
- ACK 名称：SET_FSUREBOOT_ACK
- 方向：SC→FSU
- 发起方：SC（监控中心）
- 接收方：FSU（现场监控单元）
- 功能说明：SC 远程重启 FSU 设备。高风险操作，安全禁用。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 1801 |
| 响应 | 1802 |

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 编码 | `request.info.FSUCode` | ❌ 未实现 |
| SessionID | String | 是 | 会话 ID | `request.info.SessionID` | ❌ 未实现 |

注: 协议原文 xmlData 结构待确认。

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `response.info.ResultCode` | ❌ 未实现 |

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request xmlns="http://FSUService.chinatowercom.com">
      <PK_Type>SET_FSUREBOOT</PK_Type>
      <Info>
        <FSUCode>51051243812345</FSUCode>
        <SessionID>SESSION-20260520120000</SessionID>
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
      <PK_Type>SET_FSUREBOOT_ACK</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
      </Info>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- 本命令不引用复杂数据结构。

## 7. 成功/失败判断

- 成功: ResultCode=0
- 失败: ResultCode ≠ 0

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Controller | — | ❌ 未实现 | — |
| Handler | — | ❌ 未实现 | 安全禁用 |
| Service | — | ❌ 未实现 | 安全禁用 |
| Entity | — | ❌ 未实现 | — |
| Repository | — | ❌ 未实现 | — |
| Test | — | ❌ 未实现 | — |

## 9. 真实 FSU 实测差异

- 未在真实 FSU 上测试（安全禁用，禁止执行）
- 协议原文 xmlData 结构待确认

## 10. 验收标准

1. 协议原文 xmlData 结构和 Info 字段需先确认
2. 实现前必须完成安全门禁设计
3. 需多层确认机制（角色权限+二次确认+审计日志）
4. SET_FSUREBOOT 属于高风险操作，需独立评审

## 11. 安全边界

- 禁止默认执行: `safe_enabled=false`（强制关闭）
- 需安全门禁+confirmationToken
- 需多层确认: 角色权限 + 二次确认 + 审计日志
- 高风险操作，`SET_FSUREBOOT` 枚举已定义但处于安全禁用状态
- 实现前需完成独立安全评审
- 不可在生产环境无保护执行
