import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：添加 Authorization header (FE-AUTH-005 + FE-AUTH-PERSIST-FIX-001)
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  const t = userStore.token || localStorage.getItem('fsu_auth_token')
  if (t) {
    config.headers.Authorization = `Bearer ${t}`
  }
  return config
})

// 响应拦截器：统一错误处理 (FE-INFRA-002 + FE-AUTH-005)
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const userStore = useUserStore()
        userStore.clearAuth()
        // 跳转登录页（保留当前路径为 redirect）
        const currentPath = window.location.hash.replace('#', '') || '/'
        if (currentPath !== '/login') {
          window.location.hash = `#/login?redirect=${encodeURIComponent(currentPath)}`
        }
      } else if (status === 403) {
        const currentPath = window.location.hash.replace('#', '') || '/'
        if (currentPath !== '/403' && currentPath !== '/login') {
          window.location.hash = '#/403'
        }
      } else {
        console.error('[API]', status, error.response.statusText)
      }
    } else if (error.request) {
      console.error('[API] Network error:', error.message)
    } else {
      console.error('[API]', error.message)
    }
    return Promise.reject(error)
  }
)

export default request
