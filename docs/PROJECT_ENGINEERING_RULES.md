# 工程规则 — FSU 动环监控平台

> 本文件为项目级工程规则入口，供 Claude、Codex、AI Agent 启动时加载。
> 如与 `docs/rules/CLAUDE_PROJECT_RULES.md` 冲突，以 `CLAUDE_PROJECT_RULES.md` 为准。
> 最后更新：2026-05-21 (PRE-FIX-001)

---

## 1. 规则优先级

```
1. docs/rules/CLAUDE_PROJECT_RULES.md    ← 最高优先级（21节）
2. docs/rules/CLAUDE_OPENSPEC_RULES.md   ← OpenSpec 规格驱动层
3. docs/PROJECT_ENGINEERING_RULES.md     ← 本文件（工程规则入口）
4. .agent/policies.yml                   ← Agent 协作策略与安全边界
5. .agent/agents.yml                     ← 6-Agent 角色定义与权限
6. openspec/project.md                   ← 项目上下文
7. docs/memory/README.md                 ← 记忆索引
8. docs/memory/WORKING-MEMORY.md         ← 当前工作记忆
9. docs/audit/                           ← 审计文档
10. openspec/protocols/binterface-2016/  ← 协议规范文件
```

## 2. 协议基线

- B接口2016 = 当前主开发依据 / 真实落地协议
- B接口2024 = 未来升级版本 / 可选兼容层
- 严禁重新引入 DSC/RDS 作为业务主协议

## 3. 代码修改流程

每次代码修改前必须输出修改前论证（见 CLAUDE_PROJECT_RULES.md §13）。

## 4. 安全边界

- 默认禁止真实 FSU 访问（`real-call-enabled: false`）
- 默认禁止 Scheduler（`scheduler-enabled: false`）
- 默认禁止 SET 类命令
- 默认禁止自动修改 alarm_record

## 5. 测试要求

- 默认 mvn test 不访问真实 FSU
- 真实 FSU 测试必须 @Disabled + @Tag("real-fsu") + @EnabledIfSystemProperty
- 全量测试目标: 0 failures, 0 errors

## 6. 记忆与审计

- 每次操作必须写入 docs/memory/
- 涉及协议/安全/联调必须写入 docs/audit/
- 任务未写入记忆文件和更新索引前，视为未闭环

## 7. Agent 工程流系统 (AI-FLOW-001-REV2)

项目采用 6-Agent 分工模型，由 `.agent/` 配置系统管理。

### Agent 分工

| 组 | Agent | 职责 |
|----|-------|------|
| GPT | GPT-Architect | 总规划/架构决策/阶段路线/任务拆分 |
| GPT | GPT-Protocol-Agent | B接口2016 协议专家 |
| Claude | Claude-Developer | 默认主开发/前后端/dry-run |
| Claude | Claude-Memory-Agent | 记忆/audit/WORKING-MEMORY/context-pack |
| Codex | Codex-Reviewer | 默认审计/代码审计/安全边界检查 |
| Codex | Codex-Deploy-Agent | 部署运维/Ubuntu/PostgreSQL/Nginx |

### 核心规则

- GPT 负责规划、架构、协议判断和阶段决策，不直接改本地代码
- Claude 是默认主开发 Agent，同时担任 Memory-Agent
- Codex 是默认审计 Agent，同时担任 Deploy-Agent
- 所有任务通过 `.agent/tasks/*.yml` 管理
- 任务闭环前必须更新 memory/audit/WORKING-MEMORY

### 工程流配置文件

- `.agent/agents.yml` — Agent 定义与权限
- `.agent/policies.yml` — 协作策略与安全边界
- `.agent/workflow.yml` — 工作流配置
- `.agent/locks.yml` — 任务锁管理
- `docs/ai-team/` — AI Team 完整文档 (10 文件)
