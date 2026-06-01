<template>
  <el-alert v-if="error" :title="title || '操作失败'" :type="alertType" :closable="closable" show-icon>
    <template #default>
      <div class="api-error-body">
        <div class="api-error-row">
          <ErrorCodeTag :code="error.code" :category="error.category" />
          <span class="api-error-msg">{{ error.message }}</span>
        </div>
        <AuditIdLink v-if="auditId" :auditId="auditId" />
        <WarningsList v-if="warnings?.length" :warnings="warnings" />
      </div>
    </template>
  </el-alert>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApiError, ApiWarning } from '../../types/api'
import { CATEGORY_TAG_TYPE } from '../../utils/apiError'
import ErrorCodeTag from './ErrorCodeTag.vue'
import AuditIdLink from './AuditIdLink.vue'
import WarningsList from './WarningsList.vue'

const props = withDefaults(defineProps<{
  error?: ApiError
  warnings?: Array<string | ApiWarning>
  auditId?: string
  title?: string
  closable?: boolean
}>(), {
  closable: true
})

const alertType = computed(() => {
  if (!props.error) return 'error'
  const cat = props.error.category ?? 'unknown'
  const t = CATEGORY_TAG_TYPE[cat] ?? 'info'
  return t === 'danger' ? 'error' : t === 'warning' ? 'warning' : 'info'
})
</script>

<style scoped>
.api-error-body { display: flex; flex-direction: column; gap: 4px; }
.api-error-row { display: flex; align-items: center; gap: 8px; }
.api-error-msg { color: #666; }
</style>
