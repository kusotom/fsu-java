<template>
  <div>
    <PageHeader title="B接口报文日志" description="SOAP/XML 报文记录查询（只读）" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="direction" label="方向" width="100"><template #default="{row}"><el-tag :type="row.direction==='FSU_TO_SC'?'success':'info'">{{row.direction}}</el-tag></template></el-table-column>
        <el-table-column prop="commandCode" label="命令" width="120" />
        <el-table-column prop="messageType" label="消息类型" width="100" />
        <el-table-column prop="fsuCode" label="FSU编码" width="120" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="processTimeMs" label="耗时ms" width="80" />
        <el-table-column prop="createdAt" label="接收时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getMessageLogs } from '@/api/bInterface'
import type { BInterfaceMessageLog } from '@/types'

const loading = ref(false)
const tableData = ref<BInterfaceMessageLog[]>([])
onMounted(async () => { loading.value = true; try { const res: any = await getMessageLogs(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
