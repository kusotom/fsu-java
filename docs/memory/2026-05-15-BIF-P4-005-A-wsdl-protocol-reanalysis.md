---
name: bif-p4-005-a-wsdl-protocol-reanalysis
description: BIF-P4-005-A WSDL与B接口协议原文再审计 — 确认RPC style/encoded use/operation=invoke, Fault归因于document-style <Request>不匹配WSDL RPC约定
metadata:
  type: project
---

## 任务编号
BIF-P4-005-A

## 任务名称
WSDL 与 B接口协议原文再审计

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md
- [x] 已读取 6 个 WSDL 文件（主依据2 + 辅助4）
- [x] 已读取 B接口协议 2016 手册 §1-§4
- [x] 已读取 BIF-P4-003 联调计划
- [x] 已读取 BIF-P4-004 联调记忆

## 任务目标
不写代码，纯审计。基于 BIF-P4-004 真实 FSU 联调 SOAP Fault，重新分析 WSDL 和协议原文，确认：
1. FSU 期望的 SOAP Body 第一层节点
2. operation 名称和 namespace
3. 当前项目 SOAP 构造与 WSDL 的差异
4. Fault 根因
5. 推荐适配方案

## 架构判断
- 纯审计，不修改业务代码
- 不访问真实设备
- 不启用任何安全开关

## 关键发现

### WSDL 分析
| 字段 | FSUService | SCService |
|------|-----------|-----------|
| targetNamespace | http://FSUService.chinatowercom.com | http://SCService.chinatowercom.com |
| operation | **invoke** (仅1个) | **invoke** (仅1个) |
| style | **rpc** | **rpc** |
| use | **encoded** | **encoded** |
| SOAPAction | **""** (空) | **""** (空) |
| 参数 | xmlData (soapenc:string) | xmlData (soapenc:string) |
| 返回 | invokeReturn (soapenc:string) | invokeReturn (soapenc:string) |

### Fault 归因
- FSU 的 SOAP 引擎（gSOAP）按 RPC 约定解析 Body 第一子元素为 operation 名
- 当前 SoapMessageHandler 输出 `<Request>` 作为 Body 第一子元素
- WSDL 中只有 operation=`invoke`，无 `Request`
- FSU 返回 `Method 'Request' not implemented` **完全合理**

### 正确 SOAP 结构
```
SOAP-ENV:Body
  ns1:invoke (http://FSUService.chinatowercom.com)
    xmlData (xsi:type="SOAP-ENC:string")
      Request     ← payload 根元素
        PK_Type
        Info
        xmlData
```

### WSDL vs 协议手册
协议手册 §4 描述的是 **逻辑结构**（payload层），不是 SOAP **线格式**（wire format）。二者不冲突，但线格式必须服从 WSDL。

## 推荐方案
**方案 A：新增 FsuServiceRpcAdapter**（推荐）
- 只影响 FSUService 出站请求
- 不修改 SoapMessageHandler
- 不影响现有测试/fixtures/stub
- 低风险

## 后续任务
1. BIF-P4-005-B-1：新增 FsuServiceRpcAdapter
2. BIF-P4-005-B-2：修改 RealHttpFsuServiceClient
3. BIF-P4-005-B-3：新增 RPC 测试套件
4. BIF-P4-005-B-4：重新联调真实 FSU
5. BIF-P4-006：SCService 入站 RPC 兼容评估

## 实际新增/修改文件
### 新增（1 个）
| 文件 | 说明 |
|------|------|
| `docs/audit/BIF-P4-005-A-wsdl-protocol-reanalysis.md` | WSDL/协议再审计文档 |

### 修改（0 个）
无。纯审计，未修改任何业务代码。

## 是否修改业务代码
否

## 是否访问真实设备
否

## 是否启用真实 FSU 调用
否

## 是否执行 SET 类命令
否

## 遗留问题
1. B接口协议 2016 原文 (.docx/.pdf) 未逐字比对
2. SCService 入站 RPC 兼容性未评估
3. FSU 响应 payload 结构与协议手册可能存在差异

## 建议下一步
BIF-P4-005-B：实施方案 A — 创建 FsuServiceRpcAdapter，重新联调真实 FSU
