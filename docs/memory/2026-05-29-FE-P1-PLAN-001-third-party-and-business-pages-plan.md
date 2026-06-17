# FE-P1-PLAN-001: 三方授权与业务页面 P1 规划

## 任务目标
在 P0 权限闭环基础上，规划三方授权产品化和业务页面 P1 建设路径。纯规划，不直接改代码。

## 架构判断
不涉及 B接口协议。新增 6 页面、6 后端 API、3 新 Entity。

## 规划内容
- P1A (优先): TENANT-001 (租户管理) → SCOPE-001 (站点/FSU授权) → AUDIT-001 (审计查询)
- P1B (次优先): MAPPING-001 (点位映射) → FSU-DETAIL-001 (FSU详情增强) + ALARM-001 (告警闭环并行)

## 后端补充
3 新 Entity (Tenant, SignalMapping, 增强 AuditLog), 6 新 Controller (Tenant/Scope/AuditLog/Mapping/AlarmOperation), 5 新权限点 (tenant:view/manage, fsu:manage, alarm:confirm/clear)

## 安全边界
不访问真实FSU、不执行SET、不启Scheduler、不改B接口2016主线。

## 下一步
P0 修复完成后启动 P1A → TENANT-001。
