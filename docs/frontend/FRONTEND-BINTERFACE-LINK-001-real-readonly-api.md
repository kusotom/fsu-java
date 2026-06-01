# FRONTEND-BINTERFACE-LINK-001: 前端接入真实后端 API

## 日期
2026-05-22

## 接入 API

| API | 页面 | 状态 |
|-----|------|:--:|
| GET /b-interface/fsus | FSUStatusView | ✅ |
| GET /b-interface/fsus/{fsuCode} | FSUDetailView | ✅ |
| GET /b-interface/fsus/{fsuCode}/devices | FSUDetailView | ✅ |
| GET /b-interface/alarms | BInterfaceAlarmView | ✅ |
| GET /b-interface/realtime-points | BInterfaceRealtimeView | ✅ |
| GET /b-interface/unmapped-signals | BInterfaceRealtimeView | ✅ |
| GET /b-interface/thresholds | BInterfaceThresholdView | ✅ |
| GET /b-interface/message-logs/query | (已有) | ✅ |

## 页面改造

FSU状态→真实/fsus, 告警→真实/alarms, 实时数据→真实realtime-points+unmapped.

## 验证

TypeScript 0 errors, Build ✓, Backend APIs smoke-tested OK.
