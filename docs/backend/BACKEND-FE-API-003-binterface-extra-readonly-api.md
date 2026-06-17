# BACKEND-FE-API-003: B接口剩余只读 REST API

## 日期
2026-05-22

## 新增 20 个 API 端点

| 类别 | 路径 | 数据 | Smoke |
|------|------|------|:--:|
| FTP | /ftp, /fsus/{fsuCode}/ftp | 空 | ✅ |
| LoginInfo | /login-info | FSU 注册信息 | ✅ |
| FTP Images | /ftp/images, /ftp/images/pull-runs | 空 | ✅ |
| Scheduler | /schedulers/configs (4 entries) | config defaults | ✅ |
| Scheduler | /schedulers/runs | 空 | ✅ |
| Protocol | /protocol/matrix (16 cmds) | backend-planning | ✅ |
| Protocol | /error-codes (9 codes) | static | ✅ |
| Protocol | /protocol/profiles (3) | static | ✅ |
| Protocol | /audits | 空 | ✅ |
| SET | /set/safety-policies (6) | blocked_by_default | ✅ |
| ActiveAlarm | /active-alarms/diff | 空 | ✅ |
| ActiveAlarm | /active-alarms/audit-runs | 空 | ✅ |
| RunOnce | /run-once/capabilities (5) | planning | ✅ |

## 安全

不访问真实 FSU, 不执行 SET, 不启 Scheduler, 不返回明文密码.
FTP 密码脱敏. SET policies 全部 blocked_by_default.

## 测试

1297 tests, 0/0/9.
