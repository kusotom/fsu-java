<template>
  <span class="audit-event-tag" :class="`audit-event-tag--${eventType}`">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  action: string
  allowed?: boolean
}>()

const eventType = computed(() => {
  if (props.allowed === false) return 'denied'
  const a = props.action || ''
  if (a.includes('DENIED') || a.includes('BLOCKED')) return 'denied'
  if (a.includes('RAW') || a.includes('RUN_ONCE')) return 'sensitive'
  if (a.includes('SET')) return 'danger'
  return 'normal'
})

const label = computed(() => {
  const a = props.action || ''
  if (a === 'PERMISSION_DENIED') return '权限拒绝'
  if (a === 'RAW_XML_VIEW') return 'raw 查看'
  if (a === 'RAW_XML_DOWNLOAD') return 'raw 下载'
  if (a === 'RUN_ONCE') return 'run-once'
  if (a === 'SET_COMMAND_BLOCKED') return 'SET 拦截'
  return a
})
</script>

<style scoped>
.audit-event-tag {
  display: inline-block; padding: 2px 8px; border-radius: 4px;
  font-size: 12px; font-weight: 500;
}
.audit-event-tag--normal { background: #EFF6FF; color: var(--status-info); }
.audit-event-tag--denied { background: #FEF2F2; color: var(--status-alarm); }
.audit-event-tag--sensitive { background: #FEFCE8; color: var(--status-protocol); }
.audit-event-tag--danger { background: #FEE2E2; color: var(--alarm-critical); }
</style>
