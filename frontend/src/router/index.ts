import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * FE-IA-CLEANUP-P1-001: 普通业务 IA 收敛.
 * 后端字典、映射、审计、协议诊断能力保留；普通业务导航不再暴露内部治理入口.
 *
 * 普通菜单:
 *   1. 监控中心: /dashboard, /sites/realtime, /alarms
 *   2. 站点监控: /sites, /b-interface/fsus
 *   3. 三方授权: /system/users, /system/roles, /system/permissions, /system/site-authorizations, /system/fsu-authorizations
 *   4. 系统设置: /profile/security
 */

const TENANT_ADMIN_ROLES = ['super_admin', 'platform_admin', 'admin']
const ELEVATED_ROLES = ['super_admin', 'platform_admin', 'protocol_debugger', 'admin']

const routes: RouteRecordRaw[] = [
  // ===== Auth (standalone, no layout) =====
  { path: '/login', name: 'Login', meta: { title: '登录', public: true }, component: () => import('../views/auth/LoginView.vue') },
  { path: '/forgot-password', name: 'ForgotPassword', meta: { title: '忘记密码', public: true }, component: () => import('../views/auth/ForgotPasswordView.vue') },
  { path: '/403', name: 'Forbidden', meta: { title: '无权限', public: true }, component: () => import('../views/system/ForbiddenView.vue') },

  // ===== Layout =====
  {
    path: '/',
    component: () => import('../layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      // ========== 1. 监控中心 ==========
      { path: 'dashboard', name: 'Dashboard', meta: { title: '监控驾驶舱', requiresAuth: true }, component: () => import('../views/dashboard/DashboardView.vue') },
      { path: 'sites', name: 'Sites', meta: { title: '站点监控', requiresAuth: true }, component: () => import('../views/resource/SiteManagementView.vue') },
      { path: 'sites/realtime', name: 'RealtimeData', meta: { title: '站点实时数据', requiresAuth: true }, component: () => import('../views/telemetry/RealtimeDataView.vue') },
      { path: 'telemetry/realtime', redirect: '/sites/realtime' },
      { path: 'telemetry/history', name: 'HistoryData', meta: { title: '历史数据', requiresAuth: true }, component: () => import('../views/telemetry/HistoryDataView.vue') },
      { path: 'alarms', name: 'Alarms', meta: { title: '告警中心', requiresAuth: true }, component: () => import('../views/alarm/AlarmCenterView.vue') },

      // ========== 2. 站点监控 ==========
      { path: 'b-interface/fsus', name: 'BInterfaceFsuStatus', meta: { title: 'FSU 管理', requiresAuth: true, permissions: ['fsu:view'] }, component: () => import('../views/binterface/FsuStatusView.vue') },
      { path: 'b-interface/fsus/:fsuCode', name: 'BInterfaceFsuStatusDetail', meta: { title: 'FSU 详情', requiresAuth: true, permissions: ['fsu:view'] }, component: () => import('../views/binterface/FsuStatusDetailView.vue') },

      // ========== 内部资产/点位治理：保留路由，普通菜单隐藏 ==========
      { path: 'devices', name: 'Devices', meta: { title: '设备管理', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/resource/FsuDeviceManagementView.vue') },
      { path: 'cabinets', name: 'Cabinets', meta: { title: '机柜管理', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/resource/CabinetManagementView.vue') },
      { path: 'points', name: 'Points', meta: { title: '点位字典', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/resource/PointManagementView.vue') },
      { path: 'mapping', name: 'PointMapping', meta: { title: '点位映射', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/resource/PointMappingView.vue') },
      { path: 'unmapped', name: 'UnmappedPoints', meta: { title: '未映射点位', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/resource/UnmappedPointsView.vue') },

      // ========== 3. 三方授权 ==========
      { path: 'system/users', name: 'Users', meta: { title: '用户管理', requiresAuth: true, permissions: ['user:view'], roles: TENANT_ADMIN_ROLES }, component: () => import('../views/system/UserManagementView.vue') },
      { path: 'system/roles', name: 'Roles', meta: { title: '角色管理', requiresAuth: true, permissions: ['role:view'], roles: TENANT_ADMIN_ROLES }, component: () => import('../views/system/RoleManagementView.vue') },
      { path: 'system/permissions', name: 'Permissions', meta: { title: '权限管理', requiresAuth: true, permissions: ['permission:view'], roles: TENANT_ADMIN_ROLES }, component: () => import('../views/system/PermissionManagementView.vue') },
      { path: 'system/site-authorizations', name: 'SiteAuthorizations', meta: { title: '站点授权', requiresAuth: true, permissions: ['tenant:view'], roles: TENANT_ADMIN_ROLES }, component: () => import('../views/system/SiteAuthorizationView.vue') },
      { path: 'system/fsu-authorizations', name: 'FsuAuthorizations', meta: { title: 'FSU 授权', requiresAuth: true, permissions: ['tenant:view'], roles: TENANT_ADMIN_ROLES }, component: () => import('../views/system/FsuAuthorizationView.vue') },

      // ========== 4. 协议诊断：内部路由保留，普通菜单隐藏 ==========
      { path: 'b-interface', name: 'BInterface', meta: { title: '协议概览', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceOverviewView.vue') },
      { path: 'b-interface/logs', name: 'BInterfaceLogs', meta: { title: 'raw XML', requiresAuth: true, permissions: ['protocol:raw:view'], roles: ELEVATED_ROLES }, component: () => import('../views/binterface/MessageLogView.vue') },
      { path: 'b-interface/commands', name: 'BInterfaceCommands', meta: { title: '协议矩阵', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/binterface/CommandMatrixView.vue') },
      { path: 'b-interface/calls', name: 'BInterfaceCalls', meta: { title: '通信记录', requiresAuth: true, permissions: ['protocol:raw:view'], roles: ELEVATED_ROLES }, component: () => import('../views/binterface/CallRecordView.vue') },
      { path: 'b-interface/ftp', name: 'BInterfaceFtp', meta: { title: 'FTP 记录', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/binterface/FtpRecordView.vue') },
      // 保留原有协议诊断子路由 (作为协议诊断模块内部的详细页)
      { path: 'b-interface/realtime', name: 'BInterfaceRealtime', meta: { title: 'B接口实时数据', requiresAuth: true, permissions: ['realtime:view'], roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceRealtimeView.vue') },
      { path: 'b-interface/alarms', name: 'BInterfaceAlarms', meta: { title: 'B接口告警', requiresAuth: true, permissions: ['alarm:view'], roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceAlarmView.vue') },
      { path: 'b-interface/thresholds', name: 'BInterfaceThresholds', meta: { title: '门限只读', requiresAuth: true, permissions: ['protocol:runonce:readonly'], roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceThresholdView.vue') },
      { path: 'b-interface/ftp-login-info', name: 'BInterfaceFtpLoginInfo', meta: { title: 'FTP 参数', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceFtpLoginInfoView.vue') },
      { path: 'b-interface/ftp-images', name: 'BInterfaceFtpImages', meta: { title: 'FTP 图片', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceFtpImageView.vue') },
      { path: 'b-interface/schedulers', name: 'BInterfaceSchedulers', meta: { title: '只读 run-once', requiresAuth: true, permissions: ['protocol:runonce:readonly'], roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceSchedulerView.vue') },
      { path: 'b-interface/protocol-audit', name: 'BInterfaceProtocolAudit', meta: { title: '协议治理', requiresAuth: true, roles: ELEVATED_ROLES }, component: () => import('../views/binterface/BInterfaceProtocolAuditView.vue') },

      // ========== 5. 系统设置 ==========
      { path: 'profile/security', name: 'ProfileSecurity', meta: { title: '安全设置', requiresAuth: true }, component: () => import('../views/profile/ProfileSecurityView.vue') },
    ]
  },

  // ===== FE-P1-LAYOUT-001: 旧路由 redirect =====
  // 保留所有旧 B接口 子路由入口，重定向到新位置或保持可用
  { path: '/b-interface/overview', redirect: '/b-interface' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
