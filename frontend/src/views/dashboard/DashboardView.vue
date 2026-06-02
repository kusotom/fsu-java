<template>
  <div class="page-template">
    <PageHeader title="监控主页" description="站点监控、FSU、告警与通信状态总览" />

    <!-- ===== 连接状态 ===== -->
    <DataStateAlert
      v-if="connectionStatus === 'not_connected'"
      state="network_error"
      :meta="{ errorMessage: '无法连接后端服务' }"
    />
    <DataStateAlert
      v-else-if="connectionStatus === 'partial'"
      state="server_error"
      :meta="{ errorMessage: '部分接口未接入，数据可能不完整' }"
    />

    <!-- ===== 第一行: 核心指标 ===== -->
    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="站点总数" :value="siteCount" status="info" />
      <MetricCard title="FSU 在线" :value="onlineFsuCount" unit="台" status="normal" :subtitle="`共 ${fsuCount} 台`" />
      <MetricCard title="当前告警" :value="activeAlarmCount" status="warning" />
      <MetricCard title="严重告警" :value="criticalAlarmCount" status="danger" />
      <MetricCard title="主要告警" :value="majorAlarmCount" status="warning" />
      <MetricCard title="数据异常" :value="dataAnomalyCount" :status="dataAnomalyCount > 0 ? 'warning' : 'normal'" />
    </div>

    <div class="dashboard-grid">
      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="dashboard-card__title">FSU 状态分布</div>
        </template>
          <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
          <EmptyState v-else-if="fsuCount === 0" type="empty" title="暂无 FSU 数据" />
          <div v-else class="status-distribution">
            <div class="status-distribution__item">
              <FsuOnlineBadge online-status="ONLINE" />
              <span class="status-distribution__count">{{ onlineFsuCount }}</span>
            </div>
            <div class="status-distribution__item">
              <FsuOnlineBadge online-status="OFFLINE" />
              <span class="status-distribution__count">{{ offlineFsuCount }}</span>
            </div>
          </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="dashboard-card__title">告警等级分布</div>
        </template>
          <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
          <EmptyState v-else-if="alarmTotal === 0" type="empty" title="暂无告警" description="当前无活跃告警" />
          <div v-else class="status-distribution">
            <div class="status-distribution__item" v-for="alv in alarmLevels" :key="alv.level">
              <AlarmLevelTag :level="alv.level" />
              <span class="status-distribution__count">{{ alv.count }}</span>
            </div>
          </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="dashboard-card__title">最新告警 <span class="compact-meta">{{ recentAlarms.length }}</span></div>
        </template>
          <EmptyState v-if="!loading && recentAlarms.length === 0" type="empty" title="暂无告警" />
          <div v-else>
            <div v-for="a in recentAlarms.slice(0, 5)" :key="a.id" class="event-row">
              <AlarmLevelTag :level="a.level" :dot="true" />
              <span class="event-row__desc">{{ a.desc || a.alarmName || '告警' }}</span>
              <span style="margin-left: auto; font-size: 12px; color: var(--text-muted)">{{ a.time || a.occurTime || '-' }}</span>
            </div>
          </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="dashboard-card__title">站点摘要</div>
        </template>
        <EmptyState v-if="!loading && siteRows.length === 0" type="empty" title="暂无站点数据" />
        <div v-else>
          <div v-for="site in siteSummary" :key="site.id || site.stationId || site.stationCode || site.name" class="summary-row">
            <span class="summary-row__label">{{ site.stationName || site.siteName || site.name || site.stationCode || '未命名站点' }}</span>
            <span class="compact-meta">{{ site.fsuCode || site.fsuId || 'FSU 待绑定' }}</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="dashboard-card__title">异常 FSU 摘要</div>
        </template>
        <EmptyState v-if="!loading && abnormalFsus.length === 0" type="empty" title="暂无异常 FSU" />
        <div v-else>
          <div v-for="fsu in abnormalFsus.slice(0, 5)" :key="fsu.fsuCode || fsu.id" class="summary-row">
            <FsuOnlineBadge :online-status="fsu.onlineStatus" />
            <span class="summary-row__label">{{ fsu.fsuCode || '-' }}</span>
            <span class="compact-meta">{{ fsu.lastHeartbeat || '心跳待确认' }}</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="dashboard-card__title">数据状态摘要</div>
        </template>
        <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
        <div v-else>
          <div class="event-row">
            <span class="fsu-dot fsu-dot--online"></span>
            <span>实时测点</span>
            <span style="margin-left: auto; font-size: 12px" :style="{ color: realtimeOk ? 'var(--status-online)' : 'var(--status-offline)' }">
              {{ realtimeSignalCount }}
            </span>
          </div>
          <div class="event-row">
            <span class="fsu-dot fsu-dot--online"></span>
            <span>FSU / 点位</span>
            <span class="summary-row__value">{{ realtimeFsuPointCount }}</span>
          </div>
          <div class="event-row">
            <span class="fsu-dot" :class="realtimeOk ? 'fsu-dot--online' : 'fsu-dot--alarm'"></span>
            <span>实时数据状态</span>
            <span style="margin-left: auto; font-size: 12px" :style="{ color: realtimeOk ? 'var(--status-online)' : 'var(--status-offline)' }">
              {{ realtimeOk ? '正常' : '待确认' }}
            </span>
          </div>
          <div class="event-row">
            <span class="fsu-dot" :class="historicalBackfillCount > 0 ? 'fsu-dot--alarm' : 'fsu-dot--online'"></span>
            <span>历史待回填</span>
            <span class="summary-row__value">{{ historicalBackfillCount }}</span>
          </div>
          <div class="event-row">
            <span class="fsu-dot" :class="trueUnmappedCount > 0 ? 'fsu-dot--alarm' : 'fsu-dot--online'"></span>
            <span>待映射</span>
            <span class="summary-row__value">{{ trueUnmappedCount }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- ===== 底部: 数据状态提示 ===== -->
    <DataStateAlert
      v-if="dataEmpty"
      state="empty"
      :meta="{ unmappedCount: 0 }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FsuOnlineBadge from '@/components/common/FsuOnlineBadge.vue'
import AlarmLevelTag from '@/components/common/AlarmLevelTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getSites } from '@/api/resource'
import { getRealtimeData } from '@/api/telemetry'
import { getBInterfaceFsus, getBInterfaceAlarms } from '@/api/bInterface'
import { normalizeAlarmLevel } from '@/utils/alarmDisplay'
import {
  extractApiRows,
  normalizeBusinessAlarms,
  normalizeRealtimePoints,
  summarizeAlarms,
  summarizeRealtimePoints,
} from '@/utils/monitorAdapters'

const loading = ref(true)
const connectionStatus = ref<'connected' | 'not_connected' | 'partial'>('connected')
const siteCount = ref<number | null>(null)
const fsuCount = ref<number | null>(null)
const onlineFsuCount = ref<number | null>(null)
const activeAlarmCount = ref<number | null>(null)
const realtimeFsuPointCount = ref(0)
const realtimeSignalCount = ref(0)
const historicalBackfillCount = ref(0)
const trueUnmappedCount = ref(0)
const dataAnomalyCount = ref(0)
const siteRows = ref<any[]>([])
const fsuRows = ref<any[]>([])

const offlineFsuCount = computed(() => {
  if (fsuCount.value === null || onlineFsuCount.value === null) return null
  return Math.max(0, fsuCount.value - onlineFsuCount.value)
})

const recentAlarms = ref<any[]>([])
const alarmTotal = ref(0)

const alarmLevels = computed(() => {
  const map: Record<string, number> = { CRITICAL: 0, MAJOR: 0, MINOR: 0, WARN: 0, INFO: 0 }
  for (const a of recentAlarms.value) {
    const lvl = normalizeAlarmLevel(a.alarmLevel || a.level, a.eventSeverity).level
    if (map[lvl] !== undefined) map[lvl]++
  }
  return Object.entries(map).map(([level, count]) => ({ level, count }))
})

const criticalAlarmCount = computed(() => {
  return alarmLevels.value.find(a => a.level === 'CRITICAL')?.count || 0
})
const majorAlarmCount = computed(() => {
  return alarmLevels.value.find(a => a.level === 'MAJOR')?.count || 0
})

const dataEmpty = computed(() => !loading.value && activeAlarmCount.value === 0 && fsuCount.value === 0)
const realtimeOk = computed(() => realtimeSignalCount.value > 0)
const siteSummary = computed(() => siteRows.value.slice(0, 5))
const abnormalFsus = computed(() => fsuRows.value.filter(f => {
  const status = (f.onlineStatus || '').toUpperCase()
  return status === 'OFFLINE' || status === 'ABNORMAL' || (f.heartbeatMissCount || 0) > 3
}))

onMounted(async () => {
  try {
    const [sitesRes, fsusRes, onlineFsusRes, alarmsRes, realtimeRes] = await Promise.all([
      getSites(),
      getBInterfaceFsus(),
      getBInterfaceFsus({ onlineStatus: 'ONLINE' }),
      getBInterfaceAlarms({ alarmStatus: 'ACTIVE' }),
      getRealtimeData()
    ]) as any[]

    siteRows.value = Array.isArray(sitesRes?.data) ? sitesRes.data : (sitesRes?.data?.data || [])
    fsuRows.value = Array.isArray(fsusRes?.data) ? fsusRes.data : (fsusRes?.data?.data || [])
    siteCount.value = siteRows.value.length
    fsuCount.value = fsuRows.value.length
    onlineFsuCount.value = Array.isArray(onlineFsusRes?.data) ? onlineFsusRes.data.length : 0

    const alarms = normalizeBusinessAlarms(extractApiRows(alarmsRes))
    const alarmSummary = summarizeAlarms(alarms)
    activeAlarmCount.value = alarmSummary.active
    alarmTotal.value = alarmSummary.total
    recentAlarms.value = alarms.map((row: any) => {
      return {
        ...row,
        id: row.id,
        level: row.displayAlarmLevel,
        desc: row.displayAlarmName || row.displayAlarmMeaning,
        time: row.displayOccurTime,
      }
    })

    const realtimeSummary = summarizeRealtimePoints(normalizeRealtimePoints(extractApiRows(realtimeRes)))
    realtimeFsuPointCount.value = realtimeSummary.fsuPointCount
    realtimeSignalCount.value = realtimeSummary.realtimeSignalCount
    historicalBackfillCount.value = realtimeSummary.legacy
    trueUnmappedCount.value = realtimeSummary.trueUnmapped
    dataAnomalyCount.value = realtimeSummary.anomaly
  } catch (e: any) {
    connectionStatus.value = e?.request && !e?.response ? 'not_connected' : 'partial'
  } finally {
    loading.value = false
  }
})
</script>
