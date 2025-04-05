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
                <el-option label="待审核" value="QUESTIONED" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已驳回" value="REJECTED" />
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
            <div v-if="loading">加载中...</div>
            <div v-if="!loading && (!filteredQuestionMaterials || filteredQuestionMaterials.length === 0)">
              暂无数据
            </div>
            <el-table 
              v-else
              :data="filteredQuestionMaterials"
              style="width: 100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="55" />
              
              <el-table-column prop="updatedAt" label="提交时间" width="180" />
              <el-table-column prop="userId" label="学号" width="120" />
              <el-table-column prop="title" label="材料名称" width="150" />
              <el-table-column prop="evaluationType" label="申请类别" width="120" />
              <el-table-column prop="reviewComment" label="疑问描述" min-width="200">
                <template #default="{ row }">
                  <div class="description-cell">{{ row.reviewComment }}</div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-button 
                      type="primary" 
                      size="small" 
                      @click="openReviewDialog(row)"
                      :disabled="row.status !== 'QUESTIONED'"
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
                    <el-button 
                      type="warning" 
                      size="small" 
                      @click="reportMaterial(row)"
                      :disabled="row.status !== 'QUESTIONED'"
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
                <p><strong>学号：</strong>{{ currentMaterial?.userId }}</p>
                <p><strong>材料名称：</strong>{{ currentMaterial?.title }}</p>
                <p><strong>申请类别：</strong>{{ getEvaluationTypeText(currentMaterial?.evaluationType) }}</p>
                <p><strong>疑问描述：</strong>{{ currentMaterial?.reviewComment }}</p>
                <p><strong>提交时间：</strong>{{ formatDate(currentMaterial?.updatedAt) }}</p>
              </div>
              <el-form :model="reviewForm" label-width="100px">
                <el-form-item label="审核结果">
                  <el-radio-group v-model="reviewForm.status">
                    <el-radio label="APPROVED">通过</el-radio>
                    <el-radio label="REJECTED">驳回</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <!-- 添加通过时的额外字段 -->
                <template v-if="reviewForm.status === 'APPROVED'">
                  <el-form-item label="加分种类">
                    <select 
                      v-model="reviewForm.evaluationType" 
                      class="custom-select"
                      placeholder="请选择加分种类"
                    >
                      <option value="" disabled>请选择加分种类</option>
                      <option value="A">A类</option>
                      <option value="C">C类</option>
                      <option value="D">D类</option>
                    </select>
                  </el-form-item>
                  <el-form-item label="加分分数">
                    <el-input-number 
                      v-model="reviewForm.score" 
                      :min="0" 
                      :max="100" 
                      :precision="1"
                      :step="0.5"
                      placeholder="请输入加分分数"
                    />
                  </el-form-item>
                </template>

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
                <img v-if="currentMaterial?.materialType === 'image'" :src="currentMaterial.materialUrl" style="max-width: 100%; max-height: 100%;" />
                <iframe v-else-if="currentMaterial?.materialType === 'pdf'" :src="currentMaterial.materialUrl" style="width: 100%; height: 100%;" />
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

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"
import axios from '@/utils/axios'
import debounce from 'lodash/debounce'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

// 定义响应式变量
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')
const searchKeyword = ref('')
const pendingCount = ref(0)

// 修改数据结构定义
type QuestionMaterial = {
  id: number;
  userId: string;
  title: string;
  evaluationType: string;
  status: string;
  reviewComment: string;
  updatedAt: string;
  attachments: Array<{
    id: number;
    fileName: string;
    filePath: string;
  }>;
}

const questionMaterials = ref<QuestionMaterial[]>([])

// 获取表格数据
const fetchQuestionMaterials = async () => {
  try {
    console.log('开始获取疑问材料...')
    loading.value = true
    
    const token = localStorage.getItem('token')
    console.log('当前 token:', token)  // 添加 token 调试日志
    
    const response = await axios.get('/question-materials', {  // 移除 /api 前缀
      params: {
        status: filterStatus.value,
        page: currentPage.value,
        size: pageSize.value,
        keyword: searchKeyword.value,
        type: 'questioned'
      },
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    })
    
    console.log('API响应:', response)
    
    if (response.status === 200) {
      console.log('获取数据成功，处理数据...')
      console.log('原始数据结构:', response.data.data)
      questionMaterials.value = response.data.data
      total.value = response.data.total
      
      // 修改状态判断条件，打印调试信息
      console.log('材料状态值:', questionMaterials.value.map(item => item.status))
      pendingCount.value = questionMaterials.value.filter(
        item => item.status === 'QUESTIONED'  // 改为 'QUESTIONED'
      ).length
      console.log('待审核数量:', pendingCount.value)
    }
  } catch (error) {
    console.error('获取疑问材料失败:', error)
    console.error('请求配置:', error.config)  // 添加请求配置调试日志
    ElMessage.error('获取疑问材料失败')
  } finally {
    loading.value = false
  }
}

// 筛选数据的计算属性
const filteredQuestionMaterials = computed(() => {
  let result = [...questionMaterials.value]

  if (filterStatus.value) {
    result = result.filter(item => item.status === filterStatus.value)
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(item => 
      item.userId.toLowerCase().includes(keyword) ||
      item.title.toLowerCase().includes(keyword)
    )
  }

  return result
})

// 审核相关
const reviewDialogVisible = ref(false)
const currentMaterial = ref(null)
const reviewForm = ref({
  status: '',
  comment: '',
  evaluationType: '',
  score: 0
})
const submitting = ref(false)

const openReviewDialog = (row) => {
  currentMaterial.value = row
  reviewForm.value = {
    status: '',
    comment: '',
    evaluationType: '',
    score: 0
  }
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewForm.value.status) {
    ElMessage.warning('请选择审核结果');
    return;
  }

  if (reviewForm.value.status === 'APPROVED') {
    if (!reviewForm.value.evaluationType) {
      ElMessage.warning('请选择加分种类');
      return;
    }
    if (!reviewForm.value.score) {
      ElMessage.warning('请输入加分分数');
      return;
    }
  }

  if (!reviewForm.value.comment.trim()) {
    ElMessage.warning('请输入审核意见');
    return;
  }

  try {
    submitting.value = true;
    await axios.post('/question-materials/review', {
      materialId: currentMaterial.value.id,
      status: reviewForm.value.status,
      comment: reviewForm.value.comment,
      evaluationType: reviewForm.value.evaluationType,
      score: reviewForm.value.score
    });
    
    ElMessage.success('审核提交成功');
    reviewDialogVisible.value = false;
    await fetchQuestionMaterials();
  } catch (error) {
    ElMessage.error('审核提交失败：' + (error.response?.data?.message || '未知错误'));
  } finally {
    submitting.value = false;
  }
}

// 材料预览相关
const previewDialogVisible = ref(false)

const viewMaterial = async (row) => {
  if (!row.attachments || row.attachments.length === 0) {
    ElMessage.warning('没有可查看的材料')
    return
  }

  // 多个附件时显示选择列表
  if (row.attachments.length > 1) {
    ElMessageBox.alert(
      `<div class="attachment-list">
        ${row.attachments.map((att, index) => `
          <div class="attachment-item" style="margin: 10px 0;">
            <span>${att.fileName}</span>
            <div>
              <el-button type="primary" size="small" @click="handlePreview(${index}, row.attachments)">
                预览
              </el-button>
              <el-button type="info" size="small" @click="handleDownload(${index}, row.attachments)">
                下载
              </el-button>
            </div>
          </div>
        `).join('')}
      </div>`,
      '选择要查看的附件',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
    )
    return
  }

  // 单个附件直接处理
  handlePreview(0, row.attachments)
}

const handlePreview = async (index, attachments) => {
  const attachment = attachments[index]
  const fileType = attachment.fileName.split('.').pop()?.toLowerCase()
  
  if (['jpg', 'jpeg', 'png', 'gif'].includes(fileType)) {
    try {
      const response = await axios.get(`/evaluation/preview/${attachment.id}`, {
        responseType: 'blob'
      })
      
      const url = URL.createObjectURL(response.data)
      currentMaterial.value = {
        materialType: 'image',
        materialUrl: url
      }
      previewDialogVisible.value = true
    } catch (error) {
      ElMessage.error('预览失败')
    }
  } else {
    // 对于不支持预览的文件类型，直接触发下载
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
      ElMessage.success('文件已开始下载')
    } catch (error) {
      ElMessage.error('下载失败')
    }
  }
}

const handleDownload = async (index, attachments) => {
  const attachment = attachments[index]
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

// 状态数据
const selectedMaterials = ref([])
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

// 在组件挂载时获取数据
onMounted(() => {
  fetchQuestionMaterials()
})

// 在提交上报后刷新列表
const submitReport = async () => {
  try {
    submitting.value = true
    const response = await request.post('/api/question-materials/report', {
      materialIds: [selectedMaterials.value[0].id],  // 修改这里，确保是数组格式
      note: reportForm.value.note || "请审核这些存在疑问的材料"  // 添加默认值
    }, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      baseURL: 'http://localhost:8080'
    })
    
    if (response.data.success) {
      ElMessage.success(response.data.message || '材料已成功上报')
      reportDialogVisible.value = false
      await fetchQuestionMaterials()
    }
  } catch (error) {
    ElMessage.error('上报失败：' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

// 添加请求拦截器
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 添加响应拦截器
axios.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response) {
      console.error('API错误响应:', error.response)
      if (error.response.status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

// 添加评估类型文本转换函数
const getEvaluationTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'academic': '学术成果',
    'practice': '社会实践',
    'volunteer': '志愿服务',
    'work': '学生工作',
    'other': '其他'
  };
  return typeMap[type] || type;
};

// 添加日期格式化函数
const formatDate = (dateString: string) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
};
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

.instructor-info {
  margin: 15px 0;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.instructor-info h4 {
  margin: 0 0 10px 0;
  color: #606266;
}

.instructor-info ul {
  margin: 0;
  padding-left: 20px;
}

.instructor-info li {
  color: #409eff;
  margin: 5px 0;
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

.custom-select option {
  padding: 8px;
}
</style> 
