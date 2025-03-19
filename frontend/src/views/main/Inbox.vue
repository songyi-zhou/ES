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
              <template v-if="filteredMessages.length">
                <div 
                  v-for="message in filteredMessages" 
                  :key="message.id"
                  class="message-item"
                  :class="{ unread: !message.read }"
                  @click="readMessage(message)"
                >
                  <div class="message-info">
                    <span class="message-title">{{ message.title }}</span>
                    <span class="message-time">{{ message.time }}</span>
                  </div>
                  <div class="message-preview">{{ message.preview }}</div>
                  <div class="message-footer">
                    <span class="message-sender">{{ message.sender }}</span>
                    <el-tag size="small" :type="getTagType(message.type)">
                      {{ message.type }}
                    </el-tag>
                  </div>
                </div>
              </template>
              <el-empty v-else description="暂无消息" />
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
          <span class="detail-time">{{ currentMessage?.time }}</span>
        </div>
        <div class="detail-content" v-html="currentMessage?.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 消息分类
const categories = [
  { type: 'all', name: '全部消息', icon: 'fas fa-inbox', unread: 5 },
  { type: 'system', name: '系统通知', icon: 'fas fa-bell', unread: 2 },
  { type: 'evaluation', name: '综测相关', icon: 'fas fa-tasks', unread: 3 },
  { type: 'announcement', name: '重要公告', icon: 'fas fa-bullhorn', unread: 0 }
]

const currentCategory = ref('all')
const dialogVisible = ref(false)
const currentMessage = ref(null)

// 示例消息数据
const messages = ref([
  {
    id: 1,
    type: 'evaluation',
    title: '您的综测申请已通过审核',
    preview: '您提交的2024年第一学期综合测评申请已通过审核...',
    content: '您提交的2024年第一学期综合测评申请已通过审核，最终得分为88分。如有疑问，请及时联系辅导员。',
    sender: '综测系统',
    time: '2024-03-20 14:30',
    read: false
  },
  {
    id: 2,
    type: 'system',
    title: '系统更新通知',
    preview: '系统将于今晚22:00-23:00进行例行维护...',
    content: '系统将于今晚22:00-23:00进行例行维护，维护期间系统将暂停服务。给您带来的不便敬请谅解。',
    sender: '系统管理员',
    time: '2024-03-19 16:00',
    read: true
  },
  {
    id: 3,
    type: 'announcement',
    title: '关于开展2024年综合测评工作的通知',
    preview: '现将2024年综合测评工作相关事项通知如下...',
    content: '现将2024年综合测评工作相关事项通知如下：\n1. 测评时间：3月25日-4月10日\n2. 提交材料要求：...',
    sender: '教务处',
    time: '2024-03-18 09:00',
    read: false
  }
])

// 根据当前分类筛选消息
const filteredMessages = computed(() => {
  if (currentCategory.value === 'all') {
    return messages.value
  }
  return messages.value.filter(msg => msg.type === currentCategory.value)
})

// 获取当前分类名称
const getCurrentCategoryName = computed(() => {
  const category = categories.find(c => c.type === currentCategory.value)
  return category ? category.name : '全部消息'
})

// 标记消息为已读
const readMessage = (message) => {
  message.read = true
  currentMessage.value = message
  dialogVisible.value = true
  updateUnreadCount()
}

// 更新未读消息数
const updateUnreadCount = () => {
  categories.forEach(category => {
    if (category.type === 'all') {
      category.unread = messages.value.filter(msg => !msg.read).length
    } else {
      category.unread = messages.value.filter(msg => !msg.read && msg.type === category.type).length
    }
  })
}

// 全部标记为已读
const markAllRead = () => {
  messages.value.forEach(msg => msg.read = true)
  updateUnreadCount()
  ElMessage.success('已全部标记为已读')
}

// 刷新消息
const refreshMessages = async () => {
  // TODO: 调用获取消息API
  await new Promise(resolve => setTimeout(resolve, 500))
  updateUnreadCount()
  ElMessage.success('消息已刷新')
}

// 获取标签类型
const getTagType = (type) => {
  const types = {
    system: 'info',
    evaluation: 'success',
    announcement: 'warning'
  }
  return types[type] || 'info'
}
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