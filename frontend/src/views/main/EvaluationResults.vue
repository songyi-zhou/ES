<template>
  <div class="results-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综合测评成绩公示</h2>
          <div class="countdown">
            <i class="el-icon-time"></i>
            <span>公示剩余时间：{{ formatCountdown }}</span>
          </div>
        </div>

        <!-- 班级选择 -->
        <div class="filter-section">
          <select v-model="selectedClass" class="filter-select" @change="fetchClassResults">
            <option value="">请选择班级</option>
            <option v-for="class_ in classes" :key="class_.id" :value="class_.id">
              {{ class_.name }}
            </option>
          </select>
        </div>

        <!-- 成绩列表 -->
        <div class="table-container" v-if="selectedClass">
          <table>
            <thead>
              <tr>
                <th>班级</th>
                <th>学号</th>
                <th>姓名</th>
                <th>A项得分</th>
                <th>A项标准分</th>
                <th>C项得分</th>
                <th>C项标准分</th>
                <th>D项得分</th>
                <th>D项标准分</th>
                <th>附加项</th>
                <th>B项标准分</th>
                <th>E项标准分</th>
                <th>B项名次</th>
                <th>综合测评值</th>
                <th>综合名次</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(result, index) in sortedResults" :key="result.studentId">
                <td>{{ result.className }}</td>
                <td>{{ result.studentId }}</td>
                <td>{{ result.name }}</td>
                <td>{{ result.scoreA }}</td>
                <td>{{ result.standardA }}</td>
                <td>{{ result.scoreC }}</td>
                <td>{{ result.standardC }}</td>
                <td>{{ result.scoreD }}</td>
                <td>{{ result.standardD }}</td>
                <td>{{ result.additional }}</td>
                <td>{{ result.standardB }}</td>
                <td>{{ result.standardE }}</td>
                <td>{{ result.rankB }}</td>
                <td class="total-score">{{ result.totalScore }}</td>
                <td class="rank">{{ result.totalRank }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 提示信息 -->
        <div class="notice" v-if="!selectedClass">
          <i class="notice-icon">ℹ️</i>
          请选择班级查看综测成绩
        </div>

        <!-- 公示期结束提醒 -->
        <div class="notice warning" v-if="isPublicityEnded">
          <i class="warning-icon">⚠️</i>
          公示期已结束，不再接受问题反馈
        </div>

        <!-- 反馈按钮 -->
        <button 
          class="feedback-btn" 
          @click="showFeedbackModal = true"
          v-if="!isPublicityEnded"
        >
          反馈问题
        </button>

        <!-- 反馈弹窗 -->
        <div v-if="showFeedbackModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>问题反馈</h3>
              <button class="close-btn" @click="showFeedbackModal = false">×</button>
            </div>
            <div class="form-group">
              <label>问题类型：</label>
              <select v-model="feedbackForm.type">
                <option value="">请选择问题类型</option>
                <option value="score">分数有误</option>
                <option value="missing">信息缺失</option>
                <option value="other">其他问题</option>
              </select>
            </div>
            <div class="form-group">
              <label>问题描述：</label>
              <textarea 
                v-model="feedbackForm.description" 
                rows="4" 
                placeholder="请详细描述您发现的问题..."
              ></textarea>
            </div>
            <div class="form-group">
              <label>联系方式：</label>
              <input 
                type="text" 
                v-model="feedbackForm.contact" 
                placeholder="请留下您的联系方式（手机号/邮箱）"
              >
            </div>
            <div class="modal-actions">
              <button @click="submitFeedback" class="btn-primary">提交反馈</button>
              <button @click="showFeedbackModal = false" class="btn-secondary">取消</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 班级数据
const classes = ref([
  { id: '1', name: '计算机2101' },
  { id: '2', name: '计算机2102' },
  { id: '3', name: '计算机2103' }
])

// 选中的班级
const selectedClass = ref('')

// 公示截止时间
const endTime = ref(new Date('2024-03-25 23:59:59'))

// 计算剩余时间
const formatCountdown = computed(() => {
  const now = new Date()
  const diff = endTime.value - now
  
  if (diff <= 0) return '公示已结束'
  
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  
  return `${days}天${hours}小时${minutes}分钟`
})

// 判断是否公示期结束
const isPublicityEnded = computed(() => {
  const now = new Date()
  return now > endTime.value
})

// 成绩数据
const results = ref([
  {
    className: '计算机2101',
    studentId: '2021001',
    name: '张三',
    scoreA: 85,
    standardA: 8.5,
    scoreC: 90,
    standardC: 9.0,
    scoreD: 88,
    standardD: 8.8,
    additional: 2,
    standardB: 9.2,
    standardE: 8.7,
    rankB: 5,
    totalScore: 89.5,
    totalRank: 3
  },
  // 更多成绩数据...
])

// 按总分排序的成绩
const sortedResults = computed(() => {
  return [...results.value].sort((a, b) => b.totalScore - a.totalScore)
})

// 反馈相关
const showFeedbackModal = ref(false)
const feedbackForm = ref({
  type: '',
  description: '',
  contact: ''
})

// 获取班级成绩
const fetchClassResults = async () => {
  if (!selectedClass.value) return
  
  try {
    // 这里应该调用后端API获取成绩数据
    // const response = await fetch(`/api/evaluation-results/${selectedClass.value}`)
    // results.value = await response.json()
    console.log('获取班级成绩:', selectedClass.value)
  } catch (error) {
    console.error('获取成绩失败:', error)
  }
}

// 提交反馈
const submitFeedback = async () => {
  try {
    // 这里应该调用后端API提交反馈
    console.log('提交反馈:', feedbackForm.value)
    showFeedbackModal.value = false
    // 重置表单
    feedbackForm.value = {
      type: '',
      description: '',
      contact: ''
    }
  } catch (error) {
    console.error('提交反馈失败:', error)
  }
}
</script>

<style scoped>
.results-container {
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

.countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 15px;
  background: #fff;
  border-radius: 4px;
  color: #409eff;
  font-weight: 500;
}

.filter-section {
  margin-bottom: 20px;
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
  margin-bottom: 20px;
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

.total-score, .rank {
  font-weight: bold;
  color: #409eff;
}

.notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  background: #f4f4f5;
  border-radius: 4px;
  color: #909399;
  margin: 20px 0;
}

.notice-icon {
  font-size: 20px;
}

.notice.warning {
  background: #fef0f0;
  color: #f56c6c;
  font-weight: bold;
}

.warning-icon {
  font-size: 20px;
  color: #f56c6c;
}

.feedback-btn {
  display: block;
  width: 120px;
  margin: 20px auto;
  padding: 10px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.feedback-btn:hover {
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
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
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

.btn-primary:hover {
  background: #66b1ff;
}

.btn-secondary:hover {
  background: #f9f9fa;
}
</style> 