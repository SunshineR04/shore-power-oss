/**
 * 前端 API 接口封装
 *
 * 基于 Axios 的 HTTP 请求封装，自动携带 JWT Token（通过 axios 拦截器注入 Authorization 头部）
 * 所有 API 对应后端的 REST Controller 端点
 *
 * API 模块划分：
 *   authApi       - 登录注册（对应 AuthController）
 *   userApi       - 用户管理（对应 UserController）
 *   deviceApi     - 设备管理（对应 DeviceController）
 *   alarmApi      - 告警管理（对应 AlarmController）
 *   energyApi     - 能耗分析（对应 EnergyController）
 *   maintenanceApi - 维护调度（对应 MaintenanceController）
 *   reservationApi - 预约管理（对应 ReservationController）
 *   shipApi       - 船舶管理（对应 ShipController）
 *   weatherApi    - 天气查询（对应 WeatherController）
 *   systemConfigApi - 系统配置（对应 SystemConfigController）
 *   financeApi    - 营收统计（对应 FinanceController）
 */
import request from '../utils/request'

export const authApi = {
  login: data => request.post('/auth/login', data),
  register: data => request.post('/auth/register', data)
}

export const userApi = {
  info: () => request.get('/user/info'),
  page: params => request.get('/user/page', { params }),
  add: data => request.post('/user', data),
  update: data => request.put('/user', data),
  del: id => request.delete(`/user/${id}`),
  toggle: id => request.put(`/user/toggle/${id}`),
  updateProfile: data => request.put('/user/profile', data),
  changePassword: data => request.put('/user/password', data)
}

export const deviceApi = {
  page: params => request.get('/device/page', { params }),
  list: () => request.get('/device/list'),
  get: id => request.get(`/device/${id}`),
  add: data => request.post('/device', data),
  update: data => request.put('/device', data),
  del: id => request.delete(`/device/${id}`),
  statusCount: () => request.get('/device/status-count'),
  latest: id => request.get(`/device/${id}/latest`),
  latestAll: () => request.get('/device/latest-all'),
  trend: (id, hours) => request.get(`/device/${id}/trend`, { params: { hours } }),
  types: () => request.get('/device/types')
}

export const alarmApi = {
  page: params => request.get('/alarm/page', { params }),
  handle: (id, data) => request.put(`/alarm/handle/${id}`, data),
  pendingCount: () => request.get('/alarm/pending-count'),
  levelStats: () => request.get('/alarm/level-stats')
}

export const energyApi = {
  trend: params => request.get('/energy/trend', { params }),
  byDevice: params => request.get('/energy/by-device', { params }),
  comparison: params => request.get('/energy/comparison', { params }),
  environmentalMetrics: params => request.get('/energy/environmental-metrics', { params }),
  analyze: (deviceId, days) => request.get(`/energy/analyze/${deviceId}`, { params: { days } }),
  loadBalancing: () => request.get('/energy/load-balancing'),
  predict: (deviceId, days) => request.get(`/energy/predict/${deviceId}`, { params: { days } }),
  timeOfUsePrices: () => request.get('/energy/time-of-use-prices'),
  realTimePrice: () => request.get('/energy/real-time-price')
}

export const maintenanceApi = {
  page: params => request.get('/maintenance/page', { params }),
  add: data => request.post('/maintenance', data),
  update: data => request.put('/maintenance', data),
  del: id => request.delete(`/maintenance/${id}`)
}

export const reservationApi = {
  create: (deviceId, startTime, endTime, shipId) => request.post('/user/reservation/create', null, { params: { deviceId, shipId, startTime, endTime } }),
  list: () => request.get('/user/reservation/list'),
  detail: id => request.get(`/user/reservation/detail/${id}`),
  confirm: id => request.post(`/user/reservation/confirm/${id}`),
  cancel: id => request.post(`/user/reservation/cancel/${id}`),
  startUsage: id => request.post(`/user/reservation/start/${id}`),
  endUsage: id => request.post(`/user/reservation/end/${id}`),
  pay: (reservationId, method) => request.post('/user/reservation/pay', { reservationId, method }),
  payCallback: tradeNo => request.post('/user/reservation/pay-callback', null, { params: { tradeNo } }),
  usageRecords: () => request.get('/user/reservation/usage-records'),
  submitRating: (deviceId, rating, comment) => request.post('/user/reservation/rating', null, { params: { deviceId, rating, comment } }),
  getDeviceRatings: deviceId => request.get(`/user/reservation/rating/${deviceId}`)
}

export const shipApi = {
  add: data => request.post('/user/ship/add', data),
  list: () => request.get('/user/ship/list'),
  get: id => request.get(`/user/ship/${id}`),
  update: data => request.put('/user/ship/update', data),
  del: id => request.delete(`/user/ship/${id}`),
  toggle: id => request.put(`/user/ship/toggle/${id}`)
}

export const weatherApi = {
  current: () => request.get('/weather/current'),
  refresh: (location) => request.post('/weather/refresh', null, { params: { location } })
}

export const systemConfigApi = {
  list: () => request.get('/admin/config/list'),
  types: () => request.get('/admin/config/types'),
  update: data => request.put('/admin/config/update', data),
  batchUpdate: data => request.put('/admin/config/batch-update', data),
  publicConfig: () => request.get('/config/public')
}

export const financeApi = {
  summary: () => request.get('/admin/finance/summary'),
  deviceRanking: () => request.get('/admin/finance/device-ranking'),
  userRanking: () => request.get('/admin/finance/user-ranking'),
  dailyTrend: (startDate, endDate) => request.get('/admin/finance/daily-trend', { params: { startDate, endDate } })
}
