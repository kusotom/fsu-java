# FSU-JAVA 当前工作记忆

## 最新任务：SPEC-DICT-P0-001 B接口2016字典码表全量分析 (2026-05-27)

按用户“全量分析，不可跳过、省略、精简、编造”的要求，补齐 B接口2016 主协议 docx 与设备信号字典 xlsx 的字典码表矩阵。新增 `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md`，共 1114 条，覆盖协议常量、枚举、设备编码表、PK_Type/命令码/ACK码、LOGIN相关枚举、FTP图片规则、告警文本枚举、工程ResultCode、Emerson实测差异、xlsx说明、局站类型编码、设备/系统类型编码、设备编码、设备信号字典。新增 `unknown-dictionary-items.md`，记录 12 条待确认项，包括 EVENT_LENGTH 重复、设备编码229重复、EnumDeviceType预留范围冲突、GET_HISDATA_ACK方向冲突、ResultCode非2016标准字段、LOGIN Version/Vervion不一致、PaSCword大小写异常、TAlarm DEVICEICODE_LEN拼写异常、xlsx SignalID `0316005001` 重复。未修改 Java 生产代码、SQL schema、前端页面；未访问真实FSU；未启动Scheduler；未执行SET类命令。

补充实测：2026-05-27 20:34 按用户要求读取蓄电池电压，执行只读 `BInterface2016ReadOnlyRealFsuRunOnceTest#getData_readOnly_knownSensorStatus`。真实FSU返回 `GET_DATA_ACK / 402`、`Result=1`，蓄电池组设备 `51051240700002` 下 `TSemaphore Id=0407102001` 返回 `MeasuredVal=53.9`、`Status=0`。按字典矩阵 `SPEC-2016-DICT-XLSX-SIGNAL-0256`，该点为D类机房蓄电池组总电压，单位V。raw样本前缀：`SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-*`。未执行SET，未启动Scheduler，未写业务数据库。

## 上一任务：SPEC-TEST-P0-001 真实FSU只读Run-Once (2026-05-26)

按用户要求完成真实FSU只读测试：新增 `BInterface2016ReadOnlyRealFsuRunOnceTest`，默认跳过，仅 `-DrealFsuTest.enabled=true` 执行。已访问真实FSU `http://192.168.100.100:8080/services/FSUService`，执行GET_DATA、GET_LOGININFO、GET_FTP、GET_FSUINFO、GET_THRESHOLD；另补跑 `Landing015RealFsuIntegrationTest` 验证服务层主动GET_FSUINFO状态读取。未执行SET、未启动Scheduler、未写业务数据库。结果：GET_DATA 401/402 `Result=1` 空DeviceList；GET_LOGININFO 1501/1502返回SCIP和4个Device但缺`Result`；GET_FTP 1601/1602 `Result=1`且返回FTP账号字段；GET_FSUINFO 1701/1702返回CPU/MEM但`Result=0`；GET_THRESHOLD 1901/1902 `Result=1`但未返回TThreshold明细。传感器状态专项GET_DATA显式Id查询返回8个TSemaphore，MeasuredVal均为0或0.0，因缺厂家点位表不能可靠标注电压/电流/温湿度名称。Emerson差异登记为 `[SPEC-2016-PROFILE-EMERSON-004]`、`[SPEC-2016-PROFILE-EMERSON-005]`、`[SPEC-2016-PROFILE-EMERSON-006]`，不得覆盖标准2016 `SUCCESS=1`。

# 工作记忆 — 当前会话状态

> 最后更新：2026-05-27 (SPEC-DICT-P0-001: B接口2016字典码表全量分析)
> 目的：会话重启后快速恢复上下文

---

## 当前阶段

**当前阶段：B接口前端运营观察控制台基本闭合。** 前端 ~90-93%, 后端 ~84-87%, 项目整体 ~85-88%. 测试基线：**1297 tests, 0/0/9**。Auth/权限系统前后端闭环。B接口8+5页全部真实API接入。阻塞：DeviceID→SPID/SignalID映射表缺失。下一阶段：DATA-MAPPING-001 点位映射规划。

### CODEX-AUDIT-2016-FULL-001 结论摘要

| 优先级 | 结论 |
|------|------|
| P0 | GET_DATA 未支持 Emerson DeviceList/TSemaphore，SEND_ALARM 无真实样本验收，raw-samples 缺真实 SOAP 文件 |
| P1 | BInterfaceCommand2016 未覆盖 15 命令+ACK，BInterfacePkType 缺 LOGOUT/SET_LOGININFO，存在 2024 alias 误用风险 |
| P2 | LOGOUT、GET_HISDATA、SET_LOGININFO 未实现；SET_POINT、SET_FTP 仅 stub；TIME_CHECK 未真实验证 |
| P3 | 项目内 docs/PROJECT_ENGINEERING_RULES.md 缺失，SPEC-2016 memory 索引指向不存在文件 |
| 安全 | SET/Scheduler/real-call 默认关闭，未发现本次需立即修改代码的安全事故；`TIME_CHECK` 被 legacy SET 名称集合包含需后续澄清 |

### 真实 FSU 联调结论 (截至 LANDING-008)

| 假说 | 验证结果 |
|------|----------|
| PK_Type 格式不匹配 | ❌ 排除 — structured/legacy 行为一致 |
| FSUID/FSUCode 不被识别 | ❌ 排除 — 51051243812345 与 FSU-001 结果完全相同 |
| FSU 不支持 GET_* 出站查询 | ✅ **确认** — FSU 对所有 GET_* 命令返回空响应 |
| Code=501 路由异常 | ✅ **确认** — structured GET_DATA Code=501→SEND_ALARM |
| 2016 Code 兼容性 | ✅ **突破** — 2016 Code(GET_DATA=401/GET_LOGININFO=1501)获取真实数据: CPU 14.95%/MEM 62.84%/4 DeviceIDs |
| GET_DATA 真实 DeviceID 查询 | ✅ **完成** — 4策略空响应, FSU 当前无测量数据 |
| 被动上报接收链路 | ✅ **核对完成** — SCService 4命令链路全部就绪 |

## 项目规则

1. B接口协议是唯一设备采集/上报协议
2. 禁止重新引入 DSC/RDS
3. 每次代码操作必须先论证、再验证、后写入
4. 每次任务完成必须写入 docs/memory 工程记忆
5. Claude / Codex / AI Agent 每次启动必须加载项目规范
6. SET 类命令必须经过安全门禁、confirmationToken、审计和人工确认

---

## 真实设备联调状态

| 项目 | 值 |
|------|-----|
| IP | 192.168.100.100 |
| Port | 8080 |
| Endpoint | `/services/FSUService` |
| 完整地址 | `http://192.168.100.100:8080/services/FSUService` |
| 临时 fsuCode | FSU-001 (不等同于真实 SUID) |

### LANDING-003 (已完成)
- 6 个只读命令 HTTP 200 全部可达
- FSU 对所有 GET_* 返回空 `<invokeReturn/>`
- GET_DATA 返回 SEND_ALARM 结构 + 嵌套 XML 声明
- 24 个原始 XML 报文已保存

### LANDING-004 (已完成)
- 嵌套 XML 声明容错修复: `FsuServiceRpcAdapter.stripXmlDeclaration()`
- PK_Type 兼容模式: `FsuServiceRequest.pkTypeFormat` (structured/legacy-text)
- Structured vs Legacy 重试: 10 次真实调用, 40 个新 XML 报文
- **结论**: PK_Type 格式不是根因，FSU 不支持 GET_* 出站查询
- GET_DATA Code=501 被 FSU 路由到 SEND_ALARM (与 2024 标准 SEND_ALARM=601 不一致)

### 安全边界 (保持)
- 未执行任何 SET 类命令
- 未启用 Scheduler
- 未修改 alarm_record 状态
- 未生成正式 seed SQL
- 真实 FSU 测试默认不执行 (`@Tag("real-fsu")` + `@EnabledIfSystemProperty`)

---

## 协议基线 (PROTOCOL-BASELINE-001, 2026-05-21)

| 版本 | 定位 | 说明 |
|------|------|------|
| **B接口2016** | **当前主开发依据** | 真实 FSU 51051243812345 已验证 |
| B接口2024 | 未来升级版本 / 兼容层 | 保留实现，非当前默认依据 |

2016 关键码表: LOGIN=101, GET_DATA=401→402, GET_LOGININFO=1501→1502, GET_FSUINFO=1701→1702, GET_FTP=1601→1602, SEND_ALARM=501。

---

## 2024/2016 协议联调进度

| 阶段 | 命令/内容 | Code | 状态 |
|------|-----------|------|------|
| LANDING-003 | 真实 FSU 只读联调 | — | ✅ |
| LANDING-004 | 兼容修复 + PK_Type 重试 | — | ✅ |
| LANDING-005 | 真实 FSUID 只读重试 | — | ✅ |
| LANDING-006 | B接口2016 码表兼容重试 | 401/1501 | ✅ |
| LANDING-007 | GET_DATA 真实 DeviceID 查询 | 401/402 | ✅ |
| LANDING-008 | 被动上报接收联调准备 (文档+实施) | 101/501 | ✅ |
| OPENSPEC-001 | OpenSpec 规格驱动层接入 | — | ✅ |
| OPENSPEC-CLI-001 | OpenSpec CLI 安装与校验 | — | ✅ |
| SPEC-INIT-001 | OpenSpec + Superpowers 工作流初始化 | — | ✅ |
| LANDING-009 | alarm_record 落地字段幂等 DDL 补齐 | — | ✅ |
| LANDING-010 | BInterfaceMessageLog 日志清理与查询增强 | — | ✅ |
| LANDING-011 | FSU → SCService 被动上报真实联调 | — | ✅ (操作指引) |
| LANDING-012 | /services/SCService 标准入口 | — | ✅ |
| LANDING-013 | BInterfaceMessageLog 持久化修复 | — | ✅ |
| LANDING-013-FIX-001 | Schema/Entity command_code 对齐 | — | ✅ (真实 LOGIN 日志 id=399/400) |
| LANDING-014 | 连续上报观察与入库验证 | — | ✅ (观察指引) |
| LANDING-015 | 2016 GET_FSUINFO 心跳轮询 | Code=1701 | ✅ |

---

## 测试规模

| 阶段 | 测试数 | Failures |
|------|--------|----------|
| LANDING-003 后 | 1151 | 0 (5 skipped) |
| LANDING-004 后 | **1164** | **0** (5 skipped) |
| LANDING-012 后 | **1204** | **0** (5 skipped) |

---

## 当前阻塞

- FSU 不支持 GET_* 出站查询（所有 GET_* 返回空 DeviceList）
- FSU Code 分配表与 2024 标准不一致 (501→SEND_ALARM 而非 GET_DATA)
- 真实 SUID 未知
- 管理方未提供 DeviceID/SPID 点位表
- BInterfaceMessageLogService 待实现（报文日志）→ ✅ **已实现且真实 LOGIN 已入库**
- alarm_record 缺少 spid 列 → ✅ **已补齐 (Entity)**
- alarm_record 落地 DDL → ✅ **已补齐 (LANDING-009)**
- 父目录 `/home/tom/桌面/FSU/docs/` 存在过时规则副本
- Codex 集中复审暂缓 (累积: BIF-P4-FIX-001 ~ LANDING-008)

## 下一步建议

## 当前阻塞

TASK-002 阻塞：缺少真实 DeviceID → SPID/SignalID 映射表。

## 下一阶段：前端适配

FE-TODO-001：FSU 总览与状态页 → 后续 FE-TODO-002~011

## 后续后端任务（等待点位数据后恢复）

- POINT-MAPPING-AUDIT-001：检查点位唯一键
- 等待管理方提供 DeviceID→SPID/SignalID 映射表
- 拿到点位后恢复 TASK-002 (GET_DATA/GET_THRESHOLD 真实联调)

## OpenSpec + Superpowers 规格驱动层

OpenSpec + Superpowers 工作流已初始化（SPEC-INIT-001）。后续涉及协议/数据库/SET/Scheduler/告警/前端的任务，必须先创建 spec 和 plan，再进入编码。

### 目录结构

```
openspec/
├── README.md              ← 使用说明
├── project.md             ← 项目上下文（技术栈/协议/设备/安全配置/阶段）
├── specs/                 ← 已有能力的规格文档
│   └── landing-008-passive-reporting-readiness.md
├── plans/                 ← 执行计划文档
│   └── landing-008-passive-reporting-readiness-plan.md
├── changes/               ← 进行中的变更提案（需用户确认）
└── archive/               ← 已完成并归档的变更
```

### 四类文件对应关系

| 类型 | 位置 | 含义 | 写入时机 |
|------|------|------|----------|
| **spec** | `openspec/specs/` | 意图、边界、验收标准 | 任务开始前 |
| **plan** | `openspec/plans/` | 执行步骤、验证命令 | 任务开始前 |
| **memory** | `docs/memory/` | 执行结果、关键决策 | 任务完成后 |
| **audit** | `docs/audit/` | 审计结论、协议一致性 | 完成后或审计时 |

### 规则文件

- 最高规则：`docs/rules/CLAUDE_PROJECT_RULES.md`
- 规格驱动层规则：`docs/rules/CLAUDE_OPENSPEC_RULES.md`
- 项目上下文：`openspec/project.md`
- 强制场景：10 项（详见 CLAUDE_OPENSPEC_RULES.md 第3节）

### OpenSpec CLI

| 项目 | 值 |
|------|-----|
| CLI 版本 | **1.3.1** |
| CLI 路径 | `/home/tom/.npm-global/bin/openspec` |
| Node.js | v24.15.0 |
| 项目识别 | `openspec list` / `list --specs` 正常 |
| `openspec init` | 未执行（手动创建结构） |

---

## AI-FLOW-001-REV2 工程流系统 (2026-05-24)

6-Agent 分工模型已正式写入项目工程流系统。

### Agent 分工

| 组 | Agent | 实现者 | 职责 |
|----|-------|--------|------|
| GPT | GPT-Architect | — | 总规划/架构决策/阶段路线/任务拆分 |
| GPT | GPT-Protocol-Agent | GPT-Architect | B接口2016 协议专家 |
| Claude | Claude-Developer | — | 默认主开发/前后端/dry-run |
| Claude | Claude-Memory-Agent | Claude-Developer | 记忆/audit/WORKING-MEMORY/context-pack |
| Codex | Codex-Reviewer | — | 默认审计/代码审计/安全边界检查 |
| Codex | Codex-Deploy-Agent | Codex-Reviewer | 部署运维/Ubuntu/PostgreSQL/Nginx |

### 工程流配置文件

| 路径 | 说明 |
|------|------|
| `.agent/agents.yml` | 6 Agent 定义+权限 |
| `.agent/policies.yml` | 策略/默认Agent/所有权/禁止操作/闭环条件 |
| `.agent/workflow.yml` | 7 状态工作流 + 3 阶段门禁 |
| `.agent/locks.yml` | 任务锁管理 |
| `.agent/current-task.yml` | 当前任务追踪 |
| `.agent/tasks/DATA-MAPPING-006.yml` | DATA-MAPPING-006 预置 (PLANNED) |
| `docs/ai-team/` (10 文件) | AI Team 角色/权限/工作流/安全门禁/检查清单/模板/决策日志 |
| `tools/agentflow/agentflow.py` | **工程流 CLI MVP** — status/start/check/finish/release/pack/decision |

### agentflow.py CLI (AI-FLOW-002 + AI-FLOW-002-FIX-001, 2026-05-24)

**标准入口:** `cd /home/tom/桌面/FSU && python3 tools/agentflow/agentflow.py <command>`

**7 命令:**
- `status` — 当前任务/锁/任务列表
- `start <ID> --agent <A>` — 启动 (PLANNED/APPROVED → IN_PROGRESS) + 记录 baseline
- `check <ID>` — required_outputs/forbidden_paths/actions/memory/audit (forbidden_paths 命中 → exit 1)
- `finish <ID>` — 完成 (IN_PROGRESS → SELF_TESTED, 阻止 forbidden_paths)
- `release <ID>` — 释放锁
- `pack <ID> --for gpt` — 生成 context-pack
- `decision <ID> --from <F>` — GPT 决策 (PASS → COMPLETED)

**安全门禁 (AI-FLOW-002-FIX-001):**
- baseline 机制: start 记录 git diff 快照, check 用 current - baseline 过滤历史 dirty diff
- forbidden_paths 命中 → check 返回非 0 退出码
- finish 阻止 forbidden_paths 违规

典型使用：
```bash
cd /home/tom/桌面/FSU
python3 tools/agentflow/agentflow.py status
python3 tools/agentflow/agentflow.py start AI-FLOW-002-FIX-001 --agent Claude-Developer
python3 tools/agentflow/agentflow.py check AI-FLOW-002-FIX-001
python3 tools/agentflow/agentflow.py pack AI-FLOW-002-FIX-001 --for gpt
python3 tools/agentflow/agentflow.py finish AI-FLOW-002-FIX-001
python3 tools/agentflow/agentflow.py release AI-FLOW-002-FIX-001
```

### 关键规则

- GPT 负责规划、架构、协议判断和阶段决策，不直接改本地代码
- Claude 是默认主开发 Agent，同时负责 Memory-Agent
- Codex 是默认审计 Agent，同时负责 Deploy-Agent
- 协议专家由 GPT 扮演，部署运维由 Codex 扮演，记忆审计维护由 Claude 扮演
- 所有任务通过 `.agent/tasks/*.yml` 管理
- 任务闭环前必须更新 memory/audit/WORKING-MEMORY

## SPEC-TEST-P0-001 B接口2016 标准主命令测试建设 (2026-05-26)

本次只新增测试、fixture、audit/memory，不修改生产业务代码、SQL或前端。

新增测试覆盖：

- `BInterface2016StandardCommandMatrixTest`
- `BInterface2016ResultSemanticsTest`
- `BInterface2016XmlSampleReplayTest`
- `BInterface2016ScServiceCommandTest`
- `BInterface2016FsuServiceReadOnlyCommandTest`
- `BInterface2016SetCommandSafetyTest`
- `BInterface2016EmersonProfileCompatibilityTest`

新增 fixture：

- `backend/src/test/resources/fixtures/b_interface_2016_standard/`：18个标准2016样本
- `backend/src/test/resources/fixtures/b_interface_2016_emerson/`：Emerson GET_DATA空DeviceList样本

验证命令：

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/backend
mvn test -Dtest='*BInterface2016*'
```

结果：编译通过，测试失败，`Tests run: 110, Failures: 6, Errors: 0, Skipped: 0`。

失败点为当前实现与 SPEC-P0-001 的差异：

- `PkTypeDescriptor` 用2024码表判定2016 `GET_DATA/401` 不一致
- `CommandResult.success()` 仍输出 `ResultCode=0`，标准2016应为 `Result=1`
- SCService ACK未输出 `LOGIN_ACK/102` 的标准Name+Code结构
- `GET_LOGININFO`、`GET_FTP`、`GET_THRESHOLD`、`TIME_CHECK` 出站只读服务未显式使用 `legacy-2016`
- SET安全门未识别 `SET_LOGININFO`
- `SET_THRESHOLD`、`SET_LOGININFO` 未标记高风险

安全边界：未执行SET、未启用真实FSU、未启动Scheduler。运行现有 `BInterface2016ReadOnlyRunOnceProbeIntegrationTest` 时写入了 `backend/docs/landing/raw-samples/*20260526-224049.xml`，属于既有测试副作用。

## 项目根目录

`/home/tom/桌面/FSU/fsu-platform-java/`
