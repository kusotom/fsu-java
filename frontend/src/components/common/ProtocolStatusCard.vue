<template>
  <div class="protocol-status-card">
    <div class="protocol-status-card__title">{{ title }}</div>
    <div class="protocol-status-card__value">
      <span class="fsu-dot" :class="`fsu-dot--${statusDot}`"></span>
      {{ statusText }}
    </div>
    <div v-if="subtitle" class="protocol-status-card__sub">{{ subtitle }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title: string
  statusText: string
  subtitle?: string
  status?: 'active' | 'idle' | 'blocked' | 'error'
}>()

const statusDot = computed(() => {
  if (props.status === 'active') return 'online'
  if (props.status === 'idle') return 'offline'
  if (props.status === 'blocked') return 'alarm'
  return 'unknown'
})
</script>

<style scoped>
.protocol-status-card {
  background: var(--app-card-bg); border-radius: var(--radius-panel);
  padding: 16px; border-left: 3px solid var(--status-protocol);
}
.protocol-status-card__title { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.protocol-status-card__value { font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.protocol-status-card__sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
</style>
