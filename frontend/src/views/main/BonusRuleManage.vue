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
                <select v-model="ruleForm.type">
                  <option value="思想政治">思想政治</option>
                  <option value="学习成绩">学习成绩</option>
                  <option value="科研创新">科研创新</option>
                  <option value="文体活动">文体活动</option>
                  <option value="社会工作">社会工作</option>
                </select>
              </div>
              <div class="form-group">
                <label>加分理由</label>
                <input type="text" v-model="ruleForm.reason" placeholder="请输入加分理由">
              </div>
              <div class="form-group">
                <label>级别设置</label>
                <div class="levels-container">
                  <div v-for="level in levels" :key="level.name" class="level-item">
                    <label>
                      <input type="checkbox" v-model="level.enabled">
                      {{ level.name }}
                    </label>
                    <input 
                      type="number" 
                      v-model="level.points"
                      :disabled="!level.enabled"
                      min="0"
                      max="100"
                      step="0.1"
                    >
                  </div>
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
import { ref, computed } from 'vue'
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

// 级别选项
const levels = ref([
  { name: '国家级', points: 0.5, enabled: false },
  { name: '省级', points: 0.4, enabled: false },
  { name: '市级', points: 0.3, enabled: false },
  { name: '校级', points: 0.2, enabled: false },
  { name: '院级', points: 0.1, enabled: false }
])

// 状态变量
const showAddModal = ref(false)
const showDeleteModal = ref(false)
const isEditing = ref(false)
const selectedRule = ref(null)

// 表单数据
const ruleForm = ref({
  type: '',
  reason: '',
  description: ''
})

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
         levels.value.some(level => level.enabled)
})

// 编辑规则
const editRule = (rule) => {
  isEditing.value = true
  selectedRule.value = rule
  ruleForm.value = {
    type: rule.type,
    reason: rule.reason,
    description: rule.description
  }
  // 重置并设置对应级别
  levels.value.forEach(level => {
    level.enabled = level.name === rule.level
    if (level.enabled) {
      level.points = rule.points
    }
  })
  showAddModal.value = true
}

// 删除规则
const deleteRule = (rule) => {
  selectedRule.value = rule
  showDeleteModal.value = true
}

// 确认删除
const confirmDelete = async () => {
  try {
    // 这里应该调用后端API删除规则
    // await fetch(\`/api/bonus-rules/\${selectedRule.value.id}\`, {
    //   method: 'DELETE'
    // })

    // 更新本地数据
    bonusRules.value = bonusRules.value.filter(
      rule => rule.id !== selectedRule.value.id
    )
    showDeleteModal.value = false
  } catch (error) {
    console.error('删除规则失败:', error)
  }
}

// 关闭弹窗
const closeModal = () => {
  showAddModal.value = false
  isEditing.value = false
  selectedRule.value = null
  ruleForm.value = {
    type: '',
    reason: '',
    description: ''
  }
  // 重置级别选项
  levels.value.forEach(level => {
    level.enabled = false
    level.points = 0
  })
}

// 提交规则
const submitRule = async () => {
  try {
    const enabledLevels = levels.value.filter(level => level.enabled)
    const newRules = enabledLevels.map(level => ({
      type: ruleForm.value.type,
      reason: ruleForm.value.reason,
      level: level.name,
      points: level.points,
      description: ruleForm.value.description
    }))

    // 这里应该调用后端API保存规则
    // const response = await fetch('/api/bonus-rules', {
    //   method: isEditing.value ? 'PUT' : 'POST',
    //   headers: {
    //     'Content-Type': 'application/json'
    //   },
    //   body: JSON.stringify(newRules)
    // })

    // 更新本地数据
    if (isEditing.value) {
      const index = bonusRules.value.findIndex(rule => rule.id === selectedRule.value.id)
      if (index !== -1) {
        bonusRules.value[index] = { ...selectedRule.value, ...newRules[0] }
      }
    } else {
      // 模拟后端返回的id
      newRules.forEach((rule, index) => {
        bonusRules.value.push({
          id: Date.now() + index,
          ...rule
        })
      })
    }

    closeModal()
  } catch (error) {
    console.error('提交规则失败:', error)
  }
}
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
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 8px;
}

.level-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.level-item label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  cursor: pointer;
}

.level-item input[type="number"] {
  width: 80px;
  padding: 4px 8px;
}
</style> 