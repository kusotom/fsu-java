<template>
  <div>
    <PageHeader title="点位字典" description="平台标准采集点位定义、类型、单位和告警阈值" />

    <DataStateAlert v-if="dataState !== 'normal'" :state="dataState" />

    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="标准点位" :value="totalCount" status="info" :loading="loading" />
      <MetricCard title="AI 模拟量" :value="aiCount" status="normal" :loading="loading" />
      <MetricCard title="DI 数字量" :value="diCount" status="normal" :loading="loading" />
      <MetricCard title="未确认单位" :value="unknownUnitCount" :status="unknownUnitCount > 0 ? 'warning' : 'normal'" :loading="loading" />
    </div>

    <FilterPanel style="margin-bottom: 16px">
      <el-select v-model="filterType" placeholder="点位类型" clearable size="small" style="width: 140px">
        <el-option label="AI 模拟量" value="AI" />
        <el-option label="DI 数字量" value="DI" />
        <el-option label="DO 控制量" value="DO" />
        <el-option label="PI 脉冲量" value="PI" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 120px">
        <el-option label="启用" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="点位编码/名称" clearable size="small" style="width: 180px" />
    </FilterPanel>

    <div class="app-card data-table" style="padding: 0; overflow: hidden">
      <el-table :data="filteredData" stripe v-loading="loading" size="small" empty-text=" ">
        <el-table-column prop="pointCode" label="点位编码" width="140" />
        <el-table-column prop="pointName" label="点位名称" min-width="160" />
        <el-table-column prop="pointType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.pointType === 'DI' ? 'info' : ''">{{ row.pointType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="数据类型" width="80" />
        <el-table-column prop="unit" label="单位" width="80">
          <template #default="{ row }">
            <span v-if="row.unit">{{ row.unit }}</span>
            <span v-else style="color: var(--status-warning); font-size: 12px">待确认</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && filteredData.length === 0 && dataState === 'normal'"
        type="empty" title="暂无标准点位" :description="filterActive ? '当前筛选条件下无匹配结果' : '请先导入或创建标准点位字典'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FilterPanel from '@/components/common/FilterPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { getMonitoringPoints } from '@/api/resource'

const loading = ref(false)
const rawData = ref<any[]>([])
const filterType = ref('')
const filterStatus = ref('')
const filterKeyword = ref('')
const dataState = ref<'normal' | 'api_not_found' | 'network_error' | 'unauthorized' | 'forbidden' | 'server_error' | 'empty'>('normal')

const filteredData = computed(() => {
  let rows = rawData.value
  if (filterType.value) rows = rows.filter(r => r.pointType === filterType.value)
  if (filterStatus.value) rows = rows.filter(r => r.status === filterStatus.value)
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    rows = rows.filter(r => (r.pointCode || '').toLowerCase().includes(kw) || (r.pointName || '').toLowerCase().includes(kw))
  }
  return rows
})

const totalCount = computed(() => rawData.value.length)
const aiCount = computed(() => rawData.value.filter(r => r.pointType === 'AI').length)
const diCount = computed(() => rawData.value.filter(r => r.pointType === 'DI').length)
const unknownUnitCount = computed(() => rawData.value.filter(r => !r.unit).length)
const filterActive = computed(() => !!(filterType.value || filterStatus.value || filterKeyword.value))

onMounted(async () => {
  loading.value = true; dataState.value = 'normal'
  try {
    const res: any = await getMonitoringPoints()
    rawData.value = Array.isArray(res?.data) ? res.data : (res?.data?.data || [])
    if (rawData.value.length === 0) dataState.value = 'empty'
  } catch (e: any) {
    rawData.value = []
    if (e?.response?.status === 404) dataState.value = 'api_not_found'
    else if (e?.response?.status === 401) dataState.value = 'unauthorized'
    else if (e?.response?.status === 403) dataState.value = 'forbidden'
    else if (e?.response?.status && e.response.status >= 500) dataState.value = 'server_error'
    else if (e?.request && !e?.response) dataState.value = 'network_error'
    else dataState.value = 'server_error'
  } finally { loading.value = false }
})
</script>
