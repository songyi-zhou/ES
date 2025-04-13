<template>
  <div class="review-materials-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <div class="main-content">
        <div class="page-header">
          <h2>综合测评材料审核</h2>
          <div class="header-actions">
            <div class="search-box">
              <input
                v-model="searchKeyword"
                placeholder="搜索材料名称"
                class="custom-input"
              />
              <i class="search-icon">🔍</i>
            </div>
            
            <div class="custom-dropdown">
              <div class="dropdown-header" @click="toggleStatusDropdown">
                <span>{{ filterStatus ? getStatusText(filterStatus) : '状态筛选' }}</span>
                <i class="dropdown-icon">▼</i>
              </div>
              <div class="dropdown-menu" v-if="showStatusDropdown">
                <div class="dropdown-item" @click="selectStatus('')">全部</div>
                <div class="dropdown-item" @click="selectStatus('PENDING_ALL')">待处理</div>
                <div class="dropdown-item" @click="selectStatus('PENDING')">待审核</div>
                <div class="dropdown-item" @click="selectStatus('APPROVED')">已通过</div>
                <div class="dropdown-item" @click="selectStatus('REJECTED')">已驳回</div>
                <div class="dropdown-item" @click="selectStatus('UNCORRECT')">不正确加分</div>
              </div>
            </div>
            
            <button class="custom-button primary" @click="fetchMaterials">刷新</button>
          </div>
        </div>
        
        <div class="table-card">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>
          
          <div v-else-if="filteredMaterials.length === 0" class="empty-data">
            <div class="empty-icon">📄</div>
            <p>暂无审核材料</p>
          </div>
          
          <el-table 
            v-else 
            :data="filteredMaterials" 
            border 
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266', textAlign: 'center' }"
            :cell-style="{ textAlign: 'center', cursor: 'default' }"
            :row-style="{ cursor: 'default' }"
            :resizable="false"
          >
            <el-table-column prop="createdAt" label="提交时间" min-width="100" align="center">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column prop="title" label="材料名称" min-width="100" show-overflow-tooltip align="center" />
            <el-table-column prop="evaluationType" label="材料类型" min-width="100" align="center">
              <template #default="scope">
                {{ getEvaluationTypeText(scope.row.evaluationType) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="材料状态" min-width="100" align="center">
              <template #default="scope">
                <span class="status-tag" :class="getStatusClass(scope.row.status)">
                  {{ getStatusText(scope.row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <div class="actions">
                  <template v-if="row.status === 'UNCORRECT'">
                    <button class="correct-btn" @click="openCorrectDialog(row)">改正</button>
                    <el-button 
                      type="info" 
                      size="small" 
                      @click.stop="handleViewDetails(row)"
                    >
                      详情
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button 
                      type="success" 
                      size="small" 
                      @click.stop="handleReview(row)"
                      :disabled="row.status !== 'PENDING'"
                    >
                      通过
                    </el-button>
                    <el-button 
                      type="warning" 
                      size="small" 
                      @click.stop="openQuestionDialog(row)"
                      :disabled="row.status !== 'PENDING'"
                    > 
                      提出疑问
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click.stop="openRejectDialog(row)"
                      :disabled="row.status !== 'PENDING'"
                    >
                      驳回
                    </el-button>
                    <el-button 
                      type="info" 
                      size="small" 
                      @click.stop="handleViewDetails(row)"
                    >
                      详情
                    </el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
    
    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailsDialogVisible"
      title="材料详情"
      width="60%"
      destroy-on-close
    >
      <div v-if="selectedMaterial" class="material-details">
        <div class="detail-item">
          <span class="label">提交时间:</span>
          <span>{{ formatDate(selectedMaterial.createdAt) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">学号:</span>
          <span>{{ selectedMaterial.userId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">材料名称:</span>
          <span>{{ selectedMaterial.title }}</span>
        </div>
        <div class="detail-item">
          <span class="label">材料类型:</span>
          <span>{{ getEvaluationTypeText(selectedMaterial.evaluationType) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">材料状态:</span>
          <span class="status-tag" :class="getStatusClass(selectedMaterial.status)">
            {{ getStatusText(selectedMaterial.status) }}
          </span>
        </div>
        <div class="detail-item">
          <span class="label">材料描述:</span>
          <p class="description">{{ selectedMaterial.description || '无' }}</p>
        </div>
        
        <div class="attachment-section" v-if="selectedMaterial.attachments && selectedMaterial.attachments.length > 0">
          <h3>附件列表</h3>
          <div class="attachment-list">
            <div v-for="attachment in selectedMaterial.attachments" :key="attachment.id" class="attachment-item">
              <div class="attachment-info">
                <span class="attachment-name">{{ attachment.fileName }}</span>
                <span class="attachment-size">{{ formatFileSize(attachment.fileSize) }}</span>
              </div>
              <div class="attachment-actions">
                <button class="custom-button info small" @click="previewAttachment(attachment)">
                  预览
                </button>
                <button class="custom-button primary small" @click="downloadAttachment(attachment)">
                  下载
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="no-attachments">
          <p>无附件</p>
        </div>
      </div>
    </el-dialog>
    
    <!-- 驳回对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回材料"
      width="500px"
    >
      <div class="reject-form">
        <el-form :model="rejectForm" label-width="100px">
          <el-form-item label="驳回原因" required>
            <el-input
              v-model="rejectForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入驳回原因"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="submitReject" :loading="submitting">
            确认驳回
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加提出疑问的对话框 -->
    <el-dialog
      v-model="questionDialogVisible"
      title="提出疑问"
      width="500px"
    >
      <div class="question-form">
        <el-form :model="questionForm" label-width="100px">
          <el-form-item label="疑问描述" required>
            <el-input
              v-model="questionForm.description"
              type="textarea"
              :rows="4"
              placeholder="请详细描述您的疑问"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="questionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitQuestion" :loading="submitting">
            提交
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加通过审核的对话框 -->
    <el-dialog
      v-model="approveDialogVisible"
      title="通过审核"
      width="500px"
    >
      <div class="approve-form">
        <div class="form-item">
          <label class="form-label">加分类型</label>
          <div class="custom-dropdown">
            <div class="dropdown-header" @click="toggleTypeDropdown">
              <span>{{ getEvaluationTypeText(approveForm.evaluationType) }}</span>
              <i class="dropdown-icon">▼</i>
            </div>
            <div class="dropdown-menu" v-if="showTypeDropdown">
              <div class="dropdown-item" @click="selectEvaluationType('A')">德育测评加分(A)</div>
              <div class="dropdown-item" @click="selectEvaluationType('C')">科研竞赛加分(C)</div>
              <div class="dropdown-item" @click="selectEvaluationType('D')">文体活动加分(D)</div>
            </div>
          </div>
        </div>
        
        <div class="form-item">
          <label class="form-label">加分数额</label>
          <div class="score-input-container">
            <button class="score-btn" @click="decreaseScore">-</button>
            <input 
              type="number" 
              v-model="approveForm.score" 
              min="0.5" 
              max="20" 
              step="0.5" 
              class="score-input"
            />
            <button class="score-btn" @click="increaseScore">+</button>
          </div>
        </div>
        
        <div class="form-item">
          <label class="form-label">备注</label>
          <textarea
            v-model="approveForm.comment"
            class="custom-textarea"
            rows="3"
            placeholder="请输入备注信息"
          ></textarea>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="custom-button" @click="approveDialogVisible = false">取消</button>
          <button class="custom-button primary" @click="submitApprove" :disabled="submitting">
            {{ submitting ? '提交中...' : '确认通过' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加改正对话框 -->
    <el-dialog
      v-model="showCorrectDialog"
      title="材料改正"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="correct-form">
        <div class="form-item">
          <label class="form-label">加分类型</label>
          <div class="custom-dropdown">
            <div class="dropdown-header" @click="toggleCorrectTypeDropdown">
              <span>{{ getEvaluationTypeText(correctForm.evaluationType) }}</span>
              <i class="dropdown-icon">▼</i>
            </div>
            <div class="dropdown-menu" v-if="showCorrectTypeDropdown">
              <div class="dropdown-item" @click="selectCorrectType('A')">德育测评加分(A)</div>
              <div class="dropdown-item" @click="selectCorrectType('C')">科研竞赛加分(C)</div>
              <div class="dropdown-item" @click="selectCorrectType('D')">文体活动加分(D)</div>
            </div>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">加分数额</label>
          <div class="score-input-container">
            <button class="score-btn" @click="() => correctForm.score = Math.max(0.5, correctForm.score - 0.5)">-</button>
            <input 
              type="number" 
              v-model="correctForm.score" 
              min="0.5" 
              max="10" 
              step="0.5" 
              class="score-input"
            />
            <button class="score-btn" @click="() => correctForm.score = Math.min(10, correctForm.score + 0.5)">+</button>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">改正意见</label>
          <textarea
            v-model="correctForm.reviewComment"
            class="custom-textarea"
            rows="4"
            placeholder="请输入改正意见"
          ></textarea>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="custom-button" @click="showCorrectDialog = false">取消</button>
          <button class="custom-button primary" @click="submitCorrection" :disabled="submitting">
            {{ submitting ? '提交中...' : '确认' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getReviewMaterials } from '@/api/evaluation';
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import axios from '@/utils/axios';
import request from '@/utils/request';

interface EvaluationAttachment {
  id: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  uploadTime: string;
}

interface EvaluationMaterial {
  id: number;
  userId: number;
  evaluationType: string;
  title: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  classId: string;
  attachments?: EvaluationAttachment[];
}

const materials = ref<EvaluationMaterial[]>([]);
const loading = ref(true);
const searchKeyword = ref('');
const filterStatus = ref('PENDING_ALL');
const showStatusDropdown = ref(false);
const detailsDialogVisible = ref(false);
const rejectDialogVisible = ref(false);
const selectedMaterial = ref<EvaluationMaterial | null>(null);
const rejectForm = ref({
  reason: ''
});
const submitting = ref(false);
const questionDialogVisible = ref(false);
const questionForm = ref({
  description: '',
  materialId: null
});
const approveDialogVisible = ref(false);
const approveForm = ref({
  evaluationType: 'A',
  score: 1,
  comment: '',
  materialId: null
});
const showTypeDropdown = ref(false);
const showCorrectDialog = ref(false);
const correctForm = ref({
  evaluationType: 'A',
  score: 0,
  reviewComment: ''
});
const showCorrectTypeDropdown = ref(false);

const evaluationTypes = [
  { value: 'academic', label: '学术成果' },
  { value: 'practice', label: '社会实践' },
  { value: 'volunteer', label: '志愿服务' },
  { value: 'work', label: '学生工作' },
  { value: 'other', label: '其他' }
];

const toggleStatusDropdown = () => {
  showStatusDropdown.value = !showStatusDropdown.value;
};

const selectStatus = (status: string) => {
  filterStatus.value = status;
  showStatusDropdown.value = false;
};

document.addEventListener('click', (e) => {
  const target = e.target as HTMLElement;
  if (!target.closest('.custom-dropdown')) {
    showStatusDropdown.value = false;
  }
});

const filteredMaterials = computed(() => {
  return materials.value.filter(item => {
    const matchesKeyword = searchKeyword.value 
      ? item.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
      : true;
    
    let matchesStatus = true;
    if (filterStatus.value) {
      if (filterStatus.value === 'PENDING_ALL') {
        // 待处理：包括PENDING和UNCORRECT
        matchesStatus = item.status === 'PENDING' || item.status === 'UNCORRECT';
      } else {
        matchesStatus = item.status === filterStatus.value;
      }
    }
    
    return matchesKeyword && matchesStatus;
  });
});

const getStatusClass = (status: string) => {
  const classMap: Record<string, string> = {
    'PENDING': 'status-warning',
    'APPROVED': 'status-success',
    'REJECTED': 'status-danger'
  };
  return classMap[status] || 'status-default';
};

const formatDate = (dateString: string) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    timeZone: 'Asia/Shanghai'
  });
};

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  };
  return typeMap[status] || 'info';
};

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    'PENDING_ALL': '待处理',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'QUESTIONED': '已提出疑问',
    'REPORTED': '已上报至导员',
    'DEDUCTED': '已记录',
    'PUNISHED': '已处分',
    'UNCORRECT': '不正确加分'
  };
  return textMap[status] || status;
};

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

const formatFileSize = (size: number) => {
  if (size < 1024) {
    return size + ' B';
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB';
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB';
  }
};

const handleViewDetails = (record: EvaluationMaterial) => {
  selectedMaterial.value = record;
  detailsDialogVisible.value = true;
};

const handleRowClick = (row: EvaluationMaterial) => {
  handleViewDetails(row);
};

const openRejectDialog = (record: EvaluationMaterial) => {
  selectedMaterial.value = record;
  rejectForm.value.reason = '';
  rejectDialogVisible.value = true;
};

const handleReview = (record: EvaluationMaterial) => {
  approveForm.value = {
    evaluationType: 'A',
    score: 1,
    comment: '',
    materialId: record.id
  };
  approveDialogVisible.value = true;
};

const submitReject = async () => {
  if (!rejectForm.value.reason.trim()) {
    ElMessage.warning('请输入驳回原因');
    return;
  }

  try {
    submitting.value = true;
    await axios.post('/evaluation/review-material', {
      materialId: selectedMaterial.value.id,
      status: 'REJECTED',
      comment: rejectForm.value.reason
    });
    ElMessage.success('驳回成功');
    rejectDialogVisible.value = false;
    await fetchMaterials();
  } catch (error: any) {
    if (error.response?.status === 403) {
      ElMessage.error('没有权限执行此操作');
    } else {
      ElMessage.error('操作失败：' + (error.response?.data?.message || '未知错误'));
    }
  } finally {
    submitting.value = false;
  }
};

const previewAttachment = async (attachment) => {
  const fileType = attachment.fileType.toLowerCase();
  
  // 判断是否可预览
  if (['jpg', 'jpeg', 'png', 'pdf'].includes(fileType)) {
    try {
      const response = await axios.get(`/evaluation/preview/${attachment.id}`, {
        responseType: 'blob'
      });
      
      const url = URL.createObjectURL(response.data);
      window.open(url, '_blank');
    } catch (error) {
      ElMessage.error('预览失败');
    }
  } else {
    ElMessage.info('该文件类型不支持预览，请下载后查看');
  }
};

const downloadAttachment = async (attachment) => {
  try {
    const response = await axios.get(`/evaluation/download/${attachment.id}`, {
      responseType: 'blob'
    });
    
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = attachment.fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (error) {
    ElMessage.error('下载失败');
  }
};

const openQuestionDialog = (row) => {
  questionForm.value = {
    description: '',
    materialId: row.id
  }
  questionDialogVisible.value = true
}

const submitQuestion = async () => {
  if (!questionForm.value.description.trim()) {
    ElMessage.warning('请输入疑问描述')
    return
  }

  try {
    submitting.value = true
    const response = await axios.post('/evaluation/raise-question', questionForm.value)
    
    if (response.status === 200) {
      ElMessage.success('疑问提交成功')
      questionDialogVisible.value = false
      await fetchMaterials()
    }
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const toggleTypeDropdown = () => {
  showTypeDropdown.value = !showTypeDropdown.value;
};

const selectEvaluationType = (type) => {
  approveForm.value.evaluationType = type;
  showTypeDropdown.value = false;
};

const increaseScore = () => {
  if (approveForm.value.score < 20) {
    approveForm.value.score = Math.round((approveForm.value.score + 0.5) * 10) / 10;
  }
};

const decreaseScore = () => {
  if (approveForm.value.score > 0.5) {
    approveForm.value.score = Math.round((approveForm.value.score - 0.5) * 10) / 10;
  }
};

const submitApprove = async () => {
  if (!approveForm.value.evaluationType) {
    ElMessage.warning('请选择加分类型');
    return;
  }
  
  if (!approveForm.value.score) {
    ElMessage.warning('请输入加分数额');
    return;
  }
  
  try {
    submitting.value = true;
    await axios.post('/evaluation/review-material', {
      materialId: approveForm.value.materialId,
      status: 'APPROVED',
      evaluationType: approveForm.value.evaluationType,
      score: approveForm.value.score,
      comment: approveForm.value.comment || '通过审核'
    });
    ElMessage.success('审核通过成功');
    approveDialogVisible.value = false;
    await fetchMaterials();
  } catch (error) {
    if (error.response?.status === 403) {
      ElMessage.error('没有权限执行此操作');
    } else {
      ElMessage.error('操作失败：' + (error.response?.data?.message || '未知错误'));
    }
  } finally {
    submitting.value = false;
  }
};

const fetchMaterials = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('token');
    const response = await request.get('/api/evaluation/review-materials', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      baseURL: 'http://localhost:8080'
    });
    // 直接使用返回的数据，因为后端已经返回了数组
    materials.value = response.data;
  } catch (error) {
    console.error('获取材料列表失败:', error);
    ElMessage.error('获取材料列表失败');
  } finally {
    loading.value = false;
  }
};

const openCorrectDialog = (row) => {
  selectedMaterial.value = row;
  correctForm.value = {
    evaluationType: 'A',
    score: 0.5,
    reviewComment: ''
  };
  showCorrectDialog.value = true;
};

const submitCorrection = async () => {
  try {
    if (!correctForm.value.evaluationType) {
      ElMessage.warning('请选择加分类型');
      return;
    }
    if (correctForm.value.score <= 0) {
      ElMessage.warning('加分数额必须大于0');
      return;
    }
    if (!correctForm.value.reviewComment.trim()) {
      ElMessage.warning('请输入改正意见');
      return;
    }

    const response = await request.post('/evaluation/correct', {
      materialId: selectedMaterial.value.id,
      evaluationType: correctForm.value.evaluationType,
      score: correctForm.value.score,
      reviewComment: correctForm.value.reviewComment
    });

    if (response.data.success) {
      ElMessage.success('改正成功');
      showCorrectDialog.value = false;
      fetchMaterials();
    }
  } catch (error) {
    console.error('改正失败:', error);
    ElMessage.error(error.response?.data?.message || '改正失败');
  }
};

const selectCorrectType = (type) => {
  correctForm.value.evaluationType = type;
  showCorrectTypeDropdown.value = false;
};

const toggleCorrectTypeDropdown = () => {
  showCorrectTypeDropdown.value = !showCorrectTypeDropdown.value;
};

onMounted(() => {
  selectStatus('PENDING_ALL');
  fetchMaterials();
});
</script>

<style scoped>
.review-materials-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content {
  flex: 1;
  display: flex;
  background-color: #f0f2f5;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

h2 {
  font-weight: bold;
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-box {
  position: relative;
  width: 220px;
}

.custom-input {
  width: 100%;
  height: 36px;
  padding: 0 30px 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
  outline: none;
}

.custom-input:focus {
  border-color: #409eff;
}

.search-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
  font-size: 14px;
}

.custom-dropdown {
  position: relative;
  width: 120px;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
  transition: border-color 0.2s;
}

.dropdown-header:hover {
  border-color: #c0c4cc;
}

.dropdown-icon {
  font-size: 12px;
  color: #909399;
  transition: transform 0.3s;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  margin-top: 5px;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;
  max-height: 200px;
  overflow-y: auto;
}

.dropdown-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.custom-button {
  height: 36px;
  padding: 0 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fff;
  color: #606266;
}

.custom-button:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.custom-button.primary {
  background-color: #409eff;
  border-color: #409eff;
  color: #fff;
}

.custom-button.primary:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}

.custom-button.success {
  background-color: #67c23a;
  border-color: #67c23a;
  color: #fff;
}

.custom-button.success:hover {
  background-color: #85ce61;
  border-color: #85ce61;
}

.custom-button.danger {
  background-color: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
}

.custom-button.danger:hover {
  background-color: #f78989;
  border-color: #f78989;
}

.custom-button.info {
  background-color: #909399;
  border-color: #909399;
  color: #fff;
}

.custom-button.info:hover {
  background-color: #a6a9ad;
  border-color: #a6a9ad;
}

.custom-button.small {
  height: 32px;
  padding: 0 10px;
  font-size: 12px;
}

.custom-button:disabled {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
}

.table-card {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 24px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.status-success {
  background-color: #f0f9eb;
  color: #67c23a;
}

.status-danger {
  background-color: #fef0f0;
  color: #f56c6c;
}

.status-default {
  background-color: #f4f4f5;
  color: #909399;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: nowrap;
  min-width: 330px;
}

.action-buttons .el-button {
  flex: 0 0 auto;
  margin: 0;
}

.material-details {
  padding: 10px;
}

.detail-item {
  margin-bottom: 16px;
}

.detail-item .label {
  font-weight: bold;
  margin-right: 8px;
  color: #606266;
}

.description {
  white-space: pre-wrap;
  line-height: 1.5;
  margin-top: 8px;
  padding: 10px;
  background-color: #f8f8f8;
  border-radius: 4px;
}

.attachment-section {
  margin-top: 24px;
}

.attachment-section h3 {
  margin-bottom: 16px;
  font-weight: bold;
  color: #303133;
}

.attachment-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.attachment-item:last-child {
  border-bottom: none;
}

.attachment-info {
  display: flex;
  flex-direction: column;
}

.attachment-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.attachment-size {
  font-size: 12px;
  color: #909399;
}

.attachment-actions {
  display: flex;
  gap: 8px;
}

.no-attachments {
  text-align: center;
  color: #909399;
  padding: 20px;
  background-color: #f8f8f8;
  border-radius: 4px;
}

.custom-form {
  margin-bottom: 20px;
}

.form-item {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #606266;
}

.custom-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  transition: border-color 0.2s;
  resize: vertical;
  outline: none;
}

.custom-textarea:focus {
  border-color: #409eff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.question-form {
  padding: 20px;
}

/* 添加审核表单样式 */
.approve-form {
  padding: 20px;
}

.custom-dropdown {
  position: relative;
  width: 100%;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  background-color: #fff;
}

.dropdown-icon {
  font-size: 12px;
  color: #909399;
  transition: transform 0.3s;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 10;
  margin-top: 5px;
}

.dropdown-item {
  padding: 8px 12px;
  cursor: pointer;
}

.dropdown-item:hover {
  background-color: #f5f7fa;
}

.score-input-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.score-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  font-size: 18px;
  user-select: none;
}

.score-btn:first-child {
  border-radius: 4px 0 0 4px;
}

.score-btn:last-child {
  border-radius: 0 4px 4px 0;
}

.score-input {
  flex: 1;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-left: none;
  border-right: none;
  text-align: center;
  outline: none;
  padding: 0 5px;
}

.custom-button {
  padding: 8px 20px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.custom-button.primary {
  background-color: #409eff;
  border-color: #409eff;
  color: #fff;
}

.custom-button.primary:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

.custom-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.correct-btn {
  padding: 4px 10px;
  height: 28px;
  font-size: 12px;
  border-radius: 3px;
  background-color: #67c23a;
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 52px;
}

.correct-btn:hover {
  background-color: #85ce61;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.view-btn {
  padding: 4px 10px;
  height: 28px;
  font-size: 12px;
  border-radius: 3px;
  background-color: #909399;
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 52px;
  margin-left: 4px;
}

.view-btn:hover {
  background-color: #a6a9ad;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.correct-form {
  padding: 20px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input-number) {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>