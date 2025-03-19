<template>
  <div class="config-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="page-header">
          <h2>系统参数配置</h2>
        </div>

        <div class="config-sections">
          <!-- 材料配置 -->
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <h3>材料配置</h3>
                <el-button type="primary" @click="saveMaterialConfig" :loading="saving.material">
                  保存更改
                </el-button>
              </div>
            </template>
            
            <el-form :model="materialConfig" label-width="140px">
              <el-form-item label="允许的文件类型">
                <el-select 
                  v-model="materialConfig.allowedTypes" 
                  multiple 
                  placeholder="选择允许的文件类型"
                >
                  <el-option label="PDF文件" value="pdf" />
                  <el-option label="图片文件" value="image" />
                  <el-option label="Word文档" value="word" />
                  <el-option label="Excel表格" value="excel" />
                </el-select>
              </el-form-item>
              <el-form-item label="单个文件大小限制">
                <el-input-number 
                  v-model="materialConfig.maxFileSize" 
                  :min="1" 
                  :max="50"
                /> MB
              </el-form-item>
              <el-form-item label="材料数量上限">
                <el-input-number 
                  v-model="materialConfig.maxFileCount" 
                  :min="1" 
                  :max="20"
                />
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 通知配置 -->
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <h3>通知配置</h3>
                <el-button type="primary" @click="saveNotificationConfig" :loading="saving.notification">
                  保存更改
                </el-button>
              </div>
            </template>
            
            <el-form :model="notificationConfig" label-width="140px">
              <el-form-item label="开启邮件通知">
                <el-switch v-model="notificationConfig.emailEnabled" />
              </el-form-item>
              <el-form-item 
                label="邮件服务器配置"
                v-if="notificationConfig.emailEnabled"
              >
                <el-input v-model="notificationConfig.emailServer" placeholder="SMTP服务器地址" />
              </el-form-item>
              <el-form-item label="开启站内通知">
                <el-switch v-model="notificationConfig.internalEnabled" />
              </el-form-item>
              <el-form-item label="通知保留天数">
                <el-input-number 
                  v-model="notificationConfig.retentionDays" 
                  :min="1" 
                  :max="365"
                />
              </el-form-item>
            </el-form>
          </el-card>
        </div>

        <!-- 操作日志 -->
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <h3>配置修改日志</h3>
              <el-button type="info" @click="refreshLogs">
                刷新日志
              </el-button>
            </div>
          </template>
          
          <el-table :data="configLogs" style="width: 100%">
            <el-table-column prop="time" label="修改时间" width="180" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="section" label="配置项" width="120" />
            <el-table-column prop="description" label="修改内容" />
          </el-table>
        </el-card>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from "@/components/TopBar.vue"
import Sidebar from "@/components/Sidebar.vue"

// 保存状态
const saving = reactive({
  material: false,
  notification: false
})

// 材料配置
const materialConfig = ref({
  allowedTypes: ['pdf', 'image'],
  maxFileSize: 10,
  maxFileCount: 5
})

// 通知配置
const notificationConfig = ref({
  emailEnabled: false,
  emailServer: '',
  internalEnabled: true,
  retentionDays: 30
})

// 配置修改日志
const configLogs = ref([
  {
    time: '2024-03-20 15:30:00',
    operator: '管理员',
    section: '材料配置',
    description: '修改了允许的文件类型'
  }
])

// 保存材料配置
const saveMaterialConfig = async () => {
  try {
    saving.material = true
    // TODO: 调用保存API
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success('材料配置保存成功')
    addConfigLog('材料配置', '更新了材料配置')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.material = false
  }
}

// 保存通知配置
const saveNotificationConfig = async () => {
  try {
    saving.notification = true
    // TODO: 调用保存API
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success('通知配置保存成功')
    addConfigLog('通知配置', '更新了通知配置')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.notification = false
  }
}

// 添加配置日志
const addConfigLog = (section, description) => {
  configLogs.value.unshift({
    time: new Date().toLocaleString(),
    operator: '管理员', // 应该从登录用户信息中获取
    section,
    description
  })
}

// 刷新日志
const refreshLogs = async () => {
  // TODO: 调用获取日志API
  await new Promise(resolve => setTimeout(resolve, 500))
  ElMessage.success('日志已刷新')
}
</script>

<style scoped>
.config-container {
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

.config-sections {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input-number) {
  width: 180px;
}

:deep(.el-date-picker) {
  width: 100%;
}
</style> 