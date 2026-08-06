import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from './user'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('损坏的 userInfo JSON 解析为空对象并清理会话', () => {
    sessionStorage.setItem('token', 'abc')
    sessionStorage.setItem('userInfo', '{invalid json')

    const store = useUserStore()

    expect(store.userInfo).toEqual({})
    expect(sessionStorage.getItem('token')).toBeNull()
  })

  it('setLogin 持久化 token 与用户信息', () => {
    const store = useUserStore()
    store.setLogin({ token: 'jwt-1', userId: 1, username: 'admin', role: 'ADMIN' })

    expect(store.token).toBe('jwt-1')
    expect(store.userInfo.role).toBe('ADMIN')
    expect(sessionStorage.getItem('token')).toBe('jwt-1')
    const saved = JSON.parse(sessionStorage.getItem('userInfo'))
    expect(saved.role).toBe('ADMIN')
  })

  it('logout 清理所有会话状态', () => {
    const store = useUserStore()
    store.setLogin({ token: 'jwt-1', userId: 1, username: 'admin', role: 'ADMIN' })
    store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toEqual({})
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('userInfo')).toBeNull()
  })
})
