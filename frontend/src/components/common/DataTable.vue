<template>
  <div class="data-table-wrapper">
    <div v-if="showRefresh || $slots.toolbar" class="data-table-toolbar">
      <slot name="toolbar" />
      <el-button v-if="showRefresh" :loading="loading" @click="$emit('refresh')" size="small" text>
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>
    <ApiErrorAlert v-if="error" :error="error" :warnings="warnings" :auditId="auditId" />
    <el-table v-loading="loading" :data="data" v-bind="$attrs" :empty-text="''">
      <slot />
      <template #empty>
        <EmptyState :type="error ? 'error' : 'empty'" :description="emptyTitle" />
      </template>
    </el-table>
    <div v-if="showPagination" class="data-table-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        small
        @size-change="emitPage"
        @current-change="emitPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import type { ApiError, ApiWarning } from '../../types/api'
import ApiErrorAlert from './ApiErrorAlert.vue'
import EmptyState from './EmptyState.vue'

const props = withDefaults(defineProps<{
  data: any[]
  loading?: boolean
  error?: ApiError | null
  warnings?: Array<string | ApiWarning>
  auditId?: string
  emptyTitle?: string
  showRefresh?: boolean
  showPagination?: boolean
  total?: number
  defaultPageSize?: number
}>(), {
  showRefresh: true,
  showPagination: false,
  defaultPageSize: 20
})

const emit = defineEmits<{
  refresh: []
  'update:page': [page: number, size: number]
}>()

const currentPage = ref(1)
const currentPageSize = ref(props.defaultPageSize)

function emitPage() {
  emit('update:page', currentPage.value, currentPageSize.value)
}

watch(() => props.total, () => { currentPage.value = 1 })
</script>

<style scoped>
.data-table-toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.data-table-pagination { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
