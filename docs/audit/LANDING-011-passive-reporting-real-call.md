# 审计文档：LANDING-011 FSU → SCService 被动上报真实联调

## 1. 审计日期
2026-05-20

## 2. 审计类型
真实 FSU 被动上报联调准备 + 操作指引输出

## 3. 审计目标

审校 SCService URL 核验结果和联调操作指引的正确性、完整性、安全性。

## 4. SCService URL 核验

| 端点 | 类 | 方法 | 路径 |
|------|-----|------|------|
| POST 接收 | ScServiceController | @PostMapping | `/api/b-interface/sc-service` |
| WSDL GET | ScServiceWsdlController | @GetMapping | `/api/b-interface/wsdl/sc-service` |
| 端口 | application.yml | server.port | `8080` |

**`/services/SCService`**: 无 POST 映射。仅为 WSDL 文件声明路径。

**结论**: FSU 上报地址应为 `http://{IP}:8080/api/b-interface/sc-service`。

## 5. 安全开关确认

| 开关 | application.yml | 状态 |
|------|-----------------|------|
| real-call-enabled | false | ✅ |
| scheduler-enabled | false | ✅ |
| set-threshold.enabled | false | ✅ |
| set-command-safety.enabled | false | ✅ |
| set-command-safety.scheduler-forbidden | true | ✅ |
| active-alarm-audit.scheduler-enabled | false | ✅ |
| slow-polling.enabled | false | ✅ |
| 所有 SET_* 子开关 | false | ✅ |

## 6. 操作指引完整性检查

| 检查项 | 状态 |
|--------|------|
| 平台启动命令 | ✅ |
| 端口确认命令 | ✅ |
| SCService 路径验证 | ✅ |
| FSU 配置指令 | ✅ |
| 查询 API 命令 | ✅ |
| 业务入库 SQL | ✅ |
| 原始报文导出方法 | ✅ |
| 故障排查指南 | ✅ |
| 联调观察记录表 | ✅ |
| 安全边界确认 | ✅ |

## 7. 安全边界

| 边界 | 状态 |
|------|------|
| SET 命令 | 未执行，所有开关 false |
| Scheduler | 未启用 |
| 主动 GET_* | 未调用 |
| DSC/RDS 端口 | 不考虑 |
| UDP 监听 | 不引入 |
| alarm_record 状态机 | 未修改 |
| seed SQL | 未生成 |

## 8. 测试结果

```
*BInterfaceMessageLog*:  33 tests, PASS
*SendAlarm*:             22 tests, PASS
*SendData*:              20 tests, PASS
*BInterface*:           149 tests, PASS
全量:                  1199 tests, 0/0/5, PASS
```

## 9. 结论

**通过。** SCService URL 核验正确（基于代码，非猜测），操作指引完整，安全边界全部满足。

**待现场执行后补充**：实际收到的命令列表、业务入库结果、未映射 DeviceID/SPID、原始报文样本。
