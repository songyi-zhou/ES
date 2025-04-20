<template>
  <div class="feedback-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>意见与建议</h2>
        </div>
        
        <section class="feedback-card">
          <p class="description">您的反馈对我们非常重要，请填写以下内容：</p>

          <div class="form-group">
            <label for="feedbackType">反馈类型：</label>
            <select v-model="feedback.type" class="input-box">
              <option value="bug">功能异常</option>
              <option value="suggestion">功能建议</option>
              <option value="other">其他</option>
            </select>
          </div>

          <div class="form-group">
            <label for="feedbackContent">您的意见或建议：</label>
            <textarea
                v-model="feedback.description"
                placeholder="请输入您的意见或建议..."
                class="input-box textarea-box"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="contactEmail">联系邮箱（选填）：</label>
            <input
                type="email"
                v-model="feedback.email"
                placeholder="请输入您的联系邮箱..."
                class="input-box"
            />
          </div>

          <button @click="submitFeedback" class="submit-button" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '提交' }}
          </button>
        </section>
      </main>
    </div>

    <!-- 成功提示 -->
    <transition name="fade">
      <div v-if="showSuccess" class="success-popup">
        🎉 提交成功！感谢您的反馈！
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import axios from "axios";
import { ElMessage } from "element-plus";

// 反馈表单数据
const feedback = reactive({
  type: "功能建议",
  description: "",
  email: ""
});

const showSuccess = ref(false);
const isSubmitting = ref(false);

// 提交反馈
const submitFeedback = async () => {
  if (!feedback.type || !feedback.description) {
    ElMessage.warning('请填写所有必填项');
    return;
  }

  isSubmitting.value = true;
  
  try {
    const response = await axios.post('/api/feedback', feedback);
    if (response.data.code === 200) {
      ElMessage.success('反馈提交成功！感谢您的反馈！');
      // 显示提交成功提示
      showSuccess.value = true;
      setTimeout(() => {
        showSuccess.value = false;
      }, 3000);
      
      // 清空输入
      feedback.type = "功能建议";
      feedback.description = "";
      feedback.email = "";
    }
  } catch (error) {
    console.error('提交反馈失败:', error);
    ElMessage.error(error.response?.data?.message || '提交反馈失败，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.feedback-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 20px;
  background: #f5f7fa;
  overflow-y: auto;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.page-header h2 {
  font-size: 24px;
  color: #303133;
  margin: 0;
  font-weight: 600;
}

.feedback-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.description {
  color: #606266;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.input-box {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.input-box:focus {
  border-color: #409eff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.textarea-box {
  min-height: 120px;
  resize: vertical;
}

.submit-button {
  width: 100%;
  padding: 12px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.submit-button:hover:not(:disabled) {
  background: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

.submit-button:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

/* 成功提示 */
.success-popup {
  position: fixed;
  top: 10%;
  left: 50%;
  transform: translateX(-50%);
  background: #67c23a;
  color: white;
  padding: 12px 24px;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

/* 动画效果 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
