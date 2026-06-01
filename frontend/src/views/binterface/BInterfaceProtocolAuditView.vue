<template>
  <div>
    <PageHeader title="协议治理与审计" description="B接口协议覆盖矩阵、兼容性、安全策略与审计状态" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>治理只读</template>
      本页面仅展示协议覆盖、安全策略和审计状态。不会执行任何 SET、重启、升级、Scheduler 或真实 FSU 调用。下方矩阵标注 <el-tag size="small" type="warning">frontend-planning</el-tag> 表示前端规划，不代表后端实时状态。
    </el-alert>
    <ApiErrorAlert v-if="error" :error="error" />

    <!-- 统计 -->
    <el-row :gutter="16" class="section">
      <el-col :span="3"><el-statistic title="命令总数" value="19" /></el-col>
      <el-col :span="3"><el-statistic title="已实现" value="9"><template #suffix><StatusBadge status="success" /></template></el-statistic></el-col>
      <el-col :span="3"><el-statistic title="部分" value="2"><template #suffix><StatusBadge status="warning" /></template></el-statistic></el-col>
      <el-col :span="3"><el-statistic title="阻塞" value="2"><template #suffix><StatusBadge status="failed" /></template></el-statistic></el-col>
      <el-col :span="3"><el-statistic title="真实验证" value="4"><template #suffix><StatusBadge status="mapped" /></template></el-statistic></el-col>
      <el-col :span="3"><el-statistic title="高风险" value="6"><template #suffix><el-tag size="small" type="danger">SET</el-tag></template></el-statistic></el-col>
      <el-col :span="3"><el-statistic title="Emerson" value="5"><template #suffix><el-tag size="small" type="success">2016</el-tag></template></el-statistic></el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="section">
      <el-row :gutter="10" align="middle">
        <el-col :span="4"><el-select v-model="filterProfile" placeholder="Profile" clearable size="small" style="width:100%">
          <el-option label="2016" value="2016" /><el-option label="2024" value="2024" /><el-option label="Emerson" value="emerson-2016" />
        </el-select></el-col>
        <el-col :span="4"><el-select v-model="filterStatus" placeholder="实现状态" clearable size="small" style="width:100%">
          <el-option label="已实现" value="done" /><el-option label="部分" value="partial" /><el-option label="阻塞" value="blocked" /><el-option label="规划中" value="planned" />
        </el-select></el-col>
        <el-col :span="4"><el-select v-model="filterRisk" placeholder="风险" clearable size="small" style="width:100%">
          <el-option label="高危" value="high" /><el-option label="严重" value="critical" />
        </el-select></el-col>
        <el-col :span="4"><el-input v-model="filterKeyword" placeholder="命令名" clearable size="small" /></el-col>
        <el-col :span="2"><el-button size="small" @click="refresh"><el-icon><Refresh /></el-icon></el-button></el-col>
      </el-row>
    </el-card>

    <!-- 命令覆盖矩阵 (fallback) -->
    <el-card class="section">
      <template #header><span>命令覆盖矩阵 <el-tag size="small" type="warning">frontend-planning</el-tag></span></template>
      <el-table :data="matrixList.length ? matrixList.filter((m:any) => !filterKeyword||m.commandName?.includes(filterKeyword)) : filteredMatrix" size="small" stripe max-height="600">
        <el-table-column prop="commandName" label="命令" width="150" fixed />
        <el-table-column prop="code" label="Code" width="70" />
        <el-table-column label="方向" width="90"><template #default="{row}">{{ row.direction === 'SC_TO_FSU' ? 'SC→FSU' : 'FSU→SC' }}</template></el-table-column>
        <el-table-column label="实现" width="80">
          <template #default="{row}"><StatusBadge :status="row.implStatus === 'done' ? 'success' : row.implStatus === 'partial' ? 'warning' : row.implStatus === 'blocked' ? 'failed' : 'info'" :label="implLabel(row.implStatus)" /></template>
        </el-table-column>
        <el-table-column label="验证" width="90">
          <template #default="{row}"><StatusBadge :status="row.verifyStatus === 'real_verified' ? 'success' : row.verifyStatus === 'sample_replayed' ? 'warning' : row.verifyStatus === 'real_blocked' ? 'failed' : 'info'" :label="verifyLabel(row.verifyStatus)" /></template>
        </el-table-column>
        <el-table-column label="风险" width="70"><template #default="{row}"><el-tag v-if="row.risk === 'critical'" size="small" type="danger">严重</el-tag><el-tag v-else-if="row.risk === 'high'" size="small" type="warning">高</el-tag><span v-else>-</span></template></el-table-column>
        <el-table-column label="Emerson" width="70"><template #default="{row}"><StatusBadge v-if="row.emerson" status="success" label="✓" /></template></el-table-column>
        <el-table-column label="阻塞/备注" min-width="180" show-overflow-tooltip><template #default="{row}">{{ row.note || '-' }}</template></el-table-column>
      </el-table>
    </el-card>

    <!-- Profile + Error Codes + SET Safety -->
    <el-row :gutter="16" class="section">
      <el-col :span="8">
        <el-card header="协议 Profile">
          <el-table :data="profileList.length ? profileList : profiles" size="small">
            <el-table-column label="Profile" width="130"><template #default="scope">{{ scope.row.profile }}</template></el-table-column>
            <el-table-column label="定位" min-width="160"><template #default="scope">{{ scope.row.role }}</template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="错误码体系">
          <el-table :data="errorCodeList.length ? errorCodeList : FALLBACK_ERROR_CODES" size="small">
            <el-table-column label="Code" width="140"><template #default="{row}"><ErrorCodeTag :code="row.code" :category="row.category" /></template></el-table-column>
            <el-table-column prop="severity" label="级别" width="60"><template #default="{row}"><StatusBadge :status="row.severity === 'danger' ? 'failed' : row.severity === 'warning' ? 'warning' : 'info'" /></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="SET 安全策略">
          <el-table :data="setPolicyList.length ? setPolicyList : FALLBACK_SET_POLICIES" size="small">
            <el-table-column label="命令" width="150"><template #default="scope">{{ scope.row.commandName || scope.row.cmd }}</template></el-table-column>
            <el-table-column label="状态" width="80"><template #default="scope"><StatusBadge :status="scope.row.status || 'safety_blocked'" :label="scope.row.status || 'blocked'" /></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 审计记录 -->
    <el-card header="审计记录" class="section">
      <EmptyState type="not-implemented" title="B接口审计记录接口未接入" description="后端提供 /b-interface/audits 接口后可展示审计记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import ErrorCodeTag from '@/components/common/ErrorCodeTag.vue'
import { getBInterfaceProtocolMatrix, getBInterfaceErrorCodes, getBInterfaceSetSafetyPolicies, getBInterfaceProtocolProfiles, getBInterfaceAudits } from '@/api/bInterface'
import type { ApiError } from '@/types/api'

const loading = ref(false); const error = ref<ApiError | null>(null)
const filterProfile = ref(''); const filterStatus = ref(''); const filterRisk = ref(''); const filterKeyword = ref('')
const errorCodeList = ref<any[]>([]); const setPolicyList = ref<any[]>([])
const matrixList = ref<any[]>([]); const profileList = ref<any[]>([])

// frontend-planning fallback matrix (used as fallback when backend returns empty)
const fallbackMatrix = [
  { commandName:'LOGIN', code:'101', direction:'FSU_TO_SC', implStatus:'done', verifyStatus:'real_verified', risk:'low', emerson:true, note:'' },
  { commandName:'LOGOUT', code:'103', direction:'FSU_TO_SC', implStatus:'planned', verifyStatus:'not_verified', risk:'low', emerson:false, note:'未实现' },
  { commandName:'HEARTBEAT', code:'-', direction:'SC_TO_FSU', implStatus:'done', verifyStatus:'not_verified', risk:'low', emerson:false, note:'非标准2016命令,工程心跳应映射GET_FSUINFO(1701)' },
  { commandName:'SEND_DATA', code:'-', direction:'FSU_TO_SC', implStatus:'done', verifyStatus:'not_verified', risk:'low', emerson:false, note:'FSU不主动发送' },
  { commandName:'SEND_ALARM', code:'501', direction:'FSU_TO_SC', implStatus:'done', verifyStatus:'sample_replayed', risk:'medium', emerson:true, note:'真实上报待现场触发' },
  { commandName:'GET_DATA', code:'401', direction:'SC_TO_FSU', implStatus:'partial', verifyStatus:'real_blocked', risk:'high', emerson:true, note:'缺DeviceID→SPID映射表' },
  { commandName:'GET_HISDATA', code:'403', direction:'SC_TO_FSU', implStatus:'planned', verifyStatus:'not_verified', risk:'low', emerson:false, note:'未实现' },
  { commandName:'GET_THRESHOLD', code:'1901', direction:'SC_TO_FSU', implStatus:'partial', verifyStatus:'real_blocked', risk:'high', emerson:false, note:'缺DeviceID→SPID映射表' },
  { commandName:'SET_POINT', code:'1001', direction:'SC_TO_FSU', implStatus:'planned', verifyStatus:'not_verified', risk:'high', emerson:false, note:'高风险默认阻断' },
  { commandName:'SET_THRESHOLD', code:'2001', direction:'SC_TO_FSU', implStatus:'planned', verifyStatus:'not_verified', risk:'high', emerson:false, note:'高风险默认阻断' },
  { commandName:'GET_LOGININFO', code:'1501', direction:'SC_TO_FSU', implStatus:'done', verifyStatus:'real_verified', risk:'low', emerson:true, note:'' },
  { commandName:'SET_LOGININFO', code:'1503', direction:'SC_TO_FSU', implStatus:'planned', verifyStatus:'not_verified', risk:'high', emerson:false, note:'高风险默认阻断' },
  { commandName:'GET_FTP', code:'1601', direction:'SC_TO_FSU', implStatus:'done', verifyStatus:'real_verified', risk:'low', emerson:true, note:'' },
  { commandName:'SET_FTP', code:'1603', direction:'SC_TO_FSU', implStatus:'planned', verifyStatus:'not_verified', risk:'high', emerson:false, note:'高风险默认阻断' },
  { commandName:'GET_FSUINFO', code:'1701', direction:'SC_TO_FSU', implStatus:'done', verifyStatus:'real_verified', risk:'low', emerson:true, note:'FIX-002对齐Emerson格式' },
  { commandName:'SET_FSUREBOOT', code:'1801', direction:'SC_TO_FSU', implStatus:'disabled', verifyStatus:'not_verified', risk:'critical', emerson:false, note:'安全禁用' },
  { commandName:'TIME_CHECK', code:'1301', direction:'SC_TO_FSU', implStatus:'done', verifyStatus:'not_verified', risk:'low', emerson:false, note:'' },
  { commandName:'GET_ACTIVEALARM', code:'603', direction:'SC_TO_FSU', implStatus:'done', verifyStatus:'sample_replayed', risk:'low', emerson:false, note:'' },
  { commandName:'SUREADY', code:'103', direction:'FSU_TO_SC', implStatus:'done', verifyStatus:'not_verified', risk:'low', emerson:false, note:'' },
]

const filteredMatrix = computed(() => {
  let list = fallbackMatrix
  if (filterStatus.value) list = list.filter(r => r.implStatus === filterStatus.value)
  if (filterRisk.value) list = list.filter(r => r.risk === filterRisk.value)
  if (filterKeyword.value) list = list.filter(r => r.commandName.toLowerCase().includes(filterKeyword.value.toLowerCase()))
  return list
})

const profiles = [
  { profile:'B接口2016', role:'当前主开发依据 / 真实落地协议' },
  { profile:'Emerson-2016', role:'FSU-2808IM 真实设备行为 profile' },
  { profile:'B接口2024', role:'未来升级兼容层（保留, 不删除）' },
]

const FALLBACK_ERROR_CODES = [
  { code:'safety_blocked', category:'safety', severity:'warning' },
  { code:'real_call_disabled', category:'safety', severity:'warning' },
  { code:'not_whitelisted', category:'safety', severity:'warning' },
  { code:'scheduler_disabled', category:'safety', severity:'info' },
  { code:'business_failure', category:'business', severity:'warning' },
  { code:'soap_timeout', category:'transport', severity:'danger' },
  { code:'xml_parse_error', category:'parse', severity:'danger' },
  { code:'circuit_open', category:'circuit_breaker', severity:'info' },
  { code:'not_mapped', category:'mapping', severity:'info' },
]
const FALLBACK_SET_POLICIES = [
  { cmd:'SET_POINT' }, { cmd:'SET_THRESHOLD' }, { cmd:'SET_FTP' }, { cmd:'SET_LOGININFO' }, { cmd:'SET_FSUREBOOT' }
]

function implLabel(s: string) { const m: Record<string,string>={done:'已实现',partial:'部分',blocked:'阻塞',planned:'规划',disabled:'禁用'}; return m[s]||s }
function verifyLabel(s: string) { const m: Record<string,string>={real_verified:'真实验证',sample_replayed:'样本回放',real_blocked:'真实阻塞',not_verified:'未验证'}; return m[s]||s }

async function fetchRealData() {
  try {
    const [ecRes, spRes, mxRes, pfRes] = await Promise.all([
      getBInterfaceErrorCodes(), getBInterfaceSetSafetyPolicies(),
      getBInterfaceProtocolMatrix(), getBInterfaceProtocolProfiles()
    ])
    errorCodeList.value = (ecRes as any).data || []
    setPolicyList.value = (spRes as any).data || []
    matrixList.value = (mxRes as any).data || []
    profileList.value = (pfRes as any).data || []
  } catch {}
}

function refresh() { loading.value = true; fetchRealData().finally(() => loading.value = false) }

onMounted(() => fetchRealData())
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
