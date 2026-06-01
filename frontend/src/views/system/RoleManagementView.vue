<template>
  <div>
    <PageHeader title="角色管理" description="平台角色、权限点与高风险操作边界（只读规划）" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>权限说明</template>
      前端权限只负责页面展示和按钮禁用，真实安全必须由后端校验。高风险操作需后端白名单 + confirmation_token + 维护窗口 + 审计。
    </el-alert>

    <!-- 角色概览 -->
    <el-row :gutter="16" class="section">
      <el-col :span="8" v-for="r in roleCards" :key="r.code">
        <el-card shadow="hover">
          <template #header><el-tag :type="r.risk === 'high' ? 'danger' : r.risk === 'medium' ? 'warning' : 'success'" size="large">{{ r.name }}</el-tag></template>
          <p style="color:#666;font-size:13px;margin-bottom:8px">{{ r.description }}</p>
          <el-text size="small" type="info">{{ r.code }} · {{ r.permCount }} 权限 · <StatusBadge :status="r.risk === 'low' ? 'success' : r.risk === 'medium' ? 'warning' : 'failed'" :label="r.risk" /></el-text>
        </el-card>
      </el-col>
    </el-row>

    <!-- 角色列表 -->
    <el-card header="角色列表" class="section">
      <el-table :data="roleList" size="small" stripe>
        <el-table-column prop="code" label="代码" width="120"><template #default="{row}"><el-tag size="small">{{ row.code }}</el-tag></template></el-table-column>
        <el-table-column prop="name" label="名称" width="120" />
        <el-table-column label="风险" width="80"><template #default="{row}"><StatusBadge :status="row.riskLevel === 'high' ? 'failed' : row.riskLevel === 'medium' ? 'warning' : 'success'" :label="row.riskLevel" /></template></el-table-column>
        <el-table-column label="内置" width="70"><template #default="{row}"><StatusBadge :status="row.builtin ? 'success' : 'info'" :label="row.builtin ? '是' : '否'" /></template></el-table-column>
        <el-table-column prop="permissionCount" label="权限数" width="70" />
        <el-table-column label="来源" width="160"><template #default><el-tag size="small" type="warning">frontend-planning</el-tag></template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default><el-button link size="small" disabled>编辑</el-button><el-button link size="small" disabled>分配权限</el-button><el-button link size="small" disabled>删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 权限点列表 -->
    <el-card header="权限点列表" class="section">
      <el-table :data="permList" size="small" stripe max-height="400">
        <el-table-column prop="group" label="权限组" width="130" />
        <el-table-column prop="code" label="权限点" min-width="220" />
        <el-table-column label="风险" width="70">
          <template #default="{row}"><el-tag v-if="row.riskLevel === 'critical'" size="small" type="danger">严重</el-tag><el-tag v-else-if="row.riskLevel === 'high'" size="small" type="warning">高</el-tag><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="只读" width="60"><template #default="{row}"><StatusBadge v-if="row.readOnly" status="success" label="✓" /><span v-else>-</span></template></el-table-column>
        <el-table-column label="运维" width="60"><template #default="{row}"><StatusBadge v-if="row.operator" status="success" label="✓" /><span v-else>-</span></template></el-table-column>
        <el-table-column label="管理" width="60"><template #default="{row}"><StatusBadge v-if="row.admin" status="success" label="✓" /><span v-else>-</span></template></el-table-column>
      </el-table>
    </el-card>

    <!-- 高风险权限说明 -->
    <el-card header="高风险权限" class="section">
      <el-alert type="warning" :closable="false" show-icon
        title="以下操作即使前端可见，也必须由后端执行最终校验"
        description="SET_POINT / SET_THRESHOLD / SET_FTP / SET_LOGININFO / SET_FSUREBOOT / 自动升级 / Scheduler 启停 / 真实 run-once 均需 operator + reason + confirmation_token + 白名单 + 审计。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const roleCards = [
  { code:'read_only', name:'只读用户', risk:'low', description:'查看 B接口只读页面。不可执行写操作。', permCount:11 },
  { code:'operator', name:'运维用户', risk:'medium', description:'含只读 + dry_run/mock/只读 run-once。不可执行 SET。', permCount:18 },
  { code:'admin', name:'管理员', risk:'high', description:'含运维 + 用户/角色/权限管理 + 高风险规划。所有高风险操作最终由后端校验。', permCount:25 }
]

const roleList = [
  { code:'read_only', name:'只读用户', riskLevel:'low', builtin:true, permissionCount:11 },
  { code:'operator', name:'运维用户', riskLevel:'medium', builtin:true, permissionCount:18 },
  { code:'admin', name:'管理员', riskLevel:'high', builtin:true, permissionCount:25 }
]

const permList = [
  { group:'B接口查看', code:'binterface.read', riskLevel:'low', readOnly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.fsu.read', riskLevel:'low', readOnly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.realtime.read', riskLevel:'low', readOnly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.alarm.read', riskLevel:'low', readOnly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.threshold.read', riskLevel:'low', readOnly:true, operator:true, admin:true },
  { group:'B接口只读操作', code:'binterface.dry_run', riskLevel:'low', readOnly:false, operator:true, admin:true },
  { group:'B接口只读操作', code:'binterface.mock', riskLevel:'low', readOnly:false, operator:true, admin:true },
  { group:'B接口只读操作', code:'binterface.get.run', riskLevel:'medium', readOnly:false, operator:true, admin:true },
  { group:'调度', code:'scheduler.read', riskLevel:'low', readOnly:false, operator:true, admin:true },
  { group:'调度', code:'scheduler.manage', riskLevel:'medium', readOnly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_point.plan', riskLevel:'high', readOnly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_threshold.plan', riskLevel:'high', readOnly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_ftp.plan', riskLevel:'high', readOnly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_fsureboot.plan', riskLevel:'critical', readOnly:false, operator:false, admin:true },
  { group:'系统管理', code:'user.manage', riskLevel:'high', readOnly:false, operator:false, admin:true },
  { group:'系统管理', code:'role.manage', riskLevel:'high', readOnly:false, operator:false, admin:true },
  { group:'审计', code:'audit.read', riskLevel:'low', readOnly:true, operator:true, admin:true },
]
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
