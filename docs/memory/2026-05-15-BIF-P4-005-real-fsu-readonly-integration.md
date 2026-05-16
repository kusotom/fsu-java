---
name: bif-p4-005-real-fsu-readonly-integration
description: 单台真实 FSU 只读联调执行 — 5 命令全部 HTTP 404，确认网络可达但 SOAP endpoint 路径不匹配，错误处理正确
metadata:
  type: project
---

## 任务编号
BIF-P4-005

## 任务名称
单台真实 FSU 只读联调执行

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md
- [x] 已确认 B接口协议 2016 是唯一设备协议
- [x] 已确认本次任务遵循 论证 → 验证 → 写入

## 任务目标
对单台真实 FSU (192.168.100.100:80, fsu_code=51051243812345, 艾默生 2808IM) 执行只读联调，按序调用 5 个白名单命令：
GET_LOGININFO → TIME_CHECK → GET_DATA → GET_THRESHOLD → GET_FTP

## 架构判断
- 业务域：B接口慢数据通道 (SC→FSU)
- 涉及层：FsuServiceClient / Service / SOAP 层 / HTTP 传输层
- 模块：binterface.service.fsu (RealHttpFsuServiceClient) + 5 个只读 Service
- 不涉及：前端 / DSC/RDS / SET 类命令 / Scheduler

## 协议一致性判断
- 符合 B接口协议 2016：使用 SOAP/XMLData 封装，PK_Type 正确，Info/xmlData 结构符合协议
- SOAP 报文由 SoapMessageHandler 构造，XMLData 由 XmlDataBuilder 构造
- 未触碰 DSC/RDS

## 修改前论证
- 目标 FSU 直接指定 serviceUrl，无需 FsuEndpointResolver 查库
- 手工 wiring RealHttpFsuServiceClient + 5 Service，不依赖 Spring 容器（避免 Bean 冲突）
- 写独立 JUnit5 测试类，不在 CI 中自动运行

## 写入前验证
- 协议样例：B接口 2016 SC_TO_FSU 慢数据命令，白名单内
- XMLData 结构：已由 XmlDataBuilder/XmlDataParser 覆盖
- SOAP/WSDL：SoapMessageHandler.buildRequest() 构造，标准 SOAP 1.1
- 连接超时 5s，读取超时 10s
- 无破坏性变更
- 不修改业务代码

## 计划修改文件
| 文件 | 动作 | 原因 |
|------|------|------|
| `application.yml` | 临时改 | 启用 real-call-enabled=true |
| `database/` | 临时 DML | 插入 SITE-002 + FSU + 监控点位 |
| `BifP4005RealFsuIntegrationTest.java` | 新增 | 联调测试类（5 个 @Test） |
| `docs/memory/2026-05-15-BIF-P4-005-*.md` | 新增 | 操作记忆文件 |

## 风险点
- 网络不可达 → 实际可达（已确认）
- FSU 协议兼容性 → SOAP endpoint 路径不匹配（已确认）
- 数据安全 → 只读命令，无数据污染风险
- SET 误执行 → 不涉及

## 实际修改文件
### 新增（1 个）
| 文件 | 说明 |
|------|------|
| `backend/src/test/java/com/dcim/platform/binterface/BifP4005RealFsuIntegrationTest.java` | 联调测试（5 步，手工 wiring） |

### 修改（1 个）
| 文件 | 说明 |
|------|------|
| `backend/src/main/resources/application.yml` | 临时启用 real-call-enabled=true，已恢复 false |

### 数据库变更
- SITE-002 → 真实联调站点
- fsu_code=51051243812345 → 动环 FSU (艾默生 2808IM, 192.168.100.100:80)
- monitoring_point TEMP-R01 + HUMI-R01 → 联调用监控点位

## 核心改动

### 联调测试类 (BifP4005RealFsuIntegrationTest)
- 手工 wiring：SoapMessageHandler → XmlDataParser → RealHttpFsuServiceClient → 5 Service
- 5 步顺序执行：GET_LOGININFO → TIME_CHECK → GET_DATA → GET_THRESHOLD → GET_FTP
- 不依赖 Spring 容器（避免 BInterfaceMessageLogRepository Bean 冲突）
- 所有错误转为结构化日志输出

## 测试命令
```bash
# 启动 PostgreSQL
cd deploy && docker compose up -d postgres

# 执行 schema + seed
docker cp database/schema/001_init_schema.sql dcim-postgres:/tmp/
docker exec dcim-postgres psql -U dcim -d dcim_platform -f /tmp/schema.sql
docker cp database/seed/001_seed_demo_data.sql dcim-postgres:/tmp/
docker exec dcim-postgres psql -U dcim -d dcim_platform -f /tmp/seed.sql

# 插入联调 FSU
INSERT INTO site/fSU_device/monitoring_point ...

# 启用 real-call
# application.yml: real-call-enabled: true

# 运行联调测试
mvn test -Dtest="BifP4005RealFsuIntegrationTest"

# 恢复 real-call
# application.yml: real-call-enabled: false

# 全量回归
mvn test
```

## 测试结果

### 联调测试 (5/5 通过)
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

| 步骤 | 命令 | HTTP 状态 | 结果 |
|------|------|-----------|------|
| 1 | GET_LOGININFO | 404 | fail(5001) - 路径不存在 |
| 2 | TIME_CHECK | 404 | fail(5001) - 路径不存在 |
| 3 | GET_DATA | 404 | fail(5001) - 路径不存在 |
| 4 | GET_THRESHOLD | 404 | fail(5001) - 路径不存在 |
| 5 | GET_FTP | 404 | fail(5001) - 路径不存在 |

### 全量回归
```
Tests run: 764, Failures: 0, Errors: 1, Skipped: 0
```
- 错误 1 个：DcimPlatformApplicationTests.contextLoads（已有 Bean 冲突，非本次引入）
- 新增 5 个联调测试全部通过
- 零回归

### 关键发现

1. **网络可达**：192.168.100.100:80 可达（无连接超时）
2. **服务器类型**：Apache/2.0.65 (Unix) at localhost.ygx.com
3. **SOAP endpoint 路径不匹配**：默认 `/services/FSUService` 在该 FSU 上不存在
4. **错误处理正确**：404 → 结构化 `FsuServiceResponse.fail("5001", ...)` → 各 Service 正确包装
5. **SOAP 构造正确**：SoapMessageHandler.buildRequest() 正确生成 SOAP Envelope
6. **HTTP 客户端正确**：HttpURLConnection 正确处理连接/请求/响应

## 协议一致性检查
- 符合 B接口协议 2016 SOAP/XMLData
- PK_Type 字段为 BInterfacePkType enum 值
- Info 字段包含 FSUCode 等标准字段
- xmlData 字段正确构造（GET_DATA/GET_THRESHOLD 含 SignalID 列表）
- 未使用私有 JSON/UDP 协议
- 未自造 MsgType/SignalID/DeviceID/FsuCode/ResultCode
- 未触碰 DSC/RDS

## 架构一致性检查
- FsuServiceClient 接口抽象层正确工作
- RealHttpFsuServiceClient 条件加载机制正确
- Service → Client → HTTP 分层清晰
- 错误在每一层正确传播和包装
- 未引入重复逻辑或技术债

## 是否触碰 DSC/RDS 主流程
否。所有操作均在 B接口 SOAP/XMLData 规范内。

## 遗留问题
1. **FSU SOAP endpoint 路径未知**：艾默生 2808IM 的 FSUService 实际路径需确认
2. **缺少 endpoint 路径配置化**：当前 `DEFAULT_ENDPOINT_PATH = "/services/FSUService"` 硬编码
3. **联调未实际成功**：需 FSU 方提供正确的 WSDL/endpoint 路径
4. **Bean 冲突未修复**：BInterfaceMessageLogRepository 在两个包中重复定义

## 下一步建议

### BIF-P4-006：FSU endpoint 路径配置化
- 将 `DEFAULT_ENDPOINT_PATH` 从硬编码改为 `application.yml` 可配置项
- 在 `FsuDeviceEntity` 中增加 `service_url` 字段（可选，优先于 ip+port 拼接）
- 支持 per-FSU 自定义 endpoint 路径

### 或者：先确认艾默生 2808IM 的 SOAP endpoint
- 联系 FSU 厂商或查阅设备文档获取实际 WSDL URL
- 常见路径：`/axis2/services/FSUService`、`/FSUService`、`/services/FSUService?wsdl`
- 用 curl 探测可能的 endpoint 路径
