export type UnifiedDataState =
  | 'normal'
  | 'warning'
  | 'alarm'
  | 'offline'
  | 'empty'
  | 'stale'
  | 'legacy'
  | 'unmapped'
  | 'parse_error'
  | 'api_error'
  | 'permission_denied'

const STATE_LABELS: Record<UnifiedDataState, string> = {
  normal: '正常',
  warning: '待确认',
  alarm: '告警',
  offline: '离线',
  empty: '无数据',
  stale: '数据过期',
  legacy: '历史待回填',
  unmapped: '未映射',
  parse_error: '解析异常',
  api_error: '接口异常',
  permission_denied: '无权限',
}

const STATUS_BADGE_MAP: Record<UnifiedDataState, string> = {
  normal: 'success',
  warning: 'warning',
  alarm: 'failed',
  offline: 'offline',
  empty: 'unknown',
  stale: 'warning',
  legacy: 'historical_pending_backfill',
  unmapped: 'unmapped',
  parse_error: 'warning',
  api_error: 'failed',
  permission_denied: 'not_whitelisted',
}

const METRIC_STATUS_MAP: Record<UnifiedDataState, 'normal' | 'warning' | 'danger' | 'info' | 'muted'> = {
  normal: 'normal',
  warning: 'warning',
  alarm: 'danger',
  offline: 'warning',
  empty: 'info',
  stale: 'warning',
  legacy: 'info',
  unmapped: 'warning',
  parse_error: 'danger',
  api_error: 'danger',
  permission_denied: 'warning',
}

export function unifiedStateLabel(state?: UnifiedDataState | string | null) {
  const normalized = normalizeUnifiedState(state)
  return STATE_LABELS[normalized]
}

export function unifiedStateBadgeStatus(state?: UnifiedDataState | string | null) {
  return STATUS_BADGE_MAP[normalizeUnifiedState(state)]
}

export function unifiedMetricStatus(state?: UnifiedDataState | string | null) {
  return METRIC_STATUS_MAP[normalizeUnifiedState(state)]
}

export function normalizeUnifiedState(state?: UnifiedDataState | string | null): UnifiedDataState {
  const raw = (state || '').trim().toLowerCase()
  if (!raw) return 'empty'
  if (raw in STATE_LABELS) return raw as UnifiedDataState
  if (['active', 'online', 'success', 'mapped', 'confirmed'].includes(raw)) return 'normal'
  if (['warn', 'pending', 'pending_real_data', 'template_only', 'mapped_candidate'].includes(raw)) return 'warning'
  if (['critical', 'major', 'failed', 'danger'].includes(raw)) return 'alarm'
  if (['historical_pending_backfill'].includes(raw)) return 'legacy'
  if (['unknown_event_id', 'unknown_signal_id'].includes(raw)) return 'unmapped'
  if (['forbidden', 'unauthorized'].includes(raw)) return 'permission_denied'
  if (['network_error', 'server_error', 'api_not_found'].includes(raw)) return 'api_error'
  return 'warning'
}

export function isBusinessAnomaly(state?: UnifiedDataState | string | null) {
  const normalized = normalizeUnifiedState(state)
  return ['alarm', 'offline', 'stale', 'legacy', 'unmapped', 'parse_error', 'api_error', 'permission_denied'].includes(normalized)
}
