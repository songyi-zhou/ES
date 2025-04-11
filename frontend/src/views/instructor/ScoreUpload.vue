<template>
  <div class="upload-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>成绩表格上传</h2>
        </div>

        <div class="upload-form">
          <el-form :model="formData" label-width="120px">
            <!-- 学年选择 -->
            <el-form-item label="学年">
              <el-select v-model="formData.academicYear" placeholder="请选择学年">
                <el-option
                  v-for="year in academicYears"
                  :key="year.value"
                  :label="year.label"
                  :value="year.value"
                />
              </el-select>
            </el-form-item>

            <!-- 学期选择 -->
            <el-form-item label="学期">
              <el-select v-model="formData.semester" placeholder="请选择学期">
                <el-option label="第一学期" value="1" />
                <el-option label="第二学期" value="2" />
              </el-select>
            </el-form-item>

            <!-- 文件上传组件 -->
            <el-form-item>
              <el-upload
                class="upload-demo"
                drag
                ref="uploadRef"
                :auto-upload="false"
                :on-change="handleChange"
                :on-remove="handleRemove"
                :before-upload="beforeUpload"
                :file-list="fileList"
                accept=".xlsx,.xls"
                :limit="1"
                :multiple="false"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  拖拽文件到此处或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持格式：Excel文件（.xlsx, .xls），单个文件不超过10MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <!-- 确认上传按钮 -->
            <el-form-item>
              <div class="button-group">
                <el-button type="primary" @click="submitUpload" :disabled="!canSubmit" :loading="isSubmitting">
                  {{ isSubmitting ? '上传中...' : '确认上传' }}
                </el-button>
                <el-button @click="resetUpload">重置</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 上传记录表格 -->
        <div class="upload-history">
          <h3>上传记录</h3>
          <div class="table-container">
            <el-table 
              :data="uploadHistory" 
              v-loading="loading"
              :header-cell-style="{
                textAlign: 'center'
              }"
              :cell-style="{
                textAlign: 'center'
              }"
            >
              <el-table-column prop="academicYear" label="学年" min-width="120" />
              <el-table-column prop="semester" label="学期" min-width="100">
                <template #default="scope">
                  {{ scope.row.semester === '1' ? '第一学期' : '第二学期' }}
                </template>
              </el-table-column>
              <el-table-column prop="major" label="专业" min-width="150" />
              <el-table-column prop="status" label="状态" min-width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="affectedRows" label="导入条数" min-width="100" />
              <el-table-column prop="uploadTime" label="上传时间" min-width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.uploadTime) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
// import { API_URL } from '@/config'
import axios from 'axios'

// API基础URL
const API_URL = import.meta.env.VITE_API_URL || '';

const uploadRef = ref(null);

// 表单数据
const formData = ref({
  academicYear: '',
  semester: ''
});

// 学年选项
const academicYears = ref([
  { label: '2023-2024', value: '2023-2024' },
  { label: '2024-2025', value: '2024-2025' },
  { label: '2025-2026', value: '2025-2026' }
]);

// 请求头
const getHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}));

// 表单验证
const isFormValid = computed(() => {
  return Boolean(formData.value.academicYear) && Boolean(formData.value.semester);
});

// 上传前验证
const beforeUpload = (file) => {
  // 验证文件类型
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                  file.type === 'application/vnd.ms-excel';
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件！');
    return false;
  }

  // 验证文件大小（限制为 10MB）
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB！');
    return false;
  }

  // 验证表单是否填写完整
  if (!isFormValid.value) {
    ElMessage.error('请先选择学年和学期！');
    return false;
  }

  return true;
};

// 错误相关状态
const errorDialogVisible = ref(false);
const totalRows = ref(0);
const successCount = ref(0);
const errorCount = ref(0);
const errors = ref([]);

// 上传记录相关状态
const loading = ref(false);
const uploadHistory = ref([]);

// 获取上传记录
const fetchUploadHistory = async () => {
  try {
    loading.value = true;
    const response = await axios.get(`${API_URL}/api/instructor/scores/upload-history`, {
      headers: getHeaders.value
    });
    
    if (response.data.success) {
      uploadHistory.value = response.data.data;
    } else {
      ElMessage.error(response.data.message || '获取上传记录失败');
    }
  } catch (error) {
    console.error('获取上传记录失败:', error);
    ElMessage.error('获取上传记录失败，请检查网络连接');
  } finally {
    loading.value = false;
  }
};

// 状态类型映射
const getStatusType = (status) => {
  switch (status) {
    case 1: return 'info';    // 处理中
    case 2: return 'success'; // 成功
    case 3: return 'danger';  // 失败
    default: return 'info';
  }
};

// 状态文本映射
const getStatusText = (status) => {
  switch (status) {
    case 1: return '处理中';
    case 2: return '成功';
    case 3: return '失败';
    default: return '未知';
  }
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 显示错误详情
const showErrorDetails = (record) => {
  totalRows.value = record.totalRows || 0;
  successCount.value = record.affectedRows || 0;
  errorCount.value = record.errorMessage ? record.errorMessage.split('\n').length : 0;
  errors.value = record.errorMessage ? record.errorMessage.split('\n') : [];
  errorDialogVisible.value = true;
};

// 是否正在提交
const isSubmitting = ref(false);

const fileList = ref([]);

// 文件选择处理
const handleChange = (file, files) => {
  console.log('文件变更:', {
    file,
    files,
    formData: formData.value,
    isFormValid: isFormValid.value
  });
  
  if (!isFormValid.value) {
    ElMessage.warning('请先选择学年和学期');
    fileList.value = [];
    return false;
  }
  
  if (file.status === 'ready') {
    // 验证文件类型
    const isExcel = file.raw.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                    file.raw.type === 'application/vnd.ms-excel';
    if (!isExcel) {
      ElMessage.error('只能上传 Excel 文件！');
      fileList.value = [];
      return false;
    }

    // 验证文件大小
    const isLt10M = file.size / 1024 / 1024 < 10;
    if (!isLt10M) {
      ElMessage.error('文件大小不能超过 10MB！');
      fileList.value = [];
      return false;
    }

    fileList.value = [file];
  }
  
  return true;
};

// 文件移除处理
const handleRemove = (file, files) => {
  console.log('文件移除:', {
    file,
    files
  });
  fileList.value = [];
};

// 是否可以提交
const canSubmit = computed(() => {
  const formValid = Boolean(formData.value.academicYear) && Boolean(formData.value.semester);
  const hasFile = fileList.value.length > 0;
  
  console.log('提交状态检查:', {
    formValid,
    hasFile,
    formData: formData.value,
    fileList: fileList.value
  });
  
  return formValid && hasFile;
});

// 重置上传
const resetUpload = () => {
  console.log('重置上传');
  fileList.value = [];
  formData.value.academicYear = '';
  formData.value.semester = '';
};

// 提交上传
const submitUpload = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请选择文件并填写完整信息');
    return;
  }

  if (!fileList.value.length || !fileList.value[0].raw) {
    ElMessage.error('请选择要上传的文件');
    return;
  }

  try {
    await ElMessageBox.confirm(
      '请确认上传的是"单学期或学年"的成绩表文件',
      '上传确认',
      {
        confirmButtonText: '确认上传',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    isSubmitting.value = true;
    const submitFormData = new FormData();
    submitFormData.append('academicYear', formData.value.academicYear);
    submitFormData.append('semester', formData.value.semester);
    submitFormData.append('file', fileList.value[0].raw);

    const response = await axios.post(`${API_URL}/api/instructor/scores/upload`, submitFormData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });

    console.log('上传响应:', response);

    if (response.data.success) {
      ElMessage.success(`成功导入 ${response.data.successCount} 条数据`);
      resetUpload();
      // 等待一小段时间后刷新列表，确保后端数据已更新
      setTimeout(async () => {
        await fetchUploadHistory();
      }, 500);
    } else {
      throw new Error(response.data.message || '上传失败');
    }
  } catch (error) {
    if (error.toString().includes('cancel')) {
      return;
    }
    console.error('上传失败:', error);
    if (error.response?.data) {
      const errorData = error.response.data;
      ElMessage.error(errorData.message || '上传失败，请重试');
      
      // 如果有详细错误信息，显示在错误提示中
      if (errorData.errors && errorData.errors.length > 0) {
        errorData.errors.forEach(err => {
          ElMessage.error(err);
        });
      }
    } else {
      ElMessage.error(error.message || '上传失败，请稍后重试');
    }
  } finally {
    isSubmitting.value = false;
  }
};

// 在组件挂载时获取上传记录
onMounted(() => {
  fetchUploadHistory()
})
</script>

<style scoped>
.upload-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 32px;
  background: #f5f7fa;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.page-header {
  width: 100%;
  max-width: 1000px;
  margin-bottom: 24px;
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.page-header h2 {
  color: #303133;
  font-size: 24px;
  margin: 0;
  text-align: center;
}

.upload-form {
  width: 100%;
  max-width: 1000px;
  background: white;
  padding: 32px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.upload-form :deep(.el-form) {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-form :deep(.el-form-item) {
  width: 100%;
  max-width: 600px;
  margin-bottom: 24px;
}

.upload-form :deep(.el-select) {
  width: 100%;
}

.upload-demo {
  width: 100%;
}

.el-upload {
  width: 100%;
}

.el-upload-dragger {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.el-icon--upload {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 16px;
}

.el-upload__text {
  color: #606266;
  font-size: 16px;
  margin: 8px 0;
  text-align: center;
}

.el-upload__text em {
  color: #409EFF;
  font-style: normal;
}

.el-upload__tip {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
  text-align: center;
}

.button-group {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 32px;
  width: 100%;
}

.button-group .el-button {
  min-width: 120px;
  padding: 12px 24px;
  font-size: 16px;
}

.upload-history {
  width: 100%;
  max-width: 1000px;
  margin-top: 32px;
  background: white;
  padding: 32px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.upload-history h3 {
  margin: 0 0 24px 0;
  color: #303133;
  font-size: 20px;
  text-align: center;
}

.table-container {
  width: 100%;
  overflow-x: auto;
}

:deep(.el-table) {
  margin-top: 16px;
  border-radius: 4px;
  overflow: hidden;
  width: 100% !important;
}

:deep(.el-table__header) {
  width: 100% !important;
}

:deep(.el-table__body) {
  width: 100% !important;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
  padding: 16px 0;
  text-align: center !important;
}

:deep(.el-table td) {
  padding: 16px 0;
  text-align: center !important;
}

:deep(.el-table .cell) {
  padding: 0 16px;
  white-space: nowrap;
}

:deep(.el-tag) {
  padding: 6px 12px;
  font-size: 14px;
  min-width: 80px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}

@media screen and (max-width: 768px) {
  .main-content {
    padding: 16px;
  }

  .upload-form,
  .upload-history {
    padding: 20px;
  }

  .button-group .el-button {
    min-width: 100px;
    padding: 10px 20px;
    font-size: 14px;
  }

  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 12px 0;
  }

  :deep(.el-table .cell) {
    padding: 0 8px;
  }
}
</style>
