# BIF-P4-003：单台真实 FSU 只读联调计划

> 对应阶段：BIF-P4-003
> 完成日期：2026-05-15
> 状态：计划阶段，不访问真实设备，不启用真实调用

---

## 一、本阶段目标

为单台真实 FSU 的只读联调生成完整计划、配置模板、白名单/黑名单、回滚方案、日志脱敏规则和人工确认清单。**本阶段不访问真实设备，不修改业务代码，不启用任何真实调用开关。**

## 二、前置条件

| # | 条件 | 确认 | 说明 |
|---|------|------|------|
| 1 | BIF-P3-001 ~ BIF-P3-007 全部完成 | ✅ | 6 个查询类命令 + SET_THRESHOLD 已闭环 |
| 2 | BIF-P4-001 安全验收通过 | ✅ | 19 项安全审计，711 测试通过 |
| 3 | BIF-P4-002 FSU 地址发现完成 | ✅ | FsuEndpointResolver 可从数据库解析 endpoint |
| 4 | 联调 FSU 设备就绪 | ⬜ | 待确认 |
| 5 | FSU IP/端口可达 | ⬜ | 待网络确认 |
| 6 | 联调人员授权 | ⬜ | 待确认 |

## 三、联调拓扑

```
SC Platform (开发/测试环境)
  └── RealHttpFsuServiceClient (real-call-enabled=true)
       └── HTTP POST → http://<FSU_IP>:<Port>/services/FSUService
            └── FSU 设备 (只读命令，不修改)
```

## 四、配置模板

### 4.1 application-dev.yml（联调环境专用）

```yaml
# ==================== B-Interface FSU 真实联调配置 ====================
# 警告：本配置仅用于隔离的联调环境，禁止用于生产
# 联调完成后必须关闭 real-call-enabled

b-interface:
  fsu-client:
    real-call-enabled: true                  # 启用真实 HTTP 调用
    connect-timeout: 10s                     # 连接超时
    read-timeout: 30s                        # 读取超时

  # ---------- SET 类命令安全门禁（保持关闭） ----------
  set-threshold:
    enabled: false                           # 禁止 SET_THRESHOLD 真实下发
    require-confirmation: true               # 保持二次确认

  # ---------- 慢数据轮询（保持关闭） ----------
  slow-polling:
    enabled: false                           # 禁止自动轮询
    scheduler-enabled: false                 # 禁止定时调度
    allow-real-call: false                   # 禁止轮询发起真实调用

  # ---------- 离线检测（保持关闭） ----------
  offline-detection:
    enabled: false
```

### 4.2 FSU endpoint 配置

FSU endpoint 从 `fsu_device` 表动态获取，无需硬编码到配置文件中。

```sql
-- 联调前确认 fs u_device 表中有对应记录
SELECT fsu_code, ip_addr, port, status
FROM fsu_device
WHERE fsu_code = '联调FSU编码';
```

预期返回：
| fsu_code | ip_addr | port | status |
|----------|---------|------|--------|
| FSU-XXX | 联调IP | 联调端口 | ONLINE |

## 五、联调前检查清单

| # | 检查项 | 确认 | 说明 |
|---|--------|------|------|
| 1 | 联调环境与生产网络隔离 | ⬜ | 联调网络不能与生产互通 |
| 2 | 联调 FSU 不在生产监控范围 | ⬜ | 确认不影响现网设备 |
| 3 | application-dev.yml 已备份 | ⬜ | 联调后可恢复 |
| 4 | real-call-enabled=false 配置已备份 | ⬜ | 记录原始值 |
| 5 | FSU IP 已通过数据库配置而非硬编码 | ⬜ | 不得在配置文件中写 IP |
| 6 | 日志级别未设为 DEBUG 过度打印 | ⬜ | 推荐 WARN |
| 7 | 联调人员已获授权 | ⬜ | |
| 8 | 回滚方案已就绪 | ⬜ | 见回滚计划文档 |
| 9 | 监控/告警已关闭（联调期间） | ⬜ | 避免误告警 |
| 10 | 联调时间窗口已确认 | ⬜ | |

## 六、联调步骤

### 第一阶段：网络可达性确认

```
1. ping <FSU_IP>
   预期: 可达
   
2. telnet <FSU_IP> <Port>
   预期: 端口开放
   
3. curl -v http://<FSU_IP>:<Port>/services/FSUService
   预期: 返回 SOAP 响应（即使报错也说明 HTTP 可达）
```

### 第二阶段：GET_LOGININFO 查询

```
1. 使用已有 LOGIN 能力注册 FSU（不修改 LOGIN 流程）
2. 通过 CommandDispatcher 下发 GET_LOGININFO 请求
3. 验证:
   - ResultCode=0
   - LoginStatus=LOGIN
   - OnlineStatus=ONLINE
   - SessionID 非空
   - LoginTime/LastHeartbeat 格式正确
```

### 第三阶段：TIME_CHECK 时间同步校验

```
1. 下发 TIME_CHECK 请求（携带 SC 标准时间）
2. 验证:
   - ResultCode=0
   - FSUTime 非空，格式为 ISO8601
   - SC 标准时间与 FSU 时间偏差在合理范围内
```

### 第四阶段：GET_DATA 实时数据查询

```
1. 下发 GET_DATA 请求（指定已知信号 ID 列表）
2. 验证:
   - ResultCode=0
   - 返回信号数量 > 0
   - 每个信号含 SignalID/Value/Quality/Status/CollectTime
   - Value 格式符合预期
```

### 第五阶段：GET_THRESHOLD 门限查询

```
1. 下发 GET_THRESHOLD 请求（指定信号 ID）
2. 验证:
   - ResultCode=0
   - 返回门限值含 AlarmUpper/AlarmLower/AlarmUpperUrgent/AlarmLowerUrgent
   - 门限值为有效数值
```

### 第六阶段：GET_FTP FTP 配置查询

```
1. 下发 GET_FTP 请求
2. 验证:
   - ResultCode=0
   - Host/Port/Username/PassiveMode/BasePath 非空
   - Username 在日志中脱敏
   - 不记录 Password
```

## 七、联调后验证清单

| # | 检查项 | 确认 | 说明 |
|---|--------|------|------|
| 1 | real-call-enabled 已恢复 false | ⬜ | 必须恢复 |
| 2 | application-dev.yml 已恢复 | ⬜ | 可对比备份 |
| 3 | 联调 FSU 仍在线 | ⬜ | HEARTBEAT 验证 |
| 4 | 无资源泄露 | ⬜ | 应用健康检查 UP |
| 5 | 全量测试通过 | ⬜ | mvn test 无失败 |
| 6 | 联调日志已归档（脱敏后） | ⬜ | 可选 |
| 7 | 审计记录完整 | ⬜ | SET 类审计为空 |

## 八、日志脱敏要求

| 日志内容 | 要求 |
|---------|------|
| FSU IP 地址 | 允许在调试日志中记录 |
| Username | 脱敏：首字符+****（已实现） |
| Password | ❌ 禁止记录（模型中无此字段） |
| FTP 密码 | ❌ 禁止记录 |
| SessionID | 允许在 FSU 通讯日志中记录 |
| 门限值 | 允许记录（设备配置参数） |
| 操作人 | 允许记录（审计需求） |

## 九、风险清单

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| FSU endpoint URL 格式不匹配 | 中 | 联调前 curl 验证路径 |
| FSU 返回非标准错误码 | 低 | 检查日志，适配 handler |
| 连接超时/网络抖动 | 低 | 配置 connect-timeout/read-timeout |
| SET_THRESHOLD 误开启 | 中 | 联调配置中 enabled=false 不可变 |
| 日志泄露敏感数据 | 低 | 检查 Username 脱敏逻辑 |
| 联调后未关闭 real-call-enabled | 高 | 联调后验证清单强制检查 |

## 十、禁止项清单

1. ❌ 不允许在本次联调中执行 SET_THRESHOLD
2. ❌ 不允许在本次联调中执行 SET_POINT/SET_FTP/SET_FSUREBOOT
3. ❌ 不允许启用 slow-polling/scheduler
4. ❌ 不允许批量联调多个 FSU
5. ❌ 不允许在配置文件中硬编码 FSU IP 和端口
6. ❌ 不允许在配置文件中写入设备账号密码
7. ❌ 不允许在日志中输出明文 Username/Password
8. ❌ 不允许使用生产环境配置
9. ❌ 不允许联调完成后忘记关闭 real-call-enabled

## 十一、允许范围一览

| 操作 | 允许 | 条件 |
|------|------|------|
| GET_DATA | ✅ | 只读 |
| GET_THRESHOLD | ✅ | 只读 |
| TIME_CHECK | ✅ | 只读 |
| GET_LOGININFO | ✅ | 只读 |
| GET_FTP | ✅ | 只读 |
| LOGIN | ✅ | 已有流程 |
| HEARTBEAT | ✅ | 已有流程 |
| SET_THRESHOLD | ❌ | 本阶段禁止 |
| SET_POINT | ❌ | 永久禁止 |
| SET_FTP | ❌ | 永久禁止 |
| SET_FSUREBOOT | ❌ | 永久禁止 |
| 自动轮询 | ❌ | 本阶段禁止 |
