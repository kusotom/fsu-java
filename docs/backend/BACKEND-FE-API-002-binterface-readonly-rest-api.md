# BACKEND-FE-API-002: B接口前端只读 REST API

## 日期
2026-05-22

## 新增 API (8 端点)

| 路径 | 说明 | Smoke Test |
|------|------|:--:|
| GET /api/b-interface/fsus | FSU 列表 | code=0, count=2 |
| GET /api/b-interface/fsus/{fsuCode} | FSU 详情 | ✅ |
| GET /api/b-interface/fsus/{fsuCode}/devices | DeviceID 列表 | code=0, count=5 |
| GET /api/b-interface/alarms | 告警列表 | code=0, count=1 |
| GET /api/b-interface/realtime-points | 实时数据 | code=0, count=5 |
| GET /api/b-interface/unmapped-signals | 未映射设备 | code=0, count=5 |
| GET /api/b-interface/thresholds | 门限列表 | code=0, count=0 |
| (已有) /api/b-interface/message-logs/query | 报文日志 | ✅ |

## 新增文件
- BInterfaceFrontendReadController.java, BInterfaceFrontendDtos.java

## 测试
1297 tests, 0/0/9.
