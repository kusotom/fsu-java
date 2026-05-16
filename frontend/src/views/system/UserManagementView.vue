<template>
  <div>
    <PageHeader title="用户管理" description="管理系统用户（不展示密码）" />
    <el-card>
      <div style="margin-bottom:12px"><el-button type="primary" @click="openDialog()">新增</el-button></div>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="displayName" label="显示名" width="140" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'danger'">{{row.status}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160"><template #default="{row}"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog :title="editingId?'编辑用户':'新增用户'" v-model="dialogVisible" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="启用" value="ACTIVE" /><el-option label="禁用" value="DISABLED" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave" :loading="saving">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getUsers, createUser, updateUser, deleteUser } from '@/api/system'
import type { UserAccount } from '@/types'

const loading = ref(false); const saving = ref(false)
const tableData = ref<UserAccount[]>([]); const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const form = reactive<UserAccount>({ username: '', status: 'ACTIVE' })

async function fetchData() { loading.value = true; try { const res: any = await getUsers(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } }
function openDialog(row?: UserAccount) { editingId.value = row?.id ?? null; if (row) Object.assign(form, row); else Object.assign(form, { username: '', displayName: '', email: '', phone: '', status: 'ACTIVE' }); dialogVisible.value = true }
async function handleSave() { saving.value = true; try { editingId.value ? await updateUser(editingId.value, form) : await createUser(form); ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() } catch { ElMessage.error('保存失败') } finally { saving.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); await deleteUser(id); ElMessage.success('已删除'); fetchData() } catch { /* cancelled */ } }
onMounted(fetchData)
</script>
