import { describe, it, expect } from 'vitest'
import {
  getStatusText, getStatusType, formatCost,
  RESERVATION_STATUS, RESERVATION_STATUS_TYPE
} from './reservation-utils'

describe('reservation-utils（预约页纯函数）', () => {
  it('状态文案映射完整', () => {
    expect(RESERVATION_STATUS.PENDING).toBe('待确认')
    expect(RESERVATION_STATUS.CONFIRMED).toBe('已确认')
    expect(RESERVATION_STATUS.IN_USE).toBe('使用中')
    expect(RESERVATION_STATUS.PENDING_PAYMENT).toBe('待支付')
    expect(RESERVATION_STATUS.COMPLETED).toBe('已完成')
    expect(RESERVATION_STATUS.CANCELLED).toBe('已取消')
  })

  it('未知状态原样返回且类型回退 info', () => {
    expect(getStatusText('WEIRD')).toBe('WEIRD')
    expect(getStatusText()).toBe('未知')
    expect(getStatusType('WEIRD')).toBe('info')
    expect(getStatusType()).toBe('info')
  })

  it('所有已知状态均有对应 tag 类型', () => {
    for (const status of Object.keys(RESERVATION_STATUS)) {
      expect(RESERVATION_STATUS_TYPE[status]).toBeTruthy()
    }
  })

  it('formatCost 对空值返回 0.00，数值保留两位小数', () => {
    expect(formatCost(null)).toBe('0.00')
    expect(formatCost(undefined)).toBe('0.00')
    expect(formatCost(12.345)).toBe('12.35')
    expect(formatCost('8.9')).toBe('8.90')
  })
})
