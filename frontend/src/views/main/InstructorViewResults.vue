<template>
  <div class="results-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <div class="main-content">
        <div class="filter-section">
          <!-- 班级选择 -->
          <div class="filter-item">
            <label>选择班级</label>
            <select v-model="selectedClass">
              <option value="">全部班级</option>
              <option v-for="class_ in classes" :key="class_.id" :value="class_.id">
                {{ class_.name }}
              </option>
            </select>
          </div>

          <!-- 表格类型选择 -->
          <div class="filter-item">
            <label>表格类型</label>
            <select v-model="selectedTableType">
              <option value="comprehensive">综合测评总表</option>
              <option value="moral">德育分表</option>
              <option value="intellectual">智育分表</option>
              <option value="physical">体育分表</option>
              <option value="aesthetic">美育分表</option>
              <option value="labor">劳动分表</option>
            </select>
          </div>

          <!-- 导出按钮 -->
          <button class="export-btn" @click="exportToExcel">
            <i class="fas fa-download"></i> 导出Excel
          </button>
        </div>

        <!-- 结果展示表格 -->
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>学号</th>
                <th>姓名</th>
                <th>班级</th>
                <th v-if="selectedTableType === 'comprehensive'">综合测评总分</th>
                <th v-if="selectedTableType === 'comprehensive'">综合排名</th>
                <th v-if="selectedTableType === 'moral'">德育分</th>
                <th v-if="selectedTableType === 'intellectual'">智育分</th>
                <th v-if="selectedTableType === 'physical'">体育分</th>
                <th v-if="selectedTableType === 'aesthetic'">美育分</th>
                <th v-if="selectedTableType === 'labor'">劳动分</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="student in filteredStudents" :key="student.id">
                <td>{{ student.studentId }}</td>
                <td>{{ student.name }}</td>
                <td>{{ student.className }}</td>
                <td v-if="selectedTableType === 'comprehensive'">{{ student.totalScore }}</td>
                <td v-if="selectedTableType === 'comprehensive'">{{ student.rank }}</td>
                <td v-if="selectedTableType === 'moral'">{{ student.moralScore }}</td>
                <td v-if="selectedTableType === 'intellectual'">{{ student.intellectualScore }}</td>
                <td v-if="selectedTableType === 'physical'">{{ student.physicalScore }}</td>
                <td v-if="selectedTableType === 'aesthetic'">{{ student.aestheticScore }}</td>
                <td v-if="selectedTableType === 'labor'">{{ student.laborScore }}</td>
                <td>
                  <button class="detail-btn" @click="viewDetail(student)">查看详情</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 详情弹窗 -->
        <div v-if="showDetailModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>学生测评详情</h3>
              <button class="close-btn" @click="showDetailModal = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="student-info">
                <p><strong>学号：</strong>{{ selectedStudent.studentId }}</p>
                <p><strong>姓名：</strong>{{ selectedStudent.name }}</p>
                <p><strong>班级：</strong>{{ selectedStudent.className }}</p>
              </div>
              <div class="score-details">
                <h4>分项得分详情</h4>
                <ul>
                  <li>德育分：{{ selectedStudent.moralScore }}</li>
                  <li>智育分：{{ selectedStudent.intellectualScore }}</li>
                  <li>体育分：{{ selectedStudent.physicalScore }}</li>
                  <li>美育分：{{ selectedStudent.aestheticScore }}</li>
                  <li>劳动分：{{ selectedStudent.laborScore }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import TopBar from '@/components/TopBar.vue';
import Sidebar from '@/components/Sidebar.vue';

// 班级数据
const classes = ref([
  { id: 1, name: '计算机2101' },
  { id: 2, name: '计算机2102' },
  { id: 3, name: '软件2101' },
  { id: 4, name: '软件2102' }
]);

// 选中的班级和表格类型
const selectedClass = ref('');
const selectedTableType = ref('comprehensive');

// 模拟学生数据
const students = ref([
  {
    id: 1,
    studentId: '2021001',
    name: '张三',
    className: '计算机2101',
    totalScore: 95,
    rank: 1,
    moralScore: 94,
    intellectualScore: 96,
    physicalScore: 92,
    aestheticScore: 95,
    laborScore: 98
  },
  // ... 更多学生数据
]);

// 根据选择的班级筛选学生
const filteredStudents = computed(() => {
  if (!selectedClass.value) return students.value;
  return students.value.filter(student => 
    student.className === classes.value.find(c => c.id === selectedClass.value)?.name
  );
});

// 详情弹窗控制
const showDetailModal = ref(false);
const selectedStudent = ref(null);

// 查看详情
const viewDetail = (student) => {
  selectedStudent.value = student;
  showDetailModal.value = true;
};

// 导出Excel
const exportToExcel = () => {
  // 实现导出Excel的逻辑
  console.log('导出Excel');
};
</script>

<style scoped>
.results-container {
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
  background: #f5f5f5;
  overflow-y: auto;
}

.filter-section {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item label {
  font-weight: bold;
  color: #333;
}

.filter-item select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  min-width: 200px;
}

.export-btn {
  padding: 8px 16px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
}

.export-btn:hover {
  background: #85ce61;
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
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.detail-btn {
  padding: 4px 8px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.detail-btn:hover {
  background: #66b1ff;
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

.student-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.score-details {
  padding: 15px;
}

.score-details h4 {
  margin-bottom: 10px;
  color: #303133;
}

.score-details ul {
  list-style: none;
  padding: 0;
}

.score-details li {
  margin: 8px 0;
  color: #606266;
}
</style> 