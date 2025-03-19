<template>
  <div class="guide-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>操作指南</h2>
        </div>
        
        <section class="guide-card">
          <!-- 搜索框 + 搜索按钮 -->
          <div class="search-container">
            <input
                v-model="searchQuery"
                placeholder="搜索操作指南..."
                class="search-input"
                @keyup.enter="performSearch"
            />
            <button class="search-btn" @click="performSearch">🔍 搜索</button>
          </div>

          <div class="guide-content">
            <p class="guide-intro">本页面帮助用户了解本系统的相关操作：</p>
            <ul class="guide-list">
              <li class="guide-section">
                <h3>基础功能</h3>
                <ul>
                  <li>📂 如有需要上传自己的综测材料，请点击：<router-link to="/evaluation/upload">上传材料</router-link></li>
                  <li>📊 如有需要查看自己的综测成绩，请点击：<router-link to="/evaluation/view">个人综测</router-link></li>
                  <li>📜 如有需要查看中队综测评定细则，请点击：<router-link to="/evaluation/rules">综测细则</router-link></li>
                  <li>⚙️ 如有需要查看个人信息或者修改密码，请点击：<router-link to="/profile/view">个人中心</router-link></li>
                  <li>📋 如有需要查看综合测评涉及的相关评定，请点击：<router-link to="/evaluation/results">综测相关</router-link></li>
                  <li>👥 如有需要查看自己的工作分配与班级上传的材料，请点击：<router-link to="/evaluation/group-members">综测小组</router-link></li>
                </ul>
              </li>

              <li class="guide-section">
                <h3>管理员功能模块</h3>
                <ul>
                  <li>👮‍♂️ 导员系统权限设置：<router-link to="/admin/instructor-permissions">权限管理</router-link></li>
                  <li>🔒 下属权限定制：<router-link to="/admin/subordinate-permissions">权限定制</router-link></li>
                  <li>👨‍🎓 学生信息导入：<router-link to="/admin/student-import">信息导入</router-link></li>
                </ul>
              </li>

              <li class="guide-section">
                <h3>导员功能模块</h3>
                <ul>
                  <li>📝 综测结果查看：<router-link to="/evaluation/review-evaluation-forms">结果查看</router-link></li>
                  <li>🔍 疑问个人材料审核：<router-link to="/evaluation/review-question-materials">材料审核</router-link></li>
                  <li>👥 综测小组选定：<router-link to="/evaluation/group-members-manage">小组选定</router-link></li>
                </ul>
              </li>

              <li class="guide-section">
                <h3>其他功能</h3>
                <ul>
                  <li>🏆 如有需要查看下属中队的综测事宜，请点击：<router-link to="/evaluation/review-materials">中队综测管控</router-link></li>
                  <li>📑 如有需要上传中队综测有关的规定材料，请点击：<router-link to="/evaluation/upload-rules">上传中队材料</router-link></li>
                  <li>❓ 如有其他疑问或者需求，请点击"提出问题"进行答疑：<router-link to="/system/feedback">提出疑问</router-link></li>
                  <li>🔄 如想查看系统更新情况，请点击：<router-link to="/system/updates">查看更新</router-link></li>
                </ul>
              </li>
            </ul>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import router from "@/router";

const searchQuery = ref("");

// 过滤指南
const filteredGuides = computed(() => {
  if (!searchQuery.value) return guides.value;
  return guides.value.filter((item) =>
      item.label.includes(searchQuery.value)
  );
});

// 触发搜索
const performSearch = () => {
  console.log("搜索内容：", searchQuery.value);
  // 可以在此扩展搜索逻辑，如跳转到搜索结果页面
};
</script>

<style scoped>
.guide-container {
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

.guide-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.search-container {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  border-color: #409eff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.search-btn {
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.search-btn:hover {
  background: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

.guide-content {
  padding: 0 12px;
}

.guide-intro {
  font-size: 16px;
  color: #606266;
  margin-bottom: 20px;
}

.guide-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.guide-list li {
  color: #606266;
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.6;
  padding-left: 24px;
  position: relative;
}

.guide-list li:last-child {
  margin-bottom: 0;
}

.guide-list a {
  color: #409eff;
  text-decoration: none;
  transition: color 0.3s;
}

.guide-list a:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.guide-section {
  margin-bottom: 32px;
}

.guide-section:last-child {
  margin-bottom: 0;
}

.guide-section h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 600;
  position: relative;
  padding-left: 12px;
}

.guide-section h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  background: #409eff;
  border-radius: 2px;
}

.guide-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.guide-section li {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
  line-height: 1.6;
  padding-left: 24px;
  position: relative;
}

.guide-section li:last-child {
  margin-bottom: 0;
}

.guide-section a {
  color: #409eff;
  text-decoration: none;
  transition: color 0.3s;
}

.guide-section a:hover {
  color: #66b1ff;
  text-decoration: underline;
}
</style>
