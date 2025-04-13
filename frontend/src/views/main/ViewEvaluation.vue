<template>
  <div class="view-evaluation-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="evaluation-card">
          <h2>我的综测</h2>
          <p class="description">
            学生可以通过此页面查看自己的综测。通过在下方选取不同的时间段，从而查看自己各个学期的综测成绩。
          </p>

          <div class="filter-section">
            <div class="filter-item">
              <label>请选择分类：</label>
              <select v-model="selectedCategory">
                <option value="A">A分</option>
                <option value="C">C分</option>
                <option value="D">D分</option>
              </select>
            </div>

            <div class="filter-item">
              <label>学年：</label>
              <select v-model="selectedYear">
                <option v-for="year in academicYears" :key="year" :value="year">
                  {{ year }}学年
                </option>
              </select>
            </div>

            <div class="filter-item">
              <label>学期：</label>
              <select v-model="selectedTerm">
                <option value="1">第一学期</option>
                <option value="2">第二学期</option>
              </select>
            </div>

            <div class="filter-item" v-if="selectedCategory === 'A'">
              <label>月份：</label>
              <select v-model="selectedMonth">
                <template v-if="selectedTerm === '1'">
                  <option value="9">9月</option>
                  <option value="10">10月</option>
                  <option value="11">11月</option>
                  <option value="12">12月</option>
                  <option value="1">1月</option>
                  <!-- <option value="0">学期总表</option> -->
                </template>
                <template v-else-if="selectedTerm === '2'">
                  <option value="2">2月</option>
                  <option value="3">3月</option>
                  <option value="4">4月</option>
                  <option value="5">5月</option>
                  <option value="6">6月</option>
                  <option value="7">7月</option>
                  <!-- <option value="0">学期总表</option> -->
                </template>
              </select>
            </div>
            
            <div class="search-button-container">
              <button class="search-button" @click="fetchEvaluationData" :disabled="isLoading">
                <i class="el-icon-search" v-if="!isLoading"></i>
                <i class="el-icon-loading" v-else></i>
                {{ isLoading ? '查询中...' : '查询成绩' }}
              </button>
            </div>
          </div>

          <!-- 月度评分表格 -->
          <div class="table-container" v-if="dataLoaded">
            <h3>{{ selectedCategory }}类评分表</h3>
            <table class="evaluation-table">
              <thead>
                <tr>
                  <th>班级</th>
                  <th>姓名</th>
                  <th>学号</th>
                  <th>基础分</th>
                  <th v-if="selectedCategory === 'A'">总加分</th>
                  <th v-if="selectedCategory === 'A'">总扣分</th>
                  <th>{{ selectedCategory === 'A' ? '原始总分' : '得分' }}</th>
                  <th v-if="selectedCategory !== 'A'">平均分</th>
                  <th v-if="selectedCategory !== 'A'">标准差</th>
                  <th v-if="selectedCategory !== 'A'">最终得分</th>
                  <th v-if="selectedCategory !== 'A'">排名</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!monthlyRecords || monthlyRecords.length === 0">
                  <td :colspan="selectedCategory === 'A' ? 7 : 8" class="no-data">暂无数据</td>
                </tr>
                <tr v-else v-for="(record, index) in monthlyRecords" :key="index">
                  <td>{{ record.class_id }}</td>
                  <td>{{ record.name }}</td>
                  <td>{{ record.student_id }}</td>
                  <td>{{ record.base_score || 0 }}</td>
                  <td v-if="selectedCategory === 'A'">{{ record.total_bonus || 0 }}</td>
                  <td v-if="selectedCategory === 'A'">{{ record.total_penalty || 0 }}</td>
                  <td>{{ record.raw_score || 0 }}</td>
                  <td v-if="selectedCategory !== 'A'">{{ record.avg_score || 0 }}</td>
                  <td v-if="selectedCategory !== 'A'">{{ record.std_dev || 0 }}</td>
                  <td v-if="selectedCategory !== 'A'">{{ record.final_score || 0 }}</td>
                  <td v-if="selectedCategory !== 'A'">{{ record.rank || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="monthlyRecords && monthlyRecords.length > 0" class="debug-info" style="margin-top: 10px; color: #666; font-size: 12px;">
              <!-- 添加调试信息 -->
              <!-- <pre>{{ JSON.stringify(monthlyRecords, null, 2) }}</pre> -->
            </div>
          </div>

          <!-- 学期总表 -->
          <h3>学期总表</h3>
          <div class="table-container" v-if="dataLoaded">
            <table class="semester-table">
              <thead>
                <tr>
                  <th>学年</th>
                  <th>学期</th>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>班级</th>
                  <th>中队</th>
                  <th>学院</th>
                  <th>专业</th>
                  <th>德育成绩</th>
                  <th>学业成绩</th>
                  <th>科研竞赛</th>
                  <th>文体活动</th>
                  <th>附加分</th>
                  <th>总分</th>
                  <th>排名</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in semesterRecords" :key="record.id">
                  <td>{{ record.academic_year }}</td>
                  <td>{{ record.semester }}</td>
                  <td>{{ record.student_id }}</td>
                  <td>{{ record.name }}</td>
                  <td>{{ record.class_name }}</td>
                  <td>{{ record.squad }}</td>
                  <td>{{ record.department }}</td>
                  <td>{{ record.major }}</td>
                  <td>{{ record.moral_score || '0' }}</td>
                  <td>{{ record.academic_score || '0' }}</td>
                  <td>{{ record.research_score || '0' }}</td>
                  <td>{{ record.sports_arts_score || '0' }}</td>
                  <td>{{ record.extra_score || '0' }}</td>
                  <td>{{ record.total_score || '0' }}</td>
                  <td>{{ record.rank || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <div v-if="!dataLoaded" class="notice-box">
            <p>请选择条件并点击"查询成绩"按钮获取数据</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const route = useRoute();
const selectedCategory = ref('A');
const selectedYear = ref('');
const selectedTerm = ref('1');
const selectedMonth = ref('9');
const isLoading = ref(false);
const dataLoaded = ref(false);
const hasSearched = ref(false);

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
  
  // 默认选择最近的学年
  selectedYear.value = academicYears.value[academicYears.value.length - 1];
};

// 组合学年和学期
const combinedSemester = computed(() => {
  if (!selectedYear.value || !selectedTerm.value) return '';
  return `${selectedYear.value}-${selectedTerm.value}`;
});

const monthlyRecords = ref([]);
const semesterRecords = ref([]);

// 监听分类变化
watch(selectedCategory, (newCategory) => {
  if (newCategory === 'A') {
    // 如果变为A分类，根据当前学期设置月份
    if (selectedTerm.value === '1') {
      selectedMonth.value = '9';
    } else if (selectedTerm.value === '2') {
      selectedMonth.value = '3';
    }
  }
});

// 监听学期变化，重置月份选择
watch(selectedTerm, (newTerm) => {
  if (selectedCategory.value === 'A') {
    if (newTerm === '1') {
      selectedMonth.value = '9'; // 第一学期默认选择9月
    } else if (newTerm === '2') {
      selectedMonth.value = '3'; // 第二学期默认选择3月
    }
  }
});

// 获取评分数据
const fetchEvaluationData = async () => {
  try {
    isLoading.value = true;
    hasSearched.value = true;
    dataLoaded.value = true;
    monthlyRecords.value = []; // 清空之前的数据
    
    const [yearStart] = selectedYear.value.split('-');
    // 构建semester参数，格式：year-year-term
    const semester = `${yearStart}-${parseInt(yearStart) + 1}-${selectedTerm.value}`;
    
    console.log('请求参数:', {
      category: selectedCategory.value,
      semester: semester,
      month: selectedCategory.value === 'A' ? selectedMonth.value : undefined
    });

    const token = localStorage.getItem('token');
    if (!token) {
      console.error('未找到登录令牌');
      alert('请先登录后再查询成绩');
      router.push('/login');
      return;
    }

    const response = await axios.get(`/api/my-evaluation/scores`, {
      params: {
        category: selectedCategory.value,
        semester: semester,
        month: selectedCategory.value === 'A' ? selectedMonth.value : undefined
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    console.log('收到响应:', response.data);
    
    if (response.data.success && response.data.data) {
      // 处理返回的数据
      const data = response.data.data;
      console.log('原始数据:', data);
      
      if (selectedCategory.value === 'A') {
        // A类评分处理 monthlyData
        if (data.monthlyData) {
          monthlyRecords.value = Array.isArray(data.monthlyData) ? data.monthlyData : [data.monthlyData];
        } else {
          monthlyRecords.value = [];
        }
      } else {
        // C类和D类评分处理 semesterData
        if (data.semesterData) {
          monthlyRecords.value = Array.isArray(data.semesterData) ? data.semesterData : [data.semesterData];
        } else {
          monthlyRecords.value = [];
        }
      }
      
      console.log('处理后的数据:', monthlyRecords.value);
    } else {
      console.error('请求成功但返回错误或无数据:', response.data.message);
      monthlyRecords.value = [];
    }
  } catch (error) {
    console.error('获取评分数据失败:', error);
    monthlyRecords.value = [];
    if (error.response?.status === 401 || error.response?.status === 403) {
      alert('登录已过期或没有权限，请重新登录');
      router.push('/login');
    } else {
      alert(error.response?.data?.message || '获取数据失败');
    }
  } finally {
    isLoading.value = false;
  }
};

const loadData = async () => {
  try {
    console.log('开始请求数据，参数:', {
      academicYear: route.query.academicYear,
      semester: route.query.semester
    })
    
    const response = await axios.get('/api/my-evaluation/comprehensive-results', {
      params: {
        academicYear: route.query.academicYear,
        semester: route.query.semester
      }
    })
    
    console.log('后端返回的完整响应:', response)
    console.log('响应数据:', response.data)
    
    if (response.data && response.data.data) {
      semesterRecords.value = response.data.data
      console.log('设置到表格的数据:', semesterRecords.value)
    } else {
      console.warn('响应数据格式不符合预期:', response.data)
      semesterRecords.value = []
    }
    
    dataLoaded.value = true
  } catch (error) {
    console.error('加载数据失败:', error)
    if (error.response) {
      console.error('错误响应数据:', error.response.data)
    }
  }
}

onMounted(() => {
  generateAcademicYears(); // 生成学年选项
  loadData();
});
</script>

<style scoped>
.view-evaluation-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
  background: #f0f2f5;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.evaluation-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

h2 {
  color: #2c3e50;
  font-size: 24px;
  margin-bottom: 16px;
  position: relative;
  padding-left: 16px;
}

h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: #409EFF;
  border-radius: 2px;
}

.description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
  margin-bottom: 24px;
}

.filter-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-item {
  min-width: unset;
}

.filter-item label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.filter-item select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
  background: #fff;
}

.filter-item select:hover {
  border-color: #c0c4cc;
}

.filter-item select:focus {
  border-color: #409EFF;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.search-button-container {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
}

.search-button {
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 20px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.search-button:hover {
  background-color: #66b1ff;
}

.search-button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.table-container {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin-top: 0;
}

th, td {
  padding: 12px 16px;
  text-align: center;
  border: none;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

tr:hover {
  background: #f5f7fa;
}

td {
  color: #606266;
  font-size: 14px;
}

h3 {
  color: #2c3e50;
  font-size: 20px;
  margin: 30px 0 20px;
  padding-left: 12px;
  border-left: 4px solid #409EFF;
}

.notice-box {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  color: #909399;
  font-size: 16px;
  margin: 30px 0;
}

@media (max-width: 768px) {
  .filter-section {
    grid-template-columns: 1fr;
  }
  
  .table-container {
    margin: 0 -16px;
    border-radius: 0;
  }
  
  .search-button-container {
    justify-content: center;
    margin-top: 20px;
  }
}

.evaluation-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background: white;
}

.evaluation-table th,
.evaluation-table td {
  padding: 12px;
  text-align: center;
  border: 1px solid #ebeef5;
}

.evaluation-table th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.evaluation-table tbody tr:hover {
  background-color: #f5f7fa;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 24px;
}

.table-container h3 {
  margin-bottom: 16px;
  color: #303133;
  font-size: 18px;
}
</style> 