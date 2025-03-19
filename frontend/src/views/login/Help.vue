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
      <h2>帮助中心</h2>
      <div class="help-content">
        <section>
          <h3>🔑 登录相关问题</h3>
          <p>如果您在登录时遇到问题，请参考以下指南：</p>
          <ul>
            <li>🔹 确保输入的学号和密码正确。</li>
            <li>🔹 忘记密码？请点击 <router-link to="/forgot-password">找回密码</router-link>。</li>
            <li>🔹 账户未注册？请点击 <router-link to="/register">注册</router-link>。</li>
            <li>🔹 仍然无法登录？请联系管理员 <a href="mailto:support@dlmu.edu.cn">support@dlmu.edu.cn</a>。</li>
          </ul>
        </section>

        <section>
          <h3>📌 系统简介</h3>
          <ul>
            <li>本系统主要负责学校综合积分测评工作。可极大的减少各个中队相关测评人员的负担与分数出现误差的可能性。</li>
            <li>本系统主要功能是方便广大学生进行综合测评材料的上传；以及便于综测小组成员对于不同的加分项目进行统计，随后对于不同的加分项进行加分；管理员对应各种对的综测组组长以及副组长，他们有权对于系统内部的加分条例进行修改。</li>
            <li>若有其他未能解决的问题，欢迎致电教务处或者管理员咨询。</li>
          </ul>
        </section>
      </div>
      
      <button class="back-btn" @click="goToLogin">返回登录</button>
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
import { useRouter } from 'vue-router'
import '@/assets/css/login.css'  // 添加引入全局样式

const router = useRouter()

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

h3 {
  color: #333;
  margin: 20px 0 15px;
}

.help-content {
  max-height: calc(70vh - 60px);
  overflow-y: auto;
  padding-right: 20px;
  margin-bottom: 20px;
}

.help-container {
  max-width: 600px;
  margin: 50px auto;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

h1 {
  text-align: center;
  color: #333;
}

h2 {
  margin-top: 20px;
  color: #555;
}

ul {
  padding-left: 20px;
  margin: 10px 0;
}

li {
  margin-bottom: 10px;
  line-height: 1.6;
}

/* 内容区域的链接样式 */
.help-content a {
  color: #007bff;
  text-decoration: none;
}

.help-content a:hover {
  text-decoration: underline;
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
