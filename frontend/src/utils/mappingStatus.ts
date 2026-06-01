const NORMALIZED_STATUS_LABELS: Record<string, string> = {
  MAPPED: '已映射',
  CONFIRMED: '已映射',
  VERIFIED_BY_REAL_DATA: '真实确认',
  MAPPED_CANDIDATE: '候选映射',
  HIGH: '候选映射',
  MEDIUM: '候选映射',
  TEMPLATE_ONLY: '模板存在，真实未确认',
  PENDING_REAL_DATA: '待真实数据确认',
  DEVICE_ONLY: '设备已发现，待测点上报',
  SIGNAL_PENDING: '设备已发现，待测点上报',
  WAIT_GET_DATA: '等待测点上报',
  HISTORICAL_PENDING_BACKFILL: '历史旧数据，待回填',
  UNMAPPED: '未映射',
  UNKNOWN_EVENT_ID: '未知告警事件',
  UNKNOWN_SIGNAL_ID: '未知 SignalID',
  UNKNOWN_DEVICE_SIGNAL_PAIR: '设备与点位组合未确认',
}

const UNMAPPED_STATUSES = new Set([
  'UNMAPPED',
])

const UNKNOWN_EVENT_STATUSES = new Set([
  'UNKNOWN_EVENT_ID',
])

const PENDING_STATUSES = new Set([
  'TEMPLATE_ONLY',
  'PENDING_REAL_DATA',
])

const DEVICE_ONLY_STATUSES = new Set([
  'DEVICE_ONLY',
  'SIGNAL_PENDING',
  'WAIT_GET_DATA',
])

const HISTORICAL_STATUSES = new Set([
  'HISTORICAL_PENDING_BACKFILL',
])

const MAPPED_STATUSES = new Set([
  'MAPPED',
  'CONFIRMED',
  'VERIFIED_BY_REAL_DATA',
  'MAPPED_CANDIDATE',
  'HIGH',
  'MEDIUM',
])

export function normalizeMappingStatus(status?: string | null, confidence?: string | null) {
  const rawStatus = (status || '').trim().toUpperCase()
  if (rawStatus) return rawStatus
  return (confidence || '').trim().toUpperCase()
}

export function mappingStatusLabel(status?: string | null, confidence?: string | null) {
  const normalized = normalizeMappingStatus(status, confidence)
  return NORMALIZED_STATUS_LABELS[normalized] || normalized || '未知'
}

export function mappingStatusBadgeStatus(status?: string | null, confidence?: string | null) {
  const normalized = normalizeMappingStatus(status, confidence)
  if (MAPPED_STATUSES.has(normalized)) return 'mapped'
  if (PENDING_STATUSES.has(normalized)) return 'warning'
  if (DEVICE_ONLY_STATUSES.has(normalized) || HISTORICAL_STATUSES.has(normalized)) return 'unknown'
  if (UNKNOWN_EVENT_STATUSES.has(normalized)) return 'unknown_event_id'
  return 'unmapped'
}

export function mappingConfidenceTagType(confidence?: string | null) {
  const normalized = (confidence || '').trim().toUpperCase()
  if (normalized === 'HIGH') return 'success'
  if (normalized === 'MEDIUM' || normalized === 'PENDING_REAL_DATA') return 'warning'
  return 'info'
}

export function isUnmappedMappingStatus(status?: string | null) {
  return UNMAPPED_STATUSES.has(normalizeMappingStatus(status))
}

export function isUnknownEventMappingStatus(status?: string | null) {
  return UNKNOWN_EVENT_STATUSES.has(normalizeMappingStatus(status))
}

export function isPendingMappingStatus(status?: string | null) {
  return PENDING_STATUSES.has(normalizeMappingStatus(status))
}

export function isDeviceOnlyMappingStatus(status?: string | null) {
  return DEVICE_ONLY_STATUSES.has(normalizeMappingStatus(status))
}

export function isHistoricalPendingBackfillStatus(status?: string | null) {
  return HISTORICAL_STATUSES.has(normalizeMappingStatus(status))
}

export function isMappedCandidateStatus(status?: string | null, confidence?: string | null) {
  return MAPPED_STATUSES.has(normalizeMappingStatus(status, confidence))
}

export function hasSignalIdentity(row: Record<string, any>) {
  return Boolean(firstMeaningful(row.signalId) || firstMeaningful(row.spid) || firstMeaningful(row.rawId) || firstMeaningful(row.eventId))
}

export function isDeviceOnlyObservation(row: Record<string, any>) {
  const status = normalizeMappingStatus(row.mappingStatus)
  if (DEVICE_ONLY_STATUSES.has(status)) return true
  const source = String(row.source || row.rawCommand || '').trim().toUpperCase()
  const reason = String(row.reason || '').trim().toUpperCase()
  return !hasSignalIdentity(row) && (source === 'GET_LOGININFO' || reason === 'MISSING_SIGNAL_MAPPING' || reason === 'DEVICE_DISCOVERED_WAIT_GET_DATA')
}

export function isHistoricalRealtimeRow(row: Record<string, any>) {
  const status = normalizeMappingStatus(row.mappingStatus)
  if (HISTORICAL_STATUSES.has(status)) return true
  const source = String(row.source || '').trim().toUpperCase()
  const hasBackendMapping = Boolean(row.mappingStatus || row.signalName || row.unit || row.valueMeaning || row.mappingConfidence)
  const hasDeviceIdentity = Boolean(firstMeaningful(row.deviceId) || firstMeaningful(row.deviceCode))
  return !hasBackendMapping && !hasDeviceIdentity && (!source || source === 'UNKNOWN')
}

export function isTrueUnmappedPoint(row: Record<string, any>) {
  return isUnmappedMappingStatus(row.mappingStatus) && hasSignalIdentity(row) && !isDeviceOnlyObservation(row) && !isHistoricalRealtimeRow(row)
}

function firstMeaningful(value: any) {
  const text = value === null || value === undefined ? '' : String(value).trim()
  return text && text !== '-' && text !== '--' ? text : ''
}
