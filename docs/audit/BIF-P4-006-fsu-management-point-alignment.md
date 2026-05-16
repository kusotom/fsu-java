# BIF-P4-006：FSU 管理协调 / 点位对齐

> 阶段：纯审计/文档，不修改业务代码，不访问真实设备
> 基于：BIF-P4-005-C 真实 FSU 只读联调结果
> 完成日期：2026-05-15

---

## 一、本阶段目标

基于 BIF-P4-005-C 联调成功结果（RPC adapter 被 FSU 接受，GET_DATA/GET_THRESHOLD ResultCode=0），整理 FSUCode、SignalID、MonitoringPoint 的对应关系，形成可执行的运维协调清单。

---

## 二、BIF-P4-005-C 联调结果固化

### 2.1 联调摘要

| 项目 | 值 |
|------|-----|
| 联调日期 | 2026-05-15 |
| 目标 FSU | 艾默生 2808IM |
| IP:Port | 192.168.100.100:8080 |
| FSUService Path | /services/FSUService |
| SOAP 格式 | WSDL RPC (FsuServiceRpcAdapter) |
| 联调命令 | GET_LOGININFO, TIME_CHECK, GET_DATA, GET_THRESHOLD, GET_FTP |

### 2.2 逐命令结果

| # | 命令 | HTTP | SOAP Fault | ResultCode | 数据 | 诊断 |
|---|------|------|-----------|------------|------|------|
| 1 | GET_LOGININFO | 200 | 无 | N/A | 无数据 | FSUCode 未在 FSU 注册 |
| 2 | TIME_CHECK | 200 | 无 | N/A | 无 FSUTime | 响应结构差异（见下） |
| 3 | GET_DATA | 200 | 无 | **0** | signals=0 | TEMP-R01/HUMI-R01 不在 FSU 配置中 |
| 4 | GET_THRESHOLD | 200 | 无 | **0** | thresholds=0 | 同上 |
| 5 | GET_FTP | 200 | 无 | N/A | 无数据 | FSU 未返回 FTPConfig |

### 2.3 已验证通过

- [x] WSDL RPC 格式被艾默生 2808IM 接受
- [x] SOAPAction="" 正确
- [x] FSUService namespace 正确 (http://FSUService.chinatowercom.com)
- [x] xmlData xsi:type="SOAP-ENC:string" 正确
- [x] HTTP 200 稳定（无连接超时/无 404/无 500）
- [x] GET_DATA 和 GET_THRESHOLD 返回 ResultCode=0
- [x] 错误处理链路完整（无 NPE/无未捕获异常）

---

## 三、FSU 管理协调清单

### 3.1 平台侧已注册 FSU

| 字段 | FSU-001 (demo) | 51051243812345 (真实) |
|------|---------------|---------------------|
| FSUCode | FSU-001 | 51051243812345 |
| FSU 名称 | 朝阳户外柜FSU-01 | 动环 |
| 厂商 | 中兴 | 艾默生 |
| 型号 | eStone II | 2808IM |
| IP | 192.168.1.101 | 192.168.100.100 |
| Port | 8080 | 8080 |
| Protocol | B-2016 | B-2016 |
| Status | ONLINE | ONLINE |
| Site ID | 1 (朝阳区户外柜站点A) | 2 (真实联调站点) |
| FsuDeviceEntity 存在 | ✅ | ✅ |
| serviceUrl 可解析 | ✅ (http://192.168.1.101:8080/services/FSUService) | ✅ (http://192.168.100.100:8080/services/FSUService) |
| onlineStatus | ONLINE | ONLINE |
| lastHeartbeat | 2026-05-10 (demo) | 2026-05-15 (联调时更新) |
| 允许只读联调 | ✅ | ✅ (已授权) |
| 禁止 SET 命令 | ✅ | ✅ (已禁止) |

### 3.2 设备侧运维协调清单

| # | 协调项 | 当前状态 | 需要 FSU 管理方确认/操作 |
|---|--------|---------|---------------------|
| 1 | FSUCode 一致性 | 平台=51051243812345, FSU=? | **确认 FSU 侧 FSUCode 是否与平台一致** |
| 2 | SC 上级配置 | 未知 | 确认 FSU 已配置上级 SC 地址 |
| 3 | FSUService endpoint | /services/FSUService ✅ | 确认 FSU 侧 SOAP endpoint 路径 |
| 4 | B接口上报地址 | 未知 | 确认 FSU 向平台 SCService 上报的 URL |
| 5 | 点位模板完整性 | 仅 2 个占位点位 | **确认 FSU 实际配置的信号量列表 (SignalID/名称/类型/单位)** |
| 6 | SignalID 一致性 | TEMP-R01/HUMI-R01 为占位名 | **确认 FSU 侧实际 SignalID 名称** |
| 7 | 门限配置 | 未查询到 | 确认 FSU 是否已配置告警门限值 |
| 8 | FTP 可读性 | 未返回数据 | 确认 FSU 是否允许 SC 读取 FTP 配置 |
| 9 | 控制类命令 | 平台已禁止 | 确认 FSU 侧控制类命令权限已锁定 |

---

## 四、点位对齐清单

### 4.1 当前平台 MonitoringPoint 现状（fsu_id=2, 真实 FSU）

| # | Point ID | SignalID | Point Name | Type | DataType | Unit | Status | 轮询间隔 |
|---|----------|----------|------------|------|----------|------|--------|---------|
| 1 | 6 | TEMP-R01 | 机柜温度 | AI | NUMBER | °C | ACTIVE | 300s |
| 2 | 7 | HUMI-R01 | 机柜湿度 | AI | NUMBER | %RH | ACTIVE | 300s |

### 4.2 联调对齐状态

| SignalID | 平台 MonitoringPoint | pointCode=SignalID | status | GET_DATA 返回 | GET_THRESHOLD 返回 | 平台补录 | FSU 补录 |
|----------|---------------------|-------------------|--------|--------------|-------------------|---------|---------|
| TEMP-R01 | ✅ (id=6) | ✅ | ACTIVE | ❌ 无数据 | ❌ 无数据 | ❌ 需更新为真实 ID | ✅ **需确认 FSU 侧实际温度信号ID** |
| HUMI-R01 | ✅ (id=7) | ✅ | ACTIVE | ❌ 无数据 | ❌ 无数据 | ❌ 需更新为真实 ID | ✅ **需确认 FSU 侧实际湿度信号ID** |

### 4.3 缺失点位分析

**当前平台仅有 2 个占位点位**。艾默生 2808IM 典型配置包含但不限于：

| 类别 | 建议 SignalID 前缀 | 典型数量 | 当前平台 | 状态 |
|------|-------------------|---------|---------|------|
| 温度 | TEMP-* | 2-4 | 1 占位 | ⚠️ 不足 |
| 湿度 | HUMI-* | 1-2 | 1 占位 | ⚠️ 不足 |
| 电压 | VOLT-* | 2-6 | 0 | ❌ 缺失 |
| 电流 | CURR-* | 2-6 | 0 | ❌ 缺失 |
| 门磁 | DOOR-* | 1-4 | 0 | ❌ 缺失 |
| 水浸 | WATER-* | 1-2 | 0 | ❌ 缺失 |
| 烟雾 | SMOKE-* | 1-2 | 0 | ❌ 缺失 |
| 空调 | AC-* | 0-4 | 0 | 按需 |

> **注意**：上表为 FSU 典型配置，非 B接口协议强制要求。实际 SignalID 必须从 FSU 管理方获取，**不得自造**。

---

## 五、平台侧补录建议

### 5.1 现状

- FsuDevice: 已存在，fsu_code=51051243812345
- Site: 已存在，site_code=SITE-002 (真实联调站点)
- Cabinet: 未关联（cabinet_id=NULL）
- MonitoringPoint: 仅 2 个占位记录（TEMP-R01, HUMI-R01）
- 门限: 未配置（alarm_upper/lower/urgent 均为 NULL）

### 5.2 补录策略

**不生成 seed SQL，等待 FSU 管理方提供实际信号配置后再生成。**

原因：
1. TEMP-R01/HUMI-R01 是平台侧占位命名的 SignalID，FSU 侧实际 ID 可能完全不同
2. B接口协议禁止自造 SignalID
3. 需 FSU 管理方提供完整的点位模板后统一补录

### 5.3 补录流程建议

```
1. FSU 管理方提供点位表
   ↓
2. 平台侧按 B接口 SignalID 命名规范录入 monitoring_point
   ↓
3. 更新 SignalPollingTargetService 轮询配置
   ↓
4. 用真实 SignalID 重新联调 GET_DATA/GET_THRESHOLD
   ↓
5. 联调通过后配置 alarm_upper/lower/urgent
   ↓
6. 配置 polling_interval
```

### 5.4 需要 FSU 管理方提供的信息

| # | 信息项 | 说明 | 优先级 |
|---|--------|------|--------|
| 1 | FSU 侧实际 FSUCode | 如与 51051243812345 不一致则需对齐 | **P0** |
| 2 | 完整 SignalID 列表 | 包含 ID、名称、类型、单位、量程 | **P0** |
| 3 | 门限配置 | 每个信号的 AlarmUpper/Lower/Urgent | P1 |
| 4 | FTP 配置 | Host/Port/Username/PassiveMode/BasePath | P1 |
| 5 | SC 上报地址 | FSU 向平台 SCService 上报的 URL | P1 |
| 6 | 固件版本 | 用于协议兼容性判断 | P2 |

---

## 六、GET_DATA/GET_THRESHOLD 空响应分析

### GET_DATA

- **现象**: ResultCode=0, signals=[]
- **原因**: 请求中的 SignalID (TEMP-R01, HUMI-R01) 在 FSU 侧不存在
- **非平台问题**: 平台正确发送了 RPC SOAP 请求，FSU 正确返回了成功响应
- **解决方案**: 从 FSU 管理方获取实际 SignalID，更新 monitoring_point 和轮询配置

### GET_LOGININFO

- **现象**: xmlData 为空
- **原因**: FSUCode 51051243812345 未在 FSU 侧注册
- **影响**: TIME_CHECK/GET_DATA/GET_THRESHOLD/GET_FTP 依赖 LOGIN session
- **解决方案**: FSU 管理方在 FSU 上注册 FSUCode

### TIME_CHECK

- **现象**: 响应中无 FSUTime 字段
- **原因**: FSU 响应格式可能与协议手册有偏差
- **解决方案**: 获取实际 FSU 响应报文分析字段名差异

---

## 七、安全边界确认

| 检查项 | 状态 | 说明 |
|--------|------|------|
| SET_THRESHOLD 真实下发 | ❌ 禁止 | set-threshold.enabled=false |
| SET_POINT | ❌ 禁止 | Handler 为桩 |
| SET_FTP | ❌ 禁止 | Handler 为桩 |
| SET_FSUREBOOT | ❌ 禁止 | 无 Handler |
| Scheduler 启用 | ❌ 禁止 | scheduler-enabled=false |
| 批量联调 | ❌ 禁止 | 仅 1 台 FSU |
| 真实密码写入 | ❌ 禁止 | 无密码字段 |
| 数据库写入 | ❌ 禁止 | 本阶段不写 DB |
| 前端修改 | ❌ 禁止 | 不涉及 |

---

## 八、遗留问题

1. **FSU 侧 FSUCode 未确认**：平台使用 51051243812345，需 FSU 管理方确认
2. **SignalID 为占位值**：TEMP-R01/HUMI-R01 非 FSU 实际信号ID
3. **门限未配置**：无 alarm_upper/lower/urgent 值
4. **FTP 未读取成功**：需 FSU 管理方确认配置
5. **Cabinet 未关联**：fsu_device.cabinet_id=NULL
6. **FSU 侧点位模板未知**：等待 FSU 管理方提供
7. **GET_LOGININFO 响应字段未知**：需真实响应报文分析

---

## 九、下一步建议

1. **协调 FSU 管理方**：提供 §5.4 所列信息
2. **BIF-P4-007**（收到点位信息后）：生成 seed SQL，更新 monitoring_point
3. **BIF-P4-008**（点位补录后）：用真实 SignalID 重新联调 GET_DATA/GET_THRESHOLD
4. **BIF-P4-009**（联调完整闭环后）：评估慢数据定时轮询启用条件
