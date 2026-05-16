import request from './request'
import type { UserAccount, Role } from '@/types'

// User
export function getUsers() { return request.get('/system/users') }
export function getUser(id: number) { return request.get(`/system/users/${id}`) }
export function createUser(data: UserAccount) { return request.post('/system/users', data) }
export function updateUser(id: number, data: UserAccount) { return request.put(`/system/users/${id}`, data) }
export function deleteUser(id: number) { return request.delete(`/system/users/${id}`) }

// Role
export function getRoles() { return request.get('/system/roles') }
export function getRole(id: number) { return request.get(`/system/roles/${id}`) }
export function createRole(data: Role) { return request.post('/system/roles', data) }
export function updateRole(id: number, data: Role) { return request.put(`/system/roles/${id}`, data) }
export function deleteRole(id: number) { return request.delete(`/system/roles/${id}`) }
