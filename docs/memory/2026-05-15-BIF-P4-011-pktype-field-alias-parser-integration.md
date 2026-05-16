---
name: bif-p4-011-pktype-field-alias-parser-integration
description: PK_Type双格式解析 + 字段别名解析层集成 — PkTypeDescriptor模型, SoapMessageHandler双格式parse, XmlDataParser别名归一化, 40新测试, 912全量回归
metadata:
  type: project
---

## 任务编号
BIF-P4-011

## 任务名称
2024 PK_Type Name+Code 与字段别名解析层集成

## 操作时间
2026-05-15

## 核心改动
- PkTypeDescriptor: 双格式 PK_Type 解析模型 (5种format, Name+Code校验, 归一化)
- BInterfaceMessage: 新增 pkTypeDescriptor 字段
- SoapMessageHandler.parsePkType(): 自动识别 LEGACY_TEXT / NAME_CODE 格式
- XmlDataParser.normalizeFields/Items: 字段别名归一化, 标准字段优先

## 测试结果
- PkTypeDescriptorTest: 20/20 ✅
- SoapMessageHandlerPkType2024Test: 7/7 ✅
- XmlDataParserFieldAliasTest: 13/13 ✅
- 全量回归: 912 tests, 0 failures

## 是否修改业务代码
否。仅新增模型和扩展解析层。

## 建议下一步
BIF-P4-012: 为 2024 新增命令 (SUREADY, SET_SCIP 等) 创建 Handler 骨架
