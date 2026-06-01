# 08 — XML 格式兼容

## 1. PK_Type 格式演进

| 格式 | 示例 | 来源 | 项目支持 |
|------|------|------|---------|
| 纯文本 | `<PK_Type>LOGIN</PK_Type>` | 2016 标准 | ✅ |
| Name+Code (2024) | `<Name>LOGIN</Name><Code>501</Code>` | 2024 增强 | ✅ |
| Name+Code (2016) | `<Name>LOGIN</Name><Code>101</Code>` | legacy-2016 | ✅ |

## 2. pkTypeFormat 参数

| 值 | PK_Type 格式 | Code 来源 | 用途 |
|----|-------------|-----------|------|
| `structured` | Name+Code | 2024 码表 | 默认 |
| `legacy-text` | 纯文本 | 无 | 旧 FSU |
| `legacy-2016` | Name+Code | 2016 码表 | Emerson FSU |

当前项目映射: `legacy-2016` = `emerson-2016`（LANDING-006 验证）。

## 3. Info 标签大小写兼容

| 协议原文 | Emerson 实测 | 项目使用 |
|---------|-------------|---------|
| `FSUCode` (PascalCase) | `FSUCode` (全大写) | 按实测 |

协议手册 §8.1 写的是 `FSUCode`。Emerson eStoneII FSU 接受全大写 `FSUCode`。LANDING-006/015 使用全大写成功。

PascalCase 格式 (`<FsuCode>`) 未经测试。

## 4. 嵌套 XML 声明

FSU 返回的 xmlData 可能包含内嵌 `<?xml version="1.0"?>` 声明。

平台处理: `FsuServiceRpcAdapter.stripXmlDeclaration()` 容错（LANDING-004）。

## 5. ACK 响应格式

- SC 入站 ACK: 使用与请求同名的 PK_Type（非 `_ACK` 后缀）
- SC 入站 ACK: 纯文本 PK_Type
- FSU 出站查询: Name+Code 格式

## 6. 兼容注意事项

| 场景 | 处理 |
|------|------|
| 2016 vs 2024 Code 冲突 | 2016 优先 |
| 标签大小写 | 使用 Emerson 实测格式 |
| 多余标签 | FSU 可能拒绝（如 FsuId+FsuCode） |
| 缺失标签 | FSU 返回空响应 |
