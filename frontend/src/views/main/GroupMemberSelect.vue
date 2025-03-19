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
              @click="saveSelectedMembers"
              :disabled="!hasChanges"
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
              <table>
                <thead>
                  <tr>
                    <th width="50">
                      <input 
                        type="checkbox" 
                        :checked="isAllSelected"
                        @change="toggleAllSelection"
                      >
                    </th>
                    <th>学号</th>
                    <th>姓名</th>
                    <th>专业</th>
                    <th>班级</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="student in filteredStudents" :key="student.id">
                    <td>
                      <input 
                        type="checkbox" 
                        :checked="isSelected(student)"
                        @change="toggleSelection(student)"
                      >
                    </td>
                    <td>{{ student.studentId }}</td>
                    <td>{{ student.name }}</td>
                    <td>{{ student.major }}</td>
                    <td>{{ student.class }}</td>
                  </tr>
                </tbody>
              </table>
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
                  <span class="class">{{ student.class }}</span>
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

<script setup>
import { ref, computed, onMounted } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

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

// 计算属性
const filteredClasses = computed(() => {
  if (!selectedMajor.value) return []
  return classes.value.filter(c => c.majorId === selectedMajor.value)
})

const filteredStudents = computed(() => {
  let result = allStudents.value

  // 按专业筛选
  if (selectedMajor.value) {
    const majorName = majors.value.find(m => m.id === selectedMajor.value)?.name
    result = result.filter(s => s.major === majorName)
  }

  // 按班级筛选
  if (selectedClass.value) {
    const className = classes.value.find(c => c.id === selectedClass.value)?.name
    result = result.filter(s => s.class === className)
  }

  // 按搜索关键词筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(s => 
      s.name.toLowerCase().includes(query) || 
      s.studentId.toLowerCase().includes(query)
    )
  }

  return result
})

const isAllSelected = computed(() => {
  return filteredStudents.value.length > 0 && 
         filteredStudents.value.every(s => isSelected(s))
})

const hasChanges = computed(() => {
  return JSON.stringify(selectedStudents.value.map(s => s.id).sort()) !== 
         JSON.stringify(originalSelection.value.sort())
})

// 方法
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
      !filteredStudents.value.some(fs => fs.id === s.id)
    )
  } else {
    const newSelections = filteredStudents.value.filter(s => !isSelected(s))
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
    // 调用API保存选定的成员
    // await fetch('/api/evaluation-group/members', {
    //   method: 'POST',
    //   body: JSON.stringify(selectedStudents.value)
    // })
    
    // 更新原始选择记录
    originalSelection.value = selectedStudents.value.map(s => s.id)
    alert('保存成功！')
  } catch (error) {
    console.error('保存失败:', error)
    alert('保存失败，请重试')
  }
}

// 初始化
onMounted(async () => {
  try {
    // 获取已选成员
    // const response = await fetch('/api/evaluation-group/members')
    // const data = await response.json()
    // selectedStudents.value = data
    // originalSelection.value = data.map(s => s.id)
  } catch (error) {
    console.error('获取数据失败:', error)
  }
})
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

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
  position: sticky;
  top: 0;
  z-index: 1;
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