# SET_FSUREBOOT

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | SET_FSUREBOOT |
| 请求 Code | 1801 |
| 响应名称 | SET_FSUREBOOT_ACK |
| 响应 Code | 1802 |
| 通信方向 | SC -> FSU |
| 协议版本 | B接口2016 |
| 命令类别 | 控制/运维 |
| 当前实现状态 | 安全禁用 |
| 安全等级 | 控制类高风险 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248 |
| 原文摘要 | SC发起FSU重启；用于升级等操作，SC先通过FTP上传升级文件，再发重启使FSU重启后自动升级。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-SET-FSUREBOOT-001 | SET_FSUREBOOT请求命令码1801，SET_FSUREBOOT_ACK响应命令码1802 | 协议规定 |
| SPEC-2016-CMD-SET-FSUREBOOT-002 | 重启命令返回成功标志后FSU重启 | 协议规定 |
| SPEC-2016-CMD-SET-FSUREBOOT-003 | 自动升级流程可通过FTP/USB上传升级文件后重启完成 | 协议规定 |
| SPEC-2016-SECURITY-SET-FSUREBOOT-001 | SET_FSUREBOOT为远程重启高风险命令，当前阶段默认禁用 | 工程解释 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-SET-FSUREBOOT-001] SET_FSUREBOOT请求命令码1801，SET_FSUREBOOT_ACK响应命令码1802

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248  
原文摘要：SC发起FSU重启；用于升级等操作，SC先通过FTP上传升级文件，再发重启使FSU重启后自动升级。  
规则内容：SET_FSUREBOOT请求命令码1801，SET_FSUREBOOT_ACK响应命令码1802。  
适用范围：SET_FSUREBOOT / SET_FSUREBOOT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SET-FSUREBOOT-002] 重启命令返回成功标志后FSU重启

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248  
原文摘要：SC发起FSU重启；用于升级等操作，SC先通过FTP上传升级文件，再发重启使FSU重启后自动升级。  
规则内容：重启命令返回成功标志后FSU重启。  
适用范围：SET_FSUREBOOT / SET_FSUREBOOT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SET-FSUREBOOT-003] 自动升级流程可通过FTP/USB上传升级文件后重启完成

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248  
原文摘要：SC发起FSU重启；用于升级等操作，SC先通过FTP上传升级文件，再发重启使FSU重启后自动升级。  
规则内容：自动升级流程可通过FTP/USB上传升级文件后重启完成。  
适用范围：SET_FSUREBOOT / SET_FSUREBOOT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### 安全引用：SPEC-2016-SECURITY-SET-FSUREBOOT-001 SET_FSUREBOOT为远程重启高风险命令，当前阶段默认禁用

规则类型：工程解释  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248  
原文摘要：SC发起FSU重启；用于升级等操作，SC先通过FTP上传升级文件，再发重启使FSU重启后自动升级。  
规则内容：SET_FSUREBOOT为远程重启高风险命令，当前阶段默认禁用。  
适用范围：SET_FSUREBOOT / SET_FSUREBOOT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<Request>
  <PK_Type><Name>SET_FSUREBOOT</Name><Code>1801</Code></PK_Type>
  <Info><FsuId/><FsuCode/></Info>
</Request>
```

## 5. 响应 XML 结构

```xml
<Response>
  <PK_Type><Name>SET_FSUREBOOT_ACK</Name><Code>1802</Code></PK_Type>
  <Info><FsuId/><FsuCode/><Result/></Info>
</Response>
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| FsuId/FSUID | char[FSUID_LEN] | 是 | 表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248 | FSU ID，表格写FSUID，样例FsuId |  |
| FsuCode | char[FSUCODE_LEN] | 是 | 表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248 | FSU编码 |  |
| Result | EnumResult | 是 | 表38-39，抽取稿 L3111-L3208；流程 L632-L642；自动升级 L3245-L3248 | 成功/失败 |  |

## 7. 处理规则

平台处理 `SET_FSUREBOOT` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

## 8. 成功条件

- 请求Code与ACK Code符合本文件。
- XML层级符合Request/Response + PK_Type + Info。
- 响应中如有Result，则按EnumResult解释：1=SUCCESS，0=FAILURE。
- 对待确认字段仅做兼容解析，不写成标准强约束。

## 9. 失败条件

- PK_Type Name或Code不匹配。
- 必填字段缺失且无法按明确兼容规则解析。
- Result=0。
- XML格式非法、SOAP Fault、业务根节点错误。

## 10. 平台字段映射

| 协议字段 | 平台字段 | 数据表 / DTO / 实体 | 说明 |
|---|---|---|---|
| FsuId/FSUID | `fsuId/FSUID` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuCode | `fsuCode` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Result | `result` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| XML-2016-029 | matrices/xml-sample-index.md | 原文样例索引 |
| XML-2016-030 | matrices/xml-sample-index.md | 原文样例索引 |

## 12. 已知厂商差异

未执行Emerson SET_FSUREBOOT。

## 13. 安全边界

默认禁用；必须显式授权、二次确认、操作审计、raw报文保存，禁止默认调度执行。

## 14. 测试要求

- fixture：标准请求、标准响应、缺字段、非法Code、Result=0/1。
- 单元测试：解析、构造、字段大小写、通配符和UNKNOWN分支。
- 集成测试：SOAP invoke(xmlData) envelope、raw message保存。
- 真实FSU测试：默认禁用，必须显式开关；SET类禁止真实执行。

## 15. 实现检查清单

- [ ] 命令码正确
- [ ] ACK 码正确
- [ ] XML 层级正确
- [ ] 字段大小写正确
- [ ] 必填字段校验
- [ ] 错误返回处理
- [ ] raw message 留痕
- [ ] 单元测试覆盖
- [ ] 集成测试覆盖
- [ ] 如涉及真实 FSU，保存 raw request/response
