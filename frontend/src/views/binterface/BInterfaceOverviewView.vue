<template>
  <div>
    <PageHeader title="B接口总览" description="中国铁塔动环监控系统统一互联 B 接口技术规范（试行）V1.0" />
    <el-alert v-if="backendError" type="warning" show-icon :closable="false" style="margin-bottom:16px" title="后端未连接，以下信息为静态占位" />
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
      <div style="margin-top:12px;color:#909399">SOAP/XML 解析：未实现 | FTP 连接：未实现 | 真实 FSU 调用：未实现</div>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getBInterfaceHealth } from '@/api/bInterface'

const backendError = ref(false)
const cards = reactive([
  { label: '协议标准', value: 'B接口 2016 V1.0' },
  { label: 'SCService', value: 'STUB' },
  { label: 'FSUService Client', value: 'STUB' },
  { label: 'SOAP/XML', value: '待实现' },
  { label: 'FTP', value: '待实现' },
  { label: '安全策略', value: 'SET_FSUREBOOT 禁用' }
])

onMounted(async () => { try { await getBInterfaceHealth() } catch { backendError.value = true } })
</script>
