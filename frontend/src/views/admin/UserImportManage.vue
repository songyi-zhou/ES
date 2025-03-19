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
            <!-- 导入方式选择 -->
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
                ref="userForm"
                :model="userForm"
                :rules="userRules"
                label-width="100px"
              >
                <el-form-item 
                  :label="userType === 'student' ? '学号' : '工号'"
                  prop="userId"
                >
                  <el-input v-model="userForm.userId" />
                </el-form-item>
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="userForm.name" />
                </el-form-item>
                <el-form-item label="学院" prop="department">
                  <el-select v-model="userForm.department" placeholder="请选择学院">
                    <el-option label="计算机学院" value="计算机学院" />
                    <el-option label="信息学院" value="信息学院" />
                    <!-- 其他学院选项 -->
                  </el-select>
                </el-form-item>
                <el-form-item 
                  label="班级" 
                  prop="class"
                  v-if="userType === 'student'"
                >
                  <el-input v-model="userForm.class" />
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

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 文件上传相关
const fileInfo = ref(null)
const importing = ref(false)
const importProgress = ref(0)

// 用户类型选择
const userType = ref('student')

// 表单数据
const userForm = reactive({
  userId: '',
  name: '',
  department: '',
  class: ''
})

// 表单验证规则
const userRules = {
  userId: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' },
    { pattern: /^\d+$/, message: '请输入正确的学号/工号格式', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ],
  class: [
    { required: true, message: '请输入班级', trigger: 'blur' }
  ]
}

// 导入日志
const importLogs = ref([
  {
    time: '2024-03-20 15:30:00',
    type: '批量导入',
    status: 'success',
    description: '成功导入50条学生信息'
  },
  {
    time: '2024-03-20 15:28:00',
    type: '手动添加',
    status: 'success',
    description: '添加导员：张三'
  }
])

// 下载模板
const downloadTemplate = () => {
  // TODO: 实现模板下载
  ElMessage.success('模板下载中...')
}

// 处理文件选择
const handleFileChange = (file) => {
  fileInfo.value = file
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
    
    // 模拟导入进度
    const timer = setInterval(() => {
      if (importProgress.value < 90) {
        importProgress.value += 10
      }
    }, 300)

    // TODO: 实际的文件上传和处理逻辑
    await new Promise(resolve => setTimeout(resolve, 3000))
    
    clearInterval(timer)
    importProgress.value = 100
    
    ElMessage.success('导入成功')
    addImportLog('批量导入', 'success', '成功导入学生信息')
    
    // 重置状态
    setTimeout(() => {
      importing.value = false
      fileInfo.value = null
      importProgress.value = 0
    }, 1000)
  } catch (error) {
    ElMessage.error('导入失败')
    addImportLog('批量导入', 'error', '导入失败', error.message)
  }
}

// 提交表单
const submitForm = async () => {
  try {
    // TODO: 调用添加用户API
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success('添加成功')
    addImportLog('手动添加', 'success', 
      `添加${userType.value === 'student' ? '学生' : '导员'}：${userForm.name}`
    )
    resetForm()
  } catch (error) {
    ElMessage.error('添加失败')
    addImportLog('手动添加', 'error', '添加失败', error.message)
  }
}

// 重置表单
const resetForm = () => {
  Object.keys(userForm).forEach(key => {
    userForm[key] = ''
  })
}

// 添加导入日志
const addImportLog = (type, status, description, error = '') => {
  importLogs.value.unshift({
    time: new Date().toLocaleString(),
    type,
    status,
    description,
    error
  })
}

// 刷新日志
const refreshLogs = async () => {
  // TODO: 调用获取日志API
  await new Promise(resolve => setTimeout(resolve, 500))
  ElMessage.success('日志已刷新')
}

// 进度条格式化
const progressFormat = (percentage) => {
  return percentage === 100 ? '完成' : `${percentage}%`
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
  overflow-y: auto;
}

.page-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 180px);
  overflow: hidden;
}

.main-section {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

.import-card,
.manual-card {
  margin-bottom: 20px;
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
  padding: 10px 0;
}

.file-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.log-section {
  width: 350px;
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

.log-description {
  margin: 5px 0;
  color: #666;
}

.log-error {
  color: #f56c6c;
  font-size: 12px;
  margin: 5px 0;
}

/* 滚动条样式 */
.main-section::-webkit-scrollbar,
.log-section::-webkit-scrollbar {
  width: 6px;
}

.main-section::-webkit-scrollbar-thumb,
.log-section::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.main-section::-webkit-scrollbar-track,
.log-section::-webkit-scrollbar-track {
  background: #f5f7fa;
}
</style> 