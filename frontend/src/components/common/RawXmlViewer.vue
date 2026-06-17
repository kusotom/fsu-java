<template>
  <div class="raw-xml-viewer-container">
    <div v-if="showBanner" class="protocol-banner">
      协议诊断模式 — 原始 XML 报文，仅限授权人员使用
    </div>
    <div class="raw-xml-toolbar">
      <PermissionButton :permissions="['protocol:raw:view']">
        <el-button size="small" text @click="copyXml">复制</el-button>
      </PermissionButton>
      <PermissionButton :permissions="['protocol:raw:view', 'protocol:raw:download']">
        <el-button size="small" text @click="downloadXml">下载 XML</el-button>
      </PermissionButton>
      <el-button size="small" text @click="formatted = !formatted">
        {{ formatted ? '原始' : '格式化' }}
      </el-button>
    </div>
    <pre class="raw-xml-viewer">{{ formatted ? formatXml(xml) : xml }}</pre>
    <div v-if="!xml" class="state-placeholder" style="padding: 24px">
      <div class="state-title">暂无 XML 内容</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PermissionButton from '@/components/auth/PermissionButton.vue'

const props = withDefaults(defineProps<{
  xml?: string
  showBanner?: boolean
  readonly?: boolean
}>(), {
  showBanner: true,
  readonly: false,
})

const emit = defineEmits<{ download: [] }>()

const formatted = ref(false)

function formatXml(xml: string | undefined): string {
  if (!xml) return ''
  try {
    // Simple XML formatter
    let formatted_xml = ''
    let indent = 0
    const tab = '  '
    const lines = xml.replace(/>\s*</g, '>\n<').split('\n')
    for (const line of lines) {
      if (line.match(/^<\/\w/)) indent = Math.max(0, indent - 1)
      formatted_xml += tab.repeat(indent) + line.trim() + '\n'
      if (line.match(/^<\w[^>]*[^/]>$/) && !line.match(/^<[?!]/)) indent++
    }
    return formatted_xml
  } catch {
    return xml
  }
}

function copyXml() {
  if (!props.xml) return
  navigator.clipboard.writeText(props.xml).then(
    () => ElMessage.success('已复制到剪贴板'),
    () => ElMessage.warning('复制失败')
  )
}

function downloadXml() {
  if (!props.xml) return
  emit('download')
  const blob = new Blob([props.xml], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'message.xml'; a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.raw-xml-viewer-container { border-radius: var(--radius-panel); overflow: hidden; }
.raw-xml-toolbar {
  background: #2D2D2D; padding: 4px 12px; display: flex; gap: 8px;
}
.raw-xml-toolbar .el-button { color: #D4D4D4; }
</style>
