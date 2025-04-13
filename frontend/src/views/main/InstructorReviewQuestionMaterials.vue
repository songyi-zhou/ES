<template>
  <div class="app-container">
    <TopBar />
    <div class="main-container">
      <Sidebar />
      <div class="content-container">
        <div class="instructor-review-materials">
          <div class="page-header">
            <h2>导员疑问材料审核</h2>
            <div class="filter-section">
              <el-select v-model="filterStatus" placeholder="审核状态" clearable>
                <el-option label="待审核" value="REPORTED" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已驳回" value="REJECTED" />
              </el-select>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索学生姓名/学号"
                class="search-input"
              />
            </div>
          </div>

          <div class="table-container">
            <el-table
              v-loading="loading"
              :data="filteredQuestionMaterials"
              style="width: 100%">
              <el-table-column prop="reportedAt" label="提交时间" width="180" />
              <el-table-column prop="studentId" label="学号" width="120" />
              <el-table-column prop="studentName" label="姓名" width="120" />
              <el-table-column prop="title" label="材料名称" />
              <el-table-column prop="evaluationType" label="申请类别" width="120">
                <template #default="{ row }">
                  {{ getEvaluationType(row.evaluationType) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="reviewComment" label="疑问描述" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button-group v-if="row.status === 'REPORTED'">
                    <el-button 
                      type="success" 
                      size="small" 
                      @click="handleReview(row, 'APPROVED')">
                      通过
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="handleReview(row, 'REJECTED')">
                      驳回
                    </el-button>
                  </el-button-group>
                  <el-button
                    type="info"
                    size="small"
                    @click="handleViewAttachments(row)">
                    查看附件
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </div>

          <!-- 审核对话框 -->
          <el-dialog
            v-model="reviewDialogVisible"
            :title="reviewType === 'APPROVED' ? '通过审核' : '驳回材料'"
            width="500px">
            <el-form ref="reviewFormRef" :model="reviewForm" label-width="100px">
              <el-form-item label="审核意见" prop="comment" :rules="[{ required: true, message: '请输入审核意见' }]">
                <el-input
                  v-model="reviewForm.comment"
                  type="textarea"
                  rows="4"
                  placeholder="请输入审核意见"
                />
              </el-form-item>
              
              <!-- 只在通过时显示加分选项 -->
              <template v-if="reviewType === 'APPROVED'">
                <el-form-item label="加分种类" prop="evaluationType" :rules="[{ required: true, message: '请选择加分种类' }]">
                  <select 
                    v-model="reviewForm.evaluationType" 
                    class="custom-select"
                    placeholder="请选择加分种类"
                  >
                    <option value="" disabled>请选择加分种类</option>
                    <option value="A">A类 - 思想品德</option>
                    <option value="C">C类 - 竞赛获奖</option>
                    <option value="D">D类 - 文体活动</option>
                  </select>
                </el-form-item>
                
                <el-form-item label="加分分数" prop="score" :rules="[{ required: true, message: '请输入加分分数' }]">
                  <input 
                    type="number" 
                    v-model="reviewForm.score"
                    class="custom-input"
                    min="0"
                    step="0.5"
                    placeholder="请输入加分分数"
                  />
                </el-form-item>
              </template>
            </el-form>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="reviewDialogVisible = false">取消</el-button>
                <el-button 
                  :type="reviewType === 'APPROVED' ? 'success' : 'danger'"
                  :loading="submitting"
                  @click="submitReview">
                  确认
                </el-button>
              </span>
            </template>
          </el-dialog>

          <!-- 材料预览对话框 -->
          <el-dialog
            v-model="previewDialogVisible"
            :title="'预览附件'"
            width="80%"
            destroy-on-close
            class="preview-dialog"
          >
            <div v-if="previewUrl" class="preview-container">
              <img v-if="isImage" :src="previewUrl" class="preview-image" />
              <iframe v-else-if="isPdf" :src="previewUrl" class="preview-pdf"></iframe>
              <div v-else class="unsupported-file">
                <p>不支持预览此类型的文件</p>
                <el-button type="primary" @click="downloadFile">下载文件</el-button>
              </div>
            </div>
            <div v-else class="loading-container">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
          </el-dialog>

          <!-- 附件预览对话框 -->
          <el-dialog
            v-model="attachmentDialogVisible"
            title="附件列表"
            width="600px"
          >
            <div class="attachment-section" v-if="selectedAttachments.length > 0">
              <div class="attachment-list">
                <div v-for="attachment in selectedAttachments" :key="attachment.id" class="attachment-item">
                  <div class="attachment-info">
                    <span class="attachment-name">{{ attachment.fileName }}</span>
                    <span class="attachment-size">{{ formatFileSize(attachment.fileSize) }}</span>
                  </div>
                  <div class="attachment-actions">
                    <el-button type="primary" size="small" @click="previewAttachment(attachment)">
                      预览
                    </el-button>
                    <el-button type="success" size="small" @click="downloadAttachment(attachment)">
                      下载
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="no-attachments">
              <p>无附件</p>
            </div>
          </el-dialog>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/utils/request'  // 使用配置好的axios实例
import TopBar from '@/components/TopBar.vue'
import Sidebar from '@/components/Sidebar.vue'
import { Loading } from '@element-plus/icons-vue'

// 加分类别选项
const categories = [
  { value: 'A', label: 'A类 - 思想品德' },
  { value: 'B', label: 'B类 - 学术科技' },
  { value: 'C', label: 'C类 - 文体活动' },
  { value: 'D', label: 'D类 - 社会工作' }
]

// 表格数据
const questionMaterials = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const filterStatus = ref('REPORTED')  // 默认显示待审核的

// 筛选和搜索
const searchKeyword = ref('')

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
const reviewType = ref('')
const submitting = ref(false)
const reviewForm = ref({
  comment: '',
  evaluationType: '',
  score: null
})

// 处理审核按钮点击
const handleReview = (row, type) => {
  currentMaterial.value = row
  reviewType.value = type
  reviewForm.value = {
    comment: '',
    evaluationType: '',
    score: null
  }
  reviewDialogVisible.value = true
}

// 提交审核
const submitReview = async () => {
  if (!reviewForm.value.comment.trim()) {
    ElMessage.warning('请输入审核意见')
    return
  }

  if (reviewType.value === 'APPROVED') {
    if (!reviewForm.value.evaluationType) {
      ElMessage.warning('请选择加分种类')
      return
    }
    if (!reviewForm.value.score) {
      ElMessage.warning('请输入加分分数')
      return
    }
  }

  try {
    submitting.value = true
    await axios.post('/instructor/review', {
      materialId: currentMaterial.value.id,
      status: reviewType.value,
      comment: reviewForm.value.comment,
      evaluationType: reviewForm.value.evaluationType,
      score: reviewForm.value.score
    })
    
    ElMessage.success(reviewType.value === 'APPROVED' ? '已通过审核' : '已驳回材料')
    reviewDialogVisible.value = false
    fetchQuestionMaterials() // 刷新列表
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败：' + error.message)
  } finally {
    submitting.value = false
  }
}

// API调用函数
const fetchQuestionMaterials = async () => {
  try {
    loading.value = true
    const response = await axios.get('/api/instructor/reported-materials', {
      params: {
        status: filterStatus.value,
        page: currentPage.value,
        size: pageSize.value
      },
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      baseURL: 'http://localhost:8080'
    })
    
    if (response.data.success) {
      questionMaterials.value = response.data.data
      total.value = response.data.total
    }
  } catch (error) {
    console.error('获取材料列表失败:', error)
    ElMessage.error('获取材料列表失败')
  } finally {
    loading.value = false
  }
}

// 监听分页和筛选变化
watch([currentPage, pageSize, filterStatus], () => {
  fetchQuestionMaterials()
})

// 页面加载时获取数据
onMounted(() => {
  console.log('组件已挂载，开始获取数据')
  fetchQuestionMaterials()
})

// 材料预览相关
const previewDialogVisible = ref(false)

const viewMaterial = (row) => {
  currentMaterial.value = row
  previewDialogVisible.value = true
}

// 添加状态显示处理函数
const getStatusType = (status) => {
  const types = {
    'REPORTED': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'REPORTED': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return texts[status] || status
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchQuestionMaterials()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchQuestionMaterials()
}

// 添加类型映射
const getEvaluationType = (type) => {
  const types = {
    'practice': '实践活动',
    'volunteer': '志愿服务',
    'competition': '竞赛获奖',
    'certificate': '技能证书'
  }
  return types[type] || type
}

// 添加附件预览和下载相关的状态
const attachmentDialogVisible = ref(false)
const selectedAttachments = ref([])

// 添加附件查看方法
const handleViewAttachments = (row) => {
  if (!row.attachments || row.attachments.length === 0) {
    ElMessage.warning('无附件')
    return
  }
  selectedAttachments.value = row.attachments
  attachmentDialogVisible.value = true
}

// 预览相关的响应式变量
const previewUrl = ref('')
const isImage = ref(false)
const isPdf = ref(false)

// 预览附件方法
const previewAttachment = async (attachment) => {
  try {
    // 重置预览状态
    previewUrl.value = ''
    isImage.value = false
    isPdf.value = false
    previewDialogVisible.value = true

    // 获取文件扩展名
    const fileExt = attachment.fileName.split('.').pop().toLowerCase()
    
    // 检查文件类型
    isImage.value = ['jpg', 'jpeg', 'png', 'gif'].includes(fileExt)
    isPdf.value = fileExt === 'pdf'

    // 如果是不支持预览的文件类型，直接触发下载
    if (!isImage.value && !isPdf.value) {
      ElMessage.info('不支持预览此类型的文件，将自动下载')
      downloadFile(attachment)
      return
    }

    // 获取预览URL
    const response = await axios.get(`/evaluation/preview/${attachment.id}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      responseType: 'blob'
    })

    // 创建Blob URL
    previewUrl.value = URL.createObjectURL(response.data)
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败，将尝试下载文件')
    downloadFile(attachment)
  }
}

// 下载文件方法
// 添加附件下载方法
const downloadAttachment = async (attachment) => {
  try {
    const response = await axios.get(`/evaluation/download/${attachment.id}`, {
      responseType: 'blob'
    })
    
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}


// 组件卸载前清理URL
onBeforeUnmount(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})

const formatFileSize = (size) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB'
  }
}
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.content-container {
  flex: 1;
  overflow-y: auto;
  background-color: #f0f2f5;
}

.instructor-review-materials {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 20px 0;
  color: #333;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.attachment-section {
  padding: 10px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.attachment-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.attachment-name {
  font-weight: 500;
}

.attachment-size {
  color: #666;
  font-size: 12px;
}

.attachment-actions {
  display: flex;
  gap: 8px;
}

.no-attachments {
  text-align: center;
  color: #999;
  padding: 20px;
}

.custom-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  font-size: 14px;
  color: #606266;
  transition: border-color 0.2s;
}

.custom-select:hover {
  border-color: #c0c4cc;
}

.custom-select:focus {
  outline: none;
  border-color: #409eff;
}

.custom-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  font-size: 14px;
  color: #606266;
  transition: border-color 0.2s;
}

.custom-input:hover {
  border-color: #c0c4cc;
}

.custom-input:focus {
  outline: none;
  border-color: #409eff;
}

.preview-dialog {
  .preview-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    
    .preview-image {
      max-width: 100%;
      max-height: 70vh;
      object-fit: contain;
    }
    
    .preview-pdf {
      width: 100%;
      height: 70vh;
      border: none;
    }
    
    .unsupported-file {
      text-align: center;
      padding: 20px;
    }
  }
  
  .loading-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    gap: 10px;
  }
}
</style> 