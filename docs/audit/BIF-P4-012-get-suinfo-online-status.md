# BIF-P4-012：GET_SUINFO 在线状态闭环

> 基于 BIF-P4-010 命令枚举 + BIF-P4-011 解析层
> 完成日期：2026-05-15

---

## 一、本阶段目标

实现 B接口 2024 标准心跳/在线状态命令 GET_SUINFO (Code=1001) / GET_SUINFO_ACK (Code=1002)。

---

## 二、协议依据

| 字段 | GET_SUINFO | GET_SUINFO_ACK |
|------|-----------|----------------|
| Code | 1001 | 1002 |
| 方向 | SC→FSU | FSU→SC |
| Info | SUID | SUID, Result, FailureCode |
| xmlData | (空) | TSUStatus(CPUUsage, MEMUsage, SUDateTime) |

---

## 三、实现说明

### 新增文件

| 文件 | 说明 |
|------|------|
| `GetSuInfoResult.java` | 结果模型 (suid/cpu/mem/suDateTime) |
| `GetSuInfoService.java` | 服务层: 构造请求, FSU调用, 解析响应, 更新状态 |
| `GetSuInfoCommandHandler.java` | 命令处理器: Info提取, 登录校验, SUID兼容提取 |

### 修改文件

| 文件 | 变更 |
|------|------|
| `BInterfacePkType.java` | 新增 GET_SUINFO |
| `StubFsuServiceClient.java` | 新增 GET_SUINFO stub 响应 |

---

## 四、与 HEARTBEAT 旧路径关系

| 项目 | HEARTBEAT (2016) | GET_SUINFO (2024) |
|------|-----------------|-------------------|
| 方向 | FSU→SC | SC→FSU |
| 触发 | FSU 主动 | SC 轮询/手动 |
| 数据 | CPU/Mem/温度 | CPU/Mem/SU时间 |
| 状态更新 | loginService | fsuStatusRepository |
| 兼容 | 保留 | 新主路径 |

---

## 五、测试覆盖

GetSuInfoServiceTest (11 tests): SUID/CPU/MEM/SUDateTime解析, 在线状态更新, null/empty, FSU错误, PK_Type格式

---

## 六、全量回归

923 tests, 0 failures, 1 pre-existing Error
