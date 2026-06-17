<template>
  <template v-if="canAccess">
    <slot />
  </template>
  <template v-else-if="mode === 'disable'">
    <el-tooltip :content="disabledText" placement="top">
      <span class="perm-disabled"><slot /><slot name="disabled" /></span>
    </el-tooltip>
  </template>
</template>

<script setup lang="ts">
/**
 * FE-P0-RECTIFY-002 统一权限语义:
 *   permissions: all-of — 用户必须拥有数组中全部权限码
 *   roles:       any-of — 用户只需拥有数组中任一角色
 *   permissions + roles 同时指定: AND — 两者都必须满足
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { hasAllPermissions, hasAnyRole } from '@/auth/access'

const props = withDefaults(defineProps<{
  permissions?: string[]
  roles?: string[]
  mode?: 'hide' | 'disable'
  disabledText?: string
}>(), {
  mode: 'hide',
  disabledText: '无权限'
})

const userStore = useUserStore()

const canAccess = computed(() => {
  if (userStore.isAdmin) return true

  let permOk = true
  let roleOk = true

  if (props.permissions?.length) {
    permOk = hasAllPermissions(userStore.permissions, props.permissions)
  }
  if (props.roles?.length) {
    roleOk = hasAnyRole(userStore.roles, props.roles)
  }

  // AND 语义: 两者都指定时, 两者都必须满足
  return permOk && roleOk
})
</script>

<style scoped>
.perm-disabled { cursor: not-allowed; opacity: 0.5; pointer-events: none; }
</style>
