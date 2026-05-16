# B接口架构设计

## 双通道模型

B接口包含两条通信通道：

### 1. 快数据通道（FSU → SC）

FSU作为客户端，SC作为服务端。

- FSU主动上报：LOGIN、HEARTBEAT、SEND_ALARM
- SC提供 SCService 接口接收

### 2. 慢数据通道（SC → FSU）

SC作为客户端，FSU作为服务端。

- SC主动轮询：GET_DATA
- SC主动控制：SET_DATA、SET_FSUREBOOT
- SC调用 FSUService 接口

## 当前实现状态

- SCService 服务端：占位（POST /api/b-interface/sc-service）
- FSUService 客户端：Stub（FsuServiceClientStub）
- SOAP/XML 解析：未实现
- WSDL：占位
- FTP：仅数据模型

## 后续实现计划

详见 docs/tasks/ 和 BIF-P0/BIF-P1 系列任务。
