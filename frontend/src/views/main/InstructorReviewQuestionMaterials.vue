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
            <el-table :data="filteredQuestionMaterials" style="width: 100%">
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
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-button 
                      type="success" 
                      size="small" 
                      @click="handleApprove(row)"
                      :disabled="row.status !== 'pending'"
                    >
                      同意加分
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="handleReject(row)"
                      :disabled="row.status !== 'pending'"
                    >
                      驳回加分
                    </el-button>
                    <el-button 
                      type="info" 
                      size="small" 
                      @click="viewMaterial(row)"
                    >
                      查看材料
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
            :title="reviewType === 'approve' ? '同意加分' : '驳回加分'"
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
                <el-form-item v-if="reviewType === 'approve'" label="加分类别">
                  <el-select v-model="reviewForm.category" placeholder="请选择加分类别">
                    <el-option
                      v-for="item in categories"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="reviewType === 'approve'" label="加分分值">
                  <el-input-number 
                    v-model="reviewForm.points" 
                    :min="0" 
                    :max="100" 
                    :step="0.5"
                  />
                </el-form-item>
                <el-form-item label="审核意见">
                  <el-input
                    v-model="reviewForm.comment"
                    type="textarea"
                    :rows="4"
                    :placeholder="reviewType === 'approve' ? '请输入同意加分的理由' : '请输入驳回的理由'"
                  />
                </el-form-item>
              </el-form>
            </div>
            <template #footer>
              <div class="dialog-footer">
                <el-button @click="reviewDialogVisible = false">取消</el-button>
                <el-button :type="reviewType === 'approve' ? 'success' : 'danger'" @click="submitReview" :loading="submitting">
                  {{ reviewType === 'approve' ? '确认同意' : '确认驳回' }}
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from '@/components/TopBar.vue'
import Sidebar from '@/components/Sidebar.vue'

// 加分类别选项
const categories = [
  { value: 'A', label: 'A类 - 思想品德' },
  { value: 'B', label: 'B类 - 学术科技' },
  { value: 'C', label: 'C类 - 文体活动' },
  { value: 'D', label: 'D类 - 社会工作' }
]

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
const reviewType = ref('approve') // 'approve' 或 'reject'
const reviewForm = ref({
  category: '',
  points: 0,
  comment: ''
})
const submitting = ref(false)

const handleApprove = (row) => {
  currentMaterial.value = row
  reviewType.value = 'approve'
  reviewForm.value = {
    category: row.requestedCategory,
    points: row.requestedPoints,
    comment: ''
  }
  reviewDialogVisible.value = true
}

const handleReject = (row) => {
  currentMaterial.value = row
  reviewType.value = 'reject'
  reviewForm.value = {
    category: '',
    points: 0,
    comment: ''
  }
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (reviewType.value === 'approve' && (!reviewForm.value.category || !reviewForm.value.points)) {
    ElMessage.warning('请选择加分类别和分值')
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
    
    ElMessage.success(reviewType.value === 'approve' ? '已同意加分' : '已驳回加分')
    reviewDialogVisible.value = false
    
    // 更新列表数据
    const index = questionMaterials.value.findIndex(item => item.id === currentMaterial.value.id)
    if (index !== -1) {
      questionMaterials.value[index].status = reviewType.value === 'approve' ? 'approved' : 'rejected'
    }
  } catch (error) {
    ElMessage.error('操作失败')
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
</style> 