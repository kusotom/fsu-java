/**
 * 前端权限工具函数 (FE-AUTH-003 + FE-P0-RECTIFY-002).
 * source=frontend-planning, 真实安全由后端校验.
 *
 * FE-P0-RECTIFY-002 统一语义:
 *   routeGuard / PermissionGuard 中:
 *     permissions → all-of (hasAllPermissions)
 *     roles       → any-of (hasAnyRole)
 *     permissions + roles → AND (两者同时满足)
 *   hasAnyPermission 保留供特殊场景使用，但不作为默认语义。
 */
export function hasAnyPermission(userPermissions: string[], required?: string[]): boolean {
  if (!required || required.length === 0) return true
  return required.some(p => userPermissions.includes(p))
}

export function hasAllPermissions(userPermissions: string[], required?: string[]): boolean {
  if (!required || required.length === 0) return true
  return required.every(p => userPermissions.includes(p))
}

export function hasAnyRole(userRoles: string[], required?: string[]): boolean {
  if (!required || required.length === 0) return true
  return required.some(r => userRoles.includes(r))
}

export function isHighRiskPermission(permission: string): boolean {
  return permission.includes('.execute') || permission.includes('reboot') || permission.includes('upgrade')
}

export function getPermissionRiskLevel(permission: string): 'low' | 'medium' | 'high' | 'critical' {
  if (permission.includes('reboot') || permission.includes('upgrade')) return 'critical'
  if (permission.includes('execute') || permission.includes('manage')) return 'high'
  if (permission.includes('.get.') || permission.includes('run_due')) return 'medium'
  return 'low'
}
