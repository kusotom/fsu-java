<template>
  <div>
    <PageHeader title="FSU 注册状态" description="B接口 FSU 登录与在线状态（只读）" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fsuCode" label="FSU编码" width="120" />
        <el-table-column prop="loginStatus" label="登录状态" width="100"><template #default="{row}"><el-tag :type="row.loginStatus==='LOGIN'?'success':'warning'">{{row.loginStatus}}</el-tag></template></el-table-column>
        <el-table-column prop="onlineStatus" label="在线状态" width="100"><template #default="{row}"><el-tag :type="row.onlineStatus==='ONLINE'?'success':'danger'">{{row.onlineStatus}}</el-tag></template></el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="170" />
        <el-table-column prop="heartbeatMissCount" label="心跳丢失" width="90" />
        <el-table-column prop="sessionId" label="会话ID" width="180" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getFsuStatusList } from '@/api/bInterface'
import type { BInterfaceFsuStatus } from '@/types'

const loading = ref(false)
const tableData = ref<BInterfaceFsuStatus[]>([])
onMounted(async () => { loading.value = true; try { const res: any = await getFsuStatusList(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
