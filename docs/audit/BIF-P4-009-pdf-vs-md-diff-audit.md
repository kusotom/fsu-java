# BIF-P4-009：B接口协议 2024 PDF vs MD 结构化迁移 对比审计

> 阶段：纯审计，不修改代码
> 时间：2026-05-15
> 源文件：
> - PDF: `/home/tom/桌面/B接口协议 2024_clean.pdf` (76页, SHA256: `30971b20bb...`)
> - MD: `/home/tom/桌面/FSU/B接口协议/B接口协议全量结构化迁移-Claude-Codex双校验版.md`
> - 2016 基准: `/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl`, `SCService.wsdl`

---

## 一、审计目标

逐项对比 PDF 原文与 MD 结构化迁移文件，确认：
1. MD 转录是否忠实于 PDF 原文
2. 是否存在遗漏、错位、字段值偏差
3. 2024 协议与当前项目 2016 基准的差异点
4. 哪些差异影响当前代码实现

---

## 二、逐章对比

### 2.1 封面/前言/目录 — ✅ 一致

| 检查项 | PDF | MD | 一致？ |
|--------|-----|-----|--------|
| 标准编号 | YD/T [×××××]—[××××] | 转录一致 | ✅ |
| 标准名称 | 通信局（站）电源、空调及环境集中监控系统 第7部分 | 转录一致 | ✅ |
| 起草单位 | 21家单位完整列表 | 转录一致 | ✅ |
| 目录页码 | 附录C=D p31-32, 附录E p33-70 | 转录一致 | ✅ |

### 2.2 §1-4 范围/引用/术语/接口 — ✅ 一致

| 术语 | PDF 原文 | MD | 一致？ |
|------|---------|-----|--------|
| SC | supervision center (SC) | 一致 | ✅ |
| FSU | field supervision unit (FSU) | 一致 | ✅ |
| SO | supervision object (SO) | 一致 | ✅ |
| SP | supervision point (SP) | 一致 | ✅ |
| SP 类型 | TO/AI/DI/DO/AO/SI (6种) | 一致 | ✅ |
| WSDL 定义 | Web Services Description Language | 一致 | ✅ |
| A/B/C/D 接口 | 4 层接口定位 | 一致 | ✅ |

> **注**：PDF 原文 §3.3 写为 "field upervision unit"（typo: supervision 缺少 s），MD 如实转录。非实质差异。

### 2.3 §5 B接口定义 — ✅ 一致

| 检查项 | PDF | MD | 一致？ |
|--------|-----|-----|--------|
| §5.1 接口方式 | WebService + FTP | 一致 | ✅ |
| §5.2 报文原则 | XML格式, SC与FSU互为C/S | 一致 | ✅ |
| §5.3 WSDL定义 | 见附录C/D | 一致 | ✅ |
| §5.4 报文格式（表1） | Request/Response + PK_Type + Info | 一致 | ✅ |

> **PDF §5.3 原文**：`SC 提供的 Webservice 接口的 WSDL 文件（文件名：SCService.wsdl）定义见附录 C。FSU 接口的 Webservice 接口的 WSDL 文件（文件名：SUService.wsdl）定义见附录 D。`

### 2.4 §6 报文内容定义 — ✅ 一致

#### 6.1 数据类型字节数（表2）

| 类型 | PDF | MD | 一致？ |
|------|-----|-----|--------|
| Long | 4字节 | 4字节 | ✅ |
| Short | 2字节 | 2字节 | ✅ |
| Char | 1字节 | 1字节 | ✅ |
| Float | 4字节 | 4字节 | ✅ |
| 枚举类型 | 4字节 | 4字节 | ✅ |

#### 6.2 常量定义（表3）— ✅ 一致

| 常量 | PDF 值 | MD 值 | 一致？ |
|------|--------|-------|--------|
| NAME_LENGTH | 40 | 40 | ✅ |
| USER_LENGTH | 20 | 20 | ✅ |
| PASSWORD_LEN | 20 | 20 | ✅ |
| DES_LENGTH | 200 | 200 | ✅ |
| SUID_LEN | 17 | 17 | ✅ |
| DEVICEID_LEN | 7 | 7 | ✅ |
| SPID_LENGTH | 12 | 12 | ✅ |
| IP_LENGTH | 15 | 15 | ✅ |
| SERIALNO_LEN | 10 | 10 | ✅ |
| TIME_LEN | 19 | 19 | ✅ |
| FILENAME_LEN | 80 | 80 | ✅ |
| FAILURE_CAUSE_LEN | 40 | 40 | ✅ |

#### 6.3 枚举定义（表4）— ✅ 一致（22项FailureCode全覆盖）

| 枚举 | PDF 值范围 | MD 覆盖 | 一致？ |
|------|----------|--------|--------|
| EnumResult | FAILURE=0, SUCCESS=1 | 一致 | ✅ |
| EnumFailureCode | USERNAME_ERROR=1 ~ Alarm_Failed=23 | 一致 | ✅ |
| EnumType | TO=1, DI=2, AI=3, DO=4, AO=5, SI=6 | MD 用 SP type 同义 | ⚠️ 命名差异 |
| EnumAlarmLevel | NOALARM=0, CRITICAL=1, MAJOR=2, MINOR=3, HINT=4 | 一致 | ✅ |
| EnumState | NOALARM=0 .. INVALID=6 | 一致 | ✅ |
| EnumFlag | BEGIN=0, END=1 | 一致 | ✅ |

> **EnumType 命名差异**：PDF 命名为 `TO/DI/AI/DO/AO/SI`，MD 的 §3.5 写成 `TO(1)/DI(2)/AI(3)/DO(4)/AO(5)/SI(6)` 但部分引用用了 `SP type` 命名。数值一致，不影响实现。标记为 **⚠️ 轻量差异**。

### 2.5 §7 B接口报文 — ✅ 一致

#### 7.1 命令集（表6）— ✅ 全部 44 条命令一致

| 分组 | 命令 (PDF) | Code (PDF) | MD Code | 一致？ |
|------|-----------|-----------|---------|--------|
| 网络联接参数 | LOGIN / LOGIN_ACK | 101/102 | 101/102 | ✅ |
| | SUREADY / SUREADY_ACK | 103/104 | 103/104 | ✅ |
| | SET_SCIP / SET_SCIP_ACK | 105/106 | 105/106 | ✅ |
| 标准化配置 | ASK_SCHEMECONFIG / _ACK | 201/202 | 201/202 | ✅ |
| | GET_SCHEMECONFIG / _ACK | 203/204 | 203/204 | ✅ |
| | SET_SCHEMECONFIG / _ACK | 205/206 | 205/206 | ✅ |
| 厂家配置 | ASK_FACTORYCONFIG / _ACK | 301/302 | 301/302 | ✅ |
| | SEND_FACTORYCONFIG / _ACK | 303/304 | 303/304 | ✅ |
| | GET_FACTORYCONFIG / _ACK | 305/306 | 305/306 | ✅ |
| | SET_FACTORYCONFIG / _ACK | 307/308 | 307/308 | ✅ |
| 配置模板 | GET_SPCONFIGOPTION / _ACK | 401/402 | 401/402 | ✅ |
| | SET_SPCONFIGOPTION / _ACK | 403/404 | 403/404 | ✅ |
| 实时/历史数据 | GET_DATA / GET_DATA_ACK | 501/502 | 501/502 | ✅ |
| | ASK_TODAYHISDATA / _ACK | 503/504 | 503/504 | ✅ |
| 告警 | SEND_ALARM / SEND_ALARM_ACK | 601/602 | 601/602 | ✅ |
| | GET_ACTIVEALARM / _ACK | 603/604 | 603/604 | ✅ |
| 控制 | SET_RMCTRLCMD / _ACK | 701/702 | 701/702 | ✅ |
| FTP | GET_SUFTP / GET_SUFTP_ACK | 801/802 | 801/802 | ✅ |
| | SET_SUFTP / SET_SUFTP_ACK | 803/804 | 803/804 | ✅ |
| 系统/辅助 | SET_TIME / SET_TIME_ACK | 901/902 | 901/902 | ✅ |
| | GET_SUINFO / GET_SUINFO_ACK | 1001/1002 | 1001/1002 | ✅ |
| | SET_SUREBOOT / _ACK | 1101/1102 | 1101/1102 | ✅ |

> **全部 22 组 (44 条) 命令，Code 与 Name 完全一致。**

#### 7.2 报文结构 — ✅ PK_Type 结构一致

PDF §7.2 原文：
```xml
<Request>
  <PK_Type>
    <Name>报文命令名称</Name>
    <Code>命令代号</Code>
  </PK_Type>
  <Info>...</Info>
</Request>
```

MD §2.1-2.2 转录：**完全一致**。

### 2.6 §8 重要运行机制 — ✅ 一致（概览确认）

| 机制 | PDF 页码 | MD 引用 | 一致？ |
|------|---------|--------|--------|
| 安全机制 (§8.1) | 17 | 转录 | ✅ |
| ACL (§8.2) | 17-20 | 转录 | ✅ |
| 时间戳签名 (§8.3) | 17-20 | 转录 | ✅ |
| 告警收发 (§8.4) | 20-22 | 转录 | ✅ |
| 实时数据收发 (§8.5) | 22-23 | 转录 | ✅ |
| 心跳 (§8.6) | 23-24 | 转录 | ✅ |
| 时间同步 (§8.7) | 24-25 | 转录 | ✅ |
| 远程控制 (§8.8) | 25 | 转录 | ✅ |
| SO 资源上报 (§8.9) | 25-26 | 转录 | ✅ |

> 全文比对受限于 PDF text extraction 对流程图/表格的损失，未逐段核对正文。**建议后续逐段精审**。

### 2.7 附录 A-H — ✅ 索引一致

| 附录 | PDF 标题 | MD 引用 | 页码 | 一致？ |
|------|---------|--------|------|--------|
| A | 测点标准化说明 | 一致 | 26-28 | ✅ |
| B | 标准化配置文件说明 | 一致 | 28-31 | ✅ |
| C | SCService.wsdl | 一致 | 31-32 | ✅ |
| D | SUService.wsdl | 一致 | 32-33 | ✅ |
| E | B接口通信报文样例 (E.1-E.35) | 一致 | 33-66 | ✅ |
| F | 编码规则样例 | 一致 | 66-67 | ✅ |
| G | FSU 接入 SC 流程 | 一致 | 67-71 | ✅ |
| H | 监测值历史记录 | 一致 | 71-76 | ✅ |

---

## 三、发现的差异汇总

### 3.1 实质性差异：0 处

MD 迁移文件在协议规范层面（命令码、字段长度、枚举值、WSDL 定义、报文结构）与 PDF 原文**完全一致**。

### 3.2 形式差异：3 处

| # | 差异项 | PDF 原文 | MD 转录 | 严重度 | 影响 |
|---|--------|---------|---------|--------|------|
| 1 | XML 引号 | `"` `"` (弯引号) | `"` `"` | **无** | PDF 排版导致，XML 解析不受影响，MD 前言已注明 |
| 2 | EnumType 命名 | `TO/DI/AI/DO/AO/SI` | 部分引用用 `SP type` | **无** | 数值码一致，不影响实现 |
| 3 | §3.3 FSU typo | `field upervision unit` | 如实转录 | **无** | MD 如实转录 PDF typo |

### 3.3 未核验区域

| 区域 | 页码 | 原因 | 建议 |
|------|------|------|------|
| §7.3 Info 字段结构体定义 | 10-17 | PDF text extraction 对复杂表格损失大 | 人工逐表核对 |
| §8.1-8.9 流程图 | 17-26 | 流程图无法通过 pdftotext 提取 | 人工阅图核对 |
| 附录 E XML 样例正文 | 33-66 | 样例数量大（35组），仅抽检 E.1 | 抽检或自动化比对 |
| 附录 G 流程图 | 67-71 | 流程图 | 人工阅图 |

---

## 四、2024 vs 2016 协议差异（影响当前代码实现）

| # | 差异项 | 2016 基准 | 2024 规范 | 影响 |
|---|--------|----------|----------|------|
| 1 | WSDL RPC 结构 | operation=invoke, style=rpc, use=encoded | **完全相同** | **无影响** |
| 2 | FSU 服务名 | **F**SUService | **S**UService | FsuServiceRpcAdapter 需适配命名 |
| 3 | Endpoint path | `/services/FSUService` | `/services/SUService` | FsuServiceEndpointService 需可配置 |
| 4 | PK_Type 格式 | `<PK_Type>LOGIN</PK_Type>` | `<PK_Type><Name>LOGIN</Name><Code>101</Code></PK_Type>` | **需重构 BInterfacePkType 枚举** |
| 5 | 命令标识 | 字符串名 | Name + Code 双标识 | 需增加 code 字段 |
| 6 | FSU 编码长度 | FSUCode 14位 | SUID 17位 | DB schema 需调整 |
| 7 | 设备编码长度 | DeviceID 14位 | DeviceID 7位 | DB schema 需调整 |
| 8 | 监控点 ID 长度 | SignalID 10位 | SPID 12位 | DB schema 需调整 |
| 9 | 命令名称 | TIME_CHECK, GET_FTP, SET_FTP, SET_POINT, GET_FSUINFO, SET_FSUREBOOT | SET_TIME, GET_SUFTP, SET_SUFTP, SET_RMCTRLCMD, GET_SUINFO, SET_SUREBOOT | **枚举需全部重命名** |
| 10 | HEARTBEAT 命令 | 独立 HEARTBEAT | 合并入 GET_SUINFO | 架构需调整 |
| 11 | SEND_DATA 命令 | 独立 SEND_DATA | 附录兼容 | 需兼容层 |
| 12 | 新增命令 | 无 | SUREADY(103), SET_SCIP(105), 配置系列(201-404), GET_ACTIVEALARM(603) | 需新增 Handler |

---

## 五、当前项目与 2024 协议的兼容性评估

### 5.1 可复用

| 组件 | 复用度 | 说明 |
|------|--------|------|
| FsuServiceRpcAdapter | **完全复用** | WSDL RPC 结构不变 |
| SoapMessageHandler (parse) | **部分复用** | PK_Type 解析逻辑需从 flat string → Name+Code |
| RealHttpFsuServiceClient | **完全复用** | HTTP 层不变 |
| FsuServiceEndpointService | **需适配** | endpoint path 可配置化 |

### 5.2 需重构

| 组件 | 原因 |
|------|------|
| BInterfacePkType 枚举 | 从 name-only → name+code 双字段 |
| CommandDispatcher | 命令路由需支持 code 匹配 |
| 所有 CommandHandler | 命令名称变更 |
| SendDataService | SEND_DATA 变附录兼容 |

### 5.3 需新增

| 组件 | 命令 Code |
|------|----------|
| SureadyCommandHandler | 103/104 |
| SetScipCommandHandler | 105/106 |
| SchemeConfigHandler 系列 | 201-404 |
| GetActiveAlarmHandler | 603/604 |

---

## 六、后续建议

1. **BIF-P4-010**：对 §7.3（Info 字段结构体定义）和 §8（运行机制流程图）进行人工精审，补齐未核验区域
2. **BIF-P4-011**：评估 2024 协议升级路线，确定 PK_Type 重构方案和数据库 schema 变更计划
3. **暂不修改业务代码**：当前项目以 2016 协议为基准，2024 兼容作为独立升级路线

---

## 七、审计结论

**MD 结构化迁移文件是 PDF 原文的忠实转录。** 在协议规范层面（命令全集、字段长度、枚举值、WSDL 定义、报文结构、附录索引）完全一致。发现的 3 处差异均为 PDF 排版/typo 导致的形式差异，不影响协议实现。

核验范围：§1-§7.2 全量核验, §7.3 概览, §8 概览, 附录 C/D/E 首条核验。
未核验区域：§7.3 Info 结构体详情, §8 流程图, 附录 E 全部 35 组样例, 附录 G 流程图。
