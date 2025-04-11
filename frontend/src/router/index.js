import {createRouter, createWebHistory} from 'vue-router'
import LoginView from '../views/login/LoginView.vue'
import RegisterView from '../views/login/RegisterView.vue'
import Help from "@/views/login/Help.vue";
import ForgotPassword from "@/views/login/ForgotPassword.vue";
import Feedback from "@/views/login/Feedback.vue";
import Updates from "@/views/login/Updates.vue";
import Home from "@/views/main/Home.vue";
import ProfileView from "@/views/main/ProfileView.vue";
import ProfileEdit from "@/views/main/ProfileEdit.vue";
import ChangePassword from "@/views/main/ChangePassword.vue";
import Guide from "@/views/main/Guide.vue";
import SystemFeedback from "@/views/main/SystemFeedback.vue";
import UploadEvaluation from '@/views/main/UploadEvaluation.vue';
import ViewEvaluation from '@/views/main/ViewEvaluation.vue';
import EvaluationRules from '@/views/main/EvaluationRules.vue';
import GroupMembers from '@/views/main/GroupMembers.vue';
import Inbox from '@/views/main/Inbox.vue';
import ReviewMaterials from '@/views/main/ReviewMaterials.vue';
import EvaluationResults from '@/views/main/EvaluationResults.vue'
import GroupMembersManage from '@/views/main/GroupMembersManage.vue'
import UploadRules from '@/views/main/UploadRules.vue'
import ReviewQuestionMaterials from '@/views/main/ReviewQuestionMaterials.vue'
import ReviewEvaluationForms from '@/views/main/ReviewEvaluationForms.vue'
import InstructorReviewQuestionMaterials from '@/views/main/InstructorReviewQuestionMaterials.vue'
import GroupMemberSelect from '@/views/main/GroupMemberSelect.vue'
import InstructorViewResults from '@/views/main/InstructorViewResults.vue'
import { useUserStore } from '@/stores/user'
import PenaltyUpload from '@/views/instructor/PenaltyUpload.vue'

const routes = [
    {path: '/', redirect: '/home'},
    {path: '/login', component: LoginView, meta: { requiresGuest: true }},
    {path: '/register', component: RegisterView, meta: { requiresGuest: true }},
    {path: '/help', component: Help},
    {path: '/forgot-password', component: ForgotPassword},
    {path: '/feedback', component: Feedback},
    {path: '/updates', component: Updates},
    {path: '/home', component: Home, meta: { requiresAuth: true }},
    {path: '/profile/view', component: ProfileView},
    {path: '/profile/edit', component: ProfileEdit},
    {path: '/profile/change-password', component: ChangePassword},
    {path: '/system/guide', component: Guide} ,
    {path: "/system/feedback", component: SystemFeedback},
    {path: "/evaluation/upload", component: UploadEvaluation},
    {path: "/evaluation/view", component: ViewEvaluation, name: 'ViewEvaluation', meta: { requiresAuth: true }},
    {path: "/evaluation/rules", component: EvaluationRules, name: 'EvaluationRules', meta: { requiresAuth: true }},
    {path: "/evaluation/group-members", component: GroupMembers, name: 'GroupMembers', meta: { requiresAuth: true }},
    {path: "/system/inbox", component: Inbox, name: 'Inbox', meta: { requiresAuth: true }},
    {path: "/evaluation/review-materials", component: ReviewMaterials, name: 'ReviewMaterials', meta: { requiresAuth: true }},
    {path: '/evaluation/results',
        name: 'EvaluationResults',
        component: EvaluationResults,
        meta: {
            requiresAuth: true,
            title: '综测成绩公示'
        }
    },
    {
        path: '/evaluation/group-members-manage',
        name: 'GroupMembersManage',
        component: GroupMembersManage,
        meta: {
            requiresAuth: true,
            title: '综测小组成员管理'
        }
    },
    {
        path: '/evaluation/upload-rules',
        name: 'UploadRules',
        component: UploadRules,
        meta: {
            requiresAuth: true,
            title: '综测规章上传'
        }
    },
    {
        path: '/evaluation/review-question-materials',
        name: 'ReviewQuestionMaterials',
        component: () => import('@/views/main/ReviewQuestionMaterials.vue'),
        meta: {
            requiresAuth: true,
            roles: ['instructor']
        }
    },
    {
        path: '/evaluation/review-evaluation-forms',
        name: 'ReviewEvaluationForms',
        component: ReviewEvaluationForms,
        meta: {
            requiresAuth: true,
            title: '综测结果审核'
        }
    },
    {
        path: '/evaluation/bonus-rule-manage',
        name: 'BonusRuleManage',
        component: () => import('@/views/main/BonusRuleManage.vue'),
        meta: {
            title: '加分规则管理',
            requiresAuth: true,
            roles: ['admin', 'group_leader']
        }
    },
    {
        path: '/evaluation/instructor-review-question-materials',
        name: 'InstructorReviewQuestionMaterials',
        component: InstructorReviewQuestionMaterials,
        meta: {
            title: '导员疑问材料审核',
            requiresAuth: true,
            roles: ['instructor']
        }
    },
    {
        path: '/evaluation/instructor-view',
        name: 'InstructorViewResults',
        component: () => import('@/views/main/InstructorViewResults.vue'),
        meta: {
            requiresAuth: true,
            title: '综测结果查看'
        }
    },
    {
        path: '/evaluation/group-select',
        name: 'GroupMemberSelect',
        component: () => import('@/views/main/GroupMemberSelect.vue'),
        meta: {
          requiresAuth: true,
          title: '综测小组成员选定'
        }
      },
    {
        path: '/admin/permissions',
        name: 'UserPermissionManage',
        component: () => import('@/views/admin/UserPermissionManage.vue'),
        meta: {
            requiresAuth: true,
            requiresAdmin: true,
            title: '用户权限管理'
        }
    },
    {
        path: '/instructor/permissions',
        name: 'InstructorPermissionManage',
        component: () => import('@/views/instructor/UserPermissionManage.vue'),
        meta: {
          requiresAuth: true,
          requiresInstructor: true,
          title: '班级权限管理'
        }
      },
    {
        path: '/admin/system-config',
        name: 'SystemConfig',
        component: () => import('@/views/admin/SystemConfig.vue'),
        meta: {
            requiresAuth: true,
            requiresAdmin: true,
            title: '系统参数配置'
        }
    },
    {
        path: '/evaluation/start-config',
        name: 'EvaluationStartConfig',
        component: () => import('@/views/main/EvaluationStartConfig.vue'),
        meta: {
            requiresAuth: true,
            requiresAdmin: true,
            title: '综测启动配置'
        }
    },
    {
        path: '/admin/user-import',
        name: 'UserImportManage',
        component: () => import('@/views/admin/UserImportManage.vue'),
        meta: {
          requiresAuth: true,
          requiresAdmin: true,
          title: '用户信息导入'
        }
      },
      {
        path: '/instructor/penalty-upload',
        name: 'PenaltyUpload',
        component: () => import('@/views/instructor/PenaltyUpload.vue'),
        meta: {
          requiresAuth: true,
          requiresInstructor: true,
          title: '扣分材料上传'
        }
      },
      {
        path: '/instructor/squad-cadre-manage',
        name: 'SquadCadreManage',
        component: () => import('@/views/instructor/SquadCadreManage.vue'),
        meta: {
          requiresAuth: true,
          requiresInstructor: true,
          title: '中队干部管理'
        }
      },
      {
        path: '/instructor/score-upload',
        name: 'ScoreUpload',
        component: () => import('@/views/instructor/ScoreUpload.vue'),
        meta: {
          requiresAuth: true,
          requiresInstructor: true,
          title: '成绩表格上传'
        }
      }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isAuthenticated = !!userStore.token

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.meta.requiresGuest && isAuthenticated) {
    next('/home')
  } else {
    next()
  }
})

export default router