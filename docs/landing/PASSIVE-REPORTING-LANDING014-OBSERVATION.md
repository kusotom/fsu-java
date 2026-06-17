# LANDING-014 真实 FSU 连续上报观察记录

> 日期: 2026-05-21 | 方案 C: 现场执行

---

## 一、当前 LOGIN 闭环基线

| 项目 | 状态 |
|------|------|
| FSU POST /services/SCService | ✅ |
| 平台返回 HTTP 200 + SOAP ACK | ✅ |
| ResultCode=0 + SessionID | ✅ |
| BInterfaceMessageLog 入库 (id=399/400) | ✅ |
| command=LOGIN, fsuCode=51051243812345 | ✅ |
| Content-Type: text/xml | ✅ |

---

## 二、现场查询命令（按序执行）

### 2.1 查全部入站报文（最近 20 条）

```bash
curl -s "http://localhost:8080/api/b-interface/message-logs/query?direction=INBOUND&page=0&size=20" | python3 -m json.tool
```

**记录**: totalElements = ____, 包含的命令类型: ________

### 2.2 按命令类型分别查询

```bash
# LOGIN
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=LOGIN&page=0&size=5" | python3 -m json.tool

# HEARTBEAT
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=HEARTBEAT&page=0&size=10" | python3 -m json.tool

# SEND_DATA
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=SEND_DATA&page=0&size=10" | python3 -m json.tool

# SEND_ALARM
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=SEND_ALARM&page=0&size=10" | python3 -m json.tool
```

**记录**:

| 命令 | totalElements | 状态 |
|------|--------------|------|
| LOGIN | ____ | |
| HEARTBEAT | ____ | |
| SEND_DATA | ____ | |
| SEND_ALARM | ____ | |

### 2.3 查 FSU 在线状态

```sql
SELECT fsu_code, login_status, online_status, session_id,
       last_login_time, last_heartbeat, updated_at
FROM b_interface_fsu_status
WHERE fsu_code = '51051243812345';
```

**记录**: login_status=____, online_status=____, last_heartbeat=____

### 2.4 查告警入库

```sql
SELECT id, fsu_id, serial_no, device_id, spid,
       point_code, alarm_code, alarm_status, alarm_level,
       alarm_desc, occur_time, created_at
FROM alarm_record
ORDER BY created_at DESC
LIMIT 20;
```

**检查项**:
- [ ] serial_no 是否有值
- [ ] device_id 是否有值
- [ ] spid 是否有值

### 2.5 查实时数据

```sql
SELECT id, fsu_id, point_id, point_code, value_text, value_number,
       value_status, quality, collect_time, receive_time
FROM realtime_data
ORDER BY receive_time DESC
LIMIT 20;
```

---

## 三、tcpdump 观察

```bash
sudo tcpdump -i any host 192.168.100.100 and port 8080 -A -c 50
```

观察周期: 5-10 分钟。记录收到的 HTTP 请求类型。

---

## 四、raw-samples 导出

如有报文，从 API 查询 rawMessage 并保存：

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/docs/landing/raw-samples/

# 导出 LOGIN 报文
curl -s "http://localhost:8080/api/b-interface/message-logs/1" | python3 -c "
import sys,json; d=json.load(sys.stdin)
print(d['data']['rawMessage'] if d['data'] else 'EMPTY')
" > passive-landing014-login.xml

# 导出 HEARTBEAT（替换 {id} 为实际 id）
# curl -s "http://localhost:8080/api/b-interface/message-logs/{id}" | ... > passive-landing014-heartbeat.xml

# 导出 SEND_DATA
# curl -s "http://localhost:8080/api/b-interface/message-logs/{id}" | ... > passive-landing014-send-data.xml

# 导出 SEND_ALARM
# curl -s "http://localhost:8080/api/b-interface/message-logs/{id}" | ... > passive-landing014-send-alarm.xml
```

---

## 五、观察记录表

| 观察项 | 结果 | 备注 |
|--------|------|------|
| HEARTBEAT 收到 | ⬜ 是 / ⬜ 否 | |
| SEND_DATA 收到 | ⬜ 是 / ⬜ 否 | |
| SEND_ALARM 收到 | ⬜ 是 / ⬜ 否 | |
| HEARTBEAT last_heartbeat 更新 | ⬜ 是 / ⬜ 否 | |
| alarm_record serial_no 有值 | ⬜ 是 / ⬜ 否 | |
| alarm_record device_id 有值 | ⬜ 是 / ⬜ 否 | |
| alarm_record spid 有值 | ⬜ 是 / ⬜ 否 | |
| realtime_data 写入 | ⬜ 是 / ⬜ 否 | |
| LOGIN raw-sample 已导出 | ⬜ 是 / ⬜ 否 | |
| HEARTBEAT raw-sample 已导出 | ⬜ 是 / ⬜ 否 | |
| SEND_DATA raw-sample 已导出 | ⬜ 是 / ⬜ 否 | |
| SEND_ALARM raw-sample 已导出 | ⬜ 是 / ⬜ 否 | |

---

## 六、未映射 DeviceID/SPID 清单

| DeviceID | SPID | 来源命令 | 备注 |
|----------|------|----------|------|
| | | | |

---

## 七、安全边界确认

- [ ] 未执行任何 SET_ 命令
- [ ] 未启用 Scheduler
- [ ] 未主动调用 FSUService (GET_*)
- [ ] 未修改 alarm_record 状态机
- [ ] 未生成正式 seed SQL
