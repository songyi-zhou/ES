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
                  accept=".pdf,.doc,.docx"
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
        <el-dialog v-model="showPreviewModal" title="文件预览" width="80%" destroy-on-close>
          <div class="preview-container">
            <!-- 根据文件类型显示不同的预览组件 -->
            <iframe v-if="previewUrl && selectedFile?.name.endsWith('.pdf')" 
                    :src="previewUrl" 
                    width="100%" 
                    height="600px" 
                    frameborder="0">
            </iframe>
            <img v-else-if="previewUrl" 
                 :src="previewUrl" 
                 class="preview-image" 
                 alt="文件预览">
            <div v-else class="preview-error">
              无法预览此类型的文件
            </div>
          </div>
        </el-dialog>

        <!-- 删除确认弹窗 -->
        <div v-if="showDeleteModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>确认删除</h3>
              <button class="close-btn" @click="showDeleteModal = false">×</button>
            </div>
            <div class="confirm-message">
              确定要删除文件 "{{ selectedFile?.name }}" 吗？此操作不可撤销。
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
import { ref, computed, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import axios from 'axios'

// 创建带有认证令牌的 axios 实例
const axiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

// 添加请求拦截器，自动添加 token
axiosInstance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 文件列表数据
const files = ref([])

// 获取规章列表
const fetchRules = async () => {
  try {
    const response = await axiosInstance.get('/rules')
    files.value = response.data.map(rule => ({
      id: rule.id,
      name: rule.attachments[0].fileName,
      uploadTime: rule.createdAt,
      size: rule.attachments[0].fileSize,
      url: `/api/rules/download/${rule.id}`
    }))
  } catch (error) {
    console.error('获取规章列表失败:', error)
    ElMessage.error('获取规章列表失败')
  }
}

// 上传表单
const uploadForm = ref({
  name: '',
  file: null,
  description: ''
})

// 模态框控制
const showUploadModal = ref(false)
const showPreviewModal = ref(false)
const showDeleteModal = ref(false)
const selectedFile = ref(null)
const previewUrl = ref('')

// 文件输入引用
const fileInput = ref(null)

// 是否可以提交
const canSubmit = computed(() => {
  return uploadForm.value.name && uploadForm.value.file
})

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    uploadForm.value.file = file
    if (!uploadForm.value.name) {
      uploadForm.value.name = file.name.split('.')[0]
    }
  }
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
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 预览文件
const previewFile = async (file) => {
  try {
    selectedFile.value = file;
    
    // 获取文件扩展名
    const fileExtension = file.name.split('.').pop().toLowerCase();
    
    // 检查文件类型是否可以预览
    const previewableTypes = ['pdf', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
    const nonPreviewableTypes = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'];
    
    if (nonPreviewableTypes.includes(fileExtension)) {
      // Office文档无法直接预览，提示用户下载
      ElMessage.info(`${fileExtension.toUpperCase()} 文件无法直接预览，正在下载...`);
      showPreviewModal.value = false;
      downloadFile(file);
      return;
    }
    
    // 对于可预览的文件类型
    const response = await axiosInstance.get(`/rules/download/${file.id}`, {
      responseType: 'blob'
    });
    
    // 创建Blob URL
    const blob = new Blob([response.data], {type: response.headers['content-type']});
    previewUrl.value = URL.createObjectURL(blob);
    
    // 显示预览模态框
    showPreviewModal.value = true;
  } catch (error) {
    console.error('预览失败:', error);
    ElMessage.error('预览失败');
  }
}

// 下载文件
const downloadFile = async (file) => {
  try {
    // 使用 axios 实例下载文件，而不是直接打开链接
    const response = await axiosInstance.get(`/rules/download/${file.id}`, {
      responseType: 'blob'
    });
    
    // 创建 Blob 对象
    const blob = new Blob([response.data]);
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', file.name);
    document.body.appendChild(link);
    link.click();
    
    // 清理
    window.URL.revokeObjectURL(url);
    document.body.removeChild(link);
  } catch (error) {
    console.error('下载失败:', error);
    ElMessage.error('下载失败');
  }
}

// 删除文件
const confirmDelete = (file) => {
  selectedFile.value = file;
  showDeleteModal.value = true;
}

const deleteFile = async () => {
  if (!selectedFile.value) return;
  
  try {
    // 使用axios实例发送删除请求
    await axiosInstance.delete(`/rules/${selectedFile.value.id}`);
    
    ElMessage.success('删除成功');
    showDeleteModal.value = false;
    selectedFile.value = null;
    await fetchRules();
  } catch (error) {
    console.error('删除失败:', error);
    ElMessage.error(error.response?.data?.message || '删除失败');
  }
}

// 提交上传
const submitUpload = async () => {
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    formData.append('title', uploadForm.value.name)
    if (uploadForm.value.description) {
      formData.append('description', uploadForm.value.description)
    }

    const response = await axiosInstance.post('/rules', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data) {
      ElMessage.success('上传成功')
      closeUploadModal()
      fetchRules()
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error(error.response?.data?.message || '上传失败')
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
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

onMounted(() => {
  fetchRules()
})
</script>

<style scoped>
.rules-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f5f7fa;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  position: relative;
}

.page-header h2::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 3px;
  background: #409eff;
  border-radius: 2px;
}

.upload-btn {
  font-size: 16px;
  padding: 12px 24px;
  height: auto;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
}

.upload-btn:active {
  transform: translateY(0);
}

.upload-btn .el-icon {
  margin-right: 8px;
  font-size: 18px;
}

.table-container {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-top: 24px;
}

/* 美化表格样式 */
table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  padding: 16px;
  text-align: left;
  border-bottom: 2px solid #ebeef5;
}

td {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  font-size: 24px;
  color: #409eff;
}

.actions {
  display: flex;
  gap: 12px;
}

.actions button {
  padding: 6px 12px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.preview-btn {
  background: #909399;
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

.preview-btn:hover { background: #a6a9ad; }
.download-btn:hover { background: #85ce61; }
.delete-btn:hover { background: #f78989; }

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .upload-btn {
    width: 100%;
  }
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
  max-width: 500px;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.preview-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
}

.preview-error {
  color: #f56c6c;
  font-size: 16px;
  text-align: center;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
}

.close-btn:hover {
  color: #606266;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
}

.file-input-container {
  border: 1px dashed #dcdfe6;
  padding: 20px;
  border-radius: 4px;
  text-align: center;
  cursor: pointer;
}

.file-info {
  margin-top: 10px;
  color: #409eff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary {
  padding: 8px 15px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 8px 15px;
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
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
</style> 