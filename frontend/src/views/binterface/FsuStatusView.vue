<template>
  <div>
    <PageHeader title="FSU 管理" description="FSU 注册状态、在线状态、心跳与告警总览" />

    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" />

    <!-- ===== 指标卡片 ===== -->
    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="FSU 总数" :value="totalCount" status="info" :loading="loading" />
      <MetricCard title="在线" :value="onlineCount" status="normal" :loading="loading" />
      <MetricCard title="离线" :value="offlineCount" :status="offlineCount > 0 ? 'warning' : 'normal'" :loading="loading" />
      <MetricCard title="告警" :value="alarmFsuCount" :status="alarmFsuCount > 0 ? 'danger' : 'normal'" :loading="loading" />
    </div>

    <!-- ===== 筛选区 ===== -->
    <FilterPanel style="margin-bottom: 16px">
      <el-select v-model="filterStatus" placeholder="在线状态" clearable size="small" style="width: 140px">
        <el-option label="在线" value="ONLINE" />
        <el-option label="离线" value="OFFLINE" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="FSU 编码" clearable size="small" style="width: 160px" />
    </FilterPanel>

    <!-- ===== FSU 表格 ===== -->
    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="fsuCode" label="FSU 编码" width="150" />
        <el-table-column prop="onlineStatus" label="在线状态" width="110">
          <template #default="{ row }"><FsuOnlineBadge :online-status="row.onlineStatus" :last-heartbeat="row.lastHeartbeat" :heartbeat-miss-count="row.heartbeatMissCount" /></template>
        </el-table-column>
        <el-table-column prop="loginStatus" label="登录" width="80">
          <template #default="{ row }"><StatusBadge :status="row.loginStatus" /></template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="150">
          <template #default="{ row }"><span style="font-size: 12px; color: var(--text-secondary)">{{ row.lastLoginTime || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="150">
          <template #default="{ row }"><span style="font-size: 12px; color: var(--text-secondary)">{{ row.lastHeartbeat || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="heartbeatMissCount" label="丢失" width="70" align="center">
          <template #default="{ row }">
            <span v-if="row.heartbeatMissCount && row.heartbeatMissCount > 0" style="color: var(--status-alarm); font-weight: 600">{{ row.heartbeatMissCount }}</span>
            <span v-else style="color: var(--text-muted)">0</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && filteredData.length === 0 && dataState === 'normal'" type="empty" title="暂无 FSU 数据" :description="filterActive ? '当前筛选条件下无匹配结果' : '暂无注册的 FSU 设备'" />
      <EmptyState v-else-if="!loading && filteredData.length === 0 && dataState === 'empty'" type="empty" title="暂无 FSU 数据" description="HTTP 200 成功但 data 为空" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FilterPanel from '@/components/common/FilterPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FsuOnlineBadge from '@/components/common/FsuOnlineBadge.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getBInterfaceFsus } from '@/api/bInterface'

const router = useRouter()
const loading = ref(false)
const rawData = ref<any[]>([])
const filterStatus = ref('')
const filterKeyword = ref('')
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'unauthorized' | 'forbidden' | 'server_error' | 'empty'>('normal')

const filteredData = computed(() => {
  let rows = rawData.value
  if (filterStatus.value) rows = rows.filter(r => (r.onlineStatus || '').toUpperCase() === filterStatus.value.toUpperCase())
  if (filterKeyword.value) rows = rows.filter(r => (r.fsuCode || '').toLowerCase().includes(filterKeyword.value.toLowerCase()))
  return rows
})

const totalCount = computed(() => rawData.value.length)
const onlineCount = computed(() => rawData.value.filter(r => (r.onlineStatus || '').toUpperCase() === 'ONLINE').length)
const offlineCount = computed(() => totalCount.value - onlineCount.value)
const alarmFsuCount = computed(() => rawData.value.filter(r => (r.heartbeatMissCount || 0) > 3).length)
const filterActive = computed(() => !!(filterStatus.value || filterKeyword.value))

function goDetail(row: any) { router.push(`/b-interface/fsus/${row.fsuCode}`) }

onMounted(async () => {
  loading.value = true; dataState.value = 'normal'
  try {
    const res: any = await getBInterfaceFsus()
    rawData.value = Array.isArray(res?.data) ? res.data : (res?.data?.data || [])
    if (rawData.value.length === 0) dataState.value = 'empty'
  } catch (e: any) {
    rawData.value = []
    if (e?.response?.status === 404) dataState.value = 'api_not_found'
    else if (e?.response?.status === 401) dataState.value = 'unauthorized'
    else if (e?.response?.status === 403) dataState.value = 'forbidden'
    else if (e?.response?.status && e.response.status >= 500) dataState.value = 'server_error'
    else if (e?.request && !e?.response) dataState.value = 'network_error'
    else dataState.value = 'server_error'
  } finally { loading.value = false }
})
</script>
