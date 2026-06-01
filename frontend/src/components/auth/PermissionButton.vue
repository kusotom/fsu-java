<template>
  <template v-if="canAccess">
    <slot />
  </template>
  <template v-else-if="mode === 'disable'">
    <el-tooltip :content="disabledText || '无权限'" placement="top">
      <span style="cursor: not-allowed; opacity: 0.4; pointer-events: none;">
        <slot />
      </span>
    </el-tooltip>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { hasAnyRole } from '@/auth/access'

/**
 * FE-P1-UI-001: 统一权限按钮.
 * permissions: all-of 语义 (必须全部拥有)
 * roles: any-of 语义 (满足任一即可)
 * permissions + roles: AND (两者都必须满足)
 * admin 可绕过普通权限检查; super_admin 可绕过敏感权限检查.
 */
const props = withDefaults(defineProps<{
  permissions?: string[]
  roles?: string[]
  mode?: 'hide' | 'disable'
  disabledText?: string
  requireAll?: boolean
}>(), {
  mode: 'hide',
  requireAll: true,
})

const userStore = useUserStore()

const canAccess = computed(() => {
  if (userStore.isSuperAdmin) return true

  let permOk = true
  let roleOk = true

  if (props.permissions?.length) {
    if (props.requireAll) {
      permOk = props.permissions.every(p => userStore.hasPermission(p))
    } else {
      permOk = props.permissions.some(p => userStore.hasPermission(p))
    }
    if (!permOk && userStore.isAdmin) {
      // admin-like 可绕过普通权限，但敏感权限由 AuthInterceptor 后端控制
      permOk = true
    }
  }

  if (props.roles?.length) {
    roleOk = hasAnyRole(userStore.roles, props.roles) || userStore.isAdmin
  }

  return permOk && roleOk
})
</script>
