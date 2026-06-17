/**
 * 权限点注册表 (FE-AUTH-003). source=frontend-planning.
 */
export const PERMISSION_GROUPS = {
  BINTERFACE_READ: 'B接口查看',
  BINTERFACE_OPERATION: 'B接口只读操作',
  SCHEDULER: '调度',
  RISK_OPERATION: '高风险操作',
  SYSTEM: '系统管理',
  AUDIT: '审计',
} as const

export const PERMISSIONS = {
  // B接口查看
  BINTERFACE_READ: 'binterface.read',
  BINTERFACE_FSU_READ: 'binterface.fsu.read',
  BINTERFACE_REALTIME_READ: 'binterface.realtime.read',
  BINTERFACE_ALARM_READ: 'binterface.alarm.read',
  BINTERFACE_THRESHOLD_READ: 'binterface.threshold.read',
  BINTERFACE_FTP_READ: 'binterface.ftp.read',
  BINTERFACE_FTP_IMAGE_READ: 'binterface.ftp_image.read',
  BINTERFACE_SCHEDULER_READ: 'binterface.scheduler.read',
  BINTERFACE_PROTOCOL_AUDIT_READ: 'binterface.protocol_audit.read',
  // FE-P0-RECTIFY-001: raw XML 权限
  PROTOCOL_RAW_VIEW: 'protocol:raw:view',
  PROTOCOL_RAW_DOWNLOAD: 'protocol:raw:download',
  // 只读操作
  BINTERFACE_DRY_RUN: 'binterface.dry_run',
  BINTERFACE_MOCK: 'binterface.mock',
  BINTERFACE_GET_RUN: 'binterface.get.run',
  // FE-P0-RECTIFY-001: run-once 只读
  PROTOCOL_RUNONCE_READONLY: 'protocol:runonce:readonly',
  // 调度
  SCHEDULER_READ: 'scheduler.read',
  SCHEDULER_MANAGE: 'scheduler.manage',
  // 高风险
  SET_POINT_PLAN: 'binterface.set_point.plan',
  SET_POINT_EXECUTE: 'binterface.set_point.execute',
  SET_THRESHOLD_PLAN: 'binterface.set_threshold.plan',
  SET_THRESHOLD_EXECUTE: 'binterface.set_threshold.execute',
  SET_FTP_PLAN: 'binterface.set_ftp.plan',
  SET_FTP_EXECUTE: 'binterface.set_ftp.execute',
  SET_LOGININFO_PLAN: 'binterface.set_logininfo.plan',
  SET_LOGININFO_EXECUTE: 'binterface.set_logininfo.execute',
  REBOOT_PLAN: 'binterface.reboot.plan',
  REBOOT_EXECUTE: 'binterface.reboot.execute',
  UPGRADE_PLAN: 'binterface.upgrade.plan',
  UPGRADE_EXECUTE: 'binterface.upgrade.execute',
  // 系统管理
  USER_READ: 'user.read', USER_CREATE: 'user.create', USER_UPDATE: 'user.update', USER_DISABLE: 'user.disable',
  ROLE_READ: 'role.read', ROLE_CREATE: 'role.create', ROLE_UPDATE: 'role.update', ROLE_DELETE: 'role.delete',
  PERMISSION_READ: 'permission.read', PERMISSION_ASSIGN: 'permission.assign',
  // 审计
  AUDIT_READ: 'audit.read',
} as const

export interface PermissionMeta {
  code: string; name: string; group: string; description?: string
  riskLevel: 'low' | 'medium' | 'high' | 'critical'
  source: 'frontend-planning' | 'backend'
}

export const PERMISSION_META_LIST: PermissionMeta[] = [
  { code:'binterface.read', name:'B接口查看', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.fsu.read', name:'FSU状态', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.realtime.read', name:'实时数据', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.alarm.read', name:'告警', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.threshold.read', name:'门限', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.ftp.read', name:'FTP参数', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.ftp_image.read', name:'FTP图片', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.scheduler.read', name:'调度配置', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.protocol_audit.read', name:'协议治理', group:'B接口查看', riskLevel:'low', source:'frontend-planning' },
  { code:'protocol:raw:view', name:'原始XML查看', group:'B接口查看', riskLevel:'high', source:'frontend-planning' },
  { code:'protocol:raw:download', name:'原始XML下载', group:'B接口查看', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.dry_run', name:'dry_run', group:'B接口只读操作', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.mock', name:'mock', group:'B接口只读操作', riskLevel:'low', source:'frontend-planning' },
  { code:'binterface.get.run', name:'GET run-once', group:'B接口只读操作', riskLevel:'medium', source:'frontend-planning' },
  { code:'protocol:runonce:readonly', name:'Run-once只读', group:'B接口只读操作', riskLevel:'medium', source:'frontend-planning' },
  { code:'scheduler.read', name:'调度查看', group:'调度', riskLevel:'low', source:'frontend-planning' },
  { code:'scheduler.manage', name:'调度管理', group:'调度', riskLevel:'medium', source:'frontend-planning' },
  { code:'binterface.set_point.plan', name:'SET_POINT 规划', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_point.execute', name:'SET_POINT 执行', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_threshold.plan', name:'SET_THRESHOLD 规划', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_threshold.execute', name:'SET_THRESHOLD 执行', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_ftp.plan', name:'SET_FTP 规划', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_ftp.execute', name:'SET_FTP 执行', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_logininfo.plan', name:'SET_LOGININFO 规划', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.set_logininfo.execute', name:'SET_LOGININFO 执行', group:'高风险操作', riskLevel:'high', source:'frontend-planning' },
  { code:'binterface.reboot.plan', name:'重启规划', group:'高风险操作', riskLevel:'critical', source:'frontend-planning' },
  { code:'binterface.reboot.execute', name:'重启执行', group:'高风险操作', riskLevel:'critical', source:'frontend-planning' },
  { code:'binterface.upgrade.plan', name:'升级规划', group:'高风险操作', riskLevel:'critical', source:'frontend-planning' },
  { code:'binterface.upgrade.execute', name:'升级执行', group:'高风险操作', riskLevel:'critical', source:'frontend-planning' },
  { code:'user.read', name:'用户查看', group:'系统管理', riskLevel:'low', source:'frontend-planning' },
  { code:'user.manage', name:'用户管理', group:'系统管理', riskLevel:'high', source:'frontend-planning' },
  { code:'role.manage', name:'角色管理', group:'系统管理', riskLevel:'high', source:'frontend-planning' },
  { code:'permission.read', name:'权限查看', group:'系统管理', riskLevel:'low', source:'frontend-planning' },
  { code:'audit.read', name:'审计查看', group:'审计', riskLevel:'low', source:'frontend-planning' },
]
