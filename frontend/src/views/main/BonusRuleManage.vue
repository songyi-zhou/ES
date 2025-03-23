<template>
  <div class="bonus-rule-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>加分规则管理</h2>
          <button class="add-btn" @click="showAddModal = true">
            新增规则
          </button>
        </div>

        <!-- 规则列表 -->
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>加分类型</th>
                <th>加分理由</th>
                <th>级别</th>
                <th>分值</th>
                <th>描述说明</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in bonusRules" :key="rule.id">
                <td>{{ rule.type }}</td>
                <td>{{ rule.reason }}</td>
                <td>{{ rule.level }}</td>
                <td>{{ rule.points }}</td>
                <td>{{ rule.description }}</td>
                <td>
                  <button class="edit-btn" @click="editRule(rule)">编辑</button>
                  <button class="delete-btn" @click="deleteRule(rule)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 新增/编辑规则弹窗 -->
        <div v-if="showAddModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>{{ isEditing ? '编辑规则' : '新增规则' }}</h3>
              <button class="close-btn" @click="closeModal">×</button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label>加分类型</label>
                <select v-model="ruleForm.type" @change="handleTypeChange">
                  <option value="A">A类 - 思想政治</option>
                  <option value="C">C类 - 科研竞赛</option>
                  <option value="D">D类 - 文体活动</option>
                </select>
              </div>
              <div class="form-group">
                <label>加分理由</label>
                <input type="text" v-model="ruleForm.reason" placeholder="请输入加分理由">
              </div>
              <div v-if="ruleForm.type === 'D'" class="form-group">
                <label>活动类型</label>
                <select v-model="ruleForm.activityType" @change="handleActivityTypeChange">
                  <option value="society">社团活动</option>
                  <option value="other">其他文体活动</option>
                </select>
              </div>
              <div class="form-group">
                <label>级别设置</label>
                <div class="levels-container">
                  <template v-if="ruleForm.type === 'A'">
                    <div class="level-group">
                      <div v-for="level in commonLevels" :key="level.name" class="level-item">
                        <label>
                          <input 
                            type="radio" 
                            :name="'level-A'"
                            :checked="level.enabled"
                            @change="handleLevelSelect('A', level.name)"
                          >
                          {{ level.name }}
                        </label>
                        <input 
                          type="number" 
                          v-model="level.points"
                          :disabled="!level.enabled"
                          min="0"
                          max="100"
                          step="0.1"
                          placeholder="分值"
                        >
                      </div>
                    </div>
                  </template>
                  <template v-if="ruleForm.type === 'C'">
                    <div class="award-levels">
                      <div v-for="(levelGroup, groupIndex) in competitionLevels" :key="groupIndex" class="level-group">
                        <div class="level-group-title">{{ levelGroup.groupName }}</div>
                        <div class="award-items">
                          <div v-for="award in levelGroup.awards" :key="award.name" class="award-item">
                            <div class="award-radio">
                              <input 
                                type="radio"
                                :name="'level-C'"
                                :checked="award.enabled"
                                @change="handleLevelSelect('C', award.name, levelGroup.groupName)"
                              >
                              <label>{{ award.name }}</label>
                            </div>
                            <div class="award-input">
                              <input 
                                type="number" 
                                v-model="award.points"
                                :disabled="!award.enabled"
                                min="0"
                                max="100"
                                step="0.1"
                                placeholder="分值"
                              >
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                  <template v-if="ruleForm.type === 'D' && ruleForm.activityType === 'society'">
                    <div class="level-group">
                      <div v-for="level in societyLevels" :key="level.name" class="level-item">
                        <label>
                          <input 
                            type="radio"
                            :name="'level-D-society'"
                            :checked="level.enabled"
                            @change="handleLevelSelect('D', level.name)"
                          >
                          {{ level.name }}
                        </label>
                        <input 
                          type="number" 
                          v-model="level.points"
                          :disabled="!level.enabled"
                          min="0"
                          max="100"
                          step="0.1"
                          placeholder="分值"
                        >
                      </div>
                    </div>
                  </template>
                  <template v-if="ruleForm.type === 'D' && ruleForm.activityType === 'other'">
                    <div class="level-group">
                      <div v-for="level in otherActivityLevels" :key="level.name" class="level-item">
                        <label>
                          <input 
                            type="radio"
                            :name="'level-D-other'"
                            :checked="level.enabled"
                            @change="handleLevelSelect('D', level.name)"
                          >
                          {{ level.name }}
                        </label>
                        <input 
                          type="number" 
                          v-model="level.points"
                          :disabled="!level.enabled"
                          min="0"
                          max="100"
                          step="0.1"
                          placeholder="分值"
                        >
                      </div>
                    </div>
                  </template>
                </div>
              </div>
              <div class="form-group">
                <label>描述说明</label>
                <textarea v-model="ruleForm.description" placeholder="请输入详细说明"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button 
                class="submit-btn"
                @click="submitRule"
                :disabled="!canSubmit"
              >
                确认提交
              </button>
              <button class="cancel-btn" @click="closeModal">取消</button>
            </div>
          </div>
        </div>

        <!-- 删除确认弹窗 -->
        <div v-if="showDeleteModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>确认删除</h3>
              <button class="close-btn" @click="showDeleteModal = false">×</button>
            </div>
            <div class="modal-body">
              <p>确定要删除这条加分规则吗？删除后将无法恢复。</p>
            </div>
            <div class="modal-footer">
              <button class="delete-confirm-btn" @click="confirmDelete">确认删除</button>
              <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 加分规则数据
const bonusRules = ref([
  {
    id: 1,
    type: 'D',
    reason: '参加公益活动、志愿活动并取得证书',
    level: '国家级',
    points: 0.5,
    description: '个人参加公益活动、志愿活动并取得相应证书，国家级每次加0.5分'
  },
  {
    id: 2,
    type: 'D',
    reason: '参加公益活动、志愿活动并取得证书',
    level: '省级',
    points: 0.4,
    description: '个人参加公益活动、志愿活动并取得相应证书，省级每次加0.4分'
  }
])

// 通用级别（A类和C类行政级别使用）
const commonLevels = ref([
  { name: '国家级', points: 0, enabled: false },
  { name: '省级', points: 0, enabled: false },
  { name: '市级', points: 0, enabled: false },
  { name: '校级', points: 0, enabled: false },
  { name: '院级', points: 0, enabled: false }
])

// C类竞赛获奖等级
const competitionLevels = ref([
  {
    groupName: '国家级竞赛',
    awards: [
      { name: '一等奖', points: 0, enabled: false },
      { name: '二等奖', points: 0, enabled: false },
      { name: '三等奖', points: 0, enabled: false }
    ]
  },
  {
    groupName: '省级竞赛',
    awards: [
      { name: '一等奖', points: 0, enabled: false },
      { name: '二等奖', points: 0, enabled: false },
      { name: '三等奖', points: 0, enabled: false }
    ]
  },
  {
    groupName: '市级竞赛',
    awards: [
      { name: '一等奖', points: 0, enabled: false },
      { name: '二等奖', points: 0, enabled: false },
      { name: '三等奖', points: 0, enabled: false }
    ]
  },
  {
    groupName: '校级竞赛',
    awards: [
      { name: '一等奖', points: 0, enabled: false },
      { name: '二等奖', points: 0, enabled: false },
      { name: '三等奖', points: 0, enabled: false }
    ]
  },
  {
    groupName: '院级竞赛',
    awards: [
      { name: '一等奖', points: 0, enabled: false },
      { name: '二等奖', points: 0, enabled: false },
      { name: '三等奖', points: 0, enabled: false }
    ]
  }
])

// D类社团活动等级
const societyLevels = ref([
  { name: '优秀', points: 0, enabled: false },
  { name: '良好', points: 0, enabled: false },
  { name: '合格', points: 0, enabled: false }
])

// D类其他文体活动级别
const otherActivityLevels = ref([
  { name: '国家级', points: 0, enabled: false },
  { name: '省级', points: 0, enabled: false },
  { name: '市级', points: 0, enabled: false },
  { name: '校级', points: 0, enabled: false }
])

// 状态变量
const showAddModal = ref(false)
const showDeleteModal = ref(false)
const isEditing = ref(false)
const selectedRule = ref(null)

// 表单数据
const ruleForm = ref({
  type: 'A',
  reason: '',
  description: '',
  activityType: 'other'
})

// 编辑状态标记
const editingRule = ref(null)

// 获取加分类型名称
const getBonusTypeName = (type) => {
  const typeMap = {
    'A': 'A - 思想品德',
    'C': 'C - 科技创新',
    'D': 'D - 社会实践'
  }
  return typeMap[type] || type
}

// 是否可以提交
const canSubmit = computed(() => {
  return ruleForm.value.type && 
         ruleForm.value.reason && 
         ruleForm.value.description &&
         (ruleForm.value.type === 'A' || ruleForm.value.type === 'C' || ruleForm.value.type === 'D')
})

// 获取所有规则
const fetchRules = async () => {
  try {
    const response = await axios.get('/api/bonus-rules')
    bonusRules.value = response.data
  } catch (error) {
    ElMessage.error('获取规则列表失败')
    console.error('获取规则失败:', error)
  }
}

// 重置表单
const resetForm = () => {
  ruleForm.value = {
    type: 'A',
    reason: '',
    description: '',
    activityType: 'other'
  }
  editingRule.value = null
  resetAllLevels()
}

// 提交规则
const submitRule = async () => {
  if (!ruleForm.value.type || !ruleForm.value.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    let selectedLevels = []
    
    if (ruleForm.value.type === 'A') {
      const selectedLevel = commonLevels.value.find(level => level.enabled)
      if (!selectedLevel) {
        ElMessage.warning('请选择级别')
        return
      }
      selectedLevels = [{
        level: selectedLevel.name,
        points: selectedLevel.points
      }]
    } else if (ruleForm.value.type === 'C') {
      let selectedAward = null
      competitionLevels.value.forEach(group => {
        const award = group.awards.find(a => a.enabled)
        if (award) {
          selectedAward = {
            level: group.groupName,
            awardLevel: award.name,
            points: award.points
          }
        }
      })
      if (!selectedAward) {
        ElMessage.warning('请选择获奖等级')
        return
      }
      selectedLevels = [selectedAward]
    } else if (ruleForm.value.type === 'D') {
      const levels = ruleForm.value.activityType === 'society' ? societyLevels.value : otherActivityLevels.value
      const selectedLevel = levels.find(level => level.enabled)
      if (!selectedLevel) {
        ElMessage.warning('请选择级别')
        return
      }
      selectedLevels = [{
        level: selectedLevel.name,
        points: selectedLevel.points,
        activityType: ruleForm.value.activityType
      }]
    }

    // 构建提交数据
    const submitData = selectedLevels.map(level => ({
      type: ruleForm.value.type,
      reason: ruleForm.value.reason,
      level: level.level,
      awardLevel: level.awardLevel,
      activityType: level.activityType,
      points: level.points,
      description: ruleForm.value.description
    }))

    // 提交每个规则
    for (const data of submitData) {
      if (editingRule.value) {
        await axios.put(`/api/bonus-rules/${editingRule.value.id}`, data)
      } else {
        await axios.post('/api/bonus-rules', data)
      }
    }

    ElMessage.success(editingRule.value ? '更新成功' : '添加成功')
    await fetchRules()
    showAddModal.value = false
    resetForm()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
    console.error('提交失败:', error)
  }
}

// 编辑规则
const editRule = (rule) => {
  editingRule.value = rule
  ruleForm.value.type = rule.type
  ruleForm.value.reason = rule.reason
  ruleForm.value.description = rule.description
  
  resetAllLevels()
  
  if (rule.type === 'A') {
    const level = commonLevels.value.find(l => l.name === rule.level)
    if (level) {
      level.enabled = true
      level.points = rule.points
    }
  } else if (rule.type === 'C') {
    competitionLevels.value.forEach(group => {
      if (group.groupName === rule.level) {
        const award = group.awards.find(a => a.name === rule.awardLevel)
        if (award) {
          award.enabled = true
          award.points = rule.points
        }
      }
    })
  } else if (rule.type === 'D') {
    ruleForm.value.activityType = rule.activityType
    const levels = rule.activityType === 'society' ? societyLevels.value : otherActivityLevels.value
    const level = levels.find(l => l.name === rule.level)
    if (level) {
      level.enabled = true
      level.points = rule.points
    }
  }
  
  showAddModal.value = true
}

// 删除规则
const deleteRule = async (rule) => {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
      type: 'warning'
    })
    
    await axios.delete(`/api/bonus-rules/${rule.id}`)
    ElMessage.success('删除成功')
    await fetchRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('删除失败:', error)
    }
  }
}

// 关闭弹窗
const closeModal = () => {
  showAddModal.value = false
  isEditing.value = false
  selectedRule.value = null
  ruleForm.value = {
    type: 'A',
    reason: '',
    description: '',
    activityType: 'other'
  }
  // 重置级别选项
  if (ruleForm.value.type === 'A') {
    commonLevels.value.forEach(level => {
      level.enabled = false
      level.points = 0
    })
  } else if (ruleForm.value.type === 'C') {
    competitionLevels.value.forEach(group => {
      group.awards.forEach(award => {
        award.enabled = false
        award.points = 0
      })
    })
  } else if (ruleForm.value.type === 'D') {
    ruleForm.value.activityType = 'other'
    societyLevels.value.forEach(level => {
      level.enabled = false
      level.points = 0
    })
    otherActivityLevels.value.forEach(level => {
      level.enabled = false
      level.points = 0
    })
  }
}

// 处理加分类型变化
const handleTypeChange = () => {
  // 重置所有级别的选中状态
  resetAllLevels()
  
  // 如果是C类，重置活动类型
  if (ruleForm.value.type === 'C') {
    ruleForm.value.activityType = 'other'
  }
}

// 处理D类活动类型变化
const handleActivityTypeChange = () => {
  resetAllLevels()
}

// 重置所有级别
const resetAllLevels = () => {
  const resetLevel = (level) => {
    level.enabled = false
    level.points = 0
  }
  
  commonLevels.value.forEach(resetLevel)
  competitionLevels.value.forEach(group => {
    group.awards.forEach(resetLevel)
  })
  societyLevels.value.forEach(resetLevel)
  otherActivityLevels.value.forEach(resetLevel)
}

// 添加级别选择处理方法
const handleLevelSelect = (type, selectedName, groupName = null) => {
  if (type === 'A') {
    commonLevels.value.forEach(level => {
      level.enabled = level.name === selectedName
      if (!level.enabled) level.points = 0
    })
  } else if (type === 'C') {
    competitionLevels.value.forEach(group => {
      group.awards.forEach(award => {
        award.enabled = group.groupName === groupName && award.name === selectedName
        if (!award.enabled) award.points = 0
      })
    })
  } else if (type === 'D') {
    if (ruleForm.value.activityType === 'society') {
      societyLevels.value.forEach(level => {
        level.enabled = level.name === selectedName
        if (!level.enabled) level.points = 0
      })
    } else {
      otherActivityLevels.value.forEach(level => {
        level.enabled = level.name === selectedName
        if (!level.enabled) level.points = 0
      })
    }
  }
}

// 页面加载时获取规则列表
onMounted(() => {
  fetchRules()
})
</script>

<style scoped>
.bonus-rule-container {
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
  overflow-x: hidden;
  overflow-y: auto;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.page-header h2 {
  font-size: 24px;
  color: #303133;
  margin: 0;
  font-weight: 600;
}

.add-btn {
  padding: 10px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.add-btn:hover {
  background: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  padding: 0;
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1000px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  padding: 12px 8px;
  text-align: center;
  border-bottom: 2px solid #ebeef5;
  white-space: nowrap;
}

td {
  padding: 12px 8px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  font-size: 14px;
}

tr:hover {
  background-color: #f5f7fa;
}

/* 设置列宽 */
th:nth-child(1) { width: 15%; }  /* 加分类型 */
th:nth-child(2) { width: 20%; }  /* 加分理由 */
th:nth-child(3) { width: 10%; }  /* 级别 */
th:nth-child(4) { width: 10%; }  /* 分值 */
th:nth-child(5) { width: 30%; }  /* 描述说明 */
th:nth-child(6) { width: 15%; }  /* 操作 */

.description {
  text-align: left;
  white-space: normal;
  line-height: 1.5;
}

.edit-btn,
.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  margin: 0 4px;
  transition: all 0.3s;
}

.edit-btn {
  background: #409eff1a;
  color: #409eff;
}

.delete-btn {
  background: #f56c6c1a;
  color: #f56c6c;
}

.edit-btn:hover {
  background: #409eff33;
}

.delete-btn:hover {
  background: #f56c6c33;
}

/* 修改弹窗相关样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  animation: modalFadeIn 0.3s ease;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

.close-btn {
  border: none;
  background: none;
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: color 0.3s;
}

.close-btn:hover {
  color: #606266;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

/* 修改按钮样式 */
.submit-btn,
.cancel-btn,
.delete-confirm-btn {
  flex: 1;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
  text-align: center;
}

.submit-btn {
  background: #409eff;
  color: white;
  border: none;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.submit-btn:hover:not(:disabled) {
  background: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

.submit-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.cancel-btn {
  background: #f4f4f5;
  color: #606266;
  border: 1px solid #dcdfe6;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.cancel-btn:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.delete-confirm-btn {
  background: #f56c6c;
  color: white;
  border: none;
  box-shadow: 0 2px 6px rgba(245, 108, 108, 0.2);
}

.delete-confirm-btn:hover {
  background: #f78989;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(245, 108, 108, 0.3);
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

select,
input[type="text"],
textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
}

select:focus,
input[type="text"]:focus,
textarea:focus {
  border-color: #409eff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

textarea {
  resize: vertical;
  min-height: 80px;
}

.levels-container {
  margin-top: 16px;
}

.level-group {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;
}

.level-group-title {
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.level-group-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.level-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 4px;
}

.level-item label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  cursor: pointer;
  flex: 1;
}

.level-item input[type="number"] {
  width: 100px;
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.level-item input[type="number"]:disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

.award-levels {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.level-group {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.level-group-title {
  background: #f5f7fa;
  color: #1d2129;
  font-weight: 600;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
}

.award-items {
  padding: 16px;
}

.award-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.award-item:last-child {
  border-bottom: none;
}

.award-radio {
  display: flex;
  align-items: center;
  min-width: 120px;
  margin-right: 16px;
}

.award-radio input[type="radio"] {
  margin-right: 8px;
  cursor: pointer;
}

.award-radio label {
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

/* 美化单选框样式 */
input[type="radio"] {
  appearance: none;
  width: 16px;
  height: 16px;
  border: 2px solid #dcdfe6;
  border-radius: 50%;
  position: relative;
  transition: all 0.3s;
}

input[type="radio"]:checked {
  border-color: #409eff;
}

input[type="radio"]:checked::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

input[type="radio"]:hover {
  border-color: #409eff;
}

.award-input {
  flex: 1;
  max-width: 150px;
}

.award-input input {
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.3s;
}

.award-input input:focus {
  border-color: #409eff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.award-input input:disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
  border-color: #e4e7ed;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .award-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .award-radio {
    margin-right: 0;
  }
  
  .award-input {
    width: 100%;
    max-width: none;
  }
}
</style> 