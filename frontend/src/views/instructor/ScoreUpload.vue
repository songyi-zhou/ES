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

            <!-- 专业选择
            <el-form-item label="专业">
              <el-select v-model="formData.major" placeholder="请选择专业">
                <el-option
                  v-for="major in majors"
                  :key="major.value"
                  :label="major.label"
                  :value="major.value"
                />
              </el-select>
            </el-form-item> -->

            <!-- 文件上传提示和上传组件 -->
            <el-form-item>
              <div class="upload-tip">
                <el-alert
                  title="请选择'单学期或学年'的成绩表"
                  type="warning"
                  :closable="false"
                  style="margin-bottom: 20px"
                >
                  <template #default>
                    <span style="font-weight: bold; color: #E6A23C">
                      '单学期或学年'
                    </span>
                    的成绩表
                  </template>
                </el-alert>
              </div>
              <el-upload
                class="upload-excel"
                :action="uploadUrl"
                :headers="headers"
                :data="formData"
                :before-upload="beforeUpload"
                :on-success="handleSuccess"
                :on-error="handleError"
                accept=".xlsx,.xls"
                :disabled="!isFormValid"
              >
                <el-button type="primary" :disabled="!isFormValid">
                  选择文件上传
                </el-button>
                <template #tip>
                  <div class="el-upload__tip">
                    只能上传 Excel 文件（.xlsx, .xls）
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-form>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import { ElMessage } from 'element-plus'
// import { API_URL } from '@/config'
import axios from 'axios'

// API基础URL
const API_URL = import.meta.env.VITE_API_URL || '';
// 表单数据
const formData = ref({
  academicYear: '',
  semester: '',
  major: ''
})

// 学年选项（可以根据需要调整年份范围）
const academicYears = ref([
  { label: '2023-2024', value: '2023-2024' },
  { label: '2024-2025', value: '2024-2025' },
  { label: '2025-2026', value: '2025-2026' }
])

// 专业选项（根据实际情况修改）
const majors = ref([])

// // 获取专业列表
// const fetchMajors = async () => {
//   try {
//     const token = localStorage.getItem('token')
//     if (!token) {
//       ElMessage.error('用户未登录，请先登录')
//       return
//     }
    
//     const response = await axios.get(`${API_URL}/api/classes/majors`, {
//       headers: {
//         'Authorization': `Bearer ${token}`
//       }
//     })
    
//     if (response.data.success) {
//       console.log('原始专业数据:', response.data.data)
//       // 转换数据格式以匹配下拉框需要的格式
//       majors.value = response.data.data.map((majorName, index) => ({
//         label: majorName,
//         value: index.toString()  // 使用索引作为value值
//       }))
//       console.log('转换后的专业数据:', majors.value)
//     } else {
//       ElMessage.error(response.data.message || '获取专业列表失败')
//     }
//   } catch (error) {
//     console.error('获取专业列表失败:', error)
//     ElMessage.error('获取专业列表失败，请检查网络连接')
//   }
// }

// 在组件挂载时获取专业列表
onMounted(() => {
  fetchMajors()
})

// 上传URL
const uploadUrl = `${API_URL}/api/instructor/scores/upload`

// 请求头（包含认证信息）
const headers = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

// 表单验证
const isFormValid = computed(() => {
  return formData.value.academicYear && 
         formData.value.semester && 
         formData.value.major
})

// 上传前验证
const beforeUpload = (file) => {
  // 验证文件类型
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                  file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件！')
    return false
  }

  // 验证文件大小（限制为 10MB）
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB！')
    return false
  }

  // 验证表单是否填写完整
  if (!isFormValid.value) {
    ElMessage.error('请先选择学年、学期和专业！')
    return false
  }

  return true
}

// 上传成功处理
const handleSuccess = (response) => {
  if (response.success) {
    ElMessage.success('成绩表上传成功！')
  } else {
    ElMessage.error(response.message || '上传失败，请重试')
  }
}

// 上传失败处理
const handleError = (error) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败，请检查网络连接或联系管理员')
}
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
  padding: 24px;
  background: #f5f7fa;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 24px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.page-header h2 {
  color: #303133;
  font-size: 24px;
  margin: 0;
}

.upload-form {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.upload-tip {
  margin-bottom: 20px;
}

.upload-excel {
  margin-top: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-select) {
  width: 100%;
  max-width: 300px;
}

.el-upload__tip {
  color: #909399;
  font-size: 14px;
  margin-top: 8px;
}
</style>
