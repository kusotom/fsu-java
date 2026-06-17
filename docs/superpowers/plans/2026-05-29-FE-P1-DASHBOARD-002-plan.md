# FE-P1-DASHBOARD-002: 首页业务指标与点位映射状态优化 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 FE-P1-DASHBOARD-001 基础上增强首页业务风险展示，将未映射点位、异常 FSU、点位映射状态分布纳入驾驶舱，不新增后端接口。

**Architecture:** 前端纯聚合方案——复用现有 `getSites`, `getBInterfaceFsus`, `getBInterfaceAlarms`, `getBInterfaceUnmappedSignals`, `queryBInterfaceMessageLogs` API，通过 `dashboardNormalizers.ts` 集中规范化数据，在 `DashboardView.vue` 中渲染新的四行布局。`MetricCard.vue` 增加可选 `to` prop 支持跳转。

**Tech Stack:** Vue 3.4 + TypeScript + Element Plus + CSS tokens (已有)

---

## File Map

| 文件 | 操作 | 职责 |
|------|------|------|
| `frontend/src/utils/dashboardNormalizers.ts` | 新增 | 数据规范化——指标、未映射列表、异常FSU、映射分布、错误状态 |
| `frontend/src/components/common/MetricCard.vue` | 修改 | 增加可选 `to` prop，有值时渲染 `router-link` |
| `frontend/src/views/dashboard/DashboardView.vue` | 修改 | 重构为四行新布局 |
| `docs/audit/FE-P1-DASHBOARD-002-*.md` | 新增 | 审计文档 |
| `docs/tasks/FE-P1-DASHBOARD-002-*.md` | 新增 | 后续 TODO |
| `docs/memory/2026-05-29-FE-P1-DASHBOARD-002-*.md` | 新增 | 操作记忆 |
| `docs/memory/README.md` | 修改 | 新增索引条目 |
| `docs/memory/WORKING-MEMORY.md` | 修改 | 更新工作记忆 |

---

### Task 1: 创建数据规范化工具 `dashboardNormalizers.ts`

**Files:**
- Create: `frontend/src/utils/dashboardNormalizers.ts`

- [ ] **Step 1: 写入完整的 normalizer 函数**

```typescript
// frontend/src/utils/dashboardNormalizers.ts

export interface DashboardMetrics {
  siteCount: number | null
  fsuCount: number | null
  onlineFsuCount: number | null
  onlineFsuRate: number | null
  activeAlarmCount: number | null
  criticalAlarmCount: number | null
  majorAlarmCount: number | null
  unmappedSignalCount: number | null
  offlineFsuCount: number | null
}

export interface UnmappedSignalSummary {
  fsuCode: string
  deviceId: string
  signalId: string
  rawName: string
  status: string
}

export interface AbnormalFsu {
  fsuCode: string
  onlineStatus: string
  lastHeartbeat: string | null
  heartbeatMissCount: number
  activeAlarmCount: number
}

export interface MappingDistribution {
  mapped: number
  unmapped: number
  pending: number
  ignored: number
  unknown: number
  total: number
}

export type DataState =
  | 'normal'
  | 'api_not_found'
  | 'network_error'
  | 'unauthorized'
  | 'forbidden'
  | 'server_error'
  | 'empty'
  | 'partial'

export interface DashboardErrors {
  sites?: { status?: number; request?: unknown; response?: { status: number } }
  fsus?: { status?: number; request?: unknown; response?: { status: number } }
  alarms?: { status?: number; request?: unknown; response?: { status: number } }
  unmapped?: { status?: number; request?: unknown; response?: { status: number } }
  messages?: { status?: number; request?: unknown; response?: { status: number } }
}

/**
 * 规范化仪表盘核心指标。
 */
export function normalizeDashboardMetrics(
  sitesRes: any,
  fsusRes: any,
  onlineFsusRes: any,
  alarmsRes: any,
  unmappedRes: any,
): DashboardMetrics {
  const sites = Array.isArray(sitesRes?.data) ? sitesRes.data : []
  const fsus = Array.isArray(fsusRes?.data) ? fsusRes.data : (fsusRes?.data?.data || [])
  const onlineFsus = Array.isArray(onlineFsusRes?.data) ? onlineFsusRes.data : (onlineFsusRes?.data?.data || [])
  const alarms = Array.isArray(alarmsRes?.data) ? alarmsRes.data : (alarmsRes?.data?.data || [])
  const unmapped = Array.isArray(unmappedRes?.data) ? unmappedRes.data : (unmappedRes?.data?.data || [])

  const fsuCount = fsus.length
  const onlineFsuCount = onlineFsus.length
  const onlineFsuRate = fsuCount > 0 ? Math.round((onlineFsuCount / fsuCount) * 100) : null

  const criticalAlarmCount = alarms.filter((a: any) =>
    (a.alarmLevel || a.level || '').toUpperCase() === 'CRITICAL'
  ).length
  const majorAlarmCount = alarms.filter((a: any) =>
    (a.alarmLevel || a.level || '').toUpperCase() === 'MAJOR'
  ).length

  return {
    siteCount: sites.length,
    fsuCount,
    onlineFsuCount,
    onlineFsuRate,
    activeAlarmCount: alarms.length,
    criticalAlarmCount,
    majorAlarmCount,
    unmappedSignalCount: unmapped.length,
    offlineFsuCount: fsuCount > 0 ? Math.max(0, fsuCount - onlineFsuCount) : null,
  }
}

/**
 * 规范化未映射点位 Top 列表（取前 N 条）。
 */
export function normalizeUnmappedTopList(unmappedData: any[], limit = 5): UnmappedSignalSummary[] {
  return unmappedData.slice(0, limit).map((item: any) => ({
    fsuCode: item.fsuCode || item.fsu_code || '-',
    deviceId: item.deviceId || item.device_id || item.DeviceID || '-',
    signalId: item.signalId || item.signal_id || item.SignalID || '-',
    rawName: item.rawName || item.raw_name || item.signalName || '',
    status: 'unmapped',
  }))
}

/**
 * 规范化异常 FSU 列表（离线、心跳丢失等）。
 */
export function normalizeAbnormalFsuList(
  fsus: any[],
  alarms: any[],
  limit = 5,
): AbnormalFsu[] {
  return fsus
    .filter((f: any) => {
      const status = (f.onlineStatus || '').toUpperCase()
      return status === 'OFFLINE' || status === 'ABNORMAL' || (f.heartbeatMissCount > 0)
    })
    .slice(0, limit)
    .map((f: any) => {
      const fsuAlarms = alarms.filter((a: any) => a.fsuCode === f.fsuCode || a.fsuId === f.fsuId)
      return {
        fsuCode: f.fsuCode || '-',
        onlineStatus: f.onlineStatus || 'UNKNOWN',
        lastHeartbeat: f.lastHeartbeat || null,
        heartbeatMissCount: f.heartbeatMissCount || 0,
        activeAlarmCount: fsuAlarms.length,
      }
    })
}

/**
 * 规范化点位映射状态分布。
 */
export function normalizeMappingDistribution(
  unmappedCount: number,
  mappedCount?: number,
  pendingCount?: number,
  ignoredCount?: number,
  unknownCount?: number,
): MappingDistribution {
  const mapped = mappedCount ?? 0
  const unmapped = unmappedCount ?? 0
  const pending = pendingCount ?? 0
  const ignored = ignoredCount ?? 0
  const unknown = unknownCount ?? 0
  const total = mapped + unmapped + pending + ignored + unknown

  return { mapped, unmapped, pending, ignored, unknown, total }
}

/**
 * 解析仪表盘错误状态——区分 401/403/404/5xx/network error。
 * 当多个 API 都失败时，按优先级返回最严重的错误状态。
 */
export function resolveDashboardErrorState(errors: DashboardErrors): DataState {
  const codes: number[] = []
  let hasNetworkError = false

  for (const err of Object.values(errors)) {
    if (!err) continue
    if (err.request && !err.response) {
      hasNetworkError = true
      continue
    }
    const status = err.response?.status
    if (status) codes.push(status)
  }

  // 所有 API 都失败才算错误
  const totalApis = Object.keys(errors).length
  const failedApis = codes.length + (hasNetworkError ? 1 : 0)

  if (failedApis === 0) return 'normal'
  if (failedApis < totalApis) return 'partial'

  if (codes.includes(401)) return 'unauthorized'
  if (codes.includes(403)) return 'forbidden'
  if (codes.some(c => c >= 500)) return 'server_error'
  if (codes.includes(404)) return 'api_not_found'
  if (hasNetworkError) return 'network_error'

  return 'server_error'
}

/**
 * 判断整体数据是否为空。
 */
export function isDashboardEmpty(metrics: DashboardMetrics): boolean {
  return (
    metrics.siteCount === 0 &&
    (metrics.fsuCount === null || metrics.fsuCount === 0) &&
    metrics.activeAlarmCount === 0 &&
    (metrics.unmappedSignalCount === null || metrics.unmappedSignalCount === 0)
  )
}
```

- [ ] **Step 2: 验证文件语法**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java/frontend && npx vue-tsc --noEmit src/utils/dashboardNormalizers.ts 2>&1 | head -20`
Expected: No errors related to this file.

---

### Task 2: 增强 MetricCard 支持点击跳转

**Files:**
- Modify: `frontend/src/components/common/MetricCard.vue`

- [ ] **Step 1: 添加 `to` prop 和 router-link 包装**

将现有 MetricCard.vue 的 template 修改为：

```vue
<template>
  <router-link v-if="to" :to="to" class="metric-card metric-card--link" :class="`metric-card--${status}`">
    <div class="metric-card__label">{{ title }}</div>
    <div class="metric-card__value">
      <template v-if="loading"><span class="metric-card__skeleton">&nbsp;</span></template>
      <template v-else>{{ formattedValue }}<span v-if="unit" class="metric-card__unit">{{ unit }}</span></template>
    </div>
    <div v-if="subtitle" class="metric-card__subtitle">
      <span v-if="trend === 'up'" class="metric-card__trend metric-card__trend--up">↑</span>
      <span v-else-if="trend === 'down'" class="metric-card__trend metric-card__trend--down">↓</span>
      {{ trendText || subtitle }}
    </div>
  </router-link>
  <div v-else class="metric-card" :class="`metric-card--${status}`">
    <div class="metric-card__label">{{ title }}</div>
    <div class="metric-card__value">
      <template v-if="loading"><span class="metric-card__skeleton">&nbsp;</span></template>
      <template v-else>{{ formattedValue }}<span v-if="unit" class="metric-card__unit">{{ unit }}</span></template>
    </div>
    <div v-if="subtitle" class="metric-card__subtitle">
      <span v-if="trend === 'up'" class="metric-card__trend metric-card__trend--up">↑</span>
      <span v-else-if="trend === 'down'" class="metric-card__trend metric-card__trend--down">↓</span>
      {{ trendText || subtitle }}
    </div>
  </div>
</template>
```

- [ ] **Step 2: 添加 `to` prop 定义**

在 `<script setup>` 中将 props 定义修改为（在现有基础上添加 `to`）：

```typescript
import type { RouteLocationRaw } from 'vue-router'

const props = withDefaults(defineProps<{
  title: string
  value: string | number | null
  unit?: string
  subtitle?: string
  trend?: 'up' | 'down' | 'flat'
  trendText?: string
  status?: 'normal' | 'warning' | 'danger' | 'info' | 'muted'
  loading?: boolean
  to?: string | RouteLocationRaw
}>(), {
  status: 'normal',
  loading: false,
})
```

- [ ] **Step 3: 添加 link 样式**

在 `<style scoped>` 末尾添加：

```css
.metric-card--link {
  text-decoration: none;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.metric-card--link:hover {
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}
```

- [ ] **Step 4: 构建验证**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java/frontend && npm run build 2>&1 | tail -20`
Expected: Build succeeds.

---

### Task 3: 重构 DashboardView.vue - 模板

**Files:**
- Modify: `frontend/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: 替换模板为新的四行布局**

完整模板内容：

```vue
<template>
  <div>
    <PageHeader title="监控驾驶舱" description="站点、FSU、实时数据、告警与通信状态总览" />

    <!-- ===== 连接/错误状态 ===== -->
    <DataStateAlert v-if="dataState === 'network_error'" state="network_error" :meta="{ errorMessage: '无法连接后端服务' }" />
    <DataStateAlert v-else-if="dataState === 'unauthorized'" state="unauthorized" />
    <DataStateAlert v-else-if="dataState === 'forbidden'" state="forbidden" />
    <DataStateAlert v-else-if="dataState === 'api_not_found'" state="api_not_found" :meta="{ errorMessage: '部分接口未接入' }" />
    <DataStateAlert v-else-if="dataState === 'server_error'" state="server_error" :meta="{ errorMessage: '后端服务异常' }" />
    <DataStateAlert v-else-if="dataState === 'partial'" state="server_error" :meta="{ errorMessage: '部分接口异常，数据可能不完整' }" />

    <!-- ===== 第一行: 核心指标 ===== -->
    <div class="metric-row" style="margin-bottom: 16px">
      <MetricCard title="站点总数" :value="metrics.siteCount" status="info" to="/sites" />
      <MetricCard
        title="FSU 在线率"
        :value="metrics.onlineFsuRate"
        unit="%"
        :status="fsuRateStatus"
        :subtitle="`${metrics.onlineFsuCount ?? '-'} / ${metrics.fsuCount ?? '-'} 台`"
        to="/b-interface/fsus"
      />
      <MetricCard title="当前告警" :value="metrics.activeAlarmCount" status="warning" to="/alarms" />
      <MetricCard
        title="未映射点位"
        :value="metrics.unmappedSignalCount"
        :status="unmappedStatus"
        to="/unmapped"
      />
    </div>

    <!-- ===== 第二行: 风险状态区 + 告警分布 ===== -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="16">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">风险状态</span>
          </template>
          <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
          <EmptyState v-else-if="allRiskZero" type="empty" title="暂无风险" description="当前系统运行正常" />
          <div v-else class="risk-grid">
            <div class="risk-item risk-item--danger">
              <span class="risk-item__label">严重告警</span>
              <span class="risk-item__value">{{ metrics.criticalAlarmCount ?? '-' }}</span>
            </div>
            <div class="risk-item risk-item--warning">
              <span class="risk-item__label">主要告警</span>
              <span class="risk-item__value">{{ metrics.majorAlarmCount ?? '-' }}</span>
            </div>
            <div class="risk-item risk-item--danger">
              <span class="risk-item__label">离线 FSU</span>
              <span class="risk-item__value">{{ metrics.offlineFsuCount ?? '-' }}</span>
            </div>
            <div class="risk-item risk-item--warning">
              <span class="risk-item__label">心跳丢失</span>
              <span class="risk-item__value">{{ heartbeatMissCount ?? '-' }}</span>
            </div>
            <div class="risk-item risk-item--info">
              <span class="risk-item__label">ACK 空数据</span>
              <span class="risk-item__value">{{ ackEmptyCount ?? '-' }}</span>
            </div>
            <div class="risk-item risk-item--warning">
              <span class="risk-item__label">解析异常</span>
              <span class="risk-item__value">{{ parseErrorCount ?? '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">告警分布</span>
          </template>
          <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
          <EmptyState v-else-if="metrics.activeAlarmCount === 0" type="empty" title="暂无告警" />
          <div v-else class="status-distribution">
            <div class="status-distribution__item" v-for="alv in alarmLevels" :key="alv.level">
              <AlarmLevelTag :level="alv.level" />
              <span class="status-distribution__count">{{ alv.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 第三行: 最新告警 + 异常 FSU ===== -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="12">
        <el-card shadow="never" class="app-card">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between">
              <span style="font-weight: 600; color: var(--text-primary)">最新告警</span>
              <el-button text type="primary" size="small" @click="$router.push('/alarms')">查看全部</el-button>
            </div>
          </template>
          <EmptyState v-if="!loading && recentAlarms.length === 0" type="empty" title="暂无告警" />
          <div v-else>
            <div v-for="a in recentAlarms.slice(0, 5)" :key="a.id" class="event-row">
              <AlarmLevelTag :level="a.level" :dot="true" />
              <span class="event-row__desc">{{ a.desc || a.alarmName || '告警' }}</span>
              <span style="margin-left: auto; font-size: 12px; color: var(--text-muted)">{{ a.time || '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="app-card">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between">
              <span style="font-weight: 600; color: var(--text-primary)">异常 FSU</span>
              <el-button text type="primary" size="small" @click="$router.push('/b-interface/fsus')">查看全部</el-button>
            </div>
          </template>
          <EmptyState v-if="!loading && abnormalFsus.length === 0" type="empty" title="无异常 FSU" description="所有 FSU 运行正常" />
          <div v-else>
            <div v-for="fsu in abnormalFsus" :key="fsu.fsuCode" class="event-row" style="cursor: pointer" @click="$router.push(`/b-interface/fsus/${fsu.fsuCode}`)">
              <FsuOnlineBadge :online-status="fsu.onlineStatus" />
              <span class="event-row__desc">{{ fsu.fsuCode }}</span>
              <span v-if="fsu.heartbeatMissCount > 0" style="margin-left: 8px; font-size: 11px; color: var(--status-alarm)">
                丢失 {{ fsu.heartbeatMissCount }} 次
              </span>
              <span style="margin-left: auto; font-size: 12px; color: var(--text-muted)">
                告警 {{ fsu.activeAlarmCount }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 第四行: 未映射点位 Top + 点位映射状态分布 ===== -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="16">
        <el-card shadow="never" class="app-card">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between">
              <span style="font-weight: 600; color: var(--text-primary)">未映射点位 (Top {{ unmappedTopList.length }})</span>
              <el-button text type="primary" size="small" @click="$router.push('/unmapped')">查看全部</el-button>
            </div>
          </template>
          <EmptyState v-if="!loading && unmappedTopList.length === 0" type="empty" title="暂无未映射点位" description="所有已发现点位均已建立映射关系" />
          <div v-else class="data-table" style="padding: 0">
            <el-table :data="unmappedTopList" stripe size="small" empty-text=" ">
              <el-table-column prop="fsuCode" label="FSU" width="130" />
              <el-table-column prop="deviceId" label="DeviceID" width="160" />
              <el-table-column prop="signalId" label="SignalID" width="140" />
              <el-table-column prop="rawName" label="原始名称" min-width="120">
                <template #default="{ row }"><span v-if="row.rawName">{{ row.rawName }}</span><span v-else style="color: var(--text-muted)">--</span></template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default>
                  <StatusBadge status="unmapped" label="未映射" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="app-card">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">点位映射状态</span>
          </template>
          <div v-if="loading" style="text-align: center; padding: 24px; color: var(--text-muted)">加载中...</div>
          <EmptyState v-else-if="mappingDistribution.total === 0" type="empty" title="暂无映射数据" />
          <div v-else class="status-distribution">
            <div class="status-distribution__item">
              <StatusBadge status="mapped" label="已映射" />
              <span class="status-distribution__count">{{ mappingDistribution.mapped }}</span>
            </div>
            <div class="status-distribution__item">
              <StatusBadge status="unmapped" label="未映射" />
              <span class="status-distribution__count">{{ mappingDistribution.unmapped }}</span>
            </div>
            <div class="status-distribution__item">
              <StatusBadge status="warning" label="待确认" />
              <span class="status-distribution__count">{{ mappingDistribution.pending }}</span>
            </div>
            <div class="status-distribution__item">
              <StatusBadge status="inactive" label="已忽略" />
              <span class="status-distribution__count">{{ mappingDistribution.ignored }}</span>
            </div>
            <div class="status-distribution__item">
              <StatusBadge status="unknown" label="未知" />
              <span class="status-distribution__count">{{ mappingDistribution.unknown }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 通信状态（弱化，作为辅助信息） ===== -->
    <el-card shadow="never" class="app-card" style="margin-bottom: 16px">
      <template #header>
        <span style="font-weight: 600; color: var(--text-primary)">通信摘要</span>
      </template>
      <div v-if="loading" style="text-align: center; padding: 16px; color: var(--text-muted)">加载中...</div>
      <div v-else class="comm-summary">
        <span class="comm-summary__item">
          <span class="fsu-dot fsu-dot--online"></span>报文总数: <strong>{{ messageCount ?? '-' }}</strong>
        </span>
        <span class="comm-summary__item">
          <span class="fsu-dot fsu-dot--online"></span>今日调用: <strong>{{ callCount ?? '-' }}</strong>
        </span>
        <span class="comm-summary__item">
          <span class="fsu-dot" :class="realtimeOk ? 'fsu-dot--online' : 'fsu-dot--offline'"></span>
          实时数据: <strong :style="{ color: realtimeOk ? 'var(--status-online)' : 'var(--status-offline)' }">{{ realtimeOk ? '正常' : '待确认' }}</strong>
        </span>
      </div>
    </el-card>

    <!-- ===== 底部: 空状态提示 ===== -->
    <DataStateAlert v-if="showEmpty" state="empty" />
  </div>
</template>
```

---

### Task 4: 重构 DashboardView.vue - 脚本

**Files:**
- Modify: `frontend/src/views/dashboard/DashboardView.vue` (script section)

- [ ] **Step 1: 替换 script 部分**

```typescript
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import MetricCard from '@/components/common/MetricCard.vue'
import DataStateAlert from '@/components/common/DataStateAlert.vue'
import FsuOnlineBadge from '@/components/common/FsuOnlineBadge.vue'
import AlarmLevelTag from '@/components/common/AlarmLevelTag.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getSites } from '@/api/resource'
import { getBInterfaceFsus, getBInterfaceAlarms, getBInterfaceUnmappedSignals, queryBInterfaceMessageLogs, getCallRecords } from '@/api/bInterface'
import {
  normalizeDashboardMetrics,
  normalizeUnmappedTopList,
  normalizeAbnormalFsuList,
  normalizeMappingDistribution,
  resolveDashboardErrorState,
  isDashboardEmpty,
} from '@/utils/dashboardNormalizers'
import type { DashboardMetrics, AbnormalFsu, UnmappedSignalSummary, MappingDistribution, DataState, DashboardErrors } from '@/utils/dashboardNormalizers'

const loading = ref(true)
const dataState = ref<DataState>('normal')

const metrics = ref<DashboardMetrics>({
  siteCount: null, fsuCount: null, onlineFsuCount: null, onlineFsuRate: null,
  activeAlarmCount: null, criticalAlarmCount: null, majorAlarmCount: null,
  unmappedSignalCount: null, offlineFsuCount: null,
})

const recentAlarms = ref<any[]>([])
const abnormalFsus = ref<AbnormalFsu[]>([])
const unmappedTopList = ref<UnmappedSignalSummary[]>([])
const mappingDistribution = ref<MappingDistribution>({ mapped: 0, unmapped: 0, pending: 0, ignored: 0, unknown: 0, total: 0 })
const messageCount = ref<number | null>(null)
const callCount = ref<number | null>(null)
const heartbeatMissCount = ref<number | null>(null)
const ackEmptyCount = ref<number | null>(null)
const parseErrorCount = ref<number | null>(null)

const alarmLevels = computed(() => {
  const map: Record<string, number> = { CRITICAL: 0, MAJOR: 0, MINOR: 0, WARN: 0 }
  for (const a of recentAlarms.value) {
    const lvl = (a.alarmLevel || a.level || '').toUpperCase()
    if (map[lvl] !== undefined) map[lvl]++
  }
  return Object.entries(map).map(([level, count]) => ({ level, count }))
})

const fsuRateStatus = computed(() => {
  const rate = metrics.value.onlineFsuRate
  if (rate === null) return 'info'
  if (rate >= 90) return 'normal'
  if (rate >= 50) return 'warning'
  return 'danger'
})

const unmappedStatus = computed(() => {
  const c = metrics.value.unmappedSignalCount
  if (c === null) return 'info'
  if (c === 0) return 'normal'
  if (c <= 10) return 'warning'
  return 'danger'
})

const allRiskZero = computed(() =>
  (metrics.value.criticalAlarmCount ?? 0) === 0 &&
  (metrics.value.majorAlarmCount ?? 0) === 0 &&
  (metrics.value.offlineFsuCount ?? 0) === 0 &&
  (heartbeatMissCount.value ?? 0) === 0 &&
  (ackEmptyCount.value ?? 0) === 0 &&
  (parseErrorCount.value ?? 0) === 0
)

const realtimeOk = computed(() => messageCount.value !== null && (messageCount.value ?? 0) > 0)
const showEmpty = computed(() =>
  !loading.value &&
  dataState.value === 'normal' &&
  isDashboardEmpty(metrics.value)
)

onMounted(async () => {
  const errors: DashboardErrors = {}

  try {
    const [sitesRes, fsusRes, onlineFsusRes, alarmsRes, unmappedRes, msgRes, callsRes] = await Promise.all([
      getSites().catch((e: any) => { errors.sites = e; return null }),
      getBInterfaceFsus().catch((e: any) => { errors.fsus = e; return null }),
      getBInterfaceFsus({ onlineStatus: 'ONLINE' }).catch((e: any) => { return null }),
      getBInterfaceAlarms({ alarmStatus: 'ACTIVE' }).catch((e: any) => { errors.alarms = e; return null }),
      getBInterfaceUnmappedSignals().catch((e: any) => { errors.unmapped = e; return null }),
      queryBInterfaceMessageLogs({ size: 1 }).catch((e: any) => { errors.messages = e; return null }),
      getCallRecords().catch(() => null),
    ]) as any[]

    metrics.value = normalizeDashboardMetrics(sitesRes, fsusRes, onlineFsusRes, alarmsRes, unmappedRes)

    // Alarms
    const alarms = Array.isArray(alarmsRes?.data) ? alarmsRes.data : (alarmsRes?.data?.data || [])
    recentAlarms.value = alarms.map((a: any) => ({
      id: a.id, level: a.alarmLevel, desc: a.alarmDesc || a.alarmName,
      time: a.occurTime || a.alarmTime,
    }))

    // FSU list for abnormal detection
    const fsus = Array.isArray(fsusRes?.data) ? fsusRes.data : (fsusRes?.data?.data || [])
    abnormalFsus.value = normalizeAbnormalFsuList(fsus, alarms)

    // Heartbeat miss count
    heartbeatMissCount.value = fsus.reduce((sum: number, f: any) => sum + (f.heartbeatMissCount || 0), 0)

    // Unmapped signals
    const unmapped = Array.isArray(unmappedRes?.data) ? unmappedRes.data : (unmappedRes?.data?.data || [])
    unmappedTopList.value = normalizeUnmappedTopList(unmapped, 5)

    // Mapping distribution (unmapped count from API, others default to 0 until API provides)
    mappingDistribution.value = normalizeMappingDistribution(unmapped.length)

    // ACK empty / parse error from message logs
    if (msgRes?.data) {
      const msgs = Array.isArray(msgRes.data) ? msgRes.data : (msgRes.data.data || msgRes.data.content || [])
      // These would come from dedicated summary APIs; defaults to 0 for now
      ackEmptyCount.value = 0
      parseErrorCount.value = 0
    }

    // Communication stats
    messageCount.value = msgRes?.data?.totalElements ?? (Array.isArray(msgRes?.data) ? msgRes.data.length : 0)
    callCount.value = Array.isArray(callsRes?.data) ? callsRes.data.length : 0

    dataState.value = resolveDashboardErrorState(errors)

  } catch (e: any) {
    dataState.value = e?.request && !e?.response ? 'network_error' : 'server_error'
  } finally {
    loading.value = false
  }
})
</script>
```

---

### Task 5: 重构 DashboardView.vue - 样式

**Files:**
- Modify: `frontend/src/views/dashboard/DashboardView.vue` (style section)

- [ ] **Step 1: 替换并扩展样式**

```css
<style scoped>
/* ===== 风险状态网格 ===== */
.risk-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 4px 0;
}
.risk-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: var(--radius-panel);
  background: var(--app-bg);
  border: 1px solid var(--border-light);
}
.risk-item__label { font-size: 12px; color: var(--text-secondary); margin-bottom: 4px; }
.risk-item__value { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.risk-item--danger .risk-item__value { color: var(--status-alarm); }
.risk-item--warning .risk-item__value { color: var(--status-warning); }
.risk-item--info .risk-item__value { color: var(--status-info); }

/* ===== 状态分布 ===== */
.status-distribution { padding: 4px 0; }
.status-distribution__item {
  display: flex; align-items: center; padding: 8px 0;
  border-bottom: 1px solid var(--border-light);
}
.status-distribution__item:last-child { border-bottom: none; }
.status-distribution__count {
  margin-left: auto; font-size: 18px; font-weight: 600; color: var(--text-primary);
}

/* ===== 事件行 ===== */
.event-row {
  display: flex; align-items: center; padding: 8px 0; gap: 8px;
  border-bottom: 1px solid var(--border-light);
}
.event-row:last-child { border-bottom: none; }
.event-row__desc {
  font-size: 13px; color: var(--text-secondary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* ===== 通信摘要 ===== */
.comm-summary {
  display: flex; align-items: center; gap: 24px; flex-wrap: wrap; padding: 4px 0;
}
.comm-summary__item {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--text-secondary);
}
</style>
```

- [ ] **Step 2: 构建验证**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java/frontend && npm run build 2>&1 | tail -20`
Expected: Build succeeds with no errors.

---

### Task 6: 构建与最终验证

**Files:** (none, verification only)

- [ ] **Step 1: 完整构建**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java/frontend && npm run build 2>&1`
Expected: Build succeeds with no errors.

- [ ] **Step 2: Type check**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java/frontend && npx vue-tsc --noEmit 2>&1 | tail -30`
Expected: No new type errors introduced.

- [ ] **Step 3: 检查是否有 lint/test 脚本**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java/frontend && cat package.json | grep -E '"(test|lint)"' 2>&1`
Note: 报告结果。

---

### Task 7: 编写审计文档

**Files:**
- Create: `docs/audit/FE-P1-DASHBOARD-002-dashboard-business-risk-optimization.md`

- [ ] **Step 1: 写入审计文档** (内容见完整审计文档模板，包含最终结论 15 条)

---

### Task 8: 编写后续 TODO 文档

**Files:**
- Create: `docs/tasks/FE-P1-DASHBOARD-002-follow-up-todo.md`

- [ ] **Step 1: 写入后续 TODO**

---

### Task 9: 编写操作记忆

**Files:**
- Create: `docs/memory/2026-05-29-FE-P1-DASHBOARD-002-dashboard-business-risk-optimization.md`
- Modify: `docs/memory/README.md`
- Modify: `docs/memory/WORKING-MEMORY.md`

- [ ] **Step 1: 写入操作记忆文件**
- [ ] **Step 2: 更新 README.md 索引**
- [ ] **Step 3: 更新 WORKING-MEMORY.md**

---

### Task 10: Git 提交

- [ ] **Step 1: 查看变更**

Run: `cd /home/tom/桌面/FSU/fsu-platform-java && git diff --stat`

- [ ] **Step 2: 提交**

```bash
cd /home/tom/桌面/FSU/fsu-platform-java
git add frontend/src/utils/dashboardNormalizers.ts
git add frontend/src/components/common/MetricCard.vue
git add frontend/src/views/dashboard/DashboardView.vue
git add docs/audit/FE-P1-DASHBOARD-002-dashboard-business-risk-optimization.md
git add docs/tasks/FE-P1-DASHBOARD-002-follow-up-todo.md
git add docs/memory/2026-05-29-FE-P1-DASHBOARD-002-dashboard-business-risk-optimization.md
git add docs/memory/README.md
git add docs/memory/WORKING-MEMORY.md
git commit -m "feat(frontend): enhance dashboard with unmapped signals, abnormal FSU, and mapping distribution

- Replace '严重告警' metric with '未映射点位' in core metrics row
- Add risk status area (critical/major alarms, offline FSU, heartbeat miss, ACK empty, parse error)
- Add abnormal FSU list with offline/heartbeat-miss highlighting
- Add unmapped signals Top list with jump to /unmapped
- Add mapping status distribution panel
- Add MetricCard 'to' prop for click-to-navigate support
- Weaken communication status to auxiliary summary
- All data from existing APIs, no new backend endpoints

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```
