// 通用类型定义

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// ========== 资源管理 ==========
export interface Site {
  id?: number
  siteCode: string
  siteName: string
  siteType?: string
  region?: string
  address?: string
  longitude?: number
  latitude?: number
  contactPerson?: string
  contactPhone?: string
  status: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface Cabinet {
  id?: number
  siteId: number
  cabinetCode: string
  cabinetName: string
  cabinetType?: string
  model?: string
  manufacturer?: string
  installDate?: string
  status: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface FsuDevice {
  id?: number
  siteId: number
  cabinetId?: number
  fsuCode: string
  fsuName: string
  fsuType?: string
  model?: string
  manufacturer?: string
  firmwareVersion?: string
  ipAddr?: string
  port?: number
  macAddr?: string
  protocolVersion?: string
  status: string
  registerTime?: string
  lastOnlineTime?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface MonitoringPoint {
  id?: number
  fsuId: number
  cabinetId?: number
  pointCode: string
  pointName: string
  pointType: string
  dataType?: string
  unit?: string
  valueRange?: string
  precisionVal?: number
  alarmUpper?: number
  alarmLower?: number
  alarmUpperUrgent?: number
  alarmLowerUrgent?: number
  pollingInterval?: number
  status: string
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

// ========== 数据遥测 ==========
export interface RealtimeData {
  id?: number
  fsuId: number
  pointId: number
  pointCode: string
  valueText?: string
  valueNumber?: number
  valueStatus?: string
  quality?: string
  collectTime: string
  receiveTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface HistoryData {
  id?: number
  fsuId: number
  pointId: number
  pointCode: string
  valueText?: string
  valueNumber?: number
  valueStatus?: string
  quality?: string
  collectTime: string
  receiveTime?: string
  sourceMessageId?: number
  createdAt?: string
}

export interface DeviceHeartbeat {
  id?: number
  fsuId: number
  fsuCode: string
  heartbeatTime: string
  statusInfo?: string
  sourceMessageId?: number
  createdAt?: string
}

// ========== 告警管理 ==========
export interface AlarmRecord {
  id?: number
  fsuId: number
  pointId?: number
  pointCode?: string
  alarmCode?: string
  alarmName?: string
  alarmLevel: string
  alarmStatus: string
  alarmValue?: string
  alarmDesc?: string
  occurTime: string
  confirmTime?: string
  clearTime?: string
  confirmUserId?: number
  clearedBy?: string
  sourceMessageId?: number
  createdAt?: string
  updatedAt?: string
}

// ========== B接口 ==========
export interface BInterfaceCommand {
  id?: number
  commandCode: string
  commandName: string
  direction: string
  category?: string
  description?: string
  implemented: boolean
  safeEnabled: boolean
  priority?: string
  createdAt?: string
  updatedAt?: string
}

export interface BInterfaceMessageLog {
  id?: number
  direction: string
  commandCode: string
  fsuId?: number
  fsuCode?: string
  sessionId?: string
  messageType: string
  pkType?: string
  info?: string
  xmlData?: string
  rawMessage?: string
  status?: string
  errorMessage?: string
  processTimeMs?: number
  createdAt?: string
}

export interface BInterfaceSession {
  id?: number
  fsuId: number
  fsuCode: string
  sessionId: string
  authToken?: string
  loginTime: string
  lastActiveTime: string
  logoutTime?: string
  status: string
  remoteAddr?: string
  expireSeconds?: number
  createdAt?: string
  updatedAt?: string
}

export interface BInterfaceFsuStatus {
  id?: number
  fsuId: number
  fsuCode: string
  loginStatus: string
  onlineStatus: string
  lastLoginTime?: string
  lastLogoutTime?: string
  lastHeartbeat?: string
  heartbeatMissCount?: number
  sessionId?: string
  statusDetail?: string
  createdAt?: string
  updatedAt?: string
}

export interface BInterfaceCallRecord {
  id?: number
  fsuId: number
  fsuCode: string
  commandCode: string
  callType: string
  requestBody?: string
  responseBody?: string
  status: string
  errorMessage?: string
  durationMs?: number
  retryCount?: number
  callTime: string
  sourceMessageId?: number
  createdAt?: string
}

export interface FtpTransferRecord {
  id?: number
  fsuId?: number
  fsuCode?: string
  fileName: string
  fileType: string
  fileSize?: number
  remotePath?: string
  localPath?: string
  direction: string
  transferStatus: string
  checksum?: string
  errorMessage?: string
  transferTime?: string
  completedTime?: string
  createdAt?: string
}

// ========== 系统管理 ==========
export interface UserAccount {
  id?: number
  username: string
  passwordHash?: string
  displayName?: string
  email?: string
  phone?: string
  status: string
  lastLoginTime?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface Role {
  id?: number
  roleCode: string
  roleName: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

// ========== 健康检查 ==========
export interface HealthInfo {
  service: string
  status: string
  timestamp: number
}

export interface BInterfaceHealthInfo {
  bInterfaceVersion: string
  scServiceStatus: string
  fsuServiceStatus: string
  soapEnabled: boolean
}
