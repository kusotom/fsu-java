# agentflow — 已废弃 (DEPRECATED)

> 本路径已废弃。
> 标准入口: `/home/tom/桌面/FSU/tools/agentflow/agentflow.py`

## 迁移指南

旧路径: `fsu-platform-java/tools/agentflow/agentflow.py`
新路径: `/home/tom/桌面/FSU/tools/agentflow/agentflow.py`

请从工程流根目录执行:

```bash
cd /home/tom/桌面/FSU
python3 tools/agentflow/agentflow.py status
python3 tools/agentflow/agentflow.py start <ID> --agent <AGENT>
python3 tools/agentflow/agentflow.py check <ID>
python3 tools/agentflow/agentflow.py pack <ID> --for gpt
python3 tools/agentflow/agentflow.py finish <ID>
python3 tools/agentflow/agentflow.py release <ID>
python3 tools/agentflow/agentflow.py decision <ID> --from <FILE>
```

详细文档: `/home/tom/桌面/FSU/tools/agentflow/README.md`
