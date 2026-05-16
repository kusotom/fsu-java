<template>
  <div>
    <PageHeader title="历史数据" description="查看点位历史采集记录" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fsuId" label="FSU ID" width="80" />
        <el-table-column prop="pointId" label="点位ID" width="80" />
        <el-table-column prop="pointCode" label="点位编码" width="120" />
        <el-table-column prop="valueNumber" label="数值" width="100" />
        <el-table-column prop="valueText" label="文本值" width="100" />
        <el-table-column prop="valueStatus" label="状态" width="90" />
        <el-table-column prop="quality" label="质量" width="80" />
        <el-table-column prop="collectTime" label="采集时间" width="170" />
        <el-table-column prop="receiveTime" label="接收时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getHistoryData } from '@/api/telemetry'
import type { HistoryData } from '@/types'

const loading = ref(false)
const tableData = ref<HistoryData[]>([])

onMounted(async () => { loading.value = true; try { const res: any = await getHistoryData(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
