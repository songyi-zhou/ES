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
            </div>
            <div class="filter-section">
              <div class="custom-dropdown">
                <div class="dropdown-header" @click="toggleStatusDropdown">
                  <span>{{ getStatusText(filterStatus) }}</span>
                  <i class="dropdown-icon">▼</i>
                </div>
                <div class="dropdown-menu" v-if="showStatusDropdown">
                  <div class="dropdown-item" @click="selectStatus('')">全部</div>
                  <div class="dropdown-item" @click="selectStatus('QUESTIONED')">待审核</div>
                  <div class="dropdown-item" @click="selectStatus('CORRECTED')">已改正</div>
                </div>
              </div>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索学生姓名/学号"
                class="search-input"
              />
              <div class="action-buttons">
              <button 
                class="custom-button success re-review-btn" 
                @click="handleReReview"
                :disabled="submitting"
              >
                <i v-if="!submitting" class="check-icon">✓</i>
                <i v-else class="loading-icon">⟳</i>
                {{ submitting ? '处理中...' : '修正加分错误后制表' }}
              </button>
            </div>
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
            >
              <el-table-column prop="updatedAt" label="提交时间" width="180" />
              <el-table-column prop="studentId" label="学号" width="120" />
              <el-table-column prop="studentName" label="姓名" width="120" />
              <el-table-column prop="title" label="材料名称" width="150" />
              <el-table-column prop="evaluationType" label="申请类别" width="120" />
              <el-table-column prop="status" label="材料状态" min-width="100" align="center">
                <template #default="scope">
                  <span class="status-tag" :class="getStatusClass(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="reviewComment" label="疑问描述" min-width="200">
                <template #default="{ row }">
                  <div class="description-cell">{{ row.reviewComment }}</div>
                </template>
              </el-table-column>
              <el-table-column v-if="filterStatus === 'CORRECTED'" prop="score" label="分数" width="100" align="center">
                <template #default="scope">
                  <span>{{ scope.row.score }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="320" fixed="right">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <template v-if="row.status === 'CORRECTED'">
                    <el-button 
                        type="success" 
                      size="small" 
                        @click.stop="handleReview(row)"
                      >
                        通过
                      </el-button>
                      <el-button 
                        type="danger" 
                        size="small" 
                        @click.stop="openRejectDialog(row)"
                      >
                        退回
                    </el-button>
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
                        :disabled="row.status !== 'QUESTIONED'"
                      >
                        通过
                    </el-button>
                    <el-button 
                      type="warning" 
                      size="small" 
                        @click.stop="openQuestionDialog(row)"
                      :disabled="row.status !== 'QUESTIONED'"
                    >
                        提出疑问
                    </el-button>
                      <el-button 
                        type="danger" 
                        size="small" 
                        @click.stop="openRejectDialog(row)"
                        :disabled="row.status !== 'QUESTIONED'"
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
            :close-on-click-modal="false"
          >
            <div class="preview-content">
              <div v-if="currentMaterial?.attachments && currentMaterial.attachments.length > 0" class="material-preview">
                <template v-for="attachment in currentMaterial.attachments" :key="attachment.id">
                  <div class="attachment-item">
                    <div class="attachment-info">
                      <span class="file-name">{{ attachment.fileName }}</span>
                      <div class="attachment-actions">
                        <el-button 
                          type="primary" 
                          size="small" 
                          @click="previewAttachment(attachment)"
                          v-if="isPreviewable(attachment)"
                        >
                          预览
                        </el-button>
                        <el-button 
                          type="info" 
                          size="small" 
                          @click="downloadAttachment(attachment)"
                        >
                          下载
                        </el-button>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
              <div v-else class="no-attachments">
                暂无附件
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

          <!-- 添加预览对话框 -->
          <el-dialog
            v-model="showPreviewDialog"
            title="材料预览"
            width="80%"
            destroy-on-close
            :close-on-click-modal="false"
          >
            <div class="preview-container">
              <img 
                v-if="previewUrl" 
                :src="previewUrl" 
                class="preview-image"
                alt="预览图片"
              />
            </div>
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
const showStatusDropdown = ref(false)

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
  comment: '',
  evaluationType: '',
  score: 0
})
const submitting = ref(false)

const handleReview = async (row) => {
  if (row.status === 'CORRECTED') {
    try {
      submitting.value = true;
      await axios.post('/question-materials/review', {
        materialId: row.id,
        status: 'APPROVED',
        comment: '确认修改无误，予以通过',
        evaluationType: row.evaluationType, // 使用原有的评估类型
        score: row.score // 使用原有的分数
      });
      
      ElMessage.success('审核通过成功');
      await fetchQuestionMaterials();
    } catch (error) {
      console.error('审核失败:', error);
      ElMessage.error('审核失败：' + (error.response?.data?.message || '未知错误'));
    } finally {
      submitting.value = false;
    }
  } else {
    // 原有的逻辑
    currentMaterial.value = row;
    reviewForm.value = {
      status: '',
      comment: '',
      evaluationType: '',
      score: 0
    };
    reviewDialogVisible.value = true;
  }
};

const openRejectDialog = (row) => {
  if (row.status === 'CORRECTED') {
    ElMessageBox.prompt('请输入退回原因', '退回确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入退回原因'
    }).then(({ value }) => {
      if (!value.trim()) {
        ElMessage.warning('请输入退回原因');
        return;
      }
      handleReject(row, value);
    }).catch(() => {});
  } else {
    currentMaterial.value = row;
    reviewForm.value = {
      status: 'REJECTED',
      comment: '',
      evaluationType: '',
      score: 0
    };
    reviewDialogVisible.value = true;
  }
};

const handleViewDetails = (row) => {
  currentMaterial.value = row;
  previewDialogVisible.value = true;
};

const openQuestionDialog = (row) => {
  currentMaterial.value = row;
  reportForm.value = {
    note: ''
  };
  reportDialogVisible.value = true;
};


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
    console.log('提交审核数据:', {
      materialId: currentMaterial.value.id,
      status: reviewForm.value.status,
      comment: reviewForm.value.comment,
      evaluationType: reviewForm.value.evaluationType,
      score: reviewForm.value.score
    });
    
    const response = await axios.post('/question-materials/review', {
      materialId: currentMaterial.value.id,
      status: reviewForm.value.status,
      comment: reviewForm.value.comment,
      evaluationType: reviewForm.value.evaluationType,
      score: reviewForm.value.score
    });
    
    console.log('审核提交响应:', response);
    
    if (response.data.success) {
      ElMessage.success(response.data.message || '审核提交成功');
      reviewDialogVisible.value = false;
      await fetchQuestionMaterials();
    } else {
      ElMessage.error(response.data.message || '审核提交失败');
    }
  } catch (error) {
    console.error('审核提交失败:', error);
    console.error('错误详情:', error.response?.data);
    ElMessage.error('审核提交失败：' + (error.response?.data?.message || '未知错误'));
  } finally {
    submitting.value = false;
  }
}

// 材料预览相关
const previewDialogVisible = ref(false)

const isPreviewable = (attachment) => {
  const previewableTypes = ['jpg', 'jpeg', 'png', 'gif', 'pdf'];
  const fileExtension = attachment.fileName.split('.').pop().toLowerCase();
  return previewableTypes.includes(fileExtension);
};

const previewAttachment = async (attachment) => {
  const fileExtension = attachment.fileName.split('.').pop().toLowerCase();
  
  // 检查文件类型是否支持预览
  if (!['jpg', 'jpeg', 'png', 'pdf'].includes(fileExtension)) {
    ElMessage.warning('该文件类型不支持预览，将自动下载');
    downloadAttachment(attachment);
    return;
  }

  try {
    console.log('开始预览文件:', attachment);
    loading.value = true;
    const response = await axios.get(`/evaluation/preview/${attachment.id}`, {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    console.log('获取到预览响应:', response);
    const blob = new Blob([response.data], {
      type: fileExtension === 'pdf' ? 'application/pdf' : `image/${fileExtension}`
    });
    const url = URL.createObjectURL(blob);
    
    if (fileExtension === 'pdf') {
      // 在新窗口中打开PDF
      window.open(url, '_blank');
    } else {
      // 图片在当前窗口预览
      showPreviewDialog.value = true;
      previewUrl.value = url;
    }
  } catch (error) {
    console.error('预览失败:', error);
    ElMessage.error('预览失败，请尝试下载查看');
    // 预览失败时自动触发下载
    downloadAttachment(attachment);
  } finally {
    loading.value = false;
  }
};

const downloadAttachment = async (attachment) => {
  try {
    const response = await axios.get(`/evaluation/download/${attachment.id}`, {
      responseType: 'blob',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = attachment.fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    ElMessage.success('文件已开始下载');
  } catch (error) {
    console.error('下载失败:', error);
    ElMessage.error('下载失败，请稍后重试');
  }
};

// 状态数据
const selectedMaterials = ref([])
const showRejectModal = ref(false)
const showReportModal = ref(false)
const rejectReason = ref('')
const reportNote = ref('')

// 计算属性
const isAllSelected = computed(() => false)

// 选择相关方法
const isSelected = () => false
const toggleSelection = () => {}
const toggleAllSelection = () => {}

// 添加新的响应式变量
const reportDialogVisible = ref(false)
const reportForm = ref({
  note: ''
})

onMounted(() => {
  fetchQuestionMaterials();
});

const submitReport = async () => {
  if (!currentMaterial.value || !currentMaterial.value.id) {
    ElMessage.error('无效的材料信息');
    return;
  }

  try {
    submitting.value = true;
    console.log('提交上报数据:', {
      materialIds: [currentMaterial.value.id],
      note: reportForm.value.note
    });

    const response = await axios.post('/question-materials/report', {
      materialIds: [currentMaterial.value.id],
      note: reportForm.value.note || "请审核这些存在疑问的材料"
    });
    
    console.log('上报响应:', response);
    
    if (response.data.success) {
      ElMessage.success(response.data.message || '材料已成功上报');
      reportDialogVisible.value = false;
      await fetchQuestionMaterials();
    } else {
      ElMessage.error(response.data.message || '上报失败');
    }
  } catch (error) {
    console.error('上报失败:', error);
    console.error('错误详情:', error.response?.data);
    ElMessage.error('上报失败：' + (error.response?.data?.message || error.message));
  } finally {
    submitting.value = false;
  }
};

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

axios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      console.error('API错误响应:', error.response);
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        router.push('/login');
      }
    }
    return Promise.reject(error);
  }
);

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

const toggleStatusDropdown = () => {
  showStatusDropdown.value = !showStatusDropdown.value;
};

const selectStatus = (status) => {
  filterStatus.value = status;
  showStatusDropdown.value = false;
  handleSearch();
};

const getStatusClass = (status) => {
  const classMap = {
    'QUESTIONED': 'status-warning',
    'CORRECTED': 'status-success',
    'REJECTED': 'status-danger',
    'APPROVED': 'status-success'
  };
  return classMap[status] || 'status-default';
};

const getStatusText = (status) => {
  const textMap = {
    'QUESTIONED': '待审核',
    'CORRECTED': '已改正',
    'REJECTED': '已驳回',
    'APPROVED': '已通过'
  };
  return textMap[status] || status;
};

const handleSearch = () => {
  fetchQuestionMaterials();
};

// 添加 handleReject 方法
const handleReject = async (row, comment) => {
  try {
    submitting.value = true;
    await axios.post('/question-materials/review', {
      materialId: row.id,
      status: 'UNCORRECT',
      comment: comment,
      evaluationType: '',
      score: 0
    });
    
    ElMessage.success('退回成功');
    await fetchQuestionMaterials();
  } catch (error) {
    console.error('退回失败:', error);
    ElMessage.error('退回失败：' + (error.response?.data?.message || '未知错误'));
  } finally {
    submitting.value = false;
  }
};

const handleReReview = async () => {
  try {
    ElMessageBox.confirm(
      '确定要将当前中队所有综测表状态更新为已审核状态吗？\n\n' +
      '将更新以下表格：\n' +
      '- 德育测评表(moral_monthly_evaluation)\n' +
      '- 科研竞赛表(research_competition_evaluation)\n' +
      '- 文体活动表(sports_arts_evaluation)',
      '综测表状态更新确认',
      {
        confirmButtonText: '确定更新',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true,
        customClass: 'confirm-dialog'
      }
    ).then(async () => {
      submitting.value = true;
      const response = await axios.post('/question-materials/finalize-review');
      
      console.log('finalize-review API响应:', response.data);
      
      if (response.data.success) {
        let affectedRows = 0;
        
        // 处理可能嵌套的数据结构
        if (response.data.data && response.data.data.affectedRows) {
          affectedRows = response.data.data.affectedRows;
        } else if (response.data.affectedRows) {
          affectedRows = response.data.affectedRows;
        }
        
        ElMessage({
          type: 'success',
          message: `${response.data.message || '综测表状态更新成功'}\n共更新 ${affectedRows} 条记录`,
          duration: 5000
        });
        fetchQuestionMaterials();
      } else {
        ElMessage.warning(response.data.message || '操作失败');
      }
      submitting.value = false;
    }).catch(() => {
      // 用户取消操作
    });
  } catch (error) {
    console.error('再次审核失败:', error);
    ElMessage.error('操作失败：' + (error.response?.data?.message || '未知错误'));
    submitting.value = false;
  }
};

// 添加预览对话框相关的响应式变量
const showPreviewDialog = ref(false);
const previewUrl = ref('');
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
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
  padding: 6px 12px;
  height: 32px;
  font-size: 13px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.action-buttons .el-button + .el-button {
  margin-left: 8px;
}

/* 成功按钮样式 */
.action-buttons .el-button--success {
  background: #67c23a;
  border-color: #67c23a;
}

.action-buttons .el-button--success:hover {
  background-color: #85ce61;
  border-color: #85ce61;
}

/* 危险按钮样式 */
.action-buttons .el-button--danger {
  background: #f56c6c;
  border-color: #f56c6c;
}

.action-buttons .el-button--danger:hover {
  background: #f78989;
  border-color: #f78989;
}

/* 信息按钮样式 */
.action-buttons .el-button--info {
  background: #909399;
  border-color: #909399;
}

.action-buttons .el-button--info:hover {
  background: #a6a9ad;
  border-color: #a6a9ad;
}

/* 警告按钮样式 */
.action-buttons .el-button--warning {
  background: #e6a23c;
  border-color: #e6a23c;
}

.action-buttons .el-button--warning:hover {
  background: #ebb563;
  border-color: #ebb563;
}

/* 禁用状态样式 */
.action-buttons .el-button.is-disabled {
  background: #f5f7fa;
  border-color: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
}

.action-buttons .el-button.is-disabled:hover {
  transform: none;
  box-shadow: none;
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
  padding: 20px;
}

.material-preview {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

.attachment-item {
  background: white;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.attachment-item:last-child {
  margin-bottom: 0;
}

.attachment-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-name {
  font-size: 14px;
  color: #606266;
  margin-right: 12px;
}

.attachment-actions {
  display: flex;
  gap: 8px;
}

.attachment-actions .el-button {
  margin: 0;
}

.no-attachments {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
  background: #f5f7fa;
  border-radius: 8px;
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

/* 添加自定义下拉框样式 */
.custom-dropdown {
  position: relative;
  width: 200px;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  background: white;
}

.dropdown-header:hover {
  border-color: #c0c4cc;
}

.dropdown-icon {
  font-size: 12px;
  color: #909399;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-top: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  z-index: 100;
}

.dropdown-item {
  padding: 8px 12px;
  cursor: pointer;
}

.dropdown-item:hover {
  background-color: #f5f7fa;
}

/* 添加状态样式 */
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

/* 移除批量操作相关样式 */
.batch-actions {
  display: none;
}

.batch-btn {
  display: none;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

.custom-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  outline: none;
}

.custom-button.primary {
  background-color: #409eff;
  color: white;
}

.custom-button.primary:hover {
  background-color: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.custom-button.success {
  background-color: #67c23a;
  color: white;
}

.custom-button.success:hover {
  background-color: #85ce61;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.re-review-btn {
  min-width: 110px;
  font-weight: 500;
}

.re-review-btn:disabled {
  background-color: #b3d8a4;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.refresh-icon, .check-icon {
  font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header-actions {
    flex-wrap: wrap;
  }
  
  .action-buttons {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }
}

.loading-icon {
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.confirm-dialog {
  width: 450px;
}

:deep(.confirm-dialog .el-message-box__content) {
  text-align: left;
  white-space: pre-line;
  line-height: 1.6;
}

:deep(.confirm-dialog .el-message-box__message p) {
  margin: 5px 0;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  max-height: 80vh;
  overflow: auto;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style> 