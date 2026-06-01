/**
 * 权限系统类型定义 (FE-AUTH-001 / FE-AUTH-001-FIX-002).
 */
export interface PlatformUser {
  id: string | number
  username: string
  displayName?: string
  email?: string
  phone?: string
  enabled?: boolean
  locked?: boolean
  roles?: PlatformRole[]
  permissions?: string[]
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
  source?: 'backend' | 'frontend-planning' | string
}

export interface PlatformRole {
  id: string | number
  code: 'read_only' | 'operator' | 'admin' | 'super_admin' | 'platform_admin' | 'protocol_debugger' | string
  name: string
  description?: string
  permissions?: string[]
  builtin?: boolean
  enabled?: boolean
  source?: 'backend' | 'frontend-planning' | string
}

export interface PlatformPermission {
  code: string
  name: string
  group?: string
  description?: string
  riskLevel?: 'low' | 'medium' | 'high' | 'critical'
  source?: 'backend' | 'frontend-planning' | string
}

// ===== Auth API 预留类型 (FE-AUTH-001-FIX-002) =====

export interface SmsCodeRequest {
  phone: string
  captchaToken?: string
}

export interface SmsLoginRequest {
  phone: string
  smsCode: string
  deviceId?: string
}

export interface LoginResponse {
  accessToken?: string
  refreshToken?: string
  tokenType?: string
  expiresIn?: number
  user?: PlatformUser
  roles?: PlatformRole[]
  permissions?: string[]
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface PasswordResetSmsRequest {
  phone: string
  captchaToken?: string
}

export interface PasswordResetConfirmRequest {
  phone: string
  smsCode: string
  newPassword: string
  confirmPassword: string
}
