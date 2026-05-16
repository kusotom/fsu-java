<template>
  <el-card style="margin-bottom: 16px">
    <el-form :inline="true" :model="modelValue" @keyup.enter="$emit('search')">
      <slot />
      <el-form-item>
        <el-button type="primary" @click="$emit('search')">查询</el-button>
        <el-button @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ modelValue: Record<string, string> }>()
defineEmits<{ search: []; reset: [] }>()
</script>
