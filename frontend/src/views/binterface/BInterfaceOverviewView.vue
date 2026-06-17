<template>
  <div>
    <PageHeader title="B接口总览" description="中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0" />
    <el-alert v-if="connectionStatus === 'not_connected'" type="warning" show-icon :closable="false" style="margin-bottom:16px" title="后端未连接，以下信息为静态占位" />
    <el-alert v-else-if="connectionStatus === 'partial'" type="info" show-icon :closable="false" style="margin-bottom:16px" title="部分后端接口未接入，数据可能不完整" />
    <el-row :gutter="16">
      <el-col :span="8" v-for="card in cards" :key="card.label">
        <el-card style="margin-bottom:16px">
          <div style="font-size:14px;color:#909399">{{card.label}}</div>
          <div style="font-size:24px;font-weight:bold;margin:8px 0">{{card.value}}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card>
      <template #header>安全策略</template>
      <el-tag type="danger" size="large">SET_FSUREBOOT — 远程重启FSU — 默认禁用，不可通过界面启用</el-tag>
      <div style="margin-top:12px;color:#909399">SOAP/XML 解析：{{ cards[3].value === '已启用' ? '已启用' : '未实现' }} | WSDL：{{ cards[4].value === '已启用' ? '已启用' : '未实现' }} | 真实 FSU 调用：{{ cards[2].value !== 'STUB' ? '已接入' : '未实现' }}</div>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getBInterfaceHealth } from '@/api/bInterface'

// FE-P0-RECTIFY-001: 状态口径统一
const connectionStatus = ref<'connected' | 'not_connected' | 'partial'>('connected')
const cards = reactive([
  { label: '协议标准', value: 'B接口 2016 V1.0' },
  { label: 'SCService', value: '检测中...' },
  { label: 'FSUService Client', value: '检测中...' },
  { label: 'SOAP/XML', value: '检测中...' },
  { label: 'WSDL', value: '检测中...' },
  { label: '安全策略', value: 'SET_FSUREBOOT 禁用' }
])

onMounted(async () => {
  try {
    const res = await getBInterfaceHealth() as any
    const d = res?.data
    cards[0].value = d?.bInterfaceVersion || 'B接口 2016 V1.0'
    cards[1].value = d?.scServiceStatus || 'UNKNOWN'
    cards[2].value = d?.fsuServiceStatus || 'UNKNOWN'
    cards[3].value = d?.soapEnabled ? '已启用' : '未启用'
    cards[4].value = d?.wsdlEnabled ? '已启用' : '未启用'
  } catch (e: any) {
    connectionStatus.value = e?.request && !e?.response ? 'not_connected' : 'partial'
  }
})
</script>
