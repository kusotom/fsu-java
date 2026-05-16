<template>
  <div>
    <PageHeader title="监控点位" description="管理各FSU下的监控采集点位" />
    <SearchPanel v-model="search" @search="fetchData" @reset="resetSearch">
      <el-form-item label="FSU ID"><el-input v-model="search.fsuId" placeholder="FSU ID" clearable /></el-form-item>
      <el-form-item label="点位编码"><el-input v-model="search.pointCode" placeholder="点位编码" clearable /></el-form-item>
      <template #extra><el-button type="primary" @click="openDialog()">新增</el-button></template>
    </SearchPanel>
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fsuId" label="FSU ID" width="80" />
        <el-table-column prop="pointCode" label="点位编码" width="120" />
        <el-table-column prop="pointName" label="点位名称" min-width="150" />
        <el-table-column prop="pointType" label="类型" width="80" />
        <el-table-column prop="dataType" label="数据类型" width="80" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'">{{row.status}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog :title="editingId?'编辑点位':'新增点位'" v-model="dialogVisible" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="FSU ID" required><el-input-number v-model="form.fsuId" :min="1" /></el-form-item>
        <el-form-item label="点位编码" required><el-input v-model="form.pointCode" /></el-form-item>
        <el-form-item label="点位名称" required><el-input v-model="form.pointName" /></el-form-item>
        <el-form-item label="点位类型" required><el-select v-model="form.pointType"><el-option label="AI-模拟量输入" value="AI" /><el-option label="DI-数字量输入" value="DI" /><el-option label="DO-数字量输出" value="DO" /><el-option label="PI-脉冲输入" value="PI" /></el-select></el-form-item>
        <el-form-item label="数据类型"><el-select v-model="form.dataType"><el-option label="数值" value="NUMBER" /><el-option label="文本" value="TEXT" /></el-select></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="告警上限"><el-input-number v-model="form.alarmUpper" :precision="2" /></el-form-item>
        <el-form-item label="告警下限"><el-input-number v-model="form.alarmLower" :precision="2" /></el-form-item>
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
import { getMonitoringPoints, createMonitoringPoint, updateMonitoringPoint, deleteMonitoringPoint } from '@/api/resource'
import type { MonitoringPoint } from '@/types'

const loading = ref(false); const saving = ref(false)
const tableData = ref<MonitoringPoint[]>([]); const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const search = reactive({ fsuId: '', pointCode: '' })
const form = reactive<MonitoringPoint>({ fsuId: 0, pointCode: '', pointName: '', pointType: 'AI', dataType: 'NUMBER', status: 'ACTIVE' })

async function fetchData() { loading.value = true; try { const res: any = await getMonitoringPoints(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } }
function resetSearch() { search.fsuId = ''; search.pointCode = ''; fetchData() }
function openDialog(row?: MonitoringPoint) { editingId.value = row?.id ?? null; if (row) Object.assign(form, row); else { Object.assign(form, { fsuId: 0, pointCode: '', pointName: '', pointType: 'AI', dataType: 'NUMBER', status: 'ACTIVE', unit: '', alarmUpper: undefined, alarmLower: undefined }) }; dialogVisible.value = true }
async function handleSave() { saving.value = true; try { editingId.value ? await updateMonitoringPoint(editingId.value, form) : await createMonitoringPoint(form); ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() } catch { ElMessage.error('保存失败') } finally { saving.value = false } }
async function handleDelete(id: number) { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); await deleteMonitoringPoint(id); ElMessage.success('已删除'); fetchData() } catch { /* cancelled */ } }
onMounted(fetchData)
</script>
