# BIF-P4-011：2024 PK_Type Name+Code 与字段别名解析层集成

> 基于 BIF-P4-010 枚举和映射层
> 完成日期：2026-05-15

---

## 一、本阶段目标

将 BIF-P4-010 的命令枚举、命令别名、字段别名接入解析层（SoapMessageHandler + XmlDataParser），实现 2024 PK_Type Name+Code 双标识和字段别名归一化的自动解析。

---

## 二、BIF-P4-010 遗留问题解决

| 遗留 | 解决 |
|------|------|
| 别名未集成到解析层 | XmlDataParser.normalizeFields/normalizeItems |
| PK_Type 未支持 Name+Code | SoapMessageHandler.parsePkType() + PkTypeDescriptor |

---

## 三、新增模型

### PkTypeDescriptor

封装 PK_Type 双格式解析结果：

| 格式 | `<PK_Type>` 内容 | 解析 |
|------|-----------------|------|
| LEGACY_TEXT | `GET_DATA` | fromLegacyText() |
| NAME_CODE | `<Name>GET_DATA</Name><Code>501</Code>` | fromNameCode() |
| NAME_ONLY | `<Name>GET_DATA</Name>` | fromNameOnly() |
| CODE_ONLY | `<Code>501</Code>` | fromCodeOnly() |
| INVALID | 空/无法识别 | — |

### BInterfaceMessage 扩展

新增 `pkTypeDescriptor` 字段（不删除原有 `pkType`）。

---

## 四、Name+Code 校验策略

```
Name + Code 均存在:
  ├── Name 与 Code 一致 → NAME_CODE, consistent=true
  └── Name 与 Code 不一致 → NAME_CODE, consistent=false, warning

仅 Name:
  └── NAME_ONLY, warning

仅 Code:
  └── CODE_ONLY, warning (如未知)
```

---

## 五、字段别名归一化

XmlDataParser 新增方法：
- `normalizeFields(Map)` → 字段 key 归一化
- `normalizeItems(List<Map>)` → items 列表归一化

标准字段优先：同时存在 SUID 和 FSUCode 时，SUID 覆盖 FSUCode。

---

## 六、测试覆盖

| 测试类 | 数量 | 覆盖 |
|--------|------|------|
| PkTypeDescriptorTest | 20 | LEGACY_TEXT/NAME_CODE/NAME_ONLY/CODE_ONLY/兼容/边界 |
| SoapMessageHandlerPkType2024Test | 7 | document-style/RPC-style/不一致检测/向后兼容 |
| XmlDataParserFieldAliasTest | 13 | 字段归一化/标准优先/items/未知保留/null |
| **新增** | **40** | |

---

## 七、对现有业务影响

零影响。所有改动为新增字段/方法。旧 API 完全兼容。
