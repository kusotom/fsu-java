<template>
  <div class="page-template">
    <PageHeader title="站点实时数据" description="站点监控下的 FSU 点位、传感器与实时状态" />
    <SiteMonitorTabs />

    <!-- ===== 状态提示 ===== -->
    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" :meta="stateMeta" />

    <!-- ===== 筛选区 ===== -->
    <FilterPanel style="margin-bottom: 16px">
      <el-select v-model="filterFsu" placeholder="FSU" clearable size="small" style="width: 160px">
        <el-option v-for="f in fsuList" :key="f" :label="f" :value="f" />
      </el-select>
      <el-select v-model="filterType" placeholder="测点类型" clearable size="small" style="width: 140px">
        <el-option label="模拟量(AI)" value="AI" />
        <el-option label="数字量(DI)" value="DI" />
        <el-option label="控制量(DO)" value="DO" />
      </el-select>
      <el-select v-model="filterMapping" placeholder="数据状态" clearable size="small" style="width: 150px">
        <el-option label="正常" value="normal" />
        <el-option label="待确认" value="warning" />
        <el-option label="告警" value="alarm" />
        <el-option label="离线" value="offline" />
        <el-option label="历史待回填" value="legacy" />
        <el-option label="未映射" value="unmapped" />
      </el-select>
    </FilterPanel>

    <!-- ===== 指标卡片 ===== -->
    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="FSU 点位" :value="fsuPointCount" status="info" />
      <MetricCard title="实时测点" :value="realtimeSignalCount" status="normal" />
      <MetricCard title="异常测点" :value="dataAnomalyCount" :status="dataAnomalyCount > 0 ? 'warning' : 'normal'" />
      <MetricCard title="待映射" :value="unmappedCount" :status="unmappedCount > 0 ? 'warning' : 'normal'" />
    </div>

    <!-- ===== 未映射提示 ===== -->
    <DataStateAlert v-if="unmappedCount > 0" state="unmapped" :meta="{ unmappedCount }" />

    <!-- ===== 主体表格 ===== -->
    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" " :span-method="fsuSpanMethod">
        <el-table-column prop="fsuPointName" label="FSU / 点位" min-width="150" />
        <el-table-column prop="sensorName" label="测点名称" min-width="160" />
        <el-table-column label="当前值 / 状态" min-width="180">
          <template #default="{ row }">
            <span class="realtime-value-cell" :class="{ 'realtime-value-cell--status': !row.displayUnit }">
              <span class="realtime-value-main">{{ row.displayValueWithUnit }}</span>
              <span v-if="row.displayUnit" class="realtime-value-unit">{{ row.displayUnit }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" label="采集时间" width="170">
          <template #default="{ row }">
            <span style="font-size: 12px; color: var(--text-secondary)">{{ row.collectTime }}</span>
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
import { getRealtimeData } from '@/api/telemetry'
import { getSites, getFsuDevices } from '@/api/resource'
import {
  extractApiRows,
  normalizeRealtimePoints,
  enrichSiteNames,
  summarizeRealtimePoints,
  type BusinessRealtimePoint,
} from '@/utils/monitorAdapters'

const loading = ref(false)
const rawData = ref<any[]>([])
/** FE-REALTIME-SITE-NAME-BINDING-FINAL-FIX-004: fsuId → 真实站点名称 (来自站点列表) */
const siteNameByFsuId = ref<Map<number, string>>(new Map())
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

const normalizedData = computed<BusinessRealtimePoint[]>(() =>
  enrichSiteNames(normalizeRealtimePoints(rawData.value), siteNameByFsuId.value))
const realtimeSummary = computed(() => summarizeRealtimePoints(normalizedData.value))

const filteredData = computed(() => {
  let rows = normalizedData.value
  if (filterFsu.value) rows = rows.filter(r => r.fsuCode.includes(filterFsu.value))
  if (filterType.value) rows = rows.filter(r => r.pointType === filterType.value)
  if (filterMapping.value) rows = rows.filter(r => r.businessState === filterMapping.value)
  return rows
})

const pointsWithValue = computed(() => realtimeSummary.value.withValue)
const fsuPointCount = computed(() => realtimeSummary.value.fsuPointCount)
const realtimeSignalCount = computed(() => realtimeSummary.value.realtimeSignalCount)
const unmappedCount = computed(() => realtimeSummary.value.trueUnmapped)
const dataAnomalyCount = computed(() => realtimeSummary.value.anomaly)

const emptyDescription = computed(() => {
  if (filterFsu.value || filterType.value || filterMapping.value) return '当前筛选条件下无匹配数据，请调整筛选条件'
  return '暂无实时数据记录，请确认 FSU 在线并已上报数据'
})

// ===== FSU 列合并 (span-method) =====
const fsuSpanMethod = ({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) => {
  if (columnIndex !== 0) return { rowspan: 1, colspan: 1 }
  const rows = filteredData.value
  const current = rows[rowIndex]
  const prev = rows[rowIndex - 1]
  if (prev && prev.fsuPointName === current.fsuPointName) return { rowspan: 0, colspan: 0 }
  let count = 1
  for (let i = rowIndex + 1; i < rows.length; i++) {
    if (rows[i].fsuPointName === current.fsuPointName) count++
    else break
  }
  return { rowspan: count, colspan: 1 }
}

// ===== 数据加载 =====
onMounted(async () => {
  loading.value = true
  dataState.value = 'normal'
  try {
    // FE-REALTIME-SITE-NAME-BINDING-FINAL-FIX-004: 并行加载实时数据 + 站点列表 + FSU设备
    const [res, sitesRes, fsusRes] = await Promise.all([
      getRealtimeData(),
      getSites().catch(() => null),
      getFsuDevices().catch(() => null),
    ]) as any[]

    // 构建 fsuId → siteName 映射 (FsuDevice.siteId → Site.id → Site.siteName)
    const sites = extractApiRows(sitesRes)
    const fsus = extractApiRows(fsusRes)
    const siteById = new Map<number, string>()
    for (const s of sites) {
      if (s.id != null && s.siteName) siteById.set(Number(s.id), String(s.siteName))
    }
    const map = new Map<number, string>()
    for (const f of fsus) {
      if (f.id != null && f.siteId != null) {
        const name = siteById.get(Number(f.siteId))
        if (name) map.set(Number(f.id), name)
      }
    }
    siteNameByFsuId.value = map

    const data = extractApiRows(res)
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
.realtime-value-cell {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  white-space: nowrap;
}
.realtime-value-main {
  font-size: 14px;
  font-weight: 400;
  line-height: 1.2;
  color: var(--text-primary, #303133);
  letter-spacing: 0;
}
.realtime-value-unit {
  font-size: 12px;
  font-weight: 400;
  line-height: 1;
  color: var(--text-secondary, #909399);
}
.realtime-value-cell--status .realtime-value-main {}
</style>
