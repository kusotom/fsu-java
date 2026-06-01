<template>
  <div>
    <PageHeader title="FTP 图片" description="FSU FTP 图片文件列表与拉取记录（只读，不连接真实 FTP）" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>只读展示</template>
      本页面仅展示 FTP 图片文件与拉取记录。不会连接真实 FTP，不上传/删除/移动文件。FTP 密码不在前端展示。真实 FTP 图片拉取需后端授权 + 白名单 + 审计 + 调度开关。
    </el-alert>
    <ApiErrorAlert v-if="error" :error="error" />

    <!-- 筛选 -->
    <el-card class="section">
      <el-row :gutter="12" align="middle">
        <el-col :span="5"><el-input v-model="filterFsuCode" placeholder="FSUCode" clearable size="small" /></el-col>
        <el-col :span="4">
          <el-select v-model="filterStatus" placeholder="文件状态" clearable size="small" style="width:100%">
            <el-option label="已下载" value="downloaded" /><el-option label="重复" value="duplicate" />
            <el-option label="已拒绝" value="rejected" /><el-option label="失败" value="failed" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterRejectReason" placeholder="拒绝原因" clearable size="small" style="width:100%">
            <el-option label="扩展名" value="bad_extension" /><el-option label="文件名" value="bad_filename" />
            <el-option label="超大" value="oversize" /><el-option label="路径穿越" value="path_traversal" />
            <el-option label="魔数" value="bad_magic" /><el-option label="下载失败" value="download_failed" />
          </el-select>
        </el-col>
        <el-col :span="2"><el-button size="small" :loading="loading" @click="refresh"><el-icon><Refresh /></el-icon></el-button></el-col>
      </el-row>
    </el-card>

    <!-- 统计 -->
    <el-row :gutter="16" class="section">
      <el-col :span="4"><el-statistic title="图片总数" value="0" /></el-col>
      <el-col :span="4"><el-statistic title="已下载" value="0"><template #suffix><StatusBadge status="success" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="重复" value="0"><template #suffix><StatusBadge status="warning" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="已拒绝" value="0"><template #suffix><StatusBadge status="failed" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="最近拉取" value="-" /></el-col>
    </el-row>

    <!-- 图片列表 -->
    <el-card header="FTP 图片列表" class="section">
      <EmptyState type="not-implemented" title="FTP 图片接口未接入"
        description="后端提供 GET /b-interface/ftp/images 接口后可展示 FSU FTP 图片文件列表" />
    </el-card>

    <!-- 拉取记录 -->
    <el-card header="拉取运行记录" class="section">
      <EmptyState type="not-implemented" title="FTP 图片拉取记录接口未接入"
        description="后端提供 GET /b-interface/ftp/images/pull-runs 接口后可展示拉取运行记录" />
    </el-card>

    <!-- run-once / scheduler 禁用占位 -->
    <el-row :gutter="16" class="section">
      <el-col :span="12">
        <el-card header="FTP 图片 run-once">
          <el-alert type="info" :closable="false" show-icon title="规划中"
            description="后续接入后端接口后实现。真实 FTP 拉取需后端授权 + 白名单 + 审计。" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="FTP 图片定期拉取调度">
          <el-alert type="info" :closable="false" show-icon title="规划中"
            description="后续 FE-TODO-010 调度配置页实现。需 scheduler-enabled + real-call-enabled + allowed-suids。" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import { getBInterfaceFtpImages, getBInterfaceFtpImagePullRuns } from '@/api/bInterface'
import type { ApiError } from '@/types/api'

const loading = ref(false)
const error = ref<ApiError | null>(null)
const filterFsuCode = ref('51051243812345')
const filterStatus = ref('')
const filterRejectReason = ref('')
const imageData = ref<any[]>([])
const pullRunData = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [imgRes, runRes] = await Promise.all([
      getBInterfaceFtpImages({ fsuCode: filterFsuCode.value }),
      getBInterfaceFtpImagePullRuns({ fsuCode: filterFsuCode.value })
    ])
    imageData.value = (imgRes as any).data || []
    pullRunData.value = (runRes as any).data || []
  } catch { error.value = { code: 'load_failed', message: '加载失败' } }
  finally { loading.value = false }
}

function refresh() { fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
