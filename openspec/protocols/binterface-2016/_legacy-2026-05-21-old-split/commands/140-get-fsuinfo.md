# GET_FSUINFO — 查询 FSU 运行状态

## 1. 协议定位

- 命令名称：GET_FSUINFO
- ACK 名称：GET_FSUINFO_ACK
- 方向：SC→FSU
- 发起方：SC（监控中心）
- 接收方：FSU（现场监控单元）
- 功能说明：SC 向 FSU 查询运行状态（CPU 使用率、内存使用率），用于心跳轮询和健康检查。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 1701 |
| 响应 | 1702 |

注: 2024 协议中对应命令为 GET_SUINFO (Code=1001)，命名和码值均不同。

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 编码 | `request.info.FSUCode` | FSUCode (仅此字段) |
| SessionID | String | 协议标准 | 会话 ID | `request.info.SessionID` | Not used |
| RequestTime | DateTime | 协议标准 | 请求发送时间 | `request.info.RequestTime` | Not used |

注:
- 请求无 xmlData
- standard-2016 规范要求 FSUCode + SessionID + RequestTime
- Emerson 实测仅需 FSUCode，标签全大写
- 平台当前实现仅发送 FSUCode（对齐 Emerson 实测）

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `response.info.ResultCode` | ResultCode |

注: 业务数据在 xmlData 中返回（Emerson 实测在 Info 内嵌 TFSUStatus）。

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request xmlns="http://FSUService.chinatowercom.com">
      <PK_Type>GET_FSUINFO</PK_Type>
      <Info>
        <FSUCode>51051243812345</FSUCode>
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
      <PK_Type>GET_FSUINFO_ACK</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <FsuId>51051243812345</FsuId>
        <TFSUStatus>
          <CPUUsage>25.5</CPUUsage>
          <MEMUsage>60.2</MEMUsage>
        </TFSUStatus>
      </Info>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- TFSUStatus: `06-data-structures.md` §4 — CPUUsage, MEMUsage
- BInterface2016GetFsuInfoService 从 xmlData items 提取 CPUUsage/MEMUsage

## 7. 成功/失败判断

- 成功: ResultCode=0，且解析到 CPUUsage 或 MEMUsage 至少一个
- 失败: ResultCode ≠ 0
  - 2001: 参数错误（缺少 FSU Code）
  - 2003: 无有效响应数据
  - 2004: 响应缺少 CPUUsage/MEMUsage
  - 2010: ACK Code 不匹配（期望 1702）
  - 1002: FSU 未注册
  - 5001: 服务器内部错误

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Service | `BInterface2016GetFsuInfoService.java` | ✅ LANDING-015 FIX-002 对齐 | 校验 ACK Code=1702 |
| Entity | `BInterface2016GetFsuInfoResult.java` | ✅ | — |
| Entity | `BInterfaceFsuStatusEntity.java` | ✅ | 持久化 CPU/MEM |
| Repository | `BInterfaceFsuStatusRepository.java` | ✅ | — |
| Repository | `FsuDeviceRepository.java` | ✅ | FSU 注册查询 |
| Test | `BInterface2016GetFsuInfoServiceTest.java` | ✅ | LANDING-015 FIX-002 |

注: 当前项目没有独立的 GET_FSUINFO CommandHandler。此命令由 `BInterface2016GetFsuInfoService` 直接通过定时任务调用，不经过 SC Controller/CommandHandler 链路。

## 9. 真实 FSU 实测差异

- 设备类型: Emerson eStoneII FSU-2808IM (FSUID=51051243812345, IP=192.168.100.100)
- LANDING-006 验证: 通过 ✅
- LANDING-015 FIX-002: standard-2016 vs emerson-2016 差异已对齐:
  - standard-2016: Info 含 FSUCode + SessionID + RequestTime，xmlData 结构待确认
  - emerson-2016: Info 仅需 FSUCode，响应中 Info 包含 FsuId+TFSUStatus(CPUUsage,MEMUsage)+Result
  - 平台当前实现已对齐 emerson-2016 格式
- 注意: 标准-2016 手册中 xmlData 结构标为"待确认"

## 10. 验收标准

1. 请求仅发送 FSUCode（对齐实测）
2. 响应成功时包含 CPUUsage/MEMUsage
3. 校验 ACK Code=1702，不匹配时返回 2010
4. 成功后更新 b_interface_fsu_status.last_heartbeat 和 online_status
5. BInterfaceCommand2016 需增加 GET_FSUINFO=1701 条目

## 11. 安全边界

- 只读查询，不修改 FSU 运行状态
- CPUUsage/MEMUsage 为性能数据，无明显安全风险
- 更新 FSU 状态失败不影响主流程（best-effort）
