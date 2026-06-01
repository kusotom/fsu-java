<template>
  <div>
    <PageHeader :title="`FSU ${fsuCode}`" description="FSU 详情 — 基础信息、运行状态、设备、点位与通信记录" />
    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" />

    <!-- ===== 指标卡片 ===== -->
    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="在线状态" :value="status?.onlineStatus || '-'" :status="onlineStatusClass" :loading="loading" />
      <MetricCard title="心跳丢失" :value="status?.heartbeatMissCount ?? 0" unit="次" :status="(status?.heartbeatMissCount || 0) > 3 ? 'danger' : 'normal'" :loading="loading" />
      <MetricCard title="设备数" :value="deviceIds.length" status="info" :loading="loading" />
    </div>

    <!-- ===== Tabs ===== -->
    <el-tabs v-model="activeTab" style="margin-bottom: 16px">
      <el-tab-pane label="基础信息" name="basic" />
      <el-tab-pane label="运行状态" name="status" />
      <el-tab-pane label="设备列表" name="devices" />
      <el-tab-pane label="通信记录" name="comm" />
    </el-tabs>

    <!-- ===== 基础信息 ===== -->
    <div v-if="activeTab === 'basic'" class="app-card">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="FSU 编码">{{ status?.fsuCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="在线状态"><FsuOnlineBadge :online-status="status?.onlineStatus" /></el-descriptions-item>
        <el-descriptions-item label="登录状态"><StatusBadge :status="status?.loginStatus || 'UNKNOWN'" /></el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ status?.lastLoginTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后心跳">{{ status?.lastHeartbeat || '-' }}</el-descriptions-item>
        <el-descriptions-item label="心跳丢失">{{ status?.heartbeatMissCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="会话 ID" :span="2">{{ status?.sessionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态描述">{{ status?.statusDetail || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- ===== 运行状态 ===== -->
    <div v-if="activeTab === 'status'" class="app-card">
      <el-row :gutter="24">
        <el-col :span="12" style="text-align: center">
          <el-progress type="dashboard" :percentage="cpuPercent" :color="cpuColor">
            <template #default><span style="font-size: 12px; color: var(--text-secondary)">CPU</span></template>
          </el-progress>
          <div style="text-align: center; font-size: 13px; color: var(--text-secondary); margin-top: 4px">{{ cpuDisplay }}</div>
        </el-col>
        <el-col :span="12" style="text-align: center">
          <el-progress type="dashboard" :percentage="memPercent" :color="memColor">
            <template #default><span style="font-size: 12px; color: var(--text-secondary)">MEM</span></template>
          </el-progress>
          <div style="text-align: center; font-size: 13px; color: var(--text-secondary); margin-top: 4px">{{ memDisplay }}</div>
        </el-col>
      </el-row>
      <EmptyState v-if="!hasCpuMem" type="empty" title="暂无 CPU/MEM 数据" description="FSU 尚未返回 GET_FSUINFO 运行状态数据" style="margin-top: 16px" />
    </div>

    <!-- ===== 设备列表 ===== -->
    <div v-if="activeTab === 'devices'">
      <EmptyState v-if="deviceIds.length === 0" type="empty" title="暂无设备列表" description="GET_LOGININFO 或 LOGIN 后可发现下挂设备" />
      <div v-else class="app-card data-table" style="padding: 0; overflow: hidden">
        <el-table :data="deviceIds" size="small" stripe>
          <el-table-column prop="deviceId" label="Device ID" width="200" />
          <el-table-column prop="source" label="来源" width="150" />
          <el-table-column prop="deviceCode" label="Device Code" width="180" />
          <el-table-column label="映射状态" width="120">
            <template #default="scope">
              <StatusBadge :status="mappingStatusType(scope.row.mappingStatus)" :label="mappingStatusLabel(scope.row.mappingStatus)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ===== 通信记录 ===== -->
    <div v-if="activeTab === 'comm'">
      <EmptyState v-if="!status?.lastLoginTime && !status?.lastHeartbeat" type="empty" title="暂无通信记录" description="FSU 尚未上报通信数据" />
      <div v-else class="app-card">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="最近 LOGIN">{{ status?.lastLoginTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近 HEARTBEAT">{{ status?.lastHeartbeat || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录状态"><StatusBadge :status="status?.loginStatus || 'UNKNOWN'" /></el-descriptions-item>
          <el-descriptions-item label="在线状态"><FsuOnlineBadge :online-status="status?.onlineStatus" /></el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <!-- ===== GET_FSUINFO run-once (协议诊断, 权限保护) ===== -->
    <PermissionGuard :permissions="['protocol:runonce:readonly']">
      <el-card header="协议诊断 — GET_FSUINFO 心跳 (Code=1701)" class="section" style="margin-top: 16px">
        <div class="protocol-banner" style="margin-bottom: 12px">协议诊断模式 — GET_FSUINFO run-once，仅限授权人员使用</div>
        <EmptyState type="not-implemented" title="REST 接口未接入"
          description="BInterface2016GetFsuInfoService 已实现，REST Controller 待后续新增。" />
      </el-card>
    </PermissionGuard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FsuOnlineBadge from '@/components/common/FsuOnlineBadge.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import PermissionGuard from '@/components/auth/PermissionGuard.vue'
import { getBInterfaceFsuDetail, getBInterfaceFsuDevices } from '@/api/bInterface'
import {
  mappingStatusBadgeStatus as mappingStatusType,
  mappingStatusLabel,
} from '@/utils/mappingStatus'

const route = useRoute()
const fsuCode = computed(() => route.params.fsuCode as string || '')
const loading = ref(false)
const activeTab = ref('basic')
const status = ref<any>(null)
const deviceIds = ref<any[]>([])
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'unauthorized' | 'forbidden' | 'server_error'>('normal')

const cpuDisplay = computed(() => status.value?.statusDetail ?? '-')
const memDisplay = computed(() => status.value?.statusDetail ?? '-')
const cpuPercent = computed(() => 0)
const memPercent = computed(() => 0)
const cpuColor = computed(() => '#16A34A')
const memColor = computed(() => '#1A5FDC')
const hasCpuMem = computed(() => false)
const onlineStatusClass = computed(() => {
  const s = (status.value?.onlineStatus || '').toUpperCase()
  if (s === 'ONLINE') return 'normal'
  if (s === 'OFFLINE') return 'warning'
  return 'muted'
})

async function fetchStatus() {
  loading.value = true; dataState.value = 'normal'
  try {
    const [detailRes, devicesRes] = await Promise.all([
      getBInterfaceFsuDetail(fsuCode.value),
      getBInterfaceFsuDevices(fsuCode.value)
    ])
    status.value = (detailRes as any).data || null
    deviceIds.value = (devicesRes as any).data || []
  } catch (e: any) {
    if (e?.response?.status === 404) dataState.value = 'api_not_found'
    else if (e?.response?.status === 403) dataState.value = 'forbidden'
    else if (e?.response?.status && e.response.status >= 500) dataState.value = 'server_error'
    else if (e?.request && !e?.response) dataState.value = 'network_error'
  } finally { loading.value = false }
}

onMounted(fetchStatus)
</script>

<style scoped>
.section { margin-top: 16px; }
</style>
