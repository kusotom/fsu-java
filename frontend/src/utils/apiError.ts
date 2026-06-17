import type { ApiError, ApiWarning, NormalizedApiResult, LegacyApiResponse, NewApiResponse } from '../types/api'

/**
 * 归一化后端 API 响应为统一结构 (FE-INFRA-002)。
 */
export function normalizeApiResponse<T = unknown>(
  response: unknown
): NormalizedApiResult<T> {
  if (!response || typeof response !== 'object') {
    return { ok: false, error: { code: 'invalid_response', message: '无效响应' }, warnings: [] }
  }

  const r = response as Record<string, unknown>

  // 新结构: { ok, data, error, warnings, audit_id }
  if ('ok' in r || 'error' in r) {
    const nr = r as NewApiResponse<T>
    return {
      ok: nr.ok ?? !nr.error,
      data: nr.data,
      error: nr.error,
      warnings: normalizeWarnings(nr.warnings),
      auditId: nr.auditId ?? nr.audit_id,
      raw: r
    }
  }

  // 旧结构: { code, message, data, timestamp }
  if ('code' in r && 'message' in r) {
    const lr = r as unknown as LegacyApiResponse<T>
    const code = lr.code
    const ok = code === 0 || code === 200
    return {
      ok,
      data: lr.data,
      error: ok ? undefined : { code: String(code), category: 'business', message: lr.message },
      warnings: [],
      raw: r
    }
  }

  return { ok: false, error: { code: 'unknown_format', message: '未知响应格式' }, warnings: [] }
}

function normalizeWarnings(raw?: Array<string | ApiWarning>): Array<string | ApiWarning> {
  if (!raw || !Array.isArray(raw)) return []
  return raw
}

/**
 * 脱敏敏感字段 (FE-INFRA-002)。
 */
const SENSITIVE_KEYS = [
  'password', 'Password', 'FTPPwd', 'IPSecPWD',
  'confirmation_token', 'confirmationToken', 'token'
]

export function sanitizeForDisplay(obj: unknown): unknown {
  if (!obj || typeof obj !== 'object') return obj
  if (Array.isArray(obj)) return obj.map(sanitizeForDisplay)
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(obj as Record<string, unknown>)) {
    if (SENSITIVE_KEYS.some(sk => k.toLowerCase().includes(sk.toLowerCase()))) {
      out[k] = '***'
    } else if (typeof v === 'object') {
      out[k] = sanitizeForDisplay(v)
    } else {
      out[k] = v
    }
  }
  return out
}

/**
 * error_code → category 映射。
 */
export const ERROR_CODE_CATEGORY: Record<string, string> = {
  safety_blocked: 'safety',
  not_whitelisted: 'safety',
  real_call_disabled: 'safety',
  scheduler_disabled: 'safety',
  confirmation_required: 'safety',
  business_failure: 'business',
  validation_error: 'validation',
  soap_timeout: 'transport',
  xml_parse_error: 'parse',
  circuit_open: 'circuit_breaker',
  not_mapped: 'business',
  unknown_command: 'protocol',
  invalid_session: 'authorization'
}

export const CATEGORY_TAG_TYPE: Record<string, string> = {
  safety: 'warning',
  authorization: 'warning',
  whitelist: 'warning',
  validation: 'danger',
  transport: 'danger',
  timeout: 'danger',
  business: 'warning',
  parse: 'danger',
  protocol: 'danger',
  circuit_breaker: 'info',
  unknown: 'info'
}
