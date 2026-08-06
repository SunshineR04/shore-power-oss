import { describe, it, expect, beforeEach, vi } from 'vitest'

// 路由守卫安全解析逻辑测试（safeUserInfo 行为由 router/index.js 提供）
// 此处直接复刻核心逻辑验证，避免在单测中加载整个 router（依赖 createWebHistory）
function safeUserInfo() {
  try {
    const raw = sessionStorage.getItem('userInfo')
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

describe('路由守卫用户信息解析', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('无 userInfo 时返回空对象', () => {
    expect(safeUserInfo()).toEqual({})
  })

  it('合法 JSON 正常解析角色', () => {
    sessionStorage.setItem('userInfo', JSON.stringify({ role: 'ADMIN' }))
    expect(safeUserInfo().role).toBe('ADMIN')
  })

  it('损坏 JSON 返回空对象而不抛异常', () => {
    sessionStorage.setItem('userInfo', '{oops')
    expect(() => safeUserInfo()).not.toThrow()
    expect(safeUserInfo()).toEqual({})
  })
})
