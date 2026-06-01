# LANDING-011 Plan：FSU → SCService 被动上报真实联调

> 计划版本：1.0
> 创建日期：2026-05-20
> 对应 Spec：`openspec/specs/landing-011-passive-reporting-real-call.md`

---

## 1. 读取资料

| # | 文件 | 用途 |
|---|------|------|
| 1 | `ScServiceController.java` | 核对 POST 入口路径 |
| 2 | `ScServiceWsdlController.java` | 核对 WSDL 端点路径 |
| 3 | `application.yml` | 确认端口、安全开关 |
| 4 | `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 集成方案和 FSU 配置参考 |
| 5 | `docs/landing/FSU-SC-CONFIGURATION-GUIDE.md` | FSU 配置指南 |
| 6 | `docs/audit/LANDING-008~010` | 前期审计 |

## 2. 核对 SCService URL

### 2.1 代码核验

| 端点 | 类 | 方法 | 完整路径 |
|------|-----|------|----------|
| POST 接收 | `ScServiceController` | `@PostMapping("/sc-service")` | `POST /api/b-interface/sc-service` |
| WSDL 定义 | `ScServiceWsdlController` | `@GetMapping("/sc-service")` | `GET /api/b-interface/wsdl/sc-service` |

### 2.2 结论

- **实际 POST 接收地址**: `http://{IP}:8080/api/b-interface/sc-service`
- **WSDL 路径**: `http://{IP}:8080/api/b-interface/wsdl/sc-service` (GET)
- **`/services/SCService`**: 仅为 WSDL 文件声明，**无 Java Controller 映射 POST**，不可用于接收
- **端口**: 8080 (application.yml server.port)

### 2.3 FSU 侧应配置的地址

```
http://{SC_PLATFORM_IP}:8080/api/b-interface/sc-service
```

FSU 配置参数：
- FSUID: 51051243812345
- StationName: 1
- 上报命令: LOGIN / HEARTBEAT / SEND_DATA / SEND_ALARM
- Content-Type: text/xml; charset=utf-8
- SOAPAction: "" (空字符串)

## 3. 现场配置前检查

- [x] 平台端口: 8080 (application.yml)
- [x] SCService POST 路径: /api/b-interface/sc-service
- [x] 安全开关: 所有 SET_* enabled=false, scheduler-enabled=false
- [ ] 平台后端已启动
- [ ] 平台 IP 已确认（ifconfig / ip addr）
- [ ] FSU 能访问平台 IP:8080（从 FSU 侧 telnet 测试）
- [ ] 防火墙已放通 8080 端口
- [ ] BInterfaceMessageLog 查询接口可用

## 4. 联调执行步骤

### Step 1: 确认平台就绪
```bash
curl http://localhost:8080/api/b-interface/wsdl/sc-service | head -5
curl -X POST http://localhost:8080/api/b-interface/sc-service -d '<test/>' -H 'Content-Type: text/xml'
```

### Step 2: 通知现场配置 FSU
输出精确的 FSU 配置指令（见报告第4节）

### Step 3: 等待 FSU 上报
- 观察应用日志: `tail -f logs/dcim-platform.log | grep "SCService"`
- 观察报文日志表: 周期性查询 API

### Step 4: 查询入站报文
```bash
curl "http://localhost:8080/api/b-interface/message-logs/query?direction=INBOUND&page=0&size=20"
curl "http://localhost:8080/api/b-interface/message-logs/query?fsuCode=51051243812345&page=0&size=20"
curl "http://localhost:8080/api/b-interface/message-logs/query?command=LOGIN&page=0&size=5"
curl "http://localhost:8080/api/b-interface/message-logs/query?command=SEND_ALARM&page=0&size=5"
```

### Step 5: 检查业务入库
- alarm_record 表: 检查 serialNo/deviceId/spid 有值
- b_interface_session 表: 检查 LOGIN 会话
- realtime_data 表: 检查 SEND_DATA 写入

### Step 6: 导出原始报文
从 BInterfaceMessageLog.rawMessage 导出到:
- `docs/landing/raw-samples/passive-landing011-login.xml`
- `docs/landing/raw-samples/passive-landing011-heartbeat.xml`
- `docs/landing/raw-samples/passive-landing011-send-data.xml`
- `docs/landing/raw-samples/passive-landing011-send-alarm.xml`

## 5. 验证命令

```bash
# 不修改 Java，只验证现有功能
mvn test -Dtest='*BInterfaceMessageLog*'
mvn test -Dtest='*SendAlarm*'
mvn test -Dtest='*SendData*'
mvn test -Dtest='*BInterface*'
```

## 6. 输出文档

| # | 文件 | 说明 |
|---|------|------|
| 1 | `openspec/specs/landing-011-*.md` | Spec |
| 2 | `openspec/plans/landing-011-*.md` | Plan（本文件） |
| 3 | `docs/audit/LANDING-011-*.md` | 审计文档 |
| 4 | `docs/memory/2026-05-20-LANDING-011-*.md` | 工程记忆 |
| 5 | `docs/landing/PASSIVE-REPORTING-REAL-CALL-REPORT.md` | 真实联调报告 |
| 6 | `docs/memory/README.md` | 索引更新 |
| 7 | `docs/memory/WORKING-MEMORY.md` | 工作记忆更新 |

## 7. 完成条件

| # | 条件 |
|---|------|
| 1 | SCService 地址已核对 |
| 2 | FSU 配置指令已输出 |
| 3 | 至少收到 1 条 FSU 上报（或明确 FSU 未配置/未上报的原因） |
| 4 | 原始报文已导出（或明确缺失原因） |
| 5 | 业务入库结果已检查 |
| 6 | 未执行 SET / Scheduler / 主动 GET_* |
| 7 | 测试无回归 |
| 8 | memory + audit + real-call-report 已写入 |
