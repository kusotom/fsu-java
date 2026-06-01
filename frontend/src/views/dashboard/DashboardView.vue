<template>
  <div>
    <PageHeader title="监控驾驶舱" description="站点监控、FSU、告警与通信状态总览" />

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
    </div>

    <!-- ===== 第二行: 状态分布 ===== -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="12">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">FSU 状态分布</span>
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
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">告警分布</span>
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
      </el-col>
    </el-row>

    <!-- ===== 第三行: 最新事件 ===== -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="12">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">最新告警 ({{ recentAlarms.length }})</span>
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
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">通信状态</span>
          </template>
          <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
          <div v-else>
            <div class="event-row">
              <span class="fsu-dot fsu-dot--online"></span>
              <span>B接口报文总数</span>
              <span style="margin-left: auto; color: var(--text-primary); font-weight: 600">{{ messageCount }}</span>
            </div>
            <div class="event-row">
              <span class="fsu-dot fsu-dot--online"></span>
              <span>今日调用记录</span>
              <span style="margin-left: auto; color: var(--text-primary); font-weight: 600">{{ callCount }}</span>
            </div>
            <div class="event-row">
              <span class="fsu-dot" :class="realtimeOk ? 'fsu-dot--online' : 'fsu-dot--offline'"></span>
              <span>站点实时数据状态</span>
              <span style="margin-left: auto; font-size: 12px" :style="{ color: realtimeOk ? 'var(--status-online)' : 'var(--status-offline)' }">
                {{ realtimeOk ? '正常' : '待确认' }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
import { getBInterfaceFsus, getBInterfaceAlarms, queryBInterfaceMessageLogs, getCallRecords } from '@/api/bInterface'

const loading = ref(true)
const connectionStatus = ref<'connected' | 'not_connected' | 'partial'>('connected')
const siteCount = ref<number | null>(null)
const fsuCount = ref<number | null>(null)
const onlineFsuCount = ref<number | null>(null)
const activeAlarmCount = ref<number | null>(null)
const messageCount = ref<number | null>(null)
const callCount = ref<number | null>(null)

const offlineFsuCount = computed(() => {
  if (fsuCount.value === null || onlineFsuCount.value === null) return null
  return Math.max(0, fsuCount.value - onlineFsuCount.value)
})

const recentAlarms = ref<any[]>([])
const alarmTotal = ref(0)

const alarmLevels = computed(() => {
  const map: Record<string, number> = { CRITICAL: 0, MAJOR: 0, MINOR: 0, WARN: 0 }
  for (const a of recentAlarms.value) {
    const lvl = (a.alarmLevel || a.level || '').toUpperCase()
    if (map[lvl] !== undefined) map[lvl]++
  }
  return Object.entries(map).map(([level, count]) => ({ level, count }))
})

const criticalAlarmCount = computed(() => {
  return alarmLevels.value.find(a => a.level === 'CRITICAL')?.count || 0
})

const dataEmpty = computed(() => !loading.value && activeAlarmCount.value === 0 && fsuCount.value === 0)
const realtimeOk = computed(() => messageCount.value !== null && (messageCount.value ?? 0) > 0)

onMounted(async () => {
  try {
    const [sitesRes, fsusRes, onlineFsusRes, alarmsRes, msgRes, callsRes] = await Promise.all([
      getSites(),
      getBInterfaceFsus(),
      getBInterfaceFsus({ onlineStatus: 'ONLINE' }),
      getBInterfaceAlarms({ alarmStatus: 'ACTIVE' }),
      queryBInterfaceMessageLogs({ size: 1 }),
      getCallRecords()
    ]) as any[]

    siteCount.value = Array.isArray(sitesRes?.data) ? sitesRes.data.length : 0
    fsuCount.value = Array.isArray(fsusRes?.data) ? fsusRes.data.length : 0
    onlineFsuCount.value = Array.isArray(onlineFsusRes?.data) ? onlineFsusRes.data.length : 0

    const alarms = Array.isArray(alarmsRes?.data) ? alarmsRes.data : (alarmsRes?.data?.data || [])
    activeAlarmCount.value = alarms.length
    alarmTotal.value = alarms.length
    recentAlarms.value = alarms.map((a: any) => ({
      id: a.id, level: a.alarmLevel, desc: a.alarmDesc || a.alarmName,
      time: a.occurTime || a.alarmTime
    }))

    messageCount.value = msgRes?.data?.totalElements ?? (Array.isArray(msgRes?.data) ? msgRes.data.length : 0)
    callCount.value = Array.isArray(callsRes?.data) ? callsRes.data.length : 0
  } catch (e: any) {
    connectionStatus.value = e?.request && !e?.response ? 'not_connected' : 'partial'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.status-distribution { padding: 4px 0; }
.status-distribution__item {
  display: flex; align-items: center; padding: 8px 0;
  border-bottom: 1px solid var(--border-light);
}
.status-distribution__item:last-child { border-bottom: none; }
.status-distribution__count {
  margin-left: auto; font-size: 18px; font-weight: 600; color: var(--text-primary);
}
.event-row {
  display: flex; align-items: center; padding: 8px 0; gap: 8px;
  border-bottom: 1px solid var(--border-light);
}
.event-row:last-child { border-bottom: none; }
.event-row__desc {
  font-size: 13px; color: var(--text-secondary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
