<template>
  <div class="rules-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="rules-card">
          <h2>相关细则</h2>

          <div class="filter-section">
            <div class="filter-item">
              <label>请选择学院：</label>
              <select v-model="selectedCollege">
                <option value="">请选择</option>
                <option value="信息科学技术学院">信息科学技术学院</option>
                <option value="机械工程学院">机械工程学院</option>
                <option value="电气工程学院">电气工程学院</option>
                <option value="材料科学与工程学院">材料科学与工程学院</option>
              </select>
            </div>

            <div class="filter-item">
              <label>请选择中队：</label>
              <select v-model="selectedClass">
                <option value="">请选择</option>
                <option value="2020级">2020级</option>
                <option value="2021级">2021级</option>
                <option value="2022级">2022级</option>
                <option value="2023级">2023级</option>
              </select>
            </div>
          </div>

          <div class="materials-section">
            <h3>相关材料</h3>
            <div class="materials-list">
              <div class="material-item" @click="downloadFile('rules')">
                <div class="file-icon word">
                  <i class="fas fa-file-word"></i>
                </div>
                <div class="file-info">
                  <span class="file-name">中队综合测评细则</span>
                  <span class="file-type">Word文档</span>
                </div>
              </div>

              <div class="material-item" @click="downloadFile('list')">
                <div class="file-icon pdf">
                  <i class="fas fa-file-pdf"></i>
                </div>
                <div class="file-info">
                  <span class="file-name">中队综合测评小组名单</span>
                  <span class="file-type">PDF文档</span>
                </div>
              </div>

              <div class="material-item" @click="downloadFile('scores')">
                <div class="file-icon excel">
                  <i class="fas fa-file-excel"></i>
                </div>
                <div class="file-info">
                  <span class="file-name">中队综合测评社团加分表</span>
                  <span class="file-type">Excel表格</span>
                </div>
              </div>
            </div>
          </div>

          <div class="handbook-section">
            <h3>学校学生手册：</h3>
            <a href="#" class="handbook-link" @click.prevent="viewHandbook">请单击此处查看</a>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import axios from 'axios';

const selectedCollege = ref('');
const selectedClass = ref('');

const downloadFile = async (fileType) => {
  try {
    const response = await axios.get(`/api/evaluation/download/${fileType}`, {
      params: {
        college: selectedCollege.value,
        class: selectedClass.value
      },
      responseType: 'blob'
    });

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    
    // 设置文件名
    const fileName = {
      'rules': '中队综合测评细则.docx',
      'list': '中队综合测评小组名单.pdf',
      'scores': '中队综合测评社团加分表.xlsx'
    }[fileType];
    
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    console.error('文件下载失败:', error);
    alert('文件下载失败，请稍后重试');
  }
};

const viewHandbook = () => {
  window.open('/handbook', '_blank');
};
</script>

<style scoped>
.rules-container {
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

.rules-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

h2 {
  color: #2c3e50;
  font-size: 24px;
  margin-bottom: 24px;
  position: relative;
  padding-left: 16px;
}

h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: #409EFF;
  border-radius: 2px;
}

.filter-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-item {
  min-width: unset;
}

.filter-item label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.filter-item select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
  background: #fff;
}

.filter-item select:hover {
  border-color: #c0c4cc;
}

.filter-item select:focus {
  border-color: #409EFF;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.materials-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-top: 24px;
}

h3 {
  color: #2c3e50;
  font-size: 20px;
  margin: 30px 0 20px;
  padding-left: 12px;
  border-left: 4px solid #409EFF;
}

.materials-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.material-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s;
  cursor: pointer;
}

.material-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.file-icon.word {
  background: #e3f2fd;
  color: #1976d2;
}

.file-icon.pdf {
  background: #ffebee;
  color: #d32f2f;
}

.file-icon.excel {
  background: #e8f5e9;
  color: #388e3c;
}

.file-icon i {
  font-size: 24px;
}

.file-info {
  flex: 1;
}

.file-name {
  color: #303133;
  font-weight: 500;
  margin-bottom: 4px;
}

.file-type {
  color: #909399;
  font-size: 12px;
}

.handbook-section {
  margin-top: 30px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.handbook-link {
  color: #007bff;
  text-decoration: none;
  font-weight: 500;
}

.handbook-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .filter-section {
    grid-template-columns: 1fr;
  }
  
  .materials-list {
    grid-template-columns: 1fr;
  }
}

:deep(.sidebar) {
  height: 100%;
  overflow-y: auto;
}
</style> 