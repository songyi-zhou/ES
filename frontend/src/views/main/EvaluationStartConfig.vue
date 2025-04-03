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
                
                <div class="basic-config">
                  <div class="form-row">
                    <label class="required">学年</label>
                    <div class="input-wrapper">
                      <select v-model="basicConfig.academicYear" required>
                        <option value="">请选择学年</option>
                        <option 
                          v-for="year in academicYears" 
                          :key="year.value" 
                          :value="year.value"
                        >
                          {{ year.label }}
                        </option>
                      </select>
                    </div>
                  </div>

                  <div class="form-row">
                    <label class="required">学期</label>
                    <div class="input-wrapper">
                      <select v-model="basicConfig.semester" required>
                        <option value="">请选择学期</option>
                        <option value="1">第一学期</option>
                        <option value="2">第二学期</option>
                      </select>
                    </div>
                  </div>

                  <div class="form-row">
                    <label class="required">综测表类型</label>
                    <div class="input-wrapper">
                      <select v-model="basicConfig.formType" required>
                        <option value="">请选择表类型</option>
                        <option value="MONTHLY_A">A类月表</option>
                        <option value="TYPE_C">C类表</option>
                        <option value="TYPE_D">D类表</option>
                        <option value="SEMESTER_A">A类总表</option>
                        <option value="COMPREHENSIVE">综合测评结果表</option>
                      </select>
                    </div>
                  </div>

                  <!-- 仅A类月表显示月份选择 -->
                  <div class="form-row" v-if="basicConfig.formType === 'MONTHLY_A'">
                    <label class="required">月份</label>
                    <div class="input-wrapper">
                      <select v-model="basicConfig.month" required>
                        <option value="">请选择月份</option>
                        <option v-for="m in 12" :key="m" :value="m">{{ m }}月</option>
                      </select>
                    </div>
                  </div>

                  <!-- 仅C类表和D类表显示月数选择 -->
                  <div class="form-row" v-if="['TYPE_C', 'TYPE_D'].includes(basicConfig.formType)">
                    <label class="required">本学期月数</label>
                    <div class="input-wrapper">
                      <select v-model="basicConfig.monthCount" required>
                        <option value="">请选择本学期包含几个月</option>
                        <option v-for="m in 6" :key="m" :value="m">{{ m }}个月</option>
                      </select>
                      <div class="tip-text">提示：此项用于计算本学期基础分总和（每月10分 × 月数）</div>
                    </div>
                  </div>

                  <div class="form-row">
                    <label>综测说明</label>
                    <div class="input-wrapper">
                      <textarea 
                        v-model="basicConfig.description" 
                        placeholder="请输入本次综测的总体说明"
                        rows="4"
                      ></textarea>
                    </div>
                  </div>
                </div>
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
                
                <div class="time-config">
                  <div class="time-inputs">
                    <!-- 仅对A类月表、C类表、D类表显示申报和审核时间 -->
                    <template v-if="showDeclareAndReview">
                      <div class="time-input-group">
                        <label>申报开始时间：</label>
                        <input 
                          type="datetime-local" 
                          :value="timeConfig.declareStartTime?.replace(' ', 'T')"
                          @change="e => handleTimeChange('declareStartTime', e)"
                          required
                        />
                      </div>

                      <div class="time-input-group">
                        <label>申报结束时间：</label>
                        <input 
                          type="datetime-local" 
                          :value="timeConfig.declareEndTime?.replace(' ', 'T')"
                          @change="e => handleTimeChange('declareEndTime', e)"
                          required
                        />
                      </div>

                      <div class="time-input-group">
                        <label>审核截止时间：</label>
                        <input 
                          type="datetime-local" 
                          :value="timeConfig.reviewEndTime?.replace(' ', 'T')"
                          @change="e => handleTimeChange('reviewEndTime', e)"
                          required
                        />
                      </div>
                    </template>

                    <!-- 所有表都需要公示时间 -->
                    <div class="time-input-group">
                      <label>公示开始时间：</label>
                      <input 
                        type="datetime-local" 
                        :value="timeConfig.publicityStartTime?.replace(' ', 'T')"
                        @change="e => handleTimeChange('publicityStartTime', e)"
                        required
                      />
                    </div>

                    <div class="time-input-group">
                      <label>公示截止时间：</label>
                      <input 
                        type="datetime-local" 
                        :value="timeConfig.publicityEndTime?.replace(' ', 'T')"
                        @change="e => handleTimeChange('publicityEndTime', e)"
                        required
                      />
                    </div>
                  </div>
                </div>
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
          <div class="logs-section">
            <div class="logs-header">
              <h3>配置修改日志</h3>
              <el-button type="primary" size="small" @click="refreshLogs">
                刷新日志
              </el-button>
            </div>
            
            <el-empty v-if="!logs.length" description="暂无日志" />
            
            <el-timeline v-else>
              <el-timeline-item
                v-for="log in logs"
                :key="log.createdAt"
                :timestamp="log.createdAt"
                placement="top"
              >
                <div class="log-item">
                  <div class="log-header">
                    <span class="operator">{{ log.operatorName }}</span>
                    <el-tag size="small" type="info">{{ log.academicYear }}</el-tag>
                    <el-tag size="small">第{{ log.semester }}学期</el-tag>
                  </div>
                  <div class="log-content">
                    <el-tag size="small" type="success">{{ log.section }}</el-tag>
                    <el-tag size="small" type="warning">{{ log.operationType }}</el-tag>
                    <p class="description">{{ log.description }}</p>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
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
              <p>申报时间：{{ formatDateTime(timeConfig.declareStartTime) }} 至 {{ formatDateTime(timeConfig.declareEndTime) }}</p>
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
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import request from '@/utils/request'

// 保存状态
const saving = ref({
  basic: false,
  time: false,
  score: false,
  notice: false
})

const publishing = ref(false)
const previewDialogVisible = ref(false)

// 配置保存状态
const basicConfigSaved = ref(false)
const timeConfigSaved = ref(false)

// 生成未来5个学年的选项
const currentYear = new Date().getFullYear()
const academicYears = computed(() => {
  return Array.from({ length: 3 }, (_, i) => {
    const year = currentYear -2+ i
    return {
      value: `${year}-${year + 1}`,
      label: `${year}-${year + 1}学年`
    }
  })
})

// 基本信息配置
const basicConfig = ref({
  academicYear: '',
  semester: '',
  formType: '',
  month: '',
  monthCount: '',
  description: ''
})

// 时间配置
const timeConfig = ref({
  declareStartTime: '',
  declareEndTime: '',
  reviewEndTime: '',
  publicityStartTime: '',
  publicityEndTime: ''
})

// 分数配置
const scoreConfig = ref({
  baseScore: 10,
})

// 注意事项配置
const noticeConfig = ref({
  items: ['请认真填写申请材料，确保信息真实准确。']
})

const logs = ref([])

const refreshLogs = async () => {
  try {
    const response = await request.get('/evaluation-config/logs')
    // 打印返回数据，便于调试
    console.log('API返回数据:', response)
    
    // 确保response.data存在且包含logs数组
    if (response?.data?.logs) {
      logs.value = response.data.logs.map(log => ({
        ...log,
        createdAt: new Date(log.createdAt).toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      }))
      ElMessage.success('日志已刷新')
    } else {
      logs.value = []
      ElMessage.warning('暂无日志数据')
    }
  } catch (error) {
    console.error('获取日志失败:', error)
    logs.value = []
    ElMessage.error('刷新失败：' + (error.response?.data || '未知错误'))
  }
}

// 检查是否可以发布
const canPublish = computed(() => {
  // 检查配置是否已保存
  if (!basicConfigSaved.value || !timeConfigSaved.value) {
    return false
  }
  
  return true
})

// 添加注意事项
const addNoticeItem = () => {
  noticeConfig.value.items.push('')
}

// 删除注意事项
const removeNoticeItem = (index) => {
  noticeConfig.value.items.splice(index, 1)
}

// 格式化日期时间为 YYYY-MM-DD HH:mm:ss
const formatDateTime = (date) => {
  if (!date) return ''
  return date.replace('T', ' ')
}

// 处理时间变化
const handleTimeChange = (field, event) => {
  timeConfig.value[field] = formatDateTime(event.target.value)
}

// 验证时间
const validateTimes = () => {
  const times = timeConfig.value
  return Boolean(
    times.declareStartTime &&
    times.declareEndTime &&
    times.reviewEndTime &&
    times.publicityStartTime &&
    times.publicityEndTime
  )
}

// 添加记录日志的方法
const addConfigLog = (section, description, type = 'primary') => {
  logs.value.unshift({
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
    basicConfigSaved.value = true
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
    timeConfigSaved.value = true
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

// 检查是否已存在相同配置的综测
const checkExistingEvaluation = async () => {
  try {
    const response = await request.get('/evaluation-config/check', {
      params: {
        academicYear: basicConfig.value.academicYear,
        semester: basicConfig.value.semester,
        formType: basicConfig.value.formType,
        month: basicConfig.value.month // A类表专用
      }
    })
    return response.data.exists
  } catch (error) {
    console.error('检查综测配置失败:', error)
    ElMessage.error('检查综测配置失败')
    return true // 出错时默认认为存在，防止重复发布
  }
}

// 发布综测
const publishEvaluation = async () => {
  if (!canPublish.value) {
    ElMessage.warning('请填写完整所有必填项')
    return
  }

  // 检查是否已存在
  const exists = await checkExistingEvaluation()
  if (exists) {
    ElMessage.warning('该综测配置已存在，请勿重复发布')
    return
  }

  try {
    const response = await request.post('/api/evaluation-config/publish', {
      academicYear: basicConfig.value.academicYear,
      semester: basicConfig.value.semester,
      formType: basicConfig.value.formType,
      month: basicConfig.value.formType === 'A' ? basicConfig.value.month : null,
      monthCount: ['C', 'D'].includes(basicConfig.value.formType) ? basicConfig.value.monthCount : null,
      description: basicConfig.value.description
    })

    if (response.data.success) {
      ElMessage.success(response.data.message)
      // 可以在这里添加成功后的其他操作，比如重置表单或跳转页面
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    console.error('发布综测失败:', error)
    ElMessage.error(error.response?.data?.message || '发布综测失败，请稍后重试')
  }
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

// 表单验证规则
const rules = {
  academicYear: [{ required: true, message: '请选择学年' }],
  semester: [{ required: true, message: '请选择学期' }],
  formType: [{ required: true, message: '请选择综测表类型' }],
  month: [{ required: true, message: '请选择月份', trigger: 'change' }],
  monthCount: [{ required: true, message: '请选择月数', trigger: 'change' }],
  description: [{ required: true, message: '请输入说明' }],
  declareStartTime: [{ required: true, validator: validateTimes, trigger: 'change' }],
  declareEndTime: [{ required: true, validator: validateTimes, trigger: 'change' }],
  reviewEndTime: [{ required: true, validator: validateTimes, trigger: 'change' }],
  publicityStartTime: [{ required: true, validator: validateTimes, trigger: 'change' }],
  publicityEndTime: [{ required: true, validator: validateTimes, trigger: 'change' }]
}

const formTypes = [
  { label: 'A类月表', value: 'MONTHLY_A' },
  { label: 'C类表', value: 'TYPE_C' },
  { label: 'D类表', value: 'TYPE_D' },
  { label: 'A类总表', value: 'SEMESTER_A' },
  { label: '综合测评结果表', value: 'COMPREHENSIVE' }
]

// 是否需要显示申报和审核时间
const showDeclareAndReview = computed(() => {
  return ['MONTHLY_A', 'TYPE_C', 'TYPE_D'].includes(basicConfig.value.formType)
})

// 监听表类型变化，重置相关字段
watch(() => basicConfig.value.formType, (newType) => {
  basicConfig.value.month = ''
  basicConfig.value.monthCount = ''
})

// 初始加载
refreshLogs()
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

.logs-section {
  margin-top: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.log-item {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.log-header {
  margin-bottom: 10px;
}

.log-header .operator {
  font-weight: bold;
  margin-right: 10px;
}

.log-content .el-tag {
  margin-right: 8px;
}

.description {
  margin: 8px 0 0;
  color: #606266;
}

:deep(.el-timeline-item__timestamp) {
  color: #909399;
  font-size: 12px;
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

.tip-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.time-config {
  margin: 20px 0;
}

.time-inputs {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.time-input-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time-input-group label {
  width: 120px;
  text-align: right;
}

input[type="datetime-local"] {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  width: 250px;
}

input[type="datetime-local"]:focus {
  outline: none;
  border-color: #409eff;
}

select {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  width: 250px;
}

select:focus {
  outline: none;
  border-color: #409eff;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
}

.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.basic-config {
  padding: 20px;
  max-width: 800px;
}

.form-row {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}

.form-row label {
  width: 120px;
  text-align: right;
  padding-right: 12px;
  line-height: 32px;
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.input-wrapper {
  flex: 1;
  max-width: 400px;
}

select, textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  background-color: white;
}

select {
  height: 32px;
}

select:focus, textarea:focus {
  outline: none;
  border-color: #409eff;
}

textarea {
  resize: vertical;
  min-height: 80px;
}

.tip-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style> 