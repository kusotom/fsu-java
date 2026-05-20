# LANDING-002: B接口 2024 附录标准化配置文件模板与点位映射规范

## 1. 时间

2026-05-20

## 2. 策略变更

用户已放弃单纯 Demo 阶段，当前转向项目真实落地。

## 3. Codex 复审状态

**Codex 复审暂缓。** BIF-P4-FIX-001 ~ LANDING-001 已累积。

## 4. 目标

基于 B接口 2024 附录（SUConfigInstance.xml / StdSPDic.xml / StdSPConfigOptionDic.xml / TAlarm），建立标准化点位表模板、字段映射规范和接收检查清单，为后续接收 FSU 管理方真实数据做准备。

## 5. 边界

| 边界 | 说明 |
|------|------|
| 只做文档/模板 | 不写 Java 代码 |
| 不访问真实 FSU | 不访问 192.168.100.100 |
| 不启用 Scheduler | 默认 false |
| 不执行 SET | 不调用任何 SET_ 命令 |
| 不填 demo 数据 | 模板只有表头，无示例值 |

## 6. 新增文件

| 文件 | 说明 |
|------|------|
| `docs/landing/REAL-SUCONFIGINSTANCE-TEMPLATE.csv` | SUConfigInstance 模板 28 列 |
| `docs/landing/REAL-STD-SP-DIC-TEMPLATE.csv` | StdSPDic 模板 12 列 |
| `docs/landing/REAL-STD-SP-CONFIG-OPTION-TEMPLATE.csv` | StdSPConfigOptionDic 模板 17 列 |
| `docs/landing/REAL-POINT-TABLE-MAPPING.md` | 字段映射规范 8 章 |
| `docs/landing/REAL-POINT-TABLE-CHECKLIST.md` | 检查清单 7 章 |

## 7. 协议核对

字段来源已从 `B接口协议全量结构化迁移-Claude-Codex双校验版.md` 核对。三类配置文件关系：

```
StdSPDic → 标准设备类型 + 标准监控点字典
StdSPConfigOptionDic → 配置方案（门限/存储/延时）
SUConfigInstance → FSU 实例配置（关联 OptionID）
```

## 8. 字段来源三类区分

- **协议能确定**: 字段结构、XML 标签、属性名、配置关系、TAlarm 结构、GET_SPCONFIGOPTION 规则
- **现场必须提供**: 真实 SUID、DeviceID、SPID、serviceUrl、设备名称、点位名称、OptionID 实际值、门限参数、SET 授权
- **平台生成**: localDeviceId、localPointId、enabled、setSafetyLevel、confirmationRequired、时间戳

## 9. TAlarm 关键区分

- SerialNo 是告警**实例**字段（每次告警事件唯一），不属于静态点位表
- 静态点位表（SUConfigInstance）不含 SerialNo

## 10. 安全边界确认

- [x] 不访问真实 FSU
- [x] 不启用 Scheduler
- [x] 不执行 SET
- [x] 不修改 Java/SQL 代码
- [x] 无 demo 数据
- [x] 协议样例值未当作现场真实值
