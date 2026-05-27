<template>
  <div>
    <PageHeader title="用户管理" description="平台用户、角色与权限规划（不展示密码）" />

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>权限说明</template>
      前端权限仅用于页面和按钮展示控制，真实安全必须由后端校验。高风险 B接口操作必须经过后端角色校验 + 白名单 + confirmation_token + 审计。
    </el-alert>

    <!-- 角色概览 -->
    <el-row :gutter="16" class="section">
      <el-col :span="8" v-for="role in roles" :key="role.code">
        <el-card shadow="hover">
          <template #header><el-tag :type="role.risk === 'high' ? 'danger' : role.risk === 'medium' ? 'warning' : 'success'">{{ role.name }}</el-tag></template>
          <p style="color:#666;font-size:13px;margin-bottom:8px">{{ role.description }}</p>
          <el-text size="small" type="info">权限点数: {{ role.permissions?.length || 0 }}</el-text>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户列表 -->
    <el-card header="用户列表" class="section">
      <div style="margin-bottom:12px"><el-button type="primary" @click="openDialog()">新增</el-button></div>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="50" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="displayName" label="显示名" width="120" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column label="状态" width="80"><template #default="{row}"><StatusBadge :status="row.status === 'ACTIVE' ? 'active' : 'inactive'" :label="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}"><el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button><el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色权限矩阵 -->
    <el-card class="section">
      <template #header><span>角色权限矩阵 <el-tag size="small" type="warning">frontend-planning</el-tag></span></template>
      <el-table :data="permissionMatrix" size="small" stripe>
        <el-table-column prop="group" label="权限组" width="120" />
        <el-table-column prop="code" label="权限点" width="200" />
        <el-table-column label="风险" width="70"><template #default="{row}"><el-tag v-if="row.risk === 'critical'" size="small" type="danger">严重</el-tag><el-tag v-else-if="row.risk === 'high'" size="small" type="warning">高</el-tag><span v-else>-</span></template></el-table-column>
        <el-table-column label="只读" width="60"><template #default="{row}"><StatusBadge v-if="row.readonly" status="success" label="✓" /><span v-else>-</span></template></el-table-column>
        <el-table-column label="运维" width="60"><template #default="{row}"><StatusBadge v-if="row.operator" status="success" label="✓" /><span v-else>-</span></template></el-table-column>
        <el-table-column label="管理" width="60"><template #default="{row}"><StatusBadge v-if="row.admin" status="success" label="✓" /><span v-else>-</span></template></el-table-column>
      </el-table>
    </el-card>

    <!-- 高风险权限提示 -->
    <el-card header="高风险权限" class="section">
      <el-alert type="warning" :closable="false" show-icon
        title="以下操作即使前端可见，也必须由后端执行最终校验"
        description="SET_POINT / SET_THRESHOLD / SET_FTP / SET_LOGININFO / SET_FSUREBOOT / 自动升级 / Scheduler 启停 / 真实 run-once 均需 operator + reason + confirmation_token + 白名单 + 审计。" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="editingId ? '编辑用户' : '新增用户'" v-model="dialogVisible" width="500px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="活跃" value="ACTIVE" /><el-option label="禁用" value="DISABLED" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getUsers, createUser, updateUser, deleteUser } from '@/api/system'
import type { UserAccount } from '@/types'

const loading = ref(false); const tableData = ref<UserAccount[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const form = ref<Partial<UserAccount>>({ username: '', displayName: '', email: '', status: 'ACTIVE' })

const roles = [
  { code:'read_only', name:'只读用户', risk:'low', description:'查看FSU状态/实时数据/告警/门限/FTP/调度/协议。不可执行写操作。', permissions:['binterface.read*','audit.read'] },
  { code:'operator', name:'运维用户', risk:'medium', description:'包含只读+可执行dry_run/mock/只读run-once。不可执行SET。', permissions:['+binterface.dry_run','+binterface.mock','+binterface.get.run'] },
  { code:'admin', name:'管理员', risk:'high', description:'包含运维+管理用户/角色/权限+高风险操作。所有高风险最终由后端校验。', permissions:['+user.*','+role.*','+scheduler.*','+set.*.plan'] }
]

const permissionMatrix = [
  { group:'B接口查看', code:'binterface.read', risk:'low', readonly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.fsu.read', risk:'low', readonly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.realtime.read', risk:'low', readonly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.alarm.read', risk:'low', readonly:true, operator:true, admin:true },
  { group:'B接口查看', code:'binterface.threshold.read', risk:'low', readonly:true, operator:true, admin:true },
  { group:'B接口只读操作', code:'binterface.dry_run', risk:'low', readonly:false, operator:true, admin:true },
  { group:'B接口只读操作', code:'binterface.mock', risk:'low', readonly:false, operator:true, admin:true },
  { group:'B接口只读操作', code:'binterface.get.run', risk:'medium', readonly:false, operator:true, admin:true },
  { group:'调度', code:'scheduler.read', risk:'low', readonly:false, operator:true, admin:true },
  { group:'调度', code:'scheduler.manage', risk:'medium', readonly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_point.plan', risk:'high', readonly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_threshold.plan', risk:'high', readonly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_ftp.plan', risk:'high', readonly:false, operator:false, admin:true },
  { group:'高风险 SET', code:'binterface.set_fsureboot.plan', risk:'critical', readonly:false, operator:false, admin:true },
  { group:'系统管理', code:'user.manage', risk:'high', readonly:false, operator:false, admin:true },
  { group:'系统管理', code:'role.manage', risk:'high', readonly:false, operator:false, admin:true },
  { group:'审计', code:'audit.read', risk:'low', readonly:true, operator:true, admin:true },
]

async function fetchData() {
  loading.value = true; try { const res: any = await getUsers(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false }
}

function openDialog(row?: UserAccount) {
  editingId.value = row?.id ?? null
  form.value = { username: row?.username || '', displayName: row?.displayName || '', email: row?.email || '', status: row?.status || 'ACTIVE' }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.username) { ElMessage.warning('用户名必填'); return }
  try {
    if (editingId.value) { await updateUser(editingId.value, form.value as UserAccount); ElMessage.success('已更新') }
    else { await createUser(form.value as UserAccount); ElMessage.success('已创建') }
    dialogVisible.value = false; await fetchData()
  } catch { ElMessage.error('操作失败') }
}

async function handleDelete(row: UserAccount) {
  try { await ElMessageBox.confirm('确认删除该用户？'); await deleteUser(row.id!); await fetchData(); ElMessage.success('已删除') } catch {}
}

function resetForm() { editingId.value = null; form.value = { username: '', displayName: '', email: '', status: 'ACTIVE' } }

onMounted(fetchData)
</script>

<style scoped>
.section { margin-bottom: 16px; }
</style>
