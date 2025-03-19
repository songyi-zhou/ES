<template>
  <div class="upload-evaluation-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="upload-section">
          <div class="section-title">上传综合测评材料</div>
          <el-form :model="formData" label-position="left" label-width="100px" class="upload-form">
            <el-form-item label="材料类型" required>
              <el-select 
                v-model="formData.evaluationType" 
                placeholder="请选择材料类型"
              >
                <el-option value="A" label="A类-思想政治" />
                <el-option value="C" label="C类-科研竞赛" />
                <el-option value="D" label="D类-文体活动" />
                <el-option value="E" label="其他" />
              </el-select>
            </el-form-item>

            <el-form-item label="材料标题" required>
              <el-input v-model="formData.title" placeholder="请输入材料标题" />
            </el-form-item>

            <el-form-item label="材料描述">
              <el-input 
                v-model="formData.description" 
                type="textarea" 
                :rows="4" 
                placeholder="请详细描述本次测评材料的内容（时间、地点、参与人员等）"
              />
            </el-form-item>

            <el-form-item label="上传材料" required>
              <el-upload
                class="upload-demo"
                drag
                multiple
                :auto-upload="false"
                :on-change="handleFileChange"
                :file-list="formData.files"
                :on-remove="handleRemove"
                accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  拖拽文件到此处或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持格式：PDF、Word、JPG、PNG，单个文件不超过10MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <el-form-item>
              <div class="button-group">
                <el-button @click="resetForm" size="large">重置</el-button>
                <el-button type="primary" @click="submitEvaluation" :loading="isSubmitting" size="large">
                  {{ isSubmitting ? '提交中...' : '提交材料' }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <div class="history-section">
          <div class="section-title">已上传材料记录</div>
          <el-table v-if="uploadHistory && uploadHistory.length > 0" :data="uploadHistory" border style="width: 100%">
            <el-table-column prop="evaluationType" label="材料类型" width="140" align="center">
              <template #default="scope">
                {{ getEvaluationTypeText(scope.row.evaluationType) }}
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="status" label="审核状态" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上传时间" width="180" align="center" />
          </el-table>
          <div v-else class="empty-tip">暂无上传记录</div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from "vue";
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import router from "@/router";
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';

const formData = ref({
  evaluationType: '',
  title: '',
  description: '',
  files: []
});
const uploadHistory = ref([]);
const isSubmitting = ref(false);
const errorMessage = ref('');
const fileInput = ref(null);

const triggerFileInput = () => {
  fileInput.value.click();
};

const handleFileChange = (file) => {
  console.log('文件变更:', file);
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB');
    return false;
  }
  
  const extension = file.name.split('.').pop().toLowerCase();
  const allowedTypes = ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png'];
  if (!allowedTypes.includes(extension)) {
    ElMessage.error('不支持的文件类型');
    return false;
  }
  
  formData.value.files.push(file.raw);
  return true;
};

const handleFileDrop = (event) => {
  const files = Array.from(event.dataTransfer.files);
  formData.value.files = [...formData.value.files, ...files];
};

const handleRemove = (file) => {
  const index = formData.value.files.findIndex(f => f.uid === file.uid);
  if (index !== -1) {
    formData.value.files.splice(index, 1);
  }
};

// 创建axios实例
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true
});

// 添加请求拦截器
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 添加响应拦截器
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      console.error('API错误响应:', error.response);
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        router.push('/login');
      }
    }
    return Promise.reject(error);
  }
);

// 获取上传历史
const fetchUploadHistory = async () => {
  try {
    console.log('开始获取历史记录');
    const response = await axiosInstance.get('/evaluation/my-materials');
    console.log('获取历史记录响应:', response);
    
    if (response.status === 200 && response.data && Array.isArray(response.data.data)) {
      uploadHistory.value = response.data.data;
      console.log('更新历史记录成功:', uploadHistory.value);
    } else {
      console.warn('响应数据格式不符合预期:', response.data);
      uploadHistory.value = [];
    }
  } catch (error) {
    console.error('获取历史记录失败:', error);
    if (error.response) {
      console.error('错误响应:', error.response);
      console.error('错误数据:', error.response.data);
    }
    ElMessage.error('获取历史记录失败');
    uploadHistory.value = [];
  }
};

// 提交评估材料
const submitEvaluation = async () => {
  // 表单验证
  if (!formData.value.evaluationType) {
    ElMessage.error("请选择材料类型");
    return;
  }

  if (!formData.value.title) {
    ElMessage.error("请输入材料标题");
    return;
  }

  if (formData.value.files.length === 0) {
    ElMessage.error("请上传至少一个文件");
    return;
  }

  isSubmitting.value = true;

  try {
    console.log('开始提交材料，表单数据：', formData.value);
    
    const submitFormData = new FormData();
    submitFormData.append('evaluationType', formData.value.evaluationType);
    submitFormData.append('title', formData.value.title);
    submitFormData.append('description', formData.value.description);
    
    // 添加文件
    formData.value.files.forEach((file, index) => {
      console.log(`添加第 ${index + 1} 个文件:`, file);
      submitFormData.append('files', file);
    });

    const response = await axiosInstance.post('/evaluation/submit', submitFormData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      }
    });

    console.log('上传响应:', response);

    // 修改判断逻辑，不再依赖 success 字段
    if (response.status === 200) {
      ElMessage.success(`材料上传成功！共上传 ${formData.value.files.length} 个文件`);
      resetForm();
      // 等待一小段时间后刷新列表，确保后端数据已更新
      setTimeout(async () => {
        await fetchUploadHistory();
      }, 500);
    } else {
      throw new Error(response.data?.message || '上传失败');
    }
  } catch (error) {
    console.error('上传材料失败:', error);
    if (error.response) {
      console.error('错误响应:', error.response);
      console.error('错误数据:', error.response.data);
    }
    ElMessage.error(error.response?.data?.message || '材料上传失败，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};

const resetForm = () => {
  formData.value = {
    evaluationType: '',
    title: '',
    description: '',
    files: []
  };
  errorMessage.value = '';
};

const getFileIcon = (fileName) => {
  const extension = fileName.split('.').pop().toLowerCase();
  switch (extension) {
    case 'pdf':
      return 'el-icon-pdf';
    case 'doc':
    case 'docx':
      return 'el-icon-word';
    case 'jpg':
    case 'jpeg':
    case 'png':
      return 'el-icon-image';
    default:
      return 'el-icon-file';
  }
};

const formatFileSize = (size) => {
  if (size < 1024) {
    return size + ' B';
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB';
  } else if (size < 1024 * 1024 * 1024) {
    return (size / (1024 * 1024)).toFixed(2) + ' MB';
  } else {
    return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  }
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-');
};

const getStatusType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  };
  return statusMap[status] || 'info';
};

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  };
  return statusMap[status] || '未知状态';
};

const getEvaluationTypeText = (type) => {
  const typeMap = {
    'A': 'A类-思想政治',
    'C': 'C类-科研竞赛',
    'D': 'D类-文体活动',
    'E': '其他'
  };
  return typeMap[type] || type;
};

// 添加自动刷新功能
const autoRefresh = ref(null);

onMounted(() => {
  console.log('组件挂载，准备获取数据');
  fetchUploadHistory();
  // 每30秒自动刷新一次
  autoRefresh.value = setInterval(() => {
    fetchUploadHistory();
  }, 30000);
});

onUnmounted(() => {
  // 组件卸载时清除定时器
  if (autoRefresh.value) {
    clearInterval(autoRefresh.value);
  }
});

// 监听uploadHistory的变化
watch(uploadHistory, (newVal) => {
  console.log('uploadHistory发生变化:', newVal);
}, { deep: true });
</script>

<style scoped>
.upload-evaluation-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content {
  flex: 1;
  display: flex;
  background-color: #f0f2f5;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  height: 100%;
}

.upload-section, .history-section {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  padding: 32px;
  margin-bottom: 24px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

.section-title {
  font-size: 20px;
  color: #1e1e1e;
  font-weight: 600;
  margin-bottom: 32px;
  text-align: center;
  position: relative;
  padding-bottom: 16px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 2px;
  background-color: #409EFF;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1e1e1e;
  font-size: 15px;
}

.full-width {
  width: 100%;
}

.upload-demo {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 24px;
}

.button-group {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

:deep(.el-upload-dragger) {
  width: 100%;
}

:deep(.el-upload__tip) {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__content) {
  width: calc(100% - 100px);  /* 100px是label的宽度 */
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  padding: 4px 12px;
}

:deep(.el-textarea__inner) {
  padding: 8px 12px;
}

:deep(.el-button) {
  padding: 12px 24px;
  font-size: 15px;
}

:deep(.el-table) {
  margin-top: 16px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #1e1e1e;
}

.history-section {
  margin-top: 24px;
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 30px 0;
}

.upload-form {
  max-width: 800px;
  margin: 0 auto;
}

:deep(.el-form-item) {
  display: flex;
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  width: 100px !important;
  line-height: 32px;
  padding-right: 12px;
  text-align: right;
}

:deep(.el-form-item__content) {
  flex: 1;
  margin-left: 0 !important;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  width: 100%;
}

/* 重置下拉框样式 */
:deep(.el-select) {
  display: block !important;
  width: 100% !important;
}

:deep(.el-select .el-input) {
  width: 100% !important;
}

:deep(.el-select .el-input__wrapper) {
  width: 100% !important;
  box-sizing: border-box;
}

:deep(.el-form-item__content) {
  display: block !important;
  flex: 1;
  margin-left: 0 !important;
  width: calc(100% - 100px) !important;
}

/* 确保表单项布局正确 */
:deep(.el-form-item) {
  display: flex !important;
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  flex-shrink: 0;
  width: 100px !important;
}

/* 移除可能影响布局的样式 */
.admin-view :deep(.el-form-item__content),
.admin-view :deep(.el-select) {
  display: block !important;
  width: 100% !important;
}
</style> 