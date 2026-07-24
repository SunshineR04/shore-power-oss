import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || '{}'))

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
