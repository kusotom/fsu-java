<template>
  <el-tooltip :content="tooltip || label" :disabled="!tooltip && !showTooltip" placement="top">
    <el-tag class="status-badge" :type="tagType" :size="size" :effect="effect">
      {{ innerLabel }}
    </el-tag>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const STATUS_MAP: Record<string, { type: string; label: string }> = {
  online: { type: 'success', label: '在线' },
  offline: { type: 'danger', label: '离线' },
  unknown: { type: 'info', label: '未知' },
  disabled: { type: 'info', label: '禁用' },
  maintenance: { type: 'warning', label: '维护中' },
  success: { type: 'success', label: '成功' },
  failed: { type: 'danger', label: '失败' },
  warning: { type: 'warning', label: '警告' },
  blocked: { type: 'warning', label: '已阻止' },
  safety_blocked: { type: 'warning', label: '安全阻止' },
  not_whitelisted: { type: 'warning', label: '未授权' },
  business_failure: { type: 'warning', label: '业务失败' },
  circuit_open: { type: 'info', label: '熔断' },
  mapped: { type: 'success', label: '已映射' },
  unmapped: { type: 'warning', label: '未映射' },
  mapped_candidate: { type: 'success', label: '候选映射' },
  verified_by_real_data: { type: 'success', label: '真实确认' },
  template_only: { type: 'warning', label: '模板存在，真实未确认' },
  pending_real_data: { type: 'warning', label: '待真实数据确认' },
  device_only: { type: 'info', label: '设备已发现，待测点上报' },
  signal_pending: { type: 'info', label: '设备已发现，待测点上报' },
  wait_get_data: { type: 'info', label: '等待测点上报' },
  historical_pending_backfill: { type: 'info', label: '历史旧数据，待回填' },
  unknown_event_id: { type: 'warning', label: '未知告警事件' },
  unknown_signal_id: { type: 'warning', label: '未知 SignalID' },
  unknown_device_signal_pair: { type: 'warning', label: '设备与点位组合未确认' },
  manual_mapped: { type: '', label: '手动映射' },
  conflict: { type: 'danger', label: '冲突' },
  active: { type: 'success', label: '活跃' },
  inactive: { type: 'info', label: '非活跃' }
}

const props = withDefaults(defineProps<{
  status?: string
  label?: string
  size?: 'large' | 'default' | 'small'
  tooltip?: string
  effect?: 'dark' | 'light' | 'plain'
}>(), {
  status: 'unknown',
  size: 'small',
  effect: 'light'
})

const normalizedStatus = computed(() => (props.status || 'unknown').toLowerCase())
const entry = computed(() => STATUS_MAP[normalizedStatus.value] ?? { type: 'info', label: props.status })
const tagType = computed(() => entry.value.type || 'info')
const innerLabel = computed(() => props.label || entry.value.label)
const showTooltip = computed(() => !!props.tooltip)
</script>
