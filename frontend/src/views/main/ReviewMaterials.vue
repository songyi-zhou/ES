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
                <div class="dropdown-item active" @click="selectStatus('PENDING')">待审核</div>
                <div class="dropdown-item" @click="selectStatus('APPROVED')">已通过</div>
                <div class="dropdown-item" @click="selectStatus('REJECTED')">已驳回</div>
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
            <el-table-column prop="createdAt" label="提交时间" min-width="200" align="center">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="userId" label="学号" min-width="100" align="center" />
            <el-table-column prop="title" label="材料名称" min-width="200" show-overflow-tooltip align="center" />
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
            <el-table-column label="操作" min-width="330" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
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
const filterStatus = ref('PENDING');
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
    
    const matchesStatus = filterStatus.value 
      ? item.status === filterStatus.value
      : true;
    
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
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'QUESTIONED': '已提出疑问',
    'Reported': '已上报至导员'
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

const handleReview = async (record: EvaluationMaterial) => {
  try {
    submitting.value = true;
    await axios.post('/evaluation/review-material', {
      materialId: record.id,
      status: 'APPROVED',
      comment: '通过审核'
    });
    ElMessage.success('审核通过成功');
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
    if (response.data.success) {
      materials.value = response.data.data;
    }
  } catch (error) {
    console.error('获取材料列表失败:', error);
    ElMessage.error('获取材料列表失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  selectStatus('PENDING');
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
</style>