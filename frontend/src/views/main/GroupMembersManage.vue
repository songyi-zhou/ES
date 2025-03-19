<template>
  <div class="manage-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综测小组成员管理</h2>
          <button class="add-btn" @click="showAddModal = true">
            <i class="el-icon-plus"></i>
            添加成员
          </button>
        </div>

        <!-- 筛选区域 -->
        <div class="filter-section">
          <div class="filter-item">
            <label>负责班级：</label>
            <select v-model="selectedClass" class="filter-select">
              <option value="">计算机科学与技术</option>
              <option value="1">1班</option>
              <option value="2">2班</option>
              <option value="3">3班</option>
            </select>
          </div>
        </div>

        <!-- 成员列表 -->
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>负责班级</th>
                <th>上级</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in members" :key="member.id">
                <td>{{ member.name }}</td>
                <td>{{ member.class }}</td>
                <td>{{ member.supervisor }}</td>
                <td class="actions">
                  <button class="edit-btn" @click="editMember(member)">修改</button>
                  <button class="delete-btn" @click="confirmDelete(member)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 添加/编辑成员弹窗 -->
        <div v-if="showAddModal || showEditModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>{{ showEditModal ? '修改成员信息' : '添加新成员' }}</h3>
              <button class="close-btn" @click="closeModal">×</button>
            </div>
            <div class="form-group">
              <label>选择成员：</label>
              <select v-model="memberForm.memberId">
                <option value="">请选择成员</option>
                <option 
                  v-for="member in availableMembers" 
                  :key="member.id" 
                  :value="member.id"
                >
                  {{ member.name }} ({{ member.studentId }})
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>专业：</label>
              <select v-model="memberForm.majorId">
                <option value="">请选择专业</option>
                <option 
                  v-for="major in majors" 
                  :key="major.id" 
                  :value="major.id"
                >
                  {{ major.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>班级：</label>
              <select v-model="memberForm.class" :disabled="!memberForm.majorId">
                <option value="">请选择班级</option>
                <option 
                  v-for="class_ in filteredClasses" 
                  :key="class_.id" 
                  :value="class_.name"
                >
                  {{ class_.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>上级：</label>
              <input 
                type="text" 
                v-model="memberForm.supervisor"
                placeholder="请输入上级姓名"
              >
            </div>
            <div class="modal-actions">
              <button @click="submitMember" class="btn-primary">
                {{ showEditModal ? '保存修改' : '添加' }}
              </button>
              <button @click="closeModal" class="btn-secondary">取消</button>
            </div>
          </div>
        </div>

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

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 筛选条件
const selectedClass = ref('')

// 成员数据
const members = ref([
  {
    id: 1,
    name: '张三',
    class: '计算机科学与技术1班',
    supervisor: 'C'
  },
  // 更多成员数据...
])

// 添加专业数据
const majors = ref([
  { id: 1, name: '计算机科学与技术' },
  { id: 2, name: '软件工程' }
])

// 表单数据 (删除重复声明，合并所有需要的字段)
const memberForm = ref({
  memberId: '',
  name: '',
  majorId: '',
  class: '',
  supervisor: ''
})

// 获取可选成员列表
const availableMembers = ref([])

// 模态框控制
const showAddModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const selectedMember = ref(null)

// 打开编辑模态框
const editMember = (member) => {
  selectedMember.value = member
  memberForm.value = { ...member }
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
    name: '',
    majorId: '',
    class: '',
    supervisor: ''
  }
  selectedMember.value = null
}

// 提交表单
const submitMember = async () => {
  try {
    if (showEditModal.value) {
      // 调用编辑API
      // await fetch(\`/api/members/\${selectedMember.value.id}\`, {
      //   method: 'PUT',
      //   body: JSON.stringify(memberForm.value)
      // })
      
      // 更新本地数据
      const index = members.value.findIndex(m => m.id === selectedMember.value.id)
      if (index !== -1) {
        members.value[index] = { ...selectedMember.value, ...memberForm.value }
      }
    } else {
      // 调用添加API
      // const response = await fetch('/api/members', {
      //   method: 'POST',
      //   body: JSON.stringify(memberForm.value)
      // })
      // const newMember = await response.json()
      
      // 添加到本地数据
      members.value.push({
        id: Date.now(), // 临时ID
        ...memberForm.value
      })
    }
    
    closeModal()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 删除成员
const deleteMember = async () => {
  try {
    // 调用删除API
    // await fetch(\`/api/members/\${selectedMember.value.id}\`, {
    //   method: 'DELETE'
    // })
    
    // 更新本地数据
    members.value = members.value.filter(m => m.id !== selectedMember.value.id)
    showDeleteModal.value = false
    selectedMember.value = null
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 在组件挂载时获取可选成员
onMounted(async () => {
  try {
    // 获取已选定的综测小组成员池
    // const response = await fetch('/api/evaluation-group/available-members')
    // availableMembers.value = await response.json()
  } catch (error) {
    console.error('获取可选成员失败:', error)
  }
})

// 计算属性
const filteredClasses = computed(() => {
  if (!memberForm.value.majorId) return []
  return classes.value.filter(c => c.majorId === memberForm.value.majorId)
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
  margin-bottom: 20px;
  display: flex;
  gap: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-select {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-width: 200px;
  font-size: 14px;
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
</style> 