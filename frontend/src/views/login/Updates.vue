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
      <h2>系统更新公告</h2>
      <div class="updates-content">
        <div v-if="announcements.length > 0">
          <div
              v-for="(announcement, index) in announcements"
              :key="index"
              class="announcement-item"
          >
            <div class="announcement-header" @click="toggleDetails(index)">
              <h3>{{ announcement.title }}</h3>
              <span>{{ announcement.date }}</span>
            </div>
            <p v-if="expandedIndex === index" class="announcement-content">
              {{ announcement.content }}
            </p>
          </div>
        </div>

        <div v-else class="no-updates">暂无更新公告</div>

        <!-- 返回按钮 -->
        <button class="back-btn" @click="goToLogin">返回登录</button>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 示例公告数据
const announcements = ref([
  { title: "系统维护公告", date: "2025-03-08", content: "系统将在本周六凌晨2:00-4:00进行维护，请合理安排使用时间。" },
  { title: "新功能上线", date: "2025-03-06", content: "我们新增了找回密码功能，支持手机号和微信找回，欢迎体验！" },
  { title: "用户体验优化", date: "2025-03-02", content: "优化了登录界面，提高了页面响应速度，修复了一些已知问题。" }
])

const expandedIndex = ref(null)

const toggleDetails = (index) => {
  expandedIndex.value = expandedIndex.value === index ? null : index
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.login-container {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 800px;
  margin: 0 20px;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.updates-content {
  max-height: calc(70vh - 60px);
  overflow-y: auto;
  padding-right: 20px;
  margin-bottom: 20px;
}

.announcement-item {
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 15px;
  overflow: hidden;
}

.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  cursor: pointer;
  transition: background-color 0.3s;
}

.announcement-header:hover {
  background: #e9ecef;
}

.announcement-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.announcement-header span {
  font-size: 14px;
  color: #666;
}

.announcement-content {
  padding: 15px;
  background: white;
  margin: 0;
  border-top: 1px solid #eee;
  line-height: 1.6;
}

.no-updates {
  text-align: center;
  color: #666;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.back-btn {
  display: block;
  width: 200px;
  padding: 12px;
  margin: 20px auto 0;
  text-align: center;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.back-btn:hover {
  background: #0056b3;
}
</style>
