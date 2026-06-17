<template>
  <span v-if="auditId" class="audit-id">
    <el-text size="small" type="info">Audit: {{ auditId }}</el-text>
    <el-button link size="small" @click="copy">
      <el-icon><CopyDocument /></el-icon>
    </el-button>
  </span>
</template>

<script setup lang="ts">
import { CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{ auditId?: string }>()

function copy() {
  if (props.auditId) {
    navigator.clipboard.writeText(props.auditId)
    ElMessage.success('已复制审计ID')
  }
}
</script>
