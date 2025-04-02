import axios from 'axios'
import { useUserStore } from '@/stores/user'

const axiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

// 添加请求拦截器，自动添加 token
axiosInstance.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    const token = userStore.token
    
    if (token) {
      console.log('发送请求，携带token:', token.substring(0, 20) + '...')
      config.headers['Authorization'] = `Bearer ${token}`
    } else {
      console.warn('请求未携带token')
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

export default axiosInstance 