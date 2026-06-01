# LANDING-015 Plan

## 1. 代码现状

- BInterfaceCommand2016: GET_FSUINFO=1701 ✅
- GetSuInfoService: 2024 GET_SUINFO (Code=1001), 可复用解析逻辑
- RealHttpFsuServiceClient: 支持 legacy-2016 pkTypeFormat ✅
- BInterfaceFsuStatusEntity: lastHeartbeat/onlineStatus ✅
- BInterfaceFsuStatusRepository: findByFsuCode ✅

## 2. 实现

新增 BInterface2016GetFsuInfoService:
- execute(fsuCode, fsuServiceUrl)
- 构造 FsuServiceRequest(pkType=GET_FSUINFO, pkTypeFormat="legacy-2016")
- 调用 fsuServiceClient.call()
- 解析 CPUUsage/MEMUsage (复用 GetSuInfoService 逻辑)
- 更新 fsuStatus.lastHeartbeat + onlineStatus=ONLINE

## 3. 测试

BInterface2016GetFsuInfoServiceTest:
- 使用 2016 Code=1701
- 解析 TFSUStatus/CPUUsage/MEMUsage
- 成功后更新 lastHeartbeat
- 失败不更新

## 4. 不新增 REST 端点

本次仅实现 Service + 测试。run-once 通过测试或 Landing015RealFsuIntegration test 验证。
