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
                      :before-upload="beforeUpload"
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
                  <div class="file-meta">
                    <el-icon><Document /></el-icon>
                    <span class="file-name">{{ fileInfo.name }}</span>
                    <span class="file-size">({{ formatFileSize(fileInfo.size) }})</span>
                  </div>
                  <el-button 
                    type="primary" 
                    @click="importExcel"
                    :loading="importing"
                    :disabled="!fileInfo"
                  >
                    {{ importing ? '正在导入...' : '开始导入' }}
                  </el-button>
                </div>
                
                <div v-if="importing" class="upload-progress">
                  <el-progress 
                    :percentage="importProgress" 
                    :format="progressFormat"
                    :status="uploadStatus"
                  />
                  <p class="progress-tip">{{ progressTip }}</p>
                </div>
              </div>

              <div v-else class="upload-tip">
                <el-icon><Upload /></el-icon>
                <p>点击选择或拖拽Excel文件到此处</p>
                <p class="tip-detail">支持 .xlsx, .xls 格式，文件大小不超过10MB</p>
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
                <el-form-item 
                  :label="userType === 'instructor' ? '负责中队' : '所属中队'" 
                  prop="squad"
                >
                  <el-input 
                    v-model="formData.squad" 
                    :placeholder="userType === 'instructor' ? '请输入负责的中队，多个用逗号分隔' : '请输入所属中队'"
                  />
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
                  :type="log.status === 'error' ? 'danger' : 'success'"
                  :timestamp="log.time"
                >
                  <div class="log-content">
                    <span class="log-type">{{ log.type }}</span>
                    <span class="log-status" :class="log.status">
                      {{ log.status === 'error' ? '失败' : '成功' }}
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
import { Document, Upload } from '@element-plus/icons-vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

// 文件上传相关
const fileInfo = ref<File | null>(null)
const importing = ref(false)
const importProgress = ref(0)
const uploadStatus = ref<'success' | 'exception' | ''>('')
const progressTip = ref('')

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
  classId: '',
  userType: 'student', // 默认为学生
  squad: ''  // 新增中队字段
})

// 表单验证规则
const formRules = reactive({
  userId: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (userType.value === 'instructor') {
          if (!/^\d{8}$/.test(value)) {
            callback(new Error('导员工号必须为8位纯数字'));
          }
        } else {
          if (!/^\d{10}$/.test(value)) {
            callback(new Error('学生学号必须为10位纯数字'));
          }
        }
        callback();
      },
      trigger: 'blur'
    }
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
  ],
  squad: [
    { required: true, message: '请输入中队信息', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (userType.value === 'instructor') {
          // 导员可以负责多个中队，用逗号分隔
          const squads = value.split(',');
          const isValid = squads.every(squad => /^\d{4}-[1-9]$/.test(squad.trim()));
          if (!isValid) {
            callback(new Error('中队格式错误，应为"年级-序号"，如"2021-1"，多个用逗号分隔'));
          }
        } else {
          // 学生只能属于一个中队
          if (!/^\d{4}-[1-9]$/.test(value)) {
            callback(new Error('中队格式错误，应为"年级-序号"，如"2021-1"'));
          }
        }
        callback();
      },
      trigger: 'blur'
    }
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

// 文件大小限制（10MB）
const MAX_FILE_SIZE = 10 * 1024 * 1024

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 上传前验证
const beforeUpload = (file: File) => {
  // 检查文件类型
  const isExcel = /\.(xlsx|xls)$/.test(file.name.toLowerCase())
  if (!isExcel) {
    ElMessage.error('只能上传Excel文件！')
    return false
  }

  // 检查文件大小
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(`文件大小不能超过 ${formatFileSize(MAX_FILE_SIZE)}！`)
    return false
  }

  return true
}

// 处理文件选择
const handleFileChange = (file) => {
  if (beforeUpload(file.raw)) {
    fileInfo.value = file.raw
    importProgress.value = 0
    uploadStatus.value = ''
    progressTip.value = ''
  }
}

// 修改导入方法
const importExcel = async () => {
  if (!fileInfo.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  const formData = new FormData()
  formData.append('file', fileInfo.value)

  try {
    importing.value = true
    importProgress.value = 0
    uploadStatus.value = ''
    progressTip.value = '正在上传文件...'

    const response = await request.post('/user-import/excel', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percentage = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
          importProgress.value = Math.min(99, percentage) // 保留最后1%给服务器处理时间
          
          if (percentage >= 99) {
            progressTip.value = '文件已上传，正在处理数据...'
          }
        }
      }
    })

    console.log('导入响应:', response)

    if (response.data) {
      const { success, message, data } = response.data
      
      if (success) {
        uploadStatus.value = 'success'
        importProgress.value = 100
        progressTip.value = `导入成功！共导入 ${data?.count || 0} 条记录`
        ElMessage.success(message || '导入成功')
        
        // 如果有部分导入失败的记录
        if (data?.errors?.length > 0) {
          setTimeout(() => {
            ElMessage({
              type: 'warning',
              message: `有${data.errors.length}条记录导入失败`,
              duration: 5000
            })
          }, 1000)
          
          // 为每个错误添加日志
          data.errors.forEach(error => {
            addImportLog('批量导入', 'error', error)
          })
        }
        
        // 延迟清除文件信息
        setTimeout(() => {
          fileInfo.value = null
          importing.value = false
          importProgress.value = 0
          uploadStatus.value = ''
          progressTip.value = ''
        }, 3000)
      } else {
        throw new Error(message || '导入失败')
      }
    } else {
      throw new Error('服务器响应格式错误')
    }
  } catch (error) {
    console.error('导入失败:', error)
    uploadStatus.value = 'exception'
    progressTip.value = '导入失败！'
    ElMessage.error(error.response?.data?.message || error.message || '导入失败')
    addImportLog('批量导入', 'error', '导入失败', error.message)
  } finally {
    if (uploadStatus.value === 'exception') {
      setTimeout(() => {
        importing.value = false
      }, 2000)
    }
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
  if (percentage === 100 && uploadStatus.value === 'success') {
    return '完成'
  }
  return `${percentage}%`
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
</script>

<style scoped>
.import-container {
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
  padding: 20px;
  background: #f5f7fa;
  overflow-y: auto; /* 允许主内容区域滚动 */
}

.page-header {
  margin-bottom: 20px;
}

.page-layout {
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 140px); /* 设置最小高度，确保内容区域足够高 */
}

.main-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.import-card,
.manual-card,
.log-card {
  margin-bottom: 20px;
  height: fit-content;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.file-info {
  padding: 20px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.file-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-weight: 500;
}

.file-size {
  color: #909399;
}

.upload-progress {
  margin-top: 15px;
}

.progress-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}

.upload-tip {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.upload-tip .el-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.tip-detail {
  font-size: 12px;
  margin-top: 8px;
}

.log-content {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 8px;
}

.log-type {
  font-weight: 500;
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

.log-description {
  margin: 8px 0;
  color: #606266;
}

.log-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

/* 滚动条样式 */
.main-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.main-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

/* 响应式布局 */
@media (max-width: 992px) {
  .page-layout {
    flex-direction: column;
  }

  .log-section {
    max-width: none;
  }
}

.log-section {
  flex: 1;
  min-width: 300px;
  max-width: 400px;
}

.log-card {
  margin-bottom: 20px;
  height: calc(100vh ); /* 设置固定高度，减去顶部导航和页面标题的高度 */
  display: flex;
  flex-direction: column;
}

.log-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

.log-card .el-timeline {
  padding: 20px 0;
}

/* 日志部分的滚动条样式 */
.log-card :deep(.el-card__body)::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.log-card :deep(.el-card__body)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.log-card :deep(.el-card__body)::-webkit-scrollbar-track {
  background: #f1f1f1;
}

/* 响应式布局调整 */
@media (max-width: 992px) {
  .page-layout {
    flex-direction: column;
  }

  .log-section {
    max-width: none;
  }

  .log-card {
    height: 400px; /* 在移动端设置固定高度 */
  }
}
</style>