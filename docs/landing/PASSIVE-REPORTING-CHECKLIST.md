# FSU 被动上报联调检查清单

> LANDING-008 | 2026-05-20

## 1. FSU 侧配置检查

- [ ] FSUID 已配置为 `51051243812345`
- [ ] StationName 已配置为 `1`
- [ ] SC 平台地址已配置 (IP + Port + Path)
- [ ] LOGIN 上报已启用
- [ ] HEARTBEAT 上报已启用
- [ ] SEND_DATA 上报已启用
- [ ] SEND_ALARM 上报已启用
- [ ] 上报周期已设置 (建议 30s-300s)
- [ ] SOAPAction 已设为空字符串 `""`
- [ ] Content-Type 已设为 `text/xml; charset=utf-8`

## 2. 平台侧网络检查

- [ ] SC 平台 IP 已确认 (非 127.0.0.1)
- [ ] SC 平台 HTTP 端口已确认 (默认 8080)
- [ ] 防火墙入站规则已放通 (FSU IP → SC Port)
- [ ] FSU 网络可达 (从 FSU 侧 ping SC IP 通)
- [ ] FSU 可达 SC HTTP 端口 (telnet 通)
- [ ] 平台服务已启动并监听正确端口

## 3. SCService Endpoint 检查

- [ ] `POST /api/b-interface/sc-service` 可接受请求
- [ ] WSDL `/api/b-interface/wsdl/sc-service` 可获取
- [ ] 接收 SOAP Content-Type: `text/xml` 或 `application/xml`
- [ ] 接收 SOAPAction: `""` (空字符串)
- [ ] 解析 RPC invoke 格式 (ns1:invoke + xmlData)
- [ ] 解析 document-style 格式 (Body/Request)
- [ ] 返回正确 SOAP Envelope 格式的 ACK
- [ ] URL 路径与 FSU 配置一致 (注意 `/api/b-interface/sc-service` vs `/services/SCService` 差异)

## 4. LOGIN 接收检查

- [ ] FSU 发送 LOGIN → 平台接收
- [ ] 解析 FSUCode 正确
- [ ] 查找 fsu_device 表 (需存在匹配记录)
- [ ] 生成 SessionID 并返回
- [ ] 创建/更新 b_interface_session 表
- [ ] 更新 b_interface_fsu_status (loginStatus=LOGIN, onlineStatus=ONLINE)
- [ ] 返回 ACK: ResultCode=0, SessionID, ExpireSeconds, ServerTime
- [ ] FSU 接受 ACK 并保持登录态

## 5. HEARTBEAT 接收检查

- [ ] FSU 发送 HEARTBEAT → 平台接收
- [ ] 校验登录态 (isLoggedIn) 通过
- [ ] 更新 lastHeartbeat 时间戳
- [ ] 返回 ACK: ResultCode=0, ServerTime
- [ ] 心跳超时后 offlineDetection 可触发 (当前默认 disabled)

## 6. SEND_DATA 接收检查

- [ ] FSU 发送 SEND_DATA → 平台接收
- [ ] 校验登录态通过
- [ ] 提取 FSUCode, CollectTime 正确
- [ ] 遍历 xmlData 中的 Signal items
- [ ] 提取 SignalID, Value, Quality, Status
- [ ] 匹配 monitoring_point 表 (按 fsuId + signalId)
- [ ] 未匹配 SignalID 记录在 rejectedCount 中
- [ ] Upsert realtime_data 表
- [ ] 返回 ACK: ResultCode=0, Count=N
- [ ] 验证数据值正确 (Value, Quality, CollectTime)

## 7. SEND_ALARM 接收检查

- [ ] FSU 发送 SEND_ALARM → 平台接收
- [ ] 校验登录态通过
- [ ] 提取 FSUCode, AlarmTime 正确
- [ ] 遍历 xmlData 中的 Alarm items
- [ ] 提取: SignalID, AlarmCode, AlarmLevel, AlarmName, AlarmValue, AlarmDesc, AlarmType
- [ ] **B接口2016 字段**: 提取 SerialNo, DeviceID (已实现), SPID (已提取但未入库)
- [ ] alarmType=0 (生成): 创建 ACTIVE alarm_record
- [ ] alarmType=1 (恢复): 查找匹配告警 → 更新为 RECOVERED
- [ ] 告警字段完整性: serialNo, deviceId, alarmCode, alarmLevel, alarmStatus, alarmValue, alarmDesc, occurTime
- [ ] 返回 ACK: ResultCode=0, AlarmID
- [ ] 验证 alarm_record 写入正确

## 8. 原始报文日志检查

- [ ] FSU 上报原始 SOAP 报文可获取 (在 ScServiceController 入口)
- [ ] 报文保存方向标记为 `FSU→SC`
- [ ] 报文包含完整 SOAP Envelope
- [ ] 报文不丢失 XML 声明和命名空间
- [x] **(已实现)**: BInterfaceMessageLogService 保存到 b_interface_message_log 表
- [x] **(已实现)**: DELETE /cleanup 手动清理接口 (LANDING-010)
- [x] **(已实现)**: GET /query 分页多条件查询 (LANDING-010)
- [ ] 高并发下日志写入性能验证

## 9. DeviceID / SPID 映射检查

- [ ] DeviceID 列表已知 (当前 4 个: 51051241820004, 51051241830004, 51051241840004, 51051240700002)
- [ ] 每个 DeviceID 下的 SPID/SignalID 列表已知
- [ ] monitoring_point 表已导入映射记录
- [ ] SEND_DATA 的 SignalID 能匹配到 monitoring_point
- [ ] 未匹配的 DeviceID/SPID 有记录可追踪
- [x] **SPID 入库**: alarm_record.spid 列已补充 (Entity 已新增, 需后续同步 schema DDL)

## 10. alarm_record 字段检查

- [ ] serialNo 已写入 (B接口2016 兼容)
- [ ] deviceId 已写入
- [ ] alarmCode 已写入
- [ ] alarmLevel 已写入 (ACTIVE/RECOVERED)
- [ ] alarmStatus 状态机正确 (ACTIVE → RECOVERED)
- [ ] occurTime 已写入
- [ ] clearTime (恢复时) 已写入
- [ ] fsuId 已关联
- [x] **spid 已写入** (LANDING-008 实施阶段: Entity + SendAlarmService 已补齐, schema DDL 待同步)

## 11. 安全边界检查

- [ ] 仅执行只读/接收操作
- [ ] 未执行任何 SET_ 命令
- [ ] 未启用 Scheduler 自动访问真实 FSU
- [ ] 未自动修改 alarm_record 状态机 (仅 FSU 上报驱动的告警生成/恢复)
- [ ] 未生成正式 seed SQL
- [ ] 默认 mvn test 不访问真实 FSU
- [ ] 联调前备份数据库

## 12. 回滚和故障排查

- [ ] 数据库已备份
- [ ] 可快速停止 FSU 上报 (FSU 侧禁用)
- [ ] 可回滚 alarm_record 变更
- [ ] 可回滚 realtime_data 变更
- [ ] 应用日志级别可调整为 DEBUG 查看报文详情
- [ ] 抓包工具可用 (tcpdump/Wireshark) 排查网络问题
- [ ] FSU 管理口可登录查看 FSU 侧日志
