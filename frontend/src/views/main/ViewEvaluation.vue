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
            <table class="monthly-table">
              <thead>
                <tr>
                  <th>班级</th>
                  <th>姓名</th>
                  <th>学号</th>
                  <th>基础分</th>
                  <th>总加分</th>
                  <th>总扣分</th>
                  <th>原始总分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in monthlyRecords" :key="record.id">
                  <td>{{ record.class_id || record.className }}</td>
                  <td>{{ record.name }}</td>
                  <td>{{ record.student_id || record.studentId }}</td>
                  <td>{{ record.base_score || record.baseScore || '0' }}</td>
                  <td>{{ record.total_bonus || record.totalBonus || '0' }}</td>
                  <td>{{ record.total_penalty || record.totalPenalty || '0' }}</td>
                  <td class="total-score">{{ record.raw_score || record.rawScore || '0' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 学期总表 -->
          <h3>学期总表</h3>
          <div class="table-container" v-if="dataLoaded">
            <table class="semester-table">
              <thead>
                <tr>
                  <th>班级</th>
                  <th>姓名</th>
                  <th>学号</th>
                  <th>基础分</th>
                  <th>总加分</th>
                  <th>总扣分</th>
                  <th>原始总分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in semesterRecords" :key="record.id">
                  <td>{{ record.class_name || record.className }}</td>
                  <td>{{ record.name }}</td>
                  <td>{{ record.student_id || record.studentId }}</td>
                  <td>{{ record.base_score || record.baseScore || '0' }}</td>
                  <td>{{ record.total_bonus || record.totalBonus || '0' }}</td>
                  <td>{{ record.total_penalty || record.totalPenalty || '0' }}</td>
                  <td class="total-score">{{ record.raw_score || record.rawScore || '0' }}</td>
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
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
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
    
    const [yearStart] = selectedYear.value.split('-');
    // 构建semester参数，格式：year-year-term
    const semester = `${yearStart}-${parseInt(yearStart) + 1}-${selectedTerm.value}`;
    
    console.log('请求参数:', {
      category: selectedCategory.value,
      semester: semester,
      month: selectedCategory.value === 'A' ? selectedMonth.value : undefined
    });

    const token = localStorage.getItem('token');
    console.log('使用的Token:', token ? `${token.substring(0, 20)}...` : 'Token未找到');

    if (!token) {
      console.error('未找到登录令牌');
      alert('请先登录后再查询成绩');
      router.push('/login');
      return;
    }

    console.log('开始发送请求...');
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
    
    if (response.data.success) {
      console.log('数据处理成功');
      // 根据后端返回的数据结构更新
      monthlyRecords.value = response.data.data.monthlyData || [];
      semesterRecords.value = response.data.data.semesterData || [];
    } else {
      console.error('请求成功但返回错误:', response.data.message);
      alert(response.data.message || '获取数据失败');
    }
  } catch (error) {
    console.error('获取评分数据失败:', error);
    hasSearched.value = false;
    dataLoaded.value = false;
    
    if (error.response) {
      console.error('错误详情:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      });
      
      if (error.response.status === 401 || error.response.status === 403) {
        console.log('权限错误，准备跳转到登录页');
        alert('登录已过期或没有权限，请重新登录');
        router.push('/login');
      } else {
        alert(`请求失败: ${error.response.status} ${error.response.statusText}`);
      }
    } else if (error.request) {
      console.error('请求未收到响应:', error.request);
      alert('无法连接到服务器，请检查网络连接');
    } else {
      console.error('请求配置错误:', error.message);
      alert(`发生错误: ${error.message || '未知错误'}`);
    }
  } finally {
    console.log('请求结束，重置加载状态');
    isLoading.value = false;
  }
};

onMounted(() => {
  generateAcademicYears(); // 生成学年选项
  // 移除自动查询
  // fetchEvaluationData();
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
</style> 