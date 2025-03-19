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
      <h2>找回密码</h2>
      <p>请选择找回方式：</p>

      <div class="tab-selection">
        <button :class="{ active: method === 'email' }" @click="method = 'email'">邮箱</button>
        <button :class="{ active: method === 'phone' }" @click="method = 'phone'">手机</button>
        <button :class="{ active: method === 'wechat' }" @click="method = 'wechat'">微信</button>
      </div>

      <!-- 邮箱找回 -->
      <form v-if="method === 'email'" @submit.prevent="sendVerificationCode('email')">
        <div class="form-group">
          <label>邮箱</label>
          <input type="email" v-model="email" required placeholder="请输入您的邮箱">
        </div>
        <button type="submit" class="submit-btn">发送验证码</button>
      </form>

      <!-- 手机找回 -->
      <form v-if="method === 'phone'" @submit.prevent="sendVerificationCode('phone')">
        <div class="form-group">
          <label>手机号</label>
          <input type="tel" v-model="phone" required placeholder="请输入您的手机号">
        </div>
        <button type="submit" class="submit-btn">发送验证码</button>
      </form>

      <!-- 微信找回 -->
      <div v-if="method === 'wechat'" class="wechat-recover">
        <p>请使用微信扫描以下二维码，进行找回密码：</p>
        <img src="../../assets/picture/school_logo.webp" alt="微信找回二维码" class="wechat-qr">
      </div>

      <!-- 输入验证码和重置密码 -->
      <form v-if="isCodeSent && method !== 'wechat'" @submit.prevent="resetPassword">
        <div class="form-group">
          <label>验证码</label>
          <input type="text" v-model="verificationCode" required placeholder="请输入验证码">
        </div>

        <div class="form-group">
          <label>新密码</label>
          <input :type="showPassword ? 'text' : 'password'" v-model="newPassword" required placeholder="请输入新密码">
        </div>

        <div class="form-group">
          <label>确认新密码</label>
          <input type="password" v-model="confirmPassword" required placeholder="请再次输入新密码">
        </div>

        <button type="submit" class="submit-btn">重置密码</button>
      </form>

      <div class="auth-link">
        <router-link to="/login">返回登录</router-link>
      </div>
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
import {ref} from 'vue';
// import '@/assets/css/forgot-password.css';

const method = ref('email');  // 默认方式：邮箱
const email = ref('');
const phone = ref('');
const verificationCode = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const isCodeSent = ref(false);
const showPassword = ref(false);

const sendVerificationCode = (type) => {
  if (type === 'email') {
    console.log('验证码已发送到邮箱:', email.value);
  } else if (type === 'phone') {
    console.log('验证码已发送到手机:', phone.value);
  }
  isCodeSent.value = true;
};

const resetPassword = () => {
  if (newPassword.value !== confirmPassword.value) {
    alert('两次输入的密码不一致');
    return;
  }

  console.log('密码已重置:', newPassword.value);
  alert('密码重置成功，请返回登录页面');
};
</script>

<style scoped>
.login-container {
  background: white;
  padding: 50px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 600px;
  margin: 0 20px;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
}

.forgot-password-container {
  min-width: 800px;
  max-width: 1600px;
  margin: 50px auto;
  padding: 20px;
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  background-color: #fff;
}

.tab-selection {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin: 30px 0;
}

.tab-selection button {
  padding: 12px 30px;
  border: 1px solid #ccc;
  background-color: #f8f8f8;
  cursor: pointer;
  border-radius: 6px;
  font-size: 16px;
  transition: all 0.3s;
}

.tab-selection .active {
  background-color: #007bff;
  color: white;
}

.form-group {
  margin: 25px 0;
}

input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 16px;
  margin-top: 8px;
}

.submit-btn {
  width: 60%;
  padding: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  margin: 30px auto;
  display: block;
}

.submit-btn:hover {
  background-color: #0056b3;
}

.auth-link {
  margin-top: 25px;
  font-size: 16px;
}

.auth-link a {
  color: #007bff;
  text-decoration: none;
}

.wechat-recover {
  text-align: center;
  margin: 30px 0;
}

.wechat-qr {
  width: 200px;
  margin: 20px 0;
}
</style>
