# OPENSPEC-001: OpenSpec 规格驱动层接入

## 审计日期
2026-05-20

## 审计类型
项目规范体系变更 / 文档接入（无业务代码修改）

## 1. 审计目标

将 OpenSpec 规格驱动层引入 FSU-JAVA 项目 Claude 工作流，作为"先规格、再设计、再任务、再实现"的规范驱动层。

## 2. 背景

当前项目论证→验证→写入流程中，"论证"步骤缺乏结构化的规格模板。任务背景、协议依据、非目标、回滚策略等信息分散在论证模板中，没有统一的 OpenSpec change 文档承载。引入 OpenSpec 可在大型/高风险变更前强制规格论证。

## 3. 修改范围

### 新增 (7 个文件)

| 文件 | 类型 |
|------|------|
| `docs/rules/CLAUDE_OPENSPEC_RULES.md` | 规则文件（11 节） |
| `openspec/project.md` | 项目上下文 |
| `openspec/specs/.gitkeep` | 目录占位 |
| `openspec/changes/.gitkeep` | 目录占位 |
| `openspec/archive/.gitkeep` | 目录占位 |
| `docs/memory/2026-05-20-OPENSPEC-001-claude-openspec-rules.md` | 操作记忆 |
| `docs/audit/OPENSPEC-001-claude-openspec-rules.md` | 本审计文件 |

### 修改 (3 个文件)

| 文件 | 改动类型 |
|------|----------|
| `docs/rules/CLAUDE_PROJECT_RULES.md` | 更新测试基线引用(1087→1164)；同步当前阶段为 LANDING-008；更新启动加载顺序；新增第21节 OpenSpec |
| `docs/memory/README.md` | 新增 OPENSPEC-001 索引条目 |
| `docs/memory/WORKING-MEMORY.md` | 同步阶段至 LANDING-008；新增 OPENSPEC-001 条目 |

### 未修改（明确排除）

- Java 业务代码（0 个文件）
- SQL 脚本（0 个文件）
- Vue / TS 前端代码（0 个文件）
- application.yml（0 个文件）
- 测试代码（0 个文件）
- 真实 FSU 联调代码（0 个文件）
- alarm_record 相关逻辑（0 个文件）
- Scheduler 相关代码（0 个文件）
- SET 相关代码（0 个文件）
- 父目录 `/home/tom/桌面/FSU/docs/`（0 个文件）

## 4. 协议一致性

本次不涉及 B接口协议。纯文档/规范任务。

## 5. 安全边界

| 边界项 | 状态 | 说明 |
|--------|------|------|
| 真实 FSU 访问 | 不涉及 | 纯文档任务 |
| SET 命令 | 不涉及 | — |
| Scheduler | 不涉及 | — |
| alarm_record 写入 | 不涉及 | — |
| 测试执行 | 不涉及 | 本次不运行 Maven 测试 |

## 6. OpenSpec 核心设计决策

### 6.1 规则优先级

```
1. CLAUDE_PROJECT_RULES.md       ← 最高
2. CLAUDE_OPENSPEC_RULES.md      ← 本次新增
3. openspec/project.md
4. openspec/specs/
5. docs/memory/
6. docs/audit/
```

冲突时以 `CLAUDE_PROJECT_RULES.md` 为准。后续如恢复 `PROJECT_ENGINEERING_RULES.md` 应重新评估。

### 6.2 强制 OpenSpec 场景（10 项）

1. B接口协议新增命令
2. B接口协议兼容改造
3. FSU 真实联调
4. 数据库结构改动
5. Entity / Repository / Service / Controller 改动
6. Scheduler 改动
7. SET 类操作
8. 告警 / 审计 / 持久化改动
9. 前端运维页面
10. Codex 审计修复任务

### 6.3 Change 模板（12 字段）

背景 → 目标 → 非目标 → 协议依据 → 架构判断 → 影响范围 → 安全边界 → 测试计划 → 验收标准 → 回滚策略 → memory 输出路径 → audit 输出路径

### 6.4 工作流

创建 Change → 用户确认 → 论证(基于Change) → 验证 → 写入 → 归档 → Memory/Audit 闭环

## 7. 修改原因

- 当前论证模板缺乏结构化的规格字段（非目标、回滚策略、验收标准等）
- 大型/高风险变更前缺乏统一的论证载体
- 需要明确的"写代码前停止点"防止 Claude 跳跃论证直接实现

## 8. 验证结果

| 验证项 | 结果 |
|--------|------|
| git status 仅含文档/规范文件 | ✅ |
| 无业务代码修改 | ✅ |
| 父目录 docs 未被修改 | ✅ |
| 规则优先级声明清晰 | ✅ |
| OpenSpec 定位不替代现有规则 | ✅ |
| 安全边界未触及 | ✅ |

## 9. 风险与遗留问题

### 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| 流程增加执行延迟 | 低 | 仅在 10 个特定场景触发 |
| 双重规则体系混淆 | 中 | 已明确 Java 项目 docs/ 为唯一规则源 |
| PROJECT_ENGINEERING_RULES.md 缺失 | 低 | 已备注，后续决策 |
| openspec/specs/ 空目录 | 低 | 后续逐个补充 18 个已有命令规格 |

### 遗留问题

1. `openspec/specs/` 未填充已有能力规格（18 个 CommandHandler）
2. 父目录过时 docs 副本未清理
3. `PROJECT_ENGINEERING_RULES.md` 在 Java 项目中缺失，后续需决策是否恢复

## 10. 结论

**通过。** OpenSpec 规格驱动层已成功接入 FSU-JAVA 项目规则体系。所有变更限于文档和规范文件，未触及业务代码。规则优先级清晰，安全边界完整。后续 Claude 执行 10 种强制场景任务时，必须先创建 OpenSpec change 并等待用户确认。
