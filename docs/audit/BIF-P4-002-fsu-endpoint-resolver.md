# BIF-P4-002：FSU 地址发现 / ServiceUrl 管理

> 对应阶段：BIF-P4-002
> 完成日期：2026-05-15
> 影响范围：新增 FsuEndpointResolver/FsuEndpointResult/FsuServiceEndpointService，修改 6 个 Service 层注入

---

## 一、本阶段目标

补齐真实 FSU 联调前的关键前置能力：FSU 地址发现与 ServiceUrl 管理。基于 FsuDeviceEntity 已有的 ipAddr/port 字段，构建 endpoint 解析层，使 6 个慢数据 Service 在未显式传入 serviceUrl 时能从数据库自动解析。

## 二、架构变更

### 新增
```
FsuEndpointResolver (接口)
  └── FsuServiceEndpointService (实现)
       └── FsuDeviceRepository → FsuDeviceEntity.ipAddr + port
       └── 默认路径: /services/FSUService
       └── URL 格式校验

FsuEndpointResult (模型)
  ├── fromHostPort()     — 由 ip+port 解析成功
  ├── fromExplicitServiceUrl() — 由显式 serviceUrl 解析成功（预留）
  └── fail()             — 解析失败
```

### 修改
```
GetDataService          → +FsuEndpointResolver 注入 + 自动解析
GetThresholdService     → +FsuEndpointResolver 注入 + 自动解析
TimeCheckService        → +FsuEndpointResolver 注入 + 自动解析
GetLoginInfoService     → +FsuEndpointResolver 注入 + 自动解析
GetFtpService           → +FsuEndpointResolver 注入 + 自动解析
SetThresholdService     → +FsuEndpointResolver 注入 + 自动解析
```

## 三、解析优先级

| 优先级 | 来源 | 状态 |
|--------|------|------|
| 1 | 显式传入的 fsuServiceUrl | 优先使用，不解析 |
| 2 | FsuDeviceEntity.ipAddr + port + 默认 path | 当前实现 |
| 3 | 预留的实体显式 serviceUrl 字段 | 未来扩展 |
| 错误 | 设备未找到/IP缺失/端口缺失 | 返回结构化错误 |

## 四、默认配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| DEFAULT_ENDPOINT_PATH | `/services/FSUService` | SOAP 服务路径，联调时可调 |

## 五、错误码

| ResultCode | 说明 | 场景 |
|-----------|------|------|
| 0 | 成功 | 正常解析 |
| 2001 | 缺少 FSUCode | fsuCode 为 null/空 |
| 2001 | FSU IP 地址未配置 | ipAddr 为 null/空 |
| 2001 | FSU 端口未配置 | port 为 null/<=0 |
| 2003 | FSU 未找到 | 数据库中无此编码 |
| 5001 | FSU 服务地址格式无效 | URL 构造后格式异常 |

## 六、测试覆盖

| 测试类别 | 测试数 | 状态 |
|---------|--------|------|
| FsuServiceEndpointServiceTest | 24 | ✅ 通过 |
| 全量 B-interface 测试 | 711 → 735 | ✅ 全部通过 |

## 七、安全边界

| 检查项 | 状态 | 说明 |
|--------|------|------|
| real-call-enabled 仍默认 false | ✅ | 未修改配置默认值 |
| resolver 仅在 null serviceUrl 时触发 | ✅ | 显式传入 URL 时不解析 |
| 不存储敏感字段 | ✅ | 仅使用 IP/Port |
| URL 格式校验 | ✅ | 仅 http/https 协议 |
| 不影响 Stub 模式 | ✅ | StubFsuServiceClient 仍为默认 |

## 八、遗留问题

1. /services/FSUService 路径需真实联调确认
2. FsuDeviceEntity 尚无显式 serviceUrl 字段（预留）
3. 未支持 https 的严格证书校验
