<template>
  <div class="empty-state">
    <el-icon class="empty-icon" :size="48">
      <component :is="icon" />
    </el-icon>
    <h3 v-if="title" class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <slot name="action">
      <el-button v-if="actionText" type="primary" @click="$emit('action')">{{ actionText }}</el-button>
    </slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Document, WarningFilled, Lock, CircleClose } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  type?: 'empty' | 'not-implemented' | 'error' | 'no-permission'
  actionText?: string
}>(), {
  type: 'empty'
})

defineEmits<{ action: [] }>()

const DEFAULT_TITLES: Record<string, string> = {
  empty: '暂无数据',
  'not-implemented': '接口未接入',
  error: '加载失败',
  'no-permission': '无访问权限'
}
const DEFAULT_DESCS: Record<string, string> = {
  empty: '当前没有可展示的数据',
  'not-implemented': '该功能接口尚未接入，请在后端完成后刷新',
  error: '数据加载失败，请稍后重试',
  'no-permission': '您没有访问此页面的权限'
}

const title = computed(() => props.title || DEFAULT_TITLES[props.type] || '')
const description = computed(() => props.description || DEFAULT_DESCS[props.type] || '')

const iconMap: Record<string, any> = {
  empty: Document,
  'not-implemented': WarningFilled,
  error: CircleClose,
  'no-permission': Lock
}
const icon = computed(() => iconMap[props.type] || Document)
</script>

<style scoped>
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 0; color: #999; }
.empty-icon { margin-bottom: 12px; }
.empty-title { margin: 0 0 8px; font-size: 16px; color: #666; }
.empty-desc { margin: 0 0 16px; font-size: 13px; }
</style>
