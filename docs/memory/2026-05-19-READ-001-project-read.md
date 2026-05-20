# READ-001 项目读取记录

## 任务目标

读取桌面 `FSU` 项目，确认项目路径、工程规则、技术栈、目录结构、关键模块和当前工作区状态。

## 架构判断

- 项目实际路径为 `/home/tom/桌面/FSU`，主要 Git 工程位于 `fsu-platform-java`。
- 后端为 Java 17 + Spring Boot 3.2.5 + JPA + PostgreSQL。
- 前端为 Vue 3 + TypeScript + Vite + Element Plus。
- 核心业务域为机房动环监控平台，重点模块包括 B-Interface、资源、遥测、告警、系统管理。
- B-Interface 模块已包含 command、service、SOAP、XMLData、WSDL、slow channel、FSU client、安全门禁和审计相关实现。

## 协议一致性判断

本次只读读取和记忆写入不修改协议实现。已确认项目规则要求 B接口协议 2016 是设备上报、采集、交互、管理的唯一协议标准，设备交互必须基于 SOAP/WSDL/XMLData。

## 修改前论证

- 用户请求为“先读取桌面的fsu文件夹项目”，属于项目上下文加载任务。
- 已按 Codex 项目规则读取 `AGENTS.md`、`CODEX.md`、`docs/PROJECT_ENGINEERING_RULES.md`。
- 根据项目规则，只读任务也必须写入操作记忆并更新索引，因此本次仅新增记忆文档和索引条目。
- 不修改业务代码、不调整运行逻辑、不改数据库迁移、不改前端页面。

## 写入前验证

- `/home/tom/Desktop/fsu` 不存在。
- 实际项目目录为 `/home/tom/桌面/FSU`。
- `fsu-platform-java` 是 Git 仓库。
- 已读取 README、工程规则、后端 `pom.xml`、前端 `package.json`、主要源码/测试/fixture/docs 结构。
- 当前工作区已有未提交变更和生成文件变更，本次不回退、不覆盖。

## 实际修改文件

- `docs/memory/2026-05-19-READ-001-project-read.md`
- `docs/memory/README.md`

## 核心改动

记录本次项目读取结果，并在操作记忆索引中补充 READ-001 条目，满足项目闭环要求。

## 测试命令和结果

- `pwd`：确认当前主目录为 `/home/tom`。
- `find /home/tom -maxdepth 3 -type d -iname 'fsu'`：找到 `/home/tom/桌面/FSU`。
- `git status --short`：确认 `fsu-platform-java` 当前已有未提交变更。
- 多次 `find` / `sed`：读取项目说明、工程规则、源码、测试、fixture、文档和构建配置。

未运行单元测试；本次未修改业务代码。

## 风险

- 当前工作区存在大量既有变更和生成文件变更，后续任务需要先区分用户已有改动与新改动。
- 本次只读审计未验证构建或测试是否通过。

## 遗留问题

- 未深入阅读每个业务类实现。
- 未执行后端测试、前端构建或服务启动。
- 未清理现有 `target/`、`dist/`、`node_modules/` 等生成目录状态。

## 下一步建议

- 如需继续开发，先明确具体任务编号和业务范围。
- 开始写业务代码前，按项目模板输出规则加载确认、改动论证、写入前验证、计划修改文件、风险点和测试计划。
- 后续执行测试前，优先确认当前未提交变更是否均为预期状态。

## git diff 摘要

本次新增一份只读任务操作记忆，并更新记忆索引。

## git status 摘要

读取时工作区已存在：

- 已修改的 B-Interface 测试文件、`target/` 测试报告/编译产物、`docs/memory/README.md`、`docs/memory/WORKING-MEMORY.md`。
- 未跟踪的 `docs/audit/BIF-P4-020-active-alarm-audit-orchestration.md`、`docs/memory/2026-05-19-BIF-P4-020-active-alarm-audit-orchestration.md`、部分 `target/test-classes` 内部类文件。
