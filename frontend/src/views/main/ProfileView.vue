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
              <img src="@/assets/picture/school_logo.webp" alt="用户头像" class="avatar" />
            </div>
            <div class="info-row">
              <span class="label">姓名：</span>
              <span class="value">{{ userInfo.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">学号：</span>
              <span class="value">{{ userInfo.studentNumber }}</span>
            </div>
            <div class="info-row">
              <span class="label">学院：</span>
              <span class="value">{{ userInfo.college }}</span>
            </div>
            <div class="info-row">
              <span class="label">专业：</span>
              <span class="value">{{ userInfo.major }}</span>
            </div>
            <div class="info-row">
              <span class="label">联系电话：</span>
              <span class="value">{{ userInfo.phone }}</span>
            </div>
            <div class="info-row">
              <span class="label">邮箱：</span>
              <span class="value">{{ userInfo.email }}</span>
            </div>
          </div>

          <!-- 修改个人信息 -->
          <button class="edit-btn" @click="editProfile">修改信息</button>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted} from "vue";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import router from "@/router";

// 用户信息
const userInfo = ref({
  name: "张三",
  studentNumber: "20211001",
  college: "计算机学院",
  major: "软件工程",
  phone: "13888888888",
  email: "zhangsan@example.com",
  avatar: "https://via.placeholder.com/100" // 默认头像
});

// 跳转到修改个人信息页面
const editProfile = () => {
  router.push("/profile/edit");
};

// 模拟从后端获取数据
onMounted(() => {
  // 假设这里调用API获取个人信息
  console.log("加载用户信息...");
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
  object-fit: cover;
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

/* 修改按钮 */
.edit-btn {
  width: 200px;
  margin: 32px auto 0;
  padding: 12px 24px;
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: block;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.edit-btn:active {
  transform: translateY(0);
}
</style>

