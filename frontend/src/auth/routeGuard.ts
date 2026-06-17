/**
 * 路由守卫 (FE-AUTH-005 + FE-P0-RECTIFY-002).
 * 前端权限仅用于展示控制，真实安全由后端校验。
 *
 * 权限语义 (FE-P0-RECTIFY-002 统一):
 *   permissions: all-of — 用户必须拥有数组中全部权限码
 *   roles:       any-of — 用户只需拥有数组中任一角色
 *   permissions + roles 同时存在: AND — 两者都必须满足
 */
import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

const PUBLIC_PATHS = ['/login', '/forgot-password', '/403']

export function setupRouteGuard(router: Router) {
  router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()

    // public 页面直接放行
    if (PUBLIC_PATHS.includes(to.path) || to.meta.public) {
      return next()
    }

    // requiresAuth 但未登录 → login
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // FE-P0-RECTIFY-002: permissions 为 all-of 语义
    const required = to.meta.permissions as string[] | undefined
    if (required?.length) {
      const hasPerm = required.every(p => userStore.hasPermission(p)) || userStore.isAdmin
      if (!hasPerm) {
        return next({ path: '/403' })
      }
    }

    // FE-P0-RECTIFY-002: roles 为 any-of 语义
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles?.length) {
      const hasRole = requiredRoles.some(r => userStore.hasRole(r)) || userStore.isAdmin
      if (!hasRole) {
        return next({ path: '/403' })
      }
    }

    next()
  })
}
