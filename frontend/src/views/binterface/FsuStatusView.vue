<template>
  <div class="page-template">
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

    <!-- ===== FSU 卡片 ===== -->
    <div v-loading="loading" class="fsu-card-grid">
      <article v-for="row in filteredData" :key="row.fsuCode || row.id" class="fsu-card">
        <div class="fsu-card__header">
          <div>
            <h3 class="fsu-card__code">{{ row.fsuCode || '-' }}</h3>
            <div class="fsu-card__site">{{ siteLabel(row) }}</div>
          </div>
          <div class="fsu-card__icon">
            <el-icon><Monitor /></el-icon>
          </div>
        </div>

        <div class="fsu-card__body">
          <div class="fsu-card__field">
            <div class="fsu-card__field-label">在线状态</div>
            <div class="fsu-card__field-value">
              <FsuOnlineBadge :online-status="row.onlineStatus" :heartbeat-miss-count="row.heartbeatMissCount" />
            </div>
          </div>
          <div class="fsu-card__field">
            <div class="fsu-card__field-label">心跳状态</div>
            <div class="fsu-card__field-value">
              <StatusBadge :status="heartbeatStatus(row)" :label="heartbeatLabel(row)" />
            </div>
          </div>
          <div class="fsu-card__field">
            <div class="fsu-card__field-label">最近登录时间</div>
            <div class="fsu-card__field-value">{{ row.lastLoginTime || '-' }}</div>
          </div>
          <div class="fsu-card__field">
            <div class="fsu-card__field-label">最近心跳时间</div>
            <div class="fsu-card__field-value">{{ row.lastHeartbeat || '-' }}</div>
          </div>
          <div class="fsu-card__field">
            <div class="fsu-card__field-label">设备数量</div>
            <div class="fsu-card__field-value">{{ deviceCount(row) }}</div>
          </div>
          <div class="fsu-card__field">
            <div class="fsu-card__field-label">告警数量</div>
            <div class="fsu-card__field-value">{{ alarmCount(row) }}</div>
          </div>
          <div class="fsu-card__field" style="grid-column: span 2">
            <div class="fsu-card__field-label">最后更新时间</div>
            <div class="fsu-card__field-value">{{ lastUpdate(row) }}</div>
          </div>
        </div>

        <div class="fsu-card__footer">
          <el-button size="small" type="primary" text @click="goDetail(row)">详情</el-button>
          <el-button size="small" text @click="goDetail(row)">设备列表</el-button>
          <el-button v-if="canViewCommunication" size="small" text @click="goCommunication(row)">通信记录</el-button>
          <el-button size="small" text :loading="loading" @click="loadData">刷新状态</el-button>
        </div>
      </article>
    </div>

    <div v-if="!loading && filteredData.length === 0" class="app-card">
      <EmptyState v-if="dataState === 'normal'" type="empty" title="暂无 FSU 数据" :description="filterActive ? '当前筛选条件下无匹配结果' : '暂无注册的 FSU 设备'" />
      <EmptyState v-else-if="dataState === 'empty'" type="empty" title="暂无 FSU 数据" description="HTTP 200 成功但 data 为空" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FilterPanel from '@/components/common/FilterPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FsuOnlineBadge from '@/components/common/FsuOnlineBadge.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getBInterfaceFsus } from '@/api/bInterface'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
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
const canViewCommunication = computed(() => userStore.hasPermission('protocol:raw:view'))

function goDetail(row: any) { router.push(`/b-interface/fsus/${row.fsuCode}`) }
function goCommunication(row: any) { router.push({ path: '/b-interface/calls', query: { fsuCode: row.fsuCode } }) }
function siteLabel(row: any) { return row.stationName || row.siteName || row.stationCode || '未绑定站点' }
function deviceCount(row: any) { return row.deviceCount ?? row.devicesCount ?? row.deviceTotal ?? 0 }
function alarmCount(row: any) { return row.activeAlarmCount ?? row.alarmCount ?? 0 }
function lastUpdate(row: any) { return row.updateTime || row.updatedAt || row.lastHeartbeat || row.lastLoginTime || '-' }
function heartbeatStatus(row: any) { return (row.heartbeatMissCount || 0) > 3 ? 'warning' : 'success' }
function heartbeatLabel(row: any) { return (row.heartbeatMissCount || 0) > 3 ? `丢失 ${row.heartbeatMissCount} 次` : '正常' }

async function loadData() {
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
}

onMounted(loadData)
</script>
