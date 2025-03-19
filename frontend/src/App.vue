<template>
  <router-view/>
</template>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', sans-serif;
  min-height: 100vh;
}

#app {
  width: 100%;
  min-height: 100vh;
}

/* 登录页面样式 */
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(30,144,255,0.2), rgba(0,77,153,0.8)),
              url('@/assets/picture/login_background.jpg') no-repeat center center/cover;
}

/* 主页面样式 */
.main-layout {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  #app {
    padding: 0;
  }
}
</style>

<script setup>
import { onMounted } from 'vue'
import axios from 'axios'

const token = localStorage.getItem('token')
if (token) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
}
</script>