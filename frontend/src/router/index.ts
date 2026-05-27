import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // Auth (standalone, no layout)
  { path: '/login', name: 'Login', meta: { title: '登录', public: true }, component: () => import('../views/auth/LoginView.vue') },
  { path: '/forgot-password', name: 'ForgotPassword', meta: { title: '忘记密码', public: true }, component: () => import('../views/auth/ForgotPasswordView.vue') },
  { path: '/403', name: 'Forbidden', meta: { title: '无权限', public: true }, component: () => import('../views/system/ForbiddenView.vue') },
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
      { path: 'b-interface/fsus', name: 'BInterfaceFsuStatus', meta: { title: 'FSU注册状态', requiresAuth: true, permissions: ['binterface.fsu.read'] }, component: () => import('../views/binterface/FsuStatusView.vue') },
      { path: 'b-interface/fsus/:fsuCode', name: 'BInterfaceFsuStatusDetail', meta: { title: 'FSU详情', requiresAuth: true, permissions: ['binterface.fsu.read'] }, component: () => import('../views/binterface/FsuStatusDetailView.vue') },
      { path: 'b-interface/realtime', name: 'BInterfaceRealtime', meta: { title: 'B接口实时数据', requiresAuth: true, permissions: ['binterface.realtime.read'] }, component: () => import('../views/binterface/BInterfaceRealtimeView.vue') },
      { path: 'b-interface/alarms', name: 'BInterfaceAlarms', meta: { title: 'B接口告警增强', requiresAuth: true, permissions: ['binterface.alarm.read'] }, component: () => import('../views/binterface/BInterfaceAlarmView.vue') },
      { path: 'b-interface/thresholds', name: 'BInterfaceThresholds', meta: { title: 'B接口门限只读', requiresAuth: true, permissions: ['binterface.threshold.read'] }, component: () => import('../views/binterface/BInterfaceThresholdView.vue') },
      { path: 'b-interface/ftp-login-info', name: 'BInterfaceFtpLoginInfo', meta: { title: 'FTP与注册参数', requiresAuth: true, permissions: ['binterface.ftp.read'] }, component: () => import('../views/binterface/BInterfaceFtpLoginInfoView.vue') },
      { path: 'b-interface/ftp-images', name: 'BInterfaceFtpImages', meta: { title: 'FTP图片', requiresAuth: true, permissions: ['binterface.ftp_image.read'] }, component: () => import('../views/binterface/BInterfaceFtpImageView.vue') },
      { path: 'b-interface/schedulers', name: 'BInterfaceSchedulers', meta: { title: '调度配置', requiresAuth: true, permissions: ['binterface.scheduler.read'] }, component: () => import('../views/binterface/BInterfaceSchedulerView.vue') },
      { path: 'b-interface/protocol-audit', name: 'BInterfaceProtocolAudit', meta: { title: '协议治理', requiresAuth: true, permissions: ['binterface.protocol_audit.read'] }, component: () => import('../views/binterface/BInterfaceProtocolAuditView.vue') },
      { path: 'b-interface/logs', name: 'BInterfaceLogs', meta: { title: 'B接口报文日志' }, component: () => import('../views/binterface/MessageLogView.vue') },
      { path: 'b-interface/commands', name: 'BInterfaceCommands', meta: { title: '协议命令覆盖矩阵' }, component: () => import('../views/binterface/CommandMatrixView.vue') },
      { path: 'b-interface/calls', name: 'BInterfaceCalls', meta: { title: 'FSUService调用记录' }, component: () => import('../views/binterface/CallRecordView.vue') },
      { path: 'b-interface/ftp', name: 'BInterfaceFtp', meta: { title: 'FTP文件/图片记录' }, component: () => import('../views/binterface/FtpRecordView.vue') },
      { path: 'system/users', name: 'Users', meta: { title: '用户管理', requiresAuth: true, permissions: ['user.read'] }, component: () => import('../views/system/UserManagementView.vue') },
      { path: 'system/roles', name: 'Roles', meta: { title: '角色管理', requiresAuth: true, permissions: ['role.read'] }, component: () => import('../views/system/RoleManagementView.vue') },
      { path: 'system/permissions', name: 'Permissions', meta: { title: '权限管理', requiresAuth: true, permissions: ['permission.read'] }, component: () => import('../views/system/PermissionManagementView.vue') },
      { path: 'profile/security', name: 'ProfileSecurity', meta: { title: '安全设置', requiresAuth: true }, component: () => import('../views/profile/ProfileSecurityView.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
