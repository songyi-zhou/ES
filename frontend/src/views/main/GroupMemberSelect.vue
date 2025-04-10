<template>
  <div class="select-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综测小组成员选定</h2>
          <div class="header-actions">
            <button 
              class="save-btn" 
              :disabled="selectedStudents.length === 0"
              @click="saveSelectedMembers"
            >
              保存更改
            </button>
          </div>
        </div>

        <div class="selection-layout">
          <!-- 左侧：学生列表 -->
          <div class="student-list">
            <div class="filter-section">
              <div class="search-box">
                <input 
                  type="text" 
                  v-model="searchQuery" 
                  placeholder="搜索学生姓名或学号"
                >
              </div>
              <div class="filter-group">
                <select v-model="selectedMajor">
                  <option value="">所有专业</option>
                  <option v-for="major in majors" :key="major.id" :value="major.id">
                    {{ major.name }}
                  </option>
                </select>
                <select v-model="selectedClass" :disabled="!selectedMajor">
                  <option value="">所有班级</option>
                  <option v-for="class_ in filteredClasses" :key="class_.id" :value="class_.id">
                    {{ class_.name }}
                  </option>
                </select>
              </div>
            </div>

            <div class="table-container">
              <el-table
                ref="tableRef"
                v-loading="loading"
                :data="sortedStudents"
                @selection-change="handleSelectionChange"
              >
                <el-table-column 
                  type="selection" 
                  :selectable="row => !isAlreadySelected(row)"
                />
                <el-table-column prop="studentId" label="学号" />
                <el-table-column prop="name" label="姓名" />
                <el-table-column prop="major" label="专业" />
                <el-table-column prop="className" label="班级" />
                <el-table-column label="状态" width="100">
                  <template #default="scope">
                    <el-tag 
                      v-if="isAlreadySelected(scope.row)" 
                      type="success"
                    >
                      已选定
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 右侧：已选成员 -->
          <div class="selected-members">
            <h3>已选成员 ({{ selectedStudents.length }})</h3>
            <div class="selected-list">
              <div 
                v-for="student in selectedStudents" 
                :key="student.id" 
                class="selected-item"
              >
                <div class="student-info">
                  <span class="name">{{ student.name }}</span>
                  <span class="id">{{ student.studentId }}</span>
                  <span class="class">{{ student.className }}</span>
                </div>
                <button 
                  class="remove-btn"
                  @click="removeSelection(student)"
                >
                  ×
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '@/utils/format'

// 专业和班级数据
const majors = ref([
  { id: 1, name: '计算机科学与技术' },
  { id: 2, name: '软件工程' },
  // ... 更多专业
])

const classes = ref([
  { id: 1, majorId: 1, name: '计科2101' },
  { id: 2, majorId: 1, name: '计科2102' },
  { id: 3, majorId: 2, name: '软工2101' },
  // ... 更多班级
])

// 学生数据
const allStudents = ref([
  {
    id: 1,
    studentId: '2021001',
    name: '张三',
    major: '计算机科学与技术',
    class: '计科2101'
  },
  // ... 更多学生数据
])

// 状态变量
const searchQuery = ref('')
const selectedMajor = ref('')
const selectedClass = ref('')
const selectedStudents = ref([])
const originalSelection = ref([]) // 用于跟踪更改
const router = useRouter()
const loading = ref(false)
const students = ref([])
const existingMembers = ref([])
const members = ref([])
const isComponentMounted = ref(true)

// 计算属性
const filteredClasses = computed(() => {
  if (!selectedMajor.value) return []
  return classes.value.filter(c => c.majorId === Number(selectedMajor.value))
})

const isAllSelected = computed(() => {
  return students.value.length > 0 && 
         students.value.every(s => isSelected(s))
})

const hasChanges = computed(() => {
  return JSON.stringify(selectedStudents.value.map(s => s.id).sort()) !== 
         JSON.stringify(originalSelection.value.sort())
})

const isSelected = (student) => {
  return selectedStudents.value.some(s => s.id === student.id)
}

const toggleSelection = (student) => {
  const index = selectedStudents.value.findIndex(s => s.id === student.id)
  if (index === -1) {
    selectedStudents.value.push(student)
  } else {
    selectedStudents.value.splice(index, 1)
  }
}

const toggleAllSelection = () => {
  if (isAllSelected.value) {
    selectedStudents.value = selectedStudents.value.filter(s => 
      !students.value.some(fs => fs.id === s.id)
    )
  } else {
    const newSelections = students.value.filter(s => !isSelected(s))
    selectedStudents.value = [...selectedStudents.value, ...newSelections]
  }
}

const removeSelection = (student) => {
  const index = selectedStudents.value.findIndex(s => s.id === student.id)
  if (index !== -1) {
    selectedStudents.value.splice(index, 1)
  }
}

const saveSelectedMembers = async () => {
  try {
    const response = await request.post('/group-members/batch', {
      studentIds: selectedStudents.value.map(s => s.studentId)
    })
    if (response.data.success) {
      ElMessage.success('选择成功！')
      // 添加新的提示信息
      ElMessage({
        type: 'info',
        message: '请前往中队干部管理页面手动添加小组成员',
        duration: 5000  // 显示5秒
      })
      // 重置选择状态
      selectedStudents.value = []
      // 刷新数据
      await fetchExistingMembers()
      await fetchStudents()
    }
  } catch (error) {
    console.error('提交选择失败:', error)
    ElMessage.error('提交失败，请重试')
  }
}

const fetchStudents = async () => {
  loading.value = true
  try {
    const response = await request.get('/students/list', {
      params: {
        major: selectedMajor.value || undefined,
        className: selectedClass.value || undefined,
        keyword: searchQuery.value || undefined
      }
    })
    if (response.data.success) {
      students.value = response.data.data
    }
  } catch (error) {
    if (error.response?.status === 403) {
      router.push('/login')
    }
    console.error('获取学生列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchExistingMembers = async () => {
  try {
    // 获取综测小组成员
    const membersResponse = await request.get('/instructor/group-members')
    // 获取综测负责人
    const leadersResponse = await request.get('/squad-group-leaders')
    
    if (membersResponse.data.success && leadersResponse.data.success) {
      const members = membersResponse.data.data.map(member => ({
        ...member,
        studentId: member.studentId || member.id
      }))
      
      const leaders = leadersResponse.data.data.map(leader => ({
        ...leader,
        studentId: leader.studentId || leader.id
      }))
      
      // 合并两类人员
      existingMembers.value = [...members, ...leaders]
      console.log('Processed existing members:', existingMembers.value)
    }
  } catch (error) {
    console.error('获取已选定成员失败:', error)
  }
}

const isAlreadySelected = (student) => {
  console.log('Checking student:', student)
  const studentId = student.studentId?.toString()
  console.log(`Comparing member.studentId: ${existingMembers.value.some(member => member.studentId === studentId)} with student.studentId: ${studentId}`)
  return existingMembers.value.some(member => {
    return studentId === member.studentId
  })
}

const handleSelectionChange = (selection) => {
  console.log('Selection changed:', selection);
  selectedStudents.value = selection;
}

// 添加表格引用以便清除选择状态
const tableRef = ref()

// 在保存成功后清除表格选择状态
const clearSelection = () => {
  tableRef.value?.clearSelection()
}

// 初始化
onMounted(() => {
  isComponentMounted.value = true
  fetchExistingMembers() // 先获取已选成员
  fetchStudents()        // 再获取学生列表
  fetchMembers()
})

onBeforeUnmount(() => {
  isComponentMounted.value = false
})

// 添加 watch
watch(selectedStudents, (newValue) => {
  originalSelection.value = newValue.map(s => s.id)
})

// 添加监听器以检查数据变化
watch(existingMembers, (newVal) => {
  console.log('Existing members updated:', newVal)
}, { deep: true })

watch(students, (newVal) => {
  console.log('Students updated:', newVal)
}, { deep: true })

// 添加调试信息
watch(() => students.value, (newStudents) => {
  console.log('Students data structure:', newStudents?.[0]);
}, { immediate: true, deep: true });

// 添加计算属性对表格数据进行排序
const sortedStudents = computed(() => {
  return [...students.value].sort((a, b) => {
    const aSelected = isAlreadySelected(a);
    const bSelected = isAlreadySelected(b);
    if (aSelected === bSelected) {
      // 如果选择状态相同，按学号排序
      return a.studentId?.localeCompare(b.studentId) || 0;
    }
    // 已选定的排在后面
    return aSelected ? 1 : -1;
  });
});

const fetchMembers = async () => {
  if (!isComponentMounted.value) return
  
  try {
    const response = await request.get('/class-group-members')
    if (response.data.success && isComponentMounted.value) {
      members.value = response.data.data
    }
  } catch (error) {
    if (isComponentMounted.value) {
      console.error('获取成员列表失败:', error)
      ElMessage.error('获取成员列表失败')
    }
  }
}
</script>

<style scoped>
.select-container {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.save-btn {
  padding: 8px 16px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.save-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.selection-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  height: calc(100vh - 180px);
}

.student-list {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.filter-section {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
}

.search-box input {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-bottom: 10px;
}

.filter-group {
  display: flex;
  gap: 10px;
}

.filter-group select {
  flex: 1;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.table-container {
  flex: 1;
  overflow: auto;
}

.selected-members {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.selected-members h3 {
  margin: 0 0 15px 0;
  color: #303133;
}

.selected-list {
  flex: 1;
  overflow-y: auto;
}

.selected-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.student-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.student-info .name {
  font-weight: 500;
}

.student-info .id,
.student-info .class {
  font-size: 12px;
  color: #909399;
}

.remove-btn {
  background: none;
  border: none;
  color: #f56c6c;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
}

.remove-btn:hover {
  background: #fef0f0;
  border-radius: 4px;
}
</style> 