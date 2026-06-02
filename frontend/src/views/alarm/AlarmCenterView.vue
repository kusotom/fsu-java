<template>
  <div class="page-template">
    <PageHeader title="告警中心" description="查看和管理站点与 FSU 的告警记录" />

    <!-- ===== 状态提示 ===== -->
    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" />

    <!-- ===== Tabs: 当前告警 / 历史告警 ===== -->
    <el-tabs v-model="activeTab" style="margin-bottom: 16px">
      <el-tab-pane label="当前告警" name="active" />
      <el-tab-pane label="历史告警" name="history" />
    </el-tabs>

    <!-- ===== 当前告警指标 ===== -->
    <div v-if="activeTab === 'active'" class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="当前告警" :value="activeAlarms.length" :status="activeAlarms.length > 0 ? 'danger' : 'normal'" />
      <MetricCard title="严重告警" :value="criticalCount" :status="criticalCount > 0 ? 'danger' : 'normal'" />
      <MetricCard title="主要告警" :value="majorCount" :status="majorCount > 0 ? 'warning' : 'normal'" />
      <MetricCard title="次要告警" :value="minorCount" status="info" />
    </div>

    <!-- ===== 筛选区 ===== -->
    <FilterPanel style="margin-bottom: 16px">
      <el-select v-model="filterLevel" placeholder="告警等级" clearable size="small" style="width: 130px">
        <el-option v-for="l in levels" :key="l" :label="levelLabel(l)" :value="l" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="告警状态" clearable size="small" style="width: 130px">
        <el-option label="活跃" value="ACTIVE" />
        <el-option label="已恢复" value="RECOVERED" />
        <el-option label="已确认" value="CONFIRMED" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="关键字" clearable size="small" style="width: 160px" />
    </FilterPanel>

    <!-- ===== 主体表格 ===== -->
    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="alarmLevel" label="告警等级" width="100">
          <template #default="{ row }"><AlarmLevelTag :level="row.displayAlarmLevel" /></template>
        </el-table-column>
        <el-table-column prop="alarmName" label="告警名称" min-width="180">
          <template #default="{ row }">
            <span>{{ row.displayAlarmName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="设备 / 测点" min-width="180">
          <template #default="{ row }">
            <span>{{ row.displayDevicePoint }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmStatus" label="告警状态" width="100">
          <template #default="{ row }"><StatusBadge :status="statusBadge(row.displayAlarmStatus)" :label="row.displayAlarmStatusLabel" /></template>
        </el-table-column>
        <el-table-column prop="occurTime" label="发生时间" width="160">
          <template #default="{ row }"><span style="font-size: 12px; color: var(--text-secondary)">{{ row.displayOccurTime || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="clearTime" label="恢复时间" width="160">
          <template #default="{ row }"><span style="font-size: 12px; color: var(--text-secondary)">{{ row.displayRecoverTime || (activeTab === 'active' ? '未恢复' : '-') }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ===== 空状态 ===== -->
      <EmptyState v-if="!loading && filteredData.length === 0 && dataState === 'normal'" type="empty"
        :title="activeTab === 'active' ? '暂无当前告警' : '暂无历史告警'"
        :description="filterActive ? '当前筛选条件无匹配结果，请调整筛选条件' : ''" />
    </div>

    <!-- ===== 详情抽屉 ===== -->
    <DetailDrawer :visible="drawerVisible" title="告警详情" @close="drawerVisible = false">
      <template v-if="selectedAlarm">
        <!-- 业务信息 -->
        <h4 style="margin: 0 0 12px; color: var(--text-primary)">业务信息</h4>
        <el-descriptions :column="2" size="small" border style="margin-bottom: 16px">
          <el-descriptions-item label="FSU ID">{{ selectedAlarm.displayFsu }}</el-descriptions-item>
          <el-descriptions-item label="测点编码">{{ selectedAlarm.spid || selectedAlarm.signalId || selectedAlarm.pointCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警名称">{{ selectedAlarm.displayAlarmName }}</el-descriptions-item>
          <el-descriptions-item label="告警含义">{{ selectedAlarm.displayAlarmMeaning || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警等级"><AlarmLevelTag :level="selectedAlarm.displayAlarmLevel" /></el-descriptions-item>
          <el-descriptions-item label="告警状态"><StatusBadge :status="statusBadge(selectedAlarm.displayAlarmStatus)" :label="selectedAlarm.displayAlarmStatusLabel" /></el-descriptions-item>
          <el-descriptions-item label="告警值">{{ selectedAlarm.displayAlarmValue }}</el-descriptions-item>
          <el-descriptions-item label="发生时间" :span="2">{{ selectedAlarm.displayOccurTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="恢复时间" :span="2">{{ selectedAlarm.displayRecoverTime || '未恢复' }}</el-descriptions-item>
        </el-descriptions>

        <el-collapse style="margin-bottom: 16px">
          <el-collapse-item title="技术信息" name="tech">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="SerialNo">{{ selectedAlarm.serialNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="DeviceID">{{ selectedAlarm.deviceId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="SPID">{{ selectedAlarm.spid || '-' }}</el-descriptions-item>
              <el-descriptions-item label="SignalID">{{ selectedAlarm.signalId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="EventID">{{ selectedAlarm.eventId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="SignalName">{{ selectedAlarm.signalName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="AlarmLevel">{{ selectedAlarm.alarmLevel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="EventSeverity">{{ selectedAlarm.eventSeverity || '-' }}</el-descriptions-item>
              <el-descriptions-item label="AlarmDesc">{{ selectedAlarm.alarmDesc || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源">{{ selectedAlarm.source || '-' }}</el-descriptions-item>
              <el-descriptions-item label="映射状态">{{ mappingStatusLabel(selectedAlarm.mappingStatus) }}</el-descriptions-item>
              <el-descriptions-item label="映射置信度">{{ selectedAlarm.mappingConfidence || 'UNKNOWN' }}</el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>

        <!-- 系统信息 -->
        <h4 style="margin: 16px 0 12px; color: var(--text-secondary); font-size: 13px">系统信息</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="创建时间">{{ selectedAlarm.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ selectedAlarm.updatedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="RawXml ID" :span="2">{{ selectedAlarm.sourceMessageId || '无关联报文' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </DetailDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FilterPanel from '@/components/common/FilterPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import DetailDrawer from '@/components/common/DetailDrawer.vue'
import AlarmLevelTag from '@/components/common/AlarmLevelTag.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getAlarms } from '@/api/alarm'
import { mappingStatusLabel } from '@/utils/mappingStatus'
import {
  alarmLevelLabel,
} from '@/utils/alarmDisplay'
import {
  extractApiRows,
  normalizeBusinessAlarms,
} from '@/utils/monitorAdapters'

const loading = ref(false)
const allAlarms = ref<any[]>([])
const activeTab = ref('active')
const filterLevel = ref('')
const filterStatus = ref('')
const filterKeyword = ref('')
const drawerVisible = ref(false)
const selectedAlarm = ref<any>(null)
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'unauthorized' | 'forbidden' | 'server_error' | 'empty'>('normal')

const levels = ['CRITICAL', 'MAJOR', 'MINOR', 'WARN', 'INFO']

function levelLabel(lvl: string) {
  return alarmLevelLabel(lvl)
}

const normalizedAlarms = computed(() => normalizeBusinessAlarms(allAlarms.value))

function statusBadge(status?: string) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'ACTIVE' || normalized === 'CONFIRMED') return 'active'
  if (normalized === 'RECOVERED' || normalized === 'CLOSED' || normalized === 'CLEARED') return 'inactive'
  return 'unknown'
}

// Active = alarmStatus is ACTIVE or not CLEARED
const activeAlarms = computed(() => normalizedAlarms.value.filter(a => a.displayAlarmIsActive))

const historyAlarms = computed(() => normalizedAlarms.value.filter(a => a.displayAlarmIsHistory))

const alarmSource = computed(() => activeTab.value === 'active' ? activeAlarms.value : historyAlarms.value)

const criticalCount = computed(() => alarmSource.value.filter(a => a.displayAlarmLevel === 'CRITICAL').length)
const majorCount = computed(() => alarmSource.value.filter(a => a.displayAlarmLevel === 'MAJOR').length)
const minorCount = computed(() => alarmSource.value.filter(a => {
  const l = a.displayAlarmLevel
  return l === 'MINOR' || l === 'WARN' || l === 'INFO'
}).length)

const filterActive = computed(() => !!(filterLevel.value || filterStatus.value || filterKeyword.value))
const filteredData = computed(() => {
  let rows = alarmSource.value
  if (filterLevel.value) rows = rows.filter(r => r.displayAlarmLevel === filterLevel.value)
  if (filterStatus.value) rows = rows.filter(r => r.displayAlarmStatus === filterStatus.value)
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    rows = rows.filter(r =>
      (r.displayAlarmName || '').toLowerCase().includes(kw) ||
      (r.displayAlarmMeaning || '').toLowerCase().includes(kw) ||
      (r.alarmName || '').toLowerCase().includes(kw) ||
      (r.eventName || '').toLowerCase().includes(kw) ||
      (r.signalName || '').toLowerCase().includes(kw) ||
      (r.alarmDesc || '').toLowerCase().includes(kw) ||
      (r.pointCode || '').toLowerCase().includes(kw) ||
      (r.spid || '').toLowerCase().includes(kw) ||
      (r.eventId || '').toLowerCase().includes(kw) ||
      (r.serialNo || '').toLowerCase().includes(kw)
    )
  }
  return rows
})

function openDetail(alarm: any) {
  selectedAlarm.value = alarm
  drawerVisible.value = true
}

onMounted(async () => {
  loading.value = true
  dataState.value = 'normal'
  try {
    const res: any = await getAlarms()
    allAlarms.value = extractApiRows(res)
    if (allAlarms.value.length === 0) dataState.value = 'empty'
  } catch (e: any) {
    allAlarms.value = []
    if (e?.response?.status === 404) dataState.value = 'api_not_found'
    else if (e?.response?.status === 401) dataState.value = 'unauthorized'
    else if (e?.response?.status === 403) dataState.value = 'forbidden'
    else if (e?.response?.status && e.response.status >= 500) dataState.value = 'server_error'
    else if (e?.request && !e?.response) dataState.value = 'network_error'
    else dataState.value = 'server_error'
  } finally {
    loading.value = false
  }
})
</script>
