# BIF-P4-003：真实调用回滚方案

> 对应阶段：BIF-P4-003
> 完成日期：2026-05-15
> 适用范围：单台 FSU 只读联调

---

## 一、回滚总原则

1. **安全优先**：任何异常情况，第一时间关闭 real-call-enabled
2. **最小影响**：仅操作联调 FSU，不波及生产
3. **可恢复**：所有操作有明确回滚步骤
4. **记录完整**：回滚过程需记录时间、操作、结果

## 二、回滚决策树

```
联调异常
  ├── FSU 网络不可达
  │   └── 检查网络 → 重试 → 失败 → 关闭 real-call-enabled → 结束联调
  │
  ├── FSU 响应异常 ResultCode
  │   └── 记录错误 → 检查请求参数 → 重试 → 失败 → 关闭 real-call-enabled → 结束联调
  │
  ├── 应用异常（NPE/ClassCast/超时）
  │   └── 关闭 real-call-enabled → 收集日志 → 修复后重新联调
  │
  ├── 发现 FSU 行为异常
  │   └── 立即停止所有操作 → 关闭 real-call-enabled → 通知现场人员
  │
  └── 联调完成
      └── 关闭 real-call-enabled → 恢复配置 → 验证 → 归档
```

## 三、回滚场景及步骤

### 场景 1：FSU 网络不可达

**现象**：ping 不通 / telnet 失败 / HTTP 连接超时

**回滚步骤**：
```
1. 确认应用端 real-call-enabled=false（如果已开启）
   → 修改配置: b-interface.fsu-client.real-call-enabled=false
   → 重启应用

2. 检查网络链路
   → ping FSU_IP
   → traceroute FSU_IP
   → 检查防火墙规则

3. 如为临时网络故障
   → 等待网络恢复后重试

4. 如为永久故障
   → 结束联调，通知网络管理员
```

### 场景 2：FSU 返回异常 ResultCode

**现象**：FSU 可达但 SOAP 响应中包含非 0 ResultCode

**回滚步骤**：
```
1. 记录异常 ResultCode 和响应体
2. 检查请求参数是否正确（FSUCode / SignalID / StandardTime）
3. 重试（最多 3 次）
4. 如持续失败:
   → 关闭 real-call-enabled=false
   → 检查协议版本兼容性
   → 联系 FSU 厂商

已知可能的 ResultCode:
   - 1: 通用错误
   - 1001: FSU 未注册
   - 1002: FSU 离线
   - 2001: 参数错误
   - 2003: 无数据
```

### 场景 3：应用异常

**现象**：NPE / ClassCastException / 连接超时 / 请求构造错误

**回滚步骤**：
```
1. 立即关闭 real-call-enabled=false
2. 收集异常堆栈和日志
3. 分析根因:
   - NPE → 检查请求参数是否为 null
   - ClassCast → 检查响应解析
   - 超时 → 检查超时配置
4. 修复后重新联调
```

### 场景 4：配置错误

**现象**：联调中发��配置项错误

**回滚步骤**：
```
1. 恢复备份的配置文件
   → cp application-dev.yml.bak application-dev.yml

2. 关闭所有联调开关
   → real-call-enabled=false
   → slow-polling.enabled=false
   → scheduler-enabled=false
   → set-threshold.enabled=false

3. 重启应用

4. 确认配置已恢复
   → 检查日志: "StubFsuServiceClient 已初始化"
   → 检查日志: "RealHttpFsuServiceClient" 不应出现
```

### 场景 5：SET 类命令误执行（防御性预案）

**现象**：有人误操作或配置错误导致 SET 命令执行

**回滚步骤**：
```
1. 注意: 当前 SET_POINT/SET_FTP 为 notImplemented 桩，SET_FSUREBOOT 无 Handler
   → 即使误调用也会返回错误，不会真实执行

2. SET_THRESHOLD 有安全门禁
   → enabled=false 默认关闭
   → require-confirmation=true 需二次确认
   → 审计记录可追踪

3. 如发现门禁被绕过
   → 立即关闭 enabled=false
   → 检查审计记录
   → 通知安全团队
```

### 场景 6：联调后忘记关闭 real-call-enabled

**现象**：联调完成但 real-call-enabled 仍为 true

**回滚步骤**：
```
1. 检查当前配置
   → grep "real-call-enabled" application-dev.yml

2. 如仍为 true
   → 修改为 false
   → 重启应用

3. 确认已关闭
   → 检查日志: "StubFsuServiceClient 已初始化"
   → 执行 GET_DATA: 应走 Stub

4. 如已上线到生产
   → 立即回滚配置
   → 检查是否有非预期调用
```

## 四、回滚后验证

| # | 验证项 | 命令 | 预期 |
|---|--------|------|------|
| 1 | real-call-enabled=false | 查看配置 | false |
| 2 | 应用使用 Stub | 日志检查 | "StubFsuServiceClient 已初始化" |
| 3 | 应用健康 | curl /actuator/health | UP |
| 4 | 测试通过 | mvn test | 全部通过 |
| 5 | 无异常日志 | 日志检查 | 无 ERROR |
| 6 | FSU 仍在线（如可达） | HEARTBEAT | ResultCode=0 |

## 五、回滚时间目标

| 场景 | 目标恢复时间 | 说明 |
|------|-------------|------|
| 配置错误 | 5 分钟 | 备份恢复 + 重启 |
| 网络故障 | 30 分钟 | 含故障定位 |
| 应用异常 | 2 小时 | 含根因分析和修复 |
| SET 类误执行 | 立即 | 门禁自动拦截，无需恢复 |

## 六、通讯录

| 角色 | 联系人 | 说明 |
|------|--------|------|
| 联调负责人 | （待填写） | 决策回滚 |
| FSU 现场人员 | （待填写） | 处理 FSU 侧故障 |
| 网络管理员 | （待填写） | 处理网络故障 |
| 安全团队 | （待填写） | SET 类误执行时需通知 |
