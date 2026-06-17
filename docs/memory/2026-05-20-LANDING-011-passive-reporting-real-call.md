# 工程记忆：LANDING-011 FSU → SCService 被动上报真实联调

## 1. 时间
2026-05-20

## 2. 背景

LANDING-006~010 完成了 2016 码表确认、SCService 接收链路核对、报文日志实现、spid 补齐、DDL 对齐和查询增强。现场已具备配置 FSU 上报地址的条件。

## 3. 本次目标

1. 核对 SCService 实际暴露地址（基于代码）
2. 输出完整操作指引给现场配置 FSU
3. 提供查询、观察、故障排查命令
4. 不修改 Java 代码

## 4. 关键决策

### 4.1 SCService URL 确认

| 端点 | 路径 | 用途 |
|------|------|------|
| POST 接收 | `/api/b-interface/sc-service` | **FSU 上报地址** |
| WSDL GET | `/api/b-interface/wsdl/sc-service` | WSDL 定义 |
| `/services/SCService` | — | **无 POST 映射，不可用** |

**FSU 侧配置地址**: `http://{SC_PLATFORM_IP}:8080/api/b-interface/sc-service`

### 4.2 执行方式

方案 C：输出操作指引，用户在终端和现场侧执行。

### 4.3 安全边界维持

- 所有 SET 开关保持 false
- Scheduler 不启用
- 不主动调用 FSUService (GET_*)
- 不引入 DSC/RDS/UDP

## 5. 新增文件

| 文件 | 说明 |
|------|------|
| `openspec/specs/landing-011-*.md` | Spec |
| `openspec/plans/landing-011-*.md` | Plan |
| `docs/landing/PASSIVE-REPORTING-REAL-CALL-REPORT.md` | 联调操作指引（完整） |
| `docs/audit/LANDING-011-*.md` | 审计文档 |
| `docs/memory/2026-05-20-LANDING-011-*.md` | 本文件 |

## 6. 修改文件

| 文件 | 操作 |
|------|------|
| `docs/memory/README.md` | 索引更新 |
| `docs/memory/WORKING-MEMORY.md` | 当前阶段更新 |

**Java 生产代码: 0 修改**

## 7. 测试结果

```
全量: 1199 tests, 0 failures, 0 errors, 5 skipped
```

## 8. 待现场执行后补充

- [ ] 实际收到的上报命令列表
- [ ] 业务入库结果（alarm_record serialNo/deviceId/spid）
- [ ] 未映射 DeviceID/SPID 清单
- [ ] 原始报文样本（XML 文件）
- [ ] 联调观察记录表

## 9. 遗留问题

1. 现场配置和联调尚未执行（本阶段仅输出指引）
2. monitoring_point 为空 — SEND_DATA 无法匹配 SignalID
3. Codex 复审暂缓

## 10. 下一步建议

1. 现场执行联调操作指引
2. 根据实际收到的报文补充 raw-samples 和观察记录
3. 如收到 SEND_DATA，分析报文格式确定 DeviceID 处理策略
4. 管理方提供 DeviceID/SPID 映射表
