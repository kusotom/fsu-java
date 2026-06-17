<template>
  <div>
    <PageHeader title="FSU 授权" description="三方单位与 FSU 数据范围管理" />

    <el-card class="section">
      <el-table :data="rows" stripe>
        <el-table-column prop="tenantName" label="三方单位" min-width="160" />
        <el-table-column prop="fsuScope" label="授权 FSU 范围" min-width="220" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <StatusBadge :status="row.status" />
          </template>
        </el-table-column>
      </el-table>
      <EmptyState
        v-if="rows.length === 0"
        type="not-implemented"
        title="暂无 FSU 授权数据"
        description="后端租户与 FSU 授权接口接入后展示授权范围"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const rows: Array<{ tenantName: string; fsuScope: string; status: string }> = []
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
