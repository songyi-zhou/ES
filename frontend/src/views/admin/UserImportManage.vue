<template>
  <div class="import-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>用户信息导入</h2>
        </div>

        <div class="page-layout">
          <div class="main-section">
            <!-- 批量导入卡片 -->
            <el-card class="import-card">
              <template #header>
                <div class="card-header">
                  <h3>批量导入</h3>
                  <div class="header-actions">
                    <el-button type="primary" @click="downloadTemplate">
                      下载模板
                    </el-button>
                    <el-upload
                      class="excel-upload"
                      action="#"
                      :auto-upload="false"
                      :on-change="handleFileChange"
                      :show-file-list="false"
                      accept=".xlsx,.xls"
                    >
                      <el-button type="success">
                        选择Excel文件
                      </el-button>
                    </el-upload>
                  </div>
                </div>
              </template>
              
              <div v-if="fileInfo" class="file-info">
                <div class="file-details">
                  <span>已选择文件：{{ fileInfo.name }}</span>
                  <el-button 
                    type="primary" 
                    @click="importExcel"
                    :loading="importing"
                  >
                    开始导入
                  </el-button>
                </div>
                <el-progress 
                  v-if="importing"
                  :percentage="importProgress"
                  :format="progressFormat"
                />
              </div>
            </el-card>

            <!-- 手动添加表单 -->
            <el-card class="manual-card">
              <template #header>
                <div class="card-header">
                  <h3>手动添加</h3>
                  <el-radio-group v-model="userType">
                    <el-radio-button label="student">学生</el-radio-button>
                    <el-radio-button label="instructor">导员</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              
              <el-form 
                ref="formRef"
                :model="formData"
                :rules="formRules"
                label-width="100px"
              >
                <el-form-item 
                  :label="userType === 'student' ? '学号' : '工号'"
                  prop="userId"
                >
                  <el-input v-model="formData.userId" clearable />
                </el-form-item>
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="formData.name" clearable />
                </el-form-item>
                <el-form-item label="学院" prop="department">
                  <el-input 
                    v-model="formData.department"
                    placeholder="请输入学院名称"
                    maxlength="50"
                    show-word-limit
                    clearable
                  />
                </el-form-item>
                <el-form-item 
                  label="专业" 
                  prop="major"
                  v-if="userType === 'student'"
                >
                  <el-input v-model="formData.major" clearable />
                </el-form-item>
                <el-form-item 
                  label="班级" 
                  prop="className"
                  v-if="userType === 'student'"
                >
                  <el-input v-model="formData.className" clearable />
                </el-form-item>
                <el-form-item 
                  label="班级ID" 
                  prop="classId"
                  v-if="userType === 'student'"
                >
                  <el-input v-model="formData.classId" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitForm">添加</el-button>
                  <el-button @click="resetForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>

          <!-- 导入日志 -->
          <div class="log-section">
            <el-card class="log-card">
              <template #header>
                <div class="card-header">
                  <h3>导入日志</h3>
                  <el-button type="info" @click="refreshLogs">
                    刷新日志
                  </el-button>
                </div>
              </template>
              
              <el-timeline>
                <el-timeline-item
                  v-for="(log, index) in importLogs"
                  :key="index"
                  :type="log.status === 'success' ? 'success' : 'danger'"
                  :timestamp="log.time"
                >
                  <div class="log-content">
                    <span class="log-type">{{ log.type }}</span>
                    <span class="log-status" :class="log.status">
                      {{ log.status === 'success' ? '成功' : '失败' }}
                    </span>
                    <p class="log-description">{{ log.description }}</p>
                    <p v-if="log.error" class="log-error">{{ log.error }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

// 文件上传相关
const fileInfo = ref<File | null>(null)
const importing = ref(false)
const importProgress = ref(0)

// 用户类型选择
const userType = ref<'student' | 'instructor'>('student')

// 表单 ref 和 数据
const formRef = ref<FormInstance>()
const formData = reactive({
  userId: '',
  name: '',
  department: '',
  major: '',
  className: '',
  classId: ''
})

// 表单验证规则
const formRules = reactive({
  userId: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' },
    { pattern: /^\d+$/, message: '请输入正确的学号/工号格式', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请输入学院名称', trigger: 'blur' },
    { min: 2, max: 50, message: '学院名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  major: [
    { required: true, message: '请输入专业', trigger: 'blur' }
  ],
  className: [
    { required: true, message: '请输入班级名称', trigger: 'blur' }
  ],
  classId: [
    { required: true, message: '请输入班级ID', trigger: 'blur' }
  ]
})

// 导入日志
const importLogs = ref<Array<{
  time: string
  type: string
  status: 'success' | 'error' | 'info'
  description: string
  error?: string
}>>([])

// 下载模板
const downloadTemplate = async () => {
  try {
    const response = await request.get('/user-import/template', {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '用户导入模板.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('模板下载成功')
    addImportLog('系统', 'success', '下载用户导入模板')
  } catch (error) {
    console.error('模板下载失败:', error)
    ElMessage.error('模板下载失败')
    addImportLog('系统', 'error', '下载模板失败', error.message)
  }
}

// 处理文件选择
const handleFileChange = (file: File) => {
  fileInfo.value = file
  addImportLog('系统', 'info', `已选择文件: ${file.name}`)
}

// 导入Excel
const importExcel = async () => {
  if (!fileInfo.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    importing.value = true
    importProgress.value = 0
    
    const formData = new FormData()
    formData.append('file', fileInfo.value)
    
    const token = localStorage.getItem('token')
    if (!token) {
      throw new Error('未登录或登录已过期')
    }
    
    const response = await request.post('/user-import/excel', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${token}`  
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          importProgress.value = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
        }
      }
    })

    if (response.data.success) {
      ElMessage.success('导入成功')
      addImportLog('批量导入', 'success', `成功导入${response.data.data}条记录`)
    } else {
      throw new Error(response.data.message)
    }
  } catch (error: any) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败')
    addImportLog('批量导入', 'error', '导入失败', error.message)
  } finally {
    importing.value = false
    fileInfo.value = null
    importProgress.value = 0
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    const response = await request.post('/user-import/manual', {
      ...formData,
      userType: userType.value
    }, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (response.data.success) {
      ElMessage.success('添加成功')
      addImportLog('手动添加', 'success', 
        `添加${userType.value === 'student' ? '学生' : '导员'}：${formData.name}`
      )
      resetForm()
    } else {
      throw new Error(response.data.message)
    }
  } catch (error: any) {
    if (!error.errors) { // 如果不是验证错误
      console.error('添加失败:', error)
      ElMessage.error(error.message || '添加失败')
      addImportLog('手动添加', 'error', '添加失败', error.message)
    }
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
}

// 添加导入日志
const addImportLog = (
  type: string, 
  status: 'success' | 'error' | 'info', 
  description: string, 
  error?: string
) => {
  const now = new Date()
  const timeString = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-')
  
  importLogs.value.unshift({
    time: timeString,
    type,
    status,
    description,
    error
  })
}

// 获取导入日志
const fetchLogs = async () => {
  try {
    const userStore = useUserStore()
    const token = userStore.token
    
    if (!token) {
      ElMessage.error('未登录或 token 已过期')
      return
    }
    
    console.log('Using token:', token) // 调试用
    
    const response = await request.get('/user-import/logs', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (response.data.success) {
      importLogs.value = response.data.data
      ElMessage.success('日志已刷新')
    }
  } catch (error) {
    console.error('获取日志失败:', error)
    if (error.response?.status === 403) {
      ElMessage.error('没有权限访问')
    } else {
      ElMessage.error('获取日志失败')
    }
  }
}

// 刷新日志
const refreshLogs = () => {
  fetchLogs()
}

// 在组件挂载时获取日志
onMounted(() => {
  fetchLogs()
})

// 进度条格式化
const progressFormat = (percentage: number) => {  
  return percentage === 100 ? '完成' : `${percentage}%`
}

// 格式化时间函数
const formatTime = (time: string) => {
  if (!time) return '';
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-');
}
</script>

<style scoped>
/* 样式部分保持不变 */
.import-container {
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
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.page-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 100px);
}

.main-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.import-card,
.manual-card {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.file-info {
  padding: 10px 0;
}

.file-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.log-section {
  flex: 1;
  overflow-y: auto;
}

.log-card {
  height: 100%;
}

.log-content {
  padding: 5px 0;
}

.log-type {
  font-weight: bold;
  margin-right: 8px;
}

.log-status {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.log-status.success {
  background: #f0f9eb;
  color: #67c23a;
}

.log-status.error {
  background: #fef0f0;
  color: #f56c6c;
}

.log-status.info {
  background: #f4f4f5;
  color: #909399;
}

.log-description {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.log-error {
  color: #f56c6c;
  font-size: 12px;
  margin: 5px 0;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .page-layout {
    flex-direction: column;
  }
  
  .main-section,
  .log-section {
    width: 100%;
  }
  
  .log-section {
    margin-top: 20px;
  }
}
</style>