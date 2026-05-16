<template>
  <div>
    <PageHeader title="FSUService 调用记录" description="SC 调用 FSU 的请求/响应记录（只读）" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fsuCode" label="FSU编码" width="120" />
        <el-table-column prop="commandCode" label="命令" width="130" />
        <el-table-column prop="callType" label="调用类型" width="90" />
        <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='SUCCESS'?'success':row.status==='FAILED'?'danger':'info'">{{row.status}}</el-tag></template></el-table-column>
        <el-table-column prop="durationMs" label="耗时ms" width="80" />
        <el-table-column prop="retryCount" label="重试" width="60" />
        <el-table-column prop="callTime" label="调用时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getCallRecords } from '@/api/bInterface'
import type { BInterfaceCallRecord } from '@/types'

const loading = ref(false)
const tableData = ref<BInterfaceCallRecord[]>([])
onMounted(async () => { loading.value = true; try { const res: any = await getCallRecords(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
