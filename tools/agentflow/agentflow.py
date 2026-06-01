#!/usr/bin/env python3
"""agentflow.py — FSU 动环监控平台 工程流 CLI MVP
DEPRECATED: 旧路径已废弃。标准入口: /home/tom/桌面/FSU/tools/agentflow/agentflow.py
使用方式: cd /home/tom/桌面/FSU && python3 tools/agentflow/agentflow.py <command>

AI-FLOW-002: 实现工程流状态机命令行工具。
支持 status / start / check / finish / release / pack / decision 7 命令。

使用方式:
    python tools/agentflow/agentflow.py status
    python tools/agentflow/agentflow.py start <TASK_ID> --agent <AGENT>
    python tools/agentflow/agentflow.py check <TASK_ID>
    python tools/agentflow/agentflow.py finish <TASK_ID>
    python tools/agentflow/agentflow.py release <TASK_ID>
    python tools/agentflow/agentflow.py pack <TASK_ID> --for gpt
    python tools/agentflow/agentflow.py decision <TASK_ID> --from <FILE>
"""

import argparse
import os
import subprocess
import sys
from datetime import date
from pathlib import Path

import yaml

# ── helpers ──────────────────────────────────────────────────────────────────


def find_config_root() -> Path:
    """Walk up from cwd until we find a .agent/ directory."""
    d = Path.cwd()
    for _ in range(10):
        if (d / ".agent").is_dir():
            return d
        if d.parent == d:
            break
        d = d.parent
    print("ERROR: 找不到 .agent/ 目录，请从项目根目录或子目录运行。")
    sys.exit(1)


def find_code_root(config_root: Path) -> Path:
    """Find the git repo root within config_root or its subdirectories.
    Falls back to config_root if no git repo found."""
    # Search config_root and one level of subdirectories for a git repo
    candidates = [config_root] + sorted(
        [d for d in config_root.iterdir() if d.is_dir() and not d.name.startswith(".")]
    )
    for candidate in candidates:
        try:
            r = subprocess.run(
                ["git", "rev-parse", "--show-toplevel"],
                capture_output=True, text=True, cwd=candidate, timeout=5,
            )
            if r.returncode == 0 and r.stdout.strip():
                git_root = Path(r.stdout.strip())
                # Only accept git repos that are inside or equal to config_root
                if str(git_root).startswith(str(config_root)):
                    return git_root
        except Exception:
            continue
    return config_root


def today_str() -> str:
    return date.today().isoformat()


def load_yaml(path: Path):
    """Load a YAML file, returning {} or [] for missing / empty files."""
    if not path.exists():
        return {}
    with open(path, "r", encoding="utf-8") as fh:
        data = yaml.safe_load(fh)
    return data if data is not None else {}


def save_yaml(path: Path, data):
    """Save a dict/list to a YAML file."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        yaml.dump(data, fh, allow_unicode=True, default_flow_style=False, sort_keys=False)


def load_task(config_root: Path, task_id: str) -> dict:
    """Load a task YAML file by task_id."""
    task_file = config_root / ".agent" / "tasks" / f"{task_id}.yml"
    if not task_file.exists():
        # Search by task_id field inside files
        tasks_dir = config_root / ".agent" / "tasks"
        if tasks_dir.is_dir():
            for f in sorted(tasks_dir.glob("*.yml")):
                data = load_yaml(f)
                if isinstance(data, dict) and data.get("task_id") == task_id:
                    return data
        print(f"ERROR: 任务文件不存在: {task_id}.yml")
        print(f"       检查路径: {task_file}")
        sys.exit(1)
    return load_yaml(task_file)


def save_task(config_root: Path, task_id: str, data: dict):
    """Save a task YAML file."""
    task_file = config_root / ".agent" / "tasks" / f"{task_id}.yml"
    save_yaml(task_file, data)


def load_locks(config_root: Path) -> dict:
    locks = load_yaml(config_root / ".agent" / "locks.yml")
    if not isinstance(locks, dict):
        locks = {}
    locks.setdefault("active_locks", {})
    locks.setdefault("lock_history", [])
    return locks


def save_locks(config_root: Path, locks: dict):
    save_yaml(config_root / ".agent" / "locks.yml", locks)


def load_current(config_root: Path) -> dict:
    ct = load_yaml(config_root / ".agent" / "current-task.yml")
    if not isinstance(ct, dict):
        ct = {}
    ct.setdefault("current", {})
    ct.setdefault("recently_completed", [])
    return ct


def save_current(config_root: Path, ct: dict):
    save_yaml(config_root / ".agent" / "current-task.yml", ct)


def git_diff_names(code_root: Path):
    """Return list of files changed in git diff (staged + unstaged)."""
    try:
        r = subprocess.run(
            ["git", "diff", "--name-only"],
            capture_output=True, text=True, cwd=code_root, timeout=10,
        )
        if r.returncode != 0:
            return None, f"git diff 失败 (rc={r.returncode})"
        staged = r.stdout.strip().split("\n") if r.stdout.strip() else []

        r2 = subprocess.run(
            ["git", "diff", "--name-only", "--cached"],
            capture_output=True, text=True, cwd=code_root, timeout=10,
        )
        if r2.returncode != 0:
            return None, f"git diff --cached 失败 (rc={r2.returncode})"
        staged2 = r2.stdout.strip().split("\n") if r2.stdout.strip() else []

        all_files = sorted(set(staged + staged2))
        return all_files, None
    except FileNotFoundError:
        return None, "WARNING: git 不可用，跳过 git diff 检查"
    except Exception as e:
        return None, f"WARNING: git 检查异常: {e}"


def check_forbidden_paths(root: Path, forbidden: list, changed_files: list) -> list:
    """Return list of violations (changed files matching forbidden patterns)."""
    violations = []
    for pattern in forbidden:
        # pattern like "backend/src/main/java/**"
        prefix = pattern.rstrip("/").rstrip("*").rstrip("/")
        for f in changed_files:
            if f.startswith(prefix):
                violations.append(f)
    return sorted(set(violations))


def check_required_outputs(code_root: Path, outputs: list, config_root: Path = None) -> tuple:
    """Return (missing_list, found_list). Tries code_root first, then config_root."""
    missing = []
    found = []
    for path_str in outputs:
        p = code_root / path_str
        if not p.exists() and config_root is not None:
            p2 = config_root / path_str
            if p2.exists():
                p = p2
        if p.exists():
            found.append(path_str)
        else:
            missing.append(path_str)
    return missing, found


def check_conditions(root: Path, conditions: list) -> tuple:
    """Check simple conditions (file existence, etc). Return (failures, successes)."""
    failures = []
    successes = []
    for cond in conditions:
        # For MVP, treat each condition as a keyword check
        parts = cond.strip().split()
        if not parts:
            continue
        keyword = parts[0]
        target = parts[1] if len(parts) > 1 else ""
        if keyword == "file_exists":
            if (root / target).exists():
                successes.append(cond)
            else:
                failures.append(cond)
        else:
            successes.append(cond)  # pass through unknown conditions
    return failures, successes


def status_label(s: str) -> str:
    """Colour-like indicators for status."""
    labels = {
        "PLANNED": "📋 PLANNED",
        "APPROVED": "✅ APPROVED",
        "IN_PROGRESS": "🔄 IN_PROGRESS",
        "SELF_TESTED": "🧪 SELF_TESTED",
        "REVIEW": "🔍 REVIEW",
        "DECISION": "🎯 DECISION",
        "COMPLETED": "🏁 COMPLETED",
        "CLOSED": "🔒 CLOSED",
        "BLOCKED": "🚫 BLOCKED",
        "CANCELLED": "❌ CANCELLED",
        "IDLE": "💤 IDLE",
    }
    return labels.get(s, s)


def description_for_task(data: dict) -> str:
    desc = data.get("description", "")
    if isinstance(desc, str):
        return desc[:80].replace("\n", " ")
    return str(desc)[:80]


# ── commands ─────────────────────────────────────────────────────────────────


def cmd_status(config_root: Path):
    """Display current task, locks, and task list."""
    print("=" * 60)
    print("  FSU 动环监控平台 — agentflow 工程流状态")
    print("=" * 60)

    ct = load_current(config_root)
    cur = ct.get("current", {})
    print(f"\n当前任务: {cur.get('task_id') or '(无)'}")
    print(f"状态:     {status_label(cur.get('status', 'IDLE'))}")
    print(f"开始时间: {cur.get('started_at') or '(无)'}")

    locks = load_locks(config_root)
    active = locks.get("active_locks", {})
    print(f"\n活跃锁:   {len(active)} 个")
    for tid, info in active.items():
        if isinstance(info, dict):
            print(f"  - {tid}: holder={info.get('holder')}, since={info.get('acquired_at')}")
        else:
            print(f"  - {tid}: {info}")

    tasks_dir = config_root / ".agent" / "tasks"
    print(f"\n任务列表 ({tasks_dir}):")
    if tasks_dir.is_dir():
        files = sorted(tasks_dir.glob("*.yml"))
        if not files:
            print("  (无任务文件)")
        for f in files:
            data = load_yaml(f)
            if isinstance(data, dict):
                tid = data.get("task_id", f.stem)
                st = data.get("status", "?")
                title = data.get("title", data.get("description", ""))
                owner = data.get("owner", "?")
                if isinstance(title, dict):
                    title = title.get("name", str(title))
                title_str = str(title)[:70]
                print(f"  [{status_label(st):20s}] {tid:30s} {title_str}  (owner: {owner})")
    else:
        print("  (tasks/ 目录不存在)")

    recent = ct.get("recently_completed", [])
    if recent:
        print("\n最近完成:")
        for item in recent[:5]:
            if isinstance(item, dict):
                print(f"  - {item.get('task_id')}: {item.get('title', '')} ({item.get('completed_at', '')})")

    print()


def cmd_start(config_root: Path, task_id: str, agent: str):
    """Start a task: check preconditions, acquire lock, set status -> IN_PROGRESS."""
    task = load_task(config_root, task_id)
    current_status = task.get("status", "")

    if current_status not in ("PLANNED", "APPROVED"):
        print(f"ERROR: 任务 {task_id} 当前状态为 {current_status}，不允许启动。")
        print(f"       只能从 PLANNED 或 APPROVED 状态启动。")
        sys.exit(1)

    ct = load_current(config_root)
    cur = ct.get("current", {})
    if cur.get("task_id") and cur.get("status") not in ("COMPLETED", "IDLE", "", None):
        print(f"WARNING: 当前已有活跃任务: {cur.get('task_id')} (status={cur.get('status')})")
        print(f"         请先完成或释放该任务后再启动新任务。")
        sys.exit(1)

    locks = load_locks(config_root)
    active = locks.get("active_locks", {})
    if active:
        other_locks = {k: v for k, v in active.items() if k != task_id}
        if other_locks:
            print(f"WARNING: 存在其他活跃锁: {list(other_locks.keys())}")
            print(f"         请先释放这些锁。")
            sys.exit(1)

    now = today_str()
    locks["active_locks"][task_id] = {
        "holder": agent,
        "acquired_at": now,
        "type": "write",
    }
    save_locks(config_root, locks)

    ct["current"] = {
        "task_id": task_id,
        "status": "IN_PROGRESS",
        "started_at": now,
    }
    save_current(config_root, ct)

    task["status"] = "IN_PROGRESS"
    save_task(config_root, task_id, task)

    print(f"✅ 任务 {task_id} 已启动")
    print(f"   Agent:     {agent}")
    print(f"   状态:      {current_status} → IN_PROGRESS")
    print(f"   锁:        已获取 (holder={agent}, since={now})")
    print(f"   标题:      {task.get('title', '')}")


def cmd_check(config_root: Path, task_id: str, code_root: Path = None):
    """Check task: required_outputs, forbidden_paths, forbidden_actions, memory/audit."""
    if code_root is None:
        code_root = find_code_root(config_root)

    task = load_task(config_root, task_id)
    print(f"\n检查任务: {task_id} — {task.get('title', '')}")
    print(f"当前状态: {task.get('status', '?')}")
    print(f"代码根:   {code_root}")
    print("-" * 40)

    issues = 0

    # 1. Required outputs (relative to code_root)
    required = task.get("required_outputs", [])
    if required:
        missing, found = check_required_outputs(code_root, required, config_root)
        print(f"\n[required_outputs] 检查 {len(required)} 项:")
        for f in found:
            print(f"  ✅ {f}")
        for f in missing:
            print(f"  ❌ {f}")
            issues += 1
    else:
        print("\n[required_outputs] (无)")

    # 2. Forbidden paths via git diff (relative to code_root)
    forbidden = task.get("forbidden_paths", [])
    if forbidden:
        changed, git_err = git_diff_names(code_root)
        if git_err:
            print(f"\n[forbidden_paths] {git_err}")
        elif changed is None:
            print("\n[forbidden_paths] git 不可用，跳过")
        else:
            violations = check_forbidden_paths(code_root, forbidden, changed)
            print(f"\n[forbidden_paths] git diff 文件: {len(changed)} 个")
            if violations:
                print(f"  ❌ 违规修改:")
                for v in violations:
                    print(f"     {v}")
                issues += len(violations)
            else:
                print(f"  ✅ 无 forbidden_path 违规")
    else:
        print("\n[forbidden_paths] (无)")

    # 3. Forbidden actions
    forbidden_actions = task.get("forbidden_actions", [])
    if forbidden_actions:
        print(f"\n[forbidden_actions] {len(forbidden_actions)} 项禁止操作:")
        for a in forbidden_actions:
            print(f"  ⚠️  {a} (人工确认)")
        print(f"  ℹ️  请人工确认上述操作未被触发")
    else:
        print("\n[forbidden_actions] (无)")

    # 4. Required checks
    checks = task.get("required_checks", [])
    if checks:
        print(f"\n[required_checks] {len(checks)} 项:")
        for c in checks:
            print(f"  ⬜ {c}")
        print(f"  ℹ️  请人工确认上述检查项")
    else:
        print("\n[required_checks] (无)")

    # 5. Memory file check (relative to code_root)
    mem_dir = code_root / "docs" / "memory"
    if mem_dir.is_dir():
        today = today_str()
        mem_files = list(mem_dir.glob(f"{today[:4]}-*-{task_id}-*.md"))
        if mem_files:
            print(f"\n[memory] ✅ 找到 {len(mem_files)} 个相关文件:")
            for mf in mem_files:
                print(f"  {mf.relative_to(code_root)}")
        else:
            print(f"\n[memory] ❌ 未找到 {task_id} 的 memory 文件 (日期={today})")
            issues += 1

    # 6. Audit file check (relative to code_root)
    audit_dir = code_root / "docs" / "audit"
    if audit_dir.is_dir():
        audit_files = list(audit_dir.glob(f"{task_id}-*.md")) + list(audit_dir.glob(f"*{task_id}*.md"))
        if audit_files:
            print(f"\n[audit] ✅ 找到 {len(audit_files)} 个相关文件:")
            for af in audit_files[:5]:
                print(f"  {af.relative_to(code_root)}")
        else:
            print(f"\n[audit] ⚠️  未找到 {task_id} 的 audit 文件 (非阻塞)")

    # Summary
    print("\n" + "=" * 40)
    if issues == 0:
        print("✅ 检查通过 — 所有必要项满足")
    else:
        print(f"❌ 发现问题: {issues} 项")
        print(f"   请修复上述问题后重试 check")
    print()


def cmd_finish(config_root: Path, task_id: str, code_root: Path = None):
    """Finish task: run check first, then set status -> SELF_TESTED."""
    if code_root is None:
        code_root = find_code_root(config_root)

    task = load_task(config_root, task_id)
    current_status = task.get("status", "")

    if current_status != "IN_PROGRESS":
        print(f"ERROR: 任务 {task_id} 当前状态为 {current_status}，不能 finish。")
        print(f"       只有 IN_PROGRESS 状态才能 finish。")
        sys.exit(1)

    required = task.get("required_outputs", [])
    if required:
        missing, found = check_required_outputs(code_root, required, config_root)
        if missing:
            print(f"ERROR: required_outputs 缺失，阻止 finish:")
            for m in missing:
                print(f"  ❌ {m}")
            print(f"\n请先完成所有 required_outputs 再 finish。")
            sys.exit(1)
        print(f"✅ required_outputs: {len(found)}/{len(required)} 已满足")

    task["status"] = "SELF_TESTED"
    save_task(config_root, task_id, task)

    ct = load_current(config_root)
    ct["current"] = {
        "task_id": task_id,
        "status": "SELF_TESTED",
        "started_at": ct.get("current", {}).get("started_at", today_str()),
    }
    save_current(config_root, ct)

    print(f"\n✅ 任务 {task_id} 完成实现")
    print(f"   状态:      {current_status} → SELF_TESTED")
    print(f"   下一步:    使用 'release {task_id}' 释放锁")
    print(f"   提交审计:  通知 Codex-Reviewer")


def cmd_release(config_root: Path, task_id: str):
    """Release task lock and clean current-task."""
    locks = load_locks(config_root)
    active = locks.get("active_locks", {})
    if task_id in active:
        lock_info = active.pop(task_id)
        now = today_str()
        locks["lock_history"].append({
            "task_id": task_id,
            "holder": lock_info.get("holder", "unknown"),
            "acquired_at": lock_info.get("acquired_at", "?"),
            "released_at": now,
        })
        if len(locks["lock_history"]) > 20:
            locks["lock_history"] = locks["lock_history"][-20:]
        save_locks(config_root, locks)
        print(f"✅ 锁已释放: {task_id}")
    else:
        print(f"ℹ️  任务 {task_id} 没有活跃锁")

    ct = load_current(config_root)
    cur = ct.get("current", {})
    if cur.get("task_id") == task_id:
        ct.setdefault("recently_completed", []).insert(0, {
            "task_id": task_id,
            "title": cur.get("title", ""),
            "completed_at": today_str(),
        })
        if len(ct["recently_completed"]) > 10:
            ct["recently_completed"] = ct["recently_completed"][:10]
        ct["current"] = {"task_id": None, "status": "IDLE", "started_at": None}
        save_current(config_root, ct)
        print(f"✅ current-task 已清理")
    else:
        print(f"ℹ️  current-task 当前为 {cur.get('task_id')}，无需清理")


def cmd_pack(config_root: Path, task_id: str, target: str, code_root: Path = None):
    """Generate a context-pack for GPT."""
    if code_root is None:
        code_root = find_code_root(config_root)

    task = load_task(config_root, task_id)
    pack_dir = config_root / ".agent" / "context-packs"
    pack_dir.mkdir(parents=True, exist_ok=True)
    pack_file = pack_dir / f"{task_id}-for-gpt.md"

    changed_files, git_err = git_diff_names(code_root)
    required = task.get("required_outputs", [])
    missing_outputs, found_outputs = check_required_outputs(code_root, required, config_root)
    forbidden = task.get("forbidden_paths", [])

    reviews_dir = config_root / ".agent" / "reviews"
    review_files = []
    if reviews_dir.is_dir():
        review_files = list(reviews_dir.glob(f"*{task_id}*.md"))

    mem_dir = code_root / "docs" / "memory"
    mem_files = []
    if mem_dir.is_dir():
        mem_files = list(mem_dir.glob(f"*-{task_id}-*.md"))

    lines = [
        f"# Context Pack: {task_id} for GPT",
        "",
        f"> 生成时间: {today_str()}",
        f"> 目标读者: GPT-Architect / GPT-Protocol-Agent",
        "",
        "## 任务结果摘要",
        "",
        f"- task_id: {task_id}",
        f"- 标题: {task.get('title', '')}",
        f"- 状态: {task.get('status', '?')}",
        f"- owner: {task.get('owner', '?')}",
        f"- reviewer: {task.get('reviewer', '?')}",
        f"- decision_owner: {task.get('decision_owner', '?')}",
        f"- risk_level: {task.get('risk_level', '?')}",
        "",
        "## 变更摘要",
        "",
    ]

    if changed_files:
        lines.append(f"git diff 文件 ({len(changed_files)} 个):")
        lines.append("")
        for f in changed_files:
            lines.append(f"- `{f}`")
    elif git_err:
        lines.append(f"_{git_err}_")
    else:
        lines.append("(无 git diff 信息)")

    lines += [
        "",
        "## Required Outputs 检查",
        "",
    ]
    for o in found_outputs:
        lines.append(f"- ✅ `{o}`")
    for o in missing_outputs:
        lines.append(f"- ❌ `{o}` (缺失)")

    lines += [
        "",
        "## 安全边界检查",
        "",
        f"- forbidden_paths: {len(forbidden)} 项规则",
        f"- git 违规: {'无' if not changed_files or not forbidden else '需人工确认'}",
        f"- forbidden_actions: {len(task.get('forbidden_actions', []))} 项",
        "",
    ]

    if review_files:
        lines += [
            "## Codex Review",
            "",
        ]
        for rf in review_files[:5]:
            lines.append(f"- `{rf.relative_to(config_root)}`")

    lines += [
        "",
        "## Memory / Audit 文件",
        "",
    ]
    if mem_files:
        for mf in mem_files:
            lines.append(f"- `{mf.relative_to(code_root)}`")
    else:
        lines.append("(未找到 memory 文件)")

    lines += [
        "",
        "## 需要 GPT 决策的问题",
        "",
        "(待填写)",
        "",
        "## 建议的下一步",
        "",
        f"- Codex 审计: CODEX-REVIEW-{task_id}",
        f"- GPT 决策: .agent/decisions/{task_id}-gpt-decision.md",
        "",
    ]

    content = "\n".join(lines)
    with open(pack_file, "w", encoding="utf-8") as fh:
        fh.write(content)

    print(f"✅ Context-pack 已生成: {pack_file.relative_to(config_root)}")
    print(f"   大小: {len(content)} bytes")


def cmd_decision(config_root: Path, task_id: str, decision_file: str):
    """Process a GPT decision file and update task status."""
    df_path = Path(decision_file)
    if not df_path.is_absolute():
        df_path = config_root / df_path
    if not df_path.exists():
        print(f"ERROR: decision 文件不存在: {df_path}")
        sys.exit(1)

    with open(df_path, "r", encoding="utf-8") as fh:
        content = fh.read()

    decision = None
    for line in content.split("\n"):
        line_stripped = line.strip()
        if line_stripped.startswith("decision:") or line_stripped.startswith("decision:"):
            decision = line_stripped.split(":", 1)[1].strip()
            break

    if not decision:
        for line in content.split("\n"):
            if "decision:" in line.lower():
                decision = line.split(":", 1)[1].strip()
                break

    if not decision:
        print("ERROR: 无法从 decision 文件中解析 decision 字段")
        print(f"       文件: {df_path}")
        sys.exit(1)

    task = load_task(config_root, task_id)

    print(f"任务:     {task_id}")
    print(f"当前状态: {task.get('status', '?')}")
    print(f"Decision: {decision}")
    print()

    if decision in ("PASS",):
        new_status = "COMPLETED"
        print(f"✅ GPT 决策: PASS → 状态推进到 {new_status}")
        task["status"] = new_status
        save_task(config_root, task_id, task)

        ct = load_current(config_root)
        ct["current"] = {"task_id": None, "status": "IDLE", "started_at": None}
        ct.setdefault("recently_completed", []).insert(0, {
            "task_id": task_id,
            "title": task.get("title", ""),
            "completed_at": today_str(),
        })
        if len(ct["recently_completed"]) > 10:
            ct["recently_completed"] = ct["recently_completed"][:10]
        save_current(config_root, ct)

    elif decision in ("CONDITIONAL_PASS",):
        new_status = "BLOCKED"
        print(f"⚠️  GPT 决策: CONDITIONAL_PASS → 状态标记为 {new_status}")
        print(f"   需满足条件后重新启动")
        task["status"] = new_status
        save_task(config_root, task_id, task)

    elif decision in ("FAIL",):
        new_status = "BLOCKED"
        print(f"❌ GPT 决策: FAIL → 状态标记为 {new_status}")
        print(f"   需修复后重新提交")
        task["status"] = new_status
        save_task(config_root, task_id, task)

    else:
        print(f"⚠️  未知 decision 值: {decision}")
        print(f"   允许: PASS / CONDITIONAL_PASS / FAIL")
        sys.exit(1)

    print(f"\n完成。任务 {task_id}: {task.get('status', '?')} → {new_status}")


# ── main ─────────────────────────────────────────────────────────────────────


def main():
    parser = argparse.ArgumentParser(
        description="FSU 动环监控平台 — agentflow 工程流 CLI MVP",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
命令:
  status                         显示当前任务、锁、任务列表
  start    <ID> --agent <A>      启动任务 (PLANNED/APPROVED -> IN_PROGRESS)
  check    <ID>                  检查任务 (outputs, paths, memory, audit)
  finish   <ID>                  完成任务 (IN_PROGRESS -> SELF_TESTED)
  release  <ID>                  释放任务锁
  pack     <ID> --for gpt        生成 context-pack
  decision <ID> --from <FILE>    处理 GPT 决策

示例:
  python tools/agentflow/agentflow.py status
  python tools/agentflow/agentflow.py start AI-FLOW-002 --agent Claude-Developer
  python tools/agentflow/agentflow.py check AI-FLOW-002
  python tools/agentflow/agentflow.py finish AI-FLOW-002
  python tools/agentflow/agentflow.py release AI-FLOW-002
  python tools/agentflow/agentflow.py pack AI-FLOW-002 --for gpt
  python tools/agentflow/agentflow.py decision AI-FLOW-002 --from .agent/decisions/AI-FLOW-002-gpt-decision.md
        """,
    )

    sub = parser.add_subparsers(dest="command", help="子命令")

    sub.add_parser("status", help="显示当前状态")

    p_start = sub.add_parser("start", help="启动任务")
    p_start.add_argument("task_id", help="任务 ID")
    p_start.add_argument("--agent", required=True, help="执行 Agent")

    p_check = sub.add_parser("check", help="检查任务")
    p_check.add_argument("task_id", help="任务 ID")

    p_finish = sub.add_parser("finish", help="完成任务")
    p_finish.add_argument("task_id", help="任务 ID")

    p_release = sub.add_parser("release", help="释放任务锁")
    p_release.add_argument("task_id", help="任务 ID")

    p_pack = sub.add_parser("pack", help="生成 context-pack")
    p_pack.add_argument("task_id", help="任务 ID")
    p_pack.add_argument("--for", dest="target", default="gpt", help="目标读者 (默认: gpt)")

    p_decision = sub.add_parser("decision", help="处理 GPT 决策")
    p_decision.add_argument("task_id", help="任务 ID")
    p_decision.add_argument("--from", dest="decision_file", required=True, help="Decision 文件路径")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    config_root = find_config_root()
    code_root = find_code_root(config_root)
    print(f"📁 配置根: {config_root}")
    print(f"📁 代码根: {code_root}\n")

    if args.command == "status":
        cmd_status(config_root)
    elif args.command == "start":
        cmd_start(config_root, args.task_id, args.agent)
    elif args.command == "check":
        cmd_check(config_root, args.task_id, code_root)
    elif args.command == "finish":
        cmd_finish(config_root, args.task_id, code_root)
    elif args.command == "release":
        cmd_release(config_root, args.task_id)
    elif args.command == "pack":
        cmd_pack(config_root, args.task_id, args.target, code_root)
    elif args.command == "decision":
        cmd_decision(config_root, args.task_id, args.decision_file)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
