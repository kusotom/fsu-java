# 审计文档：LANDING-015 B接口2016 GET_FSUINFO 心跳轮询

## 1. 审计日期
2026-05-21

## 2. 审计类型
新增 2016 协议能力（零破坏性变更）

## 3. 实现

新增 `BInterface2016GetFsuInfoService`:
- 使用 GET_FSUINFO (Code=1701) + pkTypeFormat="legacy-2016"
- 严格校验 ACK Code=1702 后才更新状态
- CPU/MEM 写入 statusDetail（不新增表字段）
- last_heartbeat 和 online_status 更新

## 4. 修改文件

新增: BInterface2016GetFsuInfoService, BInterface2016GetFsuInfoResult, BInterface2016GetFsuInfoServiceTest (11 tests)

现有代码: 0 修改。2024 GetSuInfoService 保留不变。

## 5. 安全边界

SET/Scheduler: 不涉及。真实 FSU: 测试使用 stub。

## 6. 测试

1215 tests, 0/0/5.

## 7. 现场验证

通过测试类 run-once（需显式启用 realFsuTest）:
```bash
mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true
```
