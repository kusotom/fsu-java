<template>
  <div>
    <PageHeader title="总览" description="机房动环监控平台运行概览" />
    <el-alert type="warning" show-icon :closable="false" style="margin-bottom: 16px">
      <template #title>后端未连接或数据待接入，以下数据为占位展示</template>
    </el-alert>
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in cards" :key="card.label">
        <el-card style="text-align: center; margin-bottom: 16px">
          <div style="font-size: 32px; font-weight: bold; color: #409eff">{{ card.value }}</div>
          <div style="color: #909399; margin-top: 8px">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'

const cards = [
  { label: '站点总数', value: '-' },
  { label: 'FSU 总数', value: '-' },
  { label: '在线 FSU', value: '-' },
  { label: '当前告警', value: '-' },
  { label: 'B接口报文', value: '-' },
  { label: '今日调用', value: '-' }
]
</script>
