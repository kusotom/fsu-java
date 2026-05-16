<template>
  <div>
    <PageHeader title="机柜管理" description="管理户外柜、室内柜等物理资产" />
    <SearchPanel v-model="search" @search="fetchData" @reset="resetSearch">
      <el-form-item label="编码"><el-input v-model="search.cabinetCode" placeholder="机柜编码" clearable /></el-form-item>
      <el-form-item label="名称"><el-input v-model="search.cabinetName" placeholder="机柜名称" clearable /></el-form-item>
      <template #extra><el-button type="primary" @click="openDialog()">新增</el-button></template>
    </SearchPanel>
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="siteId" label="站点ID" width="80" />
        <el-table-column prop="cabinetCode" label="机柜编码" width="120" />
        <el-table-column prop="cabinetName" label="机柜名称" min-width="160" />
        <el-table-column prop="cabinetType" label="类型" width="100" />
        <el-table-column prop="model" label="型号" width="100" />
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'">{{row.status}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog :title="editingId?'编辑机柜':'新增机柜'" v-model="dialogVisible" width="560px" @close="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="站点ID" required><el-input-number v-model="form.siteId" :min="1" /></el-form-item>
        <el-form-item label="机柜编码" required><el-input v-model="form.cabinetCode" /></el-form-item>
        <el-form-item label="机柜名称" required><el-input v-model="form.cabinetName" /></el-form-item>
        <el-form-item label="机柜类型"><el-input v-model="form.cabinetType" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
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
import { getCabinets, createCabinet, updateCabinet, deleteCabinet } from '@/api/resource'
import type { Cabinet } from '@/types'

const loading = ref(false); const saving = ref(false)
const tableData = ref<Cabinet[]>([]); const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const search = reactive({ cabinetCode: '', cabinetName: '' })
const form = reactive<Cabinet>({ siteId: 0, cabinetCode: '', cabinetName: '', status: 'ACTIVE' })

async function fetchData() { loading.value = true; try { const res: any = await getCabinets(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } }
function resetSearch() { search.cabinetCode = ''; search.cabinetName = ''; fetchData() }
function openDialog(row?: Cabinet) { editingId.value = row?.id ?? null; if (row) Object.assign(form, row); else resetForm(); dialogVisible.value = true }
function resetForm() { editingId.value = null; Object.assign(form, { siteId: 0, cabinetCode: '', cabinetName: '', status: 'ACTIVE' }) }
async function handleSave() { saving.value = true; try { editingId.value ? await updateCabinet(editingId.value, form) : await createCabinet(form); ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() } catch { ElMessage.error('保存失败') } finally { saving.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); await deleteCabinet(id); ElMessage.success('已删除'); fetchData() } catch { /* cancelled */ } }
onMounted(fetchData)
</script>
