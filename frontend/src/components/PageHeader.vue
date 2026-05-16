<template>
  <div style="margin-bottom: 16px">
    <h2 style="margin: 0 0 4px 0; font-size: 20px">{{ title }}</h2>
    <p v-if="description" style="margin: 0; color: #909399; font-size: 14px">{{ description }}</p>
    <slot name="extra" />
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>
