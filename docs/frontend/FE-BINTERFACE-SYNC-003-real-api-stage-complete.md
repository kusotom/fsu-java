# FE-BINTERFACE-SYNC-003: B接口前端真实 API 阶段同步

## 日期
2026-05-22

## 完成状态

### Auth/权限: 前后端闭环
用户管理/角色管理/权限点管理/登录会话/路由守卫 全部完成.
Token: Pinia内存, 无localStorage/sessionStorage.

### B接口第一批 (8页)
FSU状态/实时数据/告警/门限: 全部真实API, 空态正常.

### B接口第二批 (5页)
FTP注册参数/FTP图片/调度配置/协议治理/ActiveAlarm: 全部真实API+fallback兜底.
SET全部blocked_by_default. run-once只展示能力状态.

### 进度
项目整体 ~85-88%. 前端 ~90-93%. 后端 ~84-87%.

## 阻塞
DeviceID → SPID/SignalID 映射表缺失 (5个DeviceID已知, 无SignalID).

## 下一阶段
DATA-MAPPING-001: 点位字典解析与映射导入规划.
