<template>
  <div class="site-monitor-tabs">
    <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="站点列表" name="/sites" />
      <el-tab-pane label="实时数据" name="/sites/realtime" />
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeTab = computed(() => {
  if (route.path.startsWith('/sites/realtime')) return '/sites/realtime'
  return '/sites'
})

function handleTabChange(name: string | number) {
  const target = String(name)
  if (target !== route.path) router.push(target)
}
</script>

<style scoped>
.site-monitor-tabs {
  margin-bottom: 16px;
}

.site-monitor-tabs :deep(.el-tabs__header) {
  margin: 0;
}
</style>
