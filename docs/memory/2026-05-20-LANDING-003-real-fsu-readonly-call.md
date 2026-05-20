# LANDING-003 真实 FSU 只读联调采集

## 任务目标
向真实 FSU (192.168.100.100:8080/services/FSUService) 执行只读联调，采集原始 SOAP 报文，生成候选点位表和协议对照分析报告。

## 架构判断
- 涉及模块: B接口 FSU 通信层 (RealHttpFsuServiceClient, FsuServiceRpcAdapter)
- 涉及服务: GetLoginInfoService, GetSuInfoService, GetSpConfigOptionService, GetActiveAlarmService, GetDataService, GetThresholdService
- 不依赖 Spring 容器，手动 wiring
- 不影响现有业务代码

## 协议一致性判断
- 请求使用 2024 Name+Code 格式 (GET_SUINFO=1001, GET_SPCONFIGOPTION=401, GET_ACTIVEALARM=603, GET_DATA=501, GET_THRESHOLD=505) 和旧格式 (GET_LOGININFO)
- FSU 返回标准 WSDL RPC/encoded SOAP 响应 (namespace: http://FSUService.chinatowercom.com)
- **协议差异**: FSU 对所有 GET_* 命令返回空 `<invokeReturn/>`，可能仅支持 B-2016 或仅配置为上报模式
- **GET_DATA 异常**: FSU 对 GET_DATA 请求返回了 SEND_ALARM 结构，且响应中包含嵌套 `<?xml?>` 声明导致解析失败

## 修改前论证
- 仅需创建测试类，不修改业务代码
- 采用与 BifP4005RealFsuIntegrationTest 相同的手动 wiring 模式
- 需创建 CaptureFsuServiceClient 包装器捕获中间 XML

## 写入前验证
- CaptureFsuServiceClient 复制 RealHttpFsuServiceClient.call() 逻辑，保持调用链一致
- 所有安全门禁保持默认关闭 (real-call-enabled=false, scheduler-enabled=false)
- 不通过 Spring 容器，不触发 Scheduler

## 实际修改文件
- **新增**: `backend/src/test/java/com/dcim/platform/binterface/Landing003RealFsuIntegration.java` — 集成采集类 (main + @Test)
- **新增**: `docs/landing/REAL-FSU-READONLY-CALL-ANALYSIS.md` — 完整分析报告
- **新增**: `docs/landing/raw-samples/*.xml` (24 个文件) — 原始 request/response 报文
- **新增**: `docs/landing/candidate-points/candidate-devices.csv` — 候选设备表 (空)
- **新增**: `docs/landing/candidate-points/candidate-signals.csv` — 候选信号表 (空)

## 核心改动
1. 创建了 CaptureFsuServiceClient (FsuServiceClient 实现)，在标准调用链中插入 XML 捕获点
2. 采集了 6 个只读命令的完整四层 XML (request-payload / rpc-request / rpc-response / doc-response)
3. 确认 FSU 在线 (192.168.100.100:8080 正常响应 HTTP 200)
4. 发现协议不匹配: FSU 返回空 `<invokeReturn/>` 对所有 GET_* 命令
5. 发现 GET_DATA → SEND_ALARM 路由异常 + 嵌套 XML 声明 bug

## 测试命令和结果
```
mvn test -Dtest=Landing003RealFsuIntegration -DfailIfNoTests=false
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```
- LOGIN: HTTP 200, 空响应
- GET_SUINFO: HTTP 200, 空响应
- GET_SPCONFIGOPTION: HTTP 200, 空响应
- GET_ACTIVEALARM: HTTP 200, 空响应 (0 alarms)
- GET_DATA: HTTP 200, SEND_ALARM 回传, XML 解析失败
- GET_THRESHOLD: HTTP 200, 空响应

## 风险
- **协议不匹配风险**: FSU 不支持当前代码使用的 GET_* PK_Type 值，后续需管理方确认后调整
- **XML 解析健壮性**: FsuServiceRpcAdapter 未处理嵌套 XML 声明，GET_DATA 场景会抛异常
- **SUID 未知**: 使用临时 FSU-001，影响后续命令正确性

## 遗留问题
1. FSU 不支持 GET_* 主动查询 — 需确认固件能力
2. GET_DATA 响应包含嵌套 `<?xml?>` 声明 — FsuServiceRpcAdapter 需适配
3. SUID/DeviceID/SPID 全部未获取到 — 无法生成候选点位表
4. 未确认 FSU 是否需预登录会话

## 下一步建议
1. 向管理方确认 FSU 固件类型、协议版本 (B-2016/B-2024)、SUID
2. 如 FSU 仅支持 B-2016，切换为旧版 PK_Type 格式重试
3. 修复 FsuServiceRpcAdapter 对嵌套 XML 声明的处理
4. 提交 Codex 集中复审

## git diff 摘要
无 (未纳入 git 管理，项目在桌面目录)

## git status 摘要
无
