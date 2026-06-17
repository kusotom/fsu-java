<template>
  <div>
    <PageHeader title="B接口实时数据" description="已入库 realtime_data，按后端 eStoneII-IO 字典映射展示 | FSU: 51051243812345 | Emerson 2016" />

    <!-- FE-P0-RECTIFY-002: 完整状态口径, 按优先级排序 -->
    <el-alert v-if="dataStatus === 'api_404'" type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>后端未接入 / API 不存在</template>
      API 返回 404。该接口可能尚未实现，请联系后端确认。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'network_error'" type="error" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>网络异常或后端不可达</template>
      无法连接到后端服务，请检查网络和后端状态。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'http_401'" type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>未登录或登录失效</template>
      登录凭证已过期，请重新登录。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'http_403'" type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>无权限访问</template>
      当前账号没有访问该接口的权限。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'http_5xx'" type="error" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>后端服务异常</template>
      后端返回服务器错误 (5xx)，请联系管理员。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'parse_error'" type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>协议响应解析失败</template>
      后端解析 FSU 响应时发生 parseError。请联系管理员检查协议兼容性。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'fsu_no_ack'" type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>FSU 通信失败或未访问真实设备</template>
      ackReceived=false 或 realDeviceAccessed=false。当前展示数据库已入库数据。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'fsu_ack_empty'" type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>FSU 已 ACK，但未返回测点值</template>
      realDeviceAccessed=true + ackReceived=true + emptyData=true。FSU 在线但当前无测点数据。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'empty'" type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>暂无实时数据</template>
      HTTP 200 成功但 data 为空。FSU {{ fsu }} 可能尚未上报测点值，或后端点位映射未完成。
    </el-alert>
    <el-alert v-else-if="dataStatus === 'unmapped'" type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>已收到数据，但存在未映射点位</template>
      未映射点位: {{ unmappedCount }} 个。请在未映射清单中按 DeviceID / SPID 复核厂家点位。
    </el-alert>
    <el-alert v-else type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>链路状态提示</template>
      最近 LOGIN: {{ fsuStatus?.lastLoginTime || '—' }} | 状态: {{ fsuStatus?.onlineStatus || '—' }}
      <span v-if="lastCollectTime"> | 最近采集: {{ lastCollectTime }}</span>
    </el-alert>

    <!-- FE-P0-RECTIFY-002: meta 信息面板 -->
    <el-descriptions v-if="metaVisible" border size="small" :column="4" style="margin-bottom:16px">
      <el-descriptions-item label="真实设备访问">{{ metaRealDeviceAccessed }}</el-descriptions-item>
      <el-descriptions-item label="ACK 已收到">{{ metaAckReceived }}</el-descriptions-item>
      <el-descriptions-item label="空数据">{{ metaEmptyData }}</el-descriptions-item>
      <el-descriptions-item label="返回值数量">{{ metaValuesReturned }}</el-descriptions-item>
      <el-descriptions-item label="未映射数量">{{ unmappedCount }}</el-descriptions-item>
      <el-descriptions-item label="最近采集时间">{{ lastCollectTime || '未提供' }}</el-descriptions-item>
      <el-descriptions-item label="最近 GET_DATA">{{ lastGetDataTime || '未提供' }}</el-descriptions-item>
      <el-descriptions-item label="解析错误">{{ metaParseError }}</el-descriptions-item>
    </el-descriptions>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-statistic title="总点位" :value="points.length" /></el-col>
      <el-col :span="6"><el-statistic title="有实时值" :value="pointsWithValue"><template #suffix><el-tag size="small" type="success"/></template></el-statistic></el-col>
      <el-col :span="6"><el-statistic title="DI编码待复核" :value="diCount"><template #suffix><el-tag size="small" type="warning"/></template></el-statistic></el-col>
    </el-row>

    <ApiErrorAlert v-if="error" :error="error" />
    <div style="margin-bottom:12px">
      <el-button :loading="loading" @click="loadData"><el-icon><Refresh /></el-icon> 刷新</el-button>
      <span v-if="lastRefresh" style="margin-left:12px;color:#909399;font-size:13px">{{ lastRefresh }}</span>
    </div>

    <el-card class="group" header="蓄电池组 — DeviceID: 51051240700002">
      <el-row :gutter="12">
        <el-col v-for="p in batteryPoints" :key="p.pointCode" :span="6"><PointCard :point="p" :rt="rtMap[p.pointCode]" /></el-col>
      </el-row>
    </el-card>

    <el-card class="group" header="机房/基站环境 (3设备)">
      <el-row :gutter="12">
        <el-col v-for="p in envPoints" :key="p.pointCode" :span="6"><PointCard :point="p" :rt="rtMap[p.pointCode]" /></el-col>
      </el-row>
    </el-card>

    <EmptyState v-if="!loading && points.length===0" type="empty" title="暂无实时点位" description="HTTP 200 成功但 data 为空，或 FSU 尚未返回测点值" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ApiErrorAlert from '@/components/common/ApiErrorAlert.vue'
import PointCard from './PointCard.vue'
import { getBInterfaceFsus, getBInterfaceRealtimePoints } from '@/api/bInterface'
import { HARDCODED_REALTIME_FSU_CODE } from '@/compat/realtimeSignalFilter'
import { isTrueUnmappedPoint } from '@/utils/mappingStatus'
import type { ApiError } from '@/types/api'

const loading=ref(false); const error=ref<ApiError|null>(null); const lastRefresh=ref('')
const fsuStatus=ref<any>(null); const points=ref<any[]>([]); const rtMap=ref<Record<string,any>>({})
// FE-P0-RECTIFY-002: 完整状态口径
const dataStatus = ref<'ok' | 'empty' | 'fsu_no_ack' | 'fsu_ack_empty' | 'parse_error' | 'api_404' | 'network_error' | 'http_401' | 'http_403' | 'http_5xx' | 'unmapped'>('ok')
const unmappedCount = ref(0)
// FE-P0-RECTIFY-002: meta 字段补齐
const lastCollectTime = ref('')
const lastGetDataTime = ref('')
const errorCode = ref('')
const errorMessage = ref('')
const valuesReturned = ref(0)
const metaRealDeviceAccessed = ref('未提供')
const metaAckReceived = ref('未提供')
const metaEmptyData = ref('未提供')
const metaValuesReturned = ref('未提供')
const metaParseError = ref('否')
const metaVisible = ref(false)
const fsu = HARDCODED_REALTIME_FSU_CODE

const pointsWithValue=computed(()=>points.value.filter(p=>{
  const rt = rtMap.value[p.pointCode]
  return rt?.valueNumber != null || rt?.valueText != null || rt?.value != null
}).length)
const diCount=computed(()=>points.value.filter(p=>p.pointType==='DI').length)
const batteryPoints=computed(()=>points.value.filter(p=>p.deviceId === '51051240700002' || /Battery/i.test(p.deviceName || '')))
const envPoints=computed(()=>points.value.filter(p=>p.deviceId !== '51051240700002' && !/Battery/i.test(p.deviceName || '')))

async function loadData(){
  loading.value=true; error.value=null; dataStatus.value='ok'; metaVisible.value=false
  try{
    const rtRes:any=await getBInterfaceRealtimePoints({fsuCode:fsu})
    const rtData=Array.isArray(rtRes)?rtRes:(rtRes?.data||rtRes?.data?.data||[])
    points.value=rtData.map((d:any)=>({
      pointCode: d.signalId || d.spid || d.pointCode || d.point_code || `${d.deviceId || d.deviceCode || 'device'}-${d.spid || 'unknown'}`,
      pointName: d.signalName || d.pointName || d.point_name || '未命名点位',
      pointType: d.signalType || d.pointType || d.point_type || d.signalCategory || '-',
      unit: d.unit || '',
      deviceId: d.deviceId || d.deviceCode || '',
      deviceName: d.deviceName || '',
    }))
    const map:Record<string,any>={}
    for(const d of rtData) {
      const key = d.signalId || d.spid || d.pointCode || d.point_code || `${d.deviceId || d.deviceCode || 'device'}-${d.spid || 'unknown'}`
      map[key]=d
    }
    rtMap.value=map
    // FE-P0-RECTIFY-002: 按协议状态分析口径 (兼容 meta 缺失)
    const meta = rtRes?.meta || rtRes?.data?.meta || {}
    lastCollectTime.value = meta?.lastCollectTime || meta?.last_collect_time || ''
    lastGetDataTime.value = meta?.lastGetDataTime || meta?.last_get_data_time || ''
    errorCode.value = meta?.errorCode || meta?.error_code || ''
    errorMessage.value = meta?.errorMessage || meta?.error_message || ''
    valuesReturned.value = (meta?.valuesReturned ?? meta?.values_returned ?? rtData.length)
    unmappedCount.value = rtData.filter((d:any)=>isTrueUnmappedPoint(d)).length
    if (meta?.parseError || meta?.parse_error) dataStatus.value = 'parse_error'
    else if (!meta?.realDeviceAccessed && meta?.realDeviceAccessed !== undefined || meta?.ackReceived === false) dataStatus.value = 'fsu_no_ack'
    else if (meta?.ackReceived && meta?.emptyData) dataStatus.value = 'fsu_ack_empty'
    else if (rtData.length === 0) dataStatus.value = 'empty'
    else if (unmappedCount.value > 0) dataStatus.value = 'unmapped'
    // FE-P0-RECTIFY-002: 设置 meta 展示值 (缺失时优雅降级)
    metaRealDeviceAccessed.value = meta?.realDeviceAccessed !== undefined ? String(meta.realDeviceAccessed) : '未提供'
    metaAckReceived.value = meta?.ackReceived !== undefined ? String(meta.ackReceived) : '未提供'
    metaEmptyData.value = meta?.emptyData !== undefined ? String(meta.emptyData) : '未提供'
    metaValuesReturned.value = meta?.valuesReturned !== undefined ? String(meta.valuesReturned) : String(rtData.length)
    metaParseError.value = (meta?.parseError || meta?.parse_error) ? '是' : '否'
    metaVisible.value = meta?.realDeviceAccessed !== undefined || meta?.ackReceived !== undefined
    try{const fsRes:any=await getBInterfaceFsus({fsuCode:fsu});fsuStatus.value=(fsRes?.data||fsRes)?.find?.((f:any)=>f.fsuCode===fsu)||null}catch{}
    lastRefresh.value=new Date().toLocaleString()
  }catch(e:any){
    // FE-P0-RECTIFY-002: 精确区分错误类型
    if (e?.response?.status === 404) {
      dataStatus.value = 'api_404'
      error.value={code:'API_NOT_FOUND',message:'接口不存在 (404)'}
    } else if (e?.response?.status === 401) {
      dataStatus.value = 'http_401'
      error.value={code:'UNAUTHORIZED',message:'未登录或登录失效 (401)'}
    } else if (e?.response?.status === 403) {
      dataStatus.value = 'http_403'
      error.value={code:'FORBIDDEN',message:'无权限访问 (403)'}
    } else if (e?.response?.status && e.response.status >= 500) {
      dataStatus.value = 'http_5xx'
      error.value={code:'SERVER_ERROR',message:`后端服务异常 (${e.response.status})`}
    } else if (e?.request && !e?.response) {
      dataStatus.value = 'network_error'
      error.value={code:'NETWORK_ERROR',message:e?.message||'网络异常或后端不可达'}
    } else {
      error.value={code:'LOAD_FAILED',message:e?.message||'加载失败'}
    }
  }
  finally{loading.value=false}
}
onMounted(()=>loadData())
</script>
<style scoped>.group{margin-bottom:16px}</style>
