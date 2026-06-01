<template>
  <div v-if="items.length" class="warnings-list">
    <el-text v-for="(w, i) in items" :key="i" size="small" type="warning">
      <span v-if="typeof w === 'string'">⚠ {{ w }}</span>
      <span v-else>⚠ {{ w.message }}</span>
    </el-text>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApiWarning } from '../../types/api'

const props = defineProps<{ warnings?: Array<string | ApiWarning> }>()
const items = computed(() => props.warnings ?? [])
</script>

<style scoped>
.warnings-list { display: flex; flex-direction: column; gap: 2px; margin-top: 4px; }
</style>
