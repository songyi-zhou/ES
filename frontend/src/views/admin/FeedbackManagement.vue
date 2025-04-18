<template>
  <div class="import-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="feedback-management">
          <div class="page-header">
            <h2>反馈管理</h2>
          </div>

          <!-- 筛选条件 -->
          <div class="filter-container">
            <el-select v-model="filterType" placeholder="问题类型" clearable>
              <el-option label="功能异常" value="bug" />
              <el-option label="功能建议" value="suggestion" />
              <el-option label="其他" value="other" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="处理状态" clearable>
              <el-option label="待处理" value="pending" />
              <el-option label="处理中" value="processing" />
              <el-option label="已解决" value="resolved" />
            </el-select>
            <el-button type="primary" @click="fetchFeedback">查询</el-button>
          </div>

          <!-- 反馈列表 -->
          <el-table :data="feedbackList" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getTypeTag(row.type)">
                  {{ getTypeText(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="问题描述" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button type="primary" link @click="viewDetail(row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>

          <!-- 详情对话框 -->
          <el-dialog
            v-model="detailDialogVisible"
            title="反馈详情"
            width="50%"
          >
            <div v-if="currentFeedback" class="feedback-detail">
              <div class="detail-item">
                <span class="label">问题类型：</span>
                <el-tag :type="getTypeTag(currentFeedback.type)">
                  {{ getTypeText(currentFeedback.type) }}
                </el-tag>
              </div>
              <div class="detail-item">
                <span class="label">问题描述：</span>
                <div class="content">{{ currentFeedback.description }}</div>
              </div>
              <div class="detail-item">
                <span class="label">联系方式：</span>
                <span>{{ currentFeedback.email || '未提供' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">提交时间：</span>
                <span>{{ formatDate(currentFeedback.createdAt) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">处理状态：</span>
                <el-tag :type="getStatusTag(currentFeedback.status)">
                  {{ getStatusText(currentFeedback.status) }}
                </el-tag>
              </div>
              <div v-if="currentFeedback.resolution" class="detail-item">
                <span class="label">处理结果：</span>
                <div class="content">{{ currentFeedback.resolution }}</div>
              </div>
              <div v-if="currentFeedback.resolvedBy" class="detail-item">
                <span class="label">处理人：</span>
                <span>{{ currentFeedback.resolvedBy }}</span>
              </div>
              <div v-if="currentFeedback.resolvedAt" class="detail-item">
                <span class="label">处理时间：</span>
                <span>{{ formatDate(currentFeedback.resolvedAt) }}</span>
              </div>
              
              <!-- 处理表单 -->
              <div v-if="currentFeedback.status !== 'resolved'" class="resolution-form">
                <el-form :model="resolutionForm" label-width="80px">
                  <el-form-item label="处理结果">
                    <el-input
                      v-model="resolutionForm.resolution"
                      type="textarea"
                      :rows="4"
                      placeholder="请输入处理结果"
                    />
                  </el-form-item>
                </el-form>
                <div class="form-actions">
                  <el-button type="primary" @click="handleResolve">确认处理</el-button>
                </div>
              </div>
            </div>
          </el-dialog>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import TopBar from '@/components/TopBar.vue'
import Sidebar from '@/components/Sidebar.vue'
import { useRouter } from 'vue-router'

// 数据
const feedbackList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterType = ref('bug')
const filterStatus = ref('pending')
const detailDialogVisible = ref(false)
const currentFeedback = ref(null)

// 处理表单
const resolutionForm = reactive({
  resolution: ''
})

// 重置筛选条件
const resetFilters = () => {
  filterType.value = 'bug'
  filterStatus.value = 'pending'
  currentPage.value = 1
  fetchFeedback()
}

// 获取反馈列表
const fetchFeedback = async () => {
  console.log('开始获取反馈列表')
  console.log('请求参数:', {
    page: currentPage.value - 1,
    size: pageSize.value,
    type: filterType.value,
    status: filterStatus.value
  })
  
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    console.log('获取到的token:', token ? '已获取' : '未获取')
    
    const response = await axios.get('/api/admin/feedback', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        type: filterType.value,
        status: filterStatus.value
      },
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    console.log('获取反馈列表成功:', response.data)
    if (response.data.code === 200) {
      const responseData = response.data.data
      feedbackList.value = responseData.content
      total.value = responseData.totalElements
      console.log('更新后的反馈列表:', feedbackList.value)
      console.log('总记录数:', total.value)
      console.log('总页数:', responseData.totalPages)
    } else {
      console.error('获取反馈列表失败:', response.data.message)
      ElMessage.error(response.data.message || '获取反馈列表失败')
    }
  } catch (error) {
    console.error('获取反馈列表失败:', error)
    console.error('错误详情:', {
      status: error.response?.status,
      message: error.response?.data?.message,
      data: error.response?.data
    })
    
    if (error.response?.status === 401) {
      console.warn('Token已过期，跳转到登录页')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error('获取反馈列表失败')
    }
  } finally {
    loading.value = false
    console.log('获取反馈列表完成')
  }
}

// 查看详情
const viewDetail = (feedback) => {
  console.log('查看反馈详情:', feedback)
  currentFeedback.value = feedback
  resolutionForm.resolution = feedback.resolution || ''
  detailDialogVisible.value = true
  console.log('当前反馈详情:', currentFeedback.value)
}

// 处理反馈
const handleResolve = async () => {
  console.log('开始处理反馈')
  console.log('当前反馈ID:', currentFeedback.value.id)
  console.log('处理结果:', resolutionForm.resolution)
  
  if (!resolutionForm.resolution) {
    console.warn('处理结果为空')
    ElMessage.warning('请输入处理结果')
    return
  }

  try {
    const token = localStorage.getItem('token')
    console.log('获取到的token:', token ? '已获取' : '未获取')
    
    const response = await axios.put(`/api/admin/feedback/${currentFeedback.value.id}/resolve`, null, {
      params: {
        resolvedBy: 'admin',
        resolution: resolutionForm.resolution
      },
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    console.log('处理反馈成功:', response.data)
    ElMessage.success('处理成功')
    detailDialogVisible.value = false
    console.log('关闭详情对话框')
    fetchFeedback()
  } catch (error) {
    console.error('处理反馈失败:', error)
    console.error('错误详情:', {
      status: error.response?.status,
      message: error.response?.data?.message,
      data: error.response?.data
    })
    
    if (error.response?.status === 401) {
      console.warn('Token已过期，跳转到登录页')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error('处理反馈失败')
    }
  }
}

// 分页处理
const handleSizeChange = (val) => {
  console.log('每页显示数量变更:', val)
  pageSize.value = val
  currentPage.value = 1
  console.log('重置页码为1')
  fetchFeedback()
}

const handleCurrentChange = (val) => {
  console.log('当前页码变更:', val)
  currentPage.value = val
  fetchFeedback()
}

// 辅助方法
const getTypeTag = (type) => {
  const tag = {
    'bug': 'danger',
    'suggestion': 'success',
    'default': 'info'
  }[type] || 'info'
  console.log(`获取类型标签: ${type} -> ${tag}`)
  return tag
}

const getTypeText = (type) => {
  const text = {
    'bug': '功能异常',
    'suggestion': '功能建议',
    'default': '其他'
  }[type] || '其他'
  console.log(`获取类型文本: ${type} -> ${text}`)
  return text
}

const getStatusTag = (status) => {
  const tag = {
    'pending': 'warning',
    'processing': 'primary',
    'resolved': 'success',
    'default': 'info'
  }[status] || 'info'
  console.log(`获取状态标签: ${status} -> ${tag}`)
  return tag
}

const getStatusText = (status) => {
  const text = {
    'pending': '待处理',
    'processing': '处理中',
    'resolved': '已解决',
    'default': status
  }[status] || status
  console.log(`获取状态文本: ${status} -> ${text}`)
  return text
}

const formatDate = (date) => {
  const formattedDate = new Date(date).toLocaleString()
  console.log(`格式化日期: ${date} -> ${formattedDate}`)
  return formattedDate
}

// 初始化
const router = useRouter()
console.log('初始化反馈管理页面')
fetchFeedback()
</script>

<style scoped>
.import-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.feedback-management {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.filter-container {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.feedback-detail {
  padding: 20px;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
  align-items: flex-start;
}

.detail-item .label {
  width: 80px;
  font-weight: bold;
  color: #606266;
}

.detail-item .content {
  flex: 1;
  white-space: pre-wrap;
}

.resolution-form {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style> 