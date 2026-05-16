# 架构总览

## 项目定位

机房动环监控平台，用于户外机柜动力环境采集、FSU接入、设备管理、点位管理、实时数据、历史数据、告警管理。

## 协议基础

本项目基于《中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0，2016 年 9 月》。

B接口核心特征：
1. SC 与 FSU 之间通过 B 接口互联
2. 数据流接口基于 WebService + SOAP + XML
3. B 接口还包含 FTP 文件/图片能力
4. SC 轮询 FSU 获取慢数据时，SC 是客户端，FSU 是服务端
5. FSU 主动上报告警、状态切换等快数据时，FSU 是客户端，SC 是服务端

## 技术栈

- 前端：Vue 3 + TypeScript + Vite + Element Plus
- 后端：Java + Spring Boot 3.2 + JPA
- 数据库：PostgreSQL 15
- 部署：Docker / Docker Compose

## 模块划分

- resource - 资源管理（站点、机柜、FSU、点位）
- telemetry - 数据监控（实时、历史）
- alarm - 告警管理
- binterface - B接口核心模块（SCService、FSUService、SOAP/XML、FTP）
- task - 定时任务
- log - 日志管理
