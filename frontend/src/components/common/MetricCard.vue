<template>
  <div class="metric-card" :class="`metric-card--${status}`">
    <div class="metric-card__head">
      <div class="metric-card__label">{{ title }}</div>
      <span class="metric-card__indicator"></span>
    </div>
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
  position: relative;
  background: var(--app-card-bg, #fff);
  border: 1px solid var(--border-light, #ebeef5);
  border-radius: 8px;
  box-shadow: none;
  padding: 16px 20px;
  min-width: 150px;
  min-height: 88px;
  transition: box-shadow 0.18s ease;
}
.metric-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  border-color: var(--border-light, #ebeef5);
}
.metric-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.metric-card__indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.metric-card__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #909399);
}
.metric-card__value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--text-primary, #303133);
  letter-spacing: -0.02em;
}
.metric-card__unit { font-size: 14px; font-weight: 400; color: var(--text-muted, #c0c4cc); margin-left: 4px; }
.metric-card__subtitle { font-size: 12px; color: var(--text-muted, #c0c4cc); margin-top: 4px; }
.metric-card__skeleton { display: inline-block; width: 60px; height: 28px; background: var(--border-light, #ebeef5); border-radius: 4px; animation: pulse 1.5s infinite; }
.metric-card__trend { font-weight: 600; }
.metric-card__trend--up { color: var(--status-online); }
.metric-card__trend--down { color: var(--status-alarm); }
.metric-card--normal .metric-card__indicator { background: var(--status-online, #67c23a); }
.metric-card--warning .metric-card__indicator { background: var(--status-warning, #e6a23c); }
.metric-card--danger .metric-card__indicator { background: var(--status-alarm, #f56c6c); }
.metric-card--info .metric-card__indicator { background: var(--status-info, #409eff); }
.metric-card--muted { opacity: 0.55; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
</style>
