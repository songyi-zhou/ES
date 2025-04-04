<template>
  <div class="app-container">
    <TopBar />
    <div class="main-content">
      <Sidebar />
      <div class="penalty-upload-container">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>综合测评扣分材料上传</span>
            </div>
          </template>
          
          <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px" class="form-container">
            <div class="form-section">
              <h3 class="section-title">基本信息</h3>
              
              <!-- 学生选择 -->
              <el-form-item label="选择学生" prop="studentId">
                <el-select 
                  v-model="formData.studentId" 
                  filterable 
                  placeholder="请选择学生"
                  style="width: 100%">
                  <el-option
                    v-for="student in students"
                    :key="student.studentId"
                    :label="`${student.name} (${student.studentId})`"
                    :value="student.studentId">
                  </el-option>
                </el-select>
              </el-form-item>
              
              <!-- 扣分类型 -->
              <el-form-item label="扣分类型" prop="penaltyType">
                <el-select v-model="formData.penaltyType" placeholder="请选择扣分类型" style="width: 100%">
                  <el-option label="德育测评扣分(A)" value="A"></el-option>
                  <el-option label="学业测评扣分(B)" value="B"></el-option>
                  <el-option label="科研竞赛扣分(C)" value="C"></el-option>
                  <el-option label="文体活动扣分(D)" value="D"></el-option>
                </el-select>
              </el-form-item>
              
              <!-- 扣分分值 -->
              <el-form-item label="扣分分值" prop="penaltyScore">
                <el-input-number 
                  v-model="formData.penaltyScore" 
                  :min="0.5" 
                  :max="20" 
                  :step="0.5"
                  style="width: 100%">                                          
                </el-input-number>
              </el-form-item>
            </div>
            
            <div class="form-section">
              <h3 class="section-title">扣分详情</h3>
              
              <!-- 扣分原因 -->
              <el-form-item label="扣分原因" prop="reason">
                <el-input 
                  type="textarea" 
                  v-model="formData.reason" 
                  :rows="4"
                  placeholder="请详细描述扣分原因">
                </el-input>
              </el-form-item>
              
              <!-- 上传文件 -->
              <el-form-item label="扣分材料" prop="file">
                <el-upload
                  class="upload-demo"
                  :action="uploadUrl"
                  :headers="headers"
                  :on-preview="handlePreview"
                  :on-remove="handleRemove"
                  :on-success="handleSuccess"
                  :on-error="handleError"
                  :before-upload="beforeUpload"
                  :limit="1"
                  :file-list="fileList">
                  <el-button type="primary">点击上传</el-button>
                  <template #tip>
                    <div class="el-upload__tip">
                      请上传PDF、Word或图片格式文件，大小不超过10MB
                    </div>
                  </template>
                </el-upload>
              </el-form-item>
            </div>
            
            <!-- 提交按钮 -->
            <div class="form-actions">
              <el-button type="primary" @click="submitForm" :loading="loading" size="large">提交</el-button>
              <el-button @click="resetForm" size="large">重置</el-button>
            </div>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import axios from '@/utils/axios';
import TopBar from '@/components/TopBar.vue';
import Sidebar from '@/components/Sidebar.vue';

const userStore = useUserStore();
const formRef = ref(null);
const loading = ref(false);
const students = ref([]);
const fileList = ref([]);

// 表单数据
const formData = reactive({
  studentId: '',
  penaltyType: 'A', // 默认为A类
  penaltyScore: 1,
  reason: '',
  fileUrl: ''
});

// 表单验证规则
const rules = {
  studentId: [
    { required: true, message: '请选择学生', trigger: 'change' }
  ],
  penaltyType: [
    { required: true, message: '请选择扣分类型', trigger: 'change' }
  ],
  penaltyScore: [
    { required: true, message: '请输入扣分分值', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入扣分原因', trigger: 'blur' },
    { min: 5, message: '扣分原因不能少于5个字符', trigger: 'blur' }
  ]
};

// 上传相关
const uploadUrl = '/api/instructor/penalty/upload';
const headers = {
  Authorization: `Bearer ${userStore.token}`
};

// 获取学生列表
const fetchStudents = async () => {
  try {
    const response = await axios.get('/api/instructor/students');
    if (response.data.success) {
      students.value = response.data.data;
    } else {
      ElMessage.error(response.data.message || '获取学生列表失败');
    }
  } catch (error) {
    ElMessage.error('获取学生列表失败，请稍后重试');
    console.error('获取学生列表失败:', error);
  }
};

// 文件上传相关方法
const handlePreview = (file) => {
  window.open(file.url);
};

const handleRemove = () => {
  formData.fileUrl = '';
};

const handleSuccess = (response) => {
  if (response.success) {
    formData.fileUrl = response.data.fileUrl;
    ElMessage.success('文件上传成功');
  } else {
    ElMessage.error(response.message || '文件上传失败');
  }
};

const handleError = (err) => {
  ElMessage.error('文件上传失败');
  console.error('上传失败:', err);
};

const beforeUpload = (file) => {
  const isValidType = ['application/pdf', 'application/msword', 
                      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                      'image/jpeg', 'image/png', 'image/gif'].includes(file.type);
  const isLt10M = file.size / 1024 / 1024 < 10;

  if (!isValidType) {
    ElMessage.error('只能上传PDF、Word或图片文件!');
    return false;
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过10MB!');
    return false;
  }
  return true;
};

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!formData.fileUrl) {
        ElMessage.warning('请先上传扣分材料文件');
        return;
      }
      
      loading.value = true;
      try {
        const response = await axios.post('/api/instructor/penalty/submit', formData);
        if (response.data.success) {
          ElMessage.success('扣分材料提交成功');
          resetForm();
        } else {
          ElMessage.error(response.data.message || '提交失败');
        }
      } catch (error) {
        ElMessage.error('提交失败，请稍后重试');
        console.error('提交失败:', error);
      } finally {
        loading.value = false;
      }
    } else {
      ElMessage.warning('请完善表单信息');
      return false;
    }
  });
};

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  formData.penaltyType = 'A';
  formData.penaltyScore = 1;
  formData.fileUrl = '';
  fileList.value = [];
};

onMounted(() => {
  fetchStudents();
});
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.penalty-upload-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

.box-card {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.form-container {
  padding: 10px 20px;
}

.form-section {
  margin-bottom: 25px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #d4d4d4;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #ebeef5;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-input__inner) {
  border-radius: 4px;
}

:deep(.el-textarea__inner) {
  border-radius: 4px;
}

:deep(.el-button) {
  border-radius: 4px;
}
</style> 