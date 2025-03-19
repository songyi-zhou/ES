<template>
  <div class="edit-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <section class="edit-card">
          <h2>修改个人信息</h2>
          <!-- 头像上传区域 -->
          <div class="avatar-container" @click="triggerFileInput">
            <img src="@/assets/picture/school_logo.webp" alt="个人头像" class="avatar" />
            <div class="avatar-overlay">修改头像</div>
            <input type="file" id="fileUpload" @change="handleFileUpload" accept="image/*" hidden />
          </div>

          <form @submit.prevent="saveChanges">
            <div class="input-group">
              <label>姓名：</label>
              <input v-model="userInfo.name" type="text" required />
            </div>

            <div class="input-group">
              <label>学号：</label>
              <input v-model="userInfo.studentNumber" type="text" disabled />
            </div>

            <div class="input-group">
              <label>学院：</label>
              <input v-model="userInfo.college" type="text" required />
            </div>

            <div class="input-group">
              <label>专业：</label>
              <input v-model="userInfo.major" type="text" required />
            </div>

            <div class="input-group">
              <label>联系电话：</label>
              <input v-model="userInfo.phone" type="text" required />
            </div>

            <div class="input-group">
              <label>邮箱：</label>
              <input v-model="userInfo.email" type="email" required />
            </div>

            <button type="submit" class="save-btn">保存修改</button>
          </form>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import router from "@/router";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";

const userInfo = ref({
  name: "张三",
  studentNumber: "20211001",
  college: "计算机学院",
  major: "软件工程",
  phone: "13888888888",
  email: "zhangsan@example.com",
  avatar: "https://via.placeholder.com/120" // 默认头像
});

// 触发文件上传
const triggerFileInput = () => {
  document.getElementById("fileUpload").click();
};

// 处理头像上传
const handleFileUpload = (event) => {
  const file = event.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (e) => {
      userInfo.value.avatar = e.target.result; // 预览新头像
    };
    reader.readAsDataURL(file);

    // 可以调用API上传文件
    // const formData = new FormData();
    // formData.append("file", file);
    // axios.post("/api/upload-avatar", formData)
    //   .then(response => userInfo.value.avatar = response.data.url)
    //   .catch(error => console.error("上传失败", error));
  }
};

// 保存修改
const saveChanges = () => {
  alert("修改成功！");
  router.push("/profile/view");
};
</script>

<style scoped>
.edit-container {
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

/* 编辑卡片 */
.edit-card {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* 标题 */
.edit-card h2 {
  margin-bottom: 32px;
  text-align: center;
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 600;
}

/* 头像上传区域 */
.avatar-container {
  width: 120px;
  height: 120px;
  margin: 0 auto 32px;
  position: relative;
  cursor: pointer;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s;
}

.avatar-container:hover .avatar {
  transform: scale(1.05);
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

/* 表单组 */
.input-group {
  margin-bottom: 24px;
}

.input-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-weight: 500;
}

.input-group input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s;
}

.input-group input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
  outline: none;
}

.input-group input:disabled {
  background: #f5f7fa;
  cursor: not-allowed;
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
