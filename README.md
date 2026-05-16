# 机房动环监控平台

## 项目目标

户外机柜动力环境采集、FSU 接入、设备管理、点位管理、实时数据、历史数据、告警管理。

## 协议定位

本项目第一阶段以 **《中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0，2016 年 9 月》** 为核心。

B 接口核心特征：
- SC 与 FSU 之间通过 B 接口互联
- 数据流接口基于 **WebService + SOAP + XML**
- B 接口还包含 **FTP** 文件/图片能力
- SC 轮询 FSU 获取慢数据时 SC 是客户端，FSU 是服务端
- FSU 主动上报告警、状态切换等快数据时 FSU 是客户端，SC 是服务端

> 本项目不优先实现 DSC/RDS、UDP/TCP、MQTT 等非 B 接口能力。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |
| 后端 | Java + Spring Boot 3.2 + JPA |
| 数据库 | PostgreSQL 15 |
| 部署 | Docker / Docker Compose |

## 目录结构

```
dcim-platform/
├── backend/          Java Spring Boot 后端工程
├── frontend/         Vue 3 前端工程
├── database/         数据库脚本、迁移脚本、初始化数据
├── docs/             项目文档、架构说明、协议文档
├── deploy/           部署配置（Docker Compose）
├── tools/            工具目录（模拟器、协议测试）
└── README.md         本文件
```

## 本地启动

### 1. 启动 PostgreSQL

```bash
cd deploy
docker compose up -d postgres
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：8080
Swagger 地址：http://localhost:8080/swagger-ui.html

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认端口：5173
访问地址：http://localhost:5173

## 当前阶段

当前为 **INIT-001**：基础工程初始化阶段。

已完成：
- [x] 项目顶层目录结构
- [x] Spring Boot 后端工程（启动类、统一响应、全局异常处理、健康检查、Swagger）
- [x] B接口模块骨架（SCService、FSUService、命令分发、报文日志、FTP、WSDL）
- [x] Vue 3 前端工程（路由、布局、占位页面、Axios 封装）
- [x] PostgreSQL docker-compose
- [x] 项目文档

## 后续开发计划

### INIT 系列（工程基础）
- INIT-002：数据库核心模型设计
- INIT-003：后端基础业务模块骨架
- INIT-004：前端页面骨架增强
- INIT-005：模拟采集闭环

### BIF-P0 系列（B接口协议基础）
- BIF-P0-001：通读B接口2016协议并提取命令清单
- BIF-P0-002：整理SCService/FSUService WSDL骨架
- BIF-P0-003：整理Request/Response/PK_Type/Info/xmlData基础XML模型
- BIF-P0-004：整理B接口命令码映射表
- BIF-P0-005：整理SOAP报文日志规范

### BIF-P1 系列（B接口命令处理）
- BIF-P1-001：实现SCService接收骨架
- BIF-P1-002：实现LOGIN占位流程
- BIF-P1-003：实现HEARTBEAT占位流程
- BIF-P1-004：实现SEND_ALARM占位流程
- BIF-P1-005：实现GET_DATA调用骨架
