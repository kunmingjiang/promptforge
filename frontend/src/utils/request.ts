import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

let refreshing: Promise<boolean> | null = null

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const auth = useAuthStore()
    const config = error.config || {}
    const status = error.response?.status

    // 访问令牌过期：静默刷新一次并重放原请求，刷新失败才回登录页
    if (status === 401 && !config._retried && auth.refreshToken) {
      config._retried = true
      refreshing = refreshing || auth.tryRefresh().finally(() => (refreshing = null))
      const ok = await refreshing
      if (ok) {
        return http(config)
      }
      auth.logout()
      router.push('/login')
      return Promise.reject(error)
    }

    const message = error.response?.data?.message || '请求失败，请稍后再试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
