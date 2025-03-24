import axios from 'axios';
import { useUserStore } from '@/stores/user';

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore();
    const token = userStore.token;
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

export default request; 