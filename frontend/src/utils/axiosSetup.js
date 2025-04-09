import axios from 'axios';
import router from '@/router';

// 添加请求拦截器
axios.interceptors.request.use(
  (config) => {
    // 尝试多种方式获取 token
    let token = null;
    
    // 1. 直接从 localStorage 或 sessionStorage 获取 token
    token = localStorage.getItem('token') || sessionStorage.getItem('token');
    
    // 2. 从 userInfo 对象中获取 token
    if (!token) {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          token = userInfo.token;
        } catch (e) {
          console.error('解析 userInfo 失败:', e);
        }
      }
    }
    
    // 3. 从 Pinia 持久化存储中获取
    if (!token) {
      try {
        const piniaState = localStorage.getItem('user');
        if (piniaState) {
          const userState = JSON.parse(piniaState);
          token = userState.token;
        }
      } catch (e) {
        console.error('解析 Pinia 状态失败:', e);
      }
    }
    
    // 如果 token 存在且请求头不包含 Authorization 字段，则添加
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('添加授权头:', token.substring(0, 10) + '...');
    } else if (!token) {
      console.warn('未找到授权令牌');
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 添加响应拦截器
axios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      const { status } = error.response;
      
      if (status === 401 || status === 403) {
        console.error('授权失败或访问被拒绝，需要重新登录');
        
        // 清除所有可能存储 token 的地方
        localStorage.removeItem('token');
        sessionStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        localStorage.removeItem('user');  // Pinia 持久化存储
        
        // 如果不是在登录页面，重定向到登录页面
        if (router.currentRoute.value.path !== '/login') {
          alert('登录已过期或没有权限，请重新登录');
          router.push('/login');
        }
      }
    }
    
    return Promise.reject(error);
  }
);

export default axios; 