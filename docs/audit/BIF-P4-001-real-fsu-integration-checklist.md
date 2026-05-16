# BIF-P4-001：真实 FSU 联调前检查清单

> 本清单用于进入真实 FSU 联调前逐项确认。所有检查项必须标记为 ✅ 后方可开始联调。

---

## 一、联调前环境检查

| # | 检查项 | 确认 | 说明 |
|---|--------|------|------|
| 1 | 联调 FSU 设备就绪 | ⬜ | 确认有可用的测试 FSU，已上电且网络可达 |
| 2 | FSU FSUService endpoint 已知 | ⬜ | 格式: `http://<FSU_IP>:<Port>/FSUService` |
| 3 | 联调环境与生产网络隔离 | ⬜ | 联调网络不能与生产环境互通 |
| 4 | 联调 FSU 无业务影响 | ⬜ | 确认联调 FSU 不在生产监控范围内 |
| 5 | 回滚计划已制定 | ⬜ | 见第六节 |

## 二、联调前配置检查

| # | 检查项 | 确认 | 说明 |
|---|--------|------|------|
| 1 | application.yml 仅修改联调环境配置 | ⬜ | 不修改生产配置文件 |
| 2 | `b-interface.fsu-client.real-call-enabled=true` | ⬜ | 仅在联调环境配置中开启 |
| 3 | `b-interface.set-threshold.enabled=false` | ⬜ | 保持关闭，禁止 SET 类命令 |
| 4 | `b-interface.slow-polling.enabled=false` | ⬜ | 保持关闭，不自动轮询 |
| 5 | `b-interface.slow-polling.scheduler-enabled=false` | ⬜ | 保持关闭 |
| 6 | `b-interface.offline-detection.enabled=false` | ⬜ | 保持关闭 |
| 7 | FSU endpoint 未硬编码到 application.yml | ⬜ | 通过数据库或 API 动态指定 |
| 8 | 日志级别未调整为 DEBUG 过度打印 | ⬜ | 避免敏感数据 |
| 9 | 配置文件已备份 | ⬜ | 联调后可以恢复 |

## 三、允许执行的命令

| PK_Type | 说明 | 联调阶段 |
|---------|------|---------|
| GET_DATA | 查询 FSU 实时监控数据 | 第一阶段 |
| GET_THRESHOLD | 查询 FSU 告警门限 | 第一阶段 |
| TIME_CHECK | 时间同步校验 | 第一阶段 |
| GET_LOGININFO | 查询 FSU 登录/在线状态 | 第一阶段 |
| GET_FTP | 查询 FSU FTP 配置（只读） | 第一阶段 |
| SET_THRESHOLD | 设置告警门限 | ⚠️ 第二阶段，需单独授权 |

## 四、禁止执行的命令

| PK_Type | 说明 | 原因 |
|---------|------|------|
| SET_POINT | 遥控/遥调 | 极高风险，可能直接控制物理设备 |
| SET_FTP | 修改 FTP 配置 | 可能使 FSU 失联 |
| SET_FSUREBOOT | 远程重启 FSU | 极高风险，需完整审批流程 |
| SET_DATA | 设置监控参数 | 未实现，且需安全评估 |

## 五、单台 FSU 联调步骤

### 第一阶段：GET 类命令联调（验证通讯）

```
1. 确认 FSU 已上电 + 网络可达
   → ping <FSU_IP>

2. 确认 FSU FSUService 端口可达
   → telnet <FSU_IP> <Port>

3. 使用 SC 平台 LOGIN 命令注册 FSU
   → 预期 ResultCode=0

4. 执行 GET_LOGININFO 查询登录状态
   → 预期返回 LoginStatus=LOGIN, OnlineStatus=ONLINE

5. 执行 TIME_CHECK 时间同步校验
   → 预期返回 ResultCode=0, FSUTime 为 FSU 当前时间

6. 执行 GET_DATA 查询实时数据
   → 预期返回 ResultCode=0, 包含 FSU 当前监测信号

7. 执行 GET_THRESHOLD 查询门限
   → 预期返回 ResultCode=0, 包含当前告警阈值

8. 执行 GET_FTP 查询 FTP 配置
   → 预期返回 ResultCode=0, 包含 FTP 配置参数

9. 验证日志脱敏
   → 检查日志无明文密码
```

### 第二阶段：SET_THRESHOLD 联调（需授权）

```
1. 确认 enabled=true，require-confirmation=true
2. 获取二次确认授权
3. 准备已知测试门限值（记录原始值以便回滚）
4. 执行 SET_THRESHOLD（confirmed=true）
5. 执行 GET_THRESHOLD 验证门限已更新
6. 若验证失败 → 执行回滚
7. 验证操作审计记录
```

## 六、日志脱敏要求

| 日志内容 | 要求 |
|---------|------|
| FSU IP 地址 | 允许记录（网络地址） |
| Username | 脱敏：首字符+****+末字符 |
| Password | ❌ 禁止记录 |
| FTP 密码 | ❌ 禁止记录（当前模型无密码字段） |
| SessionId | 允许在 FSU 通讯日志中记录 |
| 门限值 | 允许记录（设备配置参数） |
| 操作人 | 允许记录（审计需求） |

## 七、回滚方式

| 场景 | 回滚方式 |
|------|---------|
| 真实调用导致 FSU 异常 | 1. 立即关闭 real-call-enabled=false<br>2. 重启应用<br>3. 联系 FSU 厂商恢复设备 |
| SET_THRESHOLD 设置错误 | 1. 关闭 enabled=false<br>2. 使用 GET_THRESHOLD 确认当前值<br>3. 按需回滚到原始值 |
| 配置文件错误 | 1. 恢复备份配置文件<br>2. 重启应用 |
| 联调 FSU 断连 | 1. 确认网络可达<br>2. 如超时，降低 connect-timeout/read-timeout |

## 八、联调后验证命令

| 检查项 | 命令 | 预期 |
|--------|------|------|
| 联调 FSU 仍在线 | HEARTBEAT | ResultCode=0 |
| 联调 FSU 数据正常 | SEND_DATA | 正常上报 |
| 无资源泄露 | 应用健康检查 | UP |
| 配置已恢复 | real-call-enabled | false |
| 全量测试通过 | mvn test | 全部通过 |

## 九、异常处理流程

```
联调异常
  ├── FSU 无响应（超时/连接失败）
  │   ├── 检查网络/防火墙
  │   ├── 检查 FSU 是否在线
  │   └── 超时重试（最多 3 次）
  │
  ├── FSU 返回错误 ResultCode
  │   ├── 检查请求参数是否正确
  │   ├── 检查 FSU 协议版本
  │   └── 记录错误并上报
  │
  ├── 应用异常（NPE / ClassCast / …）
  │   ├── 关闭 real-call-enabled
  │   ├── 收集异常日志
  │   └── 修复后重新联调
  │
  └── FSU 设备异常（物理设备问题）
      ├── 立即停止所有操作
      ├── 通知 FSU 现场人员
      └── 等待设备恢复
```
