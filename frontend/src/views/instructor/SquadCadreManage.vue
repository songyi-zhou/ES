<template>
  <div class="cadre-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>中队干部管理</h2>
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
                  <h3>手动添加干部</h3>
                </div>
              </template>
              
              <el-form 
                ref="formRef"
                :model="formData"
                :rules="formRules"
                label-width="100px"
              >
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="专业" prop="major">
                      <el-select 
                        v-model="formData.major" 
                        placeholder="请选择专业"
                        filterable
                        clearable
                        @change="handleMajorChange"
                      >
                        <el-option
                          v-for="major in majors"
                          :key="major"
                          :label="major"
                          :value="major"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="班级" prop="classId">
                      <el-select 
                        v-model="formData.classId" 
                        placeholder="请选择班级"
                        filterable
                        clearable
                        @change="handleClassChange"
                      >
                        <el-option
                          v-for="cls in filteredClasses"
                          :key="cls.id"
                          :label="cls.name"
                          :value="cls.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="学号" prop="studentId">
                      <el-select
                        v-model="formData.studentId"
                        placeholder="请选择学生"
                        clearable
                        @change="handleStudentChange"
                      >
                        <el-option
                          v-for="student in students"
                          :key="student.studentId"
                          :label="`${student.studentName} (${student.studentId})`"
                          :value="student.studentId"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="职位" prop="position">
                      <el-select 
                        v-model="formData.position" 
                        placeholder="请选择职位"
                        filterable
                        clearable
                      >
                        <el-option
                          v-for="position in positions"
                          :key="position"
                          :label="position"
                          :value="position"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-form-item label="每月加分" prop="monthlyBonus">
                  <el-input-number 
                    v-model="formData.monthlyBonus" 
                    :min="0"
                    :max="10"
                    :step="0.5"
                    :precision="2"
                    controls-position="right"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button type="primary" @click="submitForm">添加</el-button>
                  <el-button @click="resetForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>

          <!-- 当前干部列表 -->
          <div class="cadre-list-section">
            <el-card class="cadre-list-card">
              <template #header>
                <div class="card-header">
                  <h3>中队干部列表</h3>
                  <div class="header-actions">
                    <el-input
                      v-model="searchText"
                      placeholder="搜索干部信息"
                      clearable
                      @input="filterCadres"
                      style="width: 200px"
                    >
                      <template #prefix>
                        <el-icon><Search /></el-icon>
                      </template>
                    </el-input>
                    <el-button type="info" @click="refreshCadres">
                      刷新列表
                    </el-button>
                  </div>
                </div>
              </template>
              
              <el-table
                :data="filteredCadres"
                style="width: 100%"
                border
                v-loading="tableLoading"
                :height="tableHeight"
              >
                <el-table-column prop="major" label="专业" min-width="120" />
                <el-table-column prop="class_name" label="班级" min-width="120" />
                <el-table-column prop="student_name" label="姓名" min-width="100" />
                <el-table-column prop="student_id" label="学号" min-width="120" />
                <el-table-column prop="position" label="职位" min-width="100" />
                <el-table-column prop="monthly_bonus" label="每月加分" width="100">
                  <template #default="scope">
                    <span>{{ (scope.row.monthly_bonus || 0).toFixed(2) }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="update_time" label="更新时间" min-width="160">
                  <template #default="scope">
                    <span>{{ formatDate(scope.row.update_time) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="scope">
                    <el-button
                      type="danger"
                      size="small"
                      @click="removeCadre(scope.row)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import axios from 'axios'

// API基础URL
const API_URL = import.meta.env.VITE_API_URL || '';

// 文件上传相关
const fileInfo = ref(null)
const importing = ref(false)
const importProgress = ref(0)

// 表单 ref 和数据
const formRef = ref()
const formData = reactive({
  major: '',
  classId: '',
  className: '',
  studentId: '',
  studentName: '',
  position: '',
  monthlyBonus: 1.0,
  squad: '',
  department: ''
})

// 数据列表
const majors = ref([])
const classes = ref([])
const students = ref([])
const cadres = ref([])
const filteredCadres = ref([])
const tableLoading = ref(false)
const tableHeight = ref('450px')

// 搜索文本
const searchText = ref('')

// 职位选项
const positions = ref([
  '中队长',
  '副中队长',
  '学习委员',
  '生活委员',
  '心理委员',
  '体育委员',
  '宣传委员',
  '文艺委员',
  '纪律委员',
  '组织委员'
])

// 过滤后的班级列表
const filteredClasses = computed(() => {
  if (!formData.major) return classes.value
  return classes.value.filter(cls => cls.major === formData.major)
})

// 过滤后的学生列表
const filteredStudents = computed(() => {
  if (!formData.classId) return students.value
  return students.value.filter(student => student.classId === formData.classId)
})

// 表单验证规则
const formRules = reactive({
  major: [
    { required: true, message: '请选择专业', trigger: 'change' }
  ],
  classId: [
    { required: true, message: '请选择班级', trigger: 'change' }
  ],
  studentId: [
    { required: true, message: '请选择学生', trigger: 'change' }
  ],
  position: [
    { required: true, message: '请选择职位', trigger: 'change' }
  ],
  monthlyBonus: [
    { required: true, message: '请输入每月加分数额', trigger: 'change' },
    { type: 'number', min: 0, max: 10, message: '加分数额必须在0-10之间', trigger: 'change' }
  ]
})

// 下载模板
const downloadTemplate = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      return
    }
    
    const response = await axios.get(`${API_URL}/api/instructor/squad-cadre/template`, {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '中队干部导入模板.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('模板下载失败:', error)
    ElMessage.error('模板下载失败')
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  fileInfo.value = file.raw
}

// 导入Excel
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

    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      importing.value = false
      return
    }

    // 打印请求信息，便于调试
    console.log('准备上传文件:', fileInfo.value.name)
    console.log('上传地址:', `${API_URL}/api/instructor/squad-cadre/import-excel`)

    // 使用fetch API代替axios，处理multipart/form-data
    const response = await fetch(`${API_URL}/api/instructor/squad-cadre/import-excel`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
        // 不要设置Content-Type，让浏览器自动设置boundary
      },
      body: formData
    })

    // 获取响应
    const data = await response.json()
    
    if (response.ok) {
      ElMessage.success(`成功导入${data.count || 0}条记录`)
      fileInfo.value = null
      importProgress.value = 100
      // 刷新干部列表
      fetchCadres()
      
      // 如果有导入错误，显示警告
      if (data.errors && data.errors.length > 0) {
        importErrors.value = data.errors
        ElMessage({
          type: 'warning',
          message: `有${data.errors.length}条记录导入失败`,
          duration: 5000
        })
        console.warn('导入错误:', data.errors)
      }
    } else {
      throw new Error(data.message || '导入失败')
    }
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败，请检查文件格式')
  } finally {
    importing.value = false
  }
}

// 获取专业列表
const fetchMajors = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      return
    }
    
    const response = await axios.get(`${API_URL}/api/classes/majors`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (response.data.success) {
      majors.value = response.data.data
    } else {
      console.error('获取专业列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取专业列表失败:', error)
  }
}

// 获取班级列表
const fetchClasses = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      return
    }
    
    const response = await axios.get(`${API_URL}/api/classes/by-major`, {
      params: { major: formData.major },
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (response.data.success) {
      classes.value = response.data.data
    } else {
      console.error('获取班级列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 获取学生列表
const fetchStudents = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      return
    }
    
    const response = await axios.get(`${API_URL}/api/instructor/squad-cadre/students`, {
      params: { 
        className: formData.className,
      },
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (response.data && response.data.success) {
      students.value = response.data.data.map(student => ({
        studentId: student.student_id,
        studentName: student.name
      }))
      console.log('处理后的学生数据:', students.value)
    } else {
      console.error('获取学生列表失败')
      ElMessage.error('获取学生列表失败')
    }
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败: ' + error.message)
  }
}

// 获取中队干部列表
const fetchCadres = async () => {
  try {
    tableLoading.value = true;
    
    const token = localStorage.getItem('token');
    if (!token) {
      ElMessage.error('用户未登录，请先登录');
      return;
    }
    
    const response = await axios.get(`${API_URL}/api/instructor/squad-cadre/list`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (response.data.success) {
      cadres.value = response.data.data;
      filteredCadres.value = [...cadres.value];
      console.log('获取的干部数据:', cadres.value);
    } else {
      console.error('获取中队干部列表失败:', response.data.message);
    }
  } catch (error) {
    console.error('获取中队干部列表失败:', error);
  } finally {
    tableLoading.value = false;
  }
}

// 处理专业变化
const handleMajorChange = () => {
  formData.classId = ''
  formData.className = ''
  formData.studentId = ''
  formData.studentName = ''
  if (formData.major) {
    fetchClasses()
  }
}

// 处理班级变化
const handleClassChange = () => {
  formData.studentId = ''
  formData.studentName = ''
  if (formData.classId) {
    const selectedClass = classes.value.find(cls => cls.id === formData.classId)
    formData.className = selectedClass ? selectedClass.name : ''
    fetchStudents()
  }
}

// 处理学生变化
const handleStudentChange = () => {
  if (formData.studentId) {
    const selectedStudent = students.value.find(s => s.studentId === formData.studentId)
    if (selectedStudent) {
      formData.studentName = selectedStudent.studentName
    }
  } else {
    formData.studentName = ''
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      return
    }
    
    const response = await axios.post(`${API_URL}/api/instructor/squad-cadre/add`, formData, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    if (response.data.success) {
      ElMessage.success('添加干部成功')
      resetForm()
      fetchCadres()
    } else {
      throw new Error(response.data.message)
    }
  } catch (error) {
    console.error('添加干部失败:', error)
    ElMessage.error(error.response?.data?.message || error.message || '添加干部失败')
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  formData.studentName = ''
  formData.className = ''
  formData.squad = ''
  formData.department = ''
}

// 刷新干部列表
const refreshCadres = () => {
  fetchCadres()
}

// 过滤干部列表
const filterCadres = () => {
  if (!searchText.value) {
    filteredCadres.value = [...cadres.value]
    return
  }
  
  const searchLower = searchText.value.toLowerCase()
  filteredCadres.value = cadres.value.filter(cadre => 
    cadre.major.toLowerCase().includes(searchLower) ||
    cadre.className.toLowerCase().includes(searchLower) ||
    cadre.studentName.toLowerCase().includes(searchLower) ||
    cadre.studentId.toLowerCase().includes(searchLower) ||
    cadre.position.toLowerCase().includes(searchLower)
  )
}

// 删除干部
const removeCadre = async (cadre) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${cadre.student_name} 的 ${cadre.position} 职位吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('用户未登录，请先登录')
      return
    }
    
    const response = await axios.delete(`${API_URL}/api/instructor/squad-cadre/delete/${cadre.id}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (response.data.success) {
      ElMessage.success('删除干部成功')
      fetchCadres()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除干部失败:', error)
      ElMessage.error(error.response?.data?.message || '删除干部失败')
    }
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).replace(/\//g, '-')
  } catch (error) {
    return dateStr
  }
}

// 进度条格式化
const progressFormat = (percentage) => {  
  return percentage === 100 ? '完成' : `${percentage}%`
}

// 在组件挂载时初始化数据
onMounted(() => {
  fetchMajors()
  fetchCadres()
})

// Add a new ref for import errors
const importErrors = ref([])
</script>

<style scoped>
.cadre-container {
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
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.cadre-list-section {
  flex: 1.5;
  overflow-y: auto;
}

.import-card,
.manual-card,
.cadre-list-card {
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
@media (max-width: 1200px) {
  .page-layout {
    flex-direction: column;
  }
  
  .main-section,
  .cadre-list-section {
    width: 100%;
  }
  
  .cadre-list-section {
    margin-top: 20px;
  }
}
</style> 