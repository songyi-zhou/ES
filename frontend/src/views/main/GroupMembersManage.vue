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
          <el-table :data="members" border stripe>
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="className" label="负责班级" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="department" label="学院" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editMember(row)">
                  修改
                </el-button>
                <el-button type="danger" size="small" @click="confirmDelete(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 添加成员弹窗 -->
        <el-dialog
          v-model="showAddModal"
          title="请分配班级"
          width="500px"
        >
          <el-form ref="memberForm" :model="memberForm" label-width="80px">
            <el-form-item label="姓名">
              <select 
                v-model="memberForm.memberId"
                class="form-select"
                @change="handleMemberSelect"
              >
                <option value="">请选择成员</option>
                <option
                  v-for="member in availableMembers"
                  :key="member.userId"
                  :value="member.userId"
                >
                  {{ member.name }}
                </option>
              </select>
            </el-form-item>
            
            <el-form-item label="专业">
              <select 
                v-model="memberForm.major"
                class="form-select"
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
            </el-form-item>
            
            <el-form-item label="班级">
              <select 
                v-model="memberForm.className"
                class="form-select"
              >
                <option value="">请选择班级</option>
                <option
                  v-for="cls in filteredClasses"
                  :key="cls.classId"
                  :value="cls.classId"
                >
                  {{ cls.name }}
                </option>
              </select>
            </el-form-item>
          </el-form>
          
          <template #footer>
            <span class="dialog-footer">
              <button class="btn-cancel" @click="showAddModal = false">取消</button>
              <button class="btn-primary" @click="handleAdd">分配</button>
            </span>
          </template>
        </el-dialog>

        <!-- 删除确认弹窗 -->
        <div v-if="showDeleteModal" class="modal">
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
import { ref, computed, onMounted, reactive, watch } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// 筛选条件
const selectedClass = ref('')
const selectedMajor = ref('')

// 成员数据
const members = ref([])
const filteredMembers = ref([])

// 获取成员列表
const fetchMembers = async () => {
  try {
    const response = await request.get('/class-group-members')
    if (response.data.success) {
      members.value = response.data.data
    }
  } catch (error) {
    console.error('获取成员列表失败:', error)
    ElMessage.error('获取成员列表失败')
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
  className: '',
  major: ''
})
const selectedMemberInfo = ref({
  className: '',
  major: ''
})

// 获取班级数据
const fetchClasses = async () => {
  try {
    const response = await request.get('/classes')
    if (response.data.success) {
      classes.value = response.data.data
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

// 根据选择的专业筛选班级
const filteredClasses = computed(() => {
  if (!memberForm.value.major) return []
  return classes.value.filter(c => c.major === memberForm.value.major)
})

// 专业变更时重置班级选择
const handleMajorChange = () => {
  memberForm.value.className = ''
}

// 模态框控制
const showAddModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const selectedMember = ref(null)
const isEditing = ref(false)

// 打开编辑模态框
const editMember = (member) => {
  selectedMember.value = member
  memberForm.value = {
    memberId: member.userId,
    major: member.major,
    className: member.classId
  }
  showEditModal.value = true
}

// 确认删除
const confirmDelete = (member) => {
  selectedMember.value = member
  showDeleteModal.value = true
}

// 关闭模态框
const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  memberForm.value = {
    memberId: '',
    className: '',
    major: ''
  }
  selectedMember.value = null
}

// 提交表单
const submitMember = async () => {
  if (!memberForm.value.major || !memberForm.value.className) {
    ElMessage.error('请选择专业和班级')
    return
  }

  try {
    if (showEditModal.value) {
      // 找到选中班级的完整信息
      const selectedClass = classes.value.find(c => c.name === memberForm.value.className)
      
      const response = await request.put(
        `/group-members/${selectedMember.value.id}/class`,
        null,
        { 
          params: {
            major: memberForm.value.major,
            className: selectedClass?.classId  // 使用数据库中的班级ID
          }
        }
      )
      if (response.data) {
        ElMessage.success('修改成功')
        await fetchMembers()
        closeModal()
      }
    } else {
      const token = localStorage.getItem('token')
      const params = {
        major: memberForm.value.major,
        className: memberForm.value.className
      }

      const response = await request.post('/api/group-members', params, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      if (response.data.success) {
        ElMessage.success('添加成功')
        await fetchMembers()
        closeModal()
      }
    }
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 删除成员
const deleteMember = async () => {
  try {
    const token = localStorage.getItem('token');
    const response = await request.delete(
      `/api/group-members-manage/${selectedMember.value.id}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`  // 确保添加 token
        }
      }
    );
    if (response.data.success) {
      ElMessage.success('删除成功');
      await fetchMembers();
      showDeleteModal.value = false;
      selectedMember.value = null;
    }
  } catch (error) {
    console.error('删除失败:', error);
    ElMessage.error('删除失败');
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

// 选择成员时更新信息
const handleMemberSelect = (userId) => {
  const selected = availableMembers.value.find(member => member.userId === userId)
  if (selected) {
    selectedMemberInfo.value = {
      className: selected.className,
      major: selected.major
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

const handleAdd = async () => {
  if (!memberForm.value.memberId) {
    ElMessage.error('请选择成员')
    return
  }

  try {
    const response = await request.post('/class-group-members/batch', {
      memberIds: [memberForm.value.memberId]
    })
    if (response.data.success) {
      ElMessage.success('添加成功')
      showAddModal.value = false
      await fetchMembers()
    }
  } catch (error) {
    console.error('添加失败:', error)
    ElMessage.error('添加失败')
  }
}

onMounted(() => {
  fetchMembers()
  fetchMajors()
  fetchClasses()
})
</script>

<style scoped>
.manage-container {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 15px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.add-btn:hover {
  background: #66b1ff;
}

.filter-section {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-item label {
  white-space: nowrap;
  font-size: 14px;
  color: #606266;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-width: 150px;
  font-size: 14px;
  background-color: white;
  cursor: pointer;
}

.filter-select:focus {
  outline: none;
  border-color: #409eff;
}

.reset-btn {
  padding: 8px 15px;
  background: #f4f4f5;
  color: #606266;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.reset-btn:hover {
  background: #e9e9eb;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.edit-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.edit-btn {
  background: #409eff;
  color: white;
}

.delete-btn {
  background: #f56c6c;
  color: white;
}

.edit-btn:hover {
  background: #66b1ff;
}

.delete-btn:hover {
  background: #f78989;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 20px;
}

/* 弹窗样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
}

.close-btn:hover {
  color: #606266;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary {
  padding: 8px 15px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-secondary {
  padding: 8px 15px;
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.btn-danger {
  padding: 8px 15px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.confirm-message {
  margin: 20px 0;
  color: #606266;
  text-align: center;
}

.btn-primary:hover {
  background: #66b1ff;
}

.btn-secondary:hover {
  background: #f9f9fa;
}

.btn-danger:hover {
  background: #f78989;
}

.form-group select {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.form-group select:disabled {
  background: #f5f7fa;
  cursor: not-allowed;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

:deep(.el-select) {
  width: 100%;
}

.form-select {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  background-color: #fff;
}

.form-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  background-color: #f5f7fa;
}

.form-input:disabled {
  cursor: not-allowed;
}

.btn-primary {
  padding: 8px 20px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-cancel {
  padding: 8px 20px;
  background-color: #fff;
  color: #606266;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-right: 10px;
  cursor: pointer;
}

.btn-primary:hover {
  background-color: #66b1ff;
}

.btn-cancel:hover {
  background-color: #f5f7fa;
}
</style>