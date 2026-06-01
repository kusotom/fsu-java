# OpenSpec + Superpowers — FSU 动环监控平台

> 本目录用于保存 OpenSpec 规格和 Superpowers 执行计划。
> 最后更新：2026-05-20（SPEC-INIT-001）

---

## 1. 目录结构

```
openspec/
├── README.md              ← 本文件
├── project.md             ← 项目上下文（技术栈/协议/安全配置/当前状态）
├── specs/                 ← 已有能力的规格文档（每个能力一个 .md）
│   └── landing-008-passive-reporting-readiness.md
├── plans/                 ← 执行计划文档（每个任务一个 .md）
│   └── landing-008-passive-reporting-readiness-plan.md
├── changes/               ← 进行中的变更提案（需用户确认后执行）
└── archive/               ← 已完成并归档的变更提案
```

## 2. 核心原则

本目录遵循 **OpenSpec + Superpowers** 规格驱动开发工作流：

```
Spec（意图、边界、验收标准）
  → Plan（执行步骤）
    → 论证（基于 Spec/Plan）
      → 实现（TDD 先测试后代码）
        → 验证（verification-before-completion）
          → Memory 写入（执行结果 → docs/memory/）
            → Audit 写入（审计结论 → docs/audit/）
              → 下一阶段
```

### 2.1 每个任务必须先有 spec

spec 定义：
- **意图**：要达成什么
- **边界**：不做什么（防止范围蔓延）
- **验收标准**：完成条件（可量化、可验证）
- **风险**：已知风险和缓解措施

### 2.2 每个任务必须先有 plan

plan 定义：
- **执行步骤**：按顺序的操作清单
- **验证命令**：如何检查完成
- **输出文件**：预期的文档/代码/测试产物

### 2.3 Claude 执行前必须读取对应 spec / plan

Claude 每次执行任务前，必须先加载：
1. `openspec/project.md`（项目上下文）
2. `openspec/specs/<task>.md`（对应规格）
3. `openspec/plans/<task>.md`（对应计划）
4. `docs/memory/`（工程记忆）
5. `docs/audit/`（审计文档）

### 2.4 完成后必须写入 docs/memory

每次任务完成必须新增 memory 文件：
```
docs/memory/YYYY-MM-DD-任务编号-任务名称.md
```

并更新：
- `docs/memory/README.md`（索引）
- `docs/memory/WORKING-MEMORY.md`（当前状态）

### 2.5 Codex 审计时必须读取完整链路

Codex 后续审计时必须读取：
- `openspec/specs/`（规格）
- `openspec/plans/`（计划）
- `docs/memory/`（执行结果）
- `docs/audit/`（审计结论）

## 3. 四类文件定义

| 文件类型 | 位置 | 定义 | 写入时机 |
|----------|------|------|----------|
| **spec** | `openspec/specs/` | 意图、边界、验收标准 | 任务开始前 |
| **plan** | `openspec/plans/` | 执行步骤、验证命令 | 任务开始前 |
| **memory** | `docs/memory/` | 执行结果、关键决策 | 任务完成后 |
| **audit** | `docs/audit/` | 审计结论、协议一致性 | 完成后或审计时 |

## 4. 规格文件命名规则

- Spec 文件：`openspec/specs/<任务编号>-<简短描述>.md`
- Plan 文件：`openspec/plans/<任务编号>-<简短描述>-plan.md`
- Memory 文件：`docs/memory/YYYY-MM-DD-<任务编号>-<简短描述>.md`
- Audit 文件：`docs/audit/<任务编号>-<简短描述>.md`

## 5. 与现有规则的关系

- **最高规则**：`docs/rules/CLAUDE_PROJECT_RULES.md`（不可覆盖）
- **规格驱动层**：`docs/rules/CLAUDE_OPENSPEC_RULES.md`
- **本目录**：规格和计划的具体存放位置
- **不替代**：memory / audit / 工程规则
- **不绕过**：安全边界、协议唯一性、论证流程

## 6. 当前规格索引

| Spec | 状态 | 说明 |
|------|------|------|
| [landing-008-passive-reporting-readiness](specs/landing-008-passive-reporting-readiness.md) | 已创建 | B接口2016 被动上报接收联调准备 |

## 7. 当前计划索引

| Plan | 状态 | 说明 |
|------|------|------|
| [landing-008-passive-reporting-readiness-plan](plans/landing-008-passive-reporting-readiness-plan.md) | 已创建 | LANDING-008 执行计划 |
