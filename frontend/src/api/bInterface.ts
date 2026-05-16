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

// FSU Status
export function getFsuStatusList() { return request.get('/b-interface/fsu-status') }
export function getFsuStatus(id: number) { return request.get(`/b-interface/fsu-status/${id}`) }

// Call Records
export function getCallRecords() { return request.get('/b-interface/call-records') }
export function getCallRecord(id: number) { return request.get(`/b-interface/call-records/${id}`) }

// FTP Records
export function getFtpRecords() { return request.get('/b-interface/ftp-records') }
export function getFtpRecord(id: number) { return request.get(`/b-interface/ftp-records/${id}`) }
