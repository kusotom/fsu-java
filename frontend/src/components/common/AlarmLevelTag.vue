<template>
  <span class="alarm-level" :class="`alarm-level--${level}`">
    <span v-if="dot" class="alarm-level__dot"></span>
    {{ displayText }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  level?: string
  dot?: boolean
}>(), {
  dot: false,
})

const displayText = computed(() => {
  const l = (props.level || '').toUpperCase()
  if (l === 'CRITICAL') return '一级'
  if (l === 'MAJOR') return '二级'
  if (l === 'MINOR') return '三级'
  if (l === 'WARN') return '四级'
  return props.level || '未知'
})
</script>

<style scoped>
.alarm-level__dot {
  display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 4px;
  background: currentColor;
}
</style>
