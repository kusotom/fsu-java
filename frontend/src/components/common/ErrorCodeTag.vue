<template>
  <el-tag v-if="code" :type="tagType" size="small" :effect="effect">
    {{ code }}
  </el-tag>
  <span v-else class="error-code-empty">-</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ERROR_CODE_CATEGORY, CATEGORY_TAG_TYPE } from '../../utils/apiError'

const props = withDefaults(defineProps<{
  code?: string
  category?: string
  effect?: 'dark' | 'light' | 'plain'
}>(), {
  effect: 'light'
})

const tagType = computed(() => {
  const cat = props.category ?? (props.code ? (ERROR_CODE_CATEGORY[props.code] ?? 'unknown') : 'unknown')
  return CATEGORY_TAG_TYPE[cat] ?? 'info'
})
</script>

<style scoped>
.error-code-empty { color: #999; }
</style>
