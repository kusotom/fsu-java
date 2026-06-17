<template>
  <div>
    <PageHeader title="调度配置" description="B接口调度任务配置状态与运行记录（只读，不启用真实调度）" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>安全提示</template>
      本页面仅展示调度配置与运行记录。不会启用 Scheduler，不会访问真实 FSU，不会连接真实 FTP。真实 run-once / run-due 需后端授权 + 白名单 + 审计 + 调度开关。
    </el-alert>
    <ApiErrorAlert v-if="error" :error="error" />

    <!-- 统计 -->
    <el-row :gutter="16" class="section">
      <el-col :span="4"><el-statistic title="调度总数" value="4" /></el-col>
      <el-col :span="4"><el-statistic title="已启用" value="0"><template #suffix><StatusBadge status="disabled" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="realCall" value="0"><template #suffix><StatusBadge status="safety_blocked" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="白名单空" value="4"><template #suffix><el-tag size="small" type="danger">阻断</el-tag></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="最近运行" value="-" /></el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="section">
      <el-row :gutter="12" align="middle">
        <el-col :span="5"><el-select v-model="filterType" placeholder="调度类型" clearable size="small" style="width:100%">
          <el-option label="GET_DATA 轮询" value="GET_DATA_POLL" />
          <el-option label="GET_FSUINFO 心跳" value="GET_FSUINFO_HEARTBEAT" />
          <el-option label="FTP 图片拉取" value="FTP_IMAGE_PULL" />
          <el-option label="一致性审计" value="ACTIVE_ALARM_AUDIT" />
        </el-select></el-col>
        <el-col :span="4"><el-input v-model="filterFsuCode" placeholder="FSUCode" clearable size="small" /></el-col>
        <el-col :span="2"><el-button size="small" :loading="loading" @click="refresh"><el-icon><Refresh /></el-icon></el-button></el-col>
      </el-row>
    </el-card>

    <!-- 调度配置 + 安全门禁 -->
    <el-card header="调度配置" class="section">
      <EmptyState type="not-implemented" title="调度配置接口未接入"
        description="后端提供调度配置查询接口后可展示各调度类型的 enabled / interval / whitelist 等状态" />
    </el-card>

    <!-- 安全门禁说明 -->
    <el-card header="安全门禁说明" class="section">
      <el-table :data="gateRules" size="small" stripe>
        <el-table-column prop="type" label="调度类型" width="180" />
        <el-table-column prop="scheduler" label="Scheduler" width="100"><template #default><StatusBadge status="disabled" label="关闭" /></template></el-table-column>
        <el-table-column prop="realCall" label="真实调用" width="100"><template #default><StatusBadge status="safety_blocked" label="禁止" /></template></el-table-column>
        <el-table-column prop="whitelist" label="白名单" width="120"><template #default><el-tag size="small" type="danger">空=全部阻断</el-tag></template></el-table-column>
        <el-table-column prop="note" label="说明" min-width="200" />
      </el-table>
    </el-card>

    <!-- 运行记录 -->
    <el-card header="调度运行记录" class="section">
      <EmptyState type="not-implemented" title="调度运行记录接口未接入"
        description="后端提供调度运行记录查询接口后可展示各调度的 triggerType / status / duration / errorCode" />
    </el-card>

    <!-- run-once / run-due 禁用 -->
    <PermissionGuard :permissions="['protocol:runonce:readonly']">
    <el-card header="run-once / run-due" class="section">
      <el-row :gutter="16">
        <el-col :span="6"><el-card shadow="hover"><el-alert type="info" :closable="false" show-icon title="GET_DATA run-once" description="规划中" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-alert type="info" :closable="false" show-icon title="GET_FSUINFO run-once" description="规划中" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-alert type="info" :closable="false" show-icon title="FTP 图片 run-once" description="规划中" /></el-card></el-col>
        <el-col :span="6"><el-card shadow="hover"><el-alert type="info" :closable="false" show-icon title="一致性审计 run-once" description="规划中" /></el-card></el-col>
      </el-row>
    </el-card>
    </PermissionGuard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import PermissionGuard from '@/components/auth/PermissionGuard.vue'
import { getBInterfaceSchedulerConfigs, getBInterfaceSchedulerRuns, getBInterfaceRunOnceCapabilities } from '@/api/bInterface'
import type { ApiError } from '@/types/api'

const loading = ref(false)
const error = ref<ApiError | null>(null)
const filterType = ref('')
const filterFsuCode = ref('51051243812345')
const gateRules = ref<any[]>([])
const runOnceCapabilities = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [cfgRes, capRes] = await Promise.all([
      getBInterfaceSchedulerConfigs(), getBInterfaceRunOnceCapabilities()
    ])
    gateRules.value = ((cfgRes as any).data || []).map((c:any) => ({ type: c.schedulerType || '未知', scheduler: c.schedulerEnabled, realCall: c.realCallEnabled, whitelist: c.allowedSuids, note: c.source || 'config' }))
    runOnceCapabilities.value = (capRes as any).data || []
  } catch { error.value = { code: 'load_failed', message: '加载失败' } }
  finally { loading.value = false }
}

function refresh() { fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
