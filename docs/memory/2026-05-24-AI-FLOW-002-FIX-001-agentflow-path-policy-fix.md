# AI-FLOW-002-FIX-001 路径与门禁修复 — 操作记忆

## 任务目标

修复 Codex CODEX-REVIEW-AI-FLOW-002 发现的两个阻塞风险:
1. agentflow.py 路径统一到 FSU/tools/agentflow/
2. forbidden_paths 命中时 check 返回非 0 退出码，finish 阻止违规

## 架构判断

- 业务域: 工程流管理
- 层次: 工具层
- 模块: tools/agentflow/

## 修改前论证

1. **路径统一**: .agent/ 和 tools/agentflow/ 应同属工程流根目录 FSU/，而非分属 FSU/ 和 fsu-platform-java/
2. **forbidden_paths 门禁**: check 返回码必须反映安全门禁状态，finish 必须阻止违规
3. **baseline 机制**: 历史未提交改动不应阻塞当前任务检查

## 实际修改文件

### 新增
- `tools/agentflow/agentflow.py` — 标准路径 (含 baseline + 门禁修复)
- `tools/agentflow/README.md` — 标准路径使用文档
- `.agent/tasks/AI-FLOW-002-FIX-001.yml` — 任务定义
- `docs/audit/AI-FLOW-002-FIX-001-agentflow-path-policy-fix.md` — 审计
- `docs/memory/2026-05-24-AI-FLOW-002-FIX-001-agentflow-path-policy-fix.md` — 本文件

### 更新
- `docs/memory/README.md` — 索引
- `docs/memory/WORKING-MEMORY.md` — 工作记忆

### 废弃
- `fsu-platform-java/tools/agentflow/agentflow.py` — 旧路径保留但标注废弃

## 核心改动

1. **baseline 机制**: start 保存 git diff 快照，check 用 current - baseline 过滤
2. **非零退出码**: cmd_check 返回 1 当 forbidden_issues > 0
3. **finish 阻止**: cmd_finish 检查 forbidden_paths 并 sys.exit(1) 阻止
4. **路径统一**: 标准入口 cd FSU && python3 tools/agentflow/agentflow.py

## 验证结果

| 命令 | 结果 |
|------|:--:|
| status | ✅ |
| start | ✅ |
| check (正常) | ✅ |
| check (forbidden_paths 命中 → exit 1) | ✅ |
| check (forbidden_paths = 0 → exit 0) | ✅ |
| finish (阻止 forbidden_paths) | ✅ |
| pack | ✅ |
| release | ✅ |

## 风险

无。纯工具修复。

## 下一步建议

- CODEX-REVIEW-AI-FLOW-002-FIX-001
