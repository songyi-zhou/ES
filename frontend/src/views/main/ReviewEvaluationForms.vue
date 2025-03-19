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
              待审核文件：<span class="count">{{ pendingCount }}</span> 个
            </div>
            <!-- 添加批量操作按钮 -->
            <div class="batch-actions" v-if="selectedForms.length > 0">
              <button class="batch-btn approve" @click="batchApprove">
                批量通过 ({{ selectedForms.length }})
              </button>
              <button class="batch-btn export" @click="batchExport">
                批量导出
              </button>
            </div>
          </div>
        </div>

        <!-- 文件列表 -->
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th width="50">
                  <input 
                    type="checkbox" 
                    :checked="isAllSelected"
                    @change="toggleAllSelection"
                  >
                </th>
                <th>提交时间</th>
                <th>学号</th>
                <th>姓名</th>
                <th>文件名称</th>
                <th>文件类型</th>
                <th>提交说明</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="form in evaluationForms" :key="form.id">
                <td>
                  <input 
                    type="checkbox" 
                    :checked="isSelected(form)"
                    @change="toggleSelection(form)"
                  >
                </td>
                <td>{{ formatDate(form.submitTime) }}</td>
                <td>{{ form.studentId }}</td>
                <td>{{ form.studentName }}</td>
                <td class="file-name">
                  <span class="link" @click="previewFile(form)">
                    {{ form.fileName }}
                  </span>
                </td>
                <td>{{ form.fileType }}</td>
                <td>{{ form.description }}</td>
                <td class="actions">
                  <button class="approve-btn" @click="approveForm(form)">通过</button>
                  <button class="comment-btn" @click="openCommentModal(form)">批注</button>
                  <button class="return-btn" @click="openReturnModal(form)">退回</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 批注弹窗 -->
        <div v-if="showCommentModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>添加批注</h3>
              <button class="close-btn" @click="closeCommentModal">×</button>
            </div>
            <div class="modal-body">
              <div class="form-info">
                <p><strong>学生姓名：</strong>{{ selectedForm?.studentName }}</p>
                <p><strong>文件名称：</strong>{{ selectedForm?.fileName }}</p>
              </div>
              
              <div class="form-group">
                <label>批注内容：</label>
                <textarea 
                  v-model="commentForm.content"
                  rows="4"
                  placeholder="请输入批注内容，指出需要修改的问题"
                ></textarea>
              </div>

              <div class="form-group">
                <label>问题类型：</label>
                <select v-model="commentForm.type">
                  <option value="">请选择问题类型</option>
                  <option v-for="type in problemTypes" :key="type.value" :value="type.value">
                    {{ type.label }}
                  </option>
                </select>
              </div>
            </div>
            <div class="modal-footer">
              <button 
                class="submit-btn" 
                @click="submitComment"
                :disabled="!canSubmitComment"
              >
                保存批注
              </button>
              <button class="cancel-btn" @click="closeCommentModal">取消</button>
            </div>
          </div>
        </div>

        <!-- 退回弹窗 -->
        <div v-if="showReturnModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>退回文件</h3>
              <button class="close-btn" @click="closeReturnModal">×</button>
            </div>
            <div class="modal-body">
              <div class="form-info">
                <p><strong>学生姓名：</strong>{{ selectedForm?.studentName }}</p>
                <p><strong>文件名称：</strong>{{ selectedForm?.fileName }}</p>
              </div>
              
              <div class="form-group">
                <label>退回原因：</label>
                <textarea 
                  v-model="returnForm.reason"
                  rows="4"
                  placeholder="请输入退回原因"
                ></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button 
                class="submit-btn" 
                @click="submitReturn"
                :disabled="!canSubmitReturn"
              >
                确认退回
              </button>
              <button class="cancel-btn" @click="closeReturnModal">取消</button>
            </div>
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
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import * as XLSX from 'xlsx' // 需要安装：npm install xlsx

// 模拟数据
const evaluationForms = ref([
  {
    id: 1,
    submitTime: '2024-03-15 14:30:00',
    studentId: '2021001001',
    studentName: '张三',
    fileName: '2024年综测表格.pdf',
    fileType: '综测总表',
    description: '2024年综测评分表',
    fileUrl: '/files/form1.pdf'
  }
])

// 问题类型
const problemTypes = [
  { value: 'format_error', label: '格式错误' },
  { value: 'data_error', label: '数据有误' },
  { value: 'missing_info', label: '信息缺失' },
  { value: 'other', label: '其他问题' }
]

// 状态变量
const showCommentModal = ref(false)
const showReturnModal = ref(false)
const showPreviewModal = ref(false)
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
const batchApprove = () => {
  showBatchApproveModal.value = true
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

.file-name {
  color: #409eff;
  cursor: pointer;
}

.file-name:hover {
  text-decoration: underline;
}

.actions {
  display: flex;
  gap: 8px;
}

.approve-btn,
.comment-btn,
.return-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.approve-btn {
  background: #67c23a;
  color: white;
}

.comment-btn {
  background: #409eff;
  color: white;
}

.return-btn {
  background: #f56c6c;
  color: white;
}

.approve-btn:hover {
  background: #85ce61;
}

.comment-btn:hover {
  background: #66b1ff;
}

.return-btn:hover {
  background: #f78989;
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
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.submit-btn,
.cancel-btn {
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn {
  background: #409eff;
  color: white;
  border: none;
}

.submit-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.cancel-btn {
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #dcdfe6;
}

.submit-btn:not(:disabled):hover {
  background: #66b1ff;
}

.cancel-btn:hover {
  background: #f9f9fa;
}

.batch-actions {
  display: flex;
  gap: 10px;
}

.batch-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.batch-btn.approve {
  background: #67c23a;
  color: white;
}

.batch-btn.export {
  background: #409eff;
  color: white;
}

.batch-btn:hover {
  opacity: 0.9;
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

/* Checkbox styles */
input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

th:first-child,
td:first-child {
  text-align: center;
}
</style> 