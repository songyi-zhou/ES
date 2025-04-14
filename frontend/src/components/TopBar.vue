<template>
  <header class="top-bar">
    <div class="logo-container">
      <img src="@/assets/picture/logo.png" alt="校标" class="school-logo-home" />
      <span class="system-title">综合测评管理系统</span>
    </div>
    <div class="user-info">
      <span class="user-name">欢迎，{{ userName }}</span>
      <button class="logout-btn" @click="logout">登出</button>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from '@/stores/user'
import axios from 'axios'

const router = useRouter();
const userStore = useUserStore();
const userName = ref("");

onMounted(() => {
  userName.value = userStore.name || "未知用户";
})

const logout = () => {
  // 清除用户信息
  userStore.clearUserInfo()
  // 清除 axios 默认 header
  delete axios.defaults.headers.common['Authorization']
  // 跳转到登录页
  router.push('/login')
}
</script>

<style scoped>
.top-bar {
  height: 100px;
  background: #003366;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #002244;
}

.logo-container {
  display: flex;
  align-items: center;
}

.school-logo-home {
  height: 50px;
  margin-right: 10px;
}

.system-title {
  font-size: 40px;
  font-weight: bold;
  margin-left: 30px;
  color: white;
  font-family: "华文行楷", "STXingkai", "Microsoft YaHei", sans-serif; /* 添加华文行楷字体 */
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.user-name {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
}

.logout-btn {
  margin-left: 20px;
  background: #ff4d4f;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s ease;
}
</style>
