<template>
  <div class="results-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <div class="main-content">
        <h2 class="page-title">综合评测结果查询</h2>
        <div class="filter-section">
          <!-- 表格类型选择 -->
          <div class="filter-item">
            <label>表格类型</label>
            <select v-model="selectedTableType">
              <option value="moral">德育分表(A类)</option>
              <option value="research">研究竞赛评价(C类)</option>
              <option value="sports">体艺评价(D类)</option>
              <option value="comprehensive">综合测评总表</option>
            </select>
          </div>

          <!-- 学年选择 -->
          <div class="filter-item">
            <label>学年</label>
            <select v-model="selectedYear">
              <option value="">请选择学年</option>
              <option v-for="year in academicYears" :key="year" :value="year">
                {{ year }}学年
              </option>
            </select>
          </div>

          <!-- 学期选择 -->
          <div class="filter-item">
            <label>学期</label>
            <select v-model="selectedTerm">
              <option value="">请选择学期</option>
              <option value="1">第一学期</option>
              <option value="2">第二学期</option>
            </select>
          </div>
          
          <!-- 月份选择（仅德育分表A类显示） -->
          <div class="filter-item" v-if="selectedTableType === 'moral'">
            <label>月份</label>
            <select v-model="selectedMonth">
              <option value="">全部月份</option>
              <option value="9">9月</option>
              <option value="10">10月</option>
              <option value="11">11月</option>
              <option value="12">12月</option>
              <option value="1">1月</option>
              <option value="2">2月</option>
              <option value="3">3月</option>
              <option value="4">4月</option>
              <option value="5">5月</option>
              <option value="6">6月</option>
            </select>
          </div>
          
          <!-- 专业选择 -->
          <div class="filter-item">
            <label>专业</label>
            <select v-model="selectedMajor" @change="handleMajorChange">
              <option value="">全部专业</option>
              <option v-for="major in majors" :key="major.id" :value="major.id">
                {{ major.name }}
              </option>
            </select>
          </div>

          <!-- 班级选择（综合测评总表不显示） -->
          <div class="filter-item" v-if="selectedTableType !== 'comprehensive'">
            <label>班级</label>
            <select v-model="selectedClass">
              <option value="">全部班级</option>
              <option v-for="class_ in filteredClasses" :key="class_.id" :value="class_.id">
                {{ class_.name }}
              </option>
            </select>
          </div>
        </div>
        
        <!-- 查询和导出按钮 -->
        <div class="button-container">
          <button class="search-btn" @click="searchResults" :disabled="isLoading">
            <i class="fas fa-search"></i> {{ isLoading ? '查询中...' : '查询' }}
          </button>
          <button class="export-btn" @click="exportToExcel" :disabled="isLoading || students.length === 0">
            <i class="fas fa-download"></i> 导出Excel
          </button>
        </div>

        <!-- 错误信息提示 -->
        <div v-if="errorMessage" class="error-message">
          <i class="fas fa-exclamation-circle"></i> {{ errorMessage }}
        </div>

        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>正在加载数据，请稍候...</p>
        </div>

        <!-- 结果展示表格 -->
        <div v-else class="table-container">
          <div v-if="students.length === 0 && !isLoading" class="empty-data">
            <p>暂无符合条件的数据</p>
          </div>
          <table v-else>
            <thead>
              <tr>
                <th>班级</th>
                <th>姓名</th>
                <th>学号</th>
                <th>原始分</th>
                <th>总加分</th>
                <th>总扣分</th>
                <th>原始总分</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="student in students" :key="student.id || student.student_id">
                <td>{{ student.class_id || student.className }}</td>
                <td>{{ student.student_name || student.name }}</td>
                <td>{{ student.student_id }}</td>
                <td>{{ student.base_score || 0 }}</td>
                <td>{{ student.total_bonus || student.totalBonus || 0 }}</td>
                <td>{{ student.total_penalty || student.totalPenalty || 0 }}</td>
                <td>{{ student.raw_score || student.rawScore || 0 }}</td>
                <td>
                  <button class="detail-btn" @click="viewMaterials(student)">查看材料</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 详情弹窗 -->
        <div v-if="showDetailModal" class="modal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>学生测评详情</h3>
              <button class="close-btn" @click="showDetailModal = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="student-info">
                <p><strong>学号：</strong>{{ selectedStudent.studentId }}</p>
                <p><strong>姓名：</strong>{{ selectedStudent.name }}</p>
                <p><strong>班级：</strong>{{ selectedStudent.className }}</p>
              </div>
              <div class="score-details">
                <h4>分项得分详情</h4>
                <ul>
                  <li>德育分：{{ selectedStudent.moralScore }}</li>
                  <li>智育分：{{ selectedStudent.intellectualScore }}</li>
                  <li>体育分：{{ selectedStudent.physicalScore }}</li>
                  <li>美育分：{{ selectedStudent.aestheticScore }}</li>
                  <li>劳动分：{{ selectedStudent.laborScore }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- 材料弹窗 -->
        <div v-if="showMaterialsModal" class="modal">
          <div class="modal-content material-modal-content">
            <div class="modal-header">
              <h3>学生证明材料</h3>
              <button class="close-btn" @click="showMaterialsModal = false">&times;</button>
            </div>
            <div class="modal-body">
              <div class="materials-container">
                <div v-for="material in materials" :key="material.id" class="material-item">
                  <p><strong>材料名称：</strong>{{ material.title || '无标题' }}</p>
                  <p><strong>材料描述：</strong>{{ material.description || '无描述' }}</p>
                  <p><strong>上传时间：</strong>{{ formatDate(material.created_at) }}</p>
                  <p><strong>附件数量：</strong>{{ getAttachmentCount(material) }}</p>
                  
                  <div class="material-actions">
                    <select v-if="getAttachmentCount(material) > 0" 
                            v-model="selectedAttachments[material.id]"
                            class="attachment-select">
                      <option value="">选择附件</option>
                      <option v-for="attachment in parseAttachments(material.attachments)" 
                              :key="attachment.id || Math.random()" 
                              :value="attachment">
                        {{ attachment.file_name }} ({{ formatFileSize(attachment.file_size) }})
                      </option>
                    </select>
                    
                    <button class="download-btn" 
                            :disabled="!selectedAttachments[material.id]"
                            @click="downloadSelectedAttachment(material.id)">
                      查看附件
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import TopBar from '@/components/TopBar.vue';
import Sidebar from '@/components/Sidebar.vue';
import axios from 'axios';

// API基础URL
const API_URL = import.meta.env.VITE_API_URL || '';

// 班级数据
const classes = ref([]);

// 专业数据
const majors = ref([]);

// 生成学年选项（最近4年）
const academicYears = ref([]);
const generateAcademicYears = () => {
  const currentDate = new Date();
  const currentYear = currentDate.getFullYear();
  const startYear = currentYear - 4;
  
  for (let i = 0; i < 4; i++) {
    const year = startYear + i;
    academicYears.value.push(`${year}-${year + 1}`);
  }
};

// 选中的筛选条件
const selectedClass = ref('');
const selectedMajor = ref('');
const selectedTableType = ref('moral');
const selectedYear = ref('');
const selectedTerm = ref('');
const selectedMonth = ref('');
const isLoading = ref(false);

// 学生数据
const students = ref([]);
const totalRecords = ref(0);
const errorMessage = ref('');

// 已选择的附件，使用 material.id 作为键
const selectedAttachments = ref({});

// 获取专业列表
const getMajors = async () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      errorMessage.value = '用户未登录，请先登录';
      return;
    }
    
    const response = await axios.get(`${API_URL}/api/classes/majors`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    if (response.data.success) {
      // 将字符串数组转换为包含id和name的对象数组
      majors.value = response.data.data.map((majorName) => ({
        id: majorName,
        name: majorName
      }));
    } else {
      console.error('获取专业列表失败:', response.data.message);
    }
  } catch (error) {
    console.error('获取专业列表失败:', error);
  }
};

// 获取班级列表
const getClasses = async () => {
  if (!selectedMajor.value) {
    classes.value = [];
    return;
  }
  
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      errorMessage.value = '用户未登录，请先登录';
      return;
    }
    
    const response = await axios.get(`${API_URL}/api/classes/by-major`, {
      params: { major: selectedMajor.value },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    if (response.data.success) {
      classes.value = response.data.data;
    } else {
      console.error('获取班级列表失败:', response.data.message);
    }
  } catch (error) {
    console.error('获取班级列表失败:', error);
  }
};

// 根据选择的专业筛选班级
const filteredClasses = computed(() => {
  return classes.value;
});

// 监听专业变化
const handleMajorChange = () => {
  selectedClass.value = ''; // 重置班级选择
  getClasses(); // 获取对应专业的班级
};

// 表格类型映射，将前端选项映射到后端API所需的值
const tableTypeMap = {
  'moral': 'A',           // 德育分表
  'intellectual': 'B',    // 智育分表
  'research': 'C',        // 研究竞赛评价
  'sports': 'D',          // 体艺评价
  'comprehensive': 'ALL'  // 综合测评总表
};

// 查询结果
const searchResults = async () => {
  isLoading.value = true;
  errorMessage.value = '';
  
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      errorMessage.value = '用户未登录，请先登录';
      return;
    }
    
    // 将前端表格类型转换为后端API需要的格式
    const apiTableType = tableTypeMap[selectedTableType.value] || selectedTableType.value;
    
    const response = await axios.get(`${API_URL}/api/instructor/evaluation/results`, {
      params: {
        tableType: apiTableType,
        academicYear: selectedYear.value,
        term: selectedTerm.value,
        major: selectedMajor.value,
        classId: selectedTableType.value !== 'comprehensive' ? selectedClass.value : undefined,
        month: apiTableType === 'A' ? selectedMonth.value : undefined
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    console.log('查询结果:', response.data);
    
    if (response.data.success) {
      // 直接使用后端返回的数据，不做字段转换
      students.value = response.data.data;
      totalRecords.value = response.data.total;
    } else {
      errorMessage.value = response.data.message || '获取数据失败';
      students.value = [];
    }
  } catch (error) {
    console.error('查询失败:', error);
    errorMessage.value = error.response?.data?.message || '查询失败，请稍后重试';
    students.value = [];
  } finally {
    isLoading.value = false;
  }
};

// 在组件挂载时生成学年选项
onMounted(() => {
  generateAcademicYears();
  // 默认选择最近的学年
  selectedYear.value = academicYears.value[academicYears.value.length - 1];
  // 获取专业和班级数据
  getMajors();
});

// 详情弹窗
const showDetailModal = ref(false);
const selectedStudent = ref(null);

// 材料弹窗
const showMaterialsModal = ref(false);
const materials = ref([]);

// 查看详情
const viewDetail = (student) => {
  selectedStudent.value = student;
  showDetailModal.value = true;
};

// 查看材料
const viewMaterials = async (student) => {
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      errorMessage.value = '用户未登录，请先登录';
      return;
    }
    
    // 查询学生材料前先显示加载状态
    isLoading.value = true;
    
    // 获取选中的表格类型
    const apiTableType = tableTypeMap[selectedTableType.value] || selectedTableType.value;
    
    console.log('查询材料参数:', {
      formType: apiTableType,
      studentId: student.student_id
    });
    
    const response = await axios.get(`${API_URL}/api/review/materials`, {
      params: {
        formType: apiTableType,
        studentId: student.student_id
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    isLoading.value = false;
    
    console.log('材料查询响应:', response.data);
    
    if (response.data.success) {
      if (response.data.data.length === 0) {
        alert('该学生暂无证明材料');
        return;
      }
      console.log("获取到的材料:", response.data.data);
      materials.value = response.data.data;
      selectedStudent.value = student;
      showMaterialsModal.value = true;
    } else {
      alert(response.data.message || '获取证明材料失败');
    }
  } catch (error) {
    isLoading.value = false;
    console.error('获取证明材料失败:', error);
    alert('获取证明材料失败，请稍后重试: ' + (error.message || '未知错误'));
  }
};

// 解析附件字符串为对象数组
const parseAttachments = (attachmentsStr) => {
  if (!attachmentsStr) return [];
  
  console.log('原始附件数据:', attachmentsStr);
  console.log('附件数据类型:', typeof attachmentsStr);
  
  try {
    // 处理字符串形式的JSON对象 (控制台显示的形式)
    if (typeof attachmentsStr === 'string' && attachmentsStr.includes('{')) {
      // 尝试提取JSON对象的内容
      const match = attachmentsStr.match(/\{([^}]+)\}/);
      if (match) {
        console.log('匹配到的JSON内容:', match[0]);
        // 构造一个正确的JSON字符串
        try {
          return [JSON.parse(match[0])];
        } catch (e) {
          console.log('解析匹配内容失败:', e);
          // 继续尝试其他方法
        }
      }
    }
    
    // 如果已经是对象，直接返回包含该对象的数组
    if (typeof attachmentsStr === 'object' && !Array.isArray(attachmentsStr) && attachmentsStr !== null) {
      return [attachmentsStr];
    }
    
    // 如果是数组，直接返回
    if (Array.isArray(attachmentsStr)) {
      return attachmentsStr;
    }
    
    // 最后尝试直接解析字符串
    if (typeof attachmentsStr === 'string') {
      try {
        return JSON.parse(attachmentsStr);
      } catch (e) {
        console.log('直接解析字符串失败:', e);
      }
    }
    
    // 所有尝试均失败，尝试正则提取信息
    if (typeof attachmentsStr === 'string') {
      const idMatch = attachmentsStr.match(/id: (\d+)/);
      const fileNameMatch = attachmentsStr.match(/file_name: ['"](.+?)['"]/);
      const filePathMatch = attachmentsStr.match(/file_path: ['"](.+?)['"]/);
      const fileSizeMatch = attachmentsStr.match(/file_size: (\d+)/);
      
      if (idMatch || fileNameMatch || filePathMatch) {
        return [{
          id: idMatch ? parseInt(idMatch[1]) : null,
          file_name: fileNameMatch ? fileNameMatch[1] : "未知文件",
          file_path: filePathMatch ? filePathMatch[1] : "",
          file_size: fileSizeMatch ? parseInt(fileSizeMatch[1]) : 0
        }];
      }
    }
    
    return [];
  } catch (error) {
    console.error('解析附件信息失败:', error);
    return [];
  }
};

// 获取附件数量
const getAttachmentCount = (material) => {
  const attachments = parseAttachments(material.attachments);
  console.log('解析后的附件:', attachments);
  return attachments.length;
};

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间';
  try {
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch (error) {
    return dateStr; // 如果解析失败，返回原始字符串
  }
};

// 格式化文件大小
const formatFileSize = (sizeInBytes) => {
  if (!sizeInBytes) return '未知大小';
  const kb = sizeInBytes / 1024;
  if (kb < 1024) {
    return kb.toFixed(2) + ' KB';
  } else {
    return (kb / 1024).toFixed(2) + ' MB';
  }
};

// 下载选中的附件
const downloadSelectedAttachment = (materialId) => {
  const selectedAttachment = selectedAttachments.value[materialId];
  if (!selectedAttachment) {
    alert('请先选择要下载的附件');
    return;
  }
  
  downloadAttachment(selectedAttachment);
};

// 处理附件选择 - 移除即时下载的逻辑
const handleAttachmentSelect = async (event, row) => {
  // 此方法现在不需要做任何下载操作
  // 附件的选择已通过 v-model 绑定到 selectedAttachments
  console.log('附件已选择, 点击下载按钮继续');
};

// 修改 downloadFirstAttachment 函数，使用新的附件选择逻辑
const downloadFirstAttachment = (material) => {
  const attachments = parseAttachments(material.attachments);
  console.log('尝试下载的附件:', attachments);
  if (attachments && attachments.length > 0) {
    // 自动选择第一个附件
    selectedAttachments.value[material.id] = attachments[0];
    downloadAttachment(attachments[0]);
  } else {
    alert('没有可下载的附件');
  }
};

// 下载附件
const downloadAttachment = async (attachment) => {
  try {
    console.log('准备下载附件:', attachment);
    
    const token = localStorage.getItem('token');
    if (!token) {
      alert('用户未登录，请先登录');
      return;
    }
    
    // 检查附件对象格式
    if (!attachment || (!attachment.id && !attachment.file_path)) {
      alert('附件信息不完整，无法下载');
      return;
    }
    
    // 直接使用附件ID作为下载参数
    const attachmentId = attachment.id;
    console.log('使用的附件ID:', attachmentId);
    
    // 显示下载中提示
    isLoading.value = true;
    
    const response = await axios.get(`${API_URL}/api/evaluation/download/${attachmentId}`, {
      responseType: 'blob',
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    isLoading.value = false;
    
    // 创建 Blob 对象
    const blob = new Blob([response.data], { type: response.headers['content-type'] });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = attachment.file_name || '附件下载.pdf';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (error) {
    isLoading.value = false;
    console.error('下载失败:', error);
    alert('下载失败: ' + (error.response?.data?.message || error.message || '未知错误'));
  }
};

// 导出Excel
const exportToExcel = () => {
  // 实现导出Excel的逻辑
  console.log('导出Excel');
};
</script>

<style scoped>
.results-container {
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
  background: #f5f5f5;
  overflow-y: auto;
}

.page-title {
  margin-bottom: 20px;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
  align-items: flex-end;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  justify-content: center;
}

.button-container {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 200px;
}

.filter-item label {
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

.filter-item select {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-width: 180px;
  font-size: 14px;
  color: #606266;
}

.filter-item select:focus {
  border-color: #409eff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.search-btn {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  font-size: 14px;
}

.search-btn:hover {
  background: #66b1ff;
}

.search-btn:disabled, .export-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.export-btn {
  padding: 8px 20px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  font-size: 14px;
}

.export-btn:hover {
  background: #85ce61;
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
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

.detail-btn {
  padding: 4px 8px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.detail-btn:hover {
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

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
}

.student-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.score-details {
  padding: 15px;
}

.score-details h4 {
  margin-bottom: 10px;
  color: #303133;
}

.score-details ul {
  list-style: none;
  padding: 0;
}

.score-details li {
  margin: 8px 0;
  color: #606266;
}

/* 错误信息样式 */
.error-message {
  padding: 10px 15px;
  background-color: #fef0f0;
  color: #f56c6c;
  border-radius: 4px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

.empty-data {
  padding: 50px 0;
  text-align: center;
  color: #909399;
  font-size: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 材料项样式 */
.materials-container {
  max-height: 500px;
  overflow-y: auto;
}

.material-item {
  margin-bottom: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.material-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.view-btn, .download-btn {
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.view-btn {
  background: #409eff;
  color: white;
}

.download-btn {
  background: #67c23a;
  color: white;
}

.view-btn:hover {
  background: #66b1ff;
}

.download-btn:hover {
  background: #85ce61;
}

.material-modal-content {
  max-width: 600px;
  width: 90%;
}

.attachment-select {
  padding: 5px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  flex-grow: 1;
  max-width: 300px;
  font-size: 12px;
}
</style> 