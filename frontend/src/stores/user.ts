import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PlatformUser } from '@/types/auth'

const PERMISSION_ALIASES: Record<string, string[]> = {
  'fsu:view': ['binterface.fsu.read'],
  'realtime:view': ['binterface.realtime.read'],
  'alarm:view': ['binterface.alarm.read'],
  'user:view': ['user.read'],
  'role:view': ['role.read', 'role.manage'],
  'permission:view': ['permission.read'],
  'audit:view': ['audit.read'],
}

/**
 * 用户状态 (FE-AUTH-001~005 + BACKEND-FE-API-001-FIX-001).
 * 角色 code 大小写兼容：后端 ADMIN/OPERATOR/VIEWER → 前端比较时忽略大小写.
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(null)
  const refreshTokenVal = ref<string | null>(null)
  const username = ref<string | null>(null)
  const phone = ref<string | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const currentUser = ref<PlatformUser | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  /** 后端 ADMIN/OPERATOR/VIEWER 与 P0 鉴权角色 → 忽略大小写比较 */
  const hasRoleCI = (code: string) => roles.value.some(r => r.toUpperCase() === code.toUpperCase())

  const isSuperAdmin = computed(() => hasRoleCI('super_admin'))
  const isPlatformAdmin = computed(() => hasRoleCI('platform_admin'))
  const isProtocolDebugger = computed(() => hasRoleCI('protocol_debugger'))
  const isAdmin = computed(() => hasRoleCI('admin') || isSuperAdmin.value || isPlatformAdmin.value)
  const isOperator = computed(() => hasRoleCI('operator') || isAdmin.value)
  const isReadOnly = computed(() => hasRoleCI('viewer') || hasRoleCI('read_only') || (!isOperator.value && !isAdmin.value))
  // FE-P0-RECTIFY-001: 高权限角色识别
  const isTenantAdmin = computed(() => isSuperAdmin.value || isPlatformAdmin.value || hasRoleCI('admin'))
  const isElevatedUser = computed(() => isSuperAdmin.value || isPlatformAdmin.value || isProtocolDebugger.value)

  function hasRole(code: string) { return hasRoleCI(code) }
  function hasPermission(code: string) {
    const aliases = PERMISSION_ALIASES[code] || []
    return permissions.value.includes(code) || aliases.some(alias => permissions.value.includes(alias)) || isAdmin.value
  }

  /** 登录成功后保存后端返回的 token/roles/permissions. roles 归一化为小写 */
  function setAuth(t: string, rt: string, u: string, r?: string[], p?: string[]) {
    token.value = t; refreshTokenVal.value = rt; username.value = u
    roles.value = (r || []).map(x => x.toLowerCase())
    permissions.value = p || []
  }
  function clearAuth() { token.value = null; refreshTokenVal.value = null; username.value = null; phone.value = null; roles.value = []; permissions.value = []; currentUser.value = null }

  return { token, refreshToken: refreshTokenVal, username, phone, roles, permissions, currentUser, isLoggedIn, isAdmin, isOperator, isReadOnly, isSuperAdmin, isPlatformAdmin, isProtocolDebugger, isTenantAdmin, isElevatedUser, hasRole, hasPermission, setAuth, clearAuth }
})
