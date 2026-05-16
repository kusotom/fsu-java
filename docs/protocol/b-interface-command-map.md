# B接口命令码映射表

## 占位命令表

| PK_Type | 方向 | 说明 | 优先级 | 状态 |
|---------|------|------|--------|------|
| LOGIN | FSU→SC | FSU登录认证 | P0 | PENDING |
| HEARTBEAT | FSU→SC | FSU心跳上报 | P0 | PENDING |
| SEND_DATA | FSU→SC | 实时数据上报 | P0 | PENDING |
| SEND_ALARM | FSU→SC | 告警上报 | P0 | PENDING |
| GET_DATA | SC→FSU | 获取监控数据 | P0 | PENDING |
| GET_HISTORY_DATA | SC→FSU | 获取历史数据 | P2 | PENDING |
| SET_POINT | SC→FSU | 设置点位参数 | P1 | PENDING |
| GET_THRESHOLD | SC→FSU | 获取告警阈值 | P1 | PENDING |
| SET_THRESHOLD | SC→FSU | 设置告警阈值 | P1 | PENDING |
| TIME_CHECK | SC→FSU | 时间同步 | P2 | PENDING |
| GET_FTP | SC→FSU | 获取FTP文件信息 | P1 | PENDING |
| SET_FTP | SC→FSU | 设置FTP参数 | P1 | PENDING |
| GET_LOGININFO | SC→FSU | 获取登录信息 | P2 | PENDING |
| GET_FSUINFO | SC→FSU | 获取FSU信息 | P2 | PENDING |
| SET_DATA | SC→FSU | 设置监控数据 | P2 | PENDING |
| SET_FSUREBOOT | SC→FSU | 远程重启FSU（安全禁用） | P2 | PENDING |
| UNKNOWN | BOTH | 未知命令（兼容） | P3 | PENDING |

## 状态说明

- PENDING：待实现
- STUB：已创建占位类
- IMPLEMENTED：已完成
- DISABLED：安全禁用
