import request from './request'

export function getAlarms() { return request.get('/alarms') }
export function getAlarm(id: number) { return request.get(`/alarms/${id}`) }
export function getAlarmsByStatus(status: string) { return request.get(`/alarms/status/${status}`) }
