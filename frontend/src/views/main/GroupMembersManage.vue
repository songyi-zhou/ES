<template>
  <div class="manage-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综测小组成员管理</h2>
          <button class="add-btn" @click="showAddMemberDialog">
            <i class="el-icon-plus"></i>
            为组员指定班级
          </button>
        </div>

        <!-- 筛选区域 -->
        <div class="filter-section">
          <div class="filter-item">
            <label>负责专业：</label>
            <select v-model="selectedMajor" class="filter-select" @change="handleMajorFilterChange">
              <option value="">全部专业</option>
              <option v-for="major in majors" 
                      :key="major" 
                      :value="major">
                {{ major }}
              </option>
            </select>
          </div>
          
          <div class="filter-item">
            <button class="reset-btn" @click="resetFilters">重置筛选</button>
          </div>
        </div>

        <!-- 成员列表 -->
        <div class="table-container">
          <div v-if="loading" class="loading">
            <div class="spinner"></div>
            <span>加载中...</span>
          </div>
          <table v-else class="members-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>学院</th>
                <th>专业</th>
                <th>负责班级</th>
                <th>中队</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in members" :key="member.id" class="member-row">
                <td>{{ member.name }}</td>
                <td>{{ member.department }}</td>
                <td>{{ member.major }}</td>
                <td>{{ member.classId }}</td>
                <td>{{ member.squad }}</td>
                <td>{{ formatDateTime(member.createdAt) }}</td>
                <td>
                  <div class="button-container">
                    <el-button 
                      type="primary" 
                      size="small" 
                      @click="editMember(member)"
                      class="operation-button"
                    >
                      修改
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="confirmDelete(member)"
                      class="operation-button"
                    >
                      删除
                    </el-button>
                  </div>
                </td>
              </tr>
              <tr v-if="members.length === 0">
                <td colspan="7" class="no-data">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 添加成员弹窗 -->
        <el-dialog
          v-model="showAddModal"
          title="请分配班级"
          width="500px"
          custom-class="custom-dialog"
        >
          <div class="form-container">
            <div class="form-group">
              <label for="member">姓名</label>
              <select 
                id="member"
                v-model="memberForm.memberId"
                class="form-control"
                @change="handleMemberSelectChange"
              >
                <option value="">请选择成员</option>
                <option
                  v-for="member in availableMembers"
                  :key="member.id"
                  :value="member.id"
                >
                  {{ member.name }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="major">专业</label>
              <select 
                id="major"
                v-model="memberForm.major"
                class="form-control"
                @change="handleMajorChange"
              >
                <option value="">请选择专业</option>
                <option
                  v-for="major in majors"
                  :key="major"
                  :value="major"
                >
                  {{ major }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="className">班级</label>
              <select 
                id="className"
                v-model="memberForm.className"
                class="form-control"
              >
                <option value="">请选择班级</option>
                <option
                  v-for="cls in filteredClasses"
                  :key="cls.id"
                  :value="cls.id"
                >
                  {{ cls.name }}
                </option>
              </select>
            </div>
          </div>
          
          <div class="dialog-footer">
            <button class="btn btn-cancel" @click="closeModal">取消</button>
            <button class="btn btn-primary" @click="assignClass">确定</button>
          </div>
        </el-dialog>

        <!-- 编辑成员弹窗 -->
        <el-dialog
          v-model="showEditModal"
          title="修改班级信息"
          width="500px"
          custom-class="custom-dialog"
        >
          <div class="form-container">
            <div class="form-group">
              <label for="edit-major">专业</label>
              <select 
                id="edit-major"
                v-model="memberForm.major"
                class="form-control"
                @change="handleMajorChange"
              >
                <option value="">请选择专业</option>
                <option
                  v-for="major in majors"
                  :key="major"
                  :value="major"
                >
                  {{ major }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="edit-className">班级</label>
              <select 
                id="edit-className"
                v-model="memberForm.className"
                class="form-control"
              >
                <option value="">请选择班级</option>
                <option
                  v-for="cls in filteredClasses"
                  :key="cls.id"
                  :value="cls.id"
                >
                  {{ cls.name }}
                </option>
              </select>
            </div>
          </div>
          
          <div class="dialog-footer">
            <button class="btn btn-cancel" @click="closeModal">取消</button>
            <button class="btn btn-primary" @click="submitMember">确定</button>
          </div>
        </el-dialog>

        <!-- 删除确认弹窗 -->
        <div v-if="showDeleteModal" class="modal-overlay">
          <div class="modal-content">
            <div class="modal-header">
              <h3>确认删除</h3>
              <button class="close-btn" @click="showDeleteModal = false">×</button>
            </div>
            <div class="confirm-message">
              确定要删除 {{ selectedMember?.name }} 的信息吗？此操作不可撤销。
            </div>
            <div class="modal-actions">
              <button @click="deleteMember" class="btn-danger">确认删除</button>
              <button @click="showDeleteModal = false" class="btn-secondary">取消</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch, onBeforeUnmount } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/format'

// 筛选条件
const selectedClass = ref('')
const selectedMajor = ref('')

// 成员数据
const members = ref([])
const filteredMembers = ref([])
const loading = ref(false)

// 在其他 ref 变量附近添加
const selectedMemberId = ref(null)

// 获取成员列表
const fetchMembers = async () => {
  if (!isComponentMounted.value) return
  loading.value = true
  
  try {
    const response = await request.get('/class-group-members')
    console.log('API Response:', response)
    
    if (response.data.success && isComponentMounted.value) {
      members.value = response.data.data || []
      console.log('Members data:', members.value)
    }
  } catch (error) {
    if (isComponentMounted.value) {
      console.error('获取成员列表失败:', error)
      alert('获取成员列表失败')
    }
  } finally {
    if (isComponentMounted.value) {
      loading.value = false
    }
  }
}

// 筛选成员
const filterMembers = () => {
  filteredMembers.value = members.value.filter(member => {
    // 专业筛选
    const majorMatch = !selectedMajor.value || member.major === selectedMajor.value
    // 班级筛选
    const classMatch = !selectedClass.value || member.classId === selectedClass.value
    return majorMatch && classMatch
  })
}

// 重置筛选条件
const resetFilters = () => {
  selectedMajor.value = ''
  selectedClass.value = ''
}

// 专业筛选变化时重置班级筛选
const handleMajorFilterChange = () => {
  selectedClass.value = ''
}

// 根据选择的专业更新可选的班级列表
const availableClasses = computed(() => {
  if (!selectedMajor.value) return classes.value
  return classes.value.filter(c => c.major === selectedMajor.value)
})

const classes = ref([])
const memberForm = ref({
  memberId: '',
  major: '',
  className: ''  // 确保初始值为空字符串而不是 undefined
})
const selectedMemberInfo = ref({
  className: '',
  major: ''
})

//获取待分配学生数据
const Members = async () => {
  try {
    const response = await request.get('/class-group-members/available')
    if (response.data.success) {
      classes.value = response.data.data
      filterClasses()
    }
  } catch (error) {
    console.error('获取students列表失败:', error)
    ElMessage.error('获取students列表失败')
  }
}

// 获取班级数据
const fetchClasses = async () => {
  try {
    const response = await request.get('/classes')
    if (response.data.success) {
      classes.value = response.data.data
      filterClasses()
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
    ElMessage.error('获取班级列表失败')
  }
}

const majors = ref<string[]>([])

// 获取专业列表
const fetchMajors = async () => {
  try {
    const response = await request.get('/classes/majors')
    if (response.data.success) {
      majors.value = response.data.data
    }
  } catch (error) {
    console.error('获取专业列表失败:', error)
    ElMessage.error('获取专业列表失败')
  }
}

// 将 computed 属性改为普通的 ref
const filteredClasses = ref([])

// 修改 filterClasses 函数
const filterClasses = () => {
  if (memberForm.value.major) {
    filteredClasses.value = classes.value.filter(c => c.major === memberForm.value.major)
  } else {
    filteredClasses.value = []
  }
}

// 专业变更时重置班级选择并获取相应班级列表
const handleMajorChange = async () => {
  memberForm.value.className = ''  // 重置为空字符串
  await fetchClassesByMajor(memberForm.value.major)
}

// 添加获取班级列表的函数
const fetchClassesByMajor = async (major) => {
  if (!major) return
  
  try {
    const response = await request.get(`/classes/by-major?major=${major}`)
    if (response.data.success) {
      filteredClasses.value = response.data.data
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
  }
}

// 模态框控制
const showAddModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const selectedMember = ref(null)
const isEditing = ref(false)

// 打开编辑模态框
const editMember = (member) => {
  console.log('编辑成员:', member)
  
  selectedMember.value = member
  memberForm.value = {
    memberId: member.userId || member.id,
    major: member.major,
    className: member.classId
  }
  
  console.log('设置表单值:', memberForm.value)
  
  isEditing.value = true
  showEditModal.value = true
  
  if (member.major) {
    handleMajorChange()
  }
}

// 确认删除
const confirmDelete = (member) => {
  console.log('确认删除成员:', member)
  
  selectedMember.value = member
  showDeleteModal.value = true
}

// 关闭模态框
const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  showDeleteModal.value = false
  
  memberForm.value = {
    memberId: '',
    className: '',
    major: ''
  }
  
  selectedMember.value = null
  isEditing.value = false
}

// 提交表单
const submitMember = async () => {
  console.log('提交表单:', memberForm.value)
  
  if (!memberForm.value.major || !memberForm.value.className) {
    ElMessage.error('请选择专业和班级')
    return
  }

  try {
    if (isEditing.value && selectedMember.value) {
      // 记录表单数据，用于调试
      console.log('提交的表单数据:', {
        major: memberForm.value.major,
        className: memberForm.value.className
      })
      
      // 使用正确的API端点 - 使用 group-members/{id}/class
      const response = await request.put(
        `/group-members/${selectedMember.value.id}/class`,
        null,
        {
          params: {
            major: memberForm.value.major,
            className: memberForm.value.className
          }
        }
      )
      
      console.log('响应数据:', response.data)
      
      if (response.data.success) {
        ElMessage.success('修改成功')
        await fetchMembers()
        closeModal()
      }
    } else {
      // 添加新成员
      const response = await request.post('/group-members', {
        major: memberForm.value.major,
        className: memberForm.value.className
      })
      
      if (response.data.success) {
        ElMessage.success('添加成功')
        await fetchMembers()
        closeModal()
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 删除成员
const deleteMember = async () => {
  try {
    // 使用DELETE请求删除成员
    const response = await request.delete(
      `/group-members-manage/${selectedMember.value.id}`
    )
    
    if (response.data.success) {
      ElMessage.success('删除成功')
      await fetchMembers()
      showDeleteModal.value = false
      selectedMember.value = null
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

// 监听筛选条件变化
watch([selectedMajor, selectedClass], () => {
  filterMembers()
})

const availableMembers = ref([])

// 获取可选成员列表
const fetchAvailableMembers = async () => {
  try {
    const response = await request.get('/class-group-members/available')
    if (response.data.success) {
      availableMembers.value = response.data.data
    }
  } catch (error) {
    console.error('获取可选成员失败:', error)
    ElMessage.error('获取可选成员失败')
  }
}

// 重命名新的函数为 handleMemberSelectChange
const handleMemberSelectChange = (event) => {
  const memberId = event.target.value
  selectedMemberId.value = memberId
  
  const selectedMember = availableMembers.value.find(member => member.id === memberId)
  if (selectedMember) {
    memberForm.value = {
      memberId: selectedMember.id,
      major: selectedMember.major || '',
      className: ''
    }
  }
}

// 打开添加弹窗时获取可选成员
const showAddMemberDialog = () => {
  showAddModal.value = true
  fetchAvailableMembers()
  fetchMajors()
  fetchClasses()
  memberForm.value = { 
    memberId: '',
    major: '',
    className: ''
  }
}

// 显示分配班级对话框
const showAssignClassDialog = (member) => {
  console.log('显示分配班级对话框，成员信息:', member)
  
  // 确保member对象存在且有id属性
  if (!member) {
    console.error('成员对象为空')
    ElMessage.error('无效的成员')
    return
  }
  
  // 保存当前选中的成员ID
  selectedMemberId.value = member.id || member.userId
  console.log('选中的成员ID:', selectedMemberId.value)
  
  if (!selectedMemberId.value) {
    console.error('成员ID为空')
    ElMessage.error('无法获取成员ID')
    return
  }
  
  // 设置表单初始值
  memberForm.value = {
    memberId: selectedMemberId.value,
    className: '',
    major: member.major || ''
  }
  
  // 显示对话框
  showAddModal.value = true
}

// 分配班级
const assignClass = async () => {
  console.log('开始分配班级，表单数据:', memberForm.value)
  console.log('班级值:', memberForm.value.className)
  
  if (!memberForm.value.className) {
    ElMessage.error('请选择班级')
    return
  }

  try {
    const response = await request.put(`/group-members/${selectedMemberId.value}/assign`, {
      className: memberForm.value.className
    })
    
    if (response.data.success) {
      ElMessage.success('分配班级成功')
      showAddModal.value = false
      await fetchMembers()
    }
  } catch (error) {
    console.error('分配班级失败:', error)
    ElMessage.error(error.response?.data?.message || '分配班级失败')
  }
}

const isComponentMounted = ref(true)

onMounted(() => {
  isComponentMounted.value = true
  fetchMembers()
  fetchMajors()
  fetchClasses()
})

onBeforeUnmount(() => {
  isComponentMounted.value = false
})
</script>

<style scoped>
.manage-container {
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
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.add-btn {
  padding: 8px 16px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.add-btn:hover {
  background: #85ce61;
}

.filter-section {
  background: white;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-select {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-width: 200px;
}

.reset-btn {
  padding: 8px 16px;
  background: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.reset-btn:hover {
  background: #a6a9ad;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.members-table {
  width: 100%;
  border-collapse: collapse;
  overflow: auto;
}

.members-table thead {
  position: sticky;
  top: 0;
  background: #f5f7fa;
  z-index: 1;
}

.members-table th,
.members-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

.members-table th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.member-row:hover {
  background-color: #f5f7fa;
}

.button-container {
  display: flex;
  gap: 8px;
}

.operation-button {
  padding: 6px 12px;
  font-size: 12px;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 24px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #dcdfe6;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 弹窗样式 */
.custom-dialog {
  border-radius: 8px;
}

.form-container {
  padding: 20px 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

.form-control {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-cancel {
  background: #f4f4f5;
  color: #909399;
}

.btn-primary {
  background: #409eff;
  color: white;
}

.btn-cancel:hover {
  background: #e9e9eb;
}

.btn-primary:hover {
  background: #66b1ff;
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

/* 删除确认弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
  width: 400px;
  max-width: 90%;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  margin: 0;
  color: #303133;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: #909399;
  cursor: pointer;
}

.confirm-message {
  margin-bottom: 20px;
  color: #606266;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-danger {
  background: #f56c6c;
  color: white;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-secondary {
  background: #909399;
  color: white;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-danger:hover {
  background: #f78989;
}

.btn-secondary:hover {
  background: #a6a9ad;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    margin-bottom: 10px;
  }
}
</style>