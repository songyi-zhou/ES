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
                <option value="B">B分</option>
                <option value="C">C分</option>
                <option value="D">D分</option>
              </select>
            </div>

            <div class="filter-item">
              <label>请选择时段：</label>
              <select v-model="selectedSemester">
                <option value="2021-2022-1">2021-2022学年 第一学期</option>
                <option value="2021-2022-2">2021-2022学年 第二学期</option>
                <option value="2022-2023-1">2022-2023学年 第一学期</option>
                <option value="2022-2023-2">2022-2023学年 第二学期</option>
              </select>
            </div>

            <div class="filter-item">
              <label>月份：</label>
              <select v-model="selectedMonth">
                <option value="9">9月</option>
                <option value="10">10月</option>
                <option value="11">11月</option>
                <option value="12">12月</option>
                <option value="1">1月</option>
                <option value="2">2月</option>
              </select>
            </div>
          </div>

          <!-- 月度评分表格 -->
          <div class="table-container">
            <table class="monthly-table">
              <thead>
                <tr>
                  <th>班级</th>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>表扬/批评</th>
                  <th>班会</th>
                  <th>升旗、早点评</th>
                  <th>干部</th>
                  <th>内务</th>
                  <th>晚自习</th>
                  <th>晨跑</th>
                  <th>晚查寝</th>
                  <th>其他</th>
                  <th>总分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in monthlyRecords" :key="record.id">
                  <td>{{ record.className }}</td>
                  <td>{{ record.studentId }}</td>
                  <td>{{ record.name }}</td>
                  <td>{{ record.praise }}</td>
                  <td>{{ record.classmeeting }}</td>
                  <td>{{ record.flagRaising }}</td>
                  <td>{{ record.cadre }}</td>
                  <td>{{ record.internal }}</td>
                  <td>{{ record.eveningStudy }}</td>
                  <td>{{ record.morningRun }}</td>
                  <td>{{ record.eveningCheck }}</td>
                  <td>{{ record.others }}</td>
                  <td>{{ record.total }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 学期总表 -->
          <h3>学期总表</h3>
          <div class="table-container">
            <table class="semester-table">
              <thead>
                <tr>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>班级</th>
                  <th>A分</th>
                  <th>A分标准分</th>
                  <th>C分</th>
                  <th>C分标准分</th>
                  <th>D分</th>
                  <th>D分标准分</th>
                  <th>附加分</th>
                  <th>B分</th>
                  <th>B分标准分</th>
                  <th>B分排名</th>
                  <th>总分</th>
                  <th>总排名</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in semesterRecords" :key="record.id">
                  <td>{{ record.studentId }}</td>
                  <td>{{ record.name }}</td>
                  <td>{{ record.className }}</td>
                  <td>{{ record.scoreA }}</td>
                  <td>{{ record.scoreAStd }}</td>
                  <td>{{ record.scoreC }}</td>
                  <td>{{ record.scoreCStd }}</td>
                  <td>{{ record.scoreD }}</td>
                  <td>{{ record.scoreDStd }}</td>
                  <td>{{ record.scoreExtra }}</td>
                  <td>{{ record.scoreB }}</td>
                  <td>{{ record.scoreBStd }}</td>
                  <td>{{ record.rankB }}</td>
                  <td>{{ record.scoreTotal }}</td>
                  <td>{{ record.rankTotal }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const selectedCategory = ref('A');
const selectedSemester = ref('2021-2022-1');
const selectedMonth = ref('9');

const monthlyRecords = ref([
  {
    className: '计算机科学与技术2021-0',
    studentId: '2220210000',
    name: '张三',
    praise: '0',
    classmeeting: '0',
    flagRaising: '0',
    cadre: '0',
    internal: '0',
    eveningStudy: '0',
    morningRun: '0',
    eveningCheck: '0',
    others: '0',
    total: '10.0'
  }
]);

const semesterRecords = ref([
  {
    studentId: '2220210000',
    name: '张三',
    className: '计算机科学与技术2021-0',
    scoreA: '40.2',
    scoreAStd: '70.26816413',
    scoreC: '40',
    scoreCStd: '73.923276',
    scoreD: '40',
    scoreDStd: '70.28107366',
    scoreExtra: '0',
    scoreB: '4.1926',
    scoreBStd: '87.32991761',
    rankB: '6',
    scoreTotal: '81.05412848',
    rankTotal: '14'
  }
]);

// 监听筛选条件变化
watch([selectedCategory, selectedSemester, selectedMonth], async (newValues) => {
  await fetchEvaluationData();
}, { immediate: true });

const fetchEvaluationData = async () => {
  try {
    // 这里使用实际的API端点
    const response = await axios.get('/api/evaluation/scores', {
      params: {
        category: selectedCategory.value,
        semester: selectedSemester.value,
        month: selectedMonth.value
      }
    });

    // 假设API返回的数据结构符合我们的需求
    if (response.data.monthlyData) {
      monthlyRecords.value = response.data.monthlyData;
    }
    if (response.data.semesterData) {
      semesterRecords.value = response.data.semesterData;
    }
  } catch (error) {
    console.error('获取成绩数据失败:', error);
  }
};

onMounted(() => {
  fetchEvaluationData();
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

@media (max-width: 768px) {
  .filter-section {
    grid-template-columns: 1fr;
  }
  
  .table-container {
    margin: 0 -16px;
    border-radius: 0;
  }
}
</style> 