<template>
  <div class="state-placeholder">
    <div class="state-icon">!</div>
    <div class="state-title">{{ title || '加载失败' }}</div>
    <div v-if="description" class="state-desc">{{ description }}</div>
    <el-button v-if="retryable" type="primary" size="small" style="margin-top: 12px" @click="$emit('retry')">
      重试
    </el-button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
  retryable?: boolean
}>()
defineEmits<{ retry: [] }>()
</script>
