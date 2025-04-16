<template>
  <div class="class-management">
    <TopBar />
    <div class="content">
      <Sidebar />
      <div class="main-content">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>班级管理</span>
              <div class="header-actions">
                <el-button type="primary" @click="showAddDialog">添加班级</el-button>
                <el-button @click="showImportDialog">批量导入</el-button>
              </div>
            </div>
          </template>

          <!-- 搜索栏 -->
          <div class="search-bar">
            <el-input
              v-model="searchQuery"
              placeholder="搜索班级名称/专业/学院"
              class="search-input"
              clearable
              @clear="handleSearch"
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 班级列表 -->
          <el-table :data="filteredClasses" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="班级ID" width="120" />
            <el-table-column prop="name" label="班级名称" width="200" />
            <el-table-column prop="department" label="学院" width="150" />
            <el-table-column prop="major" label="专业" width="150" />
            <el-table-column prop="grade" label="年级" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="150">
              <template #default="{ row }">
                <el-button-group>
                  <el-button type="primary" link @click="editClass(row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button type="danger" link @click="deleteClass(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>

        <!-- 添加/编辑班级对话框 -->
        <el-dialog
          v-model="dialogVisible"
          :title="dialogType === 'add' ? '添加班级' : '编辑班级'"
          width="500px"
        >
          <el-form
            ref="classFormRef"
            :model="classForm"
            :rules="rules"
            label-width="80px"
          >
            <el-form-item label="班级ID" prop="id">
              <el-input v-model="classForm.id" :disabled="dialogType === 'edit'" />
            </el-form-item>
            <el-form-item label="班级名称" prop="name">
              <el-input v-model="classForm.name" />
            </el-form-item>
            <el-form-item label="学院" prop="department">
              <el-input v-model="classForm.department" />
            </el-form-item>
            <el-form-item label="专业" prop="major">
              <el-input v-model="classForm.major" />
            </el-form-item>
            <el-form-item label="年级" prop="grade">
              <el-input v-model="classForm.grade" />
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="dialogVisible = false">取消</el-button>
              <el-button type="primary" @click="submitForm">确定</el-button>
            </span>
          </template>
        </el-dialog>

        <!-- 批量导入对话框 -->
        <el-dialog
          v-model="importDialogVisible"
          title="批量导入班级"
          width="500px"
        >
          <div class="import-container">
            <el-upload
              class="upload-demo"
              drag
              action="#"
              :auto-upload="false"
              :http-request="handleUpload"
              :before-upload="beforeUpload"
              :on-success="handleImportSuccess"
              :on-error="handleImportError"
              accept=".xlsx,.xls"
              ref="uploadRef"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  请上传Excel文件，文件格式请参考
                  <el-link type="primary" @click="downloadTemplate">下载模板</el-link>
                </div>
              </template>
            </el-upload>
            <div class="import-actions">
              <el-button type="primary" @click="submitUpload">开始上传</el-button>
              <el-button @click="importDialogVisible = false">取消</el-button>
            </div>
          </div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Edit, Delete, UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 数据
const classes = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const dialogType = ref('add')
const classFormRef = ref(null)
const uploadRef = ref(null)

// 使用ref代替reactive来管理表单数据
const classForm = ref({
  id: '',
  name: '',
  department: '',
  major: '',
  grade: ''
})

// 表单验证规则
const rules = {
  id: [{ required: true, message: '请输入班级ID', trigger: 'blur' }],
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  department: [{ required: true, message: '请输入学院', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }]
}

// 计算属性：过滤后的班级列表
const filteredClasses = computed(() => {
  console.log('Filtering classes with query:', searchQuery.value)
  if (!searchQuery.value) return classes.value
  const query = searchQuery.value.toLowerCase()
  const filtered = classes.value.filter(item => 
    item.name.toLowerCase().includes(query) ||
    item.major.toLowerCase().includes(query) ||
    item.department.toLowerCase().includes(query)
  )
  console.log('Filtered results:', filtered)
  return filtered
})

// 方法
const fetchClasses = async () => {
  console.log('Fetching all classes')
  loading.value = true
  try {
    const response = await axios.get('/api/classes/all')
    console.log('Fetched classes:', response.data)
    classes.value = response.data.data
    total.value = classes.value.length
  } catch (error) {
    console.error('Error fetching classes:', error)
    ElMessage.error('获取班级列表失败')
  } finally {
    loading.value = false
  }
}

// 重置表单方法
const resetForm = () => {
  if (classFormRef.value) {
    classFormRef.value.resetFields()
  }
  classForm.value = {
    id: '',
    name: '',
    department: '',
    major: '',
    grade: ''
  }
}

// 更新showAddDialog方法
const showAddDialog = () => {
  console.log('Opening add dialog')
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

const showImportDialog = () => {
  console.log('Opening import dialog')
  importDialogVisible.value = true
}

const editClass = async (row) => {
  console.log('开始编辑班级，原始数据:', JSON.stringify(row, null, 2))
  
  // 设置对话框类型并打开
  dialogType.value = 'edit'
  dialogVisible.value = true
  
  // 等待对话框完全打开
  await nextTick()
  
  // 使用ref的方式设置表单数据
  classForm.value = {
    id: row.id,
    name: row.name,
    department: row.department,
    major: row.major,
    grade: row.grade
  }
  
  console.log('设置后的表单数据:', JSON.stringify(classForm.value, null, 2))
}

const deleteClass = async (row) => {
  console.log('Attempting to delete class:', row)
  try {
    await ElMessageBox.confirm('确定要删除该班级吗？', '提示', {
      type: 'warning'
    })
    console.log('Delete confirmed for class:', row.id)
    await axios.delete(`/api/classes/${row.id}`)
    ElMessage.success('删除成功')
    fetchClasses()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Error deleting class:', error)
      ElMessage.error('删除失败')
    }
  }
}

const submitForm = async () => {
  console.log('Submitting form:', classForm.value)
  try {
    if (dialogType.value === 'add') {
      console.log('Adding new class')
      await axios.post('/api/classes', classForm.value)
      ElMessage.success('添加成功')
    } else {
      console.log('Updating class:', classForm.value.id)
      await axios.put(`/api/classes/${classForm.value.id}`, classForm.value)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    fetchClasses()
  } catch (error) {
    console.error('Error submitting form:', error)
    ElMessage.error(dialogType.value === 'add' ? '添加失败' : '更新失败')
  }
}

const handleSearch = () => {
  console.log('Handling search with query:', searchQuery.value)
  currentPage.value = 1
  fetchClasses()
}

const handleSizeChange = (val) => {
  console.log('Page size changed to:', val)
  pageSize.value = val
  fetchClasses()
}

const handleCurrentChange = (val) => {
  console.log('Current page changed to:', val)
  currentPage.value = val
  fetchClasses()
}

const beforeUpload = (file) => {
  console.log('Validating file before upload:', file.name)
  const isExcel = file.type === 'application/vnd.ms-excel' || 
                  file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  if (!isExcel) {
    console.warn('Invalid file type:', file.type)
    ElMessage.error('只能上传Excel文件！')
    return false
  }
  console.log('File validation passed')
  return true
}

const handleImportSuccess = (response) => {
  console.log('Import response:', response)
  if (response.code === 200) {
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    fetchClasses()
  } else {
    console.error('Import failed:', response.message)
    ElMessage.error(response.message || '导入失败')
  }
}

const handleImportError = (error) => {
  console.error('Import error:', error)
  ElMessage.error('导入失败')
}

const downloadTemplate = () => {
  console.log('Downloading template')
  window.open('/api/classes/template', '_blank')
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

// 添加手动上传方法
const submitUpload = () => {
  uploadRef.value.submit()
}

// 自定义上传请求
const handleUpload = async (options) => {
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    
    const response = await axios.post('/api/classes/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.data.code === 200) {
      options.onSuccess(response.data)
      ElMessage.success('导入成功')
      importDialogVisible.value = false
      fetchClasses()
    } else {
      options.onError(new Error(response.data.message || '导入失败'))
    }
  } catch (error) {
    options.onError(error)
    ElMessage.error('导入失败')
  }
}

// 生命周期钩子
onMounted(() => {
  console.log('Component mounted, fetching initial data')
  fetchClasses()
})
</script>

<style scoped>
.class-management {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 20px;
  background: #f5f5f5;
  overflow: hidden;
}

.box-card {
  height: 100%;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
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

.search-bar {
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.import-container {
  padding: 20px;
}

.el-upload__tip {
  margin-top: 10px;
  color: #666;
}

.import-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style> 