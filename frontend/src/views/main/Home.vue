<template>
  <div class="home-container">
    <TopBar />
    <div class="content">
      <Sidebar />
      <main class="main-content">
        <div class="left-section">
          <!-- 快捷入口 -->
          <section class="quick-access">
            <h2>快捷入口</h2>
            <div class="quick-buttons">
              <button
                  v-for="(item, index) in quickLinks"
                  :key="index"
                  @click="navigate(item.path)"
                  class="quick-item"
              >
                {{ item.name }}
                <span class="remove-btn" @click.stop="removeQuickLink(index)">✖</span>
              </button>
              <!-- 添加快捷入口的按钮 -->
              <button class="quick-item add-btn" @click="showAddModal = true">+</button>
            </div>
          </section>

          <!-- 系统通知 -->
          <section class="system-notifications">
            <h2>系统通知</h2>
            <ul>
              <li v-for="(notification, index) in notifications" :key="index">
                {{ notification }}
              </li>
            </ul>
          </section>
        </div>

        <!-- 新闻 -->
        <section class="news-section">
          <h2>新闻</h2>
          <ul>
            <li v-for="(news, index) in newsList" :key="index">
              {{ news }}
            </li>
          </ul>
        </section>
      </main>
    </div>

    <!-- 添加快捷入口弹窗 -->
    <div v-if="showAddModal" class="modal">
      <div class="modal-content">
        <h3>添加快捷入口</h3>
        <select v-model="newLink.path">
          <option v-for="route in sidebarRoutes" :key="route.path" :value="route.path">
            {{ route.name }}
          </option>
        </select>
        <div class="modal-actions">
          <button @click="addQuickLink">添加</button>
          <button @click="showAddModal = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import TopBar from "@/components/TopBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import { ref, onMounted, computed } from "vue";
import router from "@/router/index.js";
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 动态生成快捷入口选项
const sidebarRoutes = computed(() => {
  const userRole = userStore.roleLevel
  const routes = []

  // 基础路由 - 首页
  routes.push({ name: "首页", path: "/home" })

  // 根据用户角色添加对应的路由
  if (userRole === 4) { // 管理员
    routes.push(
      // 管理员功能模块
      { name: "权限管理", path: "/admin/permissions" },
      { name: "用户信息导入", path: "/admin/user-import" },
      { name: "系统参数配置", path: "/admin/system-config" },
      // 系统相关
      { name: "系统更新公告", path: "/system/updates" },
      { name: "用户帮助引导", path: "/system/help" },
      { name: "意见与建议提交", path: "/system/feedback" },
      // 个人信息管理
      { name: "修改密码", path: "/profile/change-password" },
      { name: "修改个人信息", path: "/profile/edit" },
      { name: "查看个人信息", path: "/profile/view" }
    )
  } else if (userRole === 3) { // 导员
    routes.push(
      // 导员功能模块
      { name: "综测结果查看", path: "/evaluation/instructor-view" },
      { name: "导员疑问材料审核", path: "/evaluation/instructor-review-question-materials" },
      { name: "综测小组选定", path: "/evaluation/group-select" },
      { name: "中队权限管理", path: "/instructor/permissions" },
      // 系统相关
      { name: "系统更新公告", path: "/system/updates" },
      { name: "用户帮助引导", path: "/system/help" },
      { name: "意见与建议提交", path: "/system/feedback" },
      // 个人信息管理
      { name: "修改密码", path: "/profile/change-password" },
      { name: "修改个人信息", path: "/profile/edit" },
      { name: "查看个人信息", path: "/profile/view" }
    )
  } else if (userRole === 2) { // 综测小组负责人
    routes.push(
      // 个人综测管理
      { name: "综合测评材料上报", path: "/evaluation/upload" },
      { name: "综合测评细则查看", path: "/evaluation/rules" },
      { name: "综测查询", path: "/evaluation/query" },
      // 综测相关工作（负责人特有）
      { name: "综合测评细则发布", path: "/evaluation/upload-rules" },
      { name: "综合测评表格审核", path: "/evaluation/review-evaluation-forms" },
      { name: "综测小组管理", path: "/evaluation/group-members-manage" },
      { name: "加分规则管理", path: "/evaluation/bonus-rule-manage" },
      { name: "负责人疑问材料审核", path: "/evaluation/review-question-materials" },
      { name: "综测启动配置", path: "/evaluation/start-config" },
      // 系统相关
      { name: "系统更新公告", path: "/system/updates" },
      { name: "用户帮助引导", path: "/system/help" },
      { name: "意见与建议提交", path: "/system/feedback" },
      // 个人信息管理
      { name: "修改密码", path: "/profile/change-password" },
      { name: "修改个人信息", path: "/profile/edit" },
      { name: "查看个人信息", path: "/profile/view" }
    )
  } else if (userRole === 1) { // 综测小组成员
    routes.push(
      // 个人综测管理
      { name: "综合测评材料上报", path: "/evaluation/upload" },
      { name: "综合测评细则查看", path: "/evaluation/rules" },
      { name: "综测查询", path: "/evaluation/query" },
      // 综测相关工作（成员特有）
      { name: "综合测评材料审核", path: "/evaluation/review-materials" },
      // 系统相关
      { name: "系统更新公告", path: "/system/updates" },
      { name: "用户帮助引导", path: "/system/help" },
      { name: "意见与建议提交", path: "/system/feedback" },
      // 个人信息管理
      { name: "修改密码", path: "/profile/change-password" },
      { name: "修改个人信息", path: "/profile/edit" },
      { name: "查看个人信息", path: "/profile/view" }
    )
  } else { // 普通学生
    routes.push(
      // 个人综测管理
      { name: "综合测评材料上报", path: "/evaluation/upload" },
      { name: "综合测评细则查看", path: "/evaluation/rules" },
      { name: "综测查询", path: "/evaluation/query" },
      // 系统相关
      { name: "系统更新公告", path: "/system/updates" },
      { name: "用户帮助引导", path: "/system/help" },
      { name: "意见与建议提交", path: "/system/feedback" },
      // 个人信息管理
      { name: "修改密码", path: "/profile/change-password" },
      { name: "修改个人信息", path: "/profile/edit" },
      { name: "查看个人信息", path: "/profile/view" }
    )
  }

  return routes
})

// 读取本地存储的快捷入口
const quickLinks = ref([]);


// 系统通知 & 新闻
const notifications = ref([
  "【公告】四六级考试报名开始时间：2025-03-15",
  "【提醒】请尽快完成个人信息更新",
  "【重要】系统维护通知：2025-03-20 23:00 - 03:00"
]);

const newsList = ref([
  "全国高校计算机大赛报名通道开启",
  "教育部发布新一轮本科教育教学改革方案",
  "AI技术在教育行业的最新应用趋势"
]);

// 控制弹窗
const showAddModal = ref(false);
const newLink = ref({ path: "" });

// 页面加载时读取本地存储
onMounted(() => {
  const storedLinks = JSON.parse(localStorage.getItem("quickLinks"));
  if (storedLinks) {
    quickLinks.value = storedLinks;
  }
});

// 页面跳转
const navigate = (path) => {
  router.push(path);
};

// 添加快捷入口
const addQuickLink = () => {
  if (!newLink.value.path) return;

  // 找到对应的名称
  const selectedRoute = sidebarRoutes.value.find(route => route.path === newLink.value.path);
  if (!selectedRoute) return;

  quickLinks.value.push({ name: selectedRoute.name, path: selectedRoute.path });
  saveLinks();
  newLink.value = { path: "" };
  showAddModal.value = false;
};

// 移除快捷入口
const removeQuickLink = (index) => {
  quickLinks.value.splice(index, 1);
  saveLinks();
};

// 本地存储快捷入口
const saveLinks = () => {
  localStorage.setItem("quickLinks", JSON.stringify(quickLinks.value));
};
</script>


<style scoped>
/* 主页整体布局 */
.home-container {
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
  display: flex;
  flex: 1;
  padding: 0;
  background: #ffffff;
  overflow: hidden;
}

/* 仪表盘布局 */
.left-section {
  display: grid;
  grid-template-rows: 1fr 1fr;
  grid-template-columns: 1fr;
  gap: 24px;
  width: 65%;
  padding: 20px;
  background: #ffffff;
  overflow-y: auto;
}

/* 右边区域：新闻 */
.news-section {
  width: 35%;
  padding: 20px;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  margin: 20px;
  overflow-y: auto;
}

/* 统一的板块样式 */
section {
  background: #ffffff;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 3px solid #007bff;
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

/* 悬停效果 */
section:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

/* 快捷入口（占比更大） */
.quick-access {
  grid-column: span 2;
}

.quick-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 8px;
  justify-items: center;
}

.quick-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  background: linear-gradient(135deg, #007bff, #0056b3);
  color: white;
  font-size: 12px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  text-align: center;
  min-height: 50px;
  width: 100%;
  max-width: 200px;
}

/* 添加按钮 */
.add-btn {
  background: rgba(0, 123, 255, 0.2);
  border: 2px dashed #007bff;
  font-size: 20px;
}

/* 移除按钮 */
.remove-btn {
  position: absolute;
  top: 5px;
  right: 5px;
  font-size: 12px;
  opacity: 0.7;
  cursor: pointer;
}

.remove-btn:hover {
  opacity: 1;
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
  max-width: 400px;
}

.modal-content h3 {
  margin-bottom: 15px;
}

.modal-content select {
  width: 100%;
  padding: 8px;
  margin-bottom: 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-actions button {
  padding: 8px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.modal-actions button:first-child {
  background: #007bff;
  color: white;
}

.modal-actions button:last-child {
  background: #f8f9fa;
  border: 1px solid #ddd;
}

/* 系统通知 */
.system-notifications {
  grid-column: span 2;
}

.news-section {
  grid-column: 2;
}

/* 滚动列表样式 */
.system-notifications ul,
.news-section ul {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 10px;
}

.system-notifications li,
.news-section li {
  padding: 12px 0;
  border-bottom: 1px solid #e0e0e0;
  transition: color 0.3s;
}

.system-notifications li:hover,
.news-section li:hover {
  color: #007bff;
}
</style>
