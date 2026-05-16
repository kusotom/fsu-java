import request from './request'
import type { Site, Cabinet, FsuDevice, MonitoringPoint } from '@/types'

// Site
export function getSites() { return request.get('/sites') }
export function getSite(id: number) { return request.get(`/sites/${id}`) }
export function createSite(data: Site) { return request.post('/sites', data) }
export function updateSite(id: number, data: Site) { return request.put(`/sites/${id}`, data) }
export function deleteSite(id: number) { return request.delete(`/sites/${id}`) }

// Cabinet
export function getCabinets() { return request.get('/cabinets') }
export function getCabinet(id: number) { return request.get(`/cabinets/${id}`) }
export function createCabinet(data: Cabinet) { return request.post('/cabinets', data) }
export function updateCabinet(id: number, data: Cabinet) { return request.put(`/cabinets/${id}`, data) }
export function deleteCabinet(id: number) { return request.delete(`/cabinets/${id}`) }

// FsuDevice
export function getFsuDevices() { return request.get('/fsu-devices') }
export function getFsuDevice(id: number) { return request.get(`/fsu-devices/${id}`) }
export function createFsuDevice(data: FsuDevice) { return request.post('/fsu-devices', data) }
export function updateFsuDevice(id: number, data: FsuDevice) { return request.put(`/fsu-devices/${id}`, data) }
export function deleteFsuDevice(id: number) { return request.delete(`/fsu-devices/${id}`) }

// MonitoringPoint
export function getMonitoringPoints() { return request.get('/monitoring-points') }
export function getMonitoringPoint(id: number) { return request.get(`/monitoring-points/${id}`) }
export function createMonitoringPoint(data: MonitoringPoint) { return request.post('/monitoring-points', data) }
export function updateMonitoringPoint(id: number, data: MonitoringPoint) { return request.put(`/monitoring-points/${id}`, data) }
export function deleteMonitoringPoint(id: number) { return request.delete(`/monitoring-points/${id}`) }
