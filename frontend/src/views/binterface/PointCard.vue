<template>
  <el-card shadow="hover" class="point-card" :class="{ 'di-card': isDI, 'no-value': !hasValue }">
    <div class="point-name">{{ displayName }}</div>
    <div class="point-code">{{ displayCode }}</div>
    <div class="point-type">
      <el-tag :type="isDI ? 'warning' : 'success'" size="small">{{ displayType }}</el-tag>
      <span v-if="displayUnit" class="unit">{{ displayUnit }}</span>
      <span v-else-if="isDI" class="unit">无单位</span>
      <span v-else-if="!isDI" class="unit-warn">单位待确认</span>
      <el-tag v-if="rt?.needRealDataConfirm" type="warning" size="small" style="margin-left: 6px">待确认</el-tag>
    </div>

    <!-- Value display -->
    <div class="value-section">
      <template v-if="hasNumeric">
        <span class="value">{{ numericValue }}</span>
        <span v-if="displayUnit" class="unit">{{ displayUnit }}</span>
        <span v-else-if="isDI" class="unit">无单位</span>
        <span v-else class="unit-warn">单位待确认</span>
      </template>
      <template v-else-if="hasText">
        <span class="value">{{ textValue }}</span>
      </template>
      <template v-else>
        <el-tag type="info" size="small">暂无实时值</el-tag>
      </template>
      <div v-if="rt?.valueMeaning" class="value-meaning">{{ rt.valueMeaning }}</div>
    </div>

    <!-- DI warning -->
    <div v-if="isDI" class="di-warn">
      <el-icon><WarningFilled /></el-icon> DI 含义按后端字典显示，需现场复核
    </div>

    <!-- Quality -->
    <div class="meta">
      <span v-if="rt?.mappingStatus">映射: {{ mappingStatusLabel(rt.mappingStatus) }}</span>
      <span v-if="rt?.mappingConfidence">置信度: {{ rt.mappingConfidence }}</span>
      <span v-if="rt?.source || rt?.templateVariant">来源: {{ rt.source || rt.templateVariant }}</span>
      <span v-if="rt?.quality">质量: {{ rt.quality }}</span>
      <span v-if="rt?.collectTime">采集: {{ rt.collectTime?.substring(0,16) }}</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import { mappingStatusLabel } from '@/utils/mappingStatus'

const props = defineProps<{ point: any; rt: any }>()

const displayName = computed(() => props.rt?.signalName || props.point?.pointName || '未命名点位')
const displayCode = computed(() => props.rt?.spid || props.rt?.signalId || props.point?.pointCode || '-')
const displayType = computed(() => props.rt?.signalType || props.point?.pointType || '-')
const displayUnit = computed(() => props.rt?.unit || props.point?.unit || '')
const isDI = computed(() => displayType.value === 'DI')
const hasValue = computed(() => props.rt != null)
const numericValue = computed(() => props.rt?.valueNumber ?? props.rt?.value)
const textValue = computed(() => props.rt?.valueText ?? props.rt?.value)
const hasNumeric = computed(() => props.rt?.valueNumber != null || (props.rt?.value !== '' && props.rt?.value != null && !Number.isNaN(Number(props.rt.value))))
const hasText = computed(() => props.rt?.valueText != null || props.rt?.value != null)

</script>

<style scoped>
.point-card { min-height: 140px; }
.di-card { border-left: 3px solid var(--el-color-warning); }
.no-value { border-left: 3px solid var(--el-color-info); }
.point-name { font-size: 15px; font-weight: 600; margin-bottom: 2px; }
.point-code { font-size: 12px; color: #909399; margin-bottom: 6px; }
.point-type { margin-bottom: 8px; }
.unit { margin-left: 6px; color: #606266; font-size: 13px; }
.unit-warn { margin-left: 6px; color: var(--el-color-warning); font-size: 12px; }
.value-section { margin: 8px 0; }
.value { font-size: 24px; font-weight: 700; color: var(--el-color-primary); }
.value-meaning { margin-top: 4px; font-size: 12px; color: #606266; }
.di-warn { margin-top: 6px; padding: 4px 8px; background: #fdf6ec; border-radius: 4px; font-size: 12px; color: #e6a23c; display: flex; align-items: center; gap: 4px; }
.meta { margin-top: 8px; font-size: 12px; color: #909399; display: flex; flex-direction: column; gap: 2px; }
</style>
