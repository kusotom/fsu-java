# Claude 项目规则 — FSU 动环监控平台

> Claude 每次执行 FSU-JAVA 项目前，必须先读取本文件，并严格遵守所有规则。
> 最后更新：2026-05-20（OPENSPEC-001 OpenSpec 规格驱动层接入）

---

## 1. 项目协作分工

| 角色 | 职责 |
|------|------|
| **GPT** | 总体规划、阶段拆分、架构边界控制、任务提示词生成 |
| **Claude** | 代码开发、文件修改、测试补充、文档与工程记忆写入 |
| **Codex** | 代码审计、Bug 测试、协议一致性检查、安全边界检查 |

**Claude 约束：**
- 不允许擅自扩大任务范围
- 不允许跳过 GPT 规划直接做新阶段
- 不允许跳过 Codex 审计直接标记阶段完成

---

## 2. B接口协议唯一原则

FSU 通信、设备数据采集、设备查询、实时数据上报、告警上报、活动告警查询、点位数据、SET 控制命令、Scheduler 采集任务，**全部必须走 B接口协议**。

### 禁止事项

- 绕过 B接口协议直接采集设备数据
- 私自定义 FSU 私有 HTTP 接口
- 在业务代码中硬编码设备返回结果
- 使用非 B接口路径写入点位、告警、设备状态
- 用临时脚本替代正式协议链路
- 为测试通过而绕过协议模型

---

## 3. 先论证、再验证、后写入

Claude 每次修改代码前，必须先完成：

1. 阅读相关已有代码
2. 阅读 `docs/memory/` 工程记忆
3. 阅读 `docs/audit/` 审计文档
4. 判断本次任务阶段
5. 明确修改范围
6. 明确不修改范围
7. 说明 B接口协议依据
8. 说明安全边界
9. 输出修改前论证
10. 再开始写代码

**禁止直接上来改代码。**

---

## 4. 防止屎山代码原则

### 禁止事项

- Controller 写业务主逻辑
- Service 堆积大量无关职责
- Orchestrator 重复实现底层 Service 逻辑
- Handler 直接访问数据库做复杂业务
- Adapter 混入业务判断
- Repository 写协议解析逻辑
- 测试中复制大量生产逻辑
- 为了测试通过写死字段、写死 XML、写死返回值

### 推荐架构

```
Controller / Handler
  → Application Service / Orchestrator
    → Domain Service
      → Repository / Adapter
        → Protocol / XML / SOAP 工具层
```

---

## 5. 默认禁止真实访问 FSU

默认 `mvn test` 不允许访问真实 FSU，尤其禁止默认访问：

- `192.168.100.100`
- `http://192.168.100.100:8080/services/FSUService`

### 真实 FSU 联调测试要求

- 显式开关启用
- 默认关闭
- 不参与默认 `mvn test`
- 使用 `@Tag("real-fsu")`
- 使用 `@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")`
- 报告中说明是否真实访问设备

### 默认 mvn test 必须满足

- 不访问真实 FSU
- 不访问 `192.168.100.100`
- 不依赖真实网络

---

## 6. 默认禁止启用 Scheduler

除非当前任务明确要求实现 Scheduler，否则禁止：

- 启用 Scheduler
- 默认启动定时任务
- 默认周期性访问 FSU
- 测试中触发真实定时任务
- 应用启动时自动执行采集

### 涉及以下内容必须谨慎

- `@Scheduled`
- `@EnableScheduling`
- `TaskScheduler`
- `SchedulingConfigurer`
- `scheduler-enabled=true`

**默认要求：`scheduler-enabled=false`**

---

## 7. 默认禁止执行 SET 类命令

除非当前任务明确要求实现 SET 命令，并且已经完成安全门禁，否则禁止执行任何 SET 类命令。

### SET 命令清单

`SET_POINT`, `SET_THRESHOLD`, `SET_TIME`, `SET_SCIP`, `SET_SCHEMECONFIG`, `SET_FACTORYCONFIG`, `SET_SPCONFIGOPTION`, `SET_SUFTP`, `SET_RMCTRLCMD`, `SET_SUREBOOT`, `SET_FTP`, `SET_LOGININFO` 及所有 `SET_*` 命令。

### SET 类命令必须满足

- 有确认令牌 (confirmationToken)
- 有审计记录 (SetCommandAuditService)
- 有安全门禁 (SetCommandSafetyGate)
- 默认不真实下发 (dry-run)
- 测试中必须 mock
- 不允许误触发真实设备控制

---

## 8. 禁止自动修改 alarm_record

除非当前任务明确要求处理 SEND_ALARM 告警入库，否则禁止自动修改 `alarm_record`。

在活动告警审计、差异比对、GET_ACTIVEALARM 等任务中，**默认只读**。

### 禁止生产代码自动执行

`save()`, `saveAll()`, `delete()`, `deleteById()`, `deleteAll()`, `update()`, `insert()`, `recover()`, `clear()`, `ack()`, `confirm()`

### 活动告警审计类任务只允许

- 读取平台当前告警
- 读取 FSU 当前告警
- 计算差异
- 输出审计结果

**不允许自动恢复、清除、新增、确认告警。**

---

## 9. 当前项目状态基线 (2026-05-19)

### 已完成

| 模块 | 状态 |
|------|------|
| LOGIN 会话闭环 | ✅ |
| HEARTBEAT 心跳闭环 | ✅ |
| SEND_DATA 实时数据上报 | ✅ |
| SEND_ALARM 告警上报 | ✅ |
| GET_DATA 查询 | ✅ |
| GET_THRESHOLD 查询 | ✅ |
| GET_ACTIVEALARM 查询 | ✅ |
| ActiveAlarmDiffService | ✅ |
| LocalActiveAlarmSnapshotService | ✅ |
| ActiveAlarmConsistencyAuditService | ✅ |
| SET 安全门禁基础能力 | ✅ |
| FsuServiceClient (Stub + RealHttp) | ✅ |
| FsuServiceRpcAdapter (WSDL RPC/encoded) | ✅ |
| SOAP RPC/encoded 适配 | ✅ |
| xmlData 解析与构建 | ✅ |
| B接口命令分发 (18 Handler) | ✅ |
| docs/memory 工程记忆机制 | ✅ |
| docs/audit 审计文档机制 | ✅ |
| BIF-P4-FIX-001 阻断修复 | ✅ |

### 测试基线

当前项目记忆显示最新阶段为 LANDING-008，测试基线以最新 memory/audit 记录为准：

- 最新全量记录: **1164 tests, 0 failures, 0 errors, 5 skipped**
- contextLoads: **PASS**

---

## 10. BIF-P4-020 Codex 审计结论（已修复）

原审计结论（修复前）：不通过。

### Blocker（已全部修复 ✅）
1. ~~默认 mvn test 会执行真实 FSU 集成测试~~ → BifP4005RealFsuIntegrationTest 已隔离（@Tag + @Disabled）
2. ~~默认测试访问 192.168.100.100~~ → 默认 5 skipped
3. ~~全量 mvn test 仍有 1 个 Error~~ → 0 errors (1087 tests, 0 failures)

### Major（已全部修复 ✅）
1. ~~GET_ACTIVEALARM 出站报文未证明携带 Code=603~~ → SoapMessageHandler 支持 Name+Code 双标识格式：
   ```xml
   <PK_Type>
     <Name>GET_ACTIVEALARM</Name>
     <Code>603</Code>
   </PK_Type>
   ```
2. ~~SoapMessageHandler 仍可能输出纯文本~~ → 新 `buildRequest(name, code, ...)` 方法 + 旧格式兼容
3. ~~realDeviceAccessed 硬编码 false~~ → FsuServiceResponse.isRealCall() → GetActiveAlarmResult → AuditResult 全链路透传
4. ~~BInterfaceMessageLogRepository Bean 冲突~~ → 删除重复文件 + 修复查询方法 + SetTimeService @Autowired

### Minor（已全部修复 ✅）
1. ~~缺少标准 `<TAlarm>` 包裹测试~~ → GetActiveAlarmServiceTest.shouldParseAlarmWrappedInTAlarm
2. ~~缺少 xmlData 解析失败测试~~ → GetActiveAlarmServiceTest.xmlDataWithUnexpectedFormatShouldNotCrash
3. ~~缺少 CommandHandler 缺失 SUID 测试~~ → GetActiveAlarmCommandHandlerTest (新增，4 tests)
4. ~~缺少 BIF-P4-020 专属不启 Scheduler / 不触发 SET 测试~~ → ConsistencyAuditTest (2 tests)
5. ~~alarm_record 缺少 serialNo/deviceId~~ → 已知限制，非本次范围

### 唯一遗留

- `alarm_record` Entity 缺少 serialNo/deviceId 字段，限制 FSU 告警与本地记录的精确 SerialNo 匹配能力

---

## 11. 当前项目进度

项目已推进至 **LANDING-008**（被动上报接收联调准备）。

| 阶段 | 内容 |
|------|------|
| BIF-P0 ~ BIF-P4 | 协议/命令/联调/SET安全 完成 |
| BIF-P4-FIX-001 | Codex 审计阻断修复 完成 |
| BIF-P4-021 ~ 023 | 活动告警审计 Scheduler/API/持久化 完成 |
| LANDING-001 ~ 002 | 点位表模型补强/模板 完成 |
| LANDING-003 ~ 008 | 真实 FSU 联调/兼容性/码表/DeviceID/被动上报 完成 |
| OPENSPEC-001 | OpenSpec 规格驱动层接入 完成 |

**当前优先任务：等待 Codex 集中复审（累积项：BIF-P4-FIX-001 ~ LANDING-008）。**

后续所有涉及协议/数据库/SET/Scheduler/告警/前端的任务，必须先通过 OpenSpec change 论证，再进入编码。

---

## 12. Claude 每次启动必须加载

按顺序阅读：

1. `docs/rules/CLAUDE_PROJECT_RULES.md`（本文件）
2. `docs/rules/CLAUDE_OPENSPEC_RULES.md`（OpenSpec 规格驱动层规则）
3. `openspec/project.md`（项目 OpenSpec 上下文）
4. `docs/memory/README.md`
5. `docs/memory/WORKING-MEMORY.md`
6. `docs/audit/`（相关审计文档）
7. `openspec/changes/`（进行中的变更提案，如有）

如任务涉及某个阶段，还必须阅读对应 memory / audit 文件。

---

## 13. 修改前论证模板

Claude 每次输出修改前必须先完成以下论证：

```markdown
## 修改前论证

### 1. 本次任务目标
### 2. 当前相关代码位置
### 3. 拟修改文件
### 4. 不修改范围
### 5. B接口协议依据
### 6. 安全边界
### 7. 测试计划
```

**只有输出该论证后，才允许修改代码。**

---

## 14. 工程记忆规则

每次 Claude 执行项目操作后，必须新增 memory 文件：

```
docs/memory/YYYY-MM-DD-任务编号-任务名称.md
```

内容至少包括：

```markdown
# 工程记忆：任务编号 任务名称

## 1. 时间
## 2. 背景
## 3. 本次目标
## 4. 修改文件
## 5. 关键决策
## 6. B接口协议依据
## 7. 安全边界
## 8. 测试结果
## 9. 遗留问题
## 10. 下一步建议
```

同时必须更新：

- `docs/memory/README.md`（索引条目）
- `docs/memory/WORKING-MEMORY.md`（当前阶段、测试数、阻断问题、下一步建议）

---

## 15. 审计文档规则

涉及以下情况时，必须新增或更新 `docs/audit/`：

- 协议实现
- 安全门禁
- Scheduler
- 真实 FSU 调用
- 活动告警审计
- SET 命令
- Repository 写入边界
- Codex 审计问题修复
- 阶段验收结论

审计文档至少包括：

```markdown
# 审计文档：任务编号 任务名称

## 1. 审计目标
## 2. 背景
## 3. 修改范围
## 4. 协议一致性
## 5. 安全边界
## 6. 测试验证
## 7. 风险与遗留问题
## 8. 结论
```

---

## 16. 测试规则

### 默认 mvn test 必须满足

- 不访问真实 FSU
- 不访问 `192.168.100.100`
- 不启用 Scheduler
- 不执行 SET
- 不自动修改 alarm_record

### 测试执行顺序

```bash
mvn test -Dtest='*ActiveAlarm*'
mvn test -Dtest='*BInterface*'
mvn test -Dtest='DcimPlatformApplicationTests'
mvn test  # 全量
```

### 测试报告要求

- 禁止把 `0 failures, 1 error` 写成全量通过
- 正确写法："全量测试存在 1 个 Error，不能视为全量通过"
- 必须说明失败是否本次引入

---

## 17. BIF-P4-FIX-001 专项规则

以下 5 项修复规则已落地，后续任务必须遵守：

### 17.1 默认测试真实 FSU 隔离

- 文件: `BifP4005RealFsuIntegrationTest.java`
- 注解: `@Disabled` + `@Tag("real-fsu")` + `@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")`
- 默认行为: `mvn test` 跳过，5 skipped
- 手动启用: `mvn test -DrealFsuTest.enabled=true -Dtest=BifP4005RealFsuIntegrationTest`

### 17.2 Repository Bean 冲突禁止用 bean override

- 删除 `log/BInterfaceMessageLogRepository.java`（零引用）
- 修复查询方法名匹配 Entity 字段: `findByFsuId` → `findByFsuCode`
- 多构造函数的 Service 加 `@Autowired` 标注主构造器
- 目标: `contextLoads` 必须通过

### 17.3 GET_ACTIVEALARM Code=603 出站证明

- `SoapMessageHandler.buildRequest(String, Integer, String, String)` — 2024 Name+Code 格式
- `RealHttpFsuServiceClient.call()` — 通过 `BInterfaceCommandAliasMapper.to2024()` 查 code
- 测试: 检查最终 XML/SOAP 字符串含 `<Code>603</Code>`，不只查枚举
- 旧格式 `buildRequest(String, String, String)` 保留兼容

### 17.4 realDeviceAccessed 全链路透传

- `FsuServiceResponse.isRealCall()` → `GetActiveAlarmResult.isRealDeviceAccessed()` → `ActiveAlarmConsistencyAuditResult`
- `auditWithProvidedSnapshot()` → false（不访问 FSU）
- `auditByQueryingFsu()` → 从 `fsuResult` 透传
- 测试: mock isRealCall=true/false 两种场景

### 17.5 缺口测试清单

- [x] `<TAlarm>` 包裹 → `GetActiveAlarmServiceTest.shouldParseAlarmWrappedInTAlarm`
- [x] xmlData 异常格式 → `GetActiveAlarmServiceTest.xmlDataWithUnexpectedFormatShouldNotCrash`
- [x] 缺失 SUID → `GetActiveAlarmCommandHandlerTest.shouldFailForMissingSuid`
- [x] 不启 Scheduler → `ActiveAlarmConsistencyAuditServiceTest.shouldNotEnableScheduler`
- [x] 不触发 SET → `ActiveAlarmConsistencyAuditServiceTest.shouldNotTriggerSetCommand`
- [x] Code=603 出站 → `SoapMessageHandlerTest.buildRequest2024ShouldContainNameAndCode`
- [x] 默认不访问真实 FSU → `BifP4005RealFsuIntegrationTest` 默认 @Disabled

---

## 18. 下一阶段限制

在以下条件全部满足前，**禁止进入 BIF-P4-021 Scheduler**：

1. ✅ 默认 mvn test 不访问真实 FSU
2. ✅ DcimPlatformApplicationTests.contextLoads 通过
3. ✅ GET_ACTIVEALARM 出站 Code=603 有测试证明
4. ✅ realDeviceAccessed 已从底层响应透传
5. ✅ 全量测试结果可准确说明（以最新 memory/audit 记录为 1164 tests, 0 failures, 0 errors, 5 skipped）
6. ⬜ Codex 复审通过或有条件通过

---

## 19. Claude 输出约束

Claude 每次输出不能只说"已完成"。必须提供：

1. 修改文件清单
2. 关键代码变更说明
3. 协议一致性说明
4. 安全边界说明
5. 实际执行的测试命令
6. 测试结果原始摘要
7. 是否有失败
8. 失败是否本次引入
9. 文档 / memory 写入情况
10. 是否建议交给 Codex 审计

---

## 20. 最终原则

```
先架构，后代码。
先协议，后实现。
先安全边界，后功能扩展。
先可测试，后联调。
先 memory，后下一阶段。
```

本项目宁可慢一点，也不能：

- 把 B接口协议链路写乱
- 让默认测试真实访问设备
- 让 SET 或 Scheduler 在未授权情况下自动执行
- 绕过安全门禁操作告警数据

---

## 21. OpenSpec 规格驱动层

项目已引入 OpenSpec 作为规格驱动层。详细规则见 `docs/rules/CLAUDE_OPENSPEC_RULES.md`。

### 21.1 核心原则

OpenSpec 嵌入现有论证流程：

```
OpenSpec Change → 论证(基于Change) → 验证 → 写入 → Audit/Memory闭环
```

### 21.2 强制场景

以下场景必须先创建 OpenSpec change，等待用户确认后才能编码：

1. B接口协议新增命令
2. B接口协议兼容改造
3. FSU 真实联调
4. 数据库结构改动
5. Entity / Repository / Service / Controller 改动
6. Scheduler 改动
7. SET 类操作
8. 告警 / 审计 / 持久化改动
9. 前端运维页面
10. Codex 审计修复任务

### 21.3 优先级

如本文件与 `CLAUDE_OPENSPEC_RULES.md` 冲突，以本文件为准。

### 21.4 规则文件

- 规则定义：`docs/rules/CLAUDE_OPENSPEC_RULES.md`
- 项目上下文：`openspec/project.md`
- 变更提案：`openspec/changes/`
- 已归档：`openspec/archive/`
- 已有能力规格：`openspec/specs/`
