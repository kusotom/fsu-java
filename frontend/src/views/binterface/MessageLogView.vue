<template>
  <div>
    <PageHeader title="B接口报文日志" description="SOAP/XML 报文记录查询（只读）" />
    <el-card>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="direction" label="方向" width="100"><template #default="{row}"><el-tag :type="row.direction==='FSU_TO_SC'?'success':'info'">{{row.direction}}</el-tag></template></el-table-column>
        <el-table-column prop="commandCode" label="命令" width="120" />
        <el-table-column prop="messageType" label="消息类型" width="100" />
        <el-table-column prop="fsuCode" label="FSU编码" width="120" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="processTimeMs" label="耗时ms" width="80" />
        <el-table-column prop="createdAt" label="接收时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <PermissionGuard :permissions="['protocol:raw:view']">
              <el-button size="small" text type="primary" :disabled="!row.rawMessage" @click="openXmlDialog(row)">原始XML</el-button>
            </PermissionGuard>
            <PermissionGuard :permissions="['protocol:raw:download']">
              <el-button size="small" text type="primary" :disabled="!row.rawMessage" @click="downloadXml(row)">下载</el-button>
            </PermissionGuard>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="xmlDialogVisible" title="原始XML报文" width="800px" top="5vh">
      <pre style="max-height:60vh;overflow:auto;background:#1e1e1e;color:#d4d4d4;padding:16px;border-radius:4px;font-size:12px;white-space:pre-wrap;word-break:break-all">{{ xmlContent }}</pre>
      <template #footer>
        <el-button @click="copyXml" type="primary" size="small">复制到剪贴板</el-button>
        <el-button @click="xmlDialogVisible=false" size="small">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import PermissionGuard from '@/components/auth/PermissionGuard.vue'
import { getMessageLogs, getMessageLog } from '@/api/bInterface'
import type { BInterfaceMessageLog } from '@/types'

const loading = ref(false)
const tableData = ref<BInterfaceMessageLog[]>([])
const xmlDialogVisible = ref(false)
const xmlContent = ref('')
let currentXmlRow: any = null

async function openXmlDialog(row: BInterfaceMessageLog) {
  if (row.rawMessage) {
    xmlContent.value = row.rawMessage; currentXmlRow = row; xmlDialogVisible.value = true; return
  }
  try {
    const res:any = await getMessageLog(row.id!); const detail = res?.data || res
    xmlContent.value = detail?.rawMessage || '(无内容)'; currentXmlRow = detail
  } catch { xmlContent.value = '(加载失败)' }
  xmlDialogVisible.value = true
}

function downloadXml(row: BInterfaceMessageLog) {
  const content = row.rawMessage || ''
  if (!content) return
  const blob = new Blob([content], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = `msg-${row.id || 'unknown'}.xml`; a.click()
  URL.revokeObjectURL(url)
}

function copyXml() {
  if (!xmlContent.value) return
  navigator.clipboard.writeText(xmlContent.value).then(() => ElMessage.success('已复制')).catch(() => ElMessage.warning('复制失败'))
}

onMounted(async () => { loading.value = true; try { const res: any = await getMessageLogs(); tableData.value = res.data || [] } catch { tableData.value = [] } finally { loading.value = false } })
</script>
