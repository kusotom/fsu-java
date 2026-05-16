<template>
  <div>
    <PageHeader title="告警中心" description="查看和管理告警记录" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fsuId" label="FSU ID" width="80" />
        <el-table-column prop="pointCode" label="点位编码" width="120" />
        <el-table-column prop="alarmName" label="告警名称" min-width="140" />
        <el-table-column prop="alarmLevel" label="等级" width="90"><template #default="{row}"><el-tag :type="row.alarmLevel==='URGENT'?'danger':row.alarmLevel==='IMPORTANT'?'warning':'info'">{{row.alarmLevel}}</el-tag></template></el-table-column>
        <el-table-column prop="alarmStatus" label="状态" width="90"><template #default="{row}"><el-tag :type="row.alarmStatus==='ACTIVE'?'danger':row.alarmStatus==='CLEARED'?'success':'info'">{{row.alarmStatus}}</el-tag></template></el-table-column>
        <el-table-column prop="alarmValue" label="告警值" width="90" />
        <el-table-column prop="occurTime" label="发生时间" width="170" />
        <el-table-column prop="clearTime" label="恢复时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getAlarms } from '@/api/alarm'
import type { AlarmRecord } from '@/types'

const loading = ref(false)
const tableData = ref<AlarmRecord[]>([])

onMounted(async () => { loading.value = true; try { const res: any = await getAlarms(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
