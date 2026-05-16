# BIF-P3-006：TIME_CHECK 时间同步闭环

> 对应阶段：BIF-P3-006
> 完成日期：2026-05-15
> 影响范围：新增 2 个生产文件、修改 2 个生产文件、新增 2 个测试文件

---

## 一、本阶段目标

基于已有 B接口基础设施，实现 TIME_CHECK（时间同步校验）完整业务闭环。将 `TimeCheckCommandHandler` 从桩实现替换为真实实现，新增 `TimeCheckService` 和 `TimeCheckResult`，扩展 `StubFsuServiceClient` 支持 TIME_CHECK 调用。

## 二、TIME_CHECK 协议

### 2.1 协议定义

> B接口协议 2016 定义：TIME_CHECK 用于 SC 与 FSU 之间的时间同步校验。SC 向 FSU 发送标准时间信息，FSU 返回自身系统时间。

### 2.2 请求结构

```xml
<Request>
  <PK_Type>TIME_CHECK</PK_Type>
  <Info>
    <FSUCode>FSU-001</FSUCode>
    <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
    <StandardTime>2026-05-13T10:42:00+08:00</StandardTime>
  </Info>
  <xmlData/>
</Request>
```

### 2.3 响应结构

```xml
<Response>
  <PK_Type>TIME_CHECK</PK_Type>
  <Info>
    <ResultCode>0</ResultCode>
    <FSUTime>2026-05-13T10:42:01+08:00</FSUTime>
  </Info>
  <xmlData/>
</Response>
```

### 2.4 协议特点

- 请求 Info 含：FSUCode, SessionID（可选）, StandardTime
- 响应 Info 含：ResultCode, FSUTime
- 请求和响应均无 xmlData 内容
- 属于只读查询，不修改 FSU 时间，不修改系统时间

## 三、架构

```
CommandDispatcher
    ↓
TimeCheckCommandHandler.handle(context)
    ├─ extract FSUCode + StandardTime from Info
    ├─ check login (LoginService.isLoggedIn)
    └─ TimeCheckService.execute(fsuCode, serviceUrl, standardTime)
         └─ FsuServiceClient.call(request)
              └─ StubFsuServiceClient.handleTimeCheck()
```

### 3.1 与 HEARTBEAT 模式对比

| 方面 | HEARTBEAT | TIME_CHECK |
|------|-----------|------------|
| 方向 | FSU→SC（上报） | SC→FSU（查询） |
| 需调 FSU | 否（仅更新本地状态） | 是（查 FSU 时间） |
| 响应包含 | ServerTime | FSUTime |
| 需 Service 层 | 否（Handler 直接调用 LoginService） | 是（TimeCheckService 调用 FSU） |

## 四、TimeCheckResult 模型

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 操作成功标志 |
| resultCode | String | B接口 ResultCode |
| resultDesc | String | 可读描述 |
| fsuCode | String | FSU 编码 |
| fsuTime | String | FSU 端时间（ISO8601） |
| standardTime | String | SC 端标准时间（ISO8601） |
| errors | List | 错误列表 |

工厂方法：`success(fsuCode, fsuTime, standardTime)`, `fail(resultCode, desc)`, `fail(resultCode, desc, fsuCode)`

## 五、TimeCheckService 职责

| 职责 | 说明 |
|------|------|
| 参数校验 | 空/无效 FSUCode → 2001，空 StandardTime → 2003 |
| 构造请求 | Info 含 FSUCode + StandardTime，无 xmlData |
| 调用 FSU | 通过 FsuServiceClient 发送 TIME_CHECK SOAP 请求 |
| 解析响应 | 从 Info 中正则提取 FSUTime |
| 异常安全 | 所有异常返回 fail，不抛出 |
| 不修改系统时间 | 严格遵守 |

## 六、TimeCheckCommandHandler 改造说明

### 6.1 注入变更

原：无注入
新：注入 `LoginService`（校验登录态）+ `TimeCheckService`（执行时间同步）

### 6.2 处理流程

```
原：notImplemented（桩）→ 返回 ResultCode=1

新：
  1. 提取 Info XML
  2. 正则提取 FSUCode
  3. 正则提取 StandardTime
  4. 校验 FSU 登录态
  5. 调用 TimeCheckService.execute(fsuCode, null, standardTime)
  6. 构造 CommandResult（Info: ResultCode + FSUTime）
```

### 6.3 响应 Info 格式

成功：`<ResultCode>0</ResultCode><FSUTime>2026-05-13T10:42:01+08:00</FSUTime>`
失败：`<ResultCode>N</ResultCode>`（N 为对应错误码）

## 七、StubFsuServiceClient 扩展

| 新增 | 说明 |
|------|------|
| `DEFAULT_TIME_CHECK_RESPONSE` | 硬编码 SOAP 响应（基于 fixture） |
| `handleTimeCheck()` | 解析并返回预设响应 |
| `extractFsuTime()` | 从 Info XML 提取 FSUTime（可复用于日志） |

## 八、不可为事项

- 不修改系统时间
- 不修改 FSU 时间
- 不访问真实设备（Stub 模式）
- 不集成到慢数据轮询框架
- 不涉及数据库
- 不新增配置项

## 九、测试覆盖情况

### 9.1 新增测试文件

| 测试文件 | 测试数 | 类型 |
|---------|--------|------|
| TimeCheckServiceTest | 20 | 单元测试（成功/参数校验/FSU失败/请求构造/extractFsuTime/边界） |
| TimeCheckCommandHandlerTest | 19 | 单元测试（成功/pkType/校验失败/服务错误/字段提取） |

### 9.2 测试场景覆盖

**TimeCheckServiceTest（20 测试）：**
- 成功返回 FSUTime
- 解析 FSUTime 正确值
- 传递 StandardTime
- null/空 FSUCode → 2001
- null/空 StandardTime → 2003
- FSU 调用失败 → 透传错误码
- FSU 调用异常 → 5001
- disabled 响应 → 5001
- 请求构造（PK_Type / FSUCode / StandardTime / 无 xmlData）
- extractFsuTime（正确值 / 大小写 / 空 / 空白 / 不存在）

**TimeCheckCommandHandlerTest（19 测试）：**
- getSupportedPkType → TIME_CHECK
- 成功处理
- 响应包含 FSUTime
- null context → 5001
- null soapMessage → 2001
- 缺少 FSUCode → 2001
- 缺少 StandardTime → 2003
- 未登录 → 1002
- Service 返回错误 → 透传
- Service 抛出异常 → 5001
- extractFsuCode（正确值 / 大小写 / 空）
- extractStandardTime（正确值 / 大小写 / 空白 / 空 / 不存在）

### 9.3 回归测试

| 测试范围 | 测试数 | 结果 |
|---------|--------|------|
| 全量测试（不含 DcimPlatformApplicationTests） | 610 | 全部通过 |

## 十、本阶段未处理内容

- 不集成到慢数据轮询（TIME_CHECK 非轮询类命令，属于按需调用）
- 不计算时间偏移量（仅返回 FSUTime，由调用方计算 offset）
- 不自动校准系统时间

## 十一、遗留风险

| 风险 | 等级 | 说明 |
|------|------|------|
| StubFSU 返回固定 FSUTime | 低 | Stub 模式，与真实 FSU 时间无关 |
| FSUTime 解析依赖正则 | 低 | 与 extractFsuCode/extractSessionId 模式一致 |
| 不验证 SessionID | 低 | 当前未在 INFO 中传 SessionID 到 FSU |

## 十二、下一步建议

**BIF-P4-001**：慢数据通道启用确认 — 评估产出、确认配置、考虑是否在开发环境启用轮询。
