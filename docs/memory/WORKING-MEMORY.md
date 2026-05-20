# 工作记忆 — 当前会话状态

> 最后更新：2026-05-20 (OPENSPEC-CLI-001)
> 目的：会话重启后快速恢复上下文

---

## 当前阶段

**当前项目记忆显示最新阶段为 LANDING-008（被动上报接收联调准备），OPENSPEC-001（OpenSpec 规格驱动层接入）为最新任务。** 测试基线以最新 memory/audit 记录为准：1164 tests, 0 failures, 0 errors, 5 skipped。

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

## 2024/2016 协议联调进度

| 阶段 | 命令/内容 | Code | 状态 |
|------|-----------|------|------|
| LANDING-003 | 真实 FSU 只读联调 | — | ✅ |
| LANDING-004 | 兼容修复 + PK_Type 重试 | — | ✅ |
| LANDING-005 | 真实 FSUID 只读重试 | — | ✅ |
| LANDING-006 | B接口2016 码表兼容重试 | 401/1501 | ✅ |
| LANDING-007 | GET_DATA 真实 DeviceID 查询 | 401/402 | ✅ |
| LANDING-008 | 被动上报接收联调准备 | 101/501 | ✅ |
| OPENSPEC-001 | OpenSpec 规格驱动层接入 | — | ✅ |
| OPENSPEC-CLI-001 | OpenSpec CLI 安装与校验 | — | ✅ |

---

## 测试规模

| 阶段 | 测试数 | Failures |
|------|--------|----------|
| LANDING-003 后 | 1151 | 0 (5 skipped) |
| LANDING-004 后 | **1164** | **0** (5 skipped) |
| LANDING-008 后 | **1164** | **0** (5 skipped) |

---

## 当前阻塞

- FSU 不支持 GET_* 出站查询（所有 GET_* 返回空 DeviceList）
- FSU Code 分配表与 2024 标准不一致 (501→SEND_ALARM 而非 GET_DATA)
- 真实 SUID 未知
- 管理方未提供 DeviceID/SPID 点位表
- BInterfaceMessageLogService 待实现（报文日志）
- alarm_record 缺少 spid 列
- 父目录 `/home/tom/桌面/FSU/docs/` 存在过时规则副本
- Codex 集中复审暂缓 (累积: BIF-P4-FIX-001 ~ LANDING-008)

## 下一步建议

1. 向管理方确认 FSU 固件类型、协议版本、支持的命令列表
2. 获取真实 SUID
3. 确认 FSU 正确的命令 Code 分配表
4. 实现 BInterfaceMessageLogService 报文日志
5. 为 18 个已有 B接口命令逐个补充 `openspec/specs/` 规格文档
6. 清理父目录过时 docs 副本
7. 提交 Codex 集中复审

## OpenSpec 规格驱动层

OpenSpec 已接入。后续涉及协议/数据库/SET/Scheduler/告警/前端的任务，必须先创建 `openspec/changes/` change 文件，等待用户确认后编码。

- 规则文件：`docs/rules/CLAUDE_OPENSPEC_RULES.md`
- 项目上下文：`openspec/project.md`
- 强制场景：10 项（详见规则文件第3节）

### OpenSpec CLI

| 项目 | 值 |
|------|-----|
| CLI 版本 | **1.3.1** |
| CLI 路径 | `/home/tom/.npm-global/bin/openspec` |
| Node.js | v24.15.0 |
| 项目识别 | `openspec list` / `list --specs` 正常 |
| `openspec init` | 未执行（OPENSPEC-001 已手动创建结构） |

---

## 项目根目录

`/home/tom/桌面/FSU/fsu-platform-java/`
