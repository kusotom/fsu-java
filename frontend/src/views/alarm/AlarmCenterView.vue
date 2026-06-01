<template>
  <div>
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
        <el-option label="已恢复" value="CLEARED" />
        <el-option label="已确认" value="CONFIRMED" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="关键字" clearable size="small" style="width: 160px" />
    </FilterPanel>

    <!-- ===== 主体表格 ===== -->
    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="fsuId" label="FSU" width="100">
          <template #default="{ row }"><span style="font-size: 12px">{{ row.fsuId || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="alarmName" label="告警名称" min-width="160">
          <template #default="{ row }">
            <span>{{ row.eventName || row.alarmName || row.signalName || '未命名告警' }}</span>
            <el-tag v-if="row.needRealDataConfirm" size="small" type="warning" style="margin-left: 6px">待确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmMeaning" label="告警含义" min-width="120">
          <template #default="{ row }">
            <span v-if="row.alarmMeaning">{{ row.alarmMeaning }}</span>
            <span v-else style="color: var(--text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmLevel" label="等级" width="90">
          <template #default="{ row }"><AlarmLevelTag :level="row.alarmLevel" /></template>
        </el-table-column>
        <el-table-column prop="alarmStatus" label="状态" width="80">
          <template #default="{ row }"><StatusBadge :status="row.alarmStatus" /></template>
        </el-table-column>
        <el-table-column prop="alarmValue" label="告警值" width="100" :show-overflow-tooltip="true" />
        <el-table-column prop="occurTime" label="发生时间" width="160">
          <template #default="{ row }"><span style="font-size: 12px; color: var(--text-secondary)">{{ row.occurTime || row.alarmTime || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="clearTime" label="恢复时间" width="160">
          <template #default="{ row }"><span style="font-size: 12px; color: var(--text-secondary)">{{ row.clearTime || row.recoveryTime || row.recoverTime || (activeTab === 'active' ? '未恢复' : '-') }}</span></template>
        </el-table-column>
        <el-table-column label="映射" width="120">
          <template #default="{ row }">
            <StatusBadge :status="mappingStatusType(row.mappingStatus)" :label="mappingStatusLabel(row.mappingStatus)" />
          </template>
        </el-table-column>
        <el-table-column prop="mappingConfidence" label="置信度" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="confidenceTagType(row.mappingConfidence)">{{ row.mappingConfidence || 'UNKNOWN' }}</el-tag>
          </template>
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
          <el-descriptions-item label="FSU ID">{{ selectedAlarm.fsuId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="点位编码">{{ selectedAlarm.pointCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警名称">{{ selectedAlarm.eventName || selectedAlarm.alarmName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警含义">{{ selectedAlarm.alarmMeaning || '-' }}</el-descriptions-item>
          <el-descriptions-item label="告警等级"><AlarmLevelTag :level="selectedAlarm.alarmLevel" /></el-descriptions-item>
          <el-descriptions-item label="告警状态"><StatusBadge :status="selectedAlarm.alarmStatus" /></el-descriptions-item>
          <el-descriptions-item label="告警值">{{ selectedAlarm.alarmValue || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发生时间" :span="2">{{ selectedAlarm.occurTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="恢复时间" :span="2">{{ selectedAlarm.clearTime || '未恢复' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 协议字段 -->
        <h4 style="margin: 16px 0 12px; color: var(--text-secondary); font-size: 13px">协议字段 (B接口 SEND_ALARM)</h4>
        <el-descriptions :column="2" size="small" border style="margin-bottom: 16px">
          <el-descriptions-item label="SerialNo">{{ selectedAlarm.serialNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="DeviceID">{{ selectedAlarm.deviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SPID">{{ selectedAlarm.spid || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SignalID">{{ selectedAlarm.signalId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="EventID">{{ selectedAlarm.eventId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SignalName">{{ selectedAlarm.signalName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AlarmLevel">{{ selectedAlarm.alarmLevel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="AlarmDesc">{{ selectedAlarm.alarmDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ selectedAlarm.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="映射状态">{{ mappingStatusLabel(selectedAlarm.mappingStatus) }}</el-descriptions-item>
          <el-descriptions-item label="映射置信度">{{ selectedAlarm.mappingConfidence || 'UNKNOWN' }}</el-descriptions-item>
        </el-descriptions>

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
import {
  mappingConfidenceTagType as confidenceTagType,
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'

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
  const map: Record<string, string> = { CRITICAL: '一级', MAJOR: '二级', MINOR: '三级', WARN: '四级', INFO: '提示' }
  return map[lvl] || lvl
}

// Active = alarmStatus is ACTIVE or not CLEARED
const activeAlarms = computed(() => allAlarms.value.filter(a => {
  const s = (a.alarmStatus || '').toUpperCase()
  return s === 'ACTIVE' || s === 'CONFIRMED' || s === 'NEW'
}))

const historyAlarms = computed(() => allAlarms.value.filter(a => {
  const s = (a.alarmStatus || '').toUpperCase()
  return s === 'CLEARED' || s === 'CLOSED' || s === 'RECOVERED'
}))

const alarmSource = computed(() => activeTab.value === 'active' ? activeAlarms.value : historyAlarms.value)

const criticalCount = computed(() => alarmSource.value.filter(a => (a.alarmLevel || '').toUpperCase() === 'CRITICAL').length)
const majorCount = computed(() => alarmSource.value.filter(a => (a.alarmLevel || '').toUpperCase() === 'MAJOR').length)
const minorCount = computed(() => alarmSource.value.filter(a => {
  const l = (a.alarmLevel || '').toUpperCase()
  return l === 'MINOR' || l === 'WARN' || l === 'INFO'
}).length)

const filterActive = computed(() => !!(filterLevel.value || filterStatus.value || filterKeyword.value))
const filteredData = computed(() => {
  let rows = alarmSource.value
  if (filterLevel.value) rows = rows.filter(r => (r.alarmLevel || '').toUpperCase() === filterLevel.value.toUpperCase())
  if (filterStatus.value) rows = rows.filter(r => (r.alarmStatus || '').toUpperCase() === filterStatus.value.toUpperCase())
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    rows = rows.filter(r =>
      (r.alarmName || '').toLowerCase().includes(kw) ||
      (r.eventName || '').toLowerCase().includes(kw) ||
      (r.signalName || '').toLowerCase().includes(kw) ||
      (r.alarmDesc || '').toLowerCase().includes(kw) ||
      (r.pointCode || '').toLowerCase().includes(kw)
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
    allAlarms.value = Array.isArray(res?.data) ? res.data : (res?.data?.data || [])
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
