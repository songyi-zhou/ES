<template>
  <div class="review-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>综合测评材料审核</h2>
          <div class="filter-section">
            <select v-model="selectedStatus" class="filter-select">
              <option value="">材料状态</option>
              <option value="pending">待审核</option>
              <option value="approved">已通过</option>
              <option value="rejected">已退回</option>
              <option value="reported">已上报</option>
            </select>
          </div>
        </div>

        <!-- 材料列表表格 -->
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>提交时间</th>
                <th>学号</th>
                <th>姓名</th>
                <th>材料名称</th>
                <th>材料状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="material in filteredMaterials" :key="material.id">
                <td>{{ formatDate(material.submitTime) }}</td>
                <td>{{ material.studentId }}</td>
                <td>{{ material.studentName }}</td>
                <td>
                  <span class="material-name" @click="viewMaterial(material)">
                    {{ material.name }}
                  </span>
                </td>
                <td>
                  <span :class="['status-badge', material.status]">
                    {{ getStatusText(material.status) }}
                  </span>
                </td>
                <td class="actions">
                  <button @click="approve(material)" class="btn approve">
                    审核通过
                  </button>
                  <button @click="reject(material)" class="btn reject">
                    退回修改
                  </button>
                  <button @click="report(material)" class="btn report">
                    上报疑问
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 统计信息 -->
        <div class="statistics">
          <div class="stat-item">
            <span class="stat-label">待审核材料：</span>
            <span class="stat-value">{{ stats.pending }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">疑问材料：</span>
            <span class="stat-value warning">{{ stats.reported }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已处理材料：</span>
            <span class="stat-value">{{ stats.processed }}</span>
          </div>
        </div>

        <div class="notice" v-if="stats.reported > 0">
          <i class="warning-icon">⚠️</i>
          还有 {{ stats.reported }} 个疑问材料未解决，解决后才能生成统计表
        </div>

        <!-- 生成统计表按钮 -->
        <button 
          class="generate-btn" 
          :disabled="stats.reported > 0"
          @click="generateReport"
        >
          生成统计表
        </button>

        <!-- 统计表描述弹窗 -->
        <div v-if="showReportDescModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>统计表描述</h3>
              <button class="close-btn" @click="closeReportDescModal">×</button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label>统计表说明：</label>
                <textarea 
                  v-model="reportDescription"
                  rows="4"
                  placeholder="请输入对本次统计表的说明"
                ></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button 
                class="submit-btn" 
                @click="submitReportDesc"
                :disabled="!reportDescription.trim()"
              >
                确认提交
              </button>
              <button class="cancel-btn" @click="closeReportDescModal">取消</button>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 材料预览弹窗 -->
    <div v-if="showPreviewModal" class="modal">
      <div class="modal-content preview-modal">
        <div class="preview-header">
          <h3>材料预览</h3>
          <button class="close-btn" @click="showPreviewModal = false">×</button>
        </div>
        <div class="preview-body">
          <div class="preview-info">
            <div class="info-item">
              <span class="info-label">提交学生：</span>
              <span>{{ currentMaterial?.studentName }} ({{ currentMaterial?.studentId }})</span>
            </div>
            <div class="info-item">
              <span class="info-label">提交时间：</span>
              <span>{{ formatDate(currentMaterial?.submitTime) }}</span>
            </div>
          
          </div>
          <div class="preview-content">
            <div v-if="isImageFile" class="image-preview">
              <img :src="previewUrl" alt="材料预览">
            </div>
            <div v-else-if="isPdfFile" class="pdf-preview">
              <iframe :src="previewUrl" type="application/pdf" width="100%" height="100%"></iframe>
            </div>
            <div v-else class="file-info">
              <i class="file-icon">📄</i>
              <p>{{ currentMaterial?.name }}</p>
              <a :href="previewUrl" target="_blank" class="download-btn">下载查看</a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 审核弹窗 -->
    <div v-if="showApproveModal" class="modal">
      <div class="modal-content">
        <h3>审核通过</h3>
        <div class="form-group">
          <label>加分类别：</label>
          <select v-model="approvalForm.category">
            <option value="">请选择加分类别</option>
            <option value="A">A类 - 思想品德</option>
            <option value="B">B类 - 学习成绩</option>
            <option value="C">C类 - 科技创新</option>
            <option value="D">D类 - 社会实践</option>
            <option value="E">E类 - 文体特长</option>
          </select>
        </div>
        <div class="form-group">
          <label>加分分值：</label>
          <input type="number" v-model="approvalForm.points" step="0.1" min="0" max="10">
        </div>
        <div class="form-group">
          <label>审核意见：</label>
          <textarea v-model="approvalForm.comment" rows="3"></textarea>
        </div>
        <div class="modal-actions">
          <button @click="confirmApproval" class="btn-primary">确认</button>
          <button @click="showApproveModal = false" class="btn-secondary">取消</button>
        </div>
      </div>
    </div>

    <!-- 退回弹窗 -->
    <div v-if="showRejectModal" class="modal">
      <div class="modal-content">
        <h3>退回修改</h3>
        <div class="form-group">
          <label>退回原因：</label>
          <textarea v-model="rejectForm.reason" rows="4" placeholder="请详细说明退回原因..."></textarea>
        </div>
        <div class="modal-actions">
          <button @click="confirmReject" class="btn-primary">确认退回</button>
          <button @click="showRejectModal = false" class="btn-secondary">取消</button>
        </div>
      </div>
    </div>

    <!-- 上报疑问弹窗 -->
    <div v-if="showReportModal" class="modal">
      <div class="modal-content">
        <h3>上报疑问材料</h3>
        <div class="form-group">
          <label>疑问说明：</label>
          <textarea v-model="reportForm.description" rows="4" placeholder="请详细说明疑问内容..."></textarea>
        </div>
        <div class="form-group">
          <label>上报给：</label>
          <select v-model="reportForm.supervisor">
            <option value="">请选择上级审核人</option>
            <option value="1">综测组长</option>
            <option value="2">班主任</option>
            <option value="3">辅导员</option>
          </select>
        </div>
        <div class="modal-actions">
          <button @click="confirmReport" class="btn-primary">确认上报</button>
          <button @click="showReportModal = false" class="btn-secondary">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 班级数据
const classes = ref([
  { id: '1', name: '计算机2101' },
  { id: '2', name: '计算机2102' },
  { id: '3', name: '计算机2103' }
])

// 筛选条件
const selectedClass = ref('')
const selectedStatus = ref('')

// 材料数据
const materials = ref([
  {
    id: 1,
    submitTime: '2024-03-15 14:30',
    studentId: '2021001',
    studentName: '张三',
    name: '2023年数学建模竞赛国家二等奖证书.pdf',
    category: 'C类',
    requestedPoints: 5,
    status: 'pending'
  },
  // 更多材料数据...
])

// 统计数据
const stats = ref({
  pending: 5,
  reported: 2,
  processed: 8
})

// 弹窗控制
const showApproveModal = ref(false)
const showRejectModal = ref(false)
const showReportModal = ref(false)

// 表单数据
const approvalForm = ref({
  category: '',
  points: 0,
  comment: ''
})

const rejectForm = ref({
  reason: ''
})

const reportForm = ref({
  description: '',
  supervisor: ''
})

// 当前选中的材料
const currentMaterial = ref(null)

// 材料预览相关
const showPreviewModal = ref(false)
const previewUrl = ref('')

// 判断文件类型
const isImageFile = computed(() => {
  const filename = currentMaterial.value?.name.toLowerCase() || ''
  return /\.(jpg|jpeg|png|gif|webp)$/.test(filename)
})

const isPdfFile = computed(() => {
  const filename = currentMaterial.value?.name.toLowerCase() || ''
  return /\.pdf$/.test(filename)
})

// 过滤材料列表
const filteredMaterials = computed(() => {
  return materials.value.filter(material => {
    if (selectedStatus.value && material.status !== selectedStatus.value) return false
    return true
  })
})

// 格式化日期
const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已退回',
    reported: '已上报'
  }
  return statusMap[status] || status
}

// 查看材料
const viewMaterial = (material) => {
  currentMaterial.value = material
  // 这里应该从后端获取文件的URL
  previewUrl.value = `/api/materials/${material.id}/file`
  showPreviewModal.value = true
}

// 审核通过
const approve = (material) => {
  currentMaterial.value = material
  showApproveModal.value = true
}

// 确认审核通过
const confirmApproval = () => {
  // 实现确认审核的逻辑
  console.log('审核通过:', approvalForm.value)
  showApproveModal.value = false
}

// 退回材料
const reject = (material) => {
  currentMaterial.value = material
  showRejectModal.value = true
}

// 确认退回
const confirmReject = () => {
  // 实现确认退回的逻辑
  console.log('退回材料:', rejectForm.value)
  showRejectModal.value = false
}

// 上报疑问
const report = (material) => {
  currentMaterial.value = material
  showReportModal.value = true
}

// 确认上报
const confirmReport = () => {
  // 实现确认上报的逻辑
  console.log('上报疑问:', reportForm.value)
  showReportModal.value = false
}

// 生成统计表
const showReportDescModal = ref(false)
const reportDescription = ref('')

const generateReport = () => {
  if (stats.value.reported > 0) return
  showReportDescModal.value = true
}

// 关闭描述弹窗
const closeReportDescModal = () => {
  showReportDescModal.value = false
  reportDescription.value = ''
}

// 提交统计表描述
const submitReportDesc = async () => {
  try {
    // 这里应该调用后端API提交统计表和描述
    // await fetch('/api/materials/report', {
    //   method: 'POST',
    //   headers: {
    //     'Content-Type': 'application/json'
    //   },
    //   body: JSON.stringify({
    //     description: reportDescription.value
    //   })
    // })

    // 关闭弹窗
    closeReportDescModal()
    
    // 提示成功
    alert('统计表生成成功！')
  } catch (error) {
    console.error('生成统计表失败:', error)
  }
}
</script>

<style scoped>
.review-container {
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

.filter-section {
  display: flex;
  gap: 15px;
}

.filter-select {
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-width: 150px;
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
  padding: 12px 15px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
  text-align: center !important;
}

.material-name {
  color: #409eff;
  cursor: pointer;
  text-align: center;
}

.material-name:hover {
  text-decoration: underline;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-badge.pending {
  background: #e6a23c1a;
  color: #e6a23c;
}

.status-badge.approved {
  background: #67c23a1a;
  color: #67c23a;
}

.status-badge.rejected {
  background: #f56c6c1a;
  color: #f56c6c;
}

.status-badge.reported {
  background: #9093991a;
  color: #909399;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.btn.approve {
  background: #67c23a1a;
  color: #67c23a;
}

.btn.reject {
  background: #f56c6c1a;
  color: #f56c6c;
}

.btn.report {
  background: #9093991a;
  color: #909399;
}

.btn:hover {
  opacity: 0.8;
}

.statistics {
  display: flex;
  gap: 30px;
  margin: 20px 0;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  color: #606266;
}

.stat-value {
  font-weight: 500;
  color: #409eff;
}

.stat-value.warning {
  color: #e6a23c;
}

.notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 4px;
  margin: 20px 0;
}

.warning-icon {
  font-size: 18px;
}

.generate-btn {
  display: block;
  width: 200px;
  margin: 20px auto;
  padding: 12px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.generate-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.generate-btn:not(:disabled):hover {
  background: #66b1ff;
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
  max-width: 500px;
}

.modal-content h3 {
  margin-bottom: 20px;
  color: #303133;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary {
  padding: 8px 15px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-secondary {
  padding: 8px 15px;
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary:hover {
  background: #66b1ff;
}

.btn-secondary:hover {
  background: #f9f9fa;
}

/* 材料预览弹窗样式 */
.preview-modal {
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
}

.close-btn:hover {
  color: #606266;
}

.preview-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.preview-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.info-item {
  display: flex;
  gap: 10px;
}

.info-label {
  color: #909399;
  white-space: nowrap;
}

.preview-content {
  flex: 1;
  min-height: 400px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.pdf-preview {
  width: 100%;
  height: 100%;
}

.file-info {
  text-align: center;
  padding: 20px;
}

.file-icon {
  font-size: 48px;
}

.download-btn {
  display: inline-block;
  margin-top: 15px;
  padding: 8px 15px;
  background: #409eff;
  color: white;
  border-radius: 4px;
  text-decoration: none;
}

.download-btn:hover {
  background: #66b1ff;
}
</style> 