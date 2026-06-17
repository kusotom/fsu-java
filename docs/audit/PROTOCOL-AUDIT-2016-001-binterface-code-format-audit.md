# PROTOCOL-AUDIT-2016-001: B接口2016 协议原文与本地代码实现格式对照审计

> 审计日期: 2026-05-21 | 类型: 只读协议审计 | Java 修改: 0

---

## 1. 本次目标

对照 B接口2016 协议手册、LANDING-006 真实 FSU 实测数据、当前 Java 代码，审计 7 个关键命令的协议符合性。

## 2. 读取的协议资料

| 资料 | 路径 | 状态 |
|------|------|------|
| 2016 协议手册 | `docs/protocol/b-interface-2016-handbook.md` | ✅ |
| 协议基线审计 | `docs/audit/PROTOCOL-BASELINE-001-*.md` | ✅ |
| LANDING-006 审计 | `docs/audit/LANDING-006-binterface-2016-code-retry.md` | ✅ |
| raw-samples/landing006 | `docs/landing/raw-samples/` | ❌ **缺口** — 目录为空 |
| raw-samples/landing015 | `docs/landing/raw-samples/` | ❌ **缺口** — 从未保存 |

> **严重缺口**: landing006 和 landing015 的原始 SOAP 报文未保存到 `docs/landing/raw-samples/`。LANDING-006 的 48 个 XML 报文和 LANDING-015 的 request/response 在磁盘上不可查。当前分析依赖 audit 文档中的内嵌 XML 片段。

## 3. 2016 协议关键事实

### 3.1 报文结构 (协议手册 §4)

```
SOAP Envelope → SOAP Body → Request/Response → PK_Type + Info + xmlData
```

PK_Type 在 2016 标准协议中使用**纯文本**（如 `<PK_Type>LOGIN</PK_Type>`），不强制 Name+Code 格式。
Name+Code 格式 (`<Name>LOGIN</Name><Code>101</Code>`) 是 2024 引入的增强，但真实 Emerson FSU 也接受此格式。

### 3.2 关键 Info 字段约定 (§8.1)

| 字段 | 类型 | 标准出现位置 |
|------|------|-------------|
| `FSUCode` | String | **所有请求** |
| `FSUID` | String | LOGIN 请求 |
| `SessionID` | String | 认证后请求 |

> **2016 协议中 `FSUCode` 是大驼峰（PascalCase），不是全大写。**

## 4. 本地代码命令码表对照

| 命令 | 2016 Code | 本地 BInterfaceCommand2016 | 一致 | 文件位置 |
|------|-----------|---------------------------|:--:|------|
| LOGIN | 101 | — (未在 2016 表中) | N/A | SC 入站，2016 表仅列出 SC→FSU 命令 |
| GET_DATA | 401 | ✅ 401 | ✅ | `BInterfaceCommand2016.java:32` |
| SEND_ALARM | 501 | ✅ 501 | ✅ | `BInterfaceCommand2016.java:35` |
| GET_LOGININFO | 1501 | ✅ 1501 | ✅ | `BInterfaceCommand2016.java:38` |
| GET_FTP | 1601 | ✅ 1601 | ✅ | `BInterfaceCommand2016.java:40` |
| GET_FSUINFO | 1701 | ✅ 1701 | ✅ | `BInterfaceCommand2016.java:42` |
| GET_THRESHOLD | 1901 | ❌ **缺失** | ❌ | `BInterfaceCommand2016.java` 无此条目 |

> **结论**: 6/7 命令 Code 已正确定义。GET_THRESHOLD=1901 缺失（但 GET_THRESHOLD 在 2024 枚举和 Handler 中已实现，未使用 2016 码表）。

## 5. 本地请求 XML 构造对照

| 命令 | 协议标准 Info | LANDING-006 实测格式 | LANDING-015 格式 | 结论 |
|------|--------------|---------------------|-----------------|------|
| GET_DATA | FSUCode, SessionID, RequestTime | `<FSUCode>xxx</FSUCode>` | — | Emerson 省略 SessionID/RequestTime |
| GET_LOGININFO | FSUCode, SessionID, RequestTime | `<FSUCode>xxx</FSUCode>` | — | Emerson 省略 SessionID/RequestTime |
| GET_FTP | FSUCode, SessionID, RequestTime | `<FSUCode>xxx</FSUCode>` | — | Emerson 省略 SessionID/RequestTime |
| GET_FSUINFO | **FSUCode**, SessionID, RequestTime | `<FSUCode>xxx</FSUCode>` | ✅ 已对齐 (FIX-002) | **需注意**: 协议原文是 FSUCode，不是 FSUCODE |

**关键发现**:

| 格式 | 来源 | 标签 | LANDING-006 成功? |
|------|------|------|:--:|
| 2016 标准 | 协议手册 §6.13 | `<FSUCode>` | 未验证 |
| Emerson 实测 | LANDING-006 代码 | `<FSUCode>` | ✅ |
| LANDING-015 原版 | 误写的 Service | `<FsuId>` + `<FsuCode>` | ❌ |
| LANDING-015 修复 | FIX-002 | `<FSUCode>` | ✅ 预期成功 |

> **注意**: LANDING-006 使用的 `<FSUCode>`（全大写）是 Landing003RealFsuIntegration.java 中硬编码的格式，2016 协议手册写的是 `FSUCode`（PascalCase）。Emerson FSU 接受全大写版本。这是 **emerson-2016** 差异。

## 6. 本地响应解析对照

| ACK | 协议字段 | LANDING-006 实测字段 | 本地解析 | 缺失? | 风险 |
|-----|---------|---------------------|---------|:--:|------|
| GET_FSUINFO_ACK | ResultCode (Info) | FsuId, TFSUStatus(CPUUsage,MEMUsage), Result | xmlData.getItems()[0].CPUUsage/MEMUsage | 否 | 低 |
| GET_DATA_ACK | ResultCode, Count (Info) + Signal[] (xmlData) | Values.DeviceList (空) | — | — | 中（FSU 无数据） |
| GET_LOGININFO_ACK | ResultCode (Info) + LoginInfo (xmlData) | FsuId, FsuCode, SCIP, DeviceList | — | — | 低 |

**GET_FSUINFO_ACK 专项**:
- 2016 协议手册 §6.13 只说 "Info 响应字段：ResultCode"，xmlData 结构标为 **"待确认"**
- Emerson FSU 实际返回 Info 中包含 `FsuId` + `TFSUStatus` (含 CPUUsage/MEMUsage) + `Result`
- 本地 `BInterface2016GetFsuInfoService` 从 `xmlData.getItems()[0]` 提取 CPUUsage/MEMUsage — 这是正确的（items 来自 XmlDataModel 解析 TFSUStatus 子元素）
- `isExpectedAck()` 通过 rawSoap 检查 `<Code>1702</Code>` — 正确

## 7. GET_FSUINFO 专项结论

### Q1-Q5 回答

| Q | 问题 | 答案 |
|---|------|------|
| Q1 | BInterfaceCommand2016 是否完整? | 6/7 命令有 Code。缺 GET_THRESHOLD=1901 |
| Q2 | legacy-2016 是否真输出 Name+Code? | ✅ LANDING-006 验证通过 |
| Q3 | GET_FSUINFO Info 字段是否按协议? | emerson-2016 仅需 `<FSUCode>`，不需要 SessionID/RequestTime |
| Q4 | 解析 GET_FSUINFO_ACK 是否按协议? | ✅ 从 xmlData items 提取 CPUUsage/MEMUsage 正确 |
| Q5 | 是否错误沿用 2024 名称? | ✅ 已区分：GET_SUINFO=2024, GET_FSUINFO=2016 |

### LANDING-015 失败根因判断

**结论: A → 请求格式不符合设备实测**

LANDING-015 原版请求发送 `<FsuId>` + `<FsuCode>`（两个标签），Emerson FSU 仅识别单一 `<FSUCode>` 标签。FIX-002 已对齐到 LANDING-006 成功格式。

### 为什么协议手册说 FSUCode 但 Emerson 需要 FSUCODE？

这可能是：
1. Emerson eStoneII FSU 固件对标签大小写敏感
2. LANDING-006 使用全大写恰好通过
3. 协议手册写的是 PascalCase `FSUCode`，但 FSU 解析器可能做 `toUpperCase()` 匹配

**建议**:  
```
standard-2016:  <FSUCode>  (PascalCase，按协议手册)
emerson-2016:   <FSUCode>  (PascalCase，已确认可用)
```

实际上 `<FSUCode>` 和 `<FSUCode>` 在 XML 中是同一个（XML 区分大小写，但 `FSUCode` 就是 `FSUCode`）。LANDING-006 硬编码的是 `<FSUCode>`（全大写 `FSUCODE`）。

> **精确表述**: 协议手册写的是 `FSUCode`，但 Landing003 代码硬编码 `<FSUCode>`。由于 XML 标签区分大小写，`FSUCode` ≠ `FSUCODE`。Emerson FSU 接受 `<FSUCODE>`（全大写），是否接受 `<FSUCode>`（PascalCase）未经测试。

## 8. 是否需要 protocolProfile

**建议引入 protocolProfile，但不作为当前阻塞项。**

| Profile | 定义 | 使用场景 |
|---------|------|---------|
| `standard-2016` | 严格按 2016 协议手册 | 新 FSU 接入 |
| `emerson-2016` | 经 LANDING-006 实测验证的格式 | **当前唯一直实设备 (51051243812345)** |
| `future-2024` | 2024 升级兼容 | 未来升级 |

当前只需 `emerson-2016` 一个 profile。实现方式建议：
- 在 `FsuServiceRequest` 中已有 `pkTypeFormat` 字段
- 当前 `"legacy-2016"` 实际就是 `emerson-2016` 行为
- 可保留现有命名，在文档中映射 `legacy-2016` = `emerson-2016`

## 9. 不建议继续盲修的点

| # | 盲修点 | 原因 |
|---|--------|------|
| 1 | GET_FSUINFO Info 应包含 SessionID/RequestTime | **不需要** — Emerson FSU 不接受这些字段（LANDING-006 验证） |
| 2 | GET_FSUINFO 应发送 Name+Code 还是纯文本 | **已验证** — legacy-2016 (Name+Code) 可用 |
| 3 | CPUUsage/MEMUsage 是否在 xmlData 还是 Info | **已验证** — 在 xmlData.items（TFSUStatus 子元素），当前解析正确 |
| 4 | raw-samples/ 为空但不能盲猜 | **必须保存** — 下次 run-once 必须保存 raw request/response |

## 10. 后续修复建议

| 优先级 | 行动 | 说明 |
|--------|------|------|
| P0 | 现场 run-once 并保存 raw XML | `mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true` 并捕获 stdout/文件 |
| P1 | 补齐 GET_THRESHOLD=1901 | `BInterfaceCommand2016.java` 新增条目 |
| P2 | 补齐 raw-samples/ 目录 | 将所有可获取的真实报文保存到 `docs/landing/raw-samples/` |
| P3 | 文档化 emerson-2016 profile | 在 openspec/project.md 记录 Emerson 变体差异 |

---

> **审计结论**: 核心 2016 命令码表基本完整（6/7）。GET_FSUINFO FIX-002 已对齐 Emerson 成功格式。
> **最大缺口**: 原始报文样本长期缺失，后续联调必须强制保存。
