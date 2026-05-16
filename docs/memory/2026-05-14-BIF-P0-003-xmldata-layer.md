---
name: BIF-P0-003-xmldata-layer
description: 创建 XMLData 解析与构造统一层（XmlDataParser/Builder/Validator），重构测试至 77 个用例
metadata:
  type: project
---

# BIF-P0-003：XMLData 解析与构造统一层

> 对应阶段：BIF-P0-003
> 完成日期：2026-05-14
> 影响范围：XmlDataParser / XmlDataBuilder / XmlDataValidator / XmlDataModelTest

**关联记忆：** [[BIF-P0-001-command-mapping]], [[BIF-P0-002-soap-wsdl]], [[RULE-001-operation-memory-rule]]

---

## 任务目标

在 SOAP 层（BIF-P0-002）与业务 Handler 层（BIF-P1）之间建立统一的 XMLData 解析/构造/校验基础设施。支持 B-Interface 2016 协议中所有命令类型的 xmlData 结构。

## 架构判断

所属层次：XMLData 处理层（SOAP 层之上、业务 Handler 之下）。
模块：`com.dcim.platform.module.binterface.xml`

解析策略基于 xmlData 子元素的四种模式：
1. **平坦字段**（HEARTBEAT）→ fields 映射
2. **重复结构**（Signal[]）→ items 列表
3. **叶子重复**（SignalID[]）→ items 自引用条目
4. **单层包裹**（DeviceInfo/FTPConfig/LoginInfo）→ rootName + 单个 item

## 协议一致性判断

完全符合 B接口协议 2016。所有 xmlData 结构严格按 24 个 fixture 定义解析和构造，未扩展协议字段。

## 修改前论证

**Why XmlDataParser/Builder/Validator 分开？**
- 单一职责：解析（读）、构造（写）、校验（检查）各司其职
- 可测试性：每个组件可独立测试
- 与 SoapMessageHandler 对称：SOAP 层也分离了 parse/build

**Why DOM API？**
- 与 SoapMessageHandler 使用相同的 JDK DOM API，无额外依赖
- B-Interface XML 结构简单（最大深度 3 层），DOM 足够
- 无需流式解析（SAX/StAX）的复杂度

**Why fields+items 而非命令专用模型？**
- 所有命令的 xmlData 可归纳为 4 种结构模式
- 命令专用模型会引入大量重复代码，且每新增命令都需要新模型
- 通用模型 + 命令校验的组合更灵活

## 写入前验证

| 验证项 | 结果 |
|--------|------|
| 24 个 xmldata fixture 全部可解析 | 通过 |
| 各命令类型解析断言（字段值/条目数/rootName） | 通过 |
| 构造→解析往返一致性 | 通过（4 种模式各验证） |
| 字段大小写兼容（FSUCode/fsucode 等） | 通过 |
| 空 xmlData 处理（null/空字符串/自闭合） | 通过（12 用例） |
| 畸形 XML 错误处理 | 通过 |
| 校验器（SignalID 为空检测） | 通过 |
| SoapMessageHandler 未引入回归 | 通过（71 测试） |
| 全部 173 B-interface 测试通过 | 通过 |

## 实际修改文件

### 新增文件 (3)

- `src/main/java/com/dcim/platform/module/binterface/xml/XmlDataParser.java` — DOM 解析器
- `src/main/java/com/dcim/platform/module/binterface/xml/XmlDataBuilder.java` — XML 构造器
- `src/main/java/com/dcim/platform/module/binterface/xml/XmlDataValidator.java` — 内容校验器

### 修改文件 (1)

- `src/test/java/com/dcim/platform/binterface/XmlDataModelTest.java` — 10 占位测试 → 77 真实测试

### 文档 (2)

- `docs/audit/BIF-P0-003-xmldata-layer.md` — 审计文档
- `docs/memory/2026-05-14-BIF-P0-003-xmldata-layer.md` — 本记忆文件

## 核心改动

1. **XmlDataParser**: 基于 JDK DOM 的智能结构识别解析器，自动区分 4 种 xmlData 模式
2. **XmlDataBuilder**: 根据 XmlDataModel 的 rootName/fields/items 状态选择构造策略
3. **XmlDataValidator**: 非侵入式校验，返回结构化 ValidationResult，不抛异常
4. **XmlDataModelTest**: 77 个测试覆盖全部 24 个 fixture + 字段兼容 + 往返 + 错误处理 + 校验

## 测试命令和结果

```
mvn test -Dtest='*XmlData*,*Soap*,*BInterface*,*Wsdl*'
Tests run: 173, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 风险

- XmlDataParser 的 DOM 解析需要 xmlData 内容本身是格式良好的 XML 片段。畸形内容捕获为解析错误（valid=false），不抛异常到上层。
- 新命令若引入 4 种模式之外的 xmlData 结构，需扩展解析策略。当前 B-Interface 2016 所有命令均在 4 种模式内。

## 遗留问题

无（P0-003 范围内已闭环）。

## 下一步建议

1. **BIF-P0-004** — 完成 CommandDispatcher（Handler 注册/路由, UNKNOWN 兜底）
2. **BIF-P1-001~004** — LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM 业务逻辑
3. 补充 missing expected JSON（SET_FTP req/resp, GET_LOGININFO req/resp）

## git diff 摘要

无 git 仓库托管本次变更。新增/修改文件统计：

```
新增: XmlDataParser.java (~150 行)
新增: XmlDataBuilder.java (~80 行)
新增: XmlDataValidator.java (~120 行)
修改: XmlDataModelTest.java (10→77 测试)
新增: docs/audit/BIF-P0-003-xmldata-layer.md
新增: docs/memory/2026-05-14-BIF-P0-003-xmldata-layer.md
```

## git status 摘要

项目非 git 仓库，以文件变更记录替代。
