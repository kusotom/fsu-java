---
name: bif-p4-003-single-fsu-readonly-integration-plan
description: 单台真实 FSU 只读联调计划 — 生成计划/白名单/黑名单/回滚方案/配置模板/检查清单，不访问真实设备
metadata:
  type: project
---

## 任务编号
BIF-P4-003

## 任务名称
单台真实 FSU 只读联调计划 / 联调脚本准备

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 本次任务目标
为单台真实 FSU 的只读联调生成完整计划、配置模板、只读命令白名单、禁止命令黑名单、回滚方案、日志脱敏规则和人工确认清单。本阶段不访问真实设备，不修改业务代码，不启用任何真实调用开关。

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/memory/README.md
- [x] BIF-P3-001 ~ BIF-P4-002 全部审计文档

## 架构判断
```
本阶段纯文档 + 测试，不修改业务代码。

白名单 (5):
  GET_DATA / GET_THRESHOLD / TIME_CHECK / GET_LOGININFO / GET_FTP
  → 已有 Handler + Service + Stub 实现

黑名单 (4):
  SET_POINT / SET_FTP / SET_FSUREBOOT / SET_THRESHOLD
  → SET_POINT/SET_FTP: notImplemented 桩
  → SET_FSUREBOOT: 无 Handler
  → SET_THRESHOLD: 安全门禁默认关闭
```

## B接口协议一致性判断
白名单命令均为 B接口 2016 标准只读命令。黑名单命令均为高风险/修改类命令。

## 修改前论证
- 为什么只做文档和测试：真实联调需要人员、设备、网络、授权，这些当前不具备
- 为什么现在做计划：BIF-P4-002 完成后所有前置能力已就绪，需要输出可执行的联调方案
- 为什么白名单严格限定 5 个命令：仅这 5 个命令完成了完整闭环（Handler/Service/Test/Stub）

## 写入前验证
- [x] 仅包含 GET 命令的白名单
- [x] 白名单不包含任何 SET_ 命令
- [x] 黑名单包含 SET_POINT/SET_FTP/SET_FSUREBOOT/SET_THRESHOLD
- [x] 黑白名单无交集
- [x] 文档不包含真实 IP/密码
- [x] 所有白名单命令有 Stub 支持
- [x] SET_POINT/SET_FTP 仍为 notImplemented 桩
- [x] 联调计划包含完整配置模板
- [x] 联调计划包含前/后检查清单
- [x] 联调计划包含日志脱敏要求
- [x] 回滚方案覆盖 6 种场景

## 实际新增/修改文件

### 新增（5 个）
| 文件 | 说明 |
|------|------|
| `docs/audit/BIF-P4-003-single-fsu-readonly-integration-plan.md` | 联调主计划，含配置模板/检查清单/步骤/脱敏/风险/禁止项 |
| `docs/audit/BIF-P4-003-readonly-command-whitelist.md` | 只读白名单(5) + 禁止黑名单(7) + 每项说明/风险/准入标准 |
| `docs/audit/BIF-P4-003-real-call-rollback-plan.md` | 回滚方案，6 场景/回滚步骤/回滚后验证/时间目标 |
| `backend/src/test/.../ReadOnlyIntegrationSafetyTest.java` | 23 个安全测试验证白名单/黑名单/SET 命令状态 |
| `docs/memory/2026-05-15-BIF-P4-003-single-fsu-readonly-integration-plan.md` | 操作记忆文件 |

### 修改（1 个）
| 文件 | 说明 |
|------|------|
| `docs/memory/README.md` | 新增 BIF-P4-003 索引条目 |

## 核心改动说明
1. **只读联调计划**：6 阶段联调步骤（网络确认→5 个只读命令），前后检查清单，日志脱敏规则，禁止项清单
2. **白名单/黑名单文档**：5 个只读命令 + 7 个禁止命令，含准入标准、风险等级、安全边界
3. **回滚方案**：6 种场景（网络不可达/响应异常/应用异常/配置错误/SET 误执行/忘记关闭），各有完整步骤
4. **ReadOnlyIntegrationSafetyTest (23 测试)**：白名单完整性(6) / 白名单不含 SET(6) / 黑名单完整性(4) / 无交集(1) / 黑名单不含 GET(1) / SET 桩状态(2) / Handler 存在性(1) / Stub 支持(1)

## 执行的测试命令
```
mvn test -Dtest="com.dcim.platform.binterface.**"
```

## 测试结果
```
Tests run: 758, Failures: 0, Errors: 0, Skipped: 0
```
较 BIF-P4-002 的 735 测试新增 23 个 ReadOnlyIntegrationSafetyTest。

## 是否修改业务代码
否。仅新增测试和文档。

## 是否涉及数据库
否。

## 是否涉及前端
否。

## 是否涉及 DSC/RDS
否。

## 是否访问真实设备
否。

## 是否启用真实 FSU 调用
否。real-call-enabled 默认 false。

## 是否启用 Scheduler
否。

## 是否执行 SET 类命令
否。

## 是否输出明文敏感信息
否。文档中所有 IP 为示例 IP，无密码。

## 风险点
| 风险 | 等级 | 说明 |
|------|------|------|
| 联调计划未经审核 | 低 | 执行前需团队 review |
| 配置模板中有遗漏 | 低 | 执行时按 checklist 逐项确认 |
| 回滚方案未演练 | 低 | 执行前可进行 dry-run |

## 遗留问题
1. 联调 FSU 设备、网络、人员尚未确认
2. 配置模板中 connect-timeout/read-timeout 需联调时调整
3. /services/FSUService 路径需真实 FSU 确认
4. 通讯录中联系人需联调前填写

## 建议下一步
进入真实联调执行阶段（需确认人员/设备/网络后）：
1. 创建联调分支
2. 按联调计划逐步执行
3. 先 GET_LOGININFO → TIME_CHECK → GET_DATA → GET_THRESHOLD → GET_FTP
4. 联调完成后关闭 real-call-enabled
5. 运行全量测试

## Git diff 摘要
无业务代码修改。新增 4 个文档 + 1 个测试文件。

## Git status 摘要
```
A  backend/src/test/.../ReadOnlyIntegrationSafetyTest.java
A  docs/audit/BIF-P4-003-readonly-command-whitelist.md
A  docs/audit/BIF-P4-003-real-call-rollback-plan.md
A  docs/audit/BIF-P4-003-single-fsu-readonly-integration-plan.md
A  docs/memory/2026-05-15-BIF-P4-003-single-fsu-readonly-integration-plan.md
M  docs/memory/README.md
```
