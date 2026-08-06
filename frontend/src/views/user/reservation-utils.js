/**
 * 预约页面通用纯函数（状态文案/类型、金额格式化），便于单元测试。
 */

export const RESERVATION_STATUS = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  IN_USE: '使用中',
  PENDING_PAYMENT: '待支付',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

export const RESERVATION_STATUS_TYPE = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  IN_USE: 'success',
  PENDING_PAYMENT: 'danger',
  COMPLETED: 'info',
  CANCELLED: 'info'
}

/** 状态 → 展示文案（未知状态原样返回） */
export function getStatusText(status) {
  return RESERVATION_STATUS[status] || status || '未知'
}

/** 状态 → Element Plus tag 类型（未知状态回退 info） */
export function getStatusType(status) {
  return RESERVATION_STATUS_TYPE[status] || 'info'
}

/** 金额格式化：null/undefined → 0.00 */
export function formatCost(val) {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}
