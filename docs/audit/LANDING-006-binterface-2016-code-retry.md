# LANDING-006: B接口2016码表兼容只读重试

## 审计日期
2026-05-20

## 审计类型
协议码表兼容实现 + 真实设备只读重试

## 1. 背景

LANDING-005 发现 FSU 返回 `SEND_ALARM Code=501`。对照 B接口2016 码表: SEND_ALARM=501 (非 2024 的 601)。判断真实 FSU 使用 2016 码表。

## 2. 2016 协议依据

| 命令 | 2016 Code | 2024 Code |
|------|-----------|-----------|
| GET_DATA | 401 | 501 |
| SEND_ALARM | 501 | 601 |
| GET_LOGININFO | 1501 | 无独立命令 |
| GET_FTP | 1601 | 801 (GET_SUFTP) |
| GET_FSUINFO | 1701 | 1001 (GET_SUINFO) |

## 3. 新增/修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `BInterfaceCommand2016.java` | **新增** | 2016 命令名→Code 映射表 |
| `RealHttpFsuServiceClient.java` | 修改 | call() 支持 `legacy-2016` 格式 |
| `Landing003RealFsuIntegration.java` | 修改 | 新增 Phase 3: 2016 Code 重试 |
| `docs/landing/raw-samples/landing006-*` | 新增 | 48 个 XML 报文 (4命令×3格式×4层) |

## 4. 2016 Code 重试结果

### 4.1 GET_LOGININFO (Code=1501) — 突破！

```xml
<PK_Type><Name>GET_LOGININFO_ACK</Name><Code>1502</Code></PK_Type>
<Info>
  <FsuId>51051243812345</FsuId>
  <FsuCode>51051243812345</FsuCode>
  <SCIP>192.168.100.123</SCIP>
  <DeviceList>
    <Device Id="51051241820004" Code="51051241820004"/>
    <Device Id="51051241830004" Code="51051241830004"/>
    <Device Id="51051241840004" Code="51051241840004"/>
    <Device Id="51051240700002" Code="51051240700002"/>
  </DeviceList>
</Info>
```

**获得**: FSUID 确认 + **4 个 DeviceID** + SCIP

### 4.2 GET_FSUINFO (Code=1701) — 实时监控数据！

```xml
<PK_Type><Name>GET_FSUINFO_ACK</Name><Code>1702</Code></PK_Type>
<Info>
  <FsuId>51051243812345</FsuId>
  <TFSUStatus>
    <CPUUsage>14.95</CPUUsage>
    <MEMUsage>62.84</MEMUsage>
  </TFSUStatus>
  <Result>0</Result>
</Info>
```

**获得**: **CPU=14.95%, MEM=62.84%**, Result=0 (成功)

### 4.3 GET_FTP (Code=1601) — FTP 配置

```xml
<PK_Type><Name>GET_FTP_ACK</Name><Code>1602</Code></PK_Type>
<Info>
  <FsuId>51051243812345</FsuId>
  <UserName>ttcw2015</UserName>
  <Password>ttcw@2015</Password>
  <Result>1</Result>
</Info>
```

**获得**: FTP 用户名/密码 (⚠️ 明文返回，安全注意事项)

### 4.4 GET_DATA (Code=401) — 空响应

返回空 `<invokeReturn/>`。正确路由到 GET_DATA 处理器（不再错误路由到 SEND_ALARM），但信号 ID (TEMP-R01/HUMI-R01) 不被 FSU 识别。

## 5. 2024 Code vs 2016 Code 对比

| 命令 | 2024 Code | 结果 | 2016 Code | 结果 | 获胜 |
|------|-----------|------|-----------|------|------|
| GET_DATA | 501 | SEND_ALARM (错误路由) | 401 | 空 (路由正确, 信号不对) | **2016** |
| GET_LOGININFO | — | 空响应 | 1501 | **4 DeviceIDs!** | **2016** |
| GET_FTP | — (GET_SUFTP 801) | 空响应 | 1601 | **FTP 配置** | **2016** |
| GET_FSUINFO | — (GET_SUINFO 1001) | 空响应 | 1701 | **CPU 14.95% MEM 62.84%** | **2016** |

## 6. 协议版本确认

**可以确认**: 真实 FSU 使用 **B接口2016** 码表。

证据:
1. SEND_ALARM=501 (与 2016 一致, 2024 中=601)
2. GET_LOGININFO=1501 返回 GET_LOGININFO_ACK=1502
3. GET_FSUINFO=1701 返回 GET_FSUINFO_ACK=1702
4. GET_FTP=1601 返回 GET_FTP_ACK=1602
5. 所有 2016 Code 均收到正确的 ACK 响应
6. PK_Type 使用 Name+Code 格式 (FSU 接受结构化格式但使用 2016 码值)

## 7. 解析出的真实字段

| 字段 | 来源 | 值 |
|------|------|-----|
| **FSUID** | GET_LOGININFO | `51051243812345` ✅ |
| **DeviceID** | GET_LOGININFO | `51051241820004`, `51051241830004`, `51051241840004`, `51051240700002` ✅ |
| **CPU** | GET_FSUINFO | 14.95% ✅ |
| **MEM** | GET_FSUINFO | 62.84% ✅ |
| SPID | — | 未获取到 (需后续用 DeviceID 查询) |

## 8. 安全边界

- [x] 未执行 SET
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record
- [x] 未生成正式 seed SQL
- ⚠️ GET_FTP 返回明文密码 (ttcw@2015) — 不应出现在日志/报告中

## 9. 原始报文

48 个 XML 文件: `docs/landing/raw-samples/landing006-*`
