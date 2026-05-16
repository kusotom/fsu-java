---
name: bif-p4-008-scservice-rpc-compatibility
description: SCService入站RPC兼容性评估 — parse()已双向兼容, buildResponse()待FSU入站联调确认, 7个RPC解析测试通过
metadata:
  type: project
---

## 任务编号
BIF-P4-008

## 任务名称
SCService 入站 RPC 兼容性评估

## 操作时间
2026-05-15

## 规则加载确认
- [x] AGENTS.md, CLAUDE.md, PROJECT_ENGINEERING_RULES.md
- [x] SCService.wsdl 分析完成

## 核心结论

**入站解析（parse）**: ✅ 已双向兼容（BIF-P4-005-B）
- RPC invoke(xmlData) 格式 → 正确提取内层 Request
- RPC invokeResponse(invokeReturn) 格式 → 正确提取内层 Response
- Document-style → 仍支持

**出站响应（buildResponse）**: ⚠️ 仍为 document-style
- 缺少 RPC wrapper (<ns1:invokeResponse><invokeReturn>)
- 暂不修改，待真实 FSU 入站联调确认需要后实施

**建议**: 暂不实现 ScServiceRpcAdapter。触发条件为真实 FSU 入站联调时 FSU 拒绝 document response。

## 测试结果
- ScServiceRpcCompatibilityTest: 7/7 ✅
- 全量回归: 798 tests, 0 failures, 1 pre-existing Error

## 实际新增文件
| 文件 | 说明 |
|------|------|
| `docs/audit/BIF-P4-008-scservice-rpc-compatibility.md` | 评估报告 |
| `ScServiceRpcCompatibilityTest.java` | 7 个 RPC 解析测试 |
| `docs/memory/2026-05-15-BIF-P4-008-scservice-rpc-compatibility.md` | 操作记忆 |

## 是否修改业务代码
否。

## 是否访问真实设备
否。

## 遗留问题
1. buildResponse() 不兼容 RPC，待 FSU 入站联调确认
2. SOAP Fault 在 RPC 响应中的位置待确认

## 建议下一步
等待 FSU 点位表 → BIF-P4-007-B (seed SQL)
或 FSU 入站联调 → BIF-P4-010 (buildRpcResponse 实施)
