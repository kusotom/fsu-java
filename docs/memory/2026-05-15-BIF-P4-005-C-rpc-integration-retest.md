---
name: bif-p4-005-c-rpc-integration-retest
description: BIF-P4-005-C RPC adapter 真实 FSU 重新联调 — SOAP Fault 消除, GET_DATA/GET_THRESHOLD 返回 ResultCode=0
metadata:
  type: project
---

## 任务编号
BIF-P4-005-C

## 任务名称
RPC Adapter 真实 FSU 重新联调

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md
- [x] real-call-enabled=false (联调前), 联调后已恢复 false
- [x] 仅执行白名单 5 命令
- [x] 未执行 SET 类命令
- [x] 未启用 Scheduler

## 联调目标
重新联调真实 FSU 192.168.100.100:8080，验证 FsuServiceRpcAdapter 生成的 WSDL RPC 格式被 FSU 接受。

## 联调结果

| 命令 | HTTP | SOAP Fault | ResultCode | 说明 |
|------|------|-----------|------------|------|
| GET_LOGININFO | 200 | **无** | - | 响应无数据 (FSUCode 未注册) |
| TIME_CHECK | 200 | **无** | - | 响应缺少 FSUTime |
| GET_DATA | 200 | **无** | **0** ✅ | 成功，但信号为空 (信号ID不存在) |
| GET_THRESHOLD | 200 | **无** | **0** ✅ | 成功，但门限为空 (信号ID不存在) |
| GET_FTP | 200 | **无** | - | 响应无数据 |

### 关键对比

| 指标 | 旧格式 (document-style) | 新格式 (RPC adapter) |
|------|------------------------|---------------------|
| HTTP Status | 500 | **200** |
| SOAP Fault | `Method 'Request' not implemented` | **无** |
| GET_DATA | 无法解析 | **ResultCode=0** |
| GET_THRESHOLD | 无法解析 | **ResultCode=0** |
| 测试结果 | 5/5 (但全部因 Fault 失败) | **5/5 (2 命令明确成功)** |

### RPC Adapter 验证通过

WSDL RPC 封装正确：
- Body 第一层: `<ns1:invoke>` ✅
- namespace: `http://FSUService.chinatowercom.com` ✅
- xmlData 含 `xsi:type="SOAP-ENC:string"` ✅
- SOAPAction: `""` ✅
- FSU 接受并返回有效 SOAP 响应 ✅

### 遗留问题

1. **FSUCode 51051243812345 未在 FSU 注册** → GET_LOGININFO 返回空
2. **信号ID TEMP-R01/HUMI-R01 不在 FSU 配置中** → GET_DATA/GET_THRESHOLD 返回空数据
3. **TIME_CHECK 响应格式差异** → 缺少预期 FSUTime 字段
4. **GET_FTP 响应格式差异** → 缺少预期 FTPConfig

### 测试命令
```bash
mvn test -Dtest="BifP4005RealFsuIntegrationTest"    # 联调
mvn test                                              # 全量回归 791 tests
```

### 测试结果
- 联调: 5/5 通过 (0 failures, 0 errors)
- 回归: 791 tests, 0 failures, 1 pre-existing Error

### 是否修改业务代码
否。本次仅运行已有测试。

### 是否访问真实设备
是。仅向 192.168.100.100:8080 发送 5 个只读白名单命令。

### 是否启用真实 FSU 调用
是。临时启用 real-call-enabled=true，已完成并恢复 false。

### 是否启用 Scheduler
否。

### 是否执行 SET 类命令
否。

### 是否写入真实凭证
否。

### 建议下一步
BIF-P4-006：与 FSU 管理方协调，确认 FSUCode 注册和信号ID配置，完成完整联调闭环。
