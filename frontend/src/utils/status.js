/**
 * 共享状态常量与映射
 *
 * 各页面统一从这里取状态文案与 el-tag type，
 * 避免每个页面各自维护一份 statusMap/statusType。
 */

/** 设备状态：key → { label 文案, type el-tag 类型, dot 指示点色 } */
export const DEVICE_STATUS = {
  ONLINE:      { label: '在线',   type: 'primary', dot: '#2563eb' },
  IN_USE:      { label: '使用中', type: 'primary', dot: '#2563eb' },
  OFFLINE:     { label: '离线',   type: 'info',    dot: '#64748b' },
  FAULT:       { label: '故障',   type: 'danger',  dot: '#dc2626' },
  MAINTENANCE: { label: '维护中', type: 'warning', dot: '#d97706' }
}

/** 维护任务状态 */
export const TASK_STATUS = {
  PENDING:     { label: '待处理', type: 'info',    dot: '#64748b' },
  ASSIGNED:    { label: '已指派', type: 'warning', dot: '#d97706' },
  IN_PROGRESS: { label: '进行中', type: 'primary', dot: '#2563eb' },
  COMPLETED:   { label: '已完成', type: 'success', dot: '#16a34a' },
  CANCELLED:   { label: '已取消', type: 'info',    dot: '#94a3b8' }
}

/** 告警状态 */
export const ALARM_STATUS = {
  PENDING:   { label: '待处理', type: 'danger',  dot: '#dc2626' },
  PROCESSING:{ label: '处理中', type: 'warning', dot: '#d97706' },
  RESOLVED:  { label: '已解决', type: 'success', dot: '#16a34a' },
  IGNORED:   { label: '已忽略', type: 'info',    dot: '#94a3b8' }
}

/** 取状态元信息，未知状态兜底为 info 灰 */
export function statusMeta(map, key) {
  return (key && map[key]) || { label: key || '未知', type: 'info', dot: '#94a3b8' }
}
