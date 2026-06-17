# 07 — 命令总目录

## 1. 全部命令一览

| # | 命令 | Code | ACK Code | 方向 | 类别 | 优先级 |
|---|------|------|----------|------|------|--------|
| 1 | LOGIN | 101 | 102 | FSU→SC | 连接管理 | P0 |
| 2 | LOGOUT | 103 | 104 | FSU→SC | 连接管理 | P2 |
| 3 | SEND_ALARM | 501 | 502 | FSU→SC | 告警 | P0 |
| 4 | GET_DATA | 401 | 402 | SC→FSU | 实时数据 | P0 |
| 5 | GET_HISDATA | 403 | 404 | SC→FSU | 历史数据 | P2 |
| 6 | SET_POINT | 1001 | 1002 | SC→FSU | 遥控遥调 | P1 |
| 7 | GET_THRESHOLD | 1901 | 1902 | SC→FSU | 参数查询 | P1 |
| 8 | SET_THRESHOLD | 2001 | 2002 | SC→FSU | 参数设置 | P1 |
| 9 | GET_LOGININFO | 1501 | 1502 | SC→FSU | 参数查询 | P1 |
| 10 | SET_LOGININFO | 1503 | 1504 | SC→FSU | 参数设置 | P2 |
| 11 | GET_FTP | 1601 | 1602 | SC→FSU | FTP | P1 |
| 12 | SET_FTP | 1603 | 1604 | SC→FSU | FTP | P1 |
| 13 | TIME_CHECK | 1301 | 1302 | SC→FSU | 时间同步 | P2 |
| 14 | GET_FSUINFO | 1701 | 1702 | SC→FSU | FSU信息 | P1 |
| 15 | SET_FSUREBOOT | 1801 | 1802 | SC→FSU | 远程控制 | P2 (安全禁用) |

## 2. 分类汇总

| 类别 | 命令数 | 命令 |
|------|--------|------|
| 连接管理 | 2 | LOGIN, LOGOUT |
| 实时数据 | 1 | GET_DATA |
| 告警 | 1 | SEND_ALARM |
| 遥控遥调 | 1 | SET_POINT |
| 参数查询 | 3 | GET_THRESHOLD, GET_LOGININFO, GET_FSUINFO |
| 参数设置 | 3 | SET_THRESHOLD, SET_LOGININFO, SET_FTP |
| FTP | 2 | GET_FTP, SET_FTP |
| 时间同步 | 1 | TIME_CHECK |
| 历史数据 | 1 | GET_HISDATA |

## 3. 按方向

| 方向 | 命令数 | 命令 |
|------|--------|------|
| FSU→SC (入站) | 3 | LOGIN, LOGOUT, SEND_ALARM |
| SC→FSU (出站) | 12 | GET_DATA, GET_HISDATA, SET_POINT, GET_THRESHOLD, SET_THRESHOLD, GET_LOGININFO, SET_LOGININFO, GET_FTP, SET_FTP, TIME_CHECK, GET_FSUINFO, SET_FSUREBOOT |

## 4. 当前项目实现状态

| 命令 | 实现状态 | 说明 |
|------|---------|------|
| LOGIN | ✅ 已实现+实测 | LANDING-013-FIX-001 验证通过 |
| LOGOUT | ❌ 未实现 | — |
| SEND_ALARM | ✅ 已实现 | 等待 FSU 主动上报 |
| GET_DATA | ✅ 部分实现 | 2016格式已就绪，FSU无测量数据 |
| GET_HISDATA | ❌ 未实现 | — |
| SET_POINT | ✅ 已实现 | 安全禁用默认 |
| GET_THRESHOLD | ✅ 已实现 | 2024格式 |
| SET_THRESHOLD | ✅ 已实现 | 安全禁用默认 |
| GET_LOGININFO | ✅ 已实现+实测 | LANDING-006 验证通过 |
| SET_LOGININFO | ❌ 未实现 | — |
| GET_FTP | ✅ 已实现+实测 | LANDING-006 验证通过 |
| SET_FTP | ❌ 未实现 | — |
| TIME_CHECK | ✅ 已实现 | — |
| GET_FSUINFO | ✅ 已实现+实测 | LANDING-015 FIX-002 对齐 |
| SET_FSUREBOOT | ❌ 安全禁用 | — |

## 5. 详细规范文件

每个命令的详细规范在 `commands/` 目录中：

| # | 文件 |
|---|------|
| 1 | `commands/010-login.md` |
| 2 | `commands/020-logout.md` |
| ... | ... |
| 15 | `commands/150-set-fsureboot.md` |
