<template>
  <div>
    <PageHeader title="FTP 文件/图片记录" description="FTP传输记录查询（只读，不展示密码字段）" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fsuCode" label="FSU编码" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="180" />
        <el-table-column prop="fileType" label="文件类型" width="90" />
        <el-table-column prop="fileSize" label="文件大小" width="90" />
        <el-table-column prop="direction" label="方向" width="90" />
        <el-table-column prop="transferStatus" label="状态" width="90"><template #default="{row}"><el-tag :type="row.transferStatus==='SUCCESS'?'success':row.transferStatus==='FAILED'?'danger':'info'">{{row.transferStatus}}</el-tag></template></el-table-column>
        <el-table-column prop="transferTime" label="传输时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getFtpRecords } from '@/api/bInterface'
import type { FtpTransferRecord } from '@/types'

const loading = ref(false)
const tableData = ref<FtpTransferRecord[]>([])
onMounted(async () => { loading.value = true; try { const res: any = await getFtpRecords(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
