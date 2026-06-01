<template>
  <div>
    <PageHeader title="点位映射" description="FSU 原始 DeviceID / SignalID 与平台标准点位的映射关系" />

    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" />

    <!-- 说明 -->
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
      <template #title>点位映射说明</template>
      此页面展示 D 类假设全集下的候选映射。点位名称、类型、单位和置信度来自后端字典，ID 仅作为排障辅助字段。
    </el-alert>

    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="映射总数" :value="totalMappings" status="info" :loading="loading" />
      <MetricCard title="已映射" :value="mappedCount" status="normal" :loading="loading" />
      <MetricCard title="未映射" :value="unmappedCount" :status="unmappedCount > 0 ? 'warning' : 'normal'" :loading="loading" />
      <MetricCard title="待确认" :value="pendingCount" status="muted" :loading="loading" />
      <MetricCard title="设备待测点" :value="deviceOnlyCount" status="info" :loading="loading" />
    </div>

    <FilterPanel style="margin-bottom: 16px">
      <el-input v-model="filterDeviceId" placeholder="DeviceID" clearable size="small" style="width: 180px" />
      <el-input v-model="filterSignalId" placeholder="SignalID" clearable size="small" style="width: 180px" />
      <el-input v-model="filterKeyword" placeholder="关键字" clearable size="small" style="width: 160px" />
    </FilterPanel>

    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="fsuCode" label="FSU" width="120" />
        <el-table-column prop="deviceId" label="DeviceID" width="160" />
        <el-table-column prop="signalName" label="点位名称" min-width="170">
          <template #default="{ row }">
            <span v-if="row.signalName">{{ row.signalName }}</span>
            <span v-else style="color: var(--text-muted)">未命名点位</span>
          </template>
        </el-table-column>
        <el-table-column prop="signalId" label="SPID" width="140" />
        <el-table-column prop="pointName" label="标准点位" min-width="150">
          <template #default="{ row }">
            <span v-if="row.pointName">{{ row.pointName }}</span>
            <span v-else style="color: var(--text-muted)">待确认</span>
          </template>
        </el-table-column>
        <el-table-column prop="pointType" label="类型" width="70" />
        <el-table-column prop="unit" label="单位" width="70">
          <template #default="{ row }">
            <span v-if="row.unit">{{ row.unit }}</span>
            <span v-else style="color: var(--text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column label="映射状态" width="120">
          <template #default="{ row }">
            <StatusBadge :status="mappingStatusType(row.mappingStatus)" :label="mappingStatusLabel(row.mappingStatus)" />
          </template>
        </el-table-column>
        <el-table-column prop="mappingConfidence" label="置信度" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="confidenceTagType(row.mappingConfidence)">{{ row.mappingConfidence || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="templateVariant" label="模板" width="120">
          <template #default="{ row }">
            <span>{{ row.templateVariant || 'UNKNOWN' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && filteredData.length === 0 && dataState === 'normal'"
        type="empty" title="暂无映射数据"
        description="未获取到 D 类字典候选点位。请确认后端可读取标准信号字典，或先加载 FSU 设备列表。" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FilterPanel from '@/components/common/FilterPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getBInterfaceFsus, getBInterfaceUnmappedSignals } from '@/api/bInterface'
import {
  isMappedCandidateStatus,
  isPendingMappingStatus,
  isDeviceOnlyObservation,
  isTrueUnmappedPoint,
  hasSignalIdentity,
  mappingConfidenceTagType as confidenceTagType,
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'

const loading = ref(false)
const rawData = ref<any[]>([])
const filterDeviceId = ref('')
const filterSignalId = ref('')
const filterKeyword = ref('')
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'empty'>('normal')

const filteredData = computed(() => {
  let rows = rawData.value
  if (filterDeviceId.value) rows = rows.filter(r => (r.deviceId || '').includes(filterDeviceId.value))
  if (filterSignalId.value) rows = rows.filter(r => (r.signalId || '').includes(filterSignalId.value))
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    rows = rows.filter(r =>
      (r.deviceId || '').toLowerCase().includes(kw) ||
      (r.signalId || '').toLowerCase().includes(kw) ||
      (r.signalName || '').toLowerCase().includes(kw) ||
      (r.pointName || '').toLowerCase().includes(kw))
  }
  return rows
})

const totalMappings = computed(() => rawData.value.filter(r => hasSignalIdentity(r)).length)
const mappedCount = computed(() => rawData.value.filter(r => isMappedCandidateStatus(r.mappingStatus, r.mappingConfidence)).length)
const pendingCount = computed(() => rawData.value.filter(r => isPendingMappingStatus(r.mappingStatus)).length)
const unmappedCount = computed(() => rawData.value.filter(r => isTrueUnmappedPoint(r)).length)
const deviceOnlyCount = computed(() => rawData.value.filter(r => isDeviceOnlyObservation(r)).length)

onMounted(async () => {
  loading.value = true; dataState.value = 'normal'
  try {
    // Fetch FSU list, then devices for each
    const fsusRes: any = await getBInterfaceFsus()
    const fsus = Array.isArray(fsusRes?.data) ? fsusRes.data : (fsusRes?.data?.data || [])

    const mappings: any[] = []
    for (const fsu of fsus.slice(0, 10)) {
      try {
        const signalRes: any = await getBInterfaceUnmappedSignals({ fsuCode: fsu.fsuCode })
        const signals = Array.isArray(signalRes?.data) ? signalRes.data : (signalRes?.data?.data || [])
        for (const dev of signals) {
          const row = {
            fsuCode: fsu.fsuCode,
            deviceId: dev.deviceId || dev.DeviceID || dev.deviceCode || '-',
            signalId: dev.signalId || dev.spid || dev.rawId || '-',
            spid: dev.spid,
            rawId: dev.rawId,
            eventId: dev.eventId,
            signalName: dev.signalName || '',
            pointName: dev.pointName || dev.signalName || '',
            pointType: dev.pointType || dev.signalType || '-',
            unit: dev.unit || '',
            mappingStatus: dev.mappingStatus || 'TEMPLATE_ONLY',
            mappingConfidence: dev.mappingConfidence || 'UNKNOWN',
            templateVariant: dev.templateVariant || 'UNKNOWN',
            reason: dev.reason,
            source: dev.source || dev.rawCommand,
            rawCommand: dev.rawCommand,
            hasSignalIdentity: dev.hasSignalIdentity,
            isDeviceOnly: dev.isDeviceOnly,
          }
          if (isDeviceOnlyObservation(row) || row.isDeviceOnly === true) {
            row.mappingStatus = 'DEVICE_ONLY'
          } else if (!hasSignalIdentity(row)) {
            row.mappingStatus = 'SIGNAL_PENDING'
          }
          mappings.push({
            ...row,
          })
        }
      } catch { /* skip */ }
    }

    rawData.value = mappings
    if (mappings.length === 0) dataState.value = 'empty'
  } catch (e: any) {
    rawData.value = []
    if (e?.response?.status === 404) dataState.value = 'api_not_found'
    else if (e?.request && !e?.response) dataState.value = 'network_error'
  } finally { loading.value = false }
})

</script>
