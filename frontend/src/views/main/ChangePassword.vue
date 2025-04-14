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
            <input 
              v-model="oldPassword" 
              type="password" 
              required 
              :disabled="isLoading"
            />

            <label>新密码：</label>
            <input 
              v-model="newPassword" 
              type="password" 
              required 
              :disabled="isLoading"
            />

            <label>确认新密码：</label>
            <input 
              v-model="confirmPassword" 
              type="password" 
              required 
              :disabled="isLoading"
            />

            <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

            <button 
              type="submit" 
              class="save-btn"
              :disabled="isLoading"
            >
              {{ isLoading ? '修改中...' : '确认修改' }}
            </button>
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
import axios from "axios";
import { ElMessage } from "element-plus";

// 密码字段
const oldPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const errorMessage = ref("");
const isLoading = ref(false);

// 修改密码逻辑
const changePassword = async () => {
  // 重置错误信息
  errorMessage.value = "";

  // 前端验证
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

  try {
    isLoading.value = true;
    
    // 调用后端API
    const response = await axios.post("/api/profile/change-password", {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    });

    if (response.data.success) {
      ElMessage.success("密码修改成功！");
      // 清空表单
      oldPassword.value = "";
      newPassword.value = "";
      confirmPassword.value = "";
      // 跳转到个人资料页面
      router.push("/profile/view");
    } else {
      errorMessage.value = response.data.message;
    }
  } catch (error) {
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data.message || "修改密码失败";
    } else {
      errorMessage.value = "网络错误，请稍后重试";
    }
    console.error("修改密码失败:", error);
  } finally {
    isLoading.value = false;
  }
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
