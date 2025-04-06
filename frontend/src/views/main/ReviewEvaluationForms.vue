<template>
  <div class="review-forms-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综测结果审核</h2>
          <div class="header-right">
            <div class="stats">
              实际表格行数：<span class="count">{{ pendingCount }}</span> 行
            </div>
            <!-- 添加批量操作按钮 -->
            <div class="batch-operation" v-if="evaluationForms.length > 0">
              <button class="batch-btn approve" @click="batchApprove">
                <i class="el-icon-check"></i> 整体通过
              </button>
              <button class="batch-btn reject" @click="batchReject">
                <i class="el-icon-close"></i> 整体退回
              </button>
            </div>
          </div>
        </div>

        <!-- 筛选部分 -->
        <div class="filter-section">
          <div class="filter-row">
            <div class="form-group">
              <label>表格种类：</label>
              <select v-model="filterForm.formType">
                <option value="">请选择表格种类</option>
                <option v-for="type in problemTypes" :key="type.value" :value="type.value">
                  {{ type.label }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>专业：</label>
              <select v-model="filterForm.major">
                <option value="">请选择专业</option>
                <option v-for="major in majors" :key="major.id" :value="major.id">
                  {{ major.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>班级：</label>
              <select v-model="filterForm.classId">
                <option value="">请选择班级</option>
                <option v-for="classItem in classes" :key="classItem.id" :value="classItem.id">
                  {{ classItem.name }}
                </option>
              </select>
            </div>
          </div>
          <div class="filter-actions">
            <button class="search-btn" @click="handleSearch">查询</button>
            <button class="reset-btn" @click="resetFilter">重置</button>
          </div>
        </div>

        <!-- 文件列表 -->
        <div class="table-container">
          <table v-if="evaluationForms.length > 0">
            <thead>
              <tr>
                <th>姓名</th>
                <th>学号</th>
                <th>基础分</th>
                <th>总加分</th>
                <th>总扣分</th>
                <th>原始总分</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="form in evaluationForms" :key="form.id">
                <td>{{ form.studentName }}</td>
                <td>{{ form.studentId }}</td>
                <td>{{ form.baseScore }}</td>
                <td>{{ form.totalBonus }}</td>
                <td>{{ form.totalPenalty }}</td>
                <td>{{ form.rawScore }}</td>
                <td class="actions">
                  <button class="view-btn" @click="viewMaterials(form)">查看证明材料</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="no-data">
            <i class="el-icon-document"></i>
            <p>暂无数据</p>
          </div>
        </div>

        <!-- 预览弹窗 -->
        <div v-if="showPreviewModal" class="modal">
          <div class="modal-content preview-modal">
            <div class="modal-header">
              <h3>文件预览</h3>
              <button class="close-btn" @click="showPreviewModal = false">×</button>
            </div>
            <div class="preview-container">
              <iframe :src="previewUrl" type="application/pdf" width="100%" height="100%"></iframe>
            </div>
          </div>
        </div>

        <!-- 添加批量审批确认弹窗 -->
        <div v-if="showBatchApproveModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>批量审批确认</h3>
              <button class="close-btn" @click="showBatchApproveModal = false">×</button>
            </div>
            <div class="modal-body">
              <p>确定要批量通过以下 {{ selectedForms.length }} 个文件吗？</p>
              <div class="selected-files">
                <div v-for="form in selectedForms" :key="form.id" class="selected-file">
                  {{ form.studentName }} - {{ form.fileName }}
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="submit-btn" @click="confirmBatchApprove">确认通过</button>
              <button class="cancel-btn" @click="showBatchApproveModal = false">取消</button>
            </div>
          </div>
        </div>

        <!-- 查看证明材料弹窗 -->
        <div v-if="showMaterialsModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>证明材料</h3>
              <button class="close-btn" @click="showMaterialsModal = false">×</button>
            </div>
            <div class="materials-container">
              <div v-for="material in materials" :key="material.id" class="material-item">
                <div class="material-info">
                  {{ material.description }}
                </div>
                <div class="material-preview">
                  <iframe :src="material.url" width="100%" height="100%"></iframe>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import * as XLSX from 'xlsx' // 需要安装：npm install xlsx
import request from '@/utils/request'

// 评测表数据
const evaluationForms = ref([])

// 问题类型
const problemTypes = [
  { value: 'moral_monthly_evaluation', label: 'A类月表' },
  { value: 'research_competition_evaluation', label: 'C类' },
  { value: 'sports_arts_evaluation', label: 'D类' },
  { value: 'moral_semester_evaluation', label: 'A类总表' },
  { value: 'comprehensive_result', label: '综测结果表' }
]

// 状态变量
const showCommentModal = ref(false)
const showReturnModal = ref(false)
const showPreviewModal = ref(false)
const showMaterialsModal = ref(false)
const selectedForm = ref(null)
const previewUrl = ref('')
const pendingCount = ref(evaluationForms.value.length)

// 表单数据
const commentForm = ref({
  content: '',
  type: ''
})

const returnForm = ref({
  reason: ''
})

// 表单验证
const canSubmitComment = computed(() => {
  return commentForm.value.content.trim() !== '' && commentForm.value.type !== ''
})

const canSubmitReturn = computed(() => {
  return returnForm.value.reason.trim() !== ''
})

// 格式化日期
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 通过表格
const approveForm = async (form) => {
  try {
    // 这里应该调用后端API提交审核通过
    // await fetch(`/api/evaluation-forms/${form.id}/approve`, {
    //   method: 'POST'
    // })

    // 更新本地数据
    evaluationForms.value = evaluationForms.value.filter(f => f.id !== form.id)
    pendingCount.value = evaluationForms.value.length
  } catch (error) {
    console.error('审核失败:', error)
  }
}

// 显示批注弹窗
const openCommentModal = (form) => {
  selectedForm.value = form
  showCommentModal.value = true
}

// 显示退回弹窗
const openReturnModal = (form) => {
  selectedForm.value = form
  showReturnModal.value = true
}

// 预览文件
const previewFile = (form) => {
  selectedForm.value = form
  previewUrl.value = form.fileUrl
  showPreviewModal.value = true
}

// 关闭批注弹窗
const closeCommentModal = () => {
  showCommentModal.value = false
  selectedForm.value = null
  commentForm.value = {
    content: '',
    type: ''
  }
}

// 关闭退回弹窗
const closeReturnModal = () => {
  showReturnModal.value = false
  selectedForm.value = null
  returnForm.value = {
    reason: ''
  }
}

// 提交批注
const submitComment = async () => {
  try {
    // 这里应该调用后端API提交批注
    // await fetch(`/api/evaluation-forms/${selectedForm.value.id}/comment`, {
    //   method: 'POST',
    //   headers: {
    //     'Content-Type': 'application/json'
    //   },
    //   body: JSON.stringify(commentForm.value)
    // })

    closeCommentModal()
  } catch (error) {
    console.error('提交批注失败:', error)
  }
}

// 提交退回
const submitReturn = async () => {
  try {
    // 这里应该调用后端API提交退回
    // await fetch(`/api/evaluation-forms/${selectedForm.value.id}/return`, {
    //   method: 'POST',
    //   headers: {
    //     'Content-Type': 'application/json'
    //   },
    //   body: JSON.stringify(returnForm.value)
    // })

    // 更新本地数据
    evaluationForms.value = evaluationForms.value.filter(
      f => f.id !== selectedForm.value.id
    )
    pendingCount.value = evaluationForms.value.length

    closeReturnModal()
  } catch (error) {
    console.error('退回失败:', error)
  }
}

// 新增的状态
const selectedForms = ref([])
const showBatchApproveModal = ref(false)

// 计算属性：是否全选
const isAllSelected = computed(() => {
  return evaluationForms.value.length > 0 && 
         selectedForms.value.length === evaluationForms.value.length
})

// 选择相关方法
const isSelected = (form) => {
  return selectedForms.value.some(f => f.id === form.id)
}

const toggleSelection = (form) => {
  const index = selectedForms.value.findIndex(f => f.id === form.id)
  if (index === -1) {
    selectedForms.value.push(form)
  } else {
    selectedForms.value.splice(index, 1)
  }
}

const toggleAllSelection = () => {
  if (isAllSelected.value) {
    selectedForms.value = []
  } else {
    selectedForms.value = [...evaluationForms.value]
  }
}

// 批量审批相关方法
const batchApprove = async () => {
  try {
    const requestBody = {
      formType: filterForm.value.formType,
      major: filterForm.value.major,
      classId: filterForm.value.classId
    }
    
    const response = await request.post('/api/review/batch-approve', requestBody)
    if (response.data.success) {
      ElMessage.success('批量审核成功')
      // 重新加载数据
      handleSearch()
    }
  } catch (error) {
    console.error('批量审核失败:', error)
    ElMessage.error('批量审核失败，请稍后重试')
  }
}

const confirmBatchApprove = async () => {
  try {
    // 这里应该调用后端API进行批量审批
    // await Promise.all(selectedForms.value.map(form => 
    //   fetch(`/api/evaluation-forms/${form.id}/approve`, { method: 'POST' })
    // ))

    // 更新本地数据
    evaluationForms.value = evaluationForms.value.filter(
      form => !selectedForms.value.some(selected => selected.id === form.id)
    )
    pendingCount.value = evaluationForms.value.length
    selectedForms.value = []
    showBatchApproveModal.value = false
  } catch (error) {
    console.error('批量审批失败:', error)
  }
}

// 批量导出为Excel
const batchExport = () => {
  const exportData = selectedForms.value.map(form => ({
    '学号': form.studentId,
    '姓名': form.studentName,
    '文件类型': form.fileType,
    '提交时间': formatDate(form.submitTime),
    '提交说明': form.description
  }))

  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '综测审核结果')
  
  // 生成Excel文件并下载
  XLSX.writeFile(wb, `综测审核结果_${formatDate(new Date())}.xlsx`)
}

// 筛选表单数据
const filterForm = ref({
  formType: '',
  major: '',
  classId: ''
})

// 专业和班级数据
const majors = ref([])
const classes = ref([])

// 获取专业列表
const getMajors = async () => {
  try {
    const response = await request.get('/classes/majors')
    if (response.data.success) {
      // 将字符串数组转换为包含id和name的对象数组
      majors.value = response.data.data.map((majorName, index) => ({
        id: majorName,
        name: majorName
      }))
    }
  } catch (error) {
    console.error('获取专业列表失败:', error)
    ElMessage.error('获取专业列表失败')
  }
}

// 获取班级列表
const getClasses = async () => {
  if (!filterForm.value.major) {
    classes.value = []
    return
  }
  
  try {
    const response = await request.get('/classes/by-major', {
      params: { major: filterForm.value.major }
    })
    if (response.data.success) {
      // 直接使用返回的对象数组，不需要转换
      classes.value = response.data.data
    }
  } catch (error) {
    console.error('获取班级列表失败:', error)
    ElMessage.error('获取班级列表失败')
  }
}

// 监听专业变化
watch(() => filterForm.value.major, (newVal) => {
  if (newVal) {
    getClasses()
  } else {
    classes.value = []
  }
})

// 查询评测表数据
const handleSearch = async () => {
  try {
    const requestBody = {
      formType: filterForm.value.formType,
      major: filterForm.value.major,
      classId: filterForm.value.classId
    }
    
    const response = await request.post('/review/evaluation-forms', requestBody)
    if (response.data.success) {
      evaluationForms.value = response.data.data.forms || []
      pendingCount.value = response.data.data.pendingCount || 0
    }
  } catch (error) {
    console.error('查询评测表失败:', error)
    ElMessage.error('查询失败，请稍后重试')
  }
}

// 重置筛选条件
const resetFilter = () => {
  filterForm.value = {
    formType: '',
    major: '',
    classId: ''
  }
  handleSearch()
}

// 查看证明材料
const viewMaterials = async (form) => {
  try {
    const response = await fetch(`/api/evaluation-forms/${form.id}/materials`)
    materials.value = await response.json()
    showMaterialsModal.value = true
  } catch (error) {
    console.error('获取证明材料失败:', error)
    ElMessage.error('获取证明材料失败，请稍后重试')
  }
}

onMounted(() => {
  getMajors()
})
</script>

<style scoped>
.review-forms-container {
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

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stats {
  font-size: 16px;
}

.count {
  color: #f56c6c;
  font-weight: bold;
  font-size: 20px;
}

.filter-section {
  background: white;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.filter-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  background-color: #fff;
  transition: border-color 0.2s;
}

.form-group select:hover {
  border-color: #c0c4cc;
}

.form-group select:focus {
  border-color: #409eff;
  outline: none;
}

.filter-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.search-btn,
.reset-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.search-btn {
  background: #409eff;
  color: white;
}

.reset-btn {
  background: #909399;
  color: white;
}

.search-btn:hover {
  background: #66b1ff;
}

.reset-btn:hover {
  background: #a6a9ad;
}

.batch-operation {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.batch-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-btn.approve {
  background: #67c23a;
  color: white;
}

.batch-btn.reject {
  background: #f56c6c;
  color: white;
}

.batch-btn:hover {
  opacity: 0.9;
}

.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.no-data i {
  font-size: 48px;
  margin-bottom: 16px;
}

.no-data p {
  font-size: 16px;
  margin: 0;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
  padding: 12px 16px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
}

td {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  text-align: center;
}

tr:hover {
  background-color: #f5f7fa;
}

tr:last-child td {
  border-bottom: none;
}

.actions {
  display: flex;
  justify-content: center;
  align-items: center;
}

.view-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  background: #409eff;
  color: white;
  min-width: 100px;
}

.view-btn:hover {
  background: #66b1ff;
}

/* Modal styles */
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
  max-width: 600px;
}

.preview-modal {
  max-width: 800px;
  height: 80vh;
}

.preview-container {
  height: calc(100% - 60px);
  margin-top: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
}

.form-info {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.form-info p {
  margin: 8px 0;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

select,
textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

textarea {
  resize: vertical;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.submit-btn,
.cancel-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn {
  background: #409eff;
  color: white;
}

.cancel-btn {
  background: #909399;
  color: white;
}

.submit-btn:hover {
  background: #66b1ff;
}

.cancel-btn:hover {
  background: #a6a9ad;
}

.submit-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.materials-container {
  max-height: 60vh;
  overflow-y: auto;
}

.material-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.material-info {
  margin-bottom: 10px;
  font-weight: 500;
}

.material-preview {
  height: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.selected-files {
  max-height: 200px;
  overflow-y: auto;
  margin: 10px 0;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.selected-file {
  padding: 5px;
  border-bottom: 1px solid #ebeef5;
}

.selected-file:last-child {
  border-bottom: none;
}
</style> 