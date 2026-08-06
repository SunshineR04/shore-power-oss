import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 安全读取 sessionStorage 中的 JSON，解析失败时返回空对象（并清理脏数据） */
function safeReadUserInfo() {
  try {
    const raw = sessionStorage.getItem('userInfo')
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    sessionStorage.removeItem('userInfo')
    sessionStorage.removeItem('token')
    return {}
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(safeReadUserInfo())

  function setLogin(data) {
    token.value = data.token
    userInfo.value = { ...data, token: undefined }
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  const isAdmin = () => userInfo.value.role === 'ADMIN' || userInfo.value.role === 'OPERATOR'
  const isSuperAdmin = () => userInfo.value.role === 'ADMIN'
  const isOperator = () => userInfo.value.role === 'OPERATOR'

  return { token, userInfo, setLogin, logout, isAdmin, isSuperAdmin, isOperator }
})
