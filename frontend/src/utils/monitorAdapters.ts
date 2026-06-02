import {
  isHistoricalRealtimeRow,
  isTrueUnmappedPoint,
  mappingStatusLabel,
} from './mappingStatus'
import {
  isBusinessAnomaly,
  type UnifiedDataState,
  unifiedStateBadgeStatus,
  unifiedStateLabel,
} from './monitorState'
import { normalizeAlarmRow } from './alarmDisplay'

export interface BusinessRealtimePoint {
  raw: Record<string, any>
  fsuCode: string
  fsuPointName: string
  pointType: string
  deviceKey: string
  signalKey: string
  measurementKey: string
  mappingStatus: string
  businessState: UnifiedDataState
  businessStatus: string
  businessStateLabel: string
  /** 业务传感器名称，已收敛为温度/湿度/电压等 */
  sensorName: string
  /** 技术层面：采集设备名，普通页面不展示 */
  displayDevice: string
  /** 技术层面：原始信号名，普通页面不展示 */
  displaySignalName: string
  displayValue: string
  /** FE-REALTIME-TABLE-FINAL-FIX-001: 合并当前值+单位+轻量状态 */
  displayValueWithUnit: string
  /** FE-REALTIME-VALUE-UNIT-STYLE-FIX-001: 独立单位字段，页面可弱化渲染 */
  displayUnit: string
  unitLabel: string
  valueMeaning: string
  collectTime: string
  hasValue: boolean
  isTrueUnmapped: boolean
  isLegacy: boolean
}

export interface BusinessAlarmRow extends Record<string, any> {
  businessState: UnifiedDataState
  businessStatus: string
  businessStateLabel: string
  displayDevicePoint: string
}

export interface BusinessFsuDeviceRow extends Record<string, any> {
  businessState: UnifiedDataState
  businessStatus: string
  businessStateLabel: string
  displayDevice: string
  displayLastSeenAt: string
}

export function extractApiRows(response: any): any[] {
  if (Array.isArray(response)) return response
  if (Array.isArray(response?.data)) return response.data
  if (Array.isArray(response?.data?.data)) return response.data.data
  return []
}

export function normalizeRealtimePoint(row: Record<string, any>): BusinessRealtimePoint {
  const mappingStatus = normalizeRealtimeMappingStatus(row)
  const legacy = mappingStatus === 'HISTORICAL_PENDING_BACKFILL' || isHistoricalRealtimeRow({ ...row, mappingStatus })
  const trueUnmapped = isTrueUnmappedPoint({ ...row, mappingStatus })
  const value = row.valueNumber ?? row.valueText ?? row.value ?? row.measuredVal ?? row.rawValue ?? null
  const hasValue = value !== null && value !== undefined && value !== ''
  const pointType = firstText(row.pointType, row.point_type, row.signalType, row.dataType)
  const businessState = deriveRealtimeBusinessState(row, mappingStatus, legacy, trueUnmapped, hasValue)
  const deviceName = firstText(row.deviceName, row.device_name, row.displayDeviceName)
  const signalName = firstText(row.measurementName, row.measurement_name, row.signalName, row.signal_name, row.pointName, row.point_name)
  const fsuCode = firstText(row.fsuCode, row.fsu_code)
  // FE-REALTIME-SITE-NAME-BINDING-VERIFY-FIX-002: 站点名称 > FSU名称 > FSU编码 > 站点ID > 兜底
  const fsuPointName = firstText(row.stationName, row.station_name, row.siteName, row.site_name,
    row.fsuName, row.fsu_name, row.fsuPointName)
    || fsuCode
    || (row.fsuId ? `站点 ${row.fsuId}` : null)
    || '未知站点'
  const deviceKey = firstText(row.deviceId, row.device_id, row.deviceCode, row.device_code)
  const signalKey = firstText(row.signalId, row.signal_id, row.spid, row.SPID, row.rawId, row.pointCode, row.point_code)
  const measurementKey = makeMeasurementKey(fsuCode, deviceKey, signalKey, signalName)
  const unit = firstText(row.unit) || '待确认'

  return {
    raw: row,
    fsuCode,
    fsuPointName,
    pointType,
    deviceKey,
    signalKey,
    measurementKey,
    mappingStatus,
    businessState,
    businessStatus: unifiedStateBadgeStatus(businessState),
    businessStateLabel: realtimeStateLabel(businessState, mappingStatus),
    sensorName: normalizeSensorName(row)
      || (legacy && signalKey ? `历史测点(${signalKey.substring(0, 12)})` : null)
      || (legacy ? '历史测点' : trueUnmapped ? '待映射测点' : (signalName || '待确认测点')),
    displayDevice: deviceName || (legacy ? '历史设备' : '待确认设备'),
    displaySignalName: signalName || (legacy ? '历史测点' : trueUnmapped ? '待映射测点' : '待确认测点'),
    displayValue: formatValue(value),
    displayValueWithUnit: buildDisplayValueWithUnit(value, unit, normalizeSensorName(row)),
    displayUnit: buildDisplayUnit(value, unit, normalizeSensorName(row)),
    unitLabel: unit,
    valueMeaning: firstText(row.valueMeaning),
    collectTime: formatBusinessDateTime(firstText(row.collectTime, row.collect_time, row.updateTime, row.updatedAt)),
    hasValue,
    isTrueUnmapped: trueUnmapped,
    isLegacy: legacy,
  }
}

export function normalizeRealtimePoints(rows: any[]) {
  return rows.map(row => normalizeRealtimePoint(row))
}

/**
 * FE-REALTIME-SITE-NAME-BINDING-FINAL-FIX-004: 用站点列表 + FSU 设备关联真实站点名称。
 * @param points 已归一化的实时数据点
 * @param siteNameByFsuId fsuId → siteName 映射
 */
export function enrichSiteNames(
  points: BusinessRealtimePoint[],
  siteNameByFsuId: Map<number, string>,
): BusinessRealtimePoint[] {
  if (siteNameByFsuId.size === 0) return points
  return points.map(p => {
    // 从 raw 数据中提取 fsuId
    const fsuId = p.raw.fsuId ?? p.raw.fsu_id
    if (fsuId != null) {
      const realSiteName = siteNameByFsuId.get(Number(fsuId))
      if (realSiteName) {
        return { ...p, fsuPointName: realSiteName }
      }
    }
    return p
  })
}

export function summarizeRealtimePoints(rows: BusinessRealtimePoint[]) {
  return {
    total: rows.length,
    fsuPointCount: uniqueCount(rows, row => row.fsuPointName),
    realtimeSignalCount: uniqueCount(rows, row => row.measurementKey),
    withValue: rows.filter(row => row.hasValue).length,
    trueUnmapped: rows.filter(row => row.isTrueUnmapped).length,
    legacy: rows.filter(row => row.isLegacy).length,
    offline: rows.filter(row => row.businessState === 'offline').length,
    parseError: rows.filter(row => row.businessState === 'parse_error').length,
    anomaly: rows.filter(row => row.businessState !== 'legacy' && isBusinessAnomaly(row.businessState)).length,
  }
}

function uniqueCount<T>(rows: T[], keyOf: (row: T) => string) {
  const keys = new Set<string>()
  for (const row of rows) {
    const key = keyOf(row)
    if (key) keys.add(key)
  }
  return keys.size
}

function makeMeasurementKey(fsuCode: string, deviceKey: string, signalKey: string, signalName: string) {
  const signalPart = signalKey || signalName
  if (!signalPart) return ''
  return [deviceKey || fsuCode, signalPart].filter(Boolean).join('|')
}

export function normalizeBusinessAlarm(row: Record<string, any>): BusinessAlarmRow {
  const normalized = normalizeAlarmRow(row)
  const businessState = deriveAlarmBusinessState(normalized)
  const deviceName = firstText(normalized.deviceName, normalized.device_name)
  const pointName = firstText(normalized.signalName, normalized.eventName, normalized.pointName, normalized.alarmName)
  return {
    ...normalized,
    businessState,
    businessStatus: unifiedStateBadgeStatus(businessState),
    businessStateLabel: unifiedStateLabel(businessState),
    displayDevicePoint: [
      deviceName || '待确认设备',
      pointName || '待映射测点',
    ].join(' / '),
  }
}

export function normalizeBusinessAlarms(rows: any[]) {
  return rows.map(row => normalizeBusinessAlarm(row))
}

export function summarizeAlarms(rows: BusinessAlarmRow[]) {
  return {
    total: rows.length,
    active: rows.filter(row => row.displayAlarmIsActive).length,
    critical: rows.filter(row => row.displayAlarmIsActive && row.displayAlarmLevel === 'CRITICAL').length,
    major: rows.filter(row => row.displayAlarmIsActive && row.displayAlarmLevel === 'MAJOR').length,
    anomaly: rows.filter(row => isBusinessAnomaly(row.businessState)).length,
  }
}

export function normalizeFsuDevice(row: Record<string, any>): BusinessFsuDeviceRow {
  const status = String(row.mappingStatus || row.onlineStatus || row.status || '').toUpperCase()
  const businessState: UnifiedDataState =
    status === 'OFFLINE' ? 'offline' :
    status === 'UNMAPPED' ? 'unmapped' :
    status === 'TEMPLATE_ONLY' || status === 'PENDING_REAL_DATA' || status === 'MAPPED_CANDIDATE' ? 'warning' :
    status ? 'normal' : 'warning'

  return {
    ...row,
    businessState,
    businessStatus: unifiedStateBadgeStatus(businessState),
    businessStateLabel: businessState === 'warning' && row.mappingStatus ? mappingStatusLabel(row.mappingStatus) : unifiedStateLabel(businessState),
    displayDevice: firstText(row.deviceName, row.device_name) || '待确认设备',
    displayLastSeenAt: firstText(row.lastSeenAt, row.updatedAt, row.createdAt) || '-',
  }
}

/**
 * FE-REALTIME-SENSOR-NAME-SOURCE-FIX-001: 将技术/协议测点名称收敛为业务传感器名称。
 * "历史测点"/"待映射测点"等通用兜底名不作为有效名称。
 */
export function normalizeSensorName(raw: Record<string, any>): string {
  const rawName = firstText(raw.signalName, raw.signal_name, raw.pointName, raw.point_name, raw.measurementName, raw.measurement_name)
  const fallbackId = firstText(raw.signalId, raw.signal_id, raw.pointCode, raw.point_code, raw.spid, raw.SPID, raw.rawId)

  // "历史测点"等通用兜底名不作为有效源名称, 尝试用 fallbackId 替代
  const effectiveName = isGenericLegacyName(rawName) ? (fallbackId || '') : (rawName || fallbackId || '')
  if (!effectiveName) return ''

  const upper = effectiveName.toUpperCase()

  // 描述性编码前缀匹配 (如 TEMP-001, HUMI-001, VOLT-001 等 seed/demo 数据)
  if (upper.startsWith('TEMP') || upper.startsWith('温度')) return '温度'
  if (upper.startsWith('HUMI') || upper.startsWith('湿度')) return '湿度'
  if (upper.startsWith('VOLT') || upper.startsWith('电压')) return '电压'
  if (upper.startsWith('DOOR') || upper.startsWith('门磁') || upper.startsWith('门')) return '门磁/状态'
  if (upper.startsWith('WATER') || upper.startsWith('水浸') || upper.startsWith('漏水')) return '水浸/状态'
  if (upper.startsWith('SMOKE') || upper.startsWith('烟感') || upper.startsWith('烟雾')) return '烟感'
  if (upper.startsWith('TEMP') || upper.startsWith('温度')) return '温度'
  if (upper.startsWith('HUMI') || upper.startsWith('湿度')) return '湿度'
  if (upper.startsWith('DOOR') || upper.startsWith('门磁') || upper.startsWith('门')) return '门磁/状态'
  if (upper.startsWith('WATER') || upper.startsWith('水浸') || upper.startsWith('漏水')) return '水浸/状态'
  if (upper.startsWith('SMOKE') || upper.startsWith('烟感') || upper.startsWith('烟雾')) return '烟感'
  if (upper.startsWith('CURR') || upper.startsWith('电流')) return '电流'
  if (upper.startsWith('POW') || upper.startsWith('市电') || upper.startsWith('AC')) return '市电'
  if (upper.startsWith('SIGNAL') || upper.startsWith('状态') || upper.startsWith('STATUS')) return '状态'
  // 电池相关: 按电压/中点/告警细分
  if (upper.startsWith('BAT') || effectiveName.includes('电池')) {
    if (effectiveName.includes('中点电压')) return '中点电压'
    if (effectiveName.includes('中点不平衡') || effectiveName.includes('中点') && effectiveName.includes('告警')) return '电池告警'
    if (effectiveName.includes('总电压') || effectiveName.includes('电压')) return '电池电压'
    if (effectiveName.includes('告警') || effectiveName.includes('熔丝') || effectiveName.includes('故障')) return '电池告警'
    return '待确认电池测点'
  }
  if (upper.startsWith('VOLT') || upper.startsWith('电压')) return '电压'

  // 业务传感器名称收敛 (中文/混合)
  if (effectiveName.includes('中点电压')) return '中点电压'
  if (effectiveName.includes('总电压') || effectiveName.includes('电压')) return '电压'
  if (effectiveName.includes('门磁') || effectiveName.includes('门状态')) return '门磁/状态'
  if (effectiveName.includes('市电A路') || effectiveName.includes('交流A相')) return '市电A路'
  if (effectiveName.includes('市电B路') || effectiveName.includes('交流B相')) return '市电B路'
  if (effectiveName.includes('市电')) return '市电'
  if (effectiveName.includes('烟感') || effectiveName.includes('烟雾')) return '烟感'
  if (effectiveName.includes('水浸') || effectiveName.includes('漏水')) return '水浸/状态'
  if (effectiveName.includes('通讯状态') || effectiveName.includes('通信状态')) return '通信状态'
  if (effectiveName.includes('I2C温度') || effectiveName === '温度' || effectiveName.includes('温度计')) return '温度'
  if (effectiveName.includes('I2C湿度') || effectiveName === '湿度' || effectiveName.includes('湿度计')) return '湿度'
  if (effectiveName.includes('温度')) return '温度'
  if (effectiveName.includes('湿度')) return '湿度'
  if (effectiveName.includes('电流')) return '电流'
  if (effectiveName.includes('状态')) return '状态'

  // 纯数字 signalId (B接口2016): 按 device_type 前缀推断
  if (/^\d{8,}$/.test(effectiveName)) {
    const deviceType = effectiveName.substring(2, 4)
    const measureType = effectiveName.substring(6, 7)
    if (deviceType === '06') return measureType === '1' ? '状态/告警' : '电池参数'
    if (deviceType === '07') return '电池信号'
    if (deviceType === '18') return '环境信号'
    return `信号${effectiveName.substring(0, 4)}`
  }

  // rawName 有可读名称时返回原值 (排除通用兜底名)
  if (rawName && !isGenericLegacyName(rawName)) return rawName

  return ''
}

function isGenericLegacyName(name?: string | null): boolean {
  if (!name) return true
  const n = name.trim()
  return n === '历史测点' || n === '待映射测点' || n === '点位名称未确认' ||
         n === '未确认点位' || n === '待确认' || n === '待确认测点' ||
         n === '历史设备' || n === '待确认设备' || n === '点位待确认'
}

/** FE-REALTIME-FINAL-LABEL-TIME-FIX-001: ISO 时间 → 业务友好格式 */
export function formatBusinessDateTime(value?: string): string {
  if (!value) return '-'
  // 2026-05-10T12:00:00 → 2026-05-10 12:00:00
  return String(value).replace('T', ' ').substring(0, 19)
}

export function formatValue(value: unknown) {
  if (value === null || value === undefined || value === '') return '--'
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Number.isInteger(value) ? String(value) : value.toFixed(2)
  }
  const n = Number(value)
  if (!Number.isNaN(n) && String(value).trim() !== '') {
    return Number.isInteger(n) ? String(n) : n.toFixed(2)
  }
  return String(value)
}

/**
 * FE-REALTIME-VALUE-DISPLAY-FIX-001: 按 sensorName 推断展示单位兜底。
 */
export function inferUnitBySensorName(sensorName?: string): string {
  if (!sensorName) return ''
  if (sensorName.includes('温度')) return '℃'
  if (sensorName.includes('湿度')) return '%RH'
  if (sensorName.includes('电压') || sensorName.includes('电池')) return 'V'
  if (sensorName.includes('电流')) return 'A'
  if (sensorName.includes('功率')) return 'W'
  return ''
}

/**
 * FE-STATUS-CHINESE-SEMANTIC-FIX-001: 英文状态值 → 中文业务语义。
 * 普通业务页面主表不裸露 CLOSE/DRY/NORMAL/ALARM 等英文码。
 */
export function normalizeBusinessStatusText(value?: string, sensorName?: string): string {
  if (!value) return ''
  const v = String(value).trim()
  const upper = v.toUpperCase()

  if (upper === 'NORMAL') return '正常'
  if (upper === 'ALARM' || upper === 'ABNORMAL') return '告警'
  if (upper === 'ONLINE') return '在线'
  if (upper === 'OFFLINE') return '离线'
  if (upper === 'ACTIVE') return '未恢复'
  if (upper === 'RECOVERED' || upper === 'CLEARED') return '已恢复'
  if (upper === 'LEGACY' || upper === 'HISTORICAL_PENDING_BACKFILL') return '历史数据'
  if (upper === 'UNMAPPED') return '待映射'

  if (upper === 'CLOSE' || upper === 'CLOSED') return sensorName?.includes('门磁') ? '门磁闭合' : '闭合'
  if (upper === 'OPEN') return sensorName?.includes('门磁') ? '门磁打开' : '打开'

  if (upper === 'DRY') return '无水浸'
  if (upper === 'WET') return '有水浸'

  if (upper === 'GOOD') return '正常'
  if (upper === 'BAD' || upper === 'POOR') return '异常'

  return v
}

/** FE-REALTIME-VALUE-UNIT-STYLE-FIX-001: 计算展示单位 (数值型才有) */
function buildDisplayUnit(value: unknown, unit: string, sensorName: string): string {
  const isTextValue = typeof value === 'string' && isNaN(Number(value))
  if (isTextValue) return ''
  const effectiveUnit = (unit && unit !== '待确认') ? unit : inferUnitBySensorName(sensorName)
  return effectiveUnit
}

/**
 * FE-REALTIME-VALUE-DISPLAY-FIX-001 + FE-STATUS-CHINESE-SEMANTIC-FIX-001:
 * 当前值文本。文本型状态值中文化。
 */
function buildDisplayValueWithUnit(value: unknown, unit: string, sensorName: string): string {
  const val = formatValue(value)
  if (val === '--') return '--'

  const isTextValue = typeof value === 'string' && isNaN(Number(value))
  if (isTextValue) {
    const raw = String(value).trim()
    return normalizeBusinessStatusText(raw, sensorName)
  }

  // 单位由 displayUnit 独立渲染，此处只返回值文本
  return val
}

function deriveRealtimeBusinessState(
  row: Record<string, any>,
  mappingStatus: string,
  legacy: boolean,
  trueUnmapped: boolean,
  hasValue: boolean,
): UnifiedDataState {
  const valueStatus = String(row.valueStatus || row.status || row.quality || '').toUpperCase()
  if (legacy) return 'legacy'
  if (trueUnmapped) return 'unmapped'
  if (valueStatus.includes('PARSE')) return 'parse_error'
  if (valueStatus.includes('OFFLINE')) return 'offline'
  if (valueStatus.includes('ALARM') || valueStatus.includes('ABNORMAL')) return 'alarm'
  if (!hasValue) return 'empty'
  if (row.needRealDataConfirm === true || ['TEMPLATE_ONLY', 'PENDING_REAL_DATA', 'MAPPED_CANDIDATE'].includes(mappingStatus)) return 'warning'
  return 'normal'
}

function deriveAlarmBusinessState(row: Record<string, any>): UnifiedDataState {
  const mappingStatus = String(row.mappingStatus || '').toUpperCase()
  if (mappingStatus === 'UNKNOWN_EVENT_ID' || mappingStatus === 'UNMAPPED') return 'unmapped'
  if (row.displayAlarmIsActive) return row.displayAlarmLevel === 'CRITICAL' || row.displayAlarmLevel === 'MAJOR' ? 'alarm' : 'warning'
  if (row.displayAlarmIsHistory) return 'normal'
  return 'warning'
}

function normalizeRealtimeMappingStatus(row: Record<string, any>) {
  if (row.mappingStatus) return String(row.mappingStatus).toUpperCase()
  if (isHistoricalRealtimeRow(row)) return 'HISTORICAL_PENDING_BACKFILL'
  if (row.signalName || row.pointName || row.point_name) return 'TEMPLATE_ONLY'
  return 'HISTORICAL_PENDING_BACKFILL'
}

function realtimeStateLabel(state: UnifiedDataState, mappingStatus: string) {
  if (state === 'warning' && mappingStatus) return mappingStatusLabel(mappingStatus)
  return unifiedStateLabel(state)
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    if (value === null || value === undefined) continue
    const text = String(value).trim()
    if (text && text !== '-' && text !== '--') return text
  }
  return ''
}
