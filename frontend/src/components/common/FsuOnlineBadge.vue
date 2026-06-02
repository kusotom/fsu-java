<template>
  <span class="fsu-online-badge">
    <span class="fsu-dot" :class="`fsu-dot--${dotStatus}`"></span>
    <span class="fsu-online-badge__text">{{ displayText }}</span>
    <span v-if="lastHeartbeat" class="fsu-online-badge__time">{{ lastHeartbeat }}</span>
    <span v-if="missCount && missCount > 0" class="fsu-online-badge__miss">
      丢失 {{ missCount }} 次
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  onlineStatus?: string
  lastHeartbeat?: string
  heartbeatMissCount?: number
  size?: 'small' | 'default'
}>(), {
  size: 'default',
})

const dotStatus = computed(() => {
  const s = (props.onlineStatus || '').toUpperCase()
  if (s === 'ONLINE') return 'online'
  if (s === 'OFFLINE') return 'offline'
  if (s === 'ABNORMAL' || s === 'ERROR') return 'alarm'
  return 'unknown'
})

const displayText = computed(() => {
  const s = (props.onlineStatus || '').toUpperCase()
  if (s === 'ONLINE') return '在线'
  if (s === 'OFFLINE') return '离线'
  return props.onlineStatus || '未知'
})

const missCount = computed(() => props.heartbeatMissCount)
</script>

<style scoped>
.fsu-online-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 8px 2px 6px;
  font-size: 13px;
  background: #F8FAFC;
  border: 1px solid var(--border-light);
  border-radius: 999px;
}
.fsu-online-badge__text { color: var(--text-primary); font-weight: 500; }
.fsu-online-badge__time { color: var(--text-muted); font-size: 11px; margin-left: 4px; }
.fsu-online-badge__miss { color: var(--status-alarm); font-size: 11px; margin-left: 4px; }
</style>
