<template>
  <div class="metric-card" :class="`metric-card--${status}`">
    <div class="metric-card__label">{{ title }}</div>
    <div class="metric-card__value">
      <template v-if="loading"><span class="metric-card__skeleton">&nbsp;</span></template>
      <template v-else>{{ formattedValue }}<span v-if="unit" class="metric-card__unit">{{ unit }}</span></template>
    </div>
    <div v-if="subtitle" class="metric-card__subtitle">
      <span v-if="trend === 'up'" class="metric-card__trend metric-card__trend--up">↑</span>
      <span v-else-if="trend === 'down'" class="metric-card__trend metric-card__trend--down">↓</span>
      {{ trendText || subtitle }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  value: string | number | null
  unit?: string
  subtitle?: string
  trend?: 'up' | 'down' | 'flat'
  trendText?: string
  status?: 'normal' | 'warning' | 'danger' | 'info' | 'muted'
  loading?: boolean
}>(), {
  status: 'normal',
  loading: false,
})

const formattedValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return '-'
  if (typeof props.value === 'number' && Number.isFinite(props.value)) {
    return props.value.toLocaleString()
  }
  return props.value
})
</script>

<style scoped>
.metric-card {
  background: var(--app-card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 20px 24px;
  min-width: 160px;
}
.metric-card__label { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.metric-card__value { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.metric-card__unit { font-size: 14px; font-weight: 400; color: var(--text-muted); margin-left: 4px; }
.metric-card__subtitle { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.metric-card__skeleton { display: inline-block; width: 60px; height: 28px; background: var(--border-light); border-radius: 4px; animation: pulse 1.5s infinite; }
.metric-card__trend { font-weight: 600; }
.metric-card__trend--up { color: var(--status-online); }
.metric-card__trend--down { color: var(--status-alarm); }
.metric-card--warning { border-left: 3px solid var(--status-warning); }
.metric-card--danger { border-left: 3px solid var(--status-alarm); }
.metric-card--info { border-left: 3px solid var(--status-info); }
.metric-card--muted { opacity: 0.6; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
