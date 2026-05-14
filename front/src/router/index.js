import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'

//导入组件
import LoginVue from '@/views/Login.vue'
import LayoutVue from '@/views/Layout.vue'
import ErrorVue from '@/views/Error.vue'
import HomeVue from '@/views/Home.vue'
import UserInfoVue from '@/views/user/UserInfo.vue'

import CourseHomeVue from '@/views/study/CourseHome.vue'
import CourseStudyVue from '@/views/study/CourseStudy.vue'
import SearchVue from '@/views/study/Search.vue'

import ChatAIVue from '@/views/simulate/ChatAI.vue'
import EvaluationHomeVue from '@/views/simulate/EvaluationHome.vue'
import EvaluationDetailVue from '@/views/simulate/EvaluationDetail.vue'

import TestHomeVue from '@/views/test/TestHome.vue'
import SceneDetailVue from '@/views/test/SceneDetail.vue'
import QuizVue from '@/views/test/quiz/Quiz.vue'
import QuizResultVue from '@/views/test/quiz/QuizResult.vue'
import QuizListVue from '@/views/test/quiz/QuizList.vue'
import ReportHomeVue from '@/views/test/report/ReportHome.vue'
import ReportListVue from '@/views/test/report/ReportList.vue'
import ReportDetailVue from '@/views/test/report/ReportDetail.vue'

import AssessmentHomeVue from '@/views/assessment/AssessmentHome.vue'

import AdminHomeVue from '@/views/admin/AdminHome.vue'
import TeacherManageVue from '@/views/admin/TeacherManage.vue'
import StatisticsAnalysisVue from '@/views/admin/StatisticsAnalysis.vue'

const routes = [
  { path: '/login', component: LoginVue, meta: { title: '登录' } },
  {
    path: '/', 
    component: LayoutVue,
    redirect:'/home', 
    children: [
      { path: '/home', component: HomeVue, meta: { title: '首页', role: ['teacher'] } },
      { path: '/user/info', component: UserInfoVue, meta: { title: '个人中心', role: ['teacher'] } },

      { path: '/study/course/home', component: CourseHomeVue, meta: { title: '课程学习', role: ['teacher'] } },
      { path: '/study/course/learning', component: CourseStudyVue, meta: { title: '课程学习', role: ['teacher'] } },
      { path: '/study/search/home', component: SearchVue, meta: { title: '思政检索', role: ['teacher'] } },

      { path: '/simulate/chat', component: ChatAIVue, meta: { title: '沙盘演练', role: ['teacher'] } },
      { path: '/evaluation/home', component: EvaluationHomeVue, meta: { title: '决策评估', role: ['teacher'] } },
      { path: '/evaluation/detail', component: EvaluationDetailVue, meta: { title: '评估详情', role: ['teacher'] } },

      { path: '/test/home', component: TestHomeVue, meta: { title: '能力提升', role: ['teacher'] } },
      { path: '/test/scene', component: SceneDetailVue, meta: { title: '情景详情', role: ['teacher'] } },
      { path: '/test/quiz', component: QuizVue, meta: { title: '情景测试', role: ['teacher'] } },
      { path: '/test/result', component: QuizResultVue, meta: { title: '测试结果', role: ['teacher'] } },
      { path: '/test/list', component: QuizListVue, meta: { title: '测试记录', role: ['teacher'] } },
      { path: '/report/home', component: ReportHomeVue, meta: { title: '能力诊断', role: ['teacher'] } },
      { path: '/report/list', component: ReportListVue, meta: { title: '报告记录', role: ['teacher'] } },
      { path: '/report/detail', component: ReportDetailVue, meta: { title: '报告详情', role: ['teacher'] } },

      { path: '/assessment/home', component: AssessmentHomeVue, meta: { title: '多维评估', role: ['teacher'] } },

      { path: '/admin/home', component: AdminHomeVue, meta: { title: '管理首页', role: ['admin'] } },
      { path: '/admin/teachers', component: TeacherManageVue, meta: { title: '教师信息管理', role: ['admin'] } },
      { path: '/admin/statistics', component: StatisticsAnalysisVue, meta: { title: '分类统计分析', role: ['admin'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', component: ErrorVue, meta: { title: '页面不存在' } }
]

//创建路由器
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const tokenStore = useTokenStore()
  const isLoginPage = to.path === '/login'
  const hasToken = !!tokenStore.token
  const role = tokenStore.role

  if (!isLoginPage && !hasToken) {
    next('/login')
    return
  }

  const allowRoles = to.meta.role
  if (allowRoles && (!role || !allowRoles.includes(role))) {
    ElMessage.warning('无权访问该页面')
    next(role === 'admin' ? '/admin/home' : '/home')
    return
  }

  document.title = to.meta.title ? `${to.meta.title} - 师德师风系统` : '师德师风教育管理系统'
  next()
})

// 全局后置守卫
router.afterEach((to, from) => {
  // 移除页面加载动画
  // 可以在这里添加页面访问统计
})

// 全局错误处理器
router.onError((error) => {
  console.error('路由错误:', error)
  
  // 根据错误类型进行不同处理
  if (error.message.includes('Failed to fetch dynamically imported module')) {
    // 模块加载失败
    ElMessage.error('页面资源加载失败，请刷新重试')
  } else if (error.message.includes('Navigation cancelled')) {
    // 导航被取消，通常是用户快速点击
    console.log('导航被用户取消')
  } else {
    // 其他路由错误
    ElMessage.error('页面跳转失败，请稍后重试')
  }
})

//导出路由
export default router
