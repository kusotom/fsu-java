<template>
  <div>
    <PageHeader title="FSU 设备管理" description="管理动环采集设备 FSU" />
    <SearchPanel v-model="search" @search="fetchData" @reset="resetSearch">
      <el-form-item label="FSU编码"><el-input v-model="search.fsuCode" placeholder="FSU编码" clearable /></el-form-item>
      <el-form-item label="FSU名称"><el-input v-model="search.fsuName" placeholder="FSU名称" clearable /></el-form-item>
      <template #extra><el-button type="primary" @click="openDialog()">新增</el-button></template>
    </SearchPanel>
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="cabinetId" label="机柜ID" width="80" />
        <el-table-column prop="fsuCode" label="FSU编码" width="120" />
        <el-table-column prop="fsuName" label="FSU名称" min-width="160" />
        <el-table-column prop="manufacturer" label="厂商" width="80" />
        <el-table-column prop="model" label="型号" width="100" />
        <el-table-column prop="protocolVersion" label="协议" width="80" />
        <el-table-column prop="ipAddr" label="IP" width="130" />
        <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='ONLINE'?'success':row.status==='OFFLINE'?'danger':'info'">{{row.status}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog :title="editingId?'编辑FSU':'新增FSU'" v-model="dialogVisible" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="站点ID" required><el-input-number v-model="form.siteId" :min="1" /></el-form-item>
        <el-form-item label="机柜ID"><el-input-number v-model="form.cabinetId" :min="1" /></el-form-item>
        <el-form-item label="FSU编码" required><el-input v-model="form.fsuCode" /></el-form-item>
        <el-form-item label="FSU名称" required><el-input v-model="form.fsuName" /></el-form-item>
        <el-form-item label="厂商"><el-input v-model="form.manufacturer" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="协议"><el-input v-model="form.protocolVersion" /></el-form-item>
        <el-form-item label="IP"><el-input v-model="form.ipAddr" /></el-form-item>
        <el-form-item label="端口"><el-input-number v-model="form.port" :min="1" :max="65535" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" /><el-option label="维护" value="MAINTENANCE" /></el-select></el-form-item>
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
import { getFsuDevices, createFsuDevice, updateFsuDevice, deleteFsuDevice } from '@/api/resource'
import type { FsuDevice } from '@/types'

const loading = ref(false); const saving = ref(false)
const tableData = ref<FsuDevice[]>([]); const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const search = reactive({ fsuCode: '', fsuName: '' })
const form = reactive<FsuDevice>({ siteId: 0, fsuCode: '', fsuName: '', status: 'OFFLINE' })

async function fetchData() { loading.value = true; try { const res: any = await getFsuDevices(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } }
function resetSearch() { search.fsuCode = ''; search.fsuName = ''; fetchData() }
function openDialog(row?: FsuDevice) { editingId.value = row?.id ?? null; if (row) Object.assign(form, row); else { Object.assign(form, { siteId: 0, cabinetId: undefined, fsuCode: '', fsuName: '', status: 'OFFLINE', manufacturer: '', model: '', protocolVersion: '', ipAddr: '', port: undefined }) }; dialogVisible.value = true }
async function handleSave() { saving.value = true; try { editingId.value ? await updateFsuDevice(editingId.value, form) : await createFsuDevice(form); ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() } catch { ElMessage.error('保存失败') } finally { saving.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); await deleteFsuDevice(id); ElMessage.success('已删除'); fetchData() } catch { /* cancelled */ } }
onMounted(fetchData)
</script>
