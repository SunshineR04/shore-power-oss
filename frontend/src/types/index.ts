/** 系统核心类型定义 */

export interface User {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  role: 'ADMIN' | 'OPERATOR' | 'USER'
  status: number
  createTime: string
  updateTime: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface Device {
  id: number
  deviceCode: string
  deviceName: string
  deviceType: string
  location: string
  longitude?: number
  latitude?: number
  ratedVoltage: number
  ratedCurrent: number
  ratedPower: number
  manufacturer?: string
  installDate?: string
  status: 'ONLINE' | 'OFFLINE' | 'IN_USE' | 'MAINTENANCE' | 'FAULT'
  createTime: string
  updateTime: string
}

export interface DeviceData {
  id: number
  deviceId: number
  voltage: number
  currentVal: number
  power: number
  temperature: number
  humidity?: number
  powerFactor: number
  frequency: number
  energyConsumption: number
  energyCost?: number
  collectTime: string
}

export interface Alarm {
  id: number
  deviceId: number
  alarmType: string
  alarmLevel: 'INFO' | 'WARNING' | 'CRITICAL'
  alarmContent: string
  alarmValue?: string
  thresholdValue?: string
  status: 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'IGNORED'
  handlerId?: number
  handleTime?: string
  handleRemark?: string
  alarmTime: string
}

export interface Reservation {
  id: number
  userId: number
  deviceId: number
  shipId?: number
  reservationNo: string
  startTime: string
  endTime: string
  status: 'PENDING' | 'CONFIRMED' | 'IN_USE' | 'PENDING_PAYMENT' | 'COMPLETED' | 'CANCELLED'
  estimatedCost?: number
  actualCost?: number
  createTime: string
  updateTime: string
  deviceName?: string
  deviceCode?: string
  location?: string
  shipName?: string
}

export interface Ship {
  id: number
  userId: number
  shipName: string
  shipType: string
  mmsi?: string
  imo?: string
  nationality?: string
  tonnage?: number
  length?: number
  width?: number
  draft?: number
  status: number
  remark?: string
}

export interface DashboardStats {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  inUseDevices: number
  faultDevices: number
  totalEnergy: number
  totalCost: number
  pendingAlarms: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}
