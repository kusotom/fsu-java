# FE-INFRA-002: 统一 API 错误处理

## 日期
2026-05-21 | 状态: 已完成

## 新增

- `types/api.ts`: ApiError/ApiWarning/NormalizedApiResult
- `utils/apiError.ts`: normalizeApiResponse() + error code 映射 + 脱敏
- `components/common/ErrorCodeTag.vue`
- `components/common/ApiErrorAlert.vue`
- `components/common/AuditIdLink.vue`
- `components/common/WarningsList.vue`

## 修改

- `api/request.ts`: error interceptor 增强 (非敏感日志)

## 验证

TypeScript 0 errors, Vite build ✓
