<template>
  <div>
    <PageHeader title="站点授权" description="三方单位与站点访问范围管理" />

    <el-card class="section">
      <el-table :data="rows" stripe>
        <el-table-column prop="tenantName" label="三方单位" min-width="160" />
        <el-table-column prop="stationScope" label="授权站点范围" min-width="220" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :status="row.status" />
          </template>
        </el-table-column>
      </el-table>
      <EmptyState
        v-if="rows.length === 0"
        type="not-implemented"
        title="暂无站点授权数据"
        description="后端租户与站点授权接口接入后展示授权范围"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const rows: Array<{ tenantName: string; stationScope: string; status: string }> = []
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
