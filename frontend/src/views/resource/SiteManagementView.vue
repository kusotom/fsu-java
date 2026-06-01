<template>
  <div>
    <PageHeader title="站点监控" description="按站点组织机房、户外柜、FSU 绑定关系与实时数据入口" />
    <SiteMonitorTabs />
    <SearchPanel v-model="search" @search="fetchData" @reset="resetSearch">
      <el-form-item label="编码"><el-input v-model="search.siteCode" placeholder="站点编码" clearable /></el-form-item>
      <el-form-item label="名称"><el-input v-model="search.siteName" placeholder="站点名称" clearable /></el-form-item>
      <el-form-item label="状态"><el-select v-model="search.status" placeholder="全部" clearable><el-option label="启用" value="ACTIVE" /><el-option label="停用" value="INACTIVE" /></el-select></el-form-item>
      <template #extra><el-button type="primary" @click="openDialog()">新增</el-button></template>
    </SearchPanel>
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="siteCode" label="站点编码" width="120" />
        <el-table-column prop="siteName" label="站点名称" min-width="160" />
        <el-table-column prop="region" label="区域" width="120" />
        <el-table-column prop="address" label="地址" min-width="180" />
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'">{{row.status}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog :title="editingId?'编辑站点':'新增站点'" v-model="dialogVisible" width="560px" @close="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="站点编码" required><el-input v-model="form.siteCode" /></el-form-item>
        <el-form-item label="站点名称" required><el-input v-model="form.siteName" /></el-form-item>
        <el-form-item label="站点类型"><el-input v-model="form.siteType" /></el-form-item>
        <el-form-item label="区域"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="启用" value="ACTIVE" /><el-option label="停用" value="INACTIVE" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave" :loading="saving">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import SearchPanel from '@/components/SearchPanel.vue'
import SiteMonitorTabs from '@/components/SiteMonitorTabs.vue'
import { getSites, createSite, updateSite, deleteSite } from '@/api/resource'
import type { Site } from '@/types'

const loading = ref(false); const saving = ref(false)
const tableData = ref<Site[]>([]); const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const search = reactive({ siteCode: '', siteName: '', status: '' })
const form = reactive<Site>({ siteCode: '', siteName: '', status: 'ACTIVE' })

async function fetchData() { loading.value = true; try { const res: any = await getSites(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } }
function resetSearch() { search.siteCode = ''; search.siteName = ''; search.status = ''; fetchData() }
function openDialog(row?: Site) { editingId.value = row?.id ?? null; if (row) Object.assign(form, row); dialogVisible.value = true }
function resetForm() { editingId.value = null; Object.assign(form, { siteCode: '', siteName: '', status: 'ACTIVE' }) }
async function handleSave() {
  saving.value = true
  try { editingId.value ? await updateSite(editingId.value, form) : await createSite(form); ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
  catch { ElMessage.error('保存失败') } finally { saving.value = false }
}
async function handleDelete(id: number) {
  try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); await deleteSite(id); ElMessage.success('已删除'); fetchData() } catch { /* cancelled */ }
}
onMounted(fetchData)
</script>
