# 2026-05-21 CODEX-AUDIT-2016-FULL-001 B接口2016全版本审计

## 任务目标

按 `openspec/protocols/binterface-2016/` 全部规范，对当前项目做 B接口2016 全版本只读审计，输出审计报告和后续 TODO。

## 架构判断

审计范围覆盖 B接口协议层、SCService 入站、FSUService 出站、命令分发、SET 安全、Scheduler、Entity/Repository/Service/Controller/Test 和真实 FSU 实测差异。

## 协议一致性判断

B接口2016 已确认为当前主协议。当前实现支持真实 LOGIN 和部分 2016 出站只读命令，但 2016 Code 表、枚举、GET_DATA DeviceList、SEND_ALARM 真实样本等仍不完整。

## 修改前论证

本次是只读审计任务，允许新增审计报告、TODO 和 memory，不允许修改 Java/SQL、不允许真实 FSU 调用、不允许 SET、不允许启用 Scheduler。

## 写入前验证

- 已读取 `openspec/protocols/binterface-2016/` 全部 39 个文件。
- 已读取规则、memory、openspec project 和既有审计。
- 已执行用户要求的 4 组 `grep -R` 搜索。
- 已执行 `find openspec/protocols/binterface-2016 -type f | sort`。
- 未运行真实 FSU 测试。

## 实际修改文件

- `docs/audit/CODEX-AUDIT-2016-FULL-001-binterface-full-version-audit.md`
- `docs/tasks/BIF2016-FULL-AUDIT-TODO.md`
- `docs/memory/2026-05-21-CODEX-AUDIT-2016-FULL-001-binterface-full-version-audit.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`

## 核心改动

新增 B接口2016 全版本审计报告，逐命令列出协议要求、当前实现、测试覆盖、真实 FSU 状态、缺口、风险等级、修复建议和优先级；新增 P0/P1/P2/P3 TODO 任务清单；更新 memory 索引和工作记忆。

## 测试命令和结果

- `find openspec/protocols/binterface-2016 -type f | sort`：通过，39 文件。
- `grep -R "LOGIN\\|LOGOUT\\|GET_DATA\\|GET_HISDATA\\|SEND_ALARM\\|SET_POINT\\|TIME_CHECK\\|GET_LOGININFO\\|SET_LOGININFO\\|GET_FTP\\|SET_FTP\\|GET_FSUINFO\\|SET_FSUREBOOT\\|GET_THRESHOLD\\|SET_THRESHOLD" -n backend/src/main/java backend/src/test/java`：已执行。
- `grep -R "BInterfaceCommand2016\\|BInterfacePkType\\|CommandDispatcher\\|CommandHandler\\|SoapMessageHandler\\|FsuServiceClient\\|ScServiceController\\|StandardScServiceController" -n backend/src/main/java backend/src/test/java`：已执行。
- `grep -R "TSemaphore\\|TThreshold\\|TAlarm\\|TFSUStatus\\|CPUUsage\\|MEMUsage\\|DeviceList\\|FsuCode\\|FSUCode\\|FsuId" -n backend/src/main/java backend/src/test/java`：已执行。
- `grep -R "SET_POINT\\|SET_THRESHOLD\\|SET_LOGININFO\\|SET_FTP\\|SET_FSUREBOOT\\|scheduler-enabled\\|real-call-enabled\\|forbidden" -n backend/src/main/java backend/src/test/java src/main/resources database docs openspec`：已执行；因 `src/main/resources` 不存在整体退出码为 2，但已输出有效匹配。
- `git status --short`：已执行，工作区已有大量既有变更。
- `git diff --stat`：已执行，包含本次文档变更和既有变更。

未运行 `mvn test`，因为本次用户要求只审计且不运行真实 FSU 测试；现有 target 报告中存在历史 real-fsu run-once 失败记录，不作为本次执行结果。

## 风险

- 2016 Code 表不完整，存在 2024 alias 误用风险。
- GET_DATA 未对齐 Emerson DeviceList/TSemaphore，影响真实数据闭环。
- SEND_ALARM 未有真实样本验收。
- SET 类整体默认安全，但 `TIME_CHECK` 被 legacy SET 名称集合包含，语义需澄清。

## 遗留问题

- 项目内 `docs/PROJECT_ENGINEERING_RULES.md` 缺失，实际规则在父目录。
- `docs/memory/README.md` 引用了不存在的 SPEC-2016 memory 文件。
- LOGOUT、GET_HISDATA、SET_LOGININFO 未实现。
- SET_POINT、SET_FTP 仅 stub。
- raw-samples 缺真实 SOAP 文件。

## 下一步建议

优先执行：

1. `BIF2016-P0-001`：GET_DATA DeviceList/TSemaphore 闭环。
2. `BIF2016-P0-002`：SEND_ALARM 真实样本采集与入库验收。
3. `BIF2016-P1-001`：补齐 BInterfaceCommand2016 全 15 命令与 ACK 码表。
4. `BIF2016-P1-002`：统一 2016 PK_Type 枚举和别名解析。

## git diff 摘要

本次新增审计报告、TODO 文档、memory，并更新 memory 索引/工作记忆。工作区已有大量未提交 Java、target、docs、openspec 变更，本次未回滚也未清理。

## git status 摘要

工作区非干净状态。新增本次文件：

- `docs/audit/CODEX-AUDIT-2016-FULL-001-binterface-full-version-audit.md`
- `docs/tasks/BIF2016-FULL-AUDIT-TODO.md`
- `docs/memory/2026-05-21-CODEX-AUDIT-2016-FULL-001-binterface-full-version-audit.md`

