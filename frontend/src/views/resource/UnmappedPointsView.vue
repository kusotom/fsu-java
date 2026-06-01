<template>
  <div>
    <PageHeader title="未映射点位" description="FSU 返回但尚未关联平台标准点位的 DeviceID / SignalID 清单" />

    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" />

    <!-- 说明 -->
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
      <template #title>未映射与待确认点位</template>
      当前展示后端返回的未映射观测记录、D 类假设全集候选，以及设备发现但尚未返回测点的记录。只有具备 SignalID / SPID / rawId 且状态为 UNMAPPED 的记录才计入真实未映射点位。
    </el-alert>

    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="待复核项" :value="totalCount" :status="totalCount > 0 ? 'warning' : 'normal'" :loading="loading" />
      <MetricCard title="真实未映射" :value="actualUnmappedCount" :status="actualUnmappedCount > 0 ? 'warning' : 'normal'" :loading="loading" />
      <MetricCard title="未知告警事件" :value="unknownEventCount" :status="unknownEventCount > 0 ? 'warning' : 'normal'" :loading="loading" />
      <MetricCard title="设备待测点" :value="deviceOnlyCount" status="info" :loading="loading" />
      <MetricCard title="模板/待确认" :value="templateOrPendingCount" status="muted" :loading="loading" />
    </div>

    <FilterPanel style="margin-bottom: 16px">
      <el-select v-model="filterFsu" placeholder="FSU" clearable size="small" style="width: 180px">
        <el-option v-for="f in fsuList" :key="f" :label="f" :value="f" />
      </el-select>
      <el-input v-model="filterDeviceId" placeholder="DeviceID" clearable size="small" style="width: 180px" />
      <el-input v-model="filterKeyword" placeholder="SignalID / 关键字" clearable size="small" style="width: 180px" />
    </FilterPanel>

    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="fsuCode" label="FSU" width="130" />
        <el-table-column prop="deviceId" label="DeviceID" width="180" />
        <el-table-column prop="rawName" label="点位名称" min-width="170">
          <template #default="{ row }"><span v-if="row.rawName">{{ row.rawName }}</span><span v-else style="color: var(--text-muted)">未命名点位</span></template>
        </el-table-column>
        <el-table-column prop="signalId" label="SPID" width="140" />
        <el-table-column prop="signalType" label="类型" width="80" />
        <el-table-column prop="unit" label="单位" width="90">
          <template #default="{ row }">
            <span v-if="row.unit">{{ row.unit }}</span>
            <span v-else style="color: var(--text-muted)">待确认</span>
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
        <el-table-column prop="reason" label="原因" min-width="160">
          <template #default="{ row }">
            <span>{{ reasonLabel(row.reason) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rawValue" label="原始值" width="100">
          <template #default="{ row }">
            <span v-if="row.rawValue !== null && row.rawValue !== undefined && row.rawValue !== ''">{{ row.rawValue }}</span>
            <span v-else style="color: var(--text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="110">
          <template #default="{ row }">
            <span>{{ row.source || row.rawCommand || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && filteredData.length === 0 && dataState === 'normal'"
        type="empty" title="暂无未映射点位"
        description="所有已发现的 FSU 点位均已建立映射关系，或 FSU 尚未返回新的 DeviceID / SignalID。现场联调后如有新增信号，将在此页面展示。" />
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
  isPendingMappingStatus,
  isDeviceOnlyObservation,
  isTrueUnmappedPoint,
  isUnknownEventMappingStatus,
  hasSignalIdentity,
  mappingConfidenceTagType as confidenceTagType,
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'

const loading = ref(false)
const rawData = ref<any[]>([])
const fsuList = ref<string[]>([])
const filterFsu = ref('')
const filterDeviceId = ref('')
const filterKeyword = ref('')
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'empty'>('normal')

const filteredData = computed(() => {
  let rows = rawData.value
  if (filterFsu.value) rows = rows.filter(r => r.fsuCode === filterFsu.value)
  if (filterDeviceId.value) rows = rows.filter(r => (r.deviceId || '').includes(filterDeviceId.value))
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    rows = rows.filter(r =>
      (r.signalId || '').toLowerCase().includes(kw) ||
      (r.deviceId || '').toLowerCase().includes(kw) ||
      (r.rawName || '').toLowerCase().includes(kw) ||
      (r.reason || '').toLowerCase().includes(kw))
  }
  return rows
})

const totalCount = computed(() => rawData.value.length)
const actualUnmappedCount = computed(() => rawData.value.filter(r => isTrueUnmappedPoint(r)).length)
const unknownEventCount = computed(() => rawData.value.filter(r => isUnknownEventMappingStatus(r.mappingStatus)).length)
const deviceOnlyCount = computed(() => rawData.value.filter(r => isDeviceOnlyObservation(r)).length)
const templateOrPendingCount = computed(() => rawData.value.filter(r => isPendingMappingStatus(r.mappingStatus)).length)

onMounted(async () => {
  loading.value = true; dataState.value = 'normal'
  try {
    const fsusRes: any = await getBInterfaceFsus()
    const fsus = Array.isArray(fsusRes?.data) ? fsusRes.data : (fsusRes?.data?.data || [])
    fsuList.value = fsus.map((f: any) => f.fsuCode).filter(Boolean)

    const unmapped: any[] = []
    for (const fsu of fsus.slice(0, 10)) {
      try {
        const signalRes: any = await getBInterfaceUnmappedSignals({ fsuCode: fsu.fsuCode })
        const devices = Array.isArray(signalRes?.data) ? signalRes.data : (signalRes?.data?.data || [])
        for (const dev of devices) {
          const normalized = normalizeUnmappedRow(dev, fsu.fsuCode)
          unmapped.push({
            ...normalized,
          })
        }
      } catch { /* skip */ }
    }

    rawData.value = unmapped
    if (unmapped.length === 0) dataState.value = 'empty'
  } catch (e: any) {
    rawData.value = []
    if (e?.response?.status === 404) dataState.value = 'api_not_found'
    else if (e?.request && !e?.response) dataState.value = 'network_error'
  } finally { loading.value = false }
})

function reasonLabel(reason: string) {
  const map: Record<string, string> = {
    UNKNOWN_SIGNAL_ID: '未知 SignalID',
    UNKNOWN_DEVICE_ID: '未知 DeviceID',
    UNKNOWN_DEVICE_SIGNAL_PAIR: '设备与点位组合未确认',
    UNKNOWN_EVENT_ID: '未知 EventID',
    TEMPLATE_ONLY_NOT_RETURNED: '模板点位未真实返回',
    PARSE_ERROR: '解析异常',
    D_CLASS_ASSUMED_TEMPLATE_CANDIDATE: 'D 类假设全集候选',
    DEVICE_DISCOVERED_WAIT_GET_DATA: '设备已发现，待测点上报',
    SIGNAL_PENDING: '等待测点上报',
    MISSING_SIGNAL_MAPPING: '设备级记录，缺少测点编号',
  }
  return map[(reason || '').toUpperCase()] || reason || '未知原因'
}

function normalizeUnmappedRow(dev: any, fallbackFsuCode: string) {
  const row = {
    fsuCode: dev.fsuCode || fallbackFsuCode,
    deviceId: dev.deviceId || dev.DeviceID || dev.deviceCode || '-',
    signalId: dev.signalId || dev.spid || dev.eventId || dev.rawId || '-',
    spid: dev.spid,
    rawId: dev.rawId,
    eventId: dev.eventId,
    rawName: dev.rawName || dev.signalName || '',
    signalType: dev.signalType || '-',
    unit: dev.unit || '',
    mappingStatus: dev.mappingStatus || 'UNMAPPED',
    mappingConfidence: dev.mappingConfidence || 'UNKNOWN',
    templateVariant: dev.templateVariant || 'UNKNOWN',
    reason: dev.reason || 'UNKNOWN_DEVICE_SIGNAL_PAIR',
    rawValue: dev.rawValue,
    source: dev.source || dev.rawCommand,
    rawCommand: dev.rawCommand,
    hasSignalIdentity: dev.hasSignalIdentity,
    isDeviceOnly: dev.isDeviceOnly,
    observationType: dev.observationType,
  }
  if (isDeviceOnlyObservation(row) || row.isDeviceOnly === true) {
    row.mappingStatus = 'DEVICE_ONLY'
    row.reason = row.reason === 'missing_signal_mapping' ? 'DEVICE_DISCOVERED_WAIT_GET_DATA' : row.reason
  } else if (!hasSignalIdentity(row)) {
    row.mappingStatus = 'SIGNAL_PENDING'
    row.reason = row.reason || 'SIGNAL_PENDING'
  }
  return row
}
</script>
