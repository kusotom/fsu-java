# FSU → SCService 被动上报真实联调操作指引

> LANDING-011 | 2026-05-20 | 方案 C：操作指引输出

---

## 一、前置确认

### 1.1 测试基线

```
1199 tests, 0 failures, 0 errors, 5 skipped
BInterfaceMessageLogService: 23 tests ✅
SendAlarmService: 22 tests ✅
SendDataService: 20 tests ✅
contextLoads: PASS ✅
```

### 1.2 安全开关状态

所有 SET_* 命令开关: **全部 false**
Scheduler: **scheduler-enabled: false**
出站 real-call: **false**
入站 SCService: **就绪（无开关，被动接收）**

---

## 二、平台启动检查命令

### 2.1 启动平台

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/backend
mvn spring-boot:run
```

等待日志出现: `Started DcimPlatformApplication`

### 2.2 确认端口监听

```bash
# 检查 8080 端口是否在监听
ss -tlnp | grep 8080
# 预期输出: LISTEN ... :::8080

# 或
netstat -tlnp | grep 8080
```

### 2.3 确认平台 IP

```bash
# 获取本机 IP（选择一个 FSU 可达的地址）
ip addr show | grep "inet " | grep -v 127.0.0.1
# 或
hostname -I
```

例如输出 `192.168.100.123`，则该地址作为 SC_PLATFORM_IP。

### 2.4 确认 SCService 接收路径

```bash
# 测试 WSDL 端点（GET）
curl -s http://localhost:8080/api/b-interface/wsdl/sc-service | head -10
# 预期: 返回 WSDL XML 内容

# 测试 POST 端点（发送空请求，预期返回 SOAP Fault）
curl -s -X POST http://localhost:8080/api/b-interface/sc-service \
  -H "Content-Type: text/xml" \
  -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><test/></soap:Body></soap:Envelope>'
# 预期: 返回 SOAP Fault（命令未知）
```

### 2.5 确认 BInterfaceMessageLog 查询接口可用

```bash
# 查询入站报文日志（初始为空）
curl -s "http://localhost:8080/api/b-interface/message-logs/query?direction=INBOUND&page=0&size=5" | python3 -m json.tool
# 预期: {"code":0,"data":{"content":[],"totalElements":0,...}}
```

---

## 三、给现场配置 FSU 的 B接口 HTTP 上报地址

### 3.1 配置内容（交给现场管理人员）

```
请将 FSU 设备的监控中心 / SC 平台上报地址配置为：

HTTP URL:  http://{SC_PLATFORM_IP}:8080/api/b-interface/sc-service
方法:      POST
Content-Type: text/xml; charset=utf-8
SOAPAction: ""  (空字符串)

设备参数:
  FSUID:        51051243812345
  StationName:  1

上报命令（全部启用）:
  LOGIN        - 注册/会话建立
  HEARTBEAT    - 心跳保活
  SEND_DATA    - 实时数据上报
  SEND_ALARM   - 告警上报

说明:
  - 路径使用 /services/SCService（B接口标准，LANDING-012 新增）或 /api/b-interface/sc-service（原有兼容）
  - SC_PLATFORM_IP 由平台侧提供（见下文）
  - 配置完成后需重启 FSU 或触发重新注册
  - 本次仅验证接收，禁止下发 SET 类控制命令
```

### 3.2 SC_PLATFORM_IP 填写

将上面 `ip addr` 或 `hostname -I` 输出的实际 IP 填入，替换 `{SC_PLATFORM_IP}`。

例如平台 IP 为 `192.168.100.123`，则完整地址为：

```
http://192.168.100.123:8080/api/b-interface/sc-service
```

---

## 四、FSU 上报后如何观察

### 4.1 实时观察应用日志

```bash
tail -f /home/tom/桌面/FSU/fsu-platform-java/backend/logs/spring.log 2>/dev/null | grep -E "SCService|LOGIN|HEARTBEAT|SEND_DATA|SEND_ALARM"
```

如日志文件位置不同，直接在启动终端观察包含 `SCService` 的行。

### 4.2 查询入站报文日志

```bash
# 查看所有入站报文（最近 20 条）
curl -s "http://localhost:8080/api/b-interface/message-logs/query?direction=INBOUND&page=0&size=20" | python3 -m json.tool

# 按 FSUID 查询
curl -s "http://localhost:8080/api/b-interface/message-logs/query?fsuCode=51051243812345&page=0&size=20" | python3 -m json.tool

# 按命令类型查询
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=LOGIN&page=0&size=5" | python3 -m json.tool
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=HEARTBEAT&page=0&size=5" | python3 -m json.tool
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=SEND_DATA&page=0&size=5" | python3 -m json.tool
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=SEND_ALARM&page=0&size=5" | python3 -m json.tool
```

### 4.3 查看单条报文详情

```bash
# 替换 {id} 为查询结果中的 id 值
curl -s "http://localhost:8080/api/b-interface/message-logs/{id}" | python3 -m json.tool
```

---

## 五、业务入库检查

### 5.1 检查 alarm_record（告警）

连接 PostgreSQL 后执行：

```sql
-- 查看最近告警记录
SELECT id, fsu_id, point_code, serial_no, device_id, spid,
       alarm_code, alarm_level, alarm_status, occur_time
FROM alarm_record
ORDER BY created_at DESC
LIMIT 10;
```

检查项：
- [ ] serial_no 是否有值
- [ ] device_id 是否有值
- [ ] spid 是否有值
- [ ] alarm_status 状态（ACTIVE/RECOVERED）

### 5.2 检查 b_interface_session（会话）

```sql
-- 查看 FSU 登录会话
SELECT id, fsu_code, session_id, login_status, last_seen, created_at
FROM b_interface_session
WHERE fsu_code = '51051243812345'
ORDER BY created_at DESC
LIMIT 5;
```

### 5.3 检查 realtime_data（实时数据）

```sql
-- 查看 SEND_DATA 写入的实时数据
SELECT id, fsu_id, point_id, point_code, value_text, value_number,
       value_status, quality, collect_time, receive_time
FROM realtime_data
ORDER BY receive_time DESC
LIMIT 20;
```

---

## 六、原始报文样本导出

从 BInterfaceMessageLog 查询结果中提取 rawMessage 字段，保存到：

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/docs/landing/raw-samples/
```

如果收到对应命令的报文，将 rawMessage 内容（完整 SOAP XML）保存为：

| 文件 | 对应命令 | 说明 |
|------|----------|------|
| `passive-landing011-login.xml` | LOGIN | FSU 注册报文 |
| `passive-landing011-heartbeat.xml` | HEARTBEAT | 心跳报文 |
| `passive-landing011-send-data.xml` | SEND_DATA | 实时数据报文 |
| `passive-landing011-send-alarm.xml` | SEND_ALARM | 告警上报报文 |

如某类报文未收到，保存空文件并标注原因（如"FSU 未配置 SEND_DATA 上报"）。

导出方法（通过 API 查询后手动保存）：

```bash
# 示例：获取第一条 LOGIN 报文的 rawMessage
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=LOGIN&page=0&size=1" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['content'][0]['rawMessage'] if d['data']['content'] else 'EMPTY')" \
  > docs/landing/raw-samples/passive-landing011-login.xml
```

---

## 七、故障排查指南

### 7.1 没有任何入站报文

**检查顺序**：

```bash
# 1. 平台是否在监听 8080？
ss -tlnp | grep 8080

# 2. 本地 POST 是否可达？
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/b-interface/sc-service \
  -H "Content-Type: text/xml" -d '<test/>'
# 预期: 200 或 500（不是 404 或 Connection refused）

# 3. FSU 侧能否访问平台 IP？
# 在 FSU 侧或同网段机器执行：
ping {SC_PLATFORM_IP}
telnet {SC_PLATFORM_IP} 8080

# 4. 防火墙是否放通 8080？
sudo iptables -L -n | grep 8080
sudo ufw status | grep 8080
```

### 7.2 收到报文但解析失败

观察应用日志中的 SCService 相关 WARN/ERROR，常见原因：
- Content-Type 不是 text/xml
- SOAP 命名空间不匹配
- FSU 发送了未知 PK_Type

### 7.3 SEND_DATA 入库但无值

- monitoring_point 表为空 → 未匹配任何 SignalID
- 需管理方提供 DeviceID/SPID 映射表后导入

### 7.4 SEND_ALARM 入库但 spid 为空

- FSU 上报的 Alarm 项不包含 SPID 字段（旧版本固件可能省略）
- 不影响告警核心功能

---

## 八、联调观察记录表

| 观察项 | 状态 | 备注 |
|--------|------|------|
| 平台 8080 监听 | ⬜ | |
| SCService POST 可达 | ⬜ | |
| FSU 已配置 SC 地址 | ⬜ | |
| 收到 LOGIN | ⬜ | |
| 收到 HEARTBEAT | ⬜ | |
| 收到 SEND_DATA | ⬜ | |
| 收到 SEND_ALARM | ⬜ | |
| serial_no 有值 | ⬜ | |
| device_id 有值 | ⬜ | |
| spid 有值 | ⬜ | |
| 原始 LOGIN 报文已导出 | ⬜ | |
| 原始 HEARTBEAT 报文已导出 | ⬜ | |
| 原始 SEND_DATA 报文已导出 | ⬜ | |
| 原始 SEND_ALARM 报文已导出 | ⬜ | |

---

## 九、未映射 DeviceID / SPID 记录

如 SEND_DATA/SEND_ALARM 中出现的 DeviceID/SPID 未在 monitoring_point 表中，记录如下：

| DeviceID | SPID | 来源命令 | 说明 |
|----------|------|----------|------|
| | | | |

---

## 十、安全边界

本次联调期间：
- [ ] 未执行任何 SET_ 命令
- [ ] 未启用 Scheduler
- [ ] 未主动调用 FSUService (GET_*)
- [ ] 未修改 alarm_record 状态机
- [ ] 未生成正式 seed SQL
