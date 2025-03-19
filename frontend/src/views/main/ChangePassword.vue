<template>
  <div class="change-password-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <section class="password-card">
          <h2>修改密码</h2>
          <form @submit.prevent="changePassword">
            <label>旧密码：</label>
            <input v-model="oldPassword" type="password" required />

            <label>新密码：</label>
            <input v-model="newPassword" type="password" required />

            <label>确认新密码：</label>
            <input v-model="confirmPassword" type="password" required />

            <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

            <button type="submit" class="save-btn">确认修改</button>
          </form>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import router from "@/router";

// 密码字段
const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const errorMessage = ref("");

// 修改密码逻辑
const changePassword = () => {
  if (!oldPassword.value) {
    errorMessage.value = "旧密码不能为空";
    return;
  }

  if (newPassword.value.length < 6) {
    errorMessage.value = "新密码长度不能小于 6 位";
    return;
  }

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = "两次输入的新密码不一致";
    return;
  }

  // TODO: 调用后端 API 修改密码
  console.log("提交修改密码请求", {
    oldPassword: oldPassword.value,
    newPassword: newPassword.value,
  });

  alert("密码修改成功！");
  router.push("/profile/view");
};
</script>

<style scoped>
.change-password-container {
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

/* 密码卡片 */
.password-card {
  max-width: 500px;
  margin: 0 auto;
  padding: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 标题 */
.password-card h2 {
  margin-bottom: 32px;
  text-align: center;
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 600;
}

/* 表单样式 */
form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-weight: 500;
}

input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s;
}

input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
  outline: none;
}

/* 错误信息 */
.error {
  color: #f56c6c;
  font-size: 14px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 4px;
}

/* 保存按钮 */
.save-btn {
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

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.save-btn:active {
  transform: translateY(0);
}
</style>
