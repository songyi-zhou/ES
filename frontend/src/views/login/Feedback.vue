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
      <h2>问题反馈</h2>
      <form @submit.prevent="submitFeedback">
        <!-- 问题类型 -->
        <div class="form-group">
          <label>问题类型</label>
          <select v-model="feedback.type" required>
            <option value="bug">功能异常</option>
            <option value="suggestion">功能建议</option>
            <option value="other">其他</option>
          </select>
        </div>

        <!-- 问题描述 -->
        <div class="form-group">
          <label>问题描述</label>
          <textarea
              v-model="feedback.description"
              required
              placeholder="请详细描述您的问题..."
          ></textarea>
        </div>

        <!-- 联系方式（可选） -->
        <div class="form-group">
          <label>联系方式（可选）</label>
          <input
              type="email"
              v-model="feedback.email"
              placeholder="请输入您的邮箱（可选）"
          >
        </div>

        <!-- 按钮组 -->
        <div class="button-group">
          <button type="submit" class="submit-btn">提交反馈</button>
          <button type="button" class="return-btn" @click="goToLogin">返回登录</button>
        </div>
      </form>

      <!-- 提交成功提示 -->
      <div v-if="successMessage" class="success-message">
        {{ successMessage }}
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const feedback = reactive({
  type: '',
  description: '',
  email: ''
})

const successMessage = ref('')

const submitFeedback = () => {
  if (!feedback.type || !feedback.description) {
    successMessage.value = '请填写所有必填项'
    return
  }

  // 模拟提交
  console.log('提交的反馈:', feedback)

  // 成功提示
  successMessage.value = '反馈提交成功！感谢您的反馈！'

  // 清空输入
  feedback.type = ''
  feedback.description = ''
  feedback.email = ''
}

// 返回登录页面
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
  max-width: 600px;
  margin: 0 20px;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

select, input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

textarea {
  width: 100%;
  padding: 10px; /* 增大内部输入间距 */
  border: 1px solid #ccc;
  border-radius: 5px;
  height: 100px;
  resize: none;
  box-sizing: border-box; /* 确保宽高不会因 padding 变化 */
  line-height: 1.5; /* 调整行间距，提升可读性 */
}

.button-group {
  display: flex;
  justify-content: center;
  gap: 30px;
}

.submit-btn, .return-btn {
  width: 40%;
  margin-top: 10px;
  padding: 10px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  text-align: center;
}

.submit-btn {
  background-color: #007bff;
  color: #fff;
}

.submit-btn:hover {
  background-color: #0056b3;
}

.return-btn {
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #fafbfc;
  color: #007bff;
}

.return-btn:hover {
  background-color: #dadada;
}

.success-message {
  margin-top: 15px;
  padding: 10px;
  background-color: #d4edda;
  color: #155724;
  border-radius: 5px;
  text-align: center;
}
</style>
