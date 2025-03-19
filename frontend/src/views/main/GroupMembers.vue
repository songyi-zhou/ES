<template>
  <div class="group-members-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="members-card">
          <h2>小组成员</h2>
          
          <div class="filter-section">
            <div class="filter-item">
              <label>请选择学院：</label>
              <select v-model="selectedCollege" @change="fetchMembers">
                <option value="">请选择</option>
                <option value="信息科学技术学院">信息科学技术学院</option>
                <option value="机械工程学院">机械工程学院</option>
                <option value="电气工程学院">电气工程学院</option>
                <option value="材料科学与工程学院">材料科学与工程学院</option>
              </select>
            </div>
          </div>

          <!-- 成员表格 -->
          <div class="table-container">
            <table class="members-table">
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>负责班级所在专业</th>
                  <th>负责班级</th>
                  <th>上级</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in members" :key="member.id">
                  <td>{{ member.name }}</td>
                  <td>{{ member.major }}</td>
                  <td>{{ member.class }}</td>
                  <td>{{ member.supervisor }}</td>
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

const selectedCollege = ref('');
const members = ref([
  {
    name: '张三',
    major: '计算机科学与技术',
    class: '1班',
    supervisor: '李明'
  },
  {
    name: '李四',
    major: '网络工程',
    class: '1班',
    supervisor: '李明'
  },
  {
    name: '王五',
    major: '软件工程',
    class: '2班',
    supervisor: '张华'
  }
]);

const fetchMembers = async () => {
  if (!selectedCollege.value) return;
  
  try {
    const response = await axios.get('/api/evaluation/group-members', {
      params: {
        college: selectedCollege.value
      }
    });
    
    if (response.data.members) {
      members.value = response.data.members;
    }
  } catch (error) {
    console.error('获取成员数据失败:', error);
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