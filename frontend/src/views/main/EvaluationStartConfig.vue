<template>
  <div class="config-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综测启动配置</h2>
          <el-button 
            type="primary" 
            size="large"
            @click="publishEvaluation"
            :loading="publishing"
            :disabled="!canPublish"
          >
            发布综测
          </el-button>
        </div>

        <div class="page-layout">
          <div class="main-section">
            <div class="config-sections">
              <!-- 基本信息配置 -->
              <el-card class="config-card">
                <template #header>
                  <div class="card-header">
                    <h3>基本信息配置</h3>
                    <el-button type="primary" @click="saveBasicConfig" :loading="saving.basic">
                      保存更改
                    </el-button>
                  </div>
                </template>
                
                <el-form :model="basicConfig" label-width="140px">
                  <el-form-item 
                    label="学年" 
                    required
                    :rules="[{ required: true, message: '请选择学年' }]"
                  >
                    <select 
                      v-model="basicConfig.academicYear" 
                      class="custom-select"
                      required
                    >
                      <option value="" disabled>请选择学年</option>
                      <option 
                        v-for="year in academicYears" 
                        :key="year" 
                        :value="year"
                      >
                        {{ year }}学年
                      </option>
                    </select>
                  </el-form-item>
                  
                  <el-form-item 
                    label="学期" 
                    required
                    :rules="[{ required: true, message: '请选择学期' }]"
                  >
                    <select 
                      v-model="basicConfig.semester" 
                      class="custom-select"
                      required
                    >
                      <option value="" disabled>请选择学期</option>
                      <option :value="1">第一学期</option>
                      <option :value="2">第二学期</option>
                    </select>
                  </el-form-item>

                  <el-form-item 
                    label="综测表类型" 
                    required
                  >
                    <el-radio-group v-model="basicConfig.formType">
                      <el-radio label="A">
                        <el-tooltip
                          content="思想品德分测评表"
                          placement="top"
                        >
                          <span>A类表</span>
                        </el-tooltip>
                      </el-radio>
                      <el-radio label="C">
                        <el-tooltip
                          content="科研竞赛分测评表"
                          placement="top"
                        >
                          <span>C类表</span>
                        </el-tooltip>
                      </el-radio>
                      <el-radio label="D">
                        <el-tooltip
                          content="文体分测评表"
                          placement="top"
                        >
                          <span>D类表</span>
                        </el-tooltip>
                      </el-radio>
                    </el-radio-group>
                  </el-form-item>

                  <!-- 当选择A类表时显示月份选择 -->
                  <el-form-item 
                    v-if="basicConfig.formType === 'A'"
                    label="月份" 
                    required
                    :rules="[{ required: true, message: '请选择月份' }]"
                  >
                    <el-select 
                      v-model="basicConfig.month" 
                      placeholder="请选择月份"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="month in 12"
                        :key="month"
                        :label="`${month}月`"
                        :value="month"
                      />
                    </el-select>
                  </el-form-item>

                  <!-- 当选择C或D类表时显示本学期月份数 -->
                  <el-form-item 
                    v-if="basicConfig.formType === 'C' || basicConfig.formType === 'D'"
                    label="本学期月数" 
                    required
                    :rules="[{ required: true, message: '请选择本学期月数' }]"
                  >
                    <select 
                      v-model="basicConfig.monthCount" 
                      class="custom-select"
                      required
                    >
                      <option value="" disabled>请选择本学期包含几个月</option>
                      <option 
                        v-for="count in 6"
                        :key="count"
                        :value="count"
                      >
                        {{ count }}个月
                      </option>
                    </select>
                    <div class="month-hint">
                      提示：此项用于计算本学期基础分总和（每月10分 × 月数）
                    </div>
                  </el-form-item>

                  <el-form-item label="综测说明">
                    <el-input 
                      v-model="basicConfig.description" 
                      type="textarea" 
                      :rows="3"
                      placeholder="请输入本次综测的总体说明"
                    />
                  </el-form-item>
                </el-form>
              </el-card>

              <!-- 时间配置 -->
              <el-card class="config-card">
                <template #header>
                  <div class="card-header">
                    <h3>时间配置</h3>
                    <el-button type="primary" @click="saveTimeConfig" :loading="saving.time">
                      保存更改
                    </el-button>
                  </div>
                </template>
                
                <el-form :model="timeConfig" label-width="140px">
                  <el-form-item 
                    label="申报开始时间"
                    required
                    :rules="[{ required: true, message: '请选择开始时间' }]"
                  >
                    <input 
                      type="datetime-local" 
                      v-model="timeConfig.startTime"
                      class="custom-datetime"
                      required
                    >
                  </el-form-item>
                  <el-form-item 
                    label="申报结束时间"
                    required
                    :rules="[{ required: true, message: '请选择结束时间' }]"
                  >
                    <input 
                      type="datetime-local" 
                      v-model="timeConfig.endTime"
                      class="custom-datetime"
                      required
                    >
                  </el-form-item>
                  <el-form-item 
                    label="审核截止时间"
                    required
                    :rules="[{ required: true, message: '请选择审核截止时间' }]"
                  >
                    <input 
                      type="datetime-local" 
                      v-model="timeConfig.reviewEndTime"
                      class="custom-datetime"
                      required
                    >
                  </el-form-item>
                  <el-form-item 
                    label="公示开始时间"
                    required
                    :rules="[{ required: true, message: '请选择公示开始时间' }]"
                  >
                    <input 
                      type="datetime-local" 
                      v-model="timeConfig.publicityStartTime"
                      class="custom-datetime"
                      required
                    >
                  </el-form-item>
                  <el-form-item 
                    label="公示结束时间"
                    required
                    :rules="[{ required: true, message: '请选择公示结束时间' }]"
                  >
                    <input 
                      type="datetime-local" 
                      v-model="timeConfig.publicityEndTime"
                      class="custom-datetime"
                      required
                    >
                  </el-form-item>
                </el-form>
              </el-card>

              <!-- 分数配置 -->
              <el-card class="config-card">
                <template #header>
                  <div class="card-header">
                    <h3>分数配置</h3>
                    <el-button type="primary" @click="saveScoreConfig" :loading="saving.score">
                      保存更改
                    </el-button>
                  </div>
                </template>
                
                <el-form :model="scoreConfig" label-width="140px">
    <el-form-item 
      label="基础分"
      required
      :rules="[{ required: true, message: '请设置基础分' }]"
    >
      <div class="score-input-group">
        <button 
          type="button" 
          class="score-btn" 
          @click="scoreConfig.baseScore = Math.max(0, scoreConfig.baseScore - 1)"
        >-</button>
        <input 
          type="number" 
          v-model="scoreConfig.baseScore" 
          class="score-input"
          min="0"
          max="100"
        >
        <button 
          type="button" 
          class="score-btn"
          @click="scoreConfig.baseScore = Math.min(100, scoreConfig.baseScore + 1)"
        >+</button>
      </div>
      <div class="score-hint">
        提示：根据学生手册规定，基础分默认为每月10分
      </div>
    </el-form-item>
  </el-form>
</el-card>

              <!-- 注意事项 -->
              <el-card class="config-card">
                <template #header>
                  <div class="card-header">
                    <h3>注意事项</h3>
                    <el-button type="primary" @click="saveNoticeConfig" :loading="saving.notice">
                      保存更改
                    </el-button>
                  </div>
                </template>
                
                <el-form :model="noticeConfig" label-width="140px">
                  <el-form-item label="注意事项列表">
                    <div class="notice-list">
                      <div 
                        v-for="(item, index) in noticeConfig.items" 
                        :key="index"
                        class="notice-item"
                      >
                        <el-input 
                          v-model="noticeConfig.items[index]" 
                          type="textarea" 
                          :rows="2"
                        />
                        <el-button 
                          type="danger" 
                          circle
                          @click="removeNoticeItem(index)"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                    </div>
                    <el-button 
                      type="primary" 
                      plain 
                      @click="addNoticeItem"
                      class="add-notice-btn"
                    >
                      添加注意事项
                    </el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </div>
          </div>

          <!-- 添加日志部分 -->
          <div class="log-section">
            <el-card class="log-card">
              <template #header>
                <div class="card-header">
                  <h3>配置修改日志</h3>
                  <el-button type="info" @click="refreshLogs">
                    刷新日志
                  </el-button>
                </div>
              </template>
              
              <el-timeline>
                <el-timeline-item
                  v-for="(log, index) in configLogs"
                  :key="index"
                  :timestamp="log.time"
                  :type="log.type"
                >
                  <div class="log-content">
                    <span class="log-operator">{{ log.operator }}</span>
                    <span class="log-section">{{ log.section }}</span>
                    <p class="log-description">{{ log.description }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </div>
        </div>

        <!-- 预览对话框 -->
        <el-dialog
          v-model="previewDialogVisible"
          title="发布预览"
          width="600px"
        >
          <div class="preview-content">
            <h2>{{ basicConfig.name }}</h2>
            <div class="preview-section">
              <h4>综测说明</h4>
              <p>{{ basicConfig.description }}</p>
            </div>
            <div class="preview-section">
              <h4>时间安排</h4>
              <p>申报时间：{{ formatDateTime(timeConfig.startTime) }} 至 {{ formatDateTime(timeConfig.endTime) }}</p>
              <p>审核截止：{{ formatDateTime(timeConfig.reviewEndTime) }}</p>
              <p>公示时间：{{ formatDateTime(timeConfig.publicityStartTime) }} 至 {{ formatDateTime(timeConfig.publicityEndTime) }}</p>
            </div>
            <div class="preview-section">
              <h4>分数设置</h4>
              <p>基础分：{{ scoreConfig.baseScore }}</p>
              <p>单项最高分：{{ scoreConfig.maxItemScore }}</p>
              <p>总分上限：{{ scoreConfig.totalMaxScore }}</p>
            </div>
            <div class="preview-section">
              <h4>注意事项</h4>
              <ul>
                <li v-for="(item, index) in noticeConfig.items" :key="index">
                  {{ item }}
                </li>
              </ul>
            </div>
          </div>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="previewDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="confirmPublish">
                确认发布
              </el-button>
            </div>
          </template>
        </el-dialog>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 保存状态
const saving = ref({
  basic: false,
  time: false,
  score: false,
  notice: false
})

const publishing = ref(false)
const previewDialogVisible = ref(false)

// 基本信息配置
const basicConfig = ref({
  academicYear: '',
  semester: null,
  formType: '',
  month: null,    // A类表选择的月份
  monthCount: '', // C、D类表选择的月数
  description: ''
})

// 生成最近5个学年的选项
const currentYear = new Date().getFullYear()
const academicYears = ref(
  Array.from({ length: 5 }, (_, i) => {
    const year = currentYear - i
    return `${year}-${year + 1}`
  })
)

// 时间配置
const timeConfig = ref({
  startTime: null,
  endTime: null,
  reviewEndTime: null,
  publicityStartTime: null,
  publicityEndTime: null
})

// 分数配置
const scoreConfig = ref({
  baseScore: 10,
})

// 注意事项配置
const noticeConfig = ref({
  items: ['请认真填写申请材料，确保信息真实准确。']
})

// 添加日志相关的响应式数据
const configLogs = ref([
  {
    time: new Date().toLocaleString(),
    operator: '管理员',
    section: '基本信息',
    description: '创建了新的综测配置',
    type: 'primary'
  }
])

// 检查是否可以发布
const canPublish = computed(() => {
  return basicConfig.value.name && 
         basicConfig.value.formType &&
         timeConfig.value.startTime &&
         timeConfig.value.endTime &&
         timeConfig.value.reviewEndTime &&
         timeConfig.value.publicityStartTime &&
         timeConfig.value.publicityEndTime &&
         noticeConfig.value.items.length > 0
})

// 添加注意事项
const addNoticeItem = () => {
  noticeConfig.value.items.push('')
}

// 删除注意事项
const removeNoticeItem = (index) => {
  noticeConfig.value.items.splice(index, 1)
}

// 格式化日期时间
const formatDateTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

// 添加刷新日志的方法
const refreshLogs = async () => {
  try {
    // TODO: 调用获取日志API
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('日志已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

// 添加记录日志的方法
const addConfigLog = (section, description, type = 'primary') => {
  configLogs.value.unshift({
    time: new Date().toLocaleString(),
    operator: '管理员', // 应该从登录用户信息中获取
    section,
    description,
    type
  })
}

// 修改现有的保存方法，添加日志记录
const saveBasicConfig = async () => {
  try {
    saving.value.basic = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('基本信息保存成功')
    addConfigLog('基本信息', '更新了综测基本信息配置')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.basic = false
  }
}

const saveTimeConfig = async () => {
  try {
    saving.value.time = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('时间配置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.time = false
  }
}

const saveScoreConfig = async () => {
  try {
    saving.value.score = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('分数配置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.score = false
  }
}

const saveNoticeConfig = async () => {
  try {
    saving.value.notice = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('注意事项保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.notice = false
  }
}

// 发布综测
const publishEvaluation = () => {
  if (!canPublish.value) {
    ElMessage.warning('请完善所有必要配置')
    return
  }
  previewDialogVisible.value = true
}

// 确认发布
const confirmPublish = async () => {
  try {
    publishing.value = true
    // TODO: 调用发布API
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    ElMessage.success('综测已成功发布')
    previewDialogVisible.value = false
  } catch (error) {
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

// 计算基础分总和
const calculateTotalBaseScore = computed(() => {
  if (basicConfig.value.formType === 'C' || basicConfig.value.formType === 'D') {
    return basicConfig.value.monthCount * 10
  }
  return 10 // A类表每月固定10分
})
</script>

<style scoped>
.config-container {
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

.page-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 180px); /* 减去顶部导航和页头的高度 */
  overflow: hidden;
}

.main-section {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

.log-section {
  width: 350px;
  overflow-y: auto;
}

.log-card {
  height: 80%;
  display: flex;
  flex-direction: column;
}

.log-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 0 15px;
}

.log-content {
  padding: 5px 0;
}

.log-operator {
  font-weight: bold;
  margin-right: 8px;
}

.log-section {
  color: #409EFF;
  font-size: 0.9em;
}

.log-description {
  margin: 5px 0;
  color: #666;
}

/* 调整时间线样式 */
:deep(.el-timeline) {
  padding-top: 10px;
}

:deep(.el-timeline-item__node) {
  width: 12px;
  height: 12px;
}

:deep(.el-timeline-item__tail) {
  left: 5px;
}

:deep(.el-timeline-item__wrapper) {
  padding-left: 25px;
}

:deep(.el-timeline-item__timestamp) {
  font-size: 0.85em;
  color: #999;
}

/* 优化滚动条样式 */
.main-section::-webkit-scrollbar,
.log-section::-webkit-scrollbar {
  width: 6px;
}

.main-section::-webkit-scrollbar-thumb,
.log-section::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.main-section::-webkit-scrollbar-track,
.log-section::-webkit-scrollbar-track {
  background: #f5f7fa;
}

.config-sections {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.notice-list {
  margin-bottom: 10px;
}

.notice-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  align-items: flex-start;
}

.add-notice-btn {
  width: 100%;
}

.preview-content {
  max-height: 600px;
  overflow-y: auto;
  padding: 0 20px;
}

.preview-section {
  margin-bottom: 20px;
}

.preview-section h4 {
  margin-bottom: 10px;
  color: #409EFF;
}

.preview-section ul {
  padding-left: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-input-number) {
  width: 180px;
}

:deep(.el-date-picker) {
  width: 100%;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 20px;
}

:deep(.el-radio) {
  margin-right: 0;
}

.custom-select {
  width: 100%;
  height: 40px;
  padding: 0 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.custom-select:hover {
  border-color: #c0c4cc;
}

.custom-select:focus {
  border-color: #409eff;
}

.custom-select option {
  padding: 10px;
}

.custom-select option:disabled {
  color: #c0c4cc;
}

.custom-datetime {
  width: 100%;
  height: 40px;
  padding: 0 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.custom-datetime:hover {
  border-color: #c0c4cc;
}

.custom-datetime:focus {
  border-color: #409eff;
}

.score-input-group {
  display: flex;
  align-items: center;
  width: 180px;
}

.score-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.score-btn:first-child {
  border-radius: 4px 0 0 4px;
}

.score-btn:last-child {
  border-radius: 0 4px 4px 0;
}

.score-btn:hover {
  background: #f5f7fa;
}

.score-input {
  flex: 1;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-left: none;
  border-right: none;
  text-align: center;
  outline: none;
  color: #606266;
  font-size: 14px;
}

.score-input::-webkit-inner-spin-button,
.score-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.score-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.month-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style> 