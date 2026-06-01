# 01 — 通信架构

## 1. 双通道模型

```
┌─────────────────────────────────────────────────────────┐
│                      SC (监控中心)                        │
│  ┌──────────────────┐        ┌──────────────────┐       │
│  │  SCService 服务端  │        │  FSUService 客户端 │       │
│  │  (接收FSU上报)    │        │  (轮询FSU数据)    │       │
│  └────────┬─────────┘        └────────┬─────────┘       │
│           │                           │                  │
└───────────┼───────────────────────────┼──────────────────┘
            │  ▲                   │  ▲
            │  │ 快数据通道         │  │ 慢数据通道
            ▼  │                   ▼  │
┌──────────────────────────────────────────┐
│                FSU (现场监控单元)           │
│  ┌──────────────────┐  ┌──────────────┐  │
│  │  SCService 客户端  │  │ FSUService   │  │
│  │  (主动上报)       │  │ 服务端(响应) │  │
│  └──────────────────┘  └──────────────┘  │
└──────────────────────────────────────────┘
```

## 2. 快数据通道（FSU → SC，主动上报）

| 特征 | 说明 |
|------|------|
| 发起方 | FSU |
| 接收方 | SC |
| SC 角色 | 服务端（SCService） |
| WSDL 端点 | `/services/SCService` |
| 上报命令 | LOGIN, HEARTBEAT, SEND_DATA, SEND_ALARM |
| 频率 | 秒~分钟级 |

## 3. 慢数据通道（SC → FSU，SC 轮询）

| 特征 | 说明 |
|------|------|
| 发起方 | SC |
| 接收方 | FSU |
| SC 角色 | 客户端（FSUService Client） |
| WSDL 端点 | `/services/FSUService` |
| 轮询命令 | GET_DATA, SET_POINT, GET_THRESHOLD, SET_THRESHOLD, TIME_CHECK, GET_FTP, SET_FTP, GET_LOGININFO, GET_FSUINFO, SET_DATA, SET_FSUREBOOT, GET_HISTORY_DATA |
| 频率 | 分钟~小时级 |

## 4. 通道对比

| 对比项 | 快数据通道 | 慢数据通道 |
|--------|-----------|-----------|
| 启动方式 | FSU 主动 | SC 轮询 |
| 方向 | FSU → SC | SC → FSU |
| WSDL | SCService | FSUService |
| SC 角色 | 服务端 | 客户端 |

## 5. 平台实现映射

| 通道 | 项目入口 | 实现 |
|------|---------|------|
| 快数据 (FSU→SC) | `POST /services/SCService` | `StandardScServiceController` |
| 快数据 (FSU→SC) 兼容 | `POST /api/b-interface/sc-service` | `ScServiceController` |
| 慢数据 (SC→FSU) | `FsuServiceClient.call()` | `RealHttpFsuServiceClient` / `StubFsuServiceClient` |
