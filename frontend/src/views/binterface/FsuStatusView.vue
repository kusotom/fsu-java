<template>
  <div>
    <PageHeader title="FSU 注册与在线状态" description="B接口 FSU 登录状态、心跳信息（只读）" />
    <DataTable :data="tableData" :loading="loading" :error="error" @refresh="fetchData">
      <el-table-column prop="fsuCode" label="FSU编码" width="140" />
      <el-table-column prop="loginStatus" label="登录" width="80">
        <template #default="{row}"><StatusBadge :status="row.loginStatus" /></template>
      </el-table-column>
      <el-table-column prop="onlineStatus" label="在线" width="80">
        <template #default="{row}"><StatusBadge :status="row.onlineStatus" /></template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
      <el-table-column prop="lastHeartbeat" label="最后心跳" width="160" />
      <el-table-column prop="heartbeatMissCount" label="丢失" width="70" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" @click="goDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/common/DataTable.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getBInterfaceFsus } from '@/api/bInterface'
import type { ApiError } from '@/types/api'

const router = useRouter()
const loading = ref(false)
const error = ref<ApiError | null>(null)
const tableData = ref<any[]>([])

async function fetchData() {
  loading.value = true; error.value = null
  try { const res: any = await getBInterfaceFsus(); tableData.value = res.data || [] }
  catch { error.value = { code: 'load_failed', message: '加载 FSU 状态失败' } }
  finally { loading.value = false }
}

function goDetail(row: any) { router.push(`/b-interface/fsus/${row.fsuCode}`) }

onMounted(fetchData)
</script>
