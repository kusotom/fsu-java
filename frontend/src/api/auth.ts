/**
 * Auth API 预留 (FE-AUTH-001-FIX-002).
 * 真实登录、Token 持久化、路由守卫将在 FE-AUTH-004/005 实现。
 */
import request from './request'
import type { SmsCodeRequest, SmsLoginRequest, LoginResponse, ChangePasswordRequest, PasswordResetSmsRequest, PasswordResetConfirmRequest } from '@/types/auth'

// TODO: 后端接口未接入，所有方法目前返回 404
export function sendSmsLoginCode(data: SmsCodeRequest) { return request.post('/auth/sms/send-code', data) }
export function loginBySmsCode(data: SmsLoginRequest) { return request.post('/auth/sms/login', data) as Promise<LoginResponse> }

export function getCurrentUser() { return request.get('/auth/me') }
export function logout() { return request.post('/auth/logout') }
export function refreshToken() { return request.post('/auth/refresh') }

export function changePassword(data: ChangePasswordRequest) { return request.post('/auth/change-password', data) }

export function sendPasswordResetSmsCode(data: PasswordResetSmsRequest) { return request.post('/auth/password-reset/send-code', data) }
export function resetPasswordBySmsCode(data: PasswordResetConfirmRequest) { return request.post('/auth/password-reset/confirm', data) }

// 角色/权限 (后端路径: /api/auth/roles, /api/auth/permissions, /api/auth/role-permissions)
export function getRoles() { return request.get('/auth/roles') }
export function getPermissions() { return request.get('/auth/permissions') }
export function getRolePermissions() { return request.get('/auth/role-permissions') }
