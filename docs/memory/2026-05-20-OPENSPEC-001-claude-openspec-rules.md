# OPENSPEC-001 OpenSpec 规格驱动层接入

## 1. 任务目标

将 OpenSpec 引入 Claude 工作流作为"先规格、再设计、再任务、再实现"的规范驱动层。本次仅做规范接入，不修改 Java / SQL / Vue 业务代码。

## 2. 架构判断

- 涉及业务域：项目规范/规则体系
- 新增模块：`openspec/` 目录结构 + `docs/rules/CLAUDE_OPENSPEC_RULES.md`
- 不触及：B接口协议实现、数据库模型、API 合约、前端、测试、Scheduler、SET 体系

## 3. 协议一致性判断

本次不涉及 B接口协议。纯规范/文档任务。

## 4. 修改前论证

**论据**：当前论证→验证→写入流程中，"论证"步骤缺乏结构化的规格模板。任务背景、协议依据、非目标、回滚策略等分散在论证模板中，没有统一的 OpenSpec change 文档承载。

**方案**：引入 OpenSpec 作为规格驱动层，不替代现有规则。定义 10 个强制场景和 12 字段 Change 模板。OpenSpec change 创建后停止，等待用户确认。

**优先级**：如发生冲突，以 `CLAUDE_PROJECT_RULES.md` 为准。

## 5. 写入前验证

- [x] 已确认工作目录：`/home/tom/桌面/FSU/fsu-platform-java/`
- [x] 已排除父目录 `/home/tom/桌面/FSU/docs/`
- [x] 已确认不修改 Java 业务代码
- [x] 已确认不修改 SQL / Vue / TS
- [x] 已确认不修改 application.yml
- [x] 已确认不修改测试代码
- [x] 已确认不触及真实 FSU / Scheduler / SET / alarm_record

## 6. 实际修改文件

### 新增 (7 个)

| 文件 | 说明 |
|------|------|
| `docs/rules/CLAUDE_OPENSPEC_RULES.md` | OpenSpec 规则定义（11节） |
| `openspec/project.md` | 项目级 OpenSpec 上下文（12节） |
| `openspec/specs/.gitkeep` | 已有能力规格目录 |
| `openspec/changes/.gitkeep` | 变更提案目录 |
| `openspec/archive/.gitkeep` | 归档目录 |
| `docs/memory/2026-05-20-OPENSPEC-001-claude-openspec-rules.md` | 本记忆文件 |
| `docs/audit/OPENSPEC-001-claude-openspec-rules.md` | 审计文件 |

### 修改 (3 个)

| 文件 | 改动 |
|------|------|
| `docs/rules/CLAUDE_PROJECT_RULES.md` | 更新基线/阶段/启动加载顺序；新增第21节 OpenSpec 引用 |
| `docs/memory/README.md` | 新增 OPENSPEC-001 索引条目 |
| `docs/memory/WORKING-MEMORY.md` | 同步至 LANDING-008；新增 OPENSPEC-001 条目 |

## 7. 核心改动

- 建立 `openspec/` 三级目录结构（specs/changes/archive）
- 定义 OpenSpec Change 模板（12字段：背景/目标/非目标/协议依据/架构判断/影响范围/安全边界/测试计划/验收标准/回滚策略/memory输出/audit输出）
- 定义 10 个强制使用 OpenSpec 的场景
- 定义规则优先级（CLAUDE_PROJECT_RULES.md 最高）
- 将 OpenSpec 嵌入启动加载顺序

## 8. 测试命令和结果

本次为纯文档/规范接入，未运行 Maven 测试。

## 9. 风险

| 风险 | 缓解 |
|------|------|
| 双重规则体系（父目录 vs Java 项目） | 本次明确定位 Java 项目 docs/ 为唯一规则源 |
| PROJECT_ENGINEERING_RULES.md 缺失 | 在 CLAUDE_PROJECT_RULES.md 中备注，后续决策 |
| 流程步骤增加可能导致执行变慢 | OpenSpec 仅在 10 个特定场景触发，非全局强制 |

## 10. 遗留问题

1. 父目录 `/home/tom/桌面/FSU/docs/` 存在过时规则副本，建议后续清理或标记废弃
2. `PROJECT_ENGINEERING_RULES.md` 缺失于 Java 项目，如需恢复应重新评估
3. `openspec/specs/` 尚未写入已有能力的规格文档（18 个 CommandHandler 等），建议后续逐个补充

## 11. 下一步建议

1. 向 GPT/Codex 通报 OpenSpec 接入
2. 为已有 18 个 B接口命令逐个补充 `openspec/specs/` 规格文档
3. 后续 BIF-P5 或 LANDING-009 任务严格按 OpenSpec change 流程执行
4. 清理父目录过时 docs 副本

## 12. git diff 摘要

```
新增: docs/rules/CLAUDE_OPENSPEC_RULES.md
新增: openspec/project.md + .gitkeep × 3
新增: docs/memory/2026-05-20-OPENSPEC-001-claude-openspec-rules.md
新增: docs/audit/OPENSPEC-001-claude-openspec-rules.md
修改: docs/rules/CLAUDE_PROJECT_RULES.md (更新基线/阶段 + 新增第21节)
修改: docs/memory/README.md (新增索引)
修改: docs/memory/WORKING-MEMORY.md (同步 LANDING-008)
```

## 13. git status 摘要

无业务代码修改。所有变更仅限于 docs/ 和 openspec/ 目录下的文档/规范文件。
