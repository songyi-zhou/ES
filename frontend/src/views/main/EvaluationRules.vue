<template>
  <div class="rules-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="rules-card">
          <h2>相关细则</h2>
          
          <!-- 学生手册下载按钮 -->
          <div class="handbook-section">
            <h3>学生手册</h3>
            <a href="http://xg.dlmu.edu.cn/Home" target="_blank" class="download-btn">
              下载学生手册
            </a>
          </div>
          
          <!-- 规章列表 -->
          <div class="materials-list">
            <div v-for="rule in rules" :key="rule.id" class="material-item">
              <div class="material-info">
                <div class="material-name">{{ rule.title }}</div>
                <div class="material-meta">
                  <span>上传时间：{{ formatDate(rule.createdAt) }}</span>
                  <span>文件大小：{{ formatFileSize(rule.attachments[0].fileSize) }}</span>
                  <span>文件类型：{{ getFileType(rule.attachments[0].fileName) }}</span>
                  <span v-if="rule.department">所属学院：{{ rule.department }}</span>
                  <span v-else>全校通用</span>
                </div>
              </div>
              <div class="material-actions">
                <button @click="downloadFile(rule.id, rule.attachments[0].fileName)" class="download-btn">
                  下载
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopBar from '../../components/TopBar.vue'
import Sidebar from '../../components/Sidebar.vue'
import { ElMessage } from 'element-plus'
import axiosInstance from '../../utils/axios.js'

const rules = ref([])
const loading = ref(false)

// 下载学生手册
const downloadHandbook = () => {
  window.open('/static/handbook.pdf', '_blank')
}

// 格式化日期
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB'
  }
}

// 获取文件类型
const getFileType = (fileName) => {
  const extension = fileName.split('.').pop().toLowerCase()
  switch (extension) {
    case 'pdf':
      return 'PDF文件'
    case 'doc':
      return 'Word文档'
    case 'docx':
      return 'Word文档'
    case 'jpg':
    case 'jpeg':
      return 'JPG图片'
    case 'png':
      return 'PNG图片'
    default:
      return extension.toUpperCase() + '文件'
  }
}

// 下载文件
const downloadFile = async (id, fileName) => {
  try {
    const response = await axiosInstance.get(`/rules/download/${id}`, {
      responseType: 'blob'
    })
    
    // 从响应头获取文件类型
    const contentType = response.headers['content-type']
    const blob = new Blob([response.data], { type: contentType })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName) // 使用原始文件名
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 获取规章列表
const fetchRules = async () => {
  loading.value = true
  try {
    const response = await axiosInstance.get('/rules')
    rules.value = response.data
  } catch (error) {
    console.error('获取规章列表失败:', error)
    ElMessage.error('获取规章列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchRules()
})
</script>

<style scoped>
.rules-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.content {
  display: flex;
  flex: 1;
}

.main-content {
  flex: 1;
  padding: 20px;
}

.rules-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.handbook-section {
  margin-bottom: 2rem;
  padding: 1rem;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.handbook-section h3 {
  margin-bottom: 1rem;
  color: #303133;
}

.materials-list {
  margin-top: 2rem;
}

.material-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #ebeef5;
}

.material-info {
  flex: 1;
}

.material-name {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.material-meta {
  color: #909399;
  font-size: 0.9rem;
}

.material-meta span {
  margin-right: 1rem;
  display: inline-block;
}

.download-btn {
  padding: 8px 16px;
  background-color: #409eff;
  color: white !important;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
  text-decoration: none;
  display: inline-block;
}

.download-btn:hover {
  background-color: #66b1ff;
  text-decoration: none;
}
</style> 
