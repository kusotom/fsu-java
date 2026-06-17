export type AlarmLevelCode = 'CRITICAL' | 'MAJOR' | 'MINOR' | 'WARN' | 'INFO' | 'UNKNOWN'

export interface NormalizedAlarmLevel {
  level: AlarmLevelCode
  label: string
  raw?: string
}

export interface NormalizedAlarmStatus {
  status: string
  label: string
  isActive: boolean
  isHistory: boolean
}

export function normalizeAlarmLevel(alarmLevel?: unknown, eventSeverity?: unknown): NormalizedAlarmLevel {
  const severity = normalizeLevelToken(eventSeverity, true)
  if (severity.level !== 'UNKNOWN') return severity
  const level = normalizeLevelToken(alarmLevel, false)
  if (level.level !== 'UNKNOWN') return level

  const raw = firstText(eventSeverity, alarmLevel)
  return { level: 'UNKNOWN', label: raw || '待确认', raw }
}

export function normalizeAlarmStatus(status?: unknown, alarmFlag?: unknown): NormalizedAlarmStatus {
  const raw = firstText(status, alarmFlag).toUpperCase()
  if (['ACTIVE', 'NEW', 'BEGIN', 'START', 'RAISE', 'RAISED', 'ALARM', '1'].includes(raw)) {
    return { status: 'ACTIVE', label: '活跃', isActive: true, isHistory: false }
  }
  if (['CONFIRMED', 'ACK', 'ACKED'].includes(raw)) {
    return { status: 'CONFIRMED', label: '已确认', isActive: true, isHistory: false }
  }
  if (['CLEARED', 'CLEAR', 'RECOVERED', 'RECOVERY', 'END', 'CLOSED', 'NORMAL', '0'].includes(raw)) {
    return { status: 'RECOVERED', label: '已恢复', isActive: false, isHistory: true }
  }
  return { status: raw || 'UNKNOWN', label: raw || '未知', isActive: false, isHistory: false }
}

export function normalizeAlarmRow<T extends Record<string, any>>(row: T): T & Record<string, any> {
  const level = normalizeAlarmLevel(row.alarmLevel, row.eventSeverity)
  const status = normalizeAlarmStatus(row.alarmStatus || row.status, row.alarmFlag)
  const displayAlarmName = firstText(row.eventName, row.alarmName, row.signalName, row.alarmDesc) || '未命名告警'
  const displayAlarmMeaning = firstText(row.alarmMeaning, row.valueMeaning, row.alarmDesc)
  const displayOccurTime = firstText(row.occurTime, row.alarmTime, row.startTime, row.createdAt)
  const displayRecoverTime = firstText(row.clearTime, row.recoveryTime, row.recoverTime, row.endTime)
  const displayAlarmValue = formatAlarmValue(row.alarmValue ?? row.triggerVal ?? row.value)

  return {
    ...row,
    displayAlarmName,
    displayAlarmMeaning,
    displayAlarmLevel: level.level,
    displayAlarmLevelLabel: level.label,
    displayAlarmStatus: status.status,
    displayAlarmStatusLabel: status.label,
    displayAlarmIsActive: status.isActive,
    displayAlarmIsHistory: status.isHistory,
    displayOccurTime,
    displayRecoverTime,
    displayAlarmValue,
    displayFsu: firstText(row.fsuCode, row.fsuId) || '-',
    displayDevice: firstText(row.deviceName, row.deviceCode, row.deviceId) || '-',
  }
}

export function alarmLevelLabel(level?: unknown, eventSeverity?: unknown) {
  return normalizeAlarmLevel(level, eventSeverity).label
}

export function alarmLevelStatus(level?: unknown, eventSeverity?: unknown) {
  const normalized = normalizeAlarmLevel(level, eventSeverity).level
  if (normalized === 'CRITICAL' || normalized === 'MAJOR') return 'failed'
  if (normalized === 'MINOR' || normalized === 'WARN') return 'warning'
  return 'info'
}

export function formatAlarmValue(value?: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  return String(value)
}

function normalizeLevelToken(value?: unknown, fromEventSeverity = false): NormalizedAlarmLevel {
  const raw = firstText(value)
  const upper = raw.toUpperCase()
  if (!raw) return { level: 'UNKNOWN', label: '', raw }

  if (['CRITICAL', 'URGENT', 'LEVEL1', 'L1', '一级', '一级告警', '严重', '紧急'].includes(upper) || raw.includes('一级')) {
    return { level: 'CRITICAL', label: '一级告警', raw }
  }
  if (['MAJOR', 'IMPORTANT', 'LEVEL2', 'L2', '二级', '二级告警', '重要'].includes(upper) || raw.includes('二级')) {
    return { level: 'MAJOR', label: '二级告警', raw }
  }
  if (['MINOR', 'LEVEL3', 'L3', '三级', '三级告警', '次要'].includes(upper) || raw.includes('三级')) {
    return { level: 'MINOR', label: '三级告警', raw }
  }
  if (['WARN', 'WARNING', 'LEVEL4', 'L4', '四级', '四级告警', '警告'].includes(upper) || raw.includes('四级')) {
    return { level: 'WARN', label: '四级告警', raw }
  }
  if (['INFO', 'INFORMATION', '提示', '通知'].includes(upper)) {
    return { level: 'INFO', label: '提示', raw }
  }

  if (/^\d+$/.test(raw)) {
    const eventSeverityMap: Record<string, NormalizedAlarmLevel> = {
      '0': { level: 'CRITICAL', label: '一级告警', raw },
      '1': { level: 'MAJOR', label: '二级告警', raw },
      '2': { level: 'MINOR', label: '三级告警', raw },
      '3': { level: 'WARN', label: '四级告警', raw },
      '4': { level: 'INFO', label: '提示', raw },
    }
    const alarmLevelMap: Record<string, NormalizedAlarmLevel> = {
      '1': { level: 'CRITICAL', label: '一级告警', raw },
      '2': { level: 'MAJOR', label: '二级告警', raw },
      '3': { level: 'MINOR', label: '三级告警', raw },
      '4': { level: 'WARN', label: '四级告警', raw },
      '5': { level: 'INFO', label: '提示', raw },
    }
    return (fromEventSeverity ? eventSeverityMap : alarmLevelMap)[raw] || { level: 'UNKNOWN', label: `等级 ${raw}`, raw }
  }

  return { level: 'UNKNOWN', label: raw, raw }
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    if (value === null || value === undefined) continue
    const text = String(value).trim()
    if (text && text !== '-' && text !== '--') return text
  }
  return ''
}
