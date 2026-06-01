<template>
  <div>
    <PageHeader title="权限点管理" description="前端权限点注册表与路由/按钮权限规划" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>权限说明</template>
      前端权限只用于展示控制，真实操作必须由后端鉴权。当前权限点来源为 <el-tag size="small" type="warning">frontend-planning</el-tag>，后端接口接入后应以后端为准。
    </el-alert>

    <!-- 统计 -->
    <el-row :gutter="16" class="section">
      <el-col :span="4"><el-statistic title="总数" :value="metaList.length" /></el-col>
      <el-col :span="4"><el-statistic title="低风险" value="14"><template #suffix><StatusBadge status="success" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="中风险" value="3"><template #suffix><StatusBadge status="warning" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="高风险" value="10"><template #suffix><StatusBadge status="failed" /></template></el-statistic></el-col>
      <el-col :span="4"><el-statistic title="严重" value="4"><template #suffix><el-tag size="small" type="danger">critical</el-tag></template></el-statistic></el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card class="section">
      <el-row :gutter="12" align="middle">
        <el-col :span="4"><el-select v-model="filterGroup" placeholder="权限组" clearable size="small" style="width:100%">
          <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
        </el-select></el-col>
        <el-col :span="4"><el-select v-model="filterRisk" placeholder="风险" clearable size="small" style="width:100%">
          <el-option label="低" value="low" /><el-option label="中" value="medium" /><el-option label="高" value="high" /><el-option label="严重" value="critical" />
        </el-select></el-col>
        <el-col :span="4"><el-input v-model="filterKeyword" placeholder="搜索" clearable size="small" /></el-col>
      </el-row>
    </el-card>

    <!-- 权限点列表 -->
    <el-card header="权限点列表" class="section">
      <el-table :data="filtered" size="small" stripe max-height="500">
        <el-table-column prop="group" label="权限组" width="120" />
        <el-table-column prop="code" label="权限点" min-width="240" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="风险" width="70"><template #default="{row}"><StatusBadge :status="row.riskLevel === 'critical' ? 'failed' : row.riskLevel === 'high' ? 'warning' : row.riskLevel === 'medium' ? 'warning' : 'success'" :label="row.riskLevel" /></template></el-table-column>
        <el-table-column label="来源" width="160"><template #default><el-tag size="small" type="warning">frontend-planning</el-tag></template></el-table-column>
      </el-table>
    </el-card>

    <!-- 路由权限映射 -->
    <el-card header="路由权限映射" class="section">
      <el-table :data="routePermMap" size="small" stripe>
        <el-table-column prop="path" label="路由" width="250" />
        <el-table-column prop="title" label="页面" width="120" />
        <el-table-column label="所需权限" min-width="220"><template #default="{row}"><code style="font-size:12px">{{ row.permission }}</code></template></el-table-column>
      </el-table>
    </el-card>

    <!-- 按钮权限示例 -->
    <el-card header="按钮权限示例" class="section">
      <el-table :data="buttonExamples" size="small" stripe>
        <el-table-column prop="label" label="按钮" width="180" />
        <el-table-column label="所需权限" width="280"><template #default="{row}"><code style="font-size:12px">{{ row.permission }}</code></template></el-table-column>
        <el-table-column label="状态" width="120"><template #default><el-button size="small" disabled>规划中</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { PERMISSION_META_LIST } from '@/auth/permissions'

const filterGroup = ref(''); const filterRisk = ref(''); const filterKeyword = ref('')
const groups = ['B接口查看','B接口只读操作','调度','高风险操作','系统管理','审计']
const metaList = PERMISSION_META_LIST

const filtered = computed(() => {
  let list = metaList
  if (filterGroup.value) list = list.filter(m => m.group === filterGroup.value)
  if (filterRisk.value) list = list.filter(m => m.riskLevel === filterRisk.value)
  if (filterKeyword.value) list = list.filter(m => m.code.includes(filterKeyword.value))
  return list
})

const routePermMap = [
  { path:'/b-interface/fsus', title:'FSU状态', permission:'binterface.fsu.read' },
  { path:'/b-interface/realtime', title:'实时数据', permission:'binterface.realtime.read' },
  { path:'/b-interface/alarms', title:'告警', permission:'binterface.alarm.read' },
  { path:'/b-interface/thresholds', title:'门限', permission:'binterface.threshold.read' },
  { path:'/b-interface/ftp-login-info', title:'FTP参数', permission:'binterface.ftp.read' },
  { path:'/b-interface/ftp-images', title:'FTP图片', permission:'binterface.ftp_image.read' },
  { path:'/b-interface/schedulers', title:'调度', permission:'binterface.scheduler.read' },
  { path:'/b-interface/protocol-audit', title:'协议治理', permission:'binterface.protocol_audit.read' },
  { path:'/system/users', title:'用户管理', permission:'user.read' },
  { path:'/system/roles', title:'角色管理', permission:'role.manage' },
  { path:'/system/permissions', title:'权限管理', permission:'permission.read' },
]

const buttonExamples = [
  { label:'GET_DATA dry_run', permission:'binterface.dry_run' },
  { label:'GET_FSUINFO run-once', permission:'binterface.get.run' },
  { label:'Scheduler run-due', permission:'scheduler.run_due' },
  { label:'SET_POINT execute', permission:'binterface.set_point.execute + 后端门禁' },
  { label:'SET_FSUREBOOT execute', permission:'binterface.reboot.execute + 后端门禁 + 维护窗口' },
]
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
