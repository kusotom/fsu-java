# INIT-001 验收标准

## 目录验收

- [x] dcim-platform/backend/
- [x] dcim-platform/frontend/
- [x] dcim-platform/database/
- [x] dcim-platform/docs/
- [x] dcim-platform/deploy/
- [x] dcim-platform/tools/
- [x] dcim-platform/README.md

## 后端验收

- [ ] backend/ 是标准 Spring Boot 工程
- [ ] mvn spring-boot:run 可以启动
- [ ] GET /api/health 可以返回成功响应
- [ ] GET /api/b-interface/health 可以返回成功响应
- [ ] Swagger 页面可访问
- [ ] application-dev.yml 中存在 PostgreSQL 配置
- [ ] 存在统一 ApiResponse
- [ ] 存在全局异常处理
- [ ] 存在 binterface 模块
- [ ] 存在 SOAP/XML/WSDL/SCService/FSUService/FTP 占位目录或类

## 前端验收

- [x] frontend/ 是标准 Vue 3 + Vite + TypeScript 工程
- [x] npm install 可以安装依赖 (93 packages, Node v24)
- [x] npm run build 构建成功 (vue-tsc type-check + vite build)
- [ ] npm run dev 可以启动 (未执行长时间运行，build 已验证)
- [x] 页面能打开 (构建产物含全部 15 个页面)
- [x] 左侧菜单存在 (BasicLayout.vue)
- [x] 所有规划路由存在 (15 条路由)
- [x] 每个页面有占位内容
- [x] Axios 基础封装存在 (request.ts)
- [x] B接口管理菜单存在
- [x] B接口总览、FSU注册状态、B接口报文日志、协议命令覆盖矩阵等页面存在

> 注：vue-tsc 已从 ^1.8.0 升级到 ^2.2.8 以兼容 Node v24（INIT-001-FIX-001）

## 部署验收

- [ ] deploy/docker-compose.yml 存在
- [ ] docker compose up -d postgres 可以启动 PostgreSQL
- [ ] PostgreSQL 数据库名为 dcim_platform
- [ ] 用户名为 dcim
- [ ] 密码为 dcim123456

## 文档验收

- [x] README.md 完整
- [x] docs/tasks/INIT-001-foundation.md 存在
- [x] docs/acceptance/INIT-001-acceptance.md 存在
- [x] docs/protocol/b-interface-2016-summary.md 存在
- [x] docs/protocol/b-interface-command-map.md 存在
- [x] 文档明确说明当前只是基础工程，不实现具体协议
- [x] 文档明确说明本项目第一阶段以 B接口 2016 为核心
