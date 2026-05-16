# BIF-P0-003：XMLData 解析与构造统一层

> 对应阶段：BIF-P0-003
> 完成日期：2026-05-14
> 影响范围：XmlDataModel / XmlDataParser / XmlDataBuilder / XmlDataValidator / XmlDataModelTest (77 测试)

---

## 一、架构设计

### 1.1 层次定位

XMLData 层位于 SOAP 层与业务 Handler 层之间：

```
SOAP Envelope (SoapMessageHandler)
  ├── 解析 xmlData 为原始 XML 字符串
  ├── 传递原始 XML 字符串给 XmlDataParser
  ↓
XmlDataParser (新增)
  ├── 解析 XML → XmlDataModel（fields + items）
XmlDataModel (已重构)
  ├── 承载解析结果
XmlDataBuilder (新增)
  ├── 从 XmlDataModel → XML 字符串
XmlDataValidator (新增)
  ├── 校验 XmlDataModel 内容规则
  ↓
业务 Handler (BIF-P1)
```

### 1.2 xmlData 结构类型分析

通过对 24 个 xmldata fixture 的全量分析，归纳为 4 种结构模式：

| 模式 | 特征 | 示例 | rootName | fields | items |
|------|------|------|----------|--------|-------|
| 平坦字段 | 多子元素，标签名混合 | HEARTBEAT 请求 | null | {CPU→35, Memory→62, ...} | [] |
| 重复结构 | 多子元素，标签名相同，有子元素 | SEND_DATA 请求 Signal[] | "Signal" | {} | [{SignalID→TEMP-001, Value→25.5, ...}, ...] |
| 叶子重复 | 多子元素，标签名相同，无子元素 | GET_DATA 请求 SignalID[] | "SignalID" | {} | [{SignalID→TEMP-001}, {SignalID→HUMI-001}, ...] |
| 单层包裹 | 单一子元素，有子元素 | LOGIN 请求 DeviceInfo | "DeviceInfo" | {} | [{Manufacturer→中兴, Model→eStone II, ...}] |

## 二、新增文件

### 2.1 XmlDataParser.java

**路径：** `src/main/java/com/dcim/platform/module/binterface/xml/XmlDataParser.java`

**核心策略：**

1. 使用 JDK DOM API（与 SoapMessageHandler 一致），`setNamespaceAware(false)`
2. 输入为 xmlData 内部 XML（由 SoapMessageHandler 提取的字符串）
3. 用合成根 `<wrap>` 包裹以支持多顶层元素（HEARTBEAT 的 CPU/Memory 等）
4. 子元素数量 = 1 且该元素有子元素 → **包裹模式**（items 含 1 条目，rootName=包裹标签名）
5. 多子元素且标签名全部相同 → **重复模式**（items，rootName=标签名）
   - 子元素有子元素 → 结构化重复（Signal{SignalID,Value,Quality,Status}）
   - 子元素为叶子 → 叶子重复（SignalID[]）
6. 多子元素且标签名混合 → **平坦字段模式**（fields）
7. 解析失败 → set valid=false, addError()

**快捷方法：**
- `parseFields(xml)` — 直接返回 fields 映射
- `parseItems(xml)` — 直接返回 items 列表

### 2.2 XmlDataBuilder.java

**路径：** `src/main/java/com/dcim/platform/module/binterface/xml/XmlDataBuilder.java`

**构造策略：**

1. items 优先于 fields
2. items 模式下：
   - 叶子重复（rootName 是 item 的唯一键）→ 输出 `<rootName>value</rootName>`
   - 结构化重复（rootName 不是 item 的键）→ 输出 `<rootName><field1>v1</field1>...</rootName>`
3. fields 模式下：每个 field 输出为叶子元素
4. XML 特殊字符转义（& < > " '）
5. 空 model → 返回空字符串

### 2.3 XmlDataValidator.java

**路径：** `src/main/java/com/dcim/platform/module/binterface/xml/XmlDataValidator.java`

**校验规则：**

- model 非空（null 返回 error）
- model 解析有效（isValid = true）
- SignalID 不可为空或空白
- items 条目不可为空映射
- FSUCode / ResultCode **不在此层校验**（它们在 Info 层，由上层 BInterfaceMessage 校验）

**返回结构：** `ValidationResult` — errors + warnings 列表，`isPassed()` = errors.isEmpty()

## 三、字段大小写兼容

统一由 `XmlDataModel.getField(name)` 处理，存储时保留原始标签名，查找时精确匹配优先，然后大小写不敏感遍历匹配。

支持的兼容组合：
- FSUCode / FsuCode / fsucode
- DeviceID / DeviceId / deviceid
- SignalID / SignalId / signalid
- ResultCode / resultCode / RESULTCODE

## 四、测试覆盖

### 4.1 测试统计

| 类别 | 测试数 | 说明 |
|------|--------|------|
| 解析测试 | 16 | 各命令类型的 xmlData 解析断言 |
| 空 xmlData 测试 | 12 | 全部 11 个空 xmlData fixture + null/空字符串 |
| 字段兼容测试 | 3 | 大小写不敏感、默认值、存在性 |
| 构造测试 | 7 | fields/items/叶子/包裹/空 model 构造 |
| 往返测试 | 4 | 解析→构造→再解析一致性 |
| 错误处理测试 | 3 | 畸形 XML、纯文本、注释 |
| 快捷方法测试 | 5 | getFsuCode/getDeviceId 等 |
| 解析器快捷方法 | 2 | parseFields/parseItems |
| 校验测试 | 5 | 通过/空 model/解析失败/SignalID 空 |
| Fixture 存在性 | 24 | 所有 xmldata fixture 可加载解析 |
| **合计** | **77** | |

### 4.2 测试结果

```
Tests run: 77, Failures: 0, Errors: 0, Skipped: 0  (XmlDataModelTest)
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0  (SoapMessageHandlerTest)
Tests run: 7,  Failures: 0, Errors: 0, Skipped: 0  (BInterfacePkTypeTest)
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0  (BInterfaceInvalidMessageTest)
Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0  (WsdlEndpointTest)
Total: 173 test cases, all pass.
```

## 五、集成边界

### 5.1 SoapMessageHandler 集成

SoapMessageHandler.parse() 提取 xmlData 为原始字符串 → XmlDataParser.parse() → XmlDataModel

```java
// 当前集成方式（SoapMessageHandlerTest 已验证）
BInterfaceMessage msg = handler.parse(soapXml);
XmlDataModel model = parser.parse(msg.getXmlData());
```

### 5.2 CommandDispatcher 集成留待 P0-004

XmlDataModel 作为业务 Handler 的入参和出参，在 CommandDispatcher 层完成转换。

## 六、协议一致性

- 所有 xmlData 结构严格按照 24 个 fixture 定义，未扩展协议字段
- 未引入 DSC/RDS 相关逻辑
- 未使用私有 JSON 代替 XML
- 未自造 MsgType/SignalID/DeviceID/FsuCode/ResultCode

## 七、风险评估

| 风险 | 概率 | 缓解措施 |
|------|------|----------|
| 新命令引入非标准 xmlData 结构 | 低 | 解析器自动适配，只要符合 4 种模式即可 |
| 字段大小写兼容遗漏 | 低 | 统一由 getField() 处理，新增字段自动继承 |
| 性能（DOM 解析重复调用） | 低 | XMLData 层无高频调用场景 |

## 八、遗留问题

- 无（P0-003 范围内已闭环）

## 九、下一步建议

1. **BIF-P0-004**：CommandDispatcher 实现（已有部分代码，需完成）
2. **BIF-P1-001~004**：各业务 Handler 实现（LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM）
3. **补充 missing expected JSON**：SET_FTP req/resp、GET_LOGININFO req/resp

---

## 附录：文件清单

### 新增文件 (3)

| 文件 | 行数 | 说明 |
|------|------|------|
| `xml/XmlDataParser.java` | ~150 | xmlData DOM 解析器 |
| `xml/XmlDataBuilder.java` | ~80 | xmlData XML 构造器 |
| `xml/XmlDataValidator.java` | ~120 | xmlData 校验器 |

### 修改文件 (1)

| 文件 | 变更 |
|------|------|
| `XmlDataModel.java` | 已在前一阶段完成重构，本阶段未修改 |
| `XmlDataModelTest.java` | 从 10 个占位测试 → 77 个真实测试 |
