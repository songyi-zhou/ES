<template>
  <nav class="sidebar">
    <ul>
      <!-- 首页单独显示 -->
      <li>
        <div
            class="category-title"
            @click="navigate('/home')"
            :class="{ active: isActive('/home') }"
        >
          首页
        </div>
      </li>
      <li v-for="(category, index) in filteredMenuItems" :key="index">
        <!-- 分类标题，点击后展开/折叠 -->
        <div class="category-title" @click="toggleCategory(index)">
          {{ category.title }}
          <span class="arrow" :class="{ open: expandedCategories[index] }">▼</span>
        </div>
        <!-- 子菜单，默认折叠，点击分类后展开 -->
        <ul v-show="expandedCategories[index]" class="submenu">
          <li
              v-for="(item, subIndex) in category.items"
              :key="subIndex"
              @click="navigate(item.path)"
              :class="{ active: isActive(item.path) }"
          >
            {{ item.title }}
          </li>
        </ul>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserStore } from '@/stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

// 菜单数据
const menuItems = ref([
  {
    title: '个人综测管理',
    icon: 'el-icon-data-line',
    items: [
      { title: '综合测评材料上报', path: '/evaluation/upload', icon: 'el-icon-upload' },
      { title: '综合测评细则查看', path: '/evaluation/rules', icon: 'el-icon-document' },
      { title: '综测结果公示', path: '/evaluation/results', icon: 'el-icon-data-board' },
      { title: '综合测评小组名单', path: '/evaluation/group-members', icon: 'el-icon-user-solid' },
      { title: '综测查询', path: '/evaluation/view', icon: 'el-icon-search' }
    ]
  },
  {
    title: '个人信息管理',
    icon: 'el-icon-user',
    items: [
      { title: '修改密码', path: '/profile/change-password', icon: 'el-icon-lock' },
      { title: '修改个人信息', path: '/profile/edit', icon: 'el-icon-picture' },
      { title: '查看个人信息', path: '/profile/view', icon: 'el-icon-user' }
    ]
  },
  {
    title: '综测相关工作',
    icon: 'el-icon-s-cooperation',
    roles: ['group_member', 'group_leader'],
    items: [
      { 
        title: '综合测评材料审核', 
        path: '/evaluation/review-materials', 
        icon: 'el-icon-document-checked',
        roles: ['group_member']  // 只允许综测小组成员访问
      },
      { 
        title: '综合测评细则发布', 
        path: '/evaluation/upload-rules', 
        icon: 'el-icon-document',
        roles: ['group_leader']
      },
      { 
        title: '综合测评表格审核', 
        path: '/evaluation/review-evaluation-forms', 
        icon: 'el-icon-view',
        roles: ['group_leader']
      },
      { 
        title: '综测小组管理', 
        path: '/evaluation/group-members-manage', 
        icon: 'el-icon-s-tools',
        roles: ['group_leader']
      },
      { 
        title: '加分规则管理', 
        path: '/evaluation/bonus-rule-manage', 
        icon: 'el-icon-view',
        roles: ['group_leader']
      },
      { 
        title: '负责人疑问材料审核', 
        path: '/evaluation/review-question-materials', 
        icon: 'el-icon-document-checked',
        roles: ['group_leader']
      },
      { 
        title: '综测启动配置', 
        path: '/evaluation/start-config', 
        icon: 'el-icon-setting',
        roles: ['group_leader']
      }
    ]
  },
  {
    title: '系统相关',
    icon: 'el-icon-setting',
    items: [
      { title: '收件箱', path: '/system/inbox', icon: 'el-icon-bell' },
      { title: '用户帮助引导', path: '/system/guide', icon: 'el-icon-question' },
      { title: '意见与建议提出', path: '/system/feedback', icon: 'el-icon-chat-line-round' }
    ]
  },
  {
    title: '管理员功能模块',
    icon: 'el-icon-s-tools',
    roles: ['admin'],
    items: [
      { title: '权限管理', path: '/admin/permissions', icon: 'el-icon-key' },
      { title: '用户信息导入', path: '/admin/user-import', icon: 'el-icon-upload' },
      { title: '系统参数配置', path: '/admin/system-config', icon: 'el-icon-setting' },
    ]
  },
  {
    title: '导员功能模块',
    icon: 'el-icon-s-custom',
    roles: ['instructor'],
    items: [
      { title: '综测结果查看', path: '/evaluation/instructor-view', icon: 'el-icon-view' },
      { title: '导员疑问材料审核', path: '/evaluation/instructor-review-question-materials', icon: 'el-icon-document-checked' },
      { title: '综测小组选定', path: '/evaluation/group-select', icon: 'el-icon-user-solid' },
      { title: '中队权限管理', path: '/instructor/permissions', icon: 'el-icon-s-data' },
      { title: '扣分材料上传', path: '/instructor/penalty-upload', icon: 'el-icon-upload' },
      { title: '中队干部管理', path: '/instructor/squad-cadre-manage', icon: 'el-icon-s-data' }
    ]
  }
]);

// 记录每个分类的展开状态
const expandedCategories = ref({});

// 过滤菜单项
const filteredMenuItems = computed(() => {
  const userRole = userStore.roleLevel;
  return menuItems.value.filter(category => {
    // 管理员可以访问管理员模块、系统模块和个人信息管理模块
    if (userRole === 4) {
      return category.roles?.includes('admin') || 
             category.title === '系统相关' || 
             category.title === '个人信息管理';
    }
    
    // 导员只能访问导员模块
    if (userRole === 3) {
      return category.roles?.includes('instructor') ||
             category.title === '系统相关' || 
             category.title === '个人信息管理';
    }
    
    // 过滤综测相关工作模块的子项
    if (category.title === '综测相关工作') {
      category.items = category.items.filter(item => {
        if (userRole === 1) { // 综测小组成员
          return item.roles?.includes('group_member');
        }
        if (userRole === 2) { // 综测小组负责人
          return item.roles?.includes('group_leader');
        }
        return false;
      });
      return category.items.length > 0;
    }
    
    // 普通学生、综测小组成员和综测负责人可以访问的基础模块
    if (!category.roles) {
      return userRole === 0 || userRole === 1 || userRole === 2;
    }
    
    return false;
  });
});

// 跳转页面
const navigate = (path) => {
  router.push(path);
};

// 切换分类的展开/折叠状态
onMounted(() => {
  const storedState = sessionStorage.getItem("expandedCategories");
  if (storedState) {
    expandedCategories.value = JSON.parse(storedState);
  }
});
const toggleCategory = (index) => {
  expandedCategories.value[index] = !expandedCategories.value[index];
  sessionStorage.setItem("expandedCategories", JSON.stringify(expandedCategories.value));
};

// 判断是否是当前路由
const isActive = (path) => {
  return route.path === path;
};
</script>

<style scoped>
.sidebar {
  width: 220px;
  background: #599ede;
  padding: 20px;
  border-right: 1px solid #599ede;
  color: white;
  font-family: "KaiTi", "楷体", "STKaiti", "Microsoft YaHei", sans-serif;
  height: 100%;
  overflow-y: auto;
}

.sidebar ul {
  list-style: none;
  padding: 0;
}

.sidebar > ul > li {
  margin-bottom: 10px;
}

/* 分类标题 */
.category-title {
  font-weight: bold;
  font-size: 30px;
  padding: 8px 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: white;
  user-select: none;
}


/* 箭头图标 */
.arrow {
  transition: transform 0.3s ease;
}

/* 展开状态 */
.arrow.open {
  transform: rotate(180deg);
}

/* 子菜单 */
.submenu {
  padding-left: 10px;
  transition: max-height 0.3s ease-out;
}

.submenu li {
  padding: 8px;
  cursor: pointer;
  border-radius: 5px;
  font-size: 20px;
}

.submenu li:hover,
.submenu li.active {
  background: #007bff;
  color: #fff;
}

/* 活动状态下的菜单项 */
.category-title.active,
.submenu li.active {
  background-color: #007bff;
  color: white;
  border-radius: 10px; /* 圆角矩形 */
  padding: 8px 15px;   /* 增加内边距使圆角矩形更明显 */
}

/* 子菜单的高亮状态 */
.submenu li.active {
  background-color: #007bff;
  color: white;
  border-radius: 10px; /* 圆角矩形 */
}

/* 自定义滚动条样式 */
.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

</style>
