# BIF-P0-001：命令码映射补全

> 对应阶段：INIT-003 → BIF-P0-001
> 完成日期：2026-05-13
> 影响范围：BInterfacePkType 枚举 + docs + tests

---

## 修复内容

### 1. BInterfacePkType 新增枚举值

| 新增枚举 | 方向 | 说明 | fixture 依赖 |
|---------|------|------|-------------|
| SEND_DATA | FSU→SC | FSU主动上报实时采集数据（快数据通道） | soap/sc_service/send_data.*, xmldata/send_data.*, expected/parsed_send_data.* |
| SET_FTP | SC→FSU | SC设置FTP参数（慢数据通道） | soap/fsu_service/set_ftp.*, xmldata/set_ftp.* |

### 2. 放置位置

```
原顺序: LOGIN, HEARTBEAT, GET_DATA, SEND_ALARM, ...
新顺序: LOGIN, HEARTBEAT, GET_DATA, SEND_DATA, SEND_ALARM, ...
                                        ↑ 新插入（按 FSU→SC 分组）
...
          TIME_CHECK, GET_FTP, SET_FTP, GET_LOGININFO, ...
                                  ↑ 新插入（紧接 GET_FTP）
```

- SEND_DATA 放在 GET_DATA 之后、SEND_ALARM 之前（FSU→SC 主动上报组）
- SET_FTP 放在 GET_FTP 之后（FTP 配置组）

### 3. SEND_DATA vs GET_DATA 区分

| 维度 | SEND_DATA | GET_DATA |
|------|-----------|----------|
| 方向 | FSU→SC | SC→FSU |
| 通道 | 快数据（主动上报） | 慢数据（SC轮询） |
| 触发 | FSU 按采集周期上报 | SC 发起请求 |
| Info 字段 | FSUCode, SessionID, CollectTime | FSUCode, SessionID, RequestTime |
| xmlData | Signal[]（值+质量+状态） | SignalID[]（查询列表）或 Signal[]（响应） |

### 4. Fixtures 命令码覆盖检查

| PK_Type | 枚举 | SOAP Req | SOAP Res | xmlData | Expected | Notes |
|---------|:---:|:--------:|:--------:|:------:|:--------:|-------|
| LOGIN | ✅ | ✅ | ✅ | ✅ | ✅ | |
| HEARTBEAT | ✅ | ✅ | ✅ | ✅ | ✅ | |
| SEND_DATA | ✅ 新增 | ✅ | ✅ | ✅ | ✅ | 之前缺失 |
| SEND_ALARM | ✅ | ✅ | ✅ | ✅ | ✅ | |
| GET_DATA | ✅ | ✅ | ✅ | ✅ | ✅ | |
| GET_HISTORY_DATA | ✅ | ❌ | ❌ | ❌ | ❌ | 暂未创建 fixture |
| SET_POINT | ✅ | ✅ | ✅ | ✅ | ✅ | |
| GET_THRESHOLD | ✅ | ✅ | ✅ | ✅ | ✅ | |
| SET_THRESHOLD | ✅ | ✅ | ✅ | ✅ | ✅ | |
| TIME_CHECK | ✅ | ✅ | ✅ | ✅ | ✅ | |
| GET_FTP | ✅ | ✅ | ✅ | ✅ | ✅ | |
| SET_FTP | ✅ 新增 | ✅ | ✅ | ✅ | ❌ | 缺少 expected JSON |
| GET_LOGININFO | ✅ | ✅ | ✅ | ✅ | ❌ | 缺少 expected JSON |
| GET_FSUINFO | ✅ | ❌ | ❌ | ❌ | ❌ | 暂未创建 fixture |
| SET_DATA | ✅ | ❌ | ❌ | ❌ | ❌ | 暂未创建 fixture |
| SET_FSUREBOOT | ✅ | ❌ | ❌ | ❌ | ❌ | 安全禁用，暂不创建 fixture |
| UNKNOWN | ✅ | — | — | — | — | invalid/unknown_msg_type 使用 UNKNOWN_COMMAND_99 |
| UNKNOWN_COMMAND_99 | → UNKNOWN | ✅（invalid） | — | — | — | invalid fixture，映射到 UNKNOWN |

> ✅ = 已覆盖  ❌ = 未覆盖

**结论**：所有 fixture 中出现的 PK_Type 均可被枚举覆盖。

### 5. 重复定义检查

枚举中无重复值。UNKNOWN 作为兜底值，无其他枚举语义上等用于 UNKNOWN。

---

## 改动文件清单

| 文件 | 操作 | 类型 |
|------|------|------|
| `backend/src/main/java/.../model/BInterfacePkType.java` | 修改 | 代码 |
| `docs/protocol/b-interface-command-map.md` | 修改 | 文档 |
| `backend/src/test/java/.../binterface/BInterfacePkTypeTest.java` | 新增 | 测试 |
| `docs/audit/BIF-P0-001-command-mapping.md` | 新增 | 审计 |

---

## 遗留问题

1. **数据库种子数据**：`database/seed/001_seed_demo_data.sql` 中 `b_interface_command` 表不含 SEND_DATA 和 SET_FTP 记录。属于数据库初始化数据，非本次范围，建议 BIF-P0-数据库阶段补充。
2. **Expected JSON 缺失**：SET_FTP（req+res）和 GET_LOGININFO（req+res）的 expected JSON 尚未创建，建议 BIF-P0-003（SOAP 解析实现）阶段一并补全。
3. **CommandDispatcher** 当前返回 null，枚举补全后该 class 不需要修改（只做 dispatch 逻辑），待 BIF-P0-004 实现。
4. **SET_FTP** 协议原文确认：fixtures 中使用 SET_FTP 是合理的推断命名（GET_FTP → SET_FTP 对称），建议 BIF-P0-002 阶段对照协议原文验证。

---

## 建议下一步

BIF-P0-002：SOAP/WSDL 入口完整性修复 — 实现真实 WSDL 输出，为后续 SOAP 解析打基础。
