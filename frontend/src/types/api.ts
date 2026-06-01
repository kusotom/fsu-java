/**
 * 统一 API 类型定义 (FE-INFRA-002)。
 */
export interface ApiError {
  code: string
  category?: string
  message: string
  details?: Record<string, unknown>
}

export interface ApiWarning {
  code?: string
  message: string
  field?: string
}

export interface NormalizedApiResult<T = unknown> {
  ok: boolean
  data?: T
  error?: ApiError
  warnings: Array<string | ApiWarning>
  auditId?: string
  raw?: unknown
}

/**
 * 兼容当前后端旧结构: { code, message, data, timestamp }
 */
export interface LegacyApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/**
 * 兼容新结构: { ok, data, error, warnings, audit_id, auditId }
 */
export interface NewApiResponse<T = unknown> {
  ok?: boolean
  code?: number | string
  message?: string
  data?: T
  error?: ApiError
  warnings?: Array<string | ApiWarning>
  audit_id?: string
  auditId?: string
  timestamp?: string
}

export type ApiResponse<T = unknown> = LegacyApiResponse<T> | NewApiResponse<T>
