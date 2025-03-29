<template>
  <div class="permission-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>学生权限管理</h2>
          <div class="filter-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学生姓名/学号"
              clearable
              class="search-input"
              @clear="handleSearchClear"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select
              v-model="selectedClass"
              placeholder="选择班级"
              clearable
              @change="handleFilterChange"
            >
              <el-option
                v-for="option in classOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-select
              v-model="filterRole"
              placeholder="角色筛选"
              clearable
              @change="handleFilterChange"
            >
              <el-option
                v-for="option in roleOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
        </div>

        <div class="table-container">
          <el-table
            v-loading="loading"
            :data="filteredUsers"
            style="width: 100%"
            border
            stripe
            highlight-current-row
          >
            <el-table-column prop="userId" label="学号" min-width="120" sortable />
            <el-table-column prop="name" label="姓名" min-width="100" sortable />
            <el-table-column prop="className" label="班级" min-width="120" sortable />
            <el-table-column label="角色" min-width="100" sortable>
              <template #default="{ row }">
                <el-tag :type="getRoleTagType(row.role)">
                  {{ getRoleDisplayName(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="squad" label="中队" min-width="100" />
            <el-table-column 
              prop="assignTime" 
              label="分配时间" 
              min-width="180" 
              sortable
              :formatter="formatDate"
            />
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
                  详情
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
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>

        <!-- 修改权限对话框 -->
        <el-dialog
          v-model="roleDialogVisible"
          title="修改学生权限"
          width="500px"
        >
          <el-form
            ref="roleFormRef"
            :model="roleForm"
            :rules="roleRules"
            label-width="80px"
          >
            <el-form-item label="学生">
              {{ currentStudent?.name }} ({{ currentStudent?.userId }})
            </el-form-item>
            <el-form-item label="当前角色">
              <el-tag>{{ getRoleDisplayName(currentStudent?.role) }}</el-tag>
            </el-form-item>
            <el-form-item label="选择角色" prop="role">
              <el-select 
                v-model="roleForm.role"
                placeholder="请选择新角色"
                style="width: 100%"
              >
                <el-option
                  v-for="role in staticRoleOptions"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="修改原因" prop="reason">
              <el-input
                v-model="roleForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入修改原因"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="roleDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="handleRoleSubmit">确认</el-button>
            </span>
          </template>
        </el-dialog>

        <!-- 用户详情对话框 -->
        <el-dialog
          v-model="detailsDialogVisible"
          title="学生详情"
          width="600px"
        >
          <div class="user-details" v-if="currentUser">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="学号">{{ currentUser.userId }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ currentUser.name }}</el-descriptions-item>
              <el-descriptions-item label="班级">{{ currentUser.className }}</el-descriptions-item>
              <el-descriptions-item label="当前角色">
                <el-tag :type="getRoleTagType(currentUser.role)">
                  {{ getRoleDisplayName(currentUser.role) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="中队">{{ currentUser.squad }}</el-descriptions-item>
              <el-descriptions-item label="授权时间">{{ currentUser.assignTime }}</el-descriptions-item>
              <el-descriptions-item label="账号状态">
                <el-tag type="success">正常</el-tag>
              </el-descriptions-item>
            </el-descriptions>

            <div class="role-history" v-if="currentUser.roleHistory && currentUser.roleHistory.length">
              <h4>权限变更历史</h4>
              <el-timeline>
                <el-timeline-item
                  v-for="(history, index) in currentUser.roleHistory"
                  :key="index"
                  :timestamp="history.time"
                  placement="top"
                >
                  <el-card>
                    <p><strong>操作人：</strong>{{ history.operator || '系统' }}</p>
                    <p><strong>角色变更：</strong>{{ getRoleDisplayName(history.fromRole) }} → {{ getRoleDisplayName(history.toRole) }}</p>
                    <p><strong>原因：</strong>{{ history.reason }}</p>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
            <div v-else class="empty-history">
              <el-empty description="暂无权限变更历史" />
            </div>
          </div>
        </el-dialog>

        <!-- 添加用于调试的内容 -->
        <div v-if="false">
          当前角色选项: {{ roleOptions }}
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request.ts'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 搜索和筛选相关
const searchKeyword = ref('')
const selectedClass = ref('')
const filterRole = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 对话框相关
const roleDialogVisible = ref(false)
const detailsDialogVisible = ref(false)
const currentStudent = ref(null)
const submitting = ref(false)
const roleFormRef = ref(null)

// 表单数据
const roleForm = ref({
  role: '',
  reason: ''
})

// 表单验证规则
const roleRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  reason: [{ required: true, message: '请输入修改原因', trigger: 'blur' }]
}

// 数据列表
const users = ref([])

// 选项数据
const classOptions = ref([
  { label: '计科2101', value: '计科2101' },
  { label: '计科2102', value: '计科2102' },
  { label: '计科2103', value: '计科2103' },
  { label: '计科2104', value: '计科2104' }
])

const roleOptions = ref([])

// 使用静态角色选项
const staticRoleOptions = [
  { value: 'user', label: '普通学生' },
  { value: 'groupMember', label: '综测小组成员' },
  { value: 'groupLeader', label: '综测小组负责人' }
]

// 角色映射
const roleMap = {
  'user': '普通学生',
  'groupMember': '综测小组成员',
  'groupLeader': '综测小组负责人'
}

// 获取角色显示名称
const getRoleDisplayName = (role) => {
  return roleMap[role] || role
}

// 获取角色标签类型
const getRoleTagType = (role) => {
  const typeMap = {
    user: '',
    groupMember: 'success',
    groupLeader: 'warning'
  }
  return typeMap[role] || ''
}

// 获取学生列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await request.get('/instructor/students', {
      params: {
        keyword: searchKeyword.value,
        className: selectedClass.value,
        role: filterRole.value
      }
    });
    users.value = Array.isArray(response.data) ? response.data : [];
  } catch (error) {
    console.error('获取学生列表失败:', error.message);
    ElMessage.error('获取学生列表失败');
    users.value = [];
  } finally {
    loading.value = false;
  }
}

// 过滤后的用户列表
const filteredUsers = computed(() => {
  // 分页处理
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return users.value.slice(start, end)
})

// 打开修改权限对话框
const openRoleDialog = (row) => {
  currentStudent.value = row
  roleForm.value = {
    role: row.role || '',  // 设置当前角色为默认值
    reason: ''
  }
  roleDialogVisible.value = true
  console.log('当前表单数据:', roleForm.value) // 调试信息
  console.log('可选角色:', staticRoleOptions)   // 调试信息
}

// 查看用户详情
const viewUserDetails = async (user) => {
  loading.value = true
  try {
    // 如果有需要，可以在这里获取更详细的用户信息
    currentStudent.value = user
    detailsDialogVisible.value = true
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ElMessage.error('获取用户详情失败')
  } finally {
    loading.value = false
  }
}

// 提交权限修改
const submitRoleChange = async () => {
  try {
    // 验证表单
    await roleFormRef.value.validate()
    
    submitting.value = true
    
    // 确认对话框
    await ElMessageBox.confirm(
      `确定要将 ${currentStudent.value.name} 的角色从 ${getRoleDisplayName(currentStudent.value.role)} 修改为 ${getRoleDisplayName(roleForm.value.role)} 吗？`,
      '确认修改',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 发送请求
    await request.put(`/instructor/students/${currentStudent.value.userId}/role`, {
      role: roleForm.value.role,
      reason: roleForm.value.reason
    })
    
    ElMessage.success('权限修改成功')
    roleDialogVisible.value = false
    
    // 重新获取用户列表
    await fetchUsers()
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消了操作
      return
    }
    if (error.response?.status === 403) {
      ElMessage.error(error.response.data.message || '无权限修改该学生')
    } else {
      console.error('权限修改失败:', error)
      ElMessage.error('权限修改失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    submitting.value = false
  }
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUsers()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUsers()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchUsers()
}

// 处理搜索清除
const handleSearchClear = () => {
  currentPage.value = 1
  fetchUsers()
}

// 处理筛选条件变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchUsers()
}

// 获取角色选项
const fetchRoleOptions = async () => {
  try {
    const { data } = await request.get('/roles/student')
    console.log('获取到的角色选项:', data)
    roleOptions.value = data
  } catch (error) {
    console.error('获取角色列表失败:', error.message)
    ElMessage.error('获取角色列表失败')
  }
}

// 在 roleOptions 定义后添加
const formatDate = (row, column) => {
  if (!row.assignTime) return '';
  const date = new Date(row.assignTime);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

// 初始化数据
onMounted(() => {
  // 检查是否有 token
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.error('请先登录')
    // 重定向到登录页
    window.location.href = '/login'
    return
  }
  fetchRoleOptions()
  fetchUsers()
})

// 监听分页和筛选条件变化
watch([currentPage, pageSize], () => {
  fetchUsers()
}, { immediate: true })

const handleRoleSubmit = async () => {
  if (!roleForm.value.role || !roleForm.value.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    await request.put(`/instructor/student/${currentStudent.value.userId}/role`, {
      role: roleForm.value.role,
      reason: roleForm.value.reason
    })
    
    ElMessage.success('修改权限成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error('修改权限失败:', error)
    ElMessage.error('修改权限失败')
  }
}
</script>

<style scoped>
.permission-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
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
  margin: 0 0 20px 0;
  font-weight: 500;
}

.filter-section {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.search-input {
  width: 250px;
}

.table-container {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.pagination {
  margin-top: 24px;
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
  line-height: 1.6;
}

.role-history {
  margin-top: 20px;
}

.role-history h4 {
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
}

.empty-history {
  margin-top: 20px;
}

.dialog-footer {
  margin-top: 20px;
  text-align: right;
}

.user-details {
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-descriptions__body) {
  background-color: #f9f9f9;
}

:deep(.el-timeline-item__timestamp) {
  color: #666;
  font-size: 14px;
}

:deep(.el-card__body) {
  padding: 10px;
}

:deep(.el-card__body p) {
  margin: 5px 0;
  font-size: 14px;
}

:deep(.el-table) {
  --el-table-border-color: #ebeef5;
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  font-weight: 500;
  color: #606266;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafafa;
}

:deep(.el-tag) {
  font-weight: 500;
}

:deep(.el-button--small) {
  padding: 8px 15px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-pagination) {
  --el-pagination-button-bg-color: white;
}

.w-full {
  width: 100%;
}
</style>