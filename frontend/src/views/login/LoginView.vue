<template>
  <!-- 学校名称图片 -->
  <img src="../../assets/picture/logo.png" alt="学校名称" class="school-logo">

  <div class="global-nav">
    <router-link to="/help">帮助</router-link>
    <router-link to="/feedback">反馈</router-link>
    <router-link to="/updates">更新公告</router-link>
  </div>

  <div class="login-page">
    <div class="login-container">
      <h2>登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>学号/工号</label>
          <input
              type="text"
              v-model="loginForm.username"
              required
              placeholder="请输入学号/工号"
          >
        </div>

        <div class="form-group">
          <label>密码</label>
          <div class="password-input">
            <input
                :type="showPassword ? 'text' : 'password'"
                v-model="loginForm.password"
                required
                placeholder="请输入密码"
            >
            <button
                type="button"
                class="toggle-password"
                @click="togglePassword"
            >
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="auth-link">
          没有账号？<router-link to="/register">立即注册</router-link>
        </div>
        <div class="auth-link">
          忘记密码？<router-link to="/forgot-password">找回密码</router-link>
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </form>
    </div>
  </div>

  <div class="footer-links">
    <div class="href">
      <a href="https://www.dlmu.edu.cn" target="_blank">大连海事大学官网</a>
      <span>|</span>
      <a href="https://id.dlmu.edu.cn/login" target="_blank">统一身份认证</a>
    </div>
    <div>大连海事大学 版权所有</div>
  </div>
</template>

<script setup>
import '@/assets/css/login.css'
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const showPassword = ref(false)
const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const loginForm = reactive({
  username: '',
  password: ''
})

const loading = ref(false)
const errorMessage = ref(null)

const handleLogin = async () => {
  try {
    loading.value = true;
    
    const response = await axios.post('/api/auth/login', {
      userId: loginForm.username,
      password: loginForm.password
    });
    
    console.log('Login response:', response.data); // 添加登录响应调试
    
    const { token, userId, name, roleLevel } = response.data;
    
    // 使用 userStore
    userStore.setUserInfo({ token, userId, name, roleLevel });
    
    // 同时单独存储 token，确保其他使用方式可以找到
    localStorage.setItem('token', token);
    
    ElMessage.success('登录成功');
    router.push('/home');
  } catch (error) {
    console.error('Login error:', error.response?.data);
    ElMessage.error(error.response?.data?.message || error.response?.data || '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  margin: 0 20px;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}
</style>