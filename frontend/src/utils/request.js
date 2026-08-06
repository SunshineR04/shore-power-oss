import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

let isRedirecting = false

request.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    if (res.data.code === 200) {
      return res.data
    }
    ElMessage.error(res.data.msg || '请求失败')
    return Promise.reject(res.data)
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.msg || err.message || '网络异常'
    if (status === 401 && !isRedirecting) {
      // 登录过期：清理会话并跳转登录
      isRedirecting = true
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userInfo')
      router.push('/login').finally(() => { isRedirecting = false })
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error(msg === 'Request failed with status code 403' ? '权限不足' : msg)
    } else if (!status || status >= 500) {
      // 网络错误/网关错误（断线轮询、后端重启等）：静默，不弹窗刷屏
      // 页面可通过 promise 拒绝自行处理（如显示"已断开"状态）
    } else if (!isRedirecting) {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default request
