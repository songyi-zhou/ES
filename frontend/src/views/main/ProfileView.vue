<template>
  <div class="profile-container">
    <TopBar/>
    <div class="content">
      <Sidebar/>
      <main class="main-content">
        <section class="profile-card">
          <h2>个人信息</h2>
          <div class="profile-info">
            <div class="avatar-container">
              <img :src="avatarUrl" alt="用户头像" class="avatar" />
            </div>
            <div class="info-row">
              <span class="label">姓名：</span>
              <span class="value">{{ userInfo.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">学号：</span>
              <span class="value">{{ userInfo.userId }}</span>
            </div>
            <div class="info-row">
              <span class="label">学院：</span>
              <span class="value">{{ userInfo.department }}</span>
            </div>
            <div class="info-row">
              <span class="label">专业：</span>
              <span class="value">{{ userInfo.major }}</span>
            </div>
            <div class="info-row">
              <span class="label">班级：</span>
              <span class="value">{{ userInfo.className }}</span>
            </div>
            <div class="info-row">
              <span class="label">中队：</span>
              <span class="value">{{ userInfo.squad }}</span>
            </div>
            <div class="info-row">
              <span class="label">联系电话：</span>
              <span class="value">{{ userInfo.phone || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="label">邮箱：</span>
              <span class="value">{{ userInfo.email || '未设置' }}</span>
            </div>
          </div>

          <div class="button-group">
            <button class="edit-btn" @click="editProfile">修改信息</button>
            <button class="change-password-btn" @click="changePassword">修改密码</button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import router from "@/router";
import axios from "axios";
import { ElMessage } from "element-plus";
import defaultAvatar from '@/assets/picture/school_logo.webp';
import { loadImage, getImageUrl } from '@/utils/imageLoader';

// 用户信息
const userInfo = ref({
  name: "",
  userId: "",
  department: "",
  major: "",
  className: "",
  squad: "",
  phone: "",
  email: "",
  avatar: ""
});

// 头像URL
const avatarUrl = ref(defaultAvatar);

// 加载头像
const loadAvatar = async () => {
  if (userInfo.value.avatar) {
    const url = getImageUrl(userInfo.value.avatar);
    const imageUrl = await loadImage(url);
    if (imageUrl) {
      avatarUrl.value = imageUrl;
    }
  }
};

// 加载状态
const isLoading = ref(true);

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    isLoading.value = true;
    const response = await axios.get('/api/profile/info');
    if (response.data.success) {
      userInfo.value = response.data.data;
      await loadAvatar(); // 加载头像
    } else {
      ElMessage.error(response.data.message || '获取用户信息失败');
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    ElMessage.error('获取用户信息失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
};

// 跳转到修改个人信息页面
const editProfile = () => {
  router.push("/profile/edit");
};

// 跳转到修改密码页面
const changePassword = () => {
  router.push("/profile/change-password");
};

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
.profile-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
  background: #f0f2f5;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 个人信息卡片 */
.profile-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 标题 */
.profile-card h2 {
  margin-bottom: 32px;
  text-align: center;
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 600;
}

/* 头像容器 */
.avatar-container {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

/* 头像 */
.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s;
}

.avatar:hover {
  transform: scale(1.05);
}

/* 信息行 */
.info-row {
  display: flex;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.info-row:hover {
  background: #eef2ff;
  transform: translateX(4px);
}

/* 标签 */
.label {
  min-width: 100px;
  font-weight: 500;
  color: #666;
}

/* 值 */
.value {
  flex: 1;
  color: #1a1a1a;
  font-size: 15px;
}

/* 按钮组 */
.button-group {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

/* 修改按钮 */
.edit-btn,
.change-password-btn {
  width: 200px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.edit-btn {
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  color: white;
}

.change-password-btn {
  background: linear-gradient(135deg, #67c23a, #5daf34);
  color: white;
}

.edit-btn:hover,
.change-password-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.edit-btn:active,
.change-password-btn:active {
  transform: translateY(0);
}
</style>

