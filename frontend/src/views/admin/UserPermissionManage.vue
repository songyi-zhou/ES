<template>
  <div class="permission-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>用户权限管理</h2>
          <div class="filter-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户名/学号"
              clearable
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="filterRole" placeholder="角色筛选" clearable>
              <el-option label="普通用户" value="user" />
              <el-option label="综测小组成员" value="groupMember" />
              <el-option label="综测小组负责人" value="groupLeader" />
              <el-option label="导员" value="instructor" />
            </el-select>
          </div>
        </div>

        <div class="table-container">
          <el-table :data="filteredUsers" style="width: 100%">
            <el-table-column prop="userId" label="学号/工号" width="120" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="department" label="院系" width="180" />
            <el-table-column prop="major" label="专业" width="180" />
            <el-table-column prop="class" label="班级" width="120" />
            <el-table-column label="当前角色" width="150">
              <template #default="{ row }">
                <el-tag :type="getRoleTagType(row.role)">
                  {{ getRoleDisplayName(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="200">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="openRoleDialog(row)"
                >
                  修改权限
                </el-button>
                <el-button
                  type="info"
                  size="small"
                  @click="viewUserDetails(row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 30, 50]"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </div>

        <!-- 修改权限对话框 -->
        <el-dialog
          v-model="roleDialogVisible"
          title="修改用户权限"
          width="500px"
        >
          <div class="user-info">
            <p><strong>用户：</strong>{{ currentUser?.name }} ({{ currentUser?.userId }})</p>
            <p><strong>当前角色：</strong>{{ getRoleDisplayName(currentUser?.role) }}</p>
          </div>
          
          <el-form :model="roleForm" label-width="100px">
            <el-form-item label="选择角色">
              <el-select v-model="roleForm.role" placeholder="请选择新角色">
                <el-option label="普通用户" value="user" />
                <el-option label="综测小组成员" value="groupMember" />
                <el-option label="综测小组负责人" value="groupLeader" />
                <el-option label="导员" value="instructor" />
              </el-select>
            </el-form-item>
            <el-form-item label="修改原因" required>
              <el-input
                v-model="roleForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入修改原因"
              />
            </el-form-item>
          </el-form>

          <template #footer>
            <div class="dialog-footer">
              <el-button @click="roleDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="submitRoleChange" :loading="submitting">
                确认修改
              </el-button>
            </div>
          </template>
        </el-dialog>

        <!-- 用户详情对话框 -->
        <el-dialog
          v-model="detailsDialogVisible"
          title="用户详情"
          width="600px"
        >
          <div class="user-details" v-if="currentUser">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="学号/工号">{{ currentUser.userId }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ currentUser.name }}</el-descriptions-item>
              <el-descriptions-item label="院系">{{ currentUser.department }}</el-descriptions-item>
              <el-descriptions-item label="专业">{{ currentUser.major }}</el-descriptions-item>
              <el-descriptions-item label="班级">{{ currentUser.class }}</el-descriptions-item>
              <el-descriptions-item label="当前角色">
                {{ getRoleDisplayName(currentUser.role) }}
              </el-descriptions-item>
              <el-descriptions-item label="最近登录">{{ currentUser.lastLogin }}</el-descriptions-item>
              <el-descriptions-item label="账号状态">
                <el-tag :type="currentUser.status === 'active' ? 'success' : 'danger'">
                  {{ currentUser.status === 'active' ? '正常' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>

            <div class="role-history">
              <h4>权限变更历史</h4>
              <el-timeline>
                <el-timeline-item
                  v-for="(history, index) in currentUser.roleHistory"
                  :key="index"
                  :timestamp="history.time"
                  :type="history.type"
                >
                  {{ history.description }}
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </el-dialog>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 状态数据
const searchKeyword = ref('')
const filterRole = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const roleDialogVisible = ref(false)
const detailsDialogVisible = ref(false)
const currentUser = ref(null)
const submitting = ref(false)

const roleForm = ref({
  role: '',
  reason: ''
})

// 模拟用户数据
const users = ref([
  {
    userId: '2021001001',
    name: '张三',
    department: '计算机科学与技术学院',
    major: '计算机科学与技术',
    class: '2021-1',
    role: 'user',
    status: 'active',
    lastLogin: '2024-03-20 14:30:00',
    roleHistory: [
      {
        time: '2024-03-15 10:00:00',
        type: 'success',
        description: '被授予综测小组成员权限'
      }
    ]
  }
  // ... 其他用户数据
])

// 计算属性：过滤后的用户列表
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchKeyword = searchKeyword.value ? 
      (user.name.includes(searchKeyword.value) || 
       user.userId.includes(searchKeyword.value)) : true
    const matchRole = filterRole.value ? 
      user.role === filterRole.value : true
    return matchKeyword && matchRole
  })
})

// 获取角色显示名称
const getRoleDisplayName = (role) => {
  const roleMap = {
    user: '普通用户',
    groupMember: '综测小组成员',
    groupLeader: '综测小组负责人',
    instructor: '导员'
  }
  return roleMap[role] || role
}

// 获取角色标签类型
const getRoleTagType = (role) => {
  const typeMap = {
    user: '',
    groupMember: 'success',
    groupLeader: 'warning',
    instructor: 'danger'
  }
  return typeMap[role] || ''
}

// 打开修改权限对话框
const openRoleDialog = (user) => {
  currentUser.value = user
  roleForm.value.role = user.role
  roleForm.value.reason = ''
  roleDialogVisible.value = true
}

// 查看用户详情
const viewUserDetails = (user) => {
  currentUser.value = user
  detailsDialogVisible.value = true
}

// 提交权限修改
const submitRoleChange = async () => {
  if (!roleForm.value.role || !roleForm.value.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    submitting.value = true
    // TODO: 调用修改权限API
    // const response = await updateUserRole({
    //   userId: currentUser.value.userId,
    //   newRole: roleForm.value.role,
    //   reason: roleForm.value.reason
    // })
    
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    ElMessage.success('权限修改成功')
    roleDialogVisible.value = false
    
    // 更新用户数据
    const userIndex = users.value.findIndex(u => u.userId === currentUser.value.userId)
    if (userIndex !== -1) {
      users.value[userIndex].role = roleForm.value.role
      users.value[userIndex].roleHistory.unshift({
        time: new Date().toLocaleString(),
        type: 'success',
        description: `角色更改为${getRoleDisplayName(roleForm.value.role)}`
      })
    }
  } catch (error) {
    ElMessage.error('权限修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.permission-container {
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

.page-header {
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  gap: 15px;
  margin-top: 15px;
}

.search-input {
  width: 250px;
}

.table-container {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.user-info {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.user-info p {
  margin: 8px 0;
}

.role-history {
  margin-top: 20px;
}

.role-history h4 {
  margin-bottom: 15px;
  color: #333;
}

:deep(.el-tag) {
  margin-right: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 