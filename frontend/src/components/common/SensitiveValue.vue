<template>
  <span class="sensitive-value">
    <el-tag v-if="configured && !value" size="small" type="success" effect="plain">已配置</el-tag>
    <el-tag v-else-if="configured === false" size="small" type="info" effect="plain">未配置</el-tag>
    <span v-else-if="value" class="masked">******</span>
    <span v-else class="empty">-</span>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  value?: string | null
  configured?: boolean
}>()
</script>

<style scoped>
.sensitive-value { font-family: monospace; }
.masked { color: #999; }
.empty { color: #ccc; }
</style>
