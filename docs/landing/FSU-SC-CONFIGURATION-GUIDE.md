# FSU → SC 平台配置指南

> 用于现场/管理方配置 FSU 上报目标 | LANDING-008 | 2026-05-20

## 1. FSU 信息

| 配置项 | 值 |
|--------|-----|
| FSUID / FsuCode | `51051243812345` |
| StationName | `1` |
| FSU IP (管理口) | `192.168.100.100` |
| FSU Port | `8080` |
| FSU 接口路径 | `/services/FSUService` |
| FSU 协议版本 | B接口2016 |
| SCIP (FSU 当前记录) | `192.168.100.123` |

## 2. SC 平台地址 (需现场确认)

FSU 需要配置以下 SC 平台地址用于上报：

```
http://{SC_PLATFORM_IP}:{SC_PLATFORM_PORT}/api/b-interface/sc-service
```

### 需要现场确认的项目

| # | 确认项 | 说明 |
|---|--------|------|
| 1 | **SC 平台 IP** | FSU 可达的平台 IP 地址 (如 `192.168.x.x`) |
| 2 | **SC 平台端口** | 平台 HTTP 服务端口 (默认 `8080`) |
| 3 | **FSU 能否访问该 IP:Port** | 从 FSU 网络环境 ping/telnet 验证 |
| 4 | **防火墙规则** | 确保 FSU→SC 方向 TCP 端口放通 |
| 5 | **FSU 上报周期** | HEARTBEAT / SEND_DATA / SEND_ALARM 的上报间隔 |
| 6 | **FSU 上报命令类型** | 是否上报 LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM |

### 地址格式说明

- WSDL 声明的 SCService 地址: `http://127.0.0.1:8080/services/SCService`
- **实际 Spring Controller 映射路径**: `/api/b-interface/sc-service`
- 完整 URL 示例: `http://192.168.1.100:8080/api/b-interface/sc-service`
- 如果 FSU 不支持自定义路径，可能需要在平台侧配置 URL 重写或反向代理

## 3. FSU 侧需要配置的项目

| # | 配置项 | 参考值 | 说明 |
|---|--------|--------|------|
| 1 | SC 服务地址 | `http://{IP}:{PORT}/api/b-interface/sc-service` | SOAP 上报目标 URL |
| 2 | FSUID | `51051243812345` | FSU 设备唯一标识 |
| 3 | 登录使能 | 开启 | 上报数据前需先 LOGIN |
| 4 | 心跳使能 | 开启 | 周期性 HEARTBEAT |
| 5 | 数据上报使能 | 开启 | SEND_DATA 周期性上报 |
| 6 | 告警上报使能 | 开启 | SEND_ALARM 实时告警推送 |
| 7 | 上报周期 | 待确认 | 建议 30s-300s |
| 8 | SC 平台协议 | SOAP 1.1 / WSDL RPC/encoded | 与 FSUService 出站格式相同 |
| 9 | SOAPAction | `""` (空字符串) | WSDL 规范 |
| 10 | Content-Type | `text/xml; charset=utf-8` | — |

## 4. 网络验证命令 (FSU 侧执行)

```bash
# 测试 SC 平台可达性
ping {SC_PLATFORM_IP}

# 测试 HTTP 端口
telnet {SC_PLATFORM_IP} {SC_PLATFORM_PORT}

# 测试 SCService endpoint
curl -X POST http://{SC_PLATFORM_IP}:{SC_PLATFORM_PORT}/api/b-interface/sc-service \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: " \
  -d '<test/>'
```

## 5. 平台侧需要准备

| # | 准备项 | 状态 |
|---|--------|------|
| 1 | SCService Controller 已就绪 (`POST /api/b-interface/sc-service`) | ✅ |
| 2 | LOGIN 处理链路就绪 (LoginCommandHandler + LoginService) | ✅ |
| 3 | HEARTBEAT 处理链路就绪 (HeartbeatCommandHandler) | ✅ |
| 4 | SEND_DATA 处理链路就绪 (SendDataCommandHandler + SendDataService) | ✅ |
| 5 | SEND_ALARM 处理链路就绪 (SendAlarmCommandHandler + SendAlarmService) | ✅ |
| 6 | monitoring_point 点位表导入 | ⏳ 待管理方提供 |
| 7 | 原始报文日志 (BInterfaceMessageLogService) | ⏳ TODO |
| 8 | SPID 入库支持 (alarm_record.spid) | ⏳ 待补充 |
| 9 | 防火墙入站规则放通 | ⏳ 待运维确认 |

## 6. 注意事项

1. **平台当前 IP/Port 不确定**: 请用实际部署环境的 IP 和端口替换占位符
2. **不要使用 127.0.0.1**: FSU 是独立设备，不能访问 localhost
3. **SCService 路径**: 确保 FSU 配置的 URL 路径与平台实际路径一致
4. **FSU 固件限制**: 部分 FSU 固件可能不允许自定义 URL 路径，需确认
5. **首次联调建议**: 先在 FSU 侧配置上报目标，观察平台日志确认报文到达
