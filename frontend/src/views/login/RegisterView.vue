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
      <h2>注册</h2>
      <form @submit.prevent="handleRegister">
        <!-- 用户名输入 -->
        <div class="form-group">
          <label>用户名</label>
          <input
              type="text"
              v-model="registerForm.username"
              required
              placeholder="请输入用户名"
          >
        </div>

        <!-- 修正邮箱输入 -->
        <div class="form-group">
          <label>邮箱</label>
          <input
              type="email"
              v-model="registerForm.email"
              required
              placeholder="请输入邮箱"
          >
        </div>

        <!-- 修正密码输入 -->
        <div class="form-group">
          <label>密码</label>
          <div class="password-input">
            <input
                :type="showPassword ? 'text' : 'password'"
                v-model="registerForm.password"
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

        <!-- 确认密码 -->
        <div class="form-group">
          <label>确认密码</label>
          <input
              type="password"
              v-model="registerForm.confirmPassword"
              required
              placeholder="请再次输入密码"
          >
        </div>

        <button type="submit" class="submit-btn">注册</button>

        <div class="auth-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
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
import '@/assets/css/login.css';
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()

const showPassword = ref(false)
const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  // 验证密码是否匹配
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  try {
    const response = await axios.post('/api/auth/register', {
      userId: registerForm.username,
      password: registerForm.password
    })
    
    ElMessage.success('注册成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data || '注册失败')
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