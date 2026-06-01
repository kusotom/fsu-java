<template>
  <div>
    <PageHeader title="站点实时数据" description="站点监控下的设备点位实时状态、采集时间、映射状态和数据质量" />
    <SiteMonitorTabs />

    <!-- ===== 状态提示 ===== -->
    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" :meta="stateMeta" />

    <!-- ===== 筛选区 ===== -->
    <FilterPanel style="margin-bottom: 16px">
      <el-select v-model="filterFsu" placeholder="FSU" clearable size="small" style="width: 160px">
        <el-option v-for="f in fsuList" :key="f" :label="f" :value="f" />
      </el-select>
      <el-select v-model="filterType" placeholder="点位类型" clearable size="small" style="width: 140px">
        <el-option label="模拟量(AI)" value="AI" />
        <el-option label="数字量(DI)" value="DI" />
        <el-option label="控制量(DO)" value="DO" />
      </el-select>
      <el-select v-model="filterMapping" placeholder="映射状态" clearable size="small" style="width: 140px">
        <el-option label="候选映射" value="MAPPED_CANDIDATE" />
        <el-option label="模板候选" value="TEMPLATE_ONLY" />
        <el-option label="待真实确认" value="PENDING_REAL_DATA" />
        <el-option label="未映射" value="UNMAPPED" />
      </el-select>
    </FilterPanel>

    <!-- ===== 指标卡片 ===== -->
    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="实时点位" :value="totalPoints" status="info" />
      <MetricCard title="有值点位" :value="pointsWithValue" status="normal" />
      <MetricCard title="真实未映射" :value="unmappedCount" :status="unmappedCount > 0 ? 'warning' : 'normal'" />
      <MetricCard title="历史待回填" :value="historicalBackfillCount" :status="historicalBackfillCount > 0 ? 'warning' : 'normal'" />
      <MetricCard title="解析异常" :value="parseErrorCount" :status="parseErrorCount > 0 ? 'danger' : 'normal'" />
    </div>

    <!-- ===== 未映射提示 ===== -->
    <DataStateAlert v-if="unmappedCount > 0" state="unmapped" :meta="{ unmappedCount }" />

    <!-- ===== 主体表格 ===== -->
    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="fsuCode" label="FSU" width="120" />
        <el-table-column prop="deviceId" label="设备" width="140">
          <template #default="{ row }">
            <span v-if="row.deviceId">{{ row.deviceId }}</span>
            <span v-else style="color: var(--text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="pointName" label="点位名称" min-width="160">
          <template #default="{ row }">
            <span v-if="row.pointName">{{ row.pointName }}</span>
            <span v-else style="color: var(--text-muted)">未命名点位</span>
            <el-tag v-if="row.needRealDataConfirm" size="small" type="warning" style="margin-left: 6px">待确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentValue" label="当前值" width="130" align="right">
          <template #default="{ row }">
            <span v-if="row.currentValue !== null && row.currentValue !== undefined" class="point-value">
              {{ formatValue(row.currentValue) }}
            </span>
            <span v-else style="color: var(--text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="valueMeaning" label="值含义" min-width="120">
          <template #default="{ row }">
            <span v-if="row.valueMeaning">{{ row.valueMeaning }}</span>
            <span v-else style="color: var(--text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80">
          <template #default="{ row }">
            <span v-if="row.unit">{{ row.unit }}</span>
            <span v-else style="color: var(--text-muted); font-size: 12px">待确认</span>
          </template>
        </el-table-column>
        <el-table-column prop="pointType" label="类型" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.pointType === 'DI' ? 'info' : ''">{{ row.pointType || '-' }}</el-tag>
            <el-tooltip v-if="row.pointType === 'DI'" content="DI 编码待确认，请勿硬编码 0/1 语义" placement="top">
              <span style="color: var(--status-warning); font-size: 11px; margin-left: 2px">?</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="valueStatus" label="质量" width="80">
          <template #default="{ row }">
            <StatusBadge :status="row.valueStatus || 'NORMAL'" />
          </template>
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
        <el-table-column prop="source" label="来源" width="150">
          <template #default="{ row }">
            <span>{{ row.source || row.templateVariant || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" label="采集时间" width="160">
          <template #default="{ row }">
            <span v-if="row.collectTime" style="font-size: 12px; color: var(--text-secondary)">{{ row.collectTime }}</span>
            <span v-else style="color: var(--text-muted); font-size: 12px">--</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- ===== 空状态 ===== -->
      <EmptyState v-if="!loading && filteredData.length === 0 && dataState === 'normal'" type="empty" title="暂无实时数据" :description="emptyDescription" />
      <EmptyState v-else-if="!loading && filteredData.length === 0 && dataState === 'empty'" type="empty" title="暂无实时数据" description="HTTP 200 成功但 data 为空。FSU 可能尚未上报测点值。" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import SiteMonitorTabs from '@/components/SiteMonitorTabs.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FilterPanel from '@/components/common/FilterPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getRealtimeData } from '@/api/telemetry'
import {
  isHistoricalRealtimeRow,
  isTrueUnmappedPoint,
  mappingConfidenceTagType as confidenceTagType,
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'

const loading = ref(false)
const rawData = ref<any[]>([])
const fsuList = ref<string[]>([])
const filterFsu = ref('')
const filterType = ref('')
const filterMapping = ref('')
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'unauthorized' | 'forbidden' | 'server_error' | 'empty' | 'ack_empty' | 'parse_error' | 'unmapped'>('normal')

const stateMeta = computed(() => ({
  unmappedCount: unmappedCount.value,
  valuesReturned: pointsWithValue.value,
  errorMessage: '',
}))

// ===== 数据归一化 =====
interface PointRow {
  fsuCode: string; deviceId: string; pointCode: string; signalId: string;
  pointName: string; currentValue: number | string | null; unit: string;
  valueMeaning: string; pointType: string; valueStatus: string; quality: string;
  collectTime: string; receiveTime: string; mappingStatus: string;
  mappingConfidence: string; templateVariant: string; needRealDataConfirm: boolean;
  source: string;
}

const normalizedData = computed<PointRow[]>(() => {
  return rawData.value.map((d: any): PointRow => {
    const mappedStatus = normalizeRealtimeStatus(d)
    return {
    fsuCode: d.fsuCode || (d.fsuId ? `FSU-${d.fsuId}` : '-'),
    deviceId: d.deviceId || d.deviceCode || '-',
    pointCode: d.pointCode || d.point_code || '',
    signalId: d.signalId || '',
    pointName: d.pointName || d.point_name || d.signalName || '',
    currentValue: d.valueNumber ?? d.valueText ?? d.value ?? d.measuredVal ?? null,
    unit: d.unit || '',
    valueMeaning: d.valueMeaning || '',
    pointType: d.pointType || d.point_type || d.dataType || '',
    valueStatus: d.valueStatus || d.status || 'NORMAL',
    quality: d.quality || '',
    collectTime: d.collectTime || d.collect_time || '',
    receiveTime: d.receiveTime || d.receive_time || '',
    mappingStatus: mappedStatus,
    mappingConfidence: d.mappingConfidence || 'UNKNOWN',
    templateVariant: d.templateVariant || 'UNKNOWN',
    needRealDataConfirm: d.needRealDataConfirm === true,
    source: d.source || (mappedStatus === 'HISTORICAL_PENDING_BACKFILL' ? 'legacy_realtime_data' : ''),
  }
  })
})

const filteredData = computed(() => {
  let rows = normalizedData.value
  if (filterFsu.value) rows = rows.filter(r => r.fsuCode.includes(filterFsu.value))
  if (filterType.value) rows = rows.filter(r => r.pointType === filterType.value)
  if (filterMapping.value) rows = rows.filter(r => (r.mappingStatus || '').toUpperCase() === filterMapping.value)
  return rows
})

const totalPoints = computed(() => normalizedData.value.length)
const pointsWithValue = computed(() => normalizedData.value.filter(r => r.currentValue !== null && r.currentValue !== undefined).length)
const unmappedCount = computed(() => normalizedData.value.filter(r => isTrueUnmappedPoint(r as any)).length)
const historicalBackfillCount = computed(() => normalizedData.value.filter(r => isHistoricalRealtimeRow(r as any)).length)
const parseErrorCount = computed(() => 0) // populated when API returns parseError meta

const emptyDescription = computed(() => {
  if (filterFsu.value || filterType.value || filterMapping.value) return '当前筛选条件下无匹配数据，请调整筛选条件'
  return '暂无实时数据记录，请确认 FSU 在线并已上报数据'
})

function formatValue(v: number | string | null): string {
  if (v === null || v === undefined || v === '') return '--'
  if (typeof v === 'number') {
    if (Number.isInteger(v)) return String(v)
    return v.toFixed(2)
  }
  const n = Number(v)
  if (!isNaN(n)) {
    if (Number.isInteger(n)) return String(n)
    return n.toFixed(2)
  }
  return String(v)
}

function normalizeRealtimeStatus(d: any): string {
  if (d.mappingStatus) return d.mappingStatus
  if (isHistoricalRealtimeRow(d)) return 'HISTORICAL_PENDING_BACKFILL'
  if (d.signalName || d.pointName || d.point_name) return 'TEMPLATE_ONLY'
  return 'HISTORICAL_PENDING_BACKFILL'
}

// ===== 数据加载 =====
onMounted(async () => {
  loading.value = true
  dataState.value = 'normal'
  try {
    const res: any = await getRealtimeData()
    const data = Array.isArray(res) ? res : (res?.data || res?.data?.data || [])
    rawData.value = data

    // 提取 FSU 列表
    fsuList.value = [...new Set(data.map((d: any) => d.fsuCode || (d.fsuId ? `FSU-${d.fsuId}` : null)).filter(Boolean))] as string[]

    if (data.length === 0) dataState.value = 'empty'

    // 检查 meta 状态
    const meta = res?.meta || res?.data?.meta
    if (meta?.parseError) dataState.value = 'parse_error'
    else if (meta?.ackReceived && meta?.emptyData) dataState.value = 'ack_empty'
  } catch (e: any) {
    rawData.value = []
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

<style scoped>
.point-value { font-size: 16px; font-weight: 600; font-variant-numeric: tabular-nums; color: var(--text-primary); }
</style>
