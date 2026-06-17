# 工程记忆：SPEC-INIT-001 OpenSpec + Superpowers 项目工作流初始化

## 1. 时间
2026-05-20

## 2. 背景

项目已完成至 LANDING-008（被动上报接收联调准备）和 OPENSPEC-001/CLI-001（OpenSpec 规格驱动层接入）。OPENSPEC-001 创建了 `openspec/` 基础目录结构（project.md / specs / changes / archive）和 `CLAUDE_OPENSPEC_RULES.md` 规则文件，但规格目录未填充具体 spec/plan 文件。

用户决定后续使用 **OpenSpec + Superpowers** 工作流推进项目，要求建立完整的 spec → plan → memory → audit 闭环体系。

## 3. 本次目标

1. 检查项目现有 OpenSpec 目录结构
2. 补全缺失的目录和文件
3. 建立规格驱动开发规则（spec 是意图、plan 是步骤、memory 是结果、audit 是结论）
4. 为 LANDING-008 创建正式的 OpenSpec 规格草案
5. 为 LANDING-008 创建执行计划草案
6. 建立后续任务的 spec / plan / memory 对应关系

## 4. 新增文件

| 文件 | 说明 |
|------|------|
| `openspec/README.md` | OpenSpec 目录使用说明（工作流、四类文件定义、命名规则） |
| `openspec/specs/landing-008-passive-reporting-readiness.md` | LANDING-008 OpenSpec 规格（背景/目标/非目标/验收标准/风险） |
| `openspec/plans/landing-008-passive-reporting-readiness-plan.md` | LANDING-008 执行计划（读取资料/核对协议/核对代码/输出文档/验证） |
| `docs/memory/2026-05-20-SPEC-INIT-001-openspec-superpowers-workflow.md` | 本文件 |
| `docs/audit/SPEC-INIT-001-openspec-superpowers-workflow.md` | SPEC-INIT-001 审计文档 |

## 5. 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `openspec/project.md` | 更新 | 新增真实设备信息、协议结论、OpenSpec 工作流说明、已完成阶段明细 |
| `docs/memory/README.md` | 更新 | 新增 SPEC-INIT-001 条目 |
| `docs/memory/WORKING-MEMORY.md` | 更新 | 更新当前阶段、OpenSpec 结构说明 |

## 6. 目录变更

| 目录 | 操作 | 说明 |
|------|------|------|
| `openspec/plans/` | 新增 | 存放执行计划文件 |

## 7. 关键决策

### 7.1 OpenSpec + Superpowers 工作流定义

```
Spec（意图、边界、验收标准）
  → Plan（执行步骤）
    → 论证（基于 Spec/Plan）
      → TDD 实现（先测试、后代码）
        → 验证（verification-before-completion）
          → Code Review（requesting-code-review）
            → Memory 写入（执行结果）
              → Audit 写入（审计结论）
                → 下一阶段
```

### 7.2 四类文件定位

| 类型 | 含义 | 写入时机 |
|------|------|----------|
| **spec** | 意图、边界、验收标准 | 任务开始前 |
| **plan** | 执行步骤、验证命令 | 任务开始前 |
| **memory** | 执行结果、关键决策 | 任务完成后 |
| **audit** | 审计结论、协议一致性 | 完成后或审计时 |

### 7.3 强制规则

- 每个任务必须先有 spec 和 plan
- Claude 执行前必须读取对应 spec / plan
- 完成后必须写入 memory
- Codex 审计时必须读取 spec / plan / memory / audit
- spec 是意图，plan 是步骤，memory 是结果，audit 是结论

### 7.4 目录结构遵循

项目已有 `openspec/` 基础结构（来自 OPENSPEC-001），本次在此结构上扩展而非重建：
- 保留 `changes/` 和 `archive/`（用于变更提案）
- 新增 `plans/`（用于执行计划）
- 保留 `specs/`（用于能力规格）
- 新增 `README.md`（使用说明）

## 8. B接口协议依据

本次为纯文档/工作流初始化任务，不涉及 B接口协议实现。但 LANDING-008 规格和计划中完整引用了 2016 协议依据：

- 被动上报命令：LOGIN(101), SEND_ALARM(501)
- PK_Type 格式：请求 Name+Code，ACK 纯文本同名
- 协议版本确认：真实 FSU 使用 2016 码表
- SOAP 约束：1.1, RPC/encoded, operation=invoke

## 9. 安全边界

| 边界 | 本次状态 |
|------|----------|
| 修改 Java 生产代码 | **否** — 纯文档任务 |
| 访问真实 FSU | **否** |
| 启用 Scheduler | **否** |
| 执行 SET | **否** |
| 修改 alarm_record | **否** |
| 生成正式 seed SQL | **否** |
| 删除已有 memory/audit | **否** — 全部保留 |

## 10. 测试结果

本任务不修改 Java / SQL，不需要运行 Maven 测试。

测试基线保持在：**1164 tests, 0 failures, 0 errors, 5 skipped**。

## 11. 遗留问题

1. LANDING-008 的 spec 和 plan 已创建，但具体实施（BInterfaceMessageLogService 实现、spid 列补充等）待后续阶段执行
2. Codex 集中复审暂缓（累积项：BIF-P4-FIX-001 ~ LANDING-008）
3. 后续新增任务需按本工作流创建对应的 spec 和 plan 文件

## 12. 下一步建议

1. 按新工作流执行 LANDING-008 的后续实施步骤：
   - 实现 `BInterfaceMessageLogService` 报文日志
   - 补充 `alarm_record.spid` 列
2. 为已有 18 个 B接口命令逐个补充 `openspec/specs/` 规格文档
3. 提交 Codex 集中复审
4. 管理方提供 DeviceID/SPID 映射表后导入 monitoring_point
