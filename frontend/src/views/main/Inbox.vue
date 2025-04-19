<template>
  <div class="inbox-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <div class="main-content">
        <div class="inbox-layout">
          <!-- 左侧消息分类 -->
          <div class="category-sidebar">
            <div class="category-header">
              <h3>消息分类</h3>
            </div>
            <div class="category-list">
              <div 
                v-for="category in categories" 
                :key="category.type"
                class="category-item"
                :class="{ active: currentCategory === category.type }"
                @click="currentCategory = category.type"
              >
                <el-badge :value="category.unread" :hidden="!category.unread">
                  <div class="category-content">
                    <i :class="category.icon"></i>
                    <span>{{ category.name }}</span>
                  </div>
                </el-badge>
              </div>
            </div>
          </div>

          <!-- 右侧消息列表 -->
          <div class="message-section">
            <div class="message-header">
              <h2>{{ getCurrentCategoryName }}</h2>
              <div class="header-actions">
                <el-button type="primary" @click="markAllRead" size="small">
                  全部已读
                </el-button>
                <el-button @click="refreshMessages" size="small">
                  刷新
                </el-button>
              </div>
            </div>

            <div class="message-list">
              <div 
                v-for="message in filteredMessages" 
                :key="message.id"
                class="message-item"
                :class="{ unread: !message.isRead }"
                @click="readMessage(message)"
              >
                <div class="message-info">
                  <span class="message-title">{{ message.title || '无标题' }}</span>
                  <span class="message-time">{{ formatTime(message.createTime) }}</span>
                </div>
                <div class="message-preview">{{ message.content || '无内容' }}</div>
                <div class="message-footer">
                  <span class="message-sender">{{ message.sender || '系统' }}</span>
                  <el-tag size="small" :type="getTagType(message.type)">
                    {{ formatType(message.type) }}
                  </el-tag>
                </div>
              </div>
              <el-empty v-if="!filteredMessages.length" :description="`暂无${getCurrentCategoryName}`" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="currentMessage?.title"
      width="50%"
    >
      <div class="message-detail">
        <div class="detail-header">
          <span class="detail-sender">发送人：{{ currentMessage?.sender }}</span>
          <span class="detail-time">{{ currentMessage?.createTime }}</span>
        </div>
        <div class="detail-content" v-html="currentMessage?.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 消息分类
const categories = [
  { type: 'all', name: '全部消息', icon: 'fas fa-inbox', unread: 0 },
  { type: 'system', name: '系统通知', icon: 'fas fa-bell', unread: 0 },
  { type: 'evaluation', name: '综测相关', icon: 'fas fa-tasks', unread: 0 },
  { type: 'announcement', name: '重要公告', icon: 'fas fa-bullhorn', unread: 0 }
]

const currentCategory = ref('all')
const dialogVisible = ref(false)
const currentMessage = ref(null)
const messages = ref([])
const page = ref(1)
const size = ref(10)

// 获取消息列表
const fetchMessages = async () => {
  try {
    const params = new URLSearchParams({
      page: page.value.toString(),
      size: size.value.toString()
    })
    
    if (currentCategory.value !== 'all') {
      params.append('type', currentCategory.value)
    }
    
    console.log('发送请求到:', `/api/messages?${params.toString()}`)
    const response = await axios.get(`/api/messages?${params.toString()}`)
    console.log('后端返回的原始数据:', response.data)
    
    if (response.data.code === 200) {
      const rawData = response.data.data
      console.log('解析到的消息数组:', rawData)
      
      // 确保数据是数组
      const messageData = Array.isArray(rawData) ? rawData : [rawData]
      
      // 转换数据结构
      messages.value = messageData.map(msg => ({
        id: msg.id,
        title: msg.title || '无标题',
        content: msg.content || '无内容',
        createTime: msg.createTime || new Date().toISOString(),
        sender: msg.sender || '系统',
        type: msg.type || 'system',
        isRead: msg.isRead === false
      }))
      
      console.log('处理后的消息列表:', messages.value)
    } else {
      messages.value = []
      console.warn('获取消息列表失败:', response.data.message)
      ElMessage.error(response.data.message || '获取消息失败')
    }
  } catch (error) {
    messages.value = []
    console.error('获取消息列表异常:', error)
    ElMessage.error('获取消息失败')
  }
}

// 获取未读消息数
const fetchUnreadCount = async () => {
  console.log('开始获取未读消息数')
  
  try {
    const response = await axios.get('/api/messages/unread/count')
    console.log('未读消息数响应:', response.data)
    
    if (response.data.success) {
      const counts = response.data.data
      console.log('未读消息统计:', counts)
      
      categories.forEach(category => {
        if (category.type === 'all') {
          category.unread = Object.values(counts).reduce((a, b) => a + b, 0)
        } else {
          category.unread = counts[category.type] || 0
        }
      })
    } else {
      console.warn('获取未读消息数失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取未读消息数异常:', error)
    ElMessage.error('获取未读消息数失败')
  }
}

// 标记消息为已读
const readMessage = async (message) => {
  console.log('开始标记消息为已读:', message)
  
  try {
    await axios.put(`/api/messages/${message.id}/read`)
    console.log('消息标记已读成功:', message.id)
    
    message.isRead = true
    currentMessage.value = message
    dialogVisible.value = true
    
    await fetchUnreadCount()
  } catch (error) {
    console.error('标记消息已读异常:', error)
    ElMessage.error('标记已读失败')
  }
}

// 全部标记为已读
const markAllRead = async () => {
  console.log('开始标记所有消息为已读', {
    category: currentCategory.value
  })
  
  try {
    const type = currentCategory.value === 'all' ? null : currentCategory.value
    console.log('请求参数:', { type })
    
    await axios.put('/api/messages/read/all', null, { params: { type } })
    console.log('全部标记已读成功')
    
    ElMessage.success('已全部标记为已读')
    await fetchMessages()
    await fetchUnreadCount()
  } catch (error) {
    console.error('标记全部已读异常:', error)
    ElMessage.error('标记全部已读失败')
  }
}

// 刷新消息
const refreshMessages = async () => {
  console.log('开始刷新消息')
  
  await fetchMessages()
  await fetchUnreadCount()
  console.log('消息刷新完成')
  
  ElMessage.success('消息已刷新')
}

// 删除消息
const deleteMessage = async (messageId) => {
  console.log('开始删除消息:', messageId)
  
  try {
    await axios.delete(`/api/messages/${messageId}`)
    console.log('消息删除成功:', messageId)
    
    ElMessage.success('删除成功')
    await fetchMessages()
    await fetchUnreadCount()
  } catch (error) {
    console.error('删除消息异常:', error)
    ElMessage.error('删除失败')
  }
}

// 获取标签类型
const getTagType = (type) => {
  const types = {
    'system': 'info',
    'evaluation': 'success',
    'announcement': 'warning'
  }
  return types[type] || 'info'
}

// 根据当前分类筛选消息
const filteredMessages = computed(() => {
  console.log('开始筛选消息，当前分类:', currentCategory.value)
  
  if (!Array.isArray(messages.value)) {
    console.warn('messages.value 不是数组:', messages.value)
    return []
  }
  
  let result = []
  if (currentCategory.value === 'all') {
    result = messages.value
  } else {
    result = messages.value.filter(msg => msg.type === currentCategory.value)
  }
  
  console.log(`当前分类 ${currentCategory.value} 筛选后的消息:`, result)
  return result
})

// 在模板渲染部分也添加 v-if 的判断日志
const hasMessages = computed(() => {
  // 直接使用 filteredMessages 的长度判断
  return filteredMessages.value.length > 0
})

// 获取当前分类名称
const getCurrentCategoryName = computed(() => {
  const category = categories.find(c => c.type === currentCategory.value)
  return category ? category.name : '全部消息'
})

// 添加时间格式化函数
const formatTime = (time) => {
  if (!time) return '无时间'
  try {
    const date = new Date(time)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return time
  }
}

// 添加消息类型格式化函数
const formatType = (type) => {
  const typeMap = {
    'system': '系统通知',
    'evaluation': '综测相关',
    'announcement': '重要公告'
  }
  return typeMap[type] || type
}

// 初始化
onMounted(() => {
  console.log('消息中心初始化')
  fetchMessages()
  fetchUnreadCount()
})

// 监听分类变化
watch(currentCategory, (newCategory) => {
  console.log('分类切换:', newCategory)
  fetchMessages()
})
</script>

<style scoped>
.inbox-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 20px;
  background: #f5f5f5;
  overflow: hidden;
}

.inbox-layout {
  display: flex;
  gap: 20px;
  height: 100%;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* 左侧分类栏样式 */
.category-sidebar {
  width: 200px;
  border-right: 1px solid #e4e7ed;
  background: #f8f9fa;
}

.category-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.category-header h3 {
  margin: 0;
  color: #333;
}

.category-list {
  padding: 10px 0;
}

.category-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.category-item:hover {
  background: #ecf5ff;
}

.category-item.active {
  background: #ecf5ff;
  color: #409EFF;
}

.category-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 右侧消息列表样式 */
.message-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-header h2 {
  margin: 0;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  padding: 15px;
  border-radius: 8px;
  background: #f8f9fa;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.message-item:hover {
  background: #ecf5ff;
}

.message-item.unread {
  background: #fff;
  border: 1px solid #e4e7ed;
  position: relative;
}

.message-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #409EFF;
}

.message-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.message-title {
  font-weight: 500;
  color: #333;
}

.message-time {
  color: #999;
  font-size: 12px;
}

.message-preview {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-sender {
  color: #666;
  font-size: 13px;
}

/* 消息详情样式 */
.message-detail {
  padding: 20px;
}

.detail-header {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  color: #666;
}

.detail-content {
  line-height: 1.6;
  color: #333;
}
</style>