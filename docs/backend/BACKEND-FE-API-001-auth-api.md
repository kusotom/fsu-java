# BACKEND-FE-API-001: Auth API 补齐

## 日期
2026-05-21

## 新增 API (11 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/sms/send-code | 发送短信验证码 |
| POST | /api/auth/sms/login | 短信验证码登录 |
| GET | /api/auth/me | 当前用户信息 |
| POST | /api/auth/logout | 登出 |
| POST | /api/auth/refresh | 刷新 Token |
| POST | /api/auth/change-password | 修改密码 |
| POST | /api/auth/password-reset/send-code | 重置密码验证码 |
| POST | /api/auth/password-reset/confirm | 确认重置密码 |
| GET | /api/auth/roles | 角色列表 |
| GET | /api/auth/permissions | 权限点列表 |
| GET | /api/auth/role-permissions | 角色权限 |

## 新增文件

- `AuthController.java`, `AuthService.java`, `SmsCodeStore.java`, `TokenStore.java`, `AuthDtos.java`
- `UserRoleRepository.java` (system module)

## 修改

- `RoleEntity.java` +permissions 列
- `UserAccountRepository.java` +findByPhone

## Token 策略

UUID-based accessToken(2h) + refreshToken(7d), ConcurrentHashMap 存储.

## 测试

1297 tests, 0/0/9 — 零回归.
