# LANDING-005 带真实 FSUID 的只读 GET_* 重试

## 任务目标
使用管理方确认的真实 FSUID `51051243812345` (StationName=1) 替换临时 fsuCode `FSU-001`，重试 5 个只读 GET_* 命令，判断 FSUID 是否为空响应根因。

## 架构判断
- 涉及: Landing003RealFsuIntegration 测试类 (FSUID 常量更新)
- 不涉及: 业务代码、通信层、Scheduler、SET 体系

## 协议一致性判断
- 使用管理方确认的 FSUID 构造 B接口 Info 字段
- structured/legacy 双格式对比
- 遵循 B接口 2016/2024 命令规范

## 修改前论证
- 仅修改测试类常量值 (FSUID = 51051243812345)
- 不改动通信逻辑、解析逻辑、安全门禁
- 保存到新前缀 landing005-*，不覆盖 LANDING-003/004 样本

## 实际修改文件
- `Landing003RealFsuIntegration.java`: FSU_CODE → FSUID=51051243812345, 保存前缀 landing005-*
- `docs/landing/raw-samples/landing005-*`: 新增 40 个 XML 报文
- `docs/audit/LANDING-005-*.md`: 新增审计报告
- `docs/memory/2026-05-20-LANDING-005-*.md`: 本文件

## 核心结论
**FSUID 不是空响应的根因。** 使用真实 FSUID `51051243812345` 后，FSU 响应与使用临时 `FSU-001` 时完全一致:
- GET_DATA structured: SEND_ALARM (逐字节相同)
- 所有其他命令: 空 invokeReturn (完全相同)

FSU 不校验请求中的 SUID/FSUCode，不支持 GET_* 主动查询。

## 测试命令和结果
```
mvn test -Dtest='Landing003RealFsuIntegration' -DrealFsuTest.enabled=true → BUILD SUCCESS
10 次真实调用 (5命令×2格式), 全部 HTTP 200
```

## 风险
- FSU 不支持 SC 主动查询，如需数据需改为被动接收 SEND_* 上报
- 平台需适配 FSU Code 分配表 (501→SEND_ALARM 非 GET_DATA)

## 遗留问题
1. FSU 不支持 GET_* 出站查询（确认，非 FSUID 问题）
2. FSU Code 分配表与 2024 标准不一致
3. 需确认 FSU 固件版本和协议支持范围
