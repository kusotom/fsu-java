<template>
  <div>
    <PageHeader title="B接口告警增强" description="按 FSU → Device → Signal 三层展示告警，适配 B接口2016 SEND_ALARM" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>按后端字典映射结果展示</template>
      告警名称、含义、映射状态和置信度来自后端 eStoneII-IO 字典映射。UNMAPPED / UNKNOWN_EVENT_ID 才代表真实未映射或未知事件。
    </el-alert>

    <ApiErrorAlert v-if="error" :error="error" :warnings="warnings" :auditId="auditId" />

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="section">
      <el-col :span="4"><el-statistic title="当前告警" :value="alarmTotal" /></el-col>
      <el-col :span="4"><el-statistic title="一级告警" :value="criticalCount"><template #suffix><AlarmLevelTag level="CRITICAL" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="二级告警" :value="majorCount"><template #suffix><AlarmLevelTag level="MAJOR" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="未映射" :value="unmappedCount"><template #suffix><StatusBadge status="unmapped" /></template></el-statistic></el-col>
    </el-row>

    <!-- 筛选区 -->
    <el-card class="section">
      <el-row :gutter="12" align="middle">
        <el-col :span="5"><el-input v-model="filterFsuCode" placeholder="FSUCode" clearable size="small" /></el-col>
        <el-col :span="5"><el-input v-model="filterDeviceId" placeholder="DeviceID" clearable size="small" /></el-col>
        <el-col :span="4">
          <el-select v-model="filterAlarmLevel" placeholder="告警级别" clearable size="small" style="width:100%">
            <el-option label="一级告警" value="CRITICAL" /><el-option label="二级告警" value="MAJOR" /><el-option label="三级告警" value="MINOR" /><el-option label="四级告警" value="WARN" /><el-option label="提示" value="INFO" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterAlarmStatus" placeholder="状态" clearable size="small" style="width:100%">
            <el-option label="活跃" value="ACTIVE" /><el-option label="已恢复" value="RECOVERED" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterMappingStatus" placeholder="映射" clearable size="small" style="width:100%">
            <el-option label="未映射" value="unmapped" /><el-option label="已映射/候选" value="mapped" />
          </el-select>
        </el-col>
        <el-col :span="2"><el-button size="small" :loading="loading" @click="fetchData"><el-icon><Refresh /></el-icon></el-button></el-col>
      </el-row>
    </el-card>

    <!-- 当前告警表格 -->
    <el-card header="当前告警" class="section">
      <DataTable :data="filteredAlarms" :loading="loading" :error="error" @refresh="fetchData">
        <el-table-column label="FSU" width="120">
          <template #default="{row}">{{ row.fsuCode || row.fsuId || '-' }}</template>
        </el-table-column>
        <el-table-column label="DeviceID" width="170">
          <template #default="{row}">{{ row.deviceId || '-' }}</template>
        </el-table-column>
        <el-table-column label="SPID" width="120">
          <template #default="{row}">{{ row.spid || row.signalId || row.eventId || row.pointCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="serialNo" label="序列号" width="130"><template #default="{row}">{{ row.serialNo || '-' }}</template></el-table-column>
        <el-table-column prop="alarmLevel" label="级别" width="80">
          <template #default="{row}"><AlarmLevelTag :level="row.displayAlarmLevel" /></template>
        </el-table-column>
        <el-table-column prop="alarmStatus" label="状态" width="80">
          <template #default="{row}"><StatusBadge :status="statusBadge(row.displayAlarmStatus)" :label="row.displayAlarmStatusLabel" /></template>
        </el-table-column>
        <el-table-column label="告警名称/描述" min-width="180" show-overflow-tooltip>
          <template #default="{row}">{{ row.displayAlarmName || row.displayAlarmMeaning || '-' }}</template>
        </el-table-column>
        <el-table-column label="发生时间" width="160">
          <template #default="{row}">{{ row.displayOccurTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="映射" width="130">
          <template #default="{row}">
            <StatusBadge :status="mappingStatusType(row.mappingStatus)" :label="mappingStatusLabel(row.mappingStatus, row.mappingConfidence)" />
          </template>
        </el-table-column>
        <el-table-column prop="mappingConfidence" label="置信度" width="110">
          <template #default="{row}">
            <el-tag size="small" :type="confidenceTagType(row.mappingConfidence)">{{ row.mappingConfidence || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100"><template #default>SEND_ALARM</template></el-table-column>
      </DataTable>
    </el-card>

    <!-- ActiveAlarmDiff 一致性审计区 -->
    <el-card header="活动告警一致性审计" class="section">
      <EmptyState type="not-implemented" title="活动告警一致性审计接口未接入"
        description="后端 BIF-P4-020/BIF-P4-021 已具备 ActiveAlarmConsistencyAuditService + Scheduler 安全门禁框架。REST Controller 待后续新增。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/common/DataTable.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import AlarmLevelTag from '@/components/common/AlarmLevelTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import { getBInterfaceAlarms } from '@/api/bInterface'
import type { ApiError, ApiWarning } from '@/types/api'
import {
  isMappedCandidateStatus,
  isUnmappedMappingStatus,
  mappingConfidenceTagType as confidenceTagType,
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'
import { normalizeAlarmRow } from '@/utils/alarmDisplay'

const loading = ref(false)
const error = ref<ApiError | null>(null)
const warnings = ref<Array<string | ApiWarning>>([])
const auditId = ref<string>('')
const alarms = ref<any[]>([])
const filterFsuCode = ref('')
const filterDeviceId = ref('')
const filterAlarmLevel = ref('')
const filterAlarmStatus = ref('')
const filterMappingStatus = ref('')

const alarmTotal = computed(() => filteredAlarms.value.length)
const criticalCount = computed(() => filteredAlarms.value.filter(a => a.displayAlarmLevel === 'CRITICAL').length)
const majorCount = computed(() => filteredAlarms.value.filter(a => a.displayAlarmLevel === 'MAJOR').length)
const unmappedCount = computed(() => filteredAlarms.value.filter(a => isUnmappedMappingStatus(a.mappingStatus)).length)

const filteredAlarms = computed(() => {
  let list = alarms.value
  if (filterFsuCode.value) list = list.filter(a => (a.fsuCode || a.fsuId || '').includes(filterFsuCode.value))
  if (filterDeviceId.value) list = list.filter(a => (a.deviceId || '').includes(filterDeviceId.value))
  if (filterAlarmLevel.value) list = list.filter(a => a.displayAlarmLevel === filterAlarmLevel.value)
  if (filterAlarmStatus.value) list = list.filter(a => a.displayAlarmStatus === filterAlarmStatus.value)
  if (filterMappingStatus.value === 'unmapped') list = list.filter(a => isUnmappedMappingStatus(a.mappingStatus))
  if (filterMappingStatus.value === 'mapped') list = list.filter(a => isMappedCandidateStatus(a.mappingStatus, a.mappingConfidence))
  return list
})

function statusBadge(status?: string) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'ACTIVE' || normalized === 'CONFIRMED') return 'active'
  if (normalized === 'RECOVERED' || normalized === 'CLOSED' || normalized === 'CLEARED') return 'inactive'
  return 'unknown'
}

async function fetchData() {
  loading.value = true; error.value = null
  try {
    const res: any = await getBInterfaceAlarms()
    const rows = Array.isArray(res?.data) ? res.data : (res?.data?.data || [])
    alarms.value = rows.map((row: any) => normalizeAlarmRow(row))
  }
  catch { error.value = { code: 'load_failed', message: '加载告警失败' } }
  finally { loading.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
