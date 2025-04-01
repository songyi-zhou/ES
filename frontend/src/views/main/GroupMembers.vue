<template>
  <div class="group-members-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="members-card">
          <h2>小组成员</h2>
          
          <!-- 加载状态 -->
          <div v-if="loading" class="loading-state">
            正在加载成员数据...
          </div>
          
          <!-- 错误提示 -->
          <div v-if="error" class="error-message">
            {{ error }}
          </div>
          
          <!-- 成员表格 -->
          <div v-if="!loading && !error" class="table-container">
            <table class="members-table">
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>学号</th>
                  <th>负责班级</th>
                  <th>专业</th>
                  <th>学院</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in members" :key="member.id">
                  <td>{{ member.name }}</td>
                  <td>{{ member.userId }}</td>
                  <td>{{ member.className }}</td>
                  <td>{{ member.major }}</td>
                  <td>{{ member.college }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import axios from 'axios';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const members = ref([]);
const router = useRouter();
const loading = ref(false);
const error = ref(null);
const userStore = useUserStore();

const fetchMembers = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    // 从用户存储中获取令牌
    const token = userStore.token;
    if (!token) {
      console.log('未找到认证令牌，重定向到登录页面');
      router.push('/login');
      return;
    }
    
    const response = await axios.get('/api/group-members', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (response.data.success) {
      members.value = response.data.data;
    } else {
      error.value = response.data.message || '获取成员数据失败';
    }
  } catch (err) {
    console.error('获取小组成员失败:', err);
    error.value = '获取小组成员失败';
    
    if (err.response) {
      if (err.response.status === 401 || err.response.status === 403) {
        // 如果是认证问题，清除令牌并重定向到登录页面
        userStore.clearUserInfo();
        router.push('/login');
      }
    }
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchMembers();
});
</script>

<style scoped>
.group-members-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  display: flex;
  flex: 1;
  overflow: hidden;
  background: #f0f2f5;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.members-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

h2 {
  color: #2c3e50;
  font-size: 24px;
  margin-bottom: 24px;
  position: relative;
  padding-left: 16px;
}

h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: #409EFF;
  border-radius: 2px;
}

.filter-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-item {
  max-width: 300px;
}

.filter-item label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.filter-item select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.filter-item select:hover {
  border-color: #c0c4cc;
}

.filter-item select:focus {
  border-color: #409EFF;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.table-container {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-top: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.members-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.members-table th,
.members-table td {
  padding: 12px 16px;
  text-align: center;
  border: none;
  border-bottom: 1px solid #ebeef5;
}

.members-table th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.members-table tr:hover {
  background: #f5f7fa;
}

.members-table td {
  color: #606266;
  font-size: 14px;
}

@media (max-width: 768px) {
  .filter-item {
    max-width: 100%;
  }
  
  .table-container {
    margin: 0 -16px;
    border-radius: 0;
  }
}
</style> 