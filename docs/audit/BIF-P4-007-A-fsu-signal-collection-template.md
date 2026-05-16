# BIF-P4-007-A：FSU 点位资料采集模板 / 运维协调包

> 阶段：纯文档，不修改代码，不访问设备
> 基于：BIF-P4-006 FSU 管理协调 / 点位对齐结论
> 完成日期：2026-05-15
> 用途：交付给 FSU 管理方，收集真实 SignalID 和设备配置信息

---

## 一、文档用途

本文档及其附件模板用于向 FSU 管理方（艾默生 2808IM 设备运维团队）请求：

1. FSU 设备基本信息确认
2. 完整监控点位（SignalID）清单
3. 告警门限配置
4. 通信参数确认

---

## 二、联调背景（供 FSU 管理方参考）

### 2.1 已完成工作

| 项目 | 状态 |
|------|------|
| 平台 SC ↔ FSU SOAP 通信 | ✅ RPC 格式已通过 FSU 验证 |
| FSUService endpoint | ✅ http://192.168.100.100:8080/services/FSUService |
| GET_DATA 协议 | ✅ ResultCode=0（FSU 正常响应） |
| GET_THRESHOLD 协议 | ✅ ResultCode=0（FSU 正常响应） |
| SOAP Fault | ✅ 已消除 |

### 2.2 当前阻塞

- 平台使用的 SignalID（TEMP-R01, HUMI-R01）为**临时占位值**，并非 FSU 实际配置的信号ID
- GET_DATA 返回 ResultCode=0 但信号数量=0（FSU 不认识这些 SignalID）
- 需要 FSU 管理方提供**真实的信号量配置清单**

---

## 三、FSU 管理方资料请求清单

### 3.1 设备基本信息

| # | 字段 | 平台当前值 | 需 FSU 管理方确认 |
|---|------|----------|-----------------|
| 1 | FSUCode | 51051243812345 | **FSU 侧实际 FSUCode 是什么？** |
| 2 | 设备型号 | 2808IM | **是否准确？** |
| 3 | 厂商 | 艾默生 | **是否准确？** |
| 4 | 固件版本 | 1.0.0 (平台占位) | **实际固件版本？** |
| 5 | IP 地址 | 192.168.100.100 | ✅ 已确认 |
| 6 | HTTP 端口 | 8080 | ✅ 已确认 |
| 7 | FSUService 路径 | /services/FSUService | ✅ 已确认 |
| 8 | 协议版本 | B-2016 | **是否支持 B接口 2016？** |

### 3.2 通信参数

| # | 字段 | 平台当前值 | 需 FSU 管理方确认 |
|---|------|----------|-----------------|
| 1 | SC 上报地址 | 未配置 | **FSU 向平台上报的 SCService URL？** |
| 2 | 登录密码 | 未存储 | **FSU LOGIN 密码？（仅用于配置，不写入日志）** |
| 3 | FTP 服务器地址 | 未配置 | **FTP Host/Port/Username？** |
| 4 | FTP 路径 | 未配置 | **FTP BasePath？** |

### 3.3 监控点位清单（核心）

**必须提供以下信息**（见附件 CSV 模板）：

| # | 字段 | 必填 | 说明 |
|---|------|------|------|
| 1 | signal_id | **是** | FSU 侧实际 SignalID，不可为空 |
| 2 | signal_name | **是** | 中文名称，如"机柜温度" |
| 3 | signal_type | **是** | AI(模拟量)/DI(数字量)/DO(输出)/PI(脉冲)/ACC(累加) |
| 4 | unit | 否 | 单位，如 °C / %RH / V / A |
| 5 | data_type | 否 | NUMBER / TEXT / BOOLEAN，默认 NUMBER |
| 6 | alarm_upper | 否 | 告警上限 |
| 7 | alarm_lower | 否 | 告警下限 |
| 8 | alarm_upper_urgent | 否 | 严重告警上限 |
| 9 | alarm_lower_urgent | 否 | 严重告警下限 |
| 10 | enabled | 否 | 是否启用，默认是 |
| 11 | remark | 否 | 备注 |

---

## 四、点位采集 CSV 模板

### 4.1 模板格式

```csv
fsu_code,device_id,device_name,signal_id,signal_name,signal_type,unit,data_type,alarm_upper,alarm_lower,alarm_upper_urgent,alarm_lower_urgent,enabled,remark
```

### 4.2 字段说明

| 列 | 类型 | 必填 | 示例值 | 说明 |
|----|------|------|--------|------|
| fsu_code | String | **是** | 51051243812345 | FSU 编码 |
| device_id | String | **是** | FSU-2808IM-001 | 设备唯一标识 |
| device_name | String | 否 | 艾默生2808IM-1号柜 | 设备名称 |
| signal_id | String | **是** | TEMP-01 | **FSU 侧实际 SignalID** |
| signal_name | String | **是** | 机柜温度 | 信号中文名称 |
| signal_type | String | **是** | AI | AI/DI/DO/PI/ACC |
| unit | String | 否 | °C | 单位 |
| data_type | String | 否 | NUMBER | NUMBER/TEXT/BOOLEAN |
| alarm_upper | Decimal | 否 | 60.0 | 告警上限 |
| alarm_lower | Decimal | 否 | -5.0 | 告警下限 |
| alarm_upper_urgent | Decimal | 否 | 70.0 | 严重告警上限 |
| alarm_lower_urgent | Decimal | 否 | -10.0 | 严重告警下限 |
| enabled | Boolean | 否 | true | 是否启用 |
| remark | String | 否 | | 备注 |

### 4.3 填写示例

```csv
fsu_code,device_id,device_name,signal_id,signal_name,signal_type,unit,data_type,alarm_upper,alarm_lower,alarm_upper_urgent,alarm_lower_urgent,enabled,remark
51051243812345,FSU-2808IM-001,艾默生2808IM-1号柜,TEMP-01,机柜温度,AI,°C,NUMBER,60.0,-5.0,70.0,-10.0,true,
51051243812345,FSU-2808IM-001,艾默生2808IM-1号柜,HUMI-01,机柜湿度,AI,%RH,NUMBER,90.0,10.0,95.0,5.0,true,
51051243812345,FSU-2808IM-001,艾默生2808IM-1号柜,VOLT-A,交流电压A相,AI,V,NUMBER,245.0,198.0,260.0,185.0,true,
51051243812345,FSU-2808IM-001,艾默生2808IM-1号柜,DOOR-01,柜门状态,DI,,TEXT,,,,,,true,
51051243812345,FSU-2808IM-001,艾默生2808IM-1号柜,WATER-01,水浸传感器,DI,,TEXT,,,,,,true,
```

> **注意**：上表示例中的 SignalID（TEMP-01 等）仅为格式示意，**实际值必须由 FSU 管理方填写**。

---

## 五、平台补录规则

### 5.1 SignalID 规则

| 规则 | 说明 |
|------|------|
| SignalID 来源 | **必须来自 FSU 管理方提供的真实配置** |
| 禁止自造 | B接口协议禁止平台侧自造 SignalID |
| 占位值废止 | TEMP-R01 / HUMI-R01 是临时占位值，收到真实数据后删除 |
| pointCode = SignalID | `monitoring_point.point_code` 必须等于 FSU 侧实际 SignalID |
| 大小写敏感 | SignalID 大小写必须与 FSU 一致 |

### 5.2 补录流程

```
1. 收到 FSU 管理方填写的 CSV 点位表
   ↓
2. 人工确认：至少包含 fsu_code + 1 个有效 signal_id
   ↓
3. BIF-P4-007-B：基于 CSV 生成 seed SQL
   ↓
4. 人工审核 seed SQL（确认 SignalID 非自造）
   ↓
5. 执行 seed SQL 到平台数据库
   ↓
6. BIF-P4-008：用真实 SignalID 重新联调
```

### 5.3 seed SQL 生成规则（BIF-P4-007-B 阶段执行）

```sql
-- 1. 清理占位点位（如有）
DELETE FROM monitoring_point WHERE fsu_id = (SELECT id FROM fsu_device WHERE fsu_code = '51051243812345')
AND point_code IN ('TEMP-R01', 'HUMI-R01');

-- 2. 插入真实点位（基于 CSV）
INSERT INTO monitoring_point (fsu_id, point_code, point_name, point_type, data_type, unit,
    alarm_upper, alarm_lower, alarm_upper_urgent, alarm_lower_urgent, status)
SELECT f.id, '真实SignalID', '真实名称', 'AI', 'NUMBER', '°C',
    60.0, -5.0, 70.0, -10.0, 'ACTIVE'
FROM fsu_device f WHERE f.fsu_code = '51051243812345';
-- ... 每个 signal_id 一行
```

### 5.4 门限规则

- 如果 FSU 管理方提供了门限值，直接录入
- 如果未提供，门限字段留 NULL（不影响 GET_DATA，只影响 GET_THRESHOLD 结果关联）
- 门限值不参与 B接口协议通信，仅用于平台侧告警判断参考

---

## 六、禁止自造 SignalID 说明

B接口协议 2016 要求：

1. SignalID 是 FSU 侧定义的信号量标识，由 FSU 厂商在设备出厂或现场配置时确定
2. SC 平台无权自造 SignalID
3. 平台 GET_DATA 请求中的 SignalID 必须与 FSU 实际配置完全一致
4. 使用不存在的 SignalID 会导致 FSU 返回空数据（ResultCode=0, signals=[]）
5. TEMP-R01 / HUMI-R01 是平台开发阶段的临时占位值，**已确认 FSU 不识别这些 ID**
6. 必须等到 FSU 管理方提供真实 SignalID 后才能生成 seed SQL

**违反此规则的后果**：
- 联调持续失败
- 平台无法获取真实监控数据
- 点位映射混乱，后续维护成本高

---

## 七、后续 BIF-P4-007-B 输入条件

BIF-P4-007-B（生成 seed SQL）的启动条件：

| # | 条件 | 状态 |
|---|------|------|
| 1 | 已收到 FSU 管理方填写的点位表（CSV/Excel） | ⬜ 等待 |
| 2 | FSUCode 已确认 | ⬜ 等待 |
| 3 | SC 上报地址已确认 | ⬜ 等待 |
| 4 | 至少包含 1 个有效 SignalID | ⬜ 等待 |
| 5 | 至少包含 DeviceID / SignalID / SignalName | ⬜ 等待 |
| 6 | 经人工确认可以生成 seed SQL | ⬜ 等待 |

---

## 八、安全边界

| 检查项 | 状态 |
|--------|------|
| 访问真实设备 | ❌ 否 |
| real-call-enabled | false |
| Scheduler | ❌ 否 |
| SET_THRESHOLD | ❌ 否 |
| SET_POINT | ❌ 否 |
| SET_FTP | ❌ 否 |
| SET_FSUREBOOT | ❌ 否 |
| 写入真实密码 | ❌ 否 |
| 生成 seed SQL | ❌ 否（等待真实数据） |
| 修改业务代码 | ❌ 否 |

---

## 九、如果暂时拿不到点位表

如果 FSU 管理方短时间无法提供点位资料，平台侧可并行推进的任务：

1. **BIF-P4-008：SCService 入站 RPC 兼容性评估**
   - 评估 FSU → SC 入站方向是否也需要 RPC 适配
   - 分析 SCService.wsdl 与当前入站处理逻辑的一致性
   - 不依赖 FSU 点位信息

2. **BIF-P4-009：慢数据轮询框架完善**
   - 完善 SlowDataPollingService 的错误重试/降级逻辑
   - 不依赖真实 SignalID（可使用 demo 数据）

---

## 十、交付物清单

| # | 文件 | 用途 |
|---|------|------|
| 1 | 本文档 | 运维协调说明 |
| 2 | 附录A：CSV 模板 | 点位采集表格（见 §4.2-4.3） |
| 3 | `BIF-P4-006` 审计报告 | 联调结果详情 |

---

## 附录A：CSV 模板（可直接发送给 FSU 管理方）

```
B接口监控点位采集表 - 艾默生 2808IM (192.168.100.100:8080)

说明：
1. fsu_code/signal_id/signal_name/signal_type 为必填列
2. signal_id 请填写 FSU 设备侧实际配置的信号量标识
3. alarm_* 为告警门限，如未配置可留空
4. 单位(unit)请使用标准符号：°C, %RH, V, A, kW, kWh 等

CSV 格式：
fsu_code,device_id,device_name,signal_id,signal_name,signal_type,unit,data_type,alarm_upper,alarm_lower,alarm_upper_urgent,alarm_lower_urgent,enabled,remark

请在此行下方填写：
```
