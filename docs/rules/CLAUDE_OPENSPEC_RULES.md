# CLAUDE_OPENSPEC_RULES.md — OpenSpec 规格驱动层规则

> 本文件定义 Claude 在 FSU-JAVA 项目中使用 OpenSpec 规格驱动层的规则。
> OpenSpec 定位为任务规格化层，不替代现有工程规则。
> 最后更新：2026-05-20（OPENSPEC-001）

---

## 1. OpenSpec 定位

OpenSpec 是 Claude 工作流中的"规格驱动层"，嵌入现有论证→验证→写入流程：

```
OpenSpec Change → 论证(基于Change) → 验证 → 写入 → Audit/Memory闭环
```

OpenSpec 提供结构化的任务规格模板，确保每次代码修改前已完成充分的规格论证。

**OpenSpec 不替代、不覆盖、不绕过以下规则文件：**

- `docs/rules/CLAUDE_PROJECT_RULES.md`（本项目事实上的最高规则文件）
- `docs/memory/` 工程记忆体系
- `docs/audit/` 审计文档体系

如本文件与 `docs/rules/CLAUDE_PROJECT_RULES.md` 冲突，以 `CLAUDE_PROJECT_RULES.md` 为准。

---

## 2. 规则优先级

```
1. docs/rules/CLAUDE_PROJECT_RULES.md    ← 最高优先级
2. docs/rules/CLAUDE_OPENSPEC_RULES.md   ← 本文件
3. openspec/project.md                   ← 项目 OpenSpec 上下文
4. openspec/specs/                       ← 已有能力规格
5. docs/memory/                          ← 操作记忆
6. docs/audit/                           ← 审计
```

如后续恢复 `docs/PROJECT_ENGINEERING_RULES.md`，应重新评估规则优先级。

---

## 3. 强制使用 OpenSpec 的场景

以下场景，Claude **必须先创建或更新 OpenSpec change**，完成 proposal / design / tasks 后停止，等待用户确认，**不得直接编码**：

| # | 场景 | 触发条件 |
|---|------|----------|
| 1 | B接口协议新增命令 | 新增 CommandHandler / Service / PK_Type 枚举值 |
| 2 | B接口协议兼容改造 | 修改 SOAP/XMLData 解析、PK_Type 格式、字段别名 |
| 3 | FSU 真实联调 | 任何涉及 `192.168.100.100` 或真实 FSU 的调用 |
| 4 | 数据库结构改动 | 修改 Entity / 新增字段 / 迁移脚本 / seed SQL |
| 5 | Entity / Repository / Service / Controller 改动 | 修改或新增业务层代码 |
| 6 | Scheduler 改动 | 新增/修改 `@Scheduled`、`@EnableScheduling`、定时任务配置 |
| 7 | SET 类操作 | 涉及 `SET_POINT`、`SET_THRESHOLD`、`SET_TIME` 等 SET_* 命令 |
| 8 | 告警 / 审计 / 持久化改动 | 涉及 alarm_record、ActiveAlarm、AuditService、ConsistencyAudit |
| 9 | 前端运维页面 | 新增/修改 Vue 组件、路由、API 调用 |
| 10 | Codex 审计修复任务 | 响应 Codex 审计结论，修复 Blocker/Major/Minor |

**不在上述清单中的任务**，Claude 可自行判断是否需要 OpenSpec change。如果任务涉及安全边界、协议变更或架构决策，建议也创建 change。

---

## 4. OpenSpec Change 模板

每个 OpenSpec change 文件必须包含以下 12 个字段：

```markdown
# OpenSpec Change: [CHANGE-ID] [简短标题]

## 1. 背景
（为什么要做这个改动，当前存在什么问题或缺口）

## 2. 目标
（本次改动要达成的具体目标）

## 3. 非目标
（明确本次不改什么，防止范围蔓延）

## 4. 协议依据
（B接口协议 2016/2024 的具体章节、命令、码表引用）
（如不涉及协议，必须明确说明原因）

## 5. 架构判断
（涉及的业务域、层次、模块，可复用组件，是否引入新模式）

## 6. 影响范围
（列出每个受影响/可能受影响的文件、模块、测试、文档）

## 7. 安全边界
（是否涉及：真实FSU访问、SET命令、Scheduler、alarm_record写入）
（每项必须明确：是/否，以及安全措施）

## 8. 测试计划
（新增测试、修改测试、回归范围、执行命令）

## 9. 验收标准
（完成标准，可量化、可验证）

## 10. 回滚策略
（如何撤销本次改动，回滚步骤，回滚影响）

## 11. memory 输出路径
（docs/memory/YYYY-MM-DD-任务编号-任务名.md）

## 12. audit 输出路径
（docs/audit/YYYY-MM-DD-任务编号-任务名.md）
```

---

## 5. OpenSpec 文件路径约定

```
openspec/
├── project.md              # 项目上下文（技术栈/协议/安全配置/当前状态）
├── specs/                  # 已有能力的规格（每个能力一个 .md）
│   ├── binterface-login.md
│   ├── binterface-heartbeat.md
│   └── ...
├── changes/                # 进行中的变更提案
│   └── CHANGE-001-xxx.md
└── archive/                # 已完成并归档的变更
    └── CHANGE-001-xxx.md
```

**命名规则**：
- Change 文件：`openspec/changes/CHANGE-NNN-简短描述.md`
- 归档文件：`openspec/archive/CHANGE-NNN-简短描述.md`
- Spec 文件：`openspec/specs/能力名.md`

---

## 6. OpenSpec 工作流

### 6.1 创建 Change

```
1. Claude 识别任务属于强制 OpenSpec 场景
2. Claude 在 openspec/changes/ 创建 change 文件
3. Claude 填写完整的 12 字段模板
4. Claude 停止，输出 change 文件路径，等待用户确认
```

### 6.2 用户确认后

```
5. 用户确认 change 内容
6. Claude 按论证→验证→写入流程执行
7. 完成后将 change 移至 openspec/archive/
8. 写入 memory 和 audit 文件
```

### 6.3 不需要 OpenSpec 的例外

以下情况可直接执行，无需创建 change：

- 修复 typo / 注释 / 日志格式
- 为现有测试补充边界用例（不修改生产代码）
- 读取/分析/审计类只读任务
- 文档整理（不涉及业务逻辑变更）
- 规则文件自身维护（如本文件的小幅修订）

---

## 7. 与现有论证模板的关系

现有 `CLAUDE_PROJECT_RULES.md` 第13节定义的"修改前论证模板"仍然有效。

当任务需要 OpenSpec change 时，change 文件即为论证的主要载体。Claude 在写入代码前仍需输出：

```markdown
[规则加载确认]
- 已读取 CLAUDE_PROJECT_RULES.md
- 已读取 CLAUDE_OPENSPEC_RULES.md
- 已读取相关 openspec change
- 已确认 B接口协议唯一性
- 已确认安全边界

[OpenSpec Change 确认]
- Change ID: CHANGE-NNN
- 状态: 用户已确认 / 待确认

[本次改动论证]
（具体论证内容）
```

---

## 8. OpenSpec 禁止事项

1. **不得跳过 OpenSpec change 直接编码** — 凡属第3节强制场景，必须先创建 change
2. **不得在用户确认前写入业务代码** — change 文件写入后必须停止等待
3. **不得用 OpenSpec 替代 memory/audit** — change 归档不等于 memory/audit 写入
4. **不得绕过安全边界** — change 中的安全边界判断必须与 `CLAUDE_PROJECT_RULES.md` 一致
5. **不得在 change 中写入实现代码** — change 是规格文档，不是代码
6. **不得扩大 change 范围** — 一个 change 只覆盖一个明确的任务，禁止合并多个不相关任务

---

## 9. 协议一致性要求

任何涉及 B接口协议的 OpenSpec change，必须明确引用：

- 协议版本（2016 或 2024）
- 具体命令名称和 Code
- PK_Type 格式（Name+Code 或纯文本）
- XMLData 结构
- SOAP/WSDL 约束
- 相关枚举值或常量定义

如 change 不涉及 B接口协议，必须明确声明"本次不涉及 B接口协议"及理由。

---

## 10. Claude 启动加载顺序

Claude 每次启动后，按以下顺序加载：

```
1. docs/rules/CLAUDE_PROJECT_RULES.md     ← 最高规则
2. docs/rules/CLAUDE_OPENSPEC_RULES.md    ← 规格驱动层规则
3. openspec/project.md                    ← 项目上下文
4. docs/memory/README.md                  ← 记忆索引
5. docs/memory/WORKING-MEMORY.md          ← 当前工作记忆
6. docs/audit/（相关审计文档）             ← 审计
7. openspec/changes/（进行中的变更）       ← 如有
```

---

## 11. 维护要求

- 本文件修改后必须写入 memory 和 audit
- 新增/修改强制场景清单时，必须同步更新本文件
- 如 `CLAUDE_PROJECT_RULES.md` 发生重大变更，必须检查本文件是否需要同步
