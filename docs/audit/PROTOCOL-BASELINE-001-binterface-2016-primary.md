# 审计文档：PROTOCOL-BASELINE-001 B接口2016 确认为主开发依据

## 1. 审计日期
2026-05-21

## 2. 审计类型
协议基线调整（纯文档，零代码修改）

## 3. 决策

将项目协议基线调整为：**B接口2016 = 当前主开发依据，B接口2024 = 未来升级兼容层**。

## 4. 真实 FSU 证据

| 证据 | 值 |
|------|-----|
| 设备 | FSUID=51051243812345, IP=192.168.100.100 |
| LOGIN Code | **101** (2016) |
| GET_DATA Code | **401** → ACK 402 (2016) |
| GET_LOGININFO Code | **1501** → ACK 1502 (2016) |
| GET_FSUINFO Code | **1701** → ACK 1702 (2016) |
| GET_FTP Code | **1601** → ACK 1602 (2016) |
| SEND_ALARM Code | **501** (2016, 2024=601) |
| 被动上报 | 仅 LOGIN 观察到，无 HEARTBEAT/SEND_DATA |

## 5. 修改文件

| 文件 | 操作 |
|------|------|
| `docs/rules/CLAUDE_PROJECT_RULES.md` | +§2.1 协议基线规则 |
| `docs/memory/WORKING-MEMORY.md` | +协议基线节 + 后续路线调整 |
| `docs/memory/README.md` | 索引 |
| `openspec/project.md` | 协议定位更新 |

## 6. 不修改范围

Java 代码 0 修改。2024 实现保留不删除。

## 7. 结论

通过。协议基线已按真实 FSU 验证结果调整。
