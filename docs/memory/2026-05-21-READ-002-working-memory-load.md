# READ-002 工作记忆加载记录

## 任务目标

读取桌面 `FSU` 文件夹内 Java 项目的工作记忆，恢复当前项目上下文、规则约束、阶段状态和后续工作边界。

## 架构判断

- 项目实际路径为 `/home/tom/桌面/FSU/fsu-platform-java/`。
- 主要工程为 Java 17 + Spring Boot 3.2.5 + JPA + Maven/JUnit5 后端，配套 Vue 3 + TypeScript + Vite + Element Plus 前端。
- 核心业务域为机房动环监控平台，设备交互统一走 B-Interface SOAP/WSDL/XMLData。
- 本次属于只读上下文加载和工程记忆闭环，不修改 Controller、Service、Repository、Entity、前端或数据库迁移。

## 协议一致性判断

本次不修改协议实现、不新增协议字段、不调整 SOAP/XMLData 解析、不触碰 MsgType、FsuCode、DeviceID、SignalID 或 ResultCode。已确认项目规则要求 B接口协议 2016 是设备上报、采集、交互、管理的唯一协议标准，禁止重新引入 DSC/RDS 主流程。

## 修改前论证

- 用户先要求读取工作记忆，随后明确目标为“桌面的 FSU 文件夹，里面的 Java 项目”。
- 英文 `/home/tom/Desktop` 不存在，实际目录为 `/home/tom/桌面/FSU`。
- 已读取父目录工作记忆、Java 项目工作记忆、README、AGENTS、CODEX、主工程规则和 OpenSpec 规则。
- 根据项目规则，只读上下文加载也需要写入 `docs/memory/` 并更新索引，因此本次仅新增本记忆文件和索引条目。

## 写入前验证

- 已确认 `/home/tom/桌面/FSU/fsu-platform-java/docs/memory/WORKING-MEMORY.md` 存在。
- 已确认当前最新工作记忆显示项目推进至 LANDING-011。
- 已确认最新测试基线记录为 1199 tests, 0 failures, 0 errors, 5 skipped。
- 已确认 OpenSpec + Superpowers 工作流已初始化，后续涉及协议、数据库、业务层、Scheduler、SET、告警、前端等变更需先走 OpenSpec change。

## 实际修改文件

- `docs/memory/2026-05-21-READ-002-working-memory-load.md`
- `docs/memory/README.md`

## 核心改动

记录本次工作记忆加载结果，并在操作记忆索引顶部添加 READ-002 条目，满足项目只读任务闭环要求。

## 测试命令和结果

- `ls -la /home/tom`：确认存在中文桌面目录。
- `find /home/tom -maxdepth 3 -type d -name FSU -print`：确认项目路径为 `/home/tom/桌面/FSU`。
- `sed` 读取工作记忆、README、AGENTS、CODEX、工程规则和 OpenSpec 规则：读取成功。
- `git status --short`：确认工作区已有大量既有未提交变更，本次不回退、不覆盖。

未运行 `mvn test` 或前端构建；本次未修改业务代码、协议实现或前端代码。

## 风险

- 工作区已有大量既有变更、生成文件和未跟踪文件，后续任务必须先区分用户既有改动与本次新增改动。
- 父目录 `/home/tom/桌面/FSU/docs/` 存在过时规则副本，后续需谨慎确认以 Java 项目内最新规则为准。
- 本次只做上下文恢复，未验证当前工作区是否仍能全量构建通过。

## 遗留问题

- 未执行后端全量测试、前端构建或服务启动。
- 未审计当前所有未提交业务代码变更。
- 未清理既有 `target/`、`node_modules/` 等生成目录状态。

## 下一步建议

- 后续如需改业务代码，先明确任务编号和影响范围。
- 属于 OpenSpec 强制场景的任务，先创建或更新 `openspec/changes/` 中的 change，并等待确认。
- 写业务代码前按项目模板输出规则加载确认、改动论证、写入前验证、计划修改文件、风险点和测试计划。

## git diff 摘要

本次新增 READ-002 记忆文件，并更新 `docs/memory/README.md` 索引。

## git status 摘要

读取时工作区已存在多个修改和未跟踪文件，主要包括 B-Interface 相关生产/测试代码、数据库脚本、landing 文档、memory/audit 文档、OpenSpec 文档以及 `backend/target/` 生成文件。本次不回退、不清理这些既有状态。
