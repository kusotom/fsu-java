# LANDING-007: 基于 B接口2016 DeviceID 的 GET_DATA 真实点位查询

## 审计日期
2026-05-20

## 审计类型
协议结构核对 + 真实设备 GET_DATA 只读查询

## 1. 2016 GET_DATA 协议结构核对

**来源**: B接口2016 协议附录 E.13 + 真实维谛 eStoneII FSU 探测日志

### 请求结构

```xml
<Request>
  <PK_Type>
    <Name>GET_DATA</Name>
    <Code>401</Code>          <!-- 2016 Code: GET_DATA=401 -->
  </PK_Type>
  <Info>
    <FsuId>51051243812345</FsuId>
    <FsuCode>51051243812345</FsuCode>
    <DeviceList>
      <Device Id="51051241820004" Code="51051241820004">
        <!-- 可选: TSemaphore Id="..." Code="..."/ -->
      </Device>
    </DeviceList>
  </Info>
</Request>
```

关键约定:
- PK_Type 使用 Name+Code 格式, Code=401 (2016 GET_DATA)
- Info 包含 FsuId + FsuCode (两者通常相同)
- DeviceList 在 Info 内, 不在 xmlData 内
- Device 有 Id 和 Code 属性
- TSemaphore 可选, 每个指定一个信号; 不指定则查询设备全部信号
- DeviceID=全9 表示通配所有设备
- 不支持空 DeviceList (会导致 FSU 报 ParseXmlData 错误)

### 响应结构

```xml
<Response>
  <PK_Type><Name>GET_DATA_ACK</Name><Code>402</Code></PK_Type>
  <Info>
    <FsuId>...</FsuId>
    <Result>1</Result>        <!-- 1=成功, 0=失败 -->
    <Values>
      <DeviceList>
        <!-- Device/TSemaphore 含 MeasuredVal 等 -->
      </DeviceList>
    </Values>
  </Info>
</Response>
```

## 2. 使用的真实 DeviceID

从 LANDING-006 GET_LOGININFO (Code=1501) 返回获取:

| # | DeviceID |
|---|----------|
| 1 | 51051241820004 |
| 2 | 51051241830004 |
| 3 | 51051241840004 |
| 4 | 51051240700002 |

TSemaphore (SignalID) 候选值从 Python 探测日志获取。

## 3. 实际请求策略

| Test | 策略 | DeviceID | TSemaphore |
|------|------|----------|------------|
| 1 | DeviceList 仅设备 | 4 个真实 ID | 无 (查全部) |
| 2 | DeviceList + 信号 | 4 个真实 ID | 已知 TSemaphore ID |
| 3 | 单设备查询 | 51051241820004 | 无 (查全部) |
| 4 | DeviceID 通配 | 999999999999 | 无 |

## 4. 每个 DeviceID 返回结果

### 结果汇总

| Test | HTTP | PK_Type | Code | Result | DeviceList |
|------|------|---------|------|--------|------------|
| DeviceList (all) | 200 | GET_DATA_ACK | 402 | 1 (成功) | **空** |
| +TSemaphore | 200 | GET_DATA_ACK | 402 | 1 (成功) | **空** |
| Single device | 200 | GET_DATA_ACK | 402 | 1 (成功) | **空** |
| 全9 通配 | 200 | GET_DATA_ACK | 402 | 1 (成功) | **空** |

### 详细分析

```
RPC response: 1034 bytes (一致)
Doc response: 410 bytes (一致)
Response pattern: 所有 4 次测试返回结构完全相同
  - GET_DATA_ACK Code=402 ✓
  - FsuId/FsuCode 回显正确 ✓
  - Result=1 (请求成功) ✓
  - Values/DeviceList 为空 — 无测量数据
```

## 5. 解析出的真实 SPID / SignalID / Value

| 字段 | 状态 |
|------|------|
| SPID | **未获取到** — 所有响应 DeviceList 为空 |
| SignalID | **未获取到** |
| MeasuredVal | **未获取到** |
| Status | **未获取到** |
| Time | **未获取到** |

## 6. 结论

1. **协议格式正确**: GET_DATA (Code=401) 被正确路由到 GET_DATA_ACK (Code=402), Result=1 表示 FSU 接受请求
2. **DeviceID 有效**: FSU 回显 FsuId/FsuCode, 未报错或返回 Fault
3. **当前无测量数据**: 所有 4 种策略 (全设备/指定信号/单设备/通配) 均返回空 DeviceList, FSU 当前无任何监控点位数据
4. **与 Python 探测一致**: 2026-04-27 Python 探测日志中 GET_DATA 同样返回空 DeviceList, 说明此状态已持续
5. **管理方信息**: 需管理方提供 SPID/SignalID 完整点位表, 并确认设备是否已配置实际监控点位

## 7. 候选点位表生成情况

未能生成 — FSU 当前返回空 DeviceList, 无法提取任何 SPID 或测量数据。

## 8. 安全边界

- [x] 仅执行 GET_DATA (Code=401) 只读查询
- [x] 未执行任何 SET_ 命令
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record
- [x] 未生成正式 seed SQL
- [x] 默认 mvn test 不访问真实 FSU

## 9. 原始报文

16 个 XML 文件: `docs/landing/raw-samples/landing007-*`
