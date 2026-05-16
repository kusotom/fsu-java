# B接口协议 2016 技术手册

> 协议基准：《中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0，2016 年 9 月》
>
> 本文档基于协议原文、WSDL 定义、项目 Fixtures 和现有实现综合整理，作为 fsu-platform-java 的 B-Interface 协议参考手册。

---

## 目录

1. [协议概述](#1-协议概述)
2. [通信架构](#2-通信架构)
3. [WSDL 定义](#3-wsdl-定义)
4. [报文结构](#4-报文结构)
5. [命令码全集](#5-命令码全集)
6. [命令详细定义](#6-命令详细定义)
   - 6.1 [LOGIN — 登录/注册](#61-login--登录注册)
   - 6.2 [HEARTBEAT — 心跳上报](#62-heartbeat--心跳上报)
   - 6.3 [SEND_DATA — 实时数据上报](#63-send_data--实时数据上报)
   - 6.4 [SEND_ALARM — 告警上报](#64-send_alarm--告警上报)
   - 6.5 [GET_DATA — 获取监控数据](#65-get_data--获取监控数据)
   - 6.6 [SET_POINT — 遥控遥调](#66-set_point--遥控遥调)
   - 6.7 [GET_THRESHOLD — 获取告警门限](#67-get_threshold--获取告警门限)
   - 6.8 [SET_THRESHOLD — 设置告警门限](#68-set_threshold--设置告警门限)
   - 6.9 [TIME_CHECK — 时间同步](#69-time_check--时间同步)
   - 6.10 [GET_FTP — 获取FTP参数](#610-get_ftp--获取ftp参数)
   - 6.11 [SET_FTP — 设置FTP参数](#611-set_ftp--设置ftp参数)
   - 6.12 [GET_LOGININFO — 获取登录信息](#612-get_logininfo--获取登录信息)
   - 6.13 [GET_FSUINFO — 获取FSU信息](#613-get_fsuinfo--获取fsu信息)
   - 6.14 [GET_HISTORY_DATA — 获取历史数据](#614-get_history_data--获取历史数据)
   - 6.15 [SET_DATA — 设置监控数据](#615-set_data--设置监控数据)
   - 6.16 [SET_FSUREBOOT — 远程重启FSU](#616-set_fsureboot--远程重启fsu)
7. [ResultCode 定义](#7-resultcode-定义)
8. [Info 字段汇总](#8-info-字段汇总)
9. [xmlData 结构汇总](#9-xmldata-结构汇总)
10. [SOAP Fault 处理](#10-soap-fault-处理)
11. [会话管理](#11-会话管理)
12. [通道说明](#12-通道说明)
13. [Fixtures 交叉引用](#13-fixtures-交叉引用)
14. [实现状态总览](#14-实现状态总览)

---

## 1. 协议概述

### 1.1 协议定位

B接口是中国铁塔动环监控系统中 SC（Supervision Center，监控中心）与 FSU（Field Supervision Unit，现场监控单元）之间的统一互联协议。

### 1.2 协议基础

| 项目 | 内容 |
|------|------|
| 协议全称 | 中国铁塔动环监控系统统一互联 B 接口技术规范 |
| 版本 | V1.0（试行），2016 年 9 月 |
| 传输层 | HTTP |
| 协议层 | SOAP 1.1 + WSDL 1.1 |
| 编码 | XML, RPC style, soapenc:encoded |
| 数据交换 | XML（实时通道）+ FTP（文件/图片通道） |

### 1.3 协议特征

- 单一 WSDL 操作：`invoke(xmlData) → invokeReturn`
- 命令码（PK_Type）在 XML 内部承载，不依赖 WSDL 操作区分
- 快数据（FSU→SC）和慢数据（SC→FSU）使用相同的消息结构，但服务角色互换
- xmlData 承载各命令独有业务数据，随 PK_Type 动态变化

---

## 2. 通信架构

### 2.1 双通道模型

```
┌─────────────────────────────────────────────────────────┐
│                      SC (监控中心)                        │
│                                                         │
│  ┌──────────────────┐        ┌──────────────────┐       │
│  │  SCService 服务端  │        │  FSUService 客户端 │       │
│  │  (接收FSU上报)    │        │  (轮询FSU数据)    │       │
│  └────────┬─────────┘        └────────┬─────────┘       │
│           │                           │                  │
└───────────┼───────────────────────────┼──────────────────┘
            │  ▲                   │  ▲
            │  │ 快数据通道         │  │ 慢数据通道
            ▼  │                   ▼  │
┌──────────────────────────────────────────┐
│                FSU (现场监控单元)           │
│                                          │
│  ┌──────────────────┐  ┌──────────────┐  │
│  │  SCService 客户端  │  │ FSUService   │  │
│  │  (主动上报)       │  │ 服务端(响应) │  │
│  └──────────────────┘  └──────────────┘  │
└──────────────────────────────────────────┘
```

### 2.2 快数据通道（FSU → SC，主动上报）

| 方向 | 角色 | 服务 | 说明 |
|------|------|------|------|
| FSU → SC | FSU = 客户端, SC = 服务端 | SCService | FSU 主动上报登录、心跳、告警、实时数据 |
| SC | 服务端提供者 | `ScServiceController` | 接收 SOAP/XML 并处理 |

**上报命令**：LOGIN, HEARTBEAT, SEND_DATA, SEND_ALARM

### 2.3 慢数据通道（SC → FSU，SC 轮询）

| 方向 | 角色 | 服务 | 说明 |
|------|------|------|------|
| SC → FSU | SC = 客户端, FSU = 服务端 | FSUService | SC 主动查询/设置 FSU 参数 |
| SC | 客户端调用者 | `FsuServiceClient` | 向 FSU 发送 SOAP/XML 请求 |

**轮询命令**：GET_DATA, SET_POINT, GET_THRESHOLD, SET_THRESHOLD, TIME_CHECK, GET_FTP, SET_FTP, GET_LOGININFO, GET_FSUINFO, SET_DATA, SET_FSUREBOOT, GET_HISTORY_DATA

---

## 3. WSDL 定义

### 3.1 SCService WSDL

> 位置：`WSDL协议/SCService.wsdl`
> 命名空间：`http://SCService.chinatowercom.com`

```
服务端端点: POST /services/SCService
风格: RPC
编码: soapenc:encoded
操作: invoke(xmlData: soapenc:string) → invokeReturn: soapenc:string
```

**完整定义**：

```xml
<wsdl:definitions targetNamespace="http://SCService.chinatowercom.com"
    xmlns:impl="http://SCService.chinatowercom.com"
    xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/">

  <wsdl:message name="invokeRequest">
    <wsdl:part name="xmlData" type="soapenc:string"/>
  </wsdl:message>

  <wsdl:message name="invokeResponse">
    <wsdl:part name="invokeReturn" type="soapenc:string"/>
  </wsdl:message>

  <wsdl:portType name="SCService">
    <wsdl:operation name="invoke" parameterOrder="xmlData">
      <wsdl:input message="impl:invokeRequest" name="invokeRequest"/>
      <wsdl:output message="impl:invokeResponse" name="invokeResponse"/>
    </wsdl:operation>
  </wsdl:portType>

  <wsdl:binding name="SCServiceSoapBinding" type="impl:SCService">
    <wsdlsoap:binding style="rpc" transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="invoke">
      <wsdlsoap:operation soapAction=""/>
      <wsdl:input>
        <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
          namespace="http://SCService.chinatowercom.com" use="encoded"/>
      </wsdl:input>
      <wsdl:output>
        <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
          namespace="http://SCService.chinatowercom.com" use="encoded"/>
      </wsdl:output>
    </wsdl:operation>
  </wsdl:binding>

  <wsdl:service name="SCServiceService">
    <wsdl:port binding="impl:SCServiceSoapBinding" name="SCService">
      <wsdlsoap:address location="http://127.0.0.1:8080/services/SCService"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
```

### 3.2 FSUService WSDL

> 位置：`WSDL协议/FSUService.wsdl`
> 命名空间：`http://FSUService.chinatowercom.com`

与服务端的 WSDL 结构**完全一致**，仅命名空间和服务名称不同：

```
服务端端点: POST /services/FSUService
风格: RPC
编码: soapenc:encoded
操作: invoke(xmlData: soapenc:string) → invokeReturn: soapenc:string
```

### 3.3 WSDL 关键特征

| 特征 | 值 |
|------|-----|
| SOAP 风格 | RPC（非 Document） |
| 编码方式 | soapenc:encoded（非 literal） |
| SOAPAction | 空字符串 `""` |
| 参数 | 单一 `xmlData` 参数，类型 `soapenc:string` |
| 返回值 | 单一 `invokeReturn`，类型 `soapenc:string` |
| 端点前缀 | `/services/SCService` 和 `/services/FSUService` |

> **注意**：soapenc:encoded + RPC style 是较老的 SOAP 编码方式。xmlData 虽然是字符串类型，但实际承载的是 XML 片段。
> 这种"WSDL 不分命令"的模式意味着所有命令码解析必须在 XML 层面完成。

---

## 4. 报文结构

### 4.1 通用消息结构

```
SOAP Envelope
  └── SOAP Body
       ├── Request / Response          ← 根元素
       │   ├── PK_Type                 ← 命令码（大写枚举值）
       │   ├── Info                    ← 元数据（FSUCode/ResultCode 等）
       │   └── xmlData                 ← 业务数据（各命令独有结构）
```

### 4.2 请求报文模板

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>COMMAND_CODE</PK_Type>
      <Info>
        <FSUCode>FSU-XXX</FSUCode>
        <!-- 命令特有字段 -->
      </Info>
      <xmlData>
        <!-- 命令特有业务数据 -->
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

### 4.3 响应报文模板

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>COMMAND_CODE</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <!-- 命令特有字段 -->
      </Info>
      <xmlData>
        <!-- 命令特有返回数据 -->
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

### 4.4 命名空间

| 前缀 | 命名空间 | 用途 |
|------|---------|------|
| soap | `http://schemas.xmlsoap.org/soap/envelope/` | SOAP Envelope |
| soapenc | `http://schemas.xmlsoap.org/soap/encoding/` | SOAP 编码 |
| impl | `http://SCService.chinatowercom.com` | SC 命名空间 |
| impl | `http://FSUService.chinatowercom.com` | FSU 命名空间 |

### 4.5 消息方向标识

- **Request** — SOAP Body 根元素为 `<Request>`（FSU→SC 或 SC→FSU 的请求方向）
- **Response** — SOAP Body 根元素为 `<Response>`（对应请求的响应）

---

## 5. 命令码全集

### 5.1 枚举定义（`BInterfacePkType`）

```java
public enum BInterfacePkType {
    LOGIN,            // FSU登录/注册
    HEARTBEAT,        // FSU心跳上报
    SEND_DATA,        // FSU上报实时数据 ★ 当前枚举缺失
    SEND_ALARM,       // FSU告警上报
    GET_DATA,         // SC获取监控数据
    GET_HISTORY_DATA, // SC获取历史数据
    SET_POINT,        // SC遥控遥调
    GET_THRESHOLD,    // SC获取告警门限
    SET_THRESHOLD,    // SC设置告警门限
    TIME_CHECK,       // SC时间同步
    GET_FTP,          // SC获取FTP参数
    SET_FTP,          // SC设置FTP参数
    GET_LOGININFO,    // SC获取登录信息
    GET_FSUINFO,      // SC获取FSU信息
    SET_DATA,         // SC设置监控数据
    SET_FSUREBOOT,    // SC远程重启FSU（安全禁用）
    UNKNOWN           // 未知命令码
}
```

> **已知缺陷**：SEND_DATA 在枚举中缺失。`set_ftp.request.xml` 使用 `SET_FTP`，但枚举中未包含 SET_FTP。
> 参见：`BIF-P0-001` 任务。

### 5.2 命令码分类汇总

| 类别 | 命令 | 方向 | 优先级 |
|------|------|------|--------|
| **连接管理** | LOGIN, HEARTBEAT | FSU→SC | P0 |
| **实时数据** | SEND_DATA, GET_DATA | 双向 | P0 |
| **告警** | SEND_ALARM | FSU→SC | P0 |
| **参数设置** | SET_POINT, SET_DATA, SET_THRESHOLD | SC→FSU | P1 |
| **参数查询** | GET_THRESHOLD, GET_FTP, GET_LOGININFO | SC→FSU | P1 |
| **FTP** | SET_FTP | SC→FSU | P1 |
| **时间同步** | TIME_CHECK | SC→FSU | P2 |
| **历史数据** | GET_HISTORY_DATA | SC→FSU | P2 |
| **FSU信息** | GET_FSUINFO | SC→FSU | P2 |
| **远程控制** | SET_FSUREBOOT（安全禁用） | SC→FSU | P2 |

---

## 6. 命令详细定义

### 6.1 LOGIN — 登录/注册

**方向**：FSU → SC（快数据通道）
**通道**：SCService

#### 请求 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| Password | String | 是 | 登录密码（MD5 或约定加密） |
| FSUID | String | 否 | FSU 设备唯一标识 |
| DeviceID | String | 否 | 设备 ID |
| Timestamp | DateTime | 是 | 登录时间 |
| ProtocolVersion | String | 否 | 协议版本号，如 "B-2016" |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| DeviceInfo.Manufacturer | String | 否 | 设备厂商 |
| DeviceInfo.Model | String | 否 | 设备型号 |
| DeviceInfo.FirmwareVersion | String | 否 | 固件版本 |
| DeviceInfo.HardwareVersion | String | 否 | 硬件版本 |
| DeviceInfo.MacAddr | String | 否 | MAC 地址 |
| DeviceInfo.IPAddr | String | 否 | IP 地址 |

**请求示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>LOGIN</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <Password>7a3b5c8e2f1d4a6b9c0d3e5f7a8b9c0d</Password>
        <FSUID>FSU-001-BJ-CHAOYANG</FSUID>
        <DeviceID>FSU-001-01</DeviceID>
        <Timestamp>2026-05-13T10:30:00+08:00</Timestamp>
        <ProtocolVersion>B-2016</ProtocolVersion>
      </Info>
      <xmlData>
        <DeviceInfo>
          <Manufacturer>中兴</Manufacturer>
          <Model>eStone II</Model>
          <FirmwareVersion>B07D07</FirmwareVersion>
          <HardwareVersion>HW-2.1</HardwareVersion>
          <MacAddr>AA:BB:CC:DD:EE:01</MacAddr>
          <IPAddr>192.168.1.101</IPAddr>
        </DeviceInfo>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

#### 响应 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功, 非0=失败 |
| SessionID | String | 是 | 分配的会话 ID |
| ExpireSeconds | Integer | 否 | 会话过期时间（秒） |
| ServerTime | DateTime | 否 | 服务器当前时间 |

xmlData：无（空节点）

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>LOGIN</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <ExpireSeconds>3600</ExpireSeconds>
        <ServerTime>2026-05-13T10:30:05+08:00</ServerTime>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.2 HEARTBEAT — 心跳上报

**方向**：FSU → SC（快数据通道）
**通道**：SCService

#### 请求 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 登录时分配的会话 ID |
| Timestamp | DateTime | 是 | 心跳时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| CPU | Integer | 否 | CPU 使用率（%） |
| Memory | Integer | 否 | 内存使用率（%） |
| Temperature | Integer | 否 | 设备温度（°C） |
| RunningTime | Integer | 否 | 运行时间（秒） |

**请求示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>HEARTBEAT</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <Timestamp>2026-05-13T10:31:00+08:00</Timestamp>
      </Info>
      <xmlData>
        <CPU>35</CPU>
        <Memory>62</Memory>
        <Temperature>42</Temperature>
        <RunningTime>3600</RunningTime>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

#### 响应 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| ServerTime | DateTime | 否 | 服务器当前时间 |

xmlData：无（空节点）

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>HEARTBEAT</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <ServerTime>2026-05-13T10:31:05+08:00</ServerTime>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.3 SEND_DATA — 实时数据上报

**方向**：FSU → SC（快数据通道）
**通道**：SCService
**说明**：FSU 实时上报监控点位数据，与告警不同，是正常的周期性采集数据。

#### 请求 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| CollectTime | DateTime | 是 | 数据采集时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| Signal | Array | 是 | 信号量数组 |
| Signal.SignalID | String | 是 | 信号量 ID |
| Signal.Value | String | 是 | 信号量值 |
| Signal.Quality | Integer | 是 | 数据质量（1=正常） |
| Signal.Status | String | 否 | 状态描述（NORMAL, ABNORMAL 等） |

**请求示例**（包含 5 个信号量：温度、湿度、电压、门磁、水浸）：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>SEND_DATA</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <CollectTime>2026-05-13T10:32:00+08:00</CollectTime>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>TEMP-001</SignalID>
          <Value>25.5</Value>
          <Quality>1</Quality>
          <Status>NORMAL</Status>
        </Signal>
        ...多组 Signal...
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

#### 响应 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| Count | Integer | 否 | 接收的信号量数量 |

xmlData：无（空节点）

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>SEND_DATA</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <Count>5</Count>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.4 SEND_ALARM — 告警上报

**方向**：FSU → SC（快数据通道）
**通道**：SCService

#### 请求 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| AlarmTime | DateTime | 是 | 告警发生时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| Alarm | Array | 是 | 告警数组 |
| Alarm.SignalID | String | 是 | 关联信号量 ID |
| Alarm.AlarmCode | String | 是 | 告警代码 |
| Alarm.AlarmName | String | 否 | 告警名称 |
| Alarm.AlarmLevel | String | 是 | 告警级别（WARN, CRITICAL 等） |
| Alarm.AlarmValue | String | 否 | 触发告警时的值 |
| Alarm.AlarmDesc | String | 否 | 告警描述 |
| Alarm.AlarmType | Integer | 否 | 告警类型（0=越限 等） |

**请求示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>SEND_ALARM</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <AlarmTime>2026-05-13T10:33:00+08:00</AlarmTime>
      </Info>
      <xmlData>
        <Alarm>
          <SignalID>TEMP-001</SignalID>
          <AlarmCode>TEMP-HIGH</AlarmCode>
          <AlarmName>机柜温度过高</AlarmName>
          <AlarmLevel>WARN</AlarmLevel>
          <AlarmValue>62.0</AlarmValue>
          <AlarmDesc>温度超过上限60°C</AlarmDesc>
          <AlarmType>0</AlarmType>
        </Alarm>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

#### 响应 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| AlarmID | Integer | 否 | 服务端生成的告警 ID |

xmlData：无（空节点）

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>SEND_ALARM</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <AlarmID>10001</AlarmID>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.5 GET_DATA — 获取监控数据

**方向**：SC → FSU（慢数据通道，SC 轮询）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| SignalID | String[] | 否 | 要查询的信号量 ID 列表（为空则查全部） |

**请求示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>GET_DATA</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <RequestTime>2026-05-13T10:35:00+08:00</RequestTime>
      </Info>
      <xmlData>
        <SignalID>TEMP-001</SignalID>
        <SignalID>HUMI-001</SignalID>
        <SignalID>VOLT-001</SignalID>
        <SignalID>DOOR-001</SignalID>
        <SignalID>WATER-001</SignalID>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| Count | Integer | 否 | 返回的信号量数量 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| Signal | Array | 是 | 信号量数组 |
| Signal.SignalID | String | 是 | 信号量 ID |
| Signal.Value | String | 是 | 当前值 |
| Signal.Quality | Integer | 是 | 数据质量 |
| Signal.Status | String | 否 | 状态 |
| Signal.CollectTime | DateTime | 否 | 采集时间 |

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>GET_DATA</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <Count>5</Count>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>TEMP-001</SignalID>
          <Value>25.8</Value>
          <Quality>1</Quality>
          <Status>NORMAL</Status>
          <CollectTime>2026-05-13T10:35:00+08:00</CollectTime>
        </Signal>
        ...多组 Signal...
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.6 SET_POINT — 遥控遥调

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| Signal | Array | 是 | 控制信号 |
| Signal.SignalID | String | 是 | 信号量 ID |
| Signal.SetValue | String | 是 | 设定值 |
| Signal.Operator | String | 否 | 操作人 |

**请求示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>SET_POINT</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <RequestTime>2026-05-13T10:38:00+08:00</RequestTime>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>VOLT-001</SignalID>
          <SetValue>230.0</SetValue>
          <Operator>admin</Operator>
        </Signal>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| SignalID | String | 是 | 被控制的信号量 ID |

xmlData：无（空节点）

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>SET_POINT</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <SignalID>VOLT-001</SignalID>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.7 GET_THRESHOLD — 获取告警门限

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |

| xmlData | 类型 | 必填 | 说明 |
|---------|------|------|------|
| SignalID | String[] | 否 | 信号量 ID 列表 |

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| Count | Integer | 否 | 门限数量 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| Signal | Array | 是 | 门限数组 |
| Signal.SignalID | String | 是 | 信号量 ID |
| Signal.AlarmUpper | Double | 否 | 告警上限 |
| Signal.AlarmLower | Double | 否 | 告警下限 |
| Signal.AlarmUpperUrgent | Double | 否 | 严重告警上限 |
| Signal.AlarmLowerUrgent | Double | 否 | 严重告警下限 |

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>GET_THRESHOLD</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <Count>3</Count>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>TEMP-001</SignalID>
          <AlarmUpper>60.0</AlarmUpper>
          <AlarmLower>-5.0</AlarmLower>
          <AlarmUpperUrgent>70.0</AlarmUpperUrgent>
          <AlarmLowerUrgent>-10.0</AlarmLowerUrgent>
        </Signal>
        ...多组 Signal...
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.8 SET_THRESHOLD — 设置告警门限

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| Signal | Array | 是 | 门限设置 |
| Signal.SignalID | String | 是 | 信号量 ID |
| Signal.AlarmUpper | Double | 否 | 告警上限 |
| Signal.AlarmLower | Double | 否 | 告警下限 |
| Signal.AlarmUpperUrgent | Double | 否 | 严重告警上限 |
| Signal.AlarmLowerUrgent | Double | 否 | 严重告警下限 |

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| Count | Integer | 否 | 设置的门限数量 |

xmlData：无（空节点）

---

### 6.9 TIME_CHECK — 时间同步

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| StandardTime | DateTime | 是 | SC 标准时间 |

xmlData：无（空节点）

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |
| FSUTime | DateTime | 否 | FSU 当前时间 |

xmlData：无（空节点）

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>TIME_CHECK</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <FSUTime>2026-05-13T10:42:01+08:00</FSUTime>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.10 GET_FTP — 获取FTP参数

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |
| FileType | String | 否 | 文件类型（如 IMAGE） |

xmlData：无（空节点）

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| FTPConfig.Host | String | 是 | FTP 服务器地址 |
| FTPConfig.Port | Integer | 是 | FTP 端口 |
| FTPConfig.Username | String | 是 | FTP 用户名 |
| FTPConfig.PassiveMode | Boolean | 否 | 是否被动模式 |
| FTPConfig.BasePath | String | 否 | 基础路径 |

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>GET_FTP</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
      </Info>
      <xmlData>
        <FTPConfig>
          <Host>192.168.1.200</Host>
          <Port>21</Port>
          <Username>fsu_ftp</Username>
          <PassiveMode>true</PassiveMode>
          <BasePath>/fsu/images/</BasePath>
        </FTPConfig>
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.11 SET_FTP — 设置FTP参数

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| FTPConfig.Host | String | 是 | FTP 服务器地址 |
| FTPConfig.Port | Integer | 是 | FTP 端口 |
| FTPConfig.Username | String | 是 | FTP 用户名 |
| FTPConfig.Password | String | 否 | FTP 密码 |
| FTPConfig.PassiveMode | Boolean | 否 | 是否被动模式 |
| FTPConfig.BasePath | String | 否 | 基础路径 |

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |

xmlData：无（空节点）

---

### 6.12 GET_LOGININFO — 获取登录信息

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

#### 请求 (SC → FSU)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| SessionID | String | 是 | 会话 ID |
| RequestTime | DateTime | 是 | 请求时间 |

xmlData：无（空节点）

#### 响应 (FSU → SC)

| Info 字段 | 类型 | 必填 | 说明 |
|-----------|------|------|------|
| ResultCode | Integer | 是 | 0=成功 |

| xmlData 字段 | 类型 | 必填 | 说明 |
|-------------|------|------|------|
| LoginInfo.FSUCode | String | 是 | FSU 设备编码 |
| LoginInfo.LoginStatus | String | 是 | 登录状态（LOGIN/LOGOUT） |
| LoginInfo.OnlineStatus | String | 是 | 在线状态（ONLINE/OFFLINE） |
| LoginInfo.SessionID | String | 是 | 当前会话 ID |
| LoginInfo.LoginTime | DateTime | 是 | 登录时间 |
| LoginInfo.LastHeartbeat | DateTime | 否 | 最后心跳时间 |

**响应示例**：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>GET_LOGININFO</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
      </Info>
      <xmlData>
        <LoginInfo>
          <FSUCode>FSU-001</FSUCode>
          <LoginStatus>LOGIN</LoginStatus>
          <OnlineStatus>ONLINE</OnlineStatus>
          <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
          <LoginTime>2026-05-13T10:30:00+08:00</LoginTime>
          <LastHeartbeat>2026-05-13T10:40:00+08:00</LastHeartbeat>
        </LoginInfo>
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

---

### 6.13 GET_FSUINFO — 获取FSU信息

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

| 项目 | 说明 |
|------|------|
| 状态 | 仅枚举已定义，无 fixture |
| 作用 | 获取 FSU 设备基本信息 |

Info 请求字段：FSUCode, SessionID, RequestTime
Info 响应字段：ResultCode

xmlData 结构：待确认（需对照协议原文补充）

---

### 6.14 GET_HISTORY_DATA — 获取历史数据

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

| 项目 | 说明 |
|------|------|
| 状态 | 仅枚举已定义，无 fixture |
| 作用 | 查询 FSU 存储的历史数据 |

Info 请求字段：FSUCode, SessionID, 起止时间
xmlData 结构：待确认（需对照协议原文补充）

---

### 6.15 SET_DATA — 设置监控数据

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

| 项目 | 说明 |
|------|------|
| 状态 | 仅枚举已定义，无 fixture |
| 作用 | SC 向 FSU 设置监控数据参数 |

Info 请求字段：FSUCode, SessionID, RequestTime
xmlData 结构：待确认（需对照协议原文补充）

---

### 6.16 SET_FSUREBOOT — 远程重启FSU

**方向**：SC → FSU（慢数据通道）
**通道**：FSUService

| 项目 | 说明 |
|------|------|
| 状态 | **安全禁用**（safe_enabled=FALSE） |
| 作用 | 远程重启 FSU 设备 |
| 约束 | 高风险操作，仅在安全模式下启用 |

Info 请求字段：FSUCode, SessionID
xmlData 结构：待确认

---

## 7. ResultCode 定义

### 7.1 通用值

| ResultCode | 含义 | 说明 |
|-----------|------|------|
| 0 | SUCCESS | 成功 |
| 1 | FAILURE | 通用失败 |
| 1001 | INVALID_SESSION | 会话无效或已过期 |
| 1002 | INVALID_FSU | FSU 未注册或认证失败 |
| 2001 | INVALID_PARAMETER | 参数错误 |
| 5001 | INTERNAL_ERROR | 服务器内部错误（见 SOAP Fault detail） |

### 7.2 使用规范

- **所有响应必须包含 ResultCode**
- ResultCode=0 表示成功，非 0 值应附带说明信息
- 业务层应定义更具体的错误码码表（待 BIF-P1-xxx 阶段完善）

---

## 8. Info 字段汇总

### 8.1 请求通用字段

| 字段 | 出现于 | 类型 | 说明 |
|------|--------|------|------|
| FSUCode | 所有请求 | String | FSU 设备编码 |
| SessionID | 认证后请求 | String | 会话 ID（LOGIN 的请求和响应均包含） |
| Timestamp | LOGIN, HEARTBEAT | DateTime | 时间戳 |
| RequestTime | GET_DATA, SET_POINT 等 | DateTime | 请求时间 |
| StandardTime | TIME_CHECK | DateTime | SC 标准时间 |
| CollectTime | SEND_DATA | DateTime | 采集时间 |
| AlarmTime | SEND_ALARM | DateTime | 告警时间 |
| FileType | GET_FTP | String | 文件类型 |
| Password | LOGIN | String | 登录密码 |
| FSUID | LOGIN | String | FSU 唯一标识 |
| DeviceID | LOGIN | String | 设备 ID |
| ProtocolVersion | LOGIN | String | 协议版本 |

### 8.2 响应通用字段

| 字段 | 出现于 | 类型 | 说明 |
|------|--------|------|------|
| ResultCode | 所有响应 | Integer | 0=成功 |
| SessionID | LOGIN 响应 | String | 分配的会话 ID |
| ExpireSeconds | LOGIN 响应 | Integer | 会话过期时间(秒) |
| ServerTime | LOGIN, HEARTBEAT 响应 | DateTime | 服务器时间 |
| Count | SEND_DATA, GET_DATA, GET_THRESHOLD, SET_THRESHOLD 响应 | Integer | 数据条目数 |
| AlarmID | SEND_ALARM 响应 | Long | 告警 ID |
| SignalID | SET_POINT 响应 | String | 被控制的信号量 ID |
| FSUTime | TIME_CHECK 响应 | DateTime | FSU 当前时间 |

---

## 9. xmlData 结构汇总

### 9.1 请求 xmlData

| PK_Type | 根元素 | 子元素 | 说明 |
|---------|--------|--------|------|
| LOGIN | DeviceInfo | Manufacturer, Model, FirmwareVersion, HardwareVersion, MacAddr, IPAddr | FSU 设备信息 |
| HEARTBEAT | — | CPU, Memory, Temperature, RunningTime | FSU 运行状态 |
| SEND_DATA | Signal[] | SignalID, Value, Quality, Status | 实时采集信号量 |
| SEND_ALARM | Alarm[] | SignalID, AlarmCode, AlarmName, AlarmLevel, AlarmValue, AlarmDesc, AlarmType | 告警信息 |
| GET_DATA | SignalID[] | — | 要查询的信号量 ID |
| SET_POINT | Signal[] | SignalID, SetValue, Operator | 遥控遥调参数 |
| GET_THRESHOLD | SignalID[] | — | 要查询的信号量 ID |
| SET_THRESHOLD | Signal[] | SignalID, AlarmUpper, AlarmLower, AlarmUpperUrgent, AlarmLowerUrgent | 门限设置参数 |
| TIME_CHECK | 空 | — | 无业务数据 |
| GET_FTP | 空 | — | 无业务数据 |
| SET_FTP | FTPConfig | Host, Port, Username, Password, PassiveMode, BasePath | FTP 设置参数 |
| GET_LOGININFO | 空 | — | 无业务数据 |

### 9.2 响应 xmlData

| PK_Type | 根元素 | 子元素 | 说明 |
|---------|--------|--------|------|
| LOGIN | 空 | — | 无业务数据 |
| HEARTBEAT | 空 | — | 无业务数据 |
| SEND_DATA | 空 | — | 无业务数据 |
| SEND_ALARM | 空 | — | 无业务数据 |
| GET_DATA | Signal[] | SignalID, Value, Quality, Status, CollectTime | 实时数据 |
| SET_POINT | 空 | — | 无业务数据 |
| GET_THRESHOLD | Signal[] | SignalID, AlarmUpper, AlarmLower, AlarmUpperUrgent, AlarmLowerUrgent | 门限数据 |
| SET_THRESHOLD | 空 | — | 无业务数据 |
| TIME_CHECK | 空 | — | 无业务数据 |
| GET_FTP | FTPConfig | Host, Port, Username, PassiveMode, BasePath | FTP 配置 |
| SET_FTP | 空 | — | 无业务数据 |
| GET_LOGININFO | LoginInfo | FSUCode, LoginStatus, OnlineStatus, SessionID, LoginTime, LastHeartbeat | 登录状态信息 |

---

## 10. SOAP Fault 处理

### 10.1 Fault 结构

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <soap:Fault>
      <faultcode>soap:Server</faultcode>
      <faultstring>服务器内部错误</faultstring>
      <detail>
        <error>
          <code>5001</code>
          <message>数据库连接失败</message>
        </error>
      </detail>
    </soap:Fault>
  </soap:Body>
</soap:Envelope>
```

### 10.2 Fault 字段

| 字段 | 说明 | 典型值 |
|------|------|--------|
| faultcode | 错误分类 | soap:Server, soap:Client, soap:MustUnderstand |
| faultstring | 错误描述 | 人类可读的错误信息 |
| faultactor | 错误发生节点（可选） | URI |
| detail | 详细错误信息 | 业务错误码和消息 |

### 10.3 错误场景

| 场景 | Fault Code | 说明 |
|------|-----------|------|
| 格式错误 XML | soap:Client | 解析时 XML 语法异常 |
| SOAP 版本不匹配 | soap:MustUnderstand | SOAP 1.1 vs 1.2 兼容问题 |
| 内部异常 | soap:Server | 服务端未预期异常 |
| 业务拒绝 | soap:Client | 认证失败、参数无效等 |

---

## 11. 会话管理

### 11.1 会话生命周期

```
FSU 启动
   │
   ├── LOGIN (FSU→SC)
   │      ├── SC 验证 FSUCode + Password
   │      └── SC 返回 SessionID + ExpireSeconds
   │
   ├── HEARTBEAT (FSU→SC, 定期)
   │      └── 续约会话
   │
   ├── 业务通信 (使用 SessionID)
   │      ├── FSU→SC: SEND_DATA, SEND_ALARM
   │      └── SC→FSU: GET_DATA, SET_POINT, ...
   │
   └── 会话过期
          └── FSU 需重新 LOGIN
```

### 11.2 会话相关表

| 表名 | 实体 | 说明 |
|------|------|------|
| b_interface_session | BInterfaceSessionEntity | 会话记录 |
| b_interface_fsu_status | BInterfaceFsuStatusEntity | FSU 在线状态 |
| b_interface_message_log | BInterfaceMessageLogEntity | 报文日志 |

### 11.3 会话约束

- LOGIN 必须在其他命令之前完成
- 所有命令（除 LOGIN）需携带有效的 SessionID
- SC 验证 SessionID 有效性，无效返回 ResultCode=1001
- 会话超时后 FSU 需重新登录

---

## 12. 通道说明

### 12.1 快数据通道（FSU→SC，主动上报）

| 特征 | 说明 |
|------|------|
| 发起方 | FSU |
| 接收方 | SC |
| SC 角色 | 服务端（SCService） |
| 协议操作 | FSU 调用 SC 的 SCService:invoke |
| 上报频率 | 高（秒~分钟级） |
| 数据内容 | 登录、心跳、告警、实时采样 |

### 12.2 慢数据通道（SC→FSU，轮询）

| 特征 | 说明 |
|------|------|
| 发起方 | SC |
| 接收方 | FSU |
| SC 角色 | 客户端（FSUService Client） |
| 协议操作 | SC 调用 FSU 的 FSUService:invoke |
| 轮询频率 | 低（分钟~小时级） |
| 数据内容 | 监控数据查询、参数设置、FTP 文件传输 |

### 12.3 通道对比

| 对比项 | 快数据通道 | 慢数据通道 |
|--------|-----------|-----------|
| 启动方式 | FSU 主动 | SC 轮询 |
| 方向 | FSU → SC | SC → FSU |
| WSDL | SCService | FSUService |
| SC 角色 | 服务端 | 客户端 |
| 典型命令 | LOGIN, HEARTBEAT, SEND_DATA, SEND_ALARM | GET_DATA, SET_POINT, TIME_CHECK, GET_FTP |

---

## 13. Fixtures 交叉引用

### 13.1 目录结构

```
backend/fixtures/b_interface/
├── README.md                  ← 架构文档
├── soap/                      ← 完整 SOAP 报文
│   ├── sc_service/            ←  FSU→SC（快数据通道）
│   │   ├── login.request.xml
│   │   ├── login.response.xml
│   │   ├── heartbeat.request.xml
│   │   ├── heartbeat.response.xml
│   │   ├── send_data.request.xml
│   │   ├── send_data.response.xml
│   │   ├── send_alarm.request.xml
│   │   └── send_alarm.response.xml
│   └── fsu_service/           ←  SC→FSU（慢数据通道）
│       ├── get_data.request.xml
│       ├── get_data.response.xml
│       ├── get_threshold.request.xml
│       ├── get_threshold.response.xml
│       ├── set_threshold.request.xml
│       ├── set_threshold.response.xml
│       ├── set_point.request.xml
│       ├── set_point.response.xml
│       ├── get_ftp.request.xml
│       ├── get_ftp.response.xml
│       ├── set_ftp.request.xml
│       ├── set_ftp.response.xml
│       ├── get_logininfo.request.xml
│       ├── get_logininfo.response.xml
│       ├── time_check.request.xml
│       └── time_check.response.xml
├── xmldata/                   ← 纯 xmlData 内容
│   └── ... (镜像 soap/ 结构，无 SOAP Envelope)
├── invalid/                   ← 异常报文
│   ├── malformed_xml.request.xml
│   ├── missing_fsu_code.request.xml
│   ├── unknown_msg_type.request.xml
│   ├── empty_body.request.xml
│   ├── no_pk_type.request.xml
│   └── soap_fault.response.xml
└── expected/                  ← 解析期望结果 (JSON)
    ├── parsed_login.request.json
    ├── parsed_login.response.json
    ├── parsed_heartbeat.request.json
    ├── parsed_heartbeat.response.json
    ├── parsed_send_data.request.json
    ├── parsed_send_data.response.json
    ├── parsed_send_alarm.request.json
    ├── parsed_send_alarm.response.json
    ├── parsed_get_data.request.json
    ├── parsed_get_data.response.json
    ├── parsed_get_threshold.request.json
    ├── parsed_get_threshold.response.json
    ├── parsed_set_threshold.request.json
    ├── parsed_set_threshold.response.json
    ├── parsed_set_point.request.json
    ├── parsed_set_point.response.json
    ├── parsed_get_ftp.request.json
    ├── parsed_get_ftp.response.json
    ├── parsed_time_check.request.json
    └── parsed_time_check.response.json
```

### 13.2 Fixture 覆盖矩阵

| PK_Type | SOAP Request | SOAP Response | xmlData Req | xmlData Res | Expected Req | Expected Res | Invalid |
|---------|:-----------:|:------------:|:-----------:|:-----------:|:-----------:|:-----------:|:-------:|
| LOGIN | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| HEARTBEAT | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| SEND_DATA | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| SEND_ALARM | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| GET_DATA | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| GET_THRESHOLD | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| SET_THRESHOLD | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| SET_POINT | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| GET_FTP | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| SET_FTP | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | — |
| GET_LOGININFO | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | — |
| TIME_CHECK | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| malformed_xml | — | — | — | — | — | — | ✅ |
| missing_fsu_code | — | — | — | — | — | — | ✅ |
| unknown_msg_type | — | — | — | — | — | — | ✅ |
| empty_body | — | — | — | — | — | — | ✅ |
| no_pk_type | — | — | — | — | — | — | ✅ |
| soap_fault | — | — | — | — | — | — | ✅ |

> ❌ = fixture 存在但未创建 expected JSON（待补充）

### 13.3 缺失 expected JSON 的命令

- GET_LOGININFO: request + response
- SET_FTP: request + response

---

## 14. 实现状态总览

### 14.1 Java 代码状态

| 层 | 文件 | 状态 |
|----|------|------|
| **枚举** | BInterfacePkType | 完成（缺 **SEND_DATA**, **SET_FTP**） |
| **模型** | BInterfaceMessage | 骨架完成（缺序列化/反序列化） |
| **SOAP** | SoapMessageHandler | 占位（buildRequest/parseResponse 均返回 null） |
| **XML** | XmlDataModel | 占位（toXml/fromXml 均返回 null） |
| **WSDL 服务端** | ScServiceWsdlController | 占位（返回固定字符串） |
| **WSDL 模板** | FsuServiceWsdlTemplate | 占位（返回固定字符串） |
| **SCService 端点** | ScServiceController | 端点已注册（返回占位响应） |
| **SCService 处理** | ScServiceHandler | 空类 |
| **FSUService 接口** | FsuServiceClient | 接口定义完成（5 方法） |
| **FSUService 实现** | FsuServiceClientStub | 全部空方法 |
| **命令分发** | CommandDispatcher | 空（返回 null） |
| **LOGIN 处理** | LoginCommandHandler | 空类 |
| **HEARTBEAT 处理** | HeartbeatCommandHandler | 空类 |
| **GET_DATA 处理** | GetDataCommandHandler | 空类 |
| **SEND_ALARM 处理** | SendAlarmCommandHandler | 空类 |
| **日志实体** | BInterfaceMessageLogEntity | 完成 |
| **会话实体** | BInterfaceSessionEntity | 完成 |
| **FSU 状态实体** | BInterfaceFsuStatusEntity | 完成 |
| **命令实体** | BInterfaceCommandEntity | 完成 |
| **调用记录实体** | BInterfaceCallRecordEntity | 完成 |
| **FTP 传输记录实体** | FtpTransferRecordEntity | 完成 |
| **FTP 配置模型** | FtpConfigModel | 完成 |
| **查询服务** | BInterface*QueryService | 完成（全部只读） |
| **健康检查** | BInterfaceHealthController | ✅ 已实现 |

### 14.2 测试状态

| 测试类 | 状态 |
|--------|------|
| BInterfaceFixtureLoader | ✅ 已实现 |
| SoapMessageHandlerTest | 24 个 fixture 加载测试通过 + 14 个占位 |
| XmlDataModelTest | 4 个 fixture 验证 + 9 个占位 |
| BInterfaceInvalidMessageTest | 1 个 fixture 存在性检查 + 6 个占位 |

### 14.3 已知缺陷

| # | 缺陷 | 影响 | 建议任务 |
|---|------|------|---------|
| 1 | BInterfacePkType 缺少 SEND_DATA | 无法处理 FSU 实时数据上报 | BIF-P0-001 |
| 2 | BInterfacePkType 缺少 SET_FTP | 无法处理 FTP 参数设置命令 | BIF-P0-001 |
| 3 | SoapMessageHandler 全部返回 null | 无法解析/构造任何 SOAP 报文 | BIF-P0-003 |
| 4 | XmlDataModel 全部返回 null | 无法解析/构造任何 xmlData | BIF-P0-003 |
| 5 | WSDL 端点和模板均为占位 | FSU 无法获取 WSDL 定义 | BIF-P0-002 |
| 6 | CommandDispatcher 返回 null | 无法路由任何命令 | BIF-P0-004 |
| 7 | 所有 Handler 为空 | 无法处理任何业务逻辑 | BIF-P1 系列 |
| 8 | FsuServiceClientStub 全部空方法 | 无法调用 FSU（慢数据通道不可用） | BIF-P1-005 |

### 14.4 建议开发顺序

| 阶段 | 任务 | 说明 |
|------|------|------|
| **BIF-P0-001** | 命令码映射补全 | 修复 BInterfacePkType，添加 SEND_DATA, SET_FTP |
| **BIF-P0-002** | WSDL 入口修复 | 实现 ScServiceWsdlController 和 FsuServiceWsdlTemplate 的真实 WSDL 输出 |
| **BIF-P0-003** | SOAP/XMLData 解析 | 实现 SoapMessageHandler 和 XmlDataModel |
| **BIF-P0-004** | 命令分发器 | 实现 CommandDispatcher 路由逻辑 |
| **BIF-P1-001** | 快数据通道 - FSU 登录认证 | 实现 LoginCommandHandler |
| **BIF-P1-002** | 快数据通道 - 心跳处理 | 实现 HeartbeatCommandHandler + 超时判断 |
| **BIF-P1-003** | 快数据通道 - 告警上报 | 实现 SendAlarmCommandHandler |

---

> **文档版本**：v1.0
> **最后更新**：2026-05-13
> **协议基准**：中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0
> **配套 WSDL**：`SCService.wsdl` / `FSUService.wsdl`
> **项目实现**：`fsu-platform-java`
