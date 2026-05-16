# BIF-P4-010：2024 命令枚举与字段别名映射层

> 基于 BIF-P4-009 审计结论
> 完成日期：2026-05-15

---

## 一、本阶段目标

新增 B接口 2024 标准命令枚举、旧命名兼容映射、字段别名映射，在不破坏现有业务的前提下建立 2024 协议兼容基础。

---

## 二、BIF-P4-009 结论摘要

- MD 迁移文件与 PDF 原文协议规范层完全一致
- 2024 协议 44 条命令，使用 Name + Code 双标识
- 字段命名变更：FSUCode→SUID, SignalID→SPID 等

---

## 三、2024 命令枚举设计

### BInterfaceCommand2024

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 命令名, 如 "GET_DATA" |
| code | int | 数字代号, 如 501 |
| direction | Direction | FSU_TO_SC / SC_TO_FSU |
| category | Category | 命令分类 (10 类) |
| highRisk | boolean | 高风险标记 (SET_* 类) |

### 命令覆盖：44 条全部

| 分组 | 数量 | Code 范围 |
|------|------|----------|
| 网络联接 | 6 | 101-106 |
| 标准化配置 | 6 | 201-206 |
| 厂家配置 | 8 | 301-308 |
| 配置模板 | 4 | 401-404 |
| 实时/历史数据 | 4 | 501-504 |
| 告警 | 4 | 601-604 |
| 控制 | 2 | 701-702 |
| FTP | 4 | 801-804 |
| 系统/辅助 | 6 | 901-1102 |

---

## 四、旧命名兼容策略

### 映射表

| 旧名 (2016) | 2024 标准 | 策略 |
|------------|----------|------|
| LOGIN | LOGIN | 同名直接映射 |
| GET_DATA | GET_DATA | 同名直接映射 |
| SEND_ALARM | SEND_ALARM | 同名直接映射 |
| HEARTBEAT | GET_SUINFO | 重命名映射 |
| TIME_CHECK | SET_TIME | 重命名映射 |
| GET_FTP | GET_SUFTP | 重命名映射 |
| SET_FTP | SET_SUFTP | 重命名映射 |
| SET_POINT | SET_RMCTRLCMD | 重命名映射 |
| GET_FSUINFO | GET_SUINFO | 重命名映射 |
| SET_FSUREBOOT | SET_SUREBOOT | 重命名映射 |
| GET_THRESHOLD | (兼容保留) | 2024 无对应标准命令 |
| SET_THRESHOLD | (兼容保留) | 2024 无对应标准命令 |
| GET_LOGININFO | (兼容保留) | 2024 无独立命令 |

### 兼容命令标记

`BInterfaceCommandAliasMapper.isCompatCommand()` 标记 2024 标准中不存在的旧命令：
- GET_THRESHOLD — 真实艾默生设备已接受，必须保留
- SET_THRESHOLD — 同上
- GET_LOGININFO — 2024 无独立命令
- GET_HISTORY_DATA — 2024 用 ASK_TODAYHISDATA 替代

---

## 五、字段别名映射

| 旧字段 | 2024 标准 | 变体支持 |
|--------|----------|---------|
| FSUCode | SUID | FsuCode, fsu_code, SUCode, FSUID |
| SignalID | SPID | SignalId, signal_id, PointID, PointCode |
| FSUPort | SUPort | — |
| FSUIP | SUIP | — |
| DeviceID | DeviceID | DeviceId (同名) |

---

## 六、高风险命令标记

| 命令 | Code | 风险 |
|------|------|------|
| SET_SCHEMECONFIG | 205 | 配置下发 |
| SET_FACTORYCONFIG | 307 | 厂家配置下发 |
| SET_SPCONFIGOPTION | 403 | 测点配置修改 |
| SET_RMCTRLCMD | 701 | 遥控遥调 |
| SET_SUFTP | 803 | FTP参数设置 |
| SET_SUREBOOT | 1101 | 远程重启 |

---

## 七、对现有业务影响

**零影响。** 本阶段仅新增枚举和静态映射类，不修改任何现有代码路径。现有 BInterfacePkType 完整保留。FsuServiceRpcAdapter 不受影响。

---

## 八、测试覆盖

| 测试类 | 测试数 | 覆盖 |
|--------|--------|------|
| BInterfaceCommand2024Test | 34 | 44命令全量, Code双向, ACK配对, 高风险标记 |
| BInterfaceCommandAliasMapperTest | 17 | 同名映射, 重命名映射, 兼容命令, null安全 |
| BInterfaceFieldAliasMapperTest | 23 | 正向/反向映射, 大小写变体, 别名判定 |
| **合计** | **74** | |

---

## 九、后续迁移任务

1. BIF-P4-011: XmlDataParser 整合字段别名映射
2. BIF-P4-012: PK_Type 解析支持 Name+Code 双标识
3. BIF-P4-013: 新增 2024 新增命令的 Handler 骨架
