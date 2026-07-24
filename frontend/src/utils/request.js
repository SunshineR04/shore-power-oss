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
    if ((err.response?.status === 401 || err.response?.status === 403) && !isRedirecting) {
      isRedirecting = true
      sessionStorage.removeItem('token')
      router.push('/login').finally(() => { isRedirecting = false })
      ElMessage.error('登录已过期，请重新登录')
    } else if (!isRedirecting) {
      ElMessage.error(err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default request
