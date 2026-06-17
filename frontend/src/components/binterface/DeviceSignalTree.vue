<template>
  <div class="device-signal-tree">
    <EmptyState v-if="!fsuCode" type="empty" title="未选择 FSU" description="请选择 FSU 设备查看点位" />
    <el-tree
      v-else
      :data="treeData"
      :props="treeProps"
      node-key="id"
      default-expand-all
      highlight-current
      @node-click="onNodeClick"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <StatusBadge v-if="data.status" :status="data.status" size="small" />
          <span class="tree-node-label">{{ node.label }}</span>
          <el-tag v-if="data.type" size="small" type="info" effect="plain">{{ data.type }}</el-tag>
          <span v-if="data.meta" class="tree-node-meta">{{ data.meta }}</span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '../common/StatusBadge.vue'
import EmptyState from '../common/EmptyState.vue'

interface SignalNode {
  signalId: string
  spid?: string
  signalName?: string
  signalType?: string
  unit?: string
  mappingStatus?: string
  measuredVal?: string | number
  status?: string
}

interface DeviceNode {
  deviceId: string
  deviceCode?: string
  deviceName?: string
  deviceType?: string
  signals: SignalNode[]
}

interface FsuSignalTree {
  fsuCode: string
  fsuName?: string
  devices: DeviceNode[]
}

const props = defineProps<{
  data: FsuSignalTree | null
}>()

const emit = defineEmits<{
  'select-signal': [deviceId: string, signalId: string]
}>()

interface TreeNode {
  id: string
  label: string
  type?: string
  meta?: string
  status?: string
  children?: TreeNode[]
}

const treeProps = { children: 'children', label: 'label' }

const treeData = computed<TreeNode[]>(() => {
  if (!props.data) return []
  return props.data.devices.map(dev => {
    const signalChildren: TreeNode[] = (dev.signals || []).map(s => ({
      id: `${dev.deviceId}:${s.signalId}`,
      label: s.signalName || s.signalId,
      type: s.signalType || s.unit ? (s.signalType || '') + (s.unit ? ` (${s.unit})` : '') : undefined,
      meta: s.measuredVal !== undefined ? String(s.measuredVal) : undefined,
      status: s.mappingStatus,
      children: undefined
    }))
    return {
      id: dev.deviceId,
      label: `${dev.deviceName || dev.deviceId} [${dev.deviceId}]`,
      type: dev.deviceType,
      children: signalChildren.length > 0 ? signalChildren : undefined
    }
  })
})

const fsuCode = computed(() => props.data?.fsuCode)

function onNodeClick(data: TreeNode) {
  const parts = data.id.split(':')
  if (parts.length === 2) {
    emit('select-signal', parts[0], parts[1])
  }
}
</script>

<style scoped>
.device-signal-tree { min-height: 100px; }
.tree-node { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.tree-node-label { flex: 1; }
.tree-node-meta { color: #409EFF; font-weight: 500; }
</style>
