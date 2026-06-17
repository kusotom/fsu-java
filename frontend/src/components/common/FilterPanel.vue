<template>
  <div class="filter-panel">
    <slot />
    <el-button size="small" type="primary" :loading="loading" @click="$emit('search')">
      <el-icon><Search /></el-icon> 查询
    </el-button>
    <el-button size="small" @click="$emit('reset')">重置</el-button>
  </div>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'

defineProps<{ loading?: boolean }>()
defineEmits<{ search: []; reset: [] }>()
</script>
