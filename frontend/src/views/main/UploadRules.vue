<template>
  <div class="rules-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综合测评规章管理</h2>
          <el-button 
            type="primary" 
            size="large" 
            class="upload-btn" 
            @click="showUploadModal = true"
            :icon="Upload"
          >
            上传并发布材料
          </el-button>
        </div>

        <!-- 材料列表 -->
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>文件名称</th>
                <th>上传时间</th>
                <th>文件大小</th>
                <th>文件类型</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="file in files" :key="file.id">
                <td class="file-name">
                  <i class="file-icon">📄</i>
                  <span>{{ file.name }}</span>
                </td>
                <td>{{ formatDate(file.uploadTime) }}</td>
                <td>{{ formatFileSize(file.size) }}</td>
                <td>{{ getFileType(file.originalName || file.name) }}</td>
                <td class="actions">
                  <button class="preview-btn" @click="previewFile(file)">预览</button>
                  <button class="download-btn" @click="downloadFile(file)">下载</button>
                  <button class="delete-btn" @click="confirmDelete(file)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 上传弹窗 -->
        <div v-if="showUploadModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>上传规章材料</h3>
              <button class="close-btn" @click="closeUploadModal">×</button>
            </div>
            <div class="form-group">
              <label>文件名称：</label>
              <input 
                type="text" 
                v-model="uploadForm.name"
                placeholder="请输入文件名称"
              >
            </div>
            <div class="form-group">
              <label>选择文件：</label>
              <div class="file-input-container">
                <input 
                  type="file" 
                  ref="fileInput"
                  @change="handleFileChange"
                  accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.gif"
                >
                <div class="file-info" v-if="uploadForm.file">
                  已选择: {{ uploadForm.file.name }}
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>备注说明：</label>
              <textarea 
                v-model="uploadForm.description"
                rows="3"
                placeholder="请输入文件说明（选填）"
              ></textarea>
            </div>
            <div class="modal-actions">
              <button @click="submitUpload" class="btn-primary" :disabled="!canSubmit">
                确认上传
              </button>
              <button @click="closeUploadModal" class="btn-secondary">取消</button>
            </div>
          </div>
        </div>

        <!-- 预览弹窗 -->
        <div v-if="showPreviewModal" class="modal">
          <div class="modal-content preview-modal">
            <div class="modal-header">
              <h3>文件预览</h3>
              <button class="close-btn" @click="showPreviewModal = false">×</button>
            </div>
            <div class="preview-container">
              <!-- 根据文件类型显示不同的预览组件 -->
              <iframe v-if="previewUrl && isPdfFile(selectedFile)" 
                      :src="previewUrl" 
                      width="100%" 
                      height="600px">
              </iframe>
              <img v-else-if="previewUrl && isImageFile(selectedFile)"
                   :src="previewUrl"
                   style="max-width: 100%; max-height: 600px;">
              <div v-else-if="isExcelFile(selectedFile)" class="excel-preview">
                <p>Excel文件不支持直接预览，请下载后查看</p>
                <button class="download-btn" @click="downloadFile(selectedFile)">下载文件</button>
              </div>
              <div v-else class="no-preview">
                <p>该文件类型不支持在线预览，请下载后查看</p>
                <button class="download-btn" @click="downloadFile(selectedFile)">下载文件</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 删除确认弹窗 -->
        <div v-if="showDeleteModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>确认删除</h3>
              <button class="close-btn" @click="showDeleteModal = false">×</button>
            </div>
            <div class="confirm-message">
              确定要删除文件 "{{ selectedFile?.name }}" 吗？此操作不可恢复。
            </div>
            <div class="modal-actions">
              <button @click="deleteFile" class="btn-danger">确认删除</button>
              <button @click="showDeleteModal = false" class="btn-secondary">取消</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import TopBar from '@/components/TopBar.vue'
import Sidebar from '@/components/Sidebar.vue'
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const files = ref([])
const showUploadModal = ref(false)
const showPreviewModal = ref(false)
const showDeleteModal = ref(false)
const selectedFile = ref(null)
const previewUrl = ref(null)

const uploadForm = ref({
  name: '',
  file: null,
  description: ''
})

// 计算属性：是否可以提交
const canSubmit = computed(() => {
  return uploadForm.value.name && uploadForm.value.file
})

// 处理认证错误
const handleAuthError = (error) => {
  if (error.response && (error.response.status === 401 || error.response.status === 403)) {
    ElMessage.error('登录已过期，请重新登录')
    userStore.clearUserInfo()
    router.push('/login')
    return true
  }
  return false
}

// 获取文件列表
const fetchFiles = async () => {
  try {
    const response = await axios.get('/api/rules', {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    files.value = response.data.map(file => ({
      id: file.id,
      name: file.title,
      description: file.description,
      uploadTime: file.createdAt,
      size: file.attachments[0]?.fileSize || 0,
      type: file.attachments[0]?.fileType,
      originalName: file.attachments[0]?.fileName,
      path: file.attachments[0]?.filePath
    }))
  } catch (error) {
    console.error('获取文件列表失败:', error)
    if (!handleAuthError(error)) {
      ElMessage.error('获取文件列表失败')
    }
  }
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    uploadForm.value.file = file
  }
}

// 提交上传
const submitUpload = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    formData.append('title', uploadForm.value.name)
    formData.append('description', uploadForm.value.description || '')
    
    await axios.post('/api/rules', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    ElMessage.success('上传成功')
    closeUploadModal()
    fetchFiles() // 刷新文件列表
  } catch (error) {
    console.error('上传失败:', error)
    if (!handleAuthError(error)) {
      ElMessage.error('上传失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

// 下载文件
const downloadFile = async (file) => {
  if (!file) return
  
  try {
    const response = await axios.get(`/api/rules/download/${file.id}`, {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', file.originalName || file.name)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    console.error('下载失败:', error)
    if (!handleAuthError(error)) {
      ElMessage.error('文件下载失败')
    }
  }
}

// 获取文件类型
const getFileType = (fileName) => {
  if (!fileName) return '未知类型'
  
  const extension = fileName.split('.').pop().toLowerCase()
  switch (extension) {
    case 'pdf': return 'PDF文件'
    case 'doc': return 'Word文档'
    case 'docx': return 'Word文档'
    case 'xls': return 'Excel表格'
    case 'xlsx': return 'Excel表格'
    case 'ppt': return 'PPT演示文稿'
    case 'pptx': return 'PPT演示文稿'
    case 'jpg': return 'JPG图片'
    case 'jpeg': return 'JPEG图片'
    case 'png': return 'PNG图片'
    case 'gif': return 'GIF图片'
    default: return extension.toUpperCase() + '文件'
  }
}

// 判断文件类型的辅助函数
const isPdfFile = (file) => {
  if (!file) return false
  const name = file.originalName?.toLowerCase() || file.name?.toLowerCase() || ''
  return name.endsWith('.pdf') || file.type === 'application/pdf'
}

const isImageFile = (file) => {
  if (!file) return false
  const name = file.originalName?.toLowerCase() || file.name?.toLowerCase() || ''
  return name.endsWith('.jpg') || 
         name.endsWith('.jpeg') || 
         name.endsWith('.png') || 
         name.endsWith('.gif') ||
         (file.type && file.type.startsWith('image/'))
}

const isExcelFile = (file) => {
  if (!file) return false
  const name = file.originalName?.toLowerCase() || file.name?.toLowerCase() || ''
  return name.endsWith('.xls') || 
         name.endsWith('.xlsx') ||
         file.type === 'application/vnd.ms-excel' ||
         file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
}

// 预览文件
const previewFile = async (file) => {
  selectedFile.value = file
  
  try {
    const response = await axios.get(`/api/rules/download/${file.id}`, {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    previewUrl.value = URL.createObjectURL(response.data)
    showPreviewModal.value = true
  } catch (error) {
    console.error('预览失败:', error)
    if (!handleAuthError(error)) {
      ElMessage.error('文件预览失败')
    }
  }
}

// 确认删除
const confirmDelete = (file) => {
  selectedFile.value = file
  showDeleteModal.value = true
}

// 删除文件
const deleteFile = async () => {
  try {
    await axios.delete(`/api/rules/${selectedFile.value.id}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    ElMessage.success('文件删除成功')
    showDeleteModal.value = false
    fetchFiles() // 刷新文件列表
  } catch (error) {
    console.error('删除失败:', error)
    if (!handleAuthError(error)) {
      ElMessage.error('文件删除失败')
    }
  }
}

// 关闭上传弹窗
const closeUploadModal = () => {
  showUploadModal.value = false
  uploadForm.value = {
    name: '',
    file: null,
    description: ''
  }
  if (document.querySelector('input[type="file"]')) {
    document.querySelector('input[type="file"]').value = ''
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  if (bytes === 0) return '0 Byte'
  const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)))
  return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i]
}

onMounted(() => {
  // 检查是否已登录
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  fetchFiles()
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
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-style: normal;
}

.actions {
  display: flex;
  gap: 8px;
}

.preview-btn,
.download-btn,
.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.preview-btn {
  background: #409eff;
  color: white;
}

.download-btn {
  background: #67c23a;
  color: white;
}

.delete-btn {
  background: #f56c6c;
  color: white;
}

.preview-btn:hover {
  background: #66b1ff;
}

.download-btn:hover {
  background: #85ce61;
}

.delete-btn:hover {
  background: #f78989;
}

/* 弹窗样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
}

.preview-modal {
  max-width: 800px;
  height: 80vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

input[type="text"],
textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.file-input-container {
  margin-top: 8px;
}

.file-info {
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary,
.btn-secondary,
.btn-danger {
  padding: 8px 15px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background: #409eff;
  color: white;
  border: none;
}

.btn-primary:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f4f4f5;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.btn-danger {
  padding: 8px 15px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.confirm-message {
  margin: 20px 0;
  color: #606266;
  text-align: center;
}

.btn-primary:not(:disabled):hover {
  background: #66b1ff;
}

.btn-secondary:hover {
  background: #f9f9fa;
}

.btn-danger:hover {
  background: #f78989;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  flex-direction: column;
}

.no-preview, .excel-preview {
  text-align: center;
  color: #909399;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 30px;
}
</style> 