<template>
  <el-alert
    v-if="state !== 'normal'"
    :type="alertType"
    :closable="closable"
    show-icon
    style="margin-bottom: 16px"
  >
    <template #title>{{ title }}</template>
    <span v-if="description">{{ description }}</span>
    <span v-if="meta?.unmappedCount && meta.unmappedCount > 0" style="margin-left: 8px">
      <el-tag size="small" type="warning">未映射点位: {{ meta.unmappedCount }}</el-tag>
    </span>
  </el-alert>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface DataStateMeta {
  realDeviceAccessed?: boolean
  ackReceived?: boolean
  emptyData?: boolean
  valuesReturned?: number
  unmappedCount?: number
  parseError?: boolean
  errorCode?: string | number
  errorMessage?: string
  lastCollectTime?: string
  lastGetDataTime?: string
}

const props = withDefaults(defineProps<{
  state: 'normal' | 'api_not_found' | 'network_error' | 'unauthorized' | 'forbidden' | 'server_error' | 'empty' | 'ack_empty' | 'parse_error' | 'unmapped'
  meta?: DataStateMeta
  closable?: boolean
}>(), {
  state: 'normal',
  closable: false,
})

const stateMap: Record<string, { type: 'error' | 'warning' | 'info'; title: string; desc: string }> = {
  api_not_found: { type: 'error', title: '后端未接入 / API 不存在', desc: '接口返回 404，请联系管理员确认后端服务。' },
  network_error: { type: 'error', title: '网络异常或后端不可达', desc: '无法连接后端服务，请检查网络和后端状态。' },
  unauthorized: { type: 'warning', title: '未登录或登录失效', desc: '请重新登录后重试。' },
  forbidden: { type: 'warning', title: '无权限访问', desc: '当前账户没有访问此资源的权限。' },
  server_error: { type: 'error', title: '服务器内部错误', desc: '后端服务异常，请联系管理员。' },
  empty: { type: 'info', title: '暂无实时数据', desc: 'HTTP 200 成功但 data 为空。FSU 可能尚未上报测点值。' },
  ack_empty: { type: 'info', title: 'FSU 已 ACK，但未返回测点值', desc: 'realDeviceAccessed=true + ackReceived=true + emptyData=true。FSU 在线但当前无测点数据。' },
  parse_error: { type: 'warning', title: '协议响应解析失败', desc: '后端解析 FSU 响应时发生错误，请联系管理员检查协议兼容性。' },
  unmapped: { type: 'warning', title: '已收到数据，但存在未映射点位', desc: '部分点位尚未建立映射关系，请前往点位映射页面处理。' },
}

const alertType = computed(() => stateMap[props.state]?.type || 'info')
const title = computed(() => stateMap[props.state]?.title || '')
const description = computed(() => stateMap[props.state]?.desc || '')
</script>
