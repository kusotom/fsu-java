import request from './request'

// Health
export function getBInterfaceHealth() { return request.get('/b-interface/health') }

// Commands
export function getCommands() { return request.get('/b-interface/commands') }
export function getCommand(id: number) { return request.get(`/b-interface/commands/${id}`) }

// Message Logs
export function getMessageLogs() { return request.get('/b-interface/message-logs') }
export function getMessageLog(id: number) { return request.get(`/b-interface/message-logs/${id}`) }

// Sessions
export function getSessions() { return request.get('/b-interface/sessions') }
export function getSession(id: number) { return request.get(`/b-interface/sessions/${id}`) }

// FSU Status (legacy)
export function getFsuStatusList() { return request.get('/b-interface/fsu-status') }
export function getFsuStatus(id: number) { return request.get(`/b-interface/fsu-status/${id}`) }

// FSU GET_FSUINFO Heartbeat (2016) — 后端 REST Controller 待新增
export function runFsuHeartbeatDryRun(fsuCode: string) { return request.post('/b-interface/2016/heartbeat/run-once', { fsuCode, dryRun: true, mock: false }) }
export function runFsuHeartbeatReal(fsuCode: string, operator: string, reason: string) { return request.post('/b-interface/2016/heartbeat/run-once', { fsuCode, dryRun: false, mock: false, operator, reason }) }

// === BACKEND-FE-API-002: FSU / Device / Alarms / Realtime / Unmapped / Thresholds / MessageLogs ===
export function getBInterfaceFsus(params?: Record<string,any>) { return request.get('/b-interface/fsus', { params }) }
export function getBInterfaceFsuDetail(fsuCode: string) { return request.get(`/b-interface/fsus/${fsuCode}`) }
export function getBInterfaceFsuDevices(fsuCode: string) { return request.get(`/b-interface/fsus/${fsuCode}/devices`) }
export function getBInterfaceAlarms(params?: Record<string,any>) { return request.get('/b-interface/alarms', { params }) }
export function getBInterfaceRealtimePoints(params?: Record<string,any>) { return request.get('/b-interface/realtime-points', { params }) }
export function getBInterfaceUnmappedSignals(params?: Record<string,any>) { return request.get('/b-interface/unmapped-signals', { params }) }
export function getBInterfaceThresholds(params?: Record<string,any>) { return request.get('/b-interface/thresholds', { params }) }
export function queryBInterfaceMessageLogs(params?: Record<string,any>) { return request.get('/b-interface/message-logs/query', { params }) }

// === BACKEND-FE-API-003: FTP / LoginInfo / Images / Scheduler / Protocol / Audit / ActiveAlarm / RunOnce ===
export function getBInterfaceFtpConfigs(params?: Record<string,any>) { return request.get('/b-interface/ftp', { params }) }
export function getBInterfaceLoginInfo(params?: Record<string,any>) { return request.get('/b-interface/login-info', { params }) }
export function getBInterfaceFtpImages(params?: Record<string,any>) { return request.get('/b-interface/ftp/images', { params }) }
export function getBInterfaceFtpImagePullRuns(params?: Record<string,any>) { return request.get('/b-interface/ftp/images/pull-runs', { params }) }
export function getBInterfaceSchedulerConfigs(params?: Record<string,any>) { return request.get('/b-interface/schedulers/configs', { params }) }
export function getBInterfaceSchedulerRuns(params?: Record<string,any>) { return request.get('/b-interface/schedulers/runs', { params }) }
export function getBInterfaceProtocolMatrix(params?: Record<string,any>) { return request.get('/b-interface/protocol/matrix', { params }) }
export function getBInterfaceErrorCodes(params?: Record<string,any>) { return request.get('/b-interface/error-codes', { params }) }
export function getBInterfaceProtocolProfiles() { return request.get('/b-interface/protocol/profiles') }
export function getBInterfaceAudits(params?: Record<string,any>) { return request.get('/b-interface/audits', { params }) }
export function getBInterfaceSetSafetyPolicies() { return request.get('/b-interface/set/safety-policies') }
export function getBInterfaceActiveAlarmDiff(params?: Record<string,any>) { return request.get('/b-interface/active-alarms/diff', { params }) }
export function getBInterfaceActiveAlarmAuditRuns(params?: Record<string,any>) { return request.get('/b-interface/active-alarms/audit-runs', { params }) }
export function getBInterfaceRunOnceCapabilities() { return request.get('/b-interface/run-once/capabilities') }

// BIF2016-AUTH-002: FSU 注册上下文
export function getBInterfaceRegistrationContext(fsuCode: string) {
  return request.get(`/b-interface/2016/auth/registration-context/${fsuCode}`)
}

// FAST-REALDATA-001: B接口2016 只读 run-once
export function runFsuInfoReadOnly(data: { fsuCode: string; operator: string; reason: string; confirmReadOnly: boolean }) {
  return request.post('/b-interface/2016/read-only/fsu-info/run-once', data)
}
export function runGetDataProbe(data: {
  fsuCode: string; deviceIds: string[]; operator: string; reason: string; confirmReadOnly: boolean; maxSignalsPerDevice?: number
}) {
  return request.post('/b-interface/2016/read-only/get-data/probe', data)
}

// Call Records
export function getCallRecords() { return request.get('/b-interface/call-records') }
export function getCallRecord(id: number) { return request.get(`/b-interface/call-records/${id}`) }

// FTP Records
export function getFtpRecords() { return request.get('/b-interface/ftp-records') }
export function getFtpRecord(id: number) { return request.get(`/b-interface/ftp-records/${id}`) }
