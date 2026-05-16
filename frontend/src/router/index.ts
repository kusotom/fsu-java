import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('../layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { title: '总览' }, component: () => import('../views/dashboard/DashboardView.vue') },
      { path: 'sites', name: 'Sites', meta: { title: '站点管理' }, component: () => import('../views/resource/SiteManagementView.vue') },
      { path: 'cabinets', name: 'Cabinets', meta: { title: '机柜管理' }, component: () => import('../views/resource/CabinetManagementView.vue') },
      { path: 'devices', name: 'Devices', meta: { title: 'FSU设备管理' }, component: () => import('../views/resource/FsuDeviceManagementView.vue') },
      { path: 'points', name: 'Points', meta: { title: '监控点位' }, component: () => import('../views/resource/PointManagementView.vue') },
      { path: 'telemetry/realtime', name: 'RealtimeData', meta: { title: '实时数据' }, component: () => import('../views/telemetry/RealtimeDataView.vue') },
      { path: 'telemetry/history', name: 'HistoryData', meta: { title: '历史数据' }, component: () => import('../views/telemetry/HistoryDataView.vue') },
      { path: 'alarms', name: 'Alarms', meta: { title: '告警中心' }, component: () => import('../views/alarm/AlarmCenterView.vue') },
      { path: 'b-interface', name: 'BInterface', meta: { title: 'B接口总览' }, component: () => import('../views/binterface/BInterfaceOverviewView.vue') },
      { path: 'b-interface/fsus', name: 'BInterfaceFsuStatus', meta: { title: 'FSU注册状态' }, component: () => import('../views/binterface/FsuStatusView.vue') },
      { path: 'b-interface/logs', name: 'BInterfaceLogs', meta: { title: 'B接口报文日志' }, component: () => import('../views/binterface/MessageLogView.vue') },
      { path: 'b-interface/commands', name: 'BInterfaceCommands', meta: { title: '协议命令覆盖矩阵' }, component: () => import('../views/binterface/CommandMatrixView.vue') },
      { path: 'b-interface/calls', name: 'BInterfaceCalls', meta: { title: 'FSUService调用记录' }, component: () => import('../views/binterface/CallRecordView.vue') },
      { path: 'b-interface/ftp', name: 'BInterfaceFtp', meta: { title: 'FTP文件/图片记录' }, component: () => import('../views/binterface/FtpRecordView.vue') },
      { path: 'system/users', name: 'Users', meta: { title: '用户管理' }, component: () => import('../views/system/UserManagementView.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
