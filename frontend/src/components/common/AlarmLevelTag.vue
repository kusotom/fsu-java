<template>
  <span class="alarm-level" :class="`alarm-level--${normalizedLevel.level}`">
    <span v-if="dot" class="alarm-level__dot"></span>
    {{ normalizedLevel.label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { normalizeAlarmLevel } from '@/utils/alarmDisplay'

const props = withDefaults(defineProps<{
  level?: string
  eventSeverity?: string
  dot?: boolean
}>(), {
  dot: false,
})

const normalizedLevel = computed(() => normalizeAlarmLevel(props.level, props.eventSeverity))
</script>

<style scoped>
.alarm-level__dot {
  display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 4px;
  background: currentColor;
}
</style>
