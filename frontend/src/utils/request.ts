import axios from 'axios';
import { useUserStore } from '@/stores/user';

const request = axios.create({
  baseURL: '/api',
  timeout: 300000  // 增加到5分钟
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore();
    const token = userStore.token;
    if (token) {
      // 打印完整请求信息
      console.log('Making request:', {
        url: config.url,
        method: config.method,
        params: config.params,
        token: token.substring(0, 20) + '...' // 只打印token前20位
      });
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// 添加响应拦截器
request.interceptors.response.use(
  response => {
    // 打印响应数据结构
    console.log('Response structure:', {
      status: response.status,
      headers: response.headers,
      data: response.data
    });
    return response;
  },
  error => {
    console.error('Response error:', error.response?.data);
    return Promise.reject(error);
  }
);

export default request; 