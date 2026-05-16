# 工作记忆 — 当前会话状态

> 最后更新：2026-05-16
> 目的：会话重启后快速恢复上下文

---

## 当前阶段

**BIF-P4-020 完成**：GET_ACTIVEALARM + ActiveAlarmDiff 编排审计闭环。活动告警一致性审计完整链路已建立。

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
| 厂商/型号 | 艾默生 FSU-2808IM |
| IP | 192.168.100.100 |
| Port | **8080** |
| Endpoint | `/services/FSUService` |
| 完整地址 | `http://192.168.100.100:8080/services/FSUService` |

### 关键突破

- 确认 FSUService 必须使用 WSDL RPC/encoded invoke(xmlData) 线格式
- 新增并验证 **FsuServiceRpcAdapter**
- 解决 `Method 'Request' not implemented` SOAP Fault
- HTTP 500 → HTTP 200
- GET_DATA ResultCode=0 ✅
- GET_THRESHOLD ResultCode=0 ✅
- 真实设备已接受 `<ns1:invoke>` + `xmlData @xsi:type` 格式

### 当前阻塞

- FSU 管理方尚未提供真实 SUID/DeviceID/SPID 点位表
- 当前平台点位仍有占位值 (TEMP-R01, HUMI-R01)
- GET_DATA/GET_THRESHOLD 数据可能为空（真实点位未对齐）
- 未生成真实 seed SQL

---

## 2024 协议迁移进度

| 阶段 | 命令 | Code | 状态 |
|------|------|------|------|
| BIF-P4-009 | PDF vs MD 差异审计 | — | ✅ |
| BIF-P4-010 | 2024 命令枚举+别名映射 | — | ✅ |
| BIF-P4-011 | PK_Type Name+Code 解析 | — | ✅ |
| BIF-P4-012 | GET_SUINFO 在线状态 | 1001 | ✅ |
| BIF-P4-013 | GET_SUFTP FTP 查询 | 801 | ✅ |
| BIF-P4-014 | SET_TIME 时间同步 | 901 | ✅ |
| BIF-P4-015 | GET_SPCONFIGOPTION 配置模板 | 401 | ✅ |
| BIF-P4-016 | GET_ACTIVEALARM 活动告警 | 603 | ✅ |
| BIF-P4-017 | SUREADY 注册验证 | 103 | ✅ |
| BIF-P4-018 | ActiveAlarmDiff 差异核对 | — | ✅ |
| BIF-P4-019 | alarm_record 只读适配 | — | ✅ |
| BIF-P4-020 | GET_ACTIVEALARM+diff 编排 | — | ✅ |

### 2024 主链路状态

```
LOGIN → SUREADY → GET_SUINFO → GET_DATA / GET_ACTIVEALARM
```

---

## SET 安全体系进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| BIF-P4-SAFE-001 | SET 类命令统一安全门禁 | ✅ |
| BIF-P4-SAFE-002 | SET_TIME 接入安全门禁 | ✅ |
| BIF-P4-SAFE-003 | confirmationToken + SET 审计 | ✅ |
| BIF-P4-SAFE-004 | SET_TIME token+audit 主流程样板 | ✅ |

### 安全边界

- 未真实执行任何 SET 类命令
- 未启用 Scheduler
- 未保存明文 token（仅 SHA-256 hash）
- 未写入真实凭证
- SET_TIME 已成为 SET 类命令安全接入样板

### 安全默认配置

| 配置项 | 默认值 |
|--------|--------|
| enabled | false |
| allow-real-call | false |
| require-confirmation | true |
| audit-required | true |
| scheduler-forbidden | true |
| dry-run-default | true |

---

## 告警一致性审计进度

```
SEND_ALARM: FSU 主动告警变化上报
GET_ACTIVEALARM: SC 主动查询 FSU 当前活动告警快照
ActiveAlarmDiff: FSU 快照 vs 本地 alarm_record 只读差异核对
LocalActiveAlarmSnapshotService: alarm_record → LocalAlarmSnapshot
ActiveAlarmConsistencyAuditService: GetActiveAlarmService + diff 编排
```

只读边界: 不自动新增/恢复/覆盖 alarm_record, 不访问真实设备, 不启用 Scheduler。

---

## 测试规模

| 阶段 | 测试数 | Failures |
|------|--------|----------|
| BIF-P4-020 后 | **1070** | **0** |

---

## 下一步建议

- BIF-P4-021: Scheduler 定时 GET_ACTIVEALARM 差异审计
- 或等待 FSU 管理方点位表 → seed SQL → 真实点位置联调
- 或继续 2024 命令实现 (SET_SCIP, 配置系列等)

---

## 项目根目录

`/home/tom/桌面/FSU/fsu-platform-java/`

## 数据库

PostgreSQL 16 (Docker: dcim-postgres), dcim/dcim123456, dcim_platform
