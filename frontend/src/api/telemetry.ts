import request from './request'

export function getRealtimeData() { return request.get('/telemetry/realtime') }
export function getRealtimeDataById(id: number) { return request.get(`/telemetry/realtime/${id}`) }

export function getHistoryData() { return request.get('/telemetry/history') }
export function getHistoryDataById(id: number) { return request.get(`/telemetry/history/${id}`) }
export function queryHistoryData(pointId: number, start: string, end: string) {
  return request.get('/telemetry/history/query', { params: { pointId, start, end } })
}

export function getHeartbeats() { return request.get('/telemetry/heartbeats') }
export function getHeartbeatById(id: number) { return request.get(`/telemetry/heartbeats/${id}`) }
