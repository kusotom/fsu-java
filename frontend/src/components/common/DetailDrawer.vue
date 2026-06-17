<template>
  <el-drawer
    :model-value="visible"
    :title="title"
    :size="width"
    direction="rtl"
    class="detail-drawer"
    @close="$emit('close')"
  >
    <slot />
  </el-drawer>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  title?: string
  width?: string
}>()
defineEmits<{ close: [] }>()
</script>
