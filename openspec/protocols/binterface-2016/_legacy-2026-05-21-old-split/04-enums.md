# 04 — PK_Type 枚举定义

## 1. 完整枚举

```java
public enum BInterfacePkType {
    LOGIN,            // FSU登录 (101)
    LOGOUT,           // FSU登出 (103)
    SEND_DATA,        // FSU实时数据上报
    SEND_ALARM,       // FSU告警上报 (501)
    GET_DATA,         // SC获取监控数据 (401)
    GET_HISDATA,      // SC获取历史数据 (403)
    SET_POINT,        // SC遥控遥调 (1001)
    GET_THRESHOLD,    // SC获取告警门限 (1901)
    SET_THRESHOLD,    // SC设置告警门限 (2001)
    TIME_CHECK,       // SC时间同步 (1301)
    GET_FTP,          // SC获取FTP参数 (1601)
    SET_FTP,          // SC设置FTP参数 (1603)
    GET_LOGININFO,    // SC获取登录信息 (1501)
    SET_LOGININFO,    // SC设置登录信息 (1503)
    GET_FSUINFO,      // SC获取FSU信息 (1701)
    SET_FSUREBOOT,    // SC远程重启FSU (1801) — 安全禁用
    UNKNOWN           // 未知命令
}
```

## 2. 2016 Code 映射表

| 枚举 | 2016 Code | ACK Code | 方向 |
|------|-----------|----------|------|
| LOGIN | 101 | 102 | FSU→SC |
| LOGOUT | 103 | 104 | FSU→SC |
| SEND_DATA | — | — | FSU→SC |
| SEND_ALARM | 501 | 502 | FSU→SC |
| GET_DATA | 401 | 402 | SC→FSU |
| GET_HISDATA | 403 | 404 | SC→FSU |
| SET_POINT | 1001 | 1002 | SC→FSU |
| GET_THRESHOLD | 1901 | 1902 | SC→FSU |
| SET_THRESHOLD | 2001 | 2002 | SC→FSU |
| TIME_CHECK | 1301 | 1302 | SC→FSU |
| GET_FTP | 1601 | 1602 | SC→FSU |
| SET_FTP | 1603 | 1604 | SC→FSU |
| GET_LOGININFO | 1501 | 1502 | SC→FSU |
| SET_LOGININFO | 1503 | 1504 | SC→FSU |
| GET_FSUINFO | 1701 | 1702 | SC→FSU |
| SET_FSUREBOOT | 1801 | 1802 | SC→FSU |

## 3. 2016 ↔ 2024 差异

| 命令 | 2016 Code | 2024 Code | 差异说明 |
|------|-----------|-----------|---------|
| GET_DATA | 401 | 501 | Code 不同 |
| SEND_ALARM | 501 | 601 | Code 不同 |
| GET_LOGININFO | 1501 | — | 2016 独有 |
| GET_FTP | 1601 | 801 (GET_SUFTP) | 名称和 Code 均不同 |
| GET_FSUINFO | 1701 | 1001 (GET_SUINFO) | 名称和 Code 均不同 |

## 4. 平台实现

| 文件 | 说明 |
|------|------|
| `BInterfacePkType.java` | 枚举定义 |
| `BInterfaceCommand2016.java` | 2016 Code 映射（6条） |
| `BInterfaceCommand2024.java` | 2024 Code 映射（44条） |
| `BInterfaceCommandAliasMapper.java` | 旧名称→2024 映射 |

## 5. 已知缺口

- BInterfaceCommand2016 缺少 GET_THRESHOLD=1901
- BInterfaceCommand2016 缺少 GET_HISDATA=403
- BInterfaceCommand2016 缺少 TIME_CHECK=1301
- BInterfaceCommand2016 缺少 SET_POINT=1001
- BInterfaceCommand2016 缺少 SET_LOGININFO=1503
- BInterfaceCommand2016 缺少 SET_FTP=1603
- BInterfaceCommand2016 缺少 SET_THRESHOLD=2001
- BInterfaceCommand2016 缺少 SET_FSUREBOOT=1801
