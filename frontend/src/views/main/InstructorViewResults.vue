<template>
  <div class="results-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <div class="main-content">
        <h2 class="page-title">综合评测结果查询</h2>
        <div class="filter-section">
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

          <!-- 学年选择 -->
          <div class="filter-item">
            <label>学年</label>
            <select v-model="selectedYear">
              <option value="">请选择学年</option>
              <option v-for="year in academicYears" :key="year" :value="year">
                {{ year }}学年
              </option>
            </select>
          </div>

          <!-- 学期选择 -->
          <div class="filter-item">
            <label>学期</label>
            <select v-model="selectedTerm">
              <option value="">请选择学期</option>
              <option value="1">第一学期</option>
              <option value="2">第二学期</option>
            </select>
          </div>
          
          <!-- 专业选择 -->
          <div class="filter-item">
            <label>专业</label>
            <select v-model="selectedMajor">
              <option value="">全部专业</option>
              <option v-for="major in majors" :key="major.id" :value="major.id">
                {{ major.name }}
              </option>
            </select>
          </div>

          <!-- 班级选择 -->
          <div class="filter-item">
            <label>班级</label>
            <select v-model="selectedClass">
              <option value="">全部班级</option>
              <option v-for="class_ in filteredClasses" :key="class_.id" :value="class_.id">
                {{ class_.name }}
              </option>
            </select>
          </div>
        </div>
        
        <!-- 查询和导出按钮 -->
        <div class="button-container">
          <button class="search-btn" @click="searchResults" :disabled="isLoading">
            <i class="fas fa-search"></i> {{ isLoading ? '查询中...' : '查询' }}
          </button>
          <button class="export-btn" @click="exportToExcel" :disabled="isLoading || students.length === 0">
            <i class="fas fa-download"></i> 导出Excel
          </button>
        </div>

        <!-- 错误信息提示 -->
        <div v-if="errorMessage" class="error-message">
          <i class="fas fa-exclamation-circle"></i> {{ errorMessage }}
        </div>

        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>正在加载数据，请稍候...</p>
        </div>

        <!-- 结果展示表格 -->
        <div v-else class="table-container">
          <div v-if="students.length === 0 && !isLoading" class="empty-data">
            <p>暂无符合条件的数据</p>
          </div>
          <table v-else>
            <thead>
              <tr>
                <th>班级</th>
                <th>姓名</th>
                <th>学号</th>
                <th>原始分</th>
                <th>总加分</th>
                <th>总扣分</th>
                <th>原始总分</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="student in filteredStudents" :key="student.id">
                <td>{{ student.className }}</td>
                <td>{{ student.name }}</td>
                <td>{{ student.studentId }}</td>
                <td>{{ student.baseScore || 0 }}</td>
                <td>{{ student.totalBonus || 0 }}</td>
                <td>{{ student.totalPenalty || 0 }}</td>
                <td>{{ student.rawScore || 0 }}</td>
                <td>
                  <button class="detail-btn" @click="viewDetail(student)">查看材料</button>
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
import { ref, computed, onMounted } from 'vue';
import TopBar from '@/components/TopBar.vue';
import Sidebar from '@/components/Sidebar.vue';
import axios from 'axios';

// API基础URL
const API_URL = import.meta.env.VITE_API_URL || '';

// 班级数据
const classes = ref([
  { id: 1, name: '计算机2101', majorId: 1 },
  { id: 2, name: '计算机2102', majorId: 1 },
  { id: 3, name: '软件2101', majorId: 2 },
  { id: 4, name: '软件2102', majorId: 2 },
  { id: 5, name: '数据科学2101', majorId: 3 },
  { id: 6, name: '人工智能2101', majorId: 4 }
]);

// 专业数据
const majors = ref([
  { id: 1, name: '计算机科学与技术' },
  { id: 2, name: '软件工程' },
  { id: 3, name: '数据科学与大数据技术' },
  { id: 4, name: '人工智能' }
]);

// 生成学年选项（最近4年）
const academicYears = ref([]);
const generateAcademicYears = () => {
  const currentDate = new Date();
  const currentYear = currentDate.getFullYear();
  const startYear = currentYear - 4;
  
  for (let i = 0; i < 4; i++) {
    const year = startYear + i;
    academicYears.value.push(`${year}-${year + 1}`);
  }
};

// 选中的筛选条件
const selectedClass = ref('');
const selectedMajor = ref('');
const selectedTableType = ref('comprehensive');
const selectedYear = ref('');
const selectedTerm = ref('');
const isLoading = ref(false);

// 学生数据
const students = ref([]);
const totalRecords = ref(0);
const errorMessage = ref('');

// 根据选择的专业筛选班级
const filteredClasses = computed(() => {
  if (!selectedMajor.value) return classes.value;
  return classes.value.filter(cls => cls.majorId === selectedMajor.value);
});

// 根据所有筛选条件过滤学生
const filteredStudents = computed(() => {
  let result = students.value;
  
  // 按班级筛选
  if (selectedClass.value) {
    const className = classes.value.find(c => c.id === selectedClass.value)?.name;
    result = result.filter(student => student.className === className);
  }
  // 按专业筛选(如果未选择班级但选择了专业)
  else if (selectedMajor.value) {
    const majorClasses = filteredClasses.value.map(cls => cls.name);
    result = result.filter(student => majorClasses.includes(student.className));
  }
  
  // 这里可以添加按学年和学期筛选的逻辑
  // ...
  
  return result;
});

// 查询结果
const searchResults = async () => {
  isLoading.value = true;
  errorMessage.value = '';
  
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      errorMessage.value = '用户未登录，请先登录';
      return;
    }
    
    const response = await axios.get(`${API_URL}/api/instructor/evaluation/results`, {
      params: {
        tableType: selectedTableType.value,
        academicYear: selectedYear.value,
        term: selectedTerm.value,
        major: selectedMajor.value,
        classId: selectedClass.value,
        month: selectedTableType.value === 'moral' ? '1' : undefined // 如果需要月份参数，这里可以添加月份选择
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    console.log('查询结果:', response.data);
    
    if (response.data.success) {
      students.value = response.data.data.map(item => ({
        id: item.id,
        studentId: item.student_id,
        name: item.student_name,
        className: item.class_name,
        baseScore: item.baseScore,
        totalBonus: item.totalBonus,
        totalPenalty: item.totalPenalty,
        rawScore: item.rawScore
      }));
      totalRecords.value = response.data.total;
    } else {
      errorMessage.value = response.data.message || '获取数据失败';
      students.value = [];
    }
  } catch (error) {
    console.error('查询失败:', error);
    errorMessage.value = error.response?.data?.message || '查询失败，请稍后重试';
    students.value = [];
  } finally {
    isLoading.value = false;
  }
};

// 在组件挂载时生成学年选项
onMounted(() => {
  generateAcademicYears();
  // 默认选择最近的学年
  selectedYear.value = academicYears.value[academicYears.value.length - 1];
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

.page-title {
  margin-bottom: 20px;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
  align-items: flex-end;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  justify-content: center;
}

.button-container {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 200px;
}

.filter-item label {
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

.filter-item select {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-width: 180px;
  font-size: 14px;
  color: #606266;
}

.filter-item select:focus {
  border-color: #409eff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.search-btn {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  font-size: 14px;
}

.search-btn:hover {
  background: #66b1ff;
}

.search-btn:disabled, .export-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.export-btn {
  padding: 8px 20px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  font-size: 14px;
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

/* 错误信息样式 */
.error-message {
  padding: 10px 15px;
  background-color: #fef0f0;
  color: #f56c6c;
  border-radius: 4px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

.empty-data {
  padding: 50px 0;
  text-align: center;
  color: #909399;
  font-size: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style> 