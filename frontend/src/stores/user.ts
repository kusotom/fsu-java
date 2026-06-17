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

const LS_TOKEN = 'fsu_auth_token'
const LS_REFRESH = 'fsu_auth_refresh'
const LS_USER = 'fsu_auth_user'
const LS_ROLES = 'fsu_auth_roles'
const LS_PERMS = 'fsu_auth_permissions'

/** FE-AUTH-PERSIST-FIX-001: 刷新后恢复登录态 */
function loadAuth(): { token: string | null; refresh: string | null; user: string | null; phone: string | null; roles: string[]; perms: string[] } {
  try {
    return {
      token: localStorage.getItem(LS_TOKEN),
      refresh: localStorage.getItem(LS_REFRESH),
      user: localStorage.getItem(LS_USER),
      phone: localStorage.getItem('fsu_auth_phone'),
      roles: JSON.parse(localStorage.getItem(LS_ROLES) || '[]'),
      perms: JSON.parse(localStorage.getItem(LS_PERMS) || '[]'),
    }
  } catch { return { token: null, refresh: null, user: null, phone: null, roles: [], perms: [] } }
}

function persist(tokenVal: string | null, refreshVal: string | null, userVal: string | null, phoneVal: string | null, r: string[], p: string[]) {
  const set = (k: string, v: string | null) => v ? localStorage.setItem(k, v) : localStorage.removeItem(k)
  set(LS_TOKEN, tokenVal); set(LS_REFRESH, refreshVal); set(LS_USER, userVal)
  localStorage.setItem(LS_ROLES, JSON.stringify(r))
  localStorage.setItem(LS_PERMS, JSON.stringify(p))
  if (phoneVal) localStorage.setItem('fsu_auth_phone', phoneVal)
}

function clearPersist() {
  [LS_TOKEN, LS_REFRESH, LS_USER, LS_ROLES, LS_PERMS, 'fsu_auth_phone'].forEach(k => localStorage.removeItem(k))
}

/**
 * 用户状态 (FE-AUTH-001~005 + BACKEND-FE-API-001-FIX-001 + FE-AUTH-PERSIST-FIX-001).
 */
export const useUserStore = defineStore('user', () => {
  // FE-AUTH-PERSIST-FIX-001: store 创建时从 localStorage 恢复
  const saved = loadAuth()
  const token = ref<string | null>(saved.token)
  const refreshTokenVal = ref<string | null>(saved.refresh)
  const username = ref<string | null>(saved.user)
  const phone = ref<string | null>(saved.phone)
  const roles = ref<string[]>(saved.roles)
  const permissions = ref<string[]>(saved.perms)
  const currentUser = ref<PlatformUser | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  const hasRoleCI = (code: string) => roles.value.some(r => r.toUpperCase() === code.toUpperCase())

  const isSuperAdmin = computed(() => hasRoleCI('super_admin'))
  const isPlatformAdmin = computed(() => hasRoleCI('platform_admin'))
  const isProtocolDebugger = computed(() => hasRoleCI('protocol_debugger'))
  const isAdmin = computed(() => hasRoleCI('admin') || isSuperAdmin.value || isPlatformAdmin.value)
  const isOperator = computed(() => hasRoleCI('operator') || isAdmin.value)
  const isReadOnly = computed(() => hasRoleCI('viewer') || hasRoleCI('read_only') || (!isOperator.value && !isAdmin.value))
  const isTenantAdmin = computed(() => isSuperAdmin.value || isPlatformAdmin.value || hasRoleCI('admin'))
  const isElevatedUser = computed(() => isSuperAdmin.value || isPlatformAdmin.value || isProtocolDebugger.value)

  function hasRole(code: string) { return hasRoleCI(code) }
  function hasPermission(code: string) {
    const aliases = PERMISSION_ALIASES[code] || []
    return permissions.value.includes(code) || aliases.some(alias => permissions.value.includes(alias)) || isAdmin.value
  }

  function setAuth(t: string, rt: string, u: string, r?: string[], p?: string[]) {
    token.value = t; refreshTokenVal.value = rt; username.value = u
    roles.value = (r || []).map(x => x.toLowerCase())
    permissions.value = p || []
    persist(t, rt, u, phone.value, roles.value, permissions.value)
  }
  function clearAuth() {
    token.value = null; refreshTokenVal.value = null; username.value = null; phone.value = null
    roles.value = []; permissions.value = []; currentUser.value = null
    clearPersist()
  }

  /** 供外部调用的 token 获取 (request 拦截器等) */
  function getToken() { return token.value }

  return { token, refreshToken: refreshTokenVal, username, phone, roles, permissions, currentUser, isLoggedIn, isAdmin, isOperator, isReadOnly, isSuperAdmin, isPlatformAdmin, isProtocolDebugger, isTenantAdmin, isElevatedUser, hasRole, hasPermission, setAuth, clearAuth, getToken }
})
