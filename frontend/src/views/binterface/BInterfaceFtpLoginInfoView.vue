<template>
  <div>
    <PageHeader title="FTP与注册参数" description="GET_FTP / GET_LOGININFO 只读参数展示（敏感字段脱敏）" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>只读展示</template>
      本页面仅展示 GET_FTP / GET_LOGININFO 只读参数。Password / FTPPwd / IPSecPWD 等敏感字段仅显示脱敏状态。SET_FTP / SET_LOGININFO 属于高风险写操作，当前仅做规划占位。
    </el-alert>

    <ApiErrorAlert v-if="error" :error="error" />

    <!-- 筛选 -->
    <el-card class="section">
      <el-row :gutter="12" align="middle">
        <el-col :span="5"><el-input v-model="filterFsuCode" placeholder="FSUCode" clearable size="small" /></el-col>
        <el-col :span="4">
          <el-select v-model="filterSource" placeholder="来源" clearable size="small" style="width:100%">
            <el-option label="GET_FTP" value="GET_FTP" /><el-option label="GET_LOGININFO" value="GET_LOGININFO" /><el-option label="LOGIN" value="LOGIN" />
          </el-select>
        </el-col>
        <el-col :span="2"><el-button size="small" :loading="loading" @click="refresh"><el-icon><Refresh /></el-icon></el-button></el-col>
      </el-row>
    </el-card>

    <!-- FSU 注册状态 -->
    <el-card header="FSU 注册状态" class="section">
      <EmptyState type="not-implemented" title="GET_LOGININFO / LOGIN 只读接口未接入"
        description="后端提供 B接口注册参数查询接口后可展示 StationName / SCIP / MacId / Version" />
      <el-descriptions v-if="false" :column="3" border size="small">
        <el-descriptions-item label="FSUCode">{{ filterFsuCode }}</el-descriptions-item>
        <el-descriptions-item label="StationName">-</el-descriptions-item>
        <el-descriptions-item label="版本">-</el-descriptions-item>
        <el-descriptions-item label="SC IP">-</el-descriptions-item>
        <el-descriptions-item label="MAC">00:09:F5:E0:8D:B7</el-descriptions-item>
        <el-descriptions-item label="最后登录">-</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- FTP 参数 -->
    <el-card header="FTP 参数" class="section">
      <EmptyState type="not-implemented" title="GET_FTP 只读接口未接入"
        description="后端提供 B接口 FTP 参数查询接口后可展示 FTP Host / Port / Username / Path" />
      <el-descriptions v-if="false" :column="3" border size="small">
        <el-descriptions-item label="FTP Host">-</el-descriptions-item>
        <el-descriptions-item label="Port">-</el-descriptions-item>
        <el-descriptions-item label="Username">-</el-descriptions-item>
        <el-descriptions-item label="密码"><SensitiveValue :configured="false" /></el-descriptions-item>
        <el-descriptions-item label="路径">-</el-descriptions-item>
        <el-descriptions-item label="来源">-</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 注册参数 -->
    <el-card header="注册参数 (IPSec)" class="section">
      <EmptyState type="not-implemented" title="IPSec 注册参数接口未接入" />
      <el-descriptions v-if="false" :column="3" border size="small">
        <el-descriptions-item label="IPSec User">-</el-descriptions-item>
        <el-descriptions-item label="IPSec PWD"><SensitiveValue :configured="false" /></el-descriptions-item>
        <el-descriptions-item label="IPSec IP">-</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- DeviceList -->
    <el-card header="已发现 DeviceID" class="section">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px">
        当前已发现 DeviceID，但尚未导入 DeviceID → SPID/SignalID 点位映射表。DeviceList 只能说明 FSU 下有哪些设备，不能代表完整点位表。
      </el-alert>
      <el-table :data="deviceRows" size="small" stripe>
        <el-table-column prop="fsuCode" label="FSUCode" width="170" />
        <el-table-column prop="deviceId" label="DeviceID" width="200" />
        <el-table-column prop="source" label="来源" width="150"><template #default="scope">{{ scope.row.source || 'GET_LOGININFO' }}</template></el-table-column>
        <el-table-column label="映射状态" width="100"><template #default><StatusBadge status="unmapped" /></template></el-table-column>
      </el-table>
    </el-card>

    <!-- SET 禁用占位 -->
    <el-row :gutter="16" class="section">
      <el-col :span="12">
        <el-card header="SET_FTP 设置 FTP 参数">
          <el-alert type="info" :closable="false" show-icon title="规划中"
            description="真实操作需管理员权限 + confirmation_token + 审计 + 维护窗口。后续 FE-TODO-007B 实现。" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="SET_LOGININFO 设置注册参数">
          <el-alert type="info" :closable="false" show-icon title="规划中"
            description="真实操作需管理员权限 + confirmation_token + 审计 + 维护窗口。后续 FE-RISK 实现。" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import SensitiveValue from '@/components/common/SensitiveValue.vue'
import { getBInterfaceLoginInfo, getBInterfaceFsuDevices } from '@/api/bInterface'
import type { ApiError } from '@/types/api'

const loading = ref(false)
const error = ref<ApiError | null>(null)
const filterFsuCode = ref('51051243812345')
const filterSource = ref('')
const loginInfo = ref<any>(null)
const deviceRows = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [liRes, devRes] = await Promise.all([
      getBInterfaceLoginInfo({ fsuCode: filterFsuCode.value }),
      getBInterfaceFsuDevices(filterFsuCode.value)
    ])
    loginInfo.value = (liRes as any).data || null
    deviceRows.value = (devRes as any).data || []
  } catch { error.value = { code: 'load_failed', message: '加载失败' } }
  finally { loading.value = false }
}

function refresh() { fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
