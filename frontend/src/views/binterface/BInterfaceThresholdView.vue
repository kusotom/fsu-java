<template>
  <div>
    <PageHeader title="B接口门限只读" description="按 FSU → Device → Signal 三层展示门限数据 (GET_THRESHOLD 2016 Code=1901)" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>门限只读接口未接入</template>
      GET_THRESHOLD 真实点位联调仍处于 <strong>BLOCKED</strong> 状态。下方仅展示后端候选设备映射状态，不代表真实门限已返回。
    </el-alert>
    <ApiErrorAlert v-if="error" :error="error" />

    <!-- 统计 -->
    <el-row :gutter="16" class="section">
      <el-col :span="6"><el-statistic title="门限总数" :value="thresholdData.length"><template #suffix><el-tag size="small" type="info">{{thresholdData.length}}</el-tag></template></el-statistic></el-col>
      <el-col :span="6"><el-statistic title="候选设备" :value="mappedDeviceCount"><template #suffix><StatusBadge status="mapped" /></template></el-statistic></el-col>
      <el-col :span="6"><el-statistic title="未映射" :value="unmappedDeviceCount"><template #suffix><StatusBadge status="unmapped" /></template></el-statistic></el-col>
      <el-col :span="6"><el-statistic title="最近 GET_THRESHOLD" value="-" /></el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="section">
      <el-row :gutter="12" align="middle">
        <el-col :span="5"><el-input v-model="filterFsuCode" placeholder="FSUCode" clearable size="small" /></el-col>
        <el-col :span="5"><el-input v-model="filterDeviceId" placeholder="DeviceID" clearable size="small" /></el-col>
        <el-col :span="5"><el-input v-model="filterSignalId" placeholder="SPID/SignalID" clearable size="small" /></el-col>
        <el-col :span="4">
          <el-select v-model="filterMappingStatus" placeholder="映射" clearable size="small" style="width:100%">
            <el-option label="已映射/候选" value="mapped" /><el-option label="未映射" value="unmapped" />
          </el-select>
        </el-col>
        <el-col :span="3"><el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width:100%">
          <el-option label="启用" value="ENABLED" /><el-option label="禁用" value="DISABLED" />
        </el-select></el-col>
        <el-col :span="2"><el-button size="small" :loading="loading" @click="refresh"><el-icon><Refresh /></el-icon></el-button></el-col>
      </el-row>
    </el-card>

    <!-- 门限表格 -->
    <el-card header="门限列表" class="section">
      <EmptyState type="not-implemented" title="B接口门限接口未接入"
        description="后端提供 GET /b-interface/thresholds 接口后可展示真实门限数据。当前可展示已知 DeviceID。" />
      <el-table v-if="filteredDeviceData.length" :data="filteredDeviceData" size="small" stripe style="margin-top:12px">
        <el-table-column prop="fsuCode" label="FSUCode" width="170" />
        <el-table-column prop="deviceId" label="DeviceID" width="200" />
        <el-table-column prop="deviceCode" label="DeviceCode" width="120" />
        <el-table-column label="SPID" width="120"><template #default>-</template></el-table-column>
        <el-table-column label="Threshold" width="100"><template #default>-</template></el-table-column>
        <el-table-column label="映射" width="120">
          <template #default="scope">
            <StatusBadge :status="mappingStatusType(scope.row.mappingStatus)" :label="mappingStatusLabel(scope.row.mappingStatus)" />
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="130" />
      </el-table>
    </el-card>

    <!-- GET_THRESHOLD run-once 占位 -->
    <PermissionGuard :permissions="['protocol:runonce:readonly']">
    <el-card header="GET_THRESHOLD run-once" class="section">
      <EmptyState type="not-implemented" title="GET_THRESHOLD run-once 接口未接入" />
    </el-card>
    </PermissionGuard>

    <!-- SET_THRESHOLD 禁用占位 -->
    <PermissionGuard :permissions="['binterface.set_threshold.plan', 'binterface.set_threshold.execute']" mode="disable" disabledText="无SET权限 — SET_THRESHOLD 为高风险操作">
    <el-card header="SET_THRESHOLD 设置门限" class="section">
      <el-alert type="info" :closable="false" show-icon title="规划中" description="SET_THRESHOLD 操作页面规划中 (FE-TODO-006)。当前不提供写入门限的真实入口。" />
    </el-card>
    </PermissionGuard>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import PermissionGuard from '@/components/auth/PermissionGuard.vue'
import { getBInterfaceThresholds, getBInterfaceFsuDevices } from '@/api/bInterface'
import type { ApiError } from '@/types/api'
import {
  isMappedCandidateStatus,
  isUnmappedMappingStatus,
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'

const loading = ref(false)
const error = ref<ApiError | null>(null)
const filterFsuCode = ref('51051243812345')
const filterDeviceId = ref('')
const filterSignalId = ref('')
const filterMappingStatus = ref('')
const filterStatus = ref('')
const thresholdData = ref<any[]>([])
const deviceData = ref<any[]>([])

const filteredDeviceData = computed(() => {
  let rows = deviceData.value
  if (filterDeviceId.value) rows = rows.filter(d => (d.deviceId || '').includes(filterDeviceId.value))
  if (filterMappingStatus.value === 'unmapped') rows = rows.filter(d => isUnmappedMappingStatus(d.mappingStatus))
  if (filterMappingStatus.value === 'mapped') rows = rows.filter(d => isMappedCandidateStatus(d.mappingStatus))
  return rows
})
const mappedDeviceCount = computed(() => deviceData.value.filter(d => isMappedCandidateStatus(d.mappingStatus)).length)
const unmappedDeviceCount = computed(() => deviceData.value.filter(d => isUnmappedMappingStatus(d.mappingStatus)).length)

async function fetchData() {
  loading.value = true
  try {
    const [tRes, dRes] = await Promise.all([
      getBInterfaceThresholds({ fsuCode: filterFsuCode.value }),
      getBInterfaceFsuDevices(filterFsuCode.value)
    ])
    thresholdData.value = (tRes as any).data || []
    deviceData.value = (dRes as any).data || []
  } catch { error.value = { code: 'load_failed', message: '加载失败' } }
  finally { loading.value = false }
}

function refresh() { fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
