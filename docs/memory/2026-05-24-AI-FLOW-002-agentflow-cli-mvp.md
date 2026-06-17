# AI-FLOW-002 agentflow CLI MVP — 操作记忆

## 任务目标

实现 `tools/agentflow/agentflow.py` 工程流 CLI MVP，支持 status/start/check/finish/release/pack/decision 7 个命令，使后续任务能通过 CLI 完成工程流状态流转。

## 架构判断

- 业务域: 工程流管理
- 层次: 工具层
- 模块: tools/agentflow/

## 协议一致性判断

纯工程流工具实现，不涉及 B接口协议。

## 修改前论证

agentflow.py 是实现 repo-native 工程流系统的关键工具。当前任务流转依赖手动编辑 YAML 文件，CLI 可以提供结构化命令来管理状态机、锁、检查、context-pack。

## 写入前验证

- [x] PyYAML 5.4.1 可用
- [x] Python 3.10.12 可用
- [x] .agent/ 目录结构已就绪
- [x] AI-FLOW-002 任务文件已创建

## 实际修改文件

### 新增
- `tools/agentflow/agentflow.py` — CLI MVP (7 命令)
- `tools/agentflow/README.md` — 使用文档
- `.agent/tasks/AI-FLOW-002.yml` — 任务定义
- `.agent/decisions/AI-FLOW-001-REV2-gpt-decision.md` — GPT PASS 决策
- `docs/audit/AI-FLOW-002-agentflow-cli-mvp.md` — 审计文件
- `docs/memory/2026-05-24-AI-FLOW-002-agentflow-cli-mvp.md` — 本文件
- `.agent/context-packs/AI-FLOW-002-for-gpt.md` — context-pack

### 更新
- `.agent/locks.yml` — AI-FLOW-002 锁记录
- `.agent/current-task.yml` — 当前任务追踪
- `.agent/tasks/AI-FLOW-002.yml` — 状态 IN_PROGRESS → SELF_TESTED
- `docs/memory/README.md` — 索引更新
- `docs/memory/WORKING-MEMORY.md` — 工作记忆更新

## 核心改动

1. **agentflow.py**: 基于 PyYAML 的 CLI 工具，支持完整状态机流转
2. **项目根目录发现**: 通过向上查找 `.agent/` 目录定位项目根
3. **状态机**: PLANNED → APPROVED → IN_PROGRESS → SELF_TESTED → COMPLETED
4. **锁机制**: 支持获取/释放任务锁，防止并发冲突
5. **检查系统**: required_outputs 存在性、forbidden_paths git diff 检查

## 测试命令和结果

所有 7 个命令通过测试: status ✅, start ✅, check ✅, pack ✅, finish ✅, release ✅, decision ✅

## 风险

无。纯工具实现，不触碰业务代码。

## 遗留问题

- Forbidden_actions 检查仅做人工提醒
- decision 命令解析较简单，复杂场景需人工判断
- git diff 检查依赖 git 可用性

## 下一步建议

- CODEX-REVIEW-AI-FLOW-002
- 待 audit 通过后进入 DATA-MAPPING-006
