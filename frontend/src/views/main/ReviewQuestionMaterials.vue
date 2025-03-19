<template>
  <div class="review-materials-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="review-question-materials">
          <div class="page-header">
            <h2>疑问材料审核</h2>
            <div class="header-right">
              <div class="stats">
                待审核材料：<span class="count">{{ pendingCount }}</span> 个
              </div>
              <!-- 添加批量操作按钮 -->
              <div class="batch-actions" v-if="selectedMaterials.length > 0">
                <button class="batch-btn approve" @click="batchApprove">
                  批量通过 ({{ selectedMaterials.length }})
                </button>
                <button class="batch-btn report" @click="batchReport">
                  批量上报 ({{ selectedMaterials.length }})
                </button>
              </div>
            </div>
            <div class="filter-section">
              <el-select v-model="filterStatus" placeholder="审核状态" clearable>
                <el-option label="待审核" value="pending" />
                <el-option label="已通过" value="approved" />
                <el-option label="已驳回" value="rejected" />
              </el-select>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索学生姓名/学号"
                clearable
                class="search-input"
              />
            </div>
          </div>

          <div class="table-container">
            <el-table 
              :data="filteredQuestionMaterials" 
              style="width: 100%"
              @selection-change="handleSelectionChange"
            >
              <!-- 添加复选框列 -->
              <el-table-column type="selection" width="55" />
              
              <el-table-column prop="submitTime" label="提交时间" width="180" />
              <el-table-column prop="studentId" label="学号" width="120" />
              <el-table-column prop="studentName" label="姓名" width="100" />
              <el-table-column prop="materialName" label="材料名称" width="150" />
              <el-table-column prop="requestedCategory" label="申请类别" width="120" />
              <el-table-column prop="requestedPoints" label="申请分数" width="100" />
              <el-table-column prop="questionRaiser" label="提出人" width="120" />
              <el-table-column prop="questionDescription" label="疑问描述" min-width="200">
                <template #default="{ row }">
                  <div class="description-cell">{{ row.questionDescription }}</div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-button 
                      type="primary" 
                      size="small" 
                      @click="openReviewDialog(row)"
                      :disabled="row.status !== 'pending'"
                    >
                      审核
                    </el-button>
                    <el-button 
                      type="info" 
                      size="small" 
                      @click="viewMaterial(row)"
                    >
                      查看材料
                    </el-button>
                    <!-- 添加上报按钮 -->
                    <el-button 
                      type="warning" 
                      size="small" 
                      @click="reportMaterial(row)"
                      :disabled="row.status !== 'pending'"
                    >
                      上报
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 30, 50]"
                layout="total, sizes, prev, pager, next"
              />
            </div>
          </div>

          <!-- 审核对话框 -->
          <el-dialog
            v-model="reviewDialogVisible"
            title="材料审核"
            width="500px"
            :close-on-click-modal="false"
          >
            <div class="review-form">
              <div class="material-info">
                <p><strong>学生信息：</strong>{{ currentMaterial?.studentName }} ({{ currentMaterial?.studentId }})</p>
                <p><strong>材料名称：</strong>{{ currentMaterial?.materialName }}</p>
                <p><strong>申请类别：</strong>{{ currentMaterial?.requestedCategory }}</p>
                <p><strong>申请分数：</strong>{{ currentMaterial?.requestedPoints }}</p>
                <p><strong>疑问描述：</strong>{{ currentMaterial?.questionDescription }}</p>
              </div>
              <el-form :model="reviewForm" label-width="100px">
                <el-form-item label="审核结果">
                  <el-radio-group v-model="reviewForm.status">
                    <el-radio label="approved">通过</el-radio>
                    <el-radio label="rejected">驳回</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="审核意见">
                  <el-input
                    v-model="reviewForm.comment"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入审核意见"
                  />
                </el-form-item>
              </el-form>
            </div>
            <template #footer>
              <div class="dialog-footer">
                <el-button @click="reviewDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitReview" :loading="submitting">
                  提交
                </el-button>
              </div>
            </template>
          </el-dialog>

          <!-- 材料预览对话框 -->
          <el-dialog
            v-model="previewDialogVisible"
            title="材料预览"
            width="800px"
          >
            <div class="preview-content">
              <div class="material-preview">
                <!-- 这里可以根据材料类型显示不同的预览内容 -->
                <img v-if="currentMaterial?.materialType === 'image'" :src="currentMaterial?.materialUrl" />
                <iframe v-else-if="currentMaterial?.materialType === 'pdf'" :src="currentMaterial?.materialUrl" />
                <div v-else>暂不支持预览该类型的材料</div>
              </div>
            </div>
          </el-dialog>

          <!-- 添加上报确认对话框 -->
          <el-dialog
            v-model="reportDialogVisible"
            title="上报材料"
            width="500px"
            :close-on-click-modal="false"
          >
            <div class="report-form">
              <p>确定要将以下材料上报给导员审核吗？</p>
              <div class="selected-materials">
                <div v-for="material in selectedMaterials" :key="material.id" class="material-item">
                  <p>{{ material.studentName }} - {{ material.materialName }}</p>
                </div>
              </div>
              <el-form :model="reportForm" label-width="100px">
                <el-form-item label="补充说明">
                  <el-input
                    v-model="reportForm.note"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入补充说明（选填）"
                  />
                </el-form-item>
              </el-form>
            </div>
            <template #footer>
              <div class="dialog-footer">
                <el-button @click="reportDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitReport" :loading="submitting">
                  确认上报
                </el-button>
              </div>
            </template>
          </el-dialog>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 表格数据
const questionMaterials = ref([
  {
    id: 1,
    submitTime: '2024-03-20 10:30:00',
    studentId: '2021001001',
    studentName: '张三',
    materialName: '志愿者证书.pdf',
    requestedCategory: 'A类',
    requestedPoints: 2.5,
    questionRaiser: '李四',
    questionDescription: '该证书的志愿时长计算方式存在疑问，建议重新核实',
    status: 'pending',
    materialType: 'pdf',
    materialUrl: '/path/to/material.pdf'
  },
  // 更多数据...
])

// 筛选和搜索
const filterStatus = ref('')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

// 计算筛选后的数据
const filteredQuestionMaterials = computed(() => {
  let result = questionMaterials.value

  if (filterStatus.value) {
    result = result.filter(item => item.status === filterStatus.value)
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(item => 
      item.studentName.toLowerCase().includes(keyword) ||
      item.studentId.toLowerCase().includes(keyword)
    )
  }

  return result
})

// 审核相关
const reviewDialogVisible = ref(false)
const currentMaterial = ref(null)
const reviewForm = ref({
  status: '',
  comment: ''
})
const submitting = ref(false)

const openReviewDialog = (row) => {
  currentMaterial.value = row
  reviewForm.value = {
    status: '',
    comment: ''
  }
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewForm.value.status) {
    ElMessage.warning('请选择审核结果')
    return
  }

  if (!reviewForm.value.comment.trim()) {
    ElMessage.warning('请输入审核意见')
    return
  }

  try {
    submitting.value = true
    // TODO: 调用审核API
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    ElMessage.success('审核提交成功')
    reviewDialogVisible.value = false
    
    // 更新列表数据
    const index = questionMaterials.value.findIndex(item => item.id === currentMaterial.value.id)
    if (index !== -1) {
      questionMaterials.value[index].status = reviewForm.value.status
    }
  } catch (error) {
    ElMessage.error('审核提交失败')
  } finally {
    submitting.value = false
  }
}

// 材料预览相关
const previewDialogVisible = ref(false)

const viewMaterial = (row) => {
  currentMaterial.value = row
  previewDialogVisible.value = true
}

// 状态数据
const selectedMaterials = ref([])
const pendingCount = ref(0)
const showRejectModal = ref(false)
const showReportModal = ref(false)
const rejectReason = ref('')
const reportNote = ref('')
const selectedForReport = ref([])

// 计算属性
const isAllSelected = computed(() => {
  return questionMaterials.value.length > 0 && 
         selectedMaterials.value.length === questionMaterials.value.length
})

// 选择相关方法
const isSelected = (material) => {
  return selectedMaterials.value.some(m => m.id === material.id)
}

const toggleSelection = (material) => {
  const index = selectedMaterials.value.findIndex(m => m.id === material.id)
  if (index === -1) {
    selectedMaterials.value.push(material)
  } else {
    selectedMaterials.value.splice(index, 1)
  }
}

const toggleAllSelection = () => {
  if (isAllSelected.value) {
    selectedMaterials.value = []
  } else {
    selectedMaterials.value = [...questionMaterials.value]
  }
}

// 批量操作方法
const batchApprove = async () => {
  try {
    // 调用批量通过API
    // await approveMaterials(selectedMaterials.value.map(m => m.id))
    // 更新状态
    selectedMaterials.value = []
  } catch (error) {
    console.error('批量通过失败:', error)
  }
}

// 单个操作方法
const approveMaterial = async (material) => {
  try {
    // 调用通过API
    // await approveMaterial(material.id)
    // 更新状态
  } catch (error) {
    console.error('通过失败:', error)
  }
}

const openRejectModal = (material) => {
  selectedForReport.value = [material]
  showRejectModal.value = true
}

// 弹窗相关方法
const closeRejectModal = () => {
  showRejectModal.value = false
  rejectReason.value = ''
}

const confirmReject = async () => {
  if (!rejectReason.value) {
    alert('请输入驳回原因')
    return
  }

  try {
    // 调用驳回API
    // await rejectMaterial(selectedForReport.value[0].id, rejectReason.value)
    closeRejectModal()
  } catch (error) {
    console.error('驳回失败:', error)
  }
}

// 添加新的响应式变量
const reportDialogVisible = ref(false)
const reportForm = ref({
  note: ''
})

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedMaterials.value = selection
}

// 单个材料上报
const reportMaterial = (row) => {
  selectedMaterials.value = [row]
  reportDialogVisible.value = true
}

// 批量上报
const batchReport = () => {
  if (selectedMaterials.value.length === 0) {
    ElMessage.warning('请选择要上报的材料')
    return
  }
  reportDialogVisible.value = true
}

// 提交上报
const submitReport = async () => {
  try {
    submitting.value = true
    // TODO: 调用上报API
    // const response = await reportMaterials({
    //   materials: selectedMaterials.value.map(m => m.id),
    //   note: reportForm.value.note
    // })
    
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟API调用
    
    ElMessage.success('上报成功')
    reportDialogVisible.value = false
    reportForm.value.note = ''
    selectedMaterials.value = []
    
    // 刷新列表数据
    // await fetchData()
  } catch (error) {
    ElMessage.error('上报失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.review-materials-container {
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

.review-question-materials {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 20px 0;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stats {
  font-size: 14px;
  color: #666;
}

.count {
  font-weight: bold;
  color: #333;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.batch-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.batch-btn.approve {
  background: #67c23a;
  color: white;
}

.batch-btn.report {
  background: #409eff;
  color: white;
}

.filter-section {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.search-input {
  width: 200px;
}

.table-container {
  background: white;
  border-radius: 4px;
  padding: 20px;
}

.description-cell {
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.5;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.review-form {
  .material-info {
    background: #f5f7fa;
    padding: 15px;
    border-radius: 4px;
    margin-bottom: 20px;

    p {
      margin: 8px 0;
    }
  }
}

.preview-content {
  max-height: 600px;
  overflow-y: auto;
}

.material-preview {
  width: 100%;
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fa;

  img, iframe {
    max-width: 100%;
    max-height: 500px;
    object-fit: contain;
  }

  iframe {
    width: 100%;
    height: 500px;
    border: none;
  }
}

/* 确保表格内容不会溢出 */
.table-container {
  overflow-x: auto;
}

:deep(.el-table) {
  th {
    background-color: #f5f7fa;
    color: #333;
  }
  
  td {
    color: #666;
  }
}

:deep(.el-dialog__body) {
  padding: 20px;
}

.selected-materials {
  max-height: 200px;
  overflow-y: auto;
  margin: 10px 0;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.material-item {
  padding: 8px;
  border-bottom: 1px solid #ebeef5;
}

.material-item:last-child {
  border-bottom: none;
}

.status {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.status.pending {
  background: #e6a23c;
  color: white;
}

.status.approved {
  background: #67c23a;
  color: white;
}

.status.rejected {
  background: #f56c6c;
  color: white;
}

.status.reported {
  background: #409eff;
  color: white;
}

.description {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 调整表格内按钮的间距 */
.el-button + .el-button {
  margin-left: 0;
}
</style> 