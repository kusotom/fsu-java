# 10 — FSU 初始化能力

## 1. 协议定位

FSU 上电/重启后的初始化流程，包括注册、获取配置、建立会话。

## 2. 初始化流程

```
FSU 上电
  → LOGIN (FSU→SC)
    → SC 返回 SessionID + ExpireSeconds
  → GET_LOGININFO (SC→FSU, 可选)
    → 获取 FSU 注册信息
  → 进入正常工作状态
```

## 3. 相关命令

| 命令 | Code | 用途 |
|------|------|------|
| LOGIN | 101 | FSU 注册/登录 |
| GET_LOGININFO | 1501 | SC 获取 FSU 登录信息 |

## 4. 当前项目实现

| 能力 | 状态 |
|------|------|
| LOGIN 接收 | ✅ `LoginCommandHandler` → `LoginService.login()` |
| SessionID 生成 | ✅ `SESSION-{fsuCode}-{uuid}` |
| fsu_status 维护 | ✅ `BInterfaceFsuStatusEntity` |
| GET_LOGININFO | ✅ LANDING-006 验证通过 |

## 5. 初始化约束

- LOGIN 必须在其他命令之前完成
- 所有后续命令（除 LOGIN）需携带有效 SessionID
- 会话超时后 FSU 需重新 LOGIN
