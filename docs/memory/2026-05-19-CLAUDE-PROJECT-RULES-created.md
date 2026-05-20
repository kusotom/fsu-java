---
name: claude-project-rules-created
description: "创建 docs/rules/CLAUDE_PROJECT_RULES.md — 18 条固定规则, Claude 每次启动必须加载"
metadata:
  type: project
---

## 任务名称 CLAUDE_PROJECT_RULES 生成
## 操作时间 2026-05-19

## 核心改动
- 新建 `docs/rules/CLAUDE_PROJECT_RULES.md`: 18 条固定规则
  - 协作分工 / B接口协议唯一 / 先论证后写入 / 防屎山
  - 默认禁止真实FSU / 禁止Scheduler / 禁止SET / 禁止改alarm_record
  - 项目状态基线 / Codex审计结论 / 启动加载顺序 / 修改前论证模板
  - 工程记忆规则 / 审计文档规则 / 测试规则 / Claude输出约束 / 最终原则

## 文件清单
- 新增: docs/rules/CLAUDE_PROJECT_RULES.md
- 更新: docs/memory/README.md
- 更新: docs/memory/WORKING-MEMORY.md

## 后续
Claude 每次启动必须先读取 docs/rules/CLAUDE_PROJECT_RULES.md
