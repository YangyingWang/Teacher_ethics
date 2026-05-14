<script lang="ts" setup>
import { ArrowRight, Search, MoreFilled, View, Refresh, Document, Warning, CaretTop, CaretBottom,
  Bell, CircleCheck, InfoFilled, WarningFilled} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

import { ref, onMounted, onBeforeUnmount, watch, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'

import useUserInfoStore from '@/stores/userInfo.js'
const userInfoStore = useUserInfoStore()
const router = useRouter()
const toUserInfo = () => {
  setTimeout(() => {
    router.push({ path: '/user/info' })
  }, 500)
}

const imgList = ref([
  { 
    src: 'src/assets/home/1.png',
    title: '师德师风建设',
    description: '加强教师职业道德素养'
  },{ 
    src: 'src/assets/home/2.png',
    title: '课程思政',
    description: '将思政元素融入课堂教学'
  },{ 
    src: 'src/assets/home/3.png',
    title: '学术诚信',
    description: '坚守科研道德底线'
  },{ 
    src: 'src/assets/home/4.png',
    title: '教师发展',
    description: '持续提升专业能力与道德修养'
  }
])

// 推荐课程数据
const recommendedCourses = ref([
  {
    id: 1,
    title: '科研诚信：如何避免学术不端行为',
    views: 3463000,
    growth: 35,
    tag: '热门',
    tagType: 'danger'
  },
  {
    id: 2,
    title: '家国情怀与高校教师使命担当',
    views: 3242000,
    growth: 22,
    tag: '推荐',
    tagType: 'success'
  },
  {
    id: 3,
    title: '新时代师德师风建设的实践路径',
    views: 3189000,
    growth: -5,
    tag: '必修',
    tagType: 'warning'
  },
  {
    id: 4,
    title: '从课堂到社会：教师的道德影响力',
    views: 2579000,
    growth: 17,
    tag: '选修',
    tagType: ''
  },
  {
    id: 5,
    title: '教育公平视角下的师德反思',
    views: 1242000,
    growth: 37,
    tag: '新课程',
    tagType: 'info'
  }
])

// 通知公告数据
const notices = ref([
  {
    id: 1,
    title: '关于实施《新时代高校教师职业行为十项准则》的通知',
    time: '2024-03-15',
    department: '党委教师工作部',
    type: 'policy',
    icon: Bell,
    priority: 'high',
    read: false
  },
  {
    id: 2,
    title: '师德师风教育管理系统操作指南（V1.0）',
    time: '2024-03-10',
    department: '信息中心',
    type: 'system',
    icon: InfoFilled,
    priority: 'normal',
    read: true
  },
  {
    id: 3,
    title: '师德失范典型案例警示教育',
    time: '2024-03-05',
    department: '纪委办公室',
    type: 'warning',
    icon: WarningFilled,
    priority: 'high',
    read: false
  },
  {
    id: 4,
    title: '重要提醒：2024年师德考核材料提交截止时间为4月30日',
    time: '2024-03-01',
    department: '人事处',
    type: 'reminder',
    icon: CircleCheck,
    priority: 'normal',
    read: true
  }
])

// 最近动态数据
const activities = ref([
  {
    content: '注册并登入系统',
    timestamp: '刚刚',
    icon: MoreFilled
  },
  {
    content: '完成了《科研诚信与学术规范》课程学习',
    timestamp: '30分钟前',
    icon: CircleCheck
  },
  {
    content: '完成了"师生关系边界"情景测试',
    timestamp: '2小时前',
    icon: Document
  },
  {
    content: '查看个人师德能力评估报告',
    timestamp: '昨天',
    icon: View
  },
  {
    content: '完成"网络舆情应对"数字沙盘演练',
    timestamp: '2天前',
    icon: Warning
  }
])

// 图表相关
const progressChartRef = ref<HTMLElement>()
const radarChartRef = ref<HTMLElement>()
let progressChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null
const compareMode = ref('personal')

// 学习进度数据
const learningStats = ref({
  completed: 15,
  total: 25,
  hours: 36,
  completionRate: 60
})

const learningProgress = ref([
  { 
    name: '科研诚信专题', 
    value: 85, 
    completed: 17, 
    total: 20,
    color: '#36c',
    link: '/learning/scientific-integrity'
  },
  { 
    name: '课程思政元素', 
    value: 70, 
    completed: 7, 
    total: 10,
    color: '#34d399',
    link: '/learning/ideological-elements'
  },
  { 
    name: '师德法规政策', 
    value: 45, 
    completed: 9, 
    total: 20,
    color: '#f59e0b',
    link: '/learning/regulations'
  },
  { 
    name: '教师职业伦理', 
    value: 90, 
    completed: 18, 
    total: 20,
    color: '#8b5cf6',
    link: '/learning/ethics'
  }
])

// 能力评估数据
const abilityStats = ref({
  overallScore: 82,
  ranking: '良好',
  percentile: 25,
  lastUpdate: '3天前'
})

const abilityData = ref({
  dimensions: ['道德认知', '情景应对', '课堂融入', '学术诚信', '社会责任', '师生关系'],
  current: [
    { name: '道德认知', value: 85 },
    { name: '情景应对', value: 78 },
    { name: '课堂融入', value: 92 },
    { name: '学术诚信', value: 80 },
    { name: '社会责任', value: 75 },
    { name: '师生关系', value: 82 }
  ],
  average: [
    { name: '道德认知', value: 80 },
    { name: '情景应对', value: 75 },
    { name: '课堂融入', value: 85 },
    { name: '学术诚信', value: 78 },
    { name: '社会责任', value: 70 },
    { name: '师生关系', value: 76 }
  ],
  department: [
    { name: '道德认知', value: 82 },
    { name: '情景应对', value: 79 },
    { name: '课堂融入', value: 88 },
    { name: '学术诚信', value: 81 },
    { name: '社会责任', value: 77 },
    { name: '师生关系', value: 80 }
  ]
})

// 计算属性
const unreadCount = computed(() => {
  return notices.value.filter(notice => !notice.read).length
})

// 工具函数
const formatViews = (num: number) => {
  if (num >= 1000000) {
    return `${(num / 1000000).toFixed(1)}w+`
  } else if (num >= 10000) {
    return `${Math.floor(num / 10000)}万`
  }
  return num.toString()
}

const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#34d399'
  if (percentage >= 60) return '#3b82f6'
  if (percentage >= 40) return '#f59e0b'
  return '#ef4444'
}

const getScoreClass = (score: number) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 70) return 'score-average'
  return 'score-poor'
}

const getScoreLevel = (score: number) => {
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 70) return '中等'
  return '待提升'
}

// 表格行样式
const tableRowClassName = ({ rowIndex }: { rowIndex: number }) => {
  if (rowIndex === 0) return 'first-row'
  if (rowIndex === 1) return 'second-row'
  if (rowIndex === 2) return 'third-row'
  return ''
}

// 图表初始化
const initCharts = () => {
  if (!progressChartRef.value || !radarChartRef.value) return
  
  // 初始化学习进度环形图
  progressChart = echarts.init(progressChartRef.value)
  const progressOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}%'
    },
    series: [
      {
        name: '学习进度',
        type: 'pie',
        radius: ['70%', '85%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
            formatter: '{c}%',
            color: '#333'
          }
        },
        labelLine: {
          show: false
        },
        data: learningProgress.value.map(item => ({
          value: item.value,
          name: item.name,
          itemStyle: { color: item.color }
        }))
      },
      {
        name: '背景环',
        type: 'pie',
        radius: ['65%', '90%'],
        center: ['50%', '50%'],
        silent: true,
        itemStyle: {
          color: '#f0f2f5',
          borderWidth: 0
        },
        label: {
          show: false
        },
        data: [{ value: 100 }]
      }
    ]
  }
  progressChart.setOption(progressOption)

  // 初始化能力雷达图
  radarChart = echarts.init(radarChartRef.value)
  updateRadarChart()

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
}

// 更新雷达图
const updateRadarChart = () => {
  let compareData = []
  
  switch (compareMode.value) {
    case 'personal':
      compareData = [abilityData.value.current]
      break
    case 'department':
      compareData = [abilityData.value.current, abilityData.value.department]
      break
    case 'university':
      compareData = [abilityData.value.current, abilityData.value.average]
      break
  }
  
  const radarOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ebeef5',
      borderWidth: 1,
      textStyle: {
        color: '#606266'
      }
    },
    legend: {
      data: compareMode.value === 'personal' 
        ? ['个人能力'] 
        : ['个人能力', compareMode.value === 'department' ? '院系平均' : '全校平均'],
      bottom: 10,
      textStyle: {
        fontSize: 12,
        color: '#909399'
      }
    },
    radar: {
      indicator: abilityData.value.dimensions.map((dim, index) => ({
        name: dim,
        max: 100,
        color: '#999'
      })),
      center: ['50%', '52%'],
      radius: '65%',
      axisName: {
        color: '#666',
        fontSize: 12
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(240, 242, 245, 0.6)', 'rgba(240, 242, 245, 0.2)']
        }
      },
      splitLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: compareData.map((data, index) => ({
          value: data.map(item => item.value),
          name: index === 0 ? '个人能力' : (compareMode.value === 'department' ? '院系平均' : '全校平均'),
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: {
            width: 2,
            type: index === 0 ? 'solid' : 'dashed'
          },
          areaStyle: {
            opacity: index === 0 ? 0.3 : 0.1
          },
          itemStyle: {
            color: index === 0 ? '#409eff' : (compareMode.value === 'department' ? '#67c23a' : '#e6a23c')
          }
        }))
      }
    ]
  }
  
  if (radarChart) {
    radarChart.setOption(radarOption)
  }
}

// 处理窗口大小变化
const handleResize = () => {
  if (progressChart) {
    progressChart.resize()
  }
  if (radarChart) {
    radarChart.resize()
  }
}

// 交互函数
const refreshChart = (type: string) => {
  if (type === 'progress') {
    // 模拟刷新数据
    learningStats.value = {
      completed: Math.min(25, learningStats.value.completed + 1),
      total: 25,
      hours: learningStats.value.hours + 1,
      completionRate: Math.round((learningStats.value.completed + 1) / 25 * 100)
    }
    
    ElMessage.success('学习进度已更新')
  }
}

const viewLearningReport = () => {
  router.push('/report/learning')
}

const generateAssessmentReport = () => {
  ElMessage.success('评估报告生成中...')
  // 实际开发中这里会调用API生成报告
}

const continueLearning = (item: any) => {
  router.push(item.link)
}

const viewCourseDetail = (course: any) => {
  router.push(`/course/${course.id}`)
}

const startLearning = (course: any) => {
  ElMessage.info(`开始学习: ${course.title}`)
  // 实际开发中这里会跳转到课程学习页面
}

const searchCourseIdeology = () => {
  router.push('/search/ideology')
}

const viewAllCourses = () => {
  router.push('/course')
}

const viewAllNotices = () => {
  router.push('/notice')
}

const viewAllActivities = () => {
  router.push('/activity')
}

const viewNoticeDetail = (notice: any) => {
  notice.read = true
  ElMessage.info(`查看通知: ${notice.title}`)
  // 实际开发中这里会跳转到通知详情页
}

// 生命周期
onMounted(() => {
  nextTick(() => {
    initCharts()
  })
})

onBeforeUnmount(() => {
  if (progressChart) {
    progressChart.dispose()
  }
  if (radarChart) {
    radarChart.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

// 监听对比模式变化
watch(compareMode, () => {
  updateRadarChart()
})
</script>


<template>
  <div class="home-container">
    <el-card class="welcome-card" shadow="never">
      <template #header>
        <div class="welcome-header">
          <div class="welcome-title">
            <h2>欢迎回来！{{ userInfoStore.info.realName }}</h2>
            <p class="welcome-subtitle">师德师风教育管理系统，助力您成为更好的教育工作者</p>
          </div>
          <el-button type="primary" plain round @click="toUserInfo">
            进入个人中心<el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>

      <div class="carousel-container">
        <el-carousel height="300px" motion-blur>
          <el-carousel-item v-for="(img, index) in imgList" :key="index">
            <img :src="img.src" :alt="img.title" class="carousel-image" />
            <div class="carousel-overlay" v-if="img.title">
              <h3>{{ img.title }}</h3>
              <p>{{ img.description }}</p>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </el-card>

    <el-row :gutter="20" class="main-content">
      <!-- 左侧内容区 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="17" :xl="17" class="left-column">
        <el-card class="course-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>今日课程推荐</h3>
              <el-button type="text" @click="viewAllCourses">查看更多</el-button>
            </div>
          </template>
          <div class="course-table-container">
            <el-table 
              :data="recommendedCourses" 
              :show-header="true" 
              style="width: 100%" 
              class="course-table"
              :row-class-name="tableRowClassName"
            >
              <el-table-column label="排名" width="80" align="center">
                <template #default="{ $index }">
                  <span class="rank" :class="`rank-${$index + 1}`">{{ $index + 1 }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="title" label="课程标题" min-width="200">
                <template #default="{ row }">
                  <div class="course-title" @click="viewCourseDetail(row)">
                    <span class="title-text">{{ row.title }}</span>
                    <el-tag v-if="row.tag" size="small" :type="row.tagType">{{ row.tag }}</el-tag>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="观看量" width="100" align="center">
                <template #default="{ row }">
                  <div class="views-count">
                    <el-icon><View /></el-icon>
                    <span>{{ formatViews(row.views) }}</span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="热度" width="120" align="center">
                <template #default="{ row }">
                  <div class="growth-indicator">
                    <el-icon :class="row.growth >= 0 ? 'growth-up' : 'growth-down'">
                      <CaretTop v-if="row.growth >= 0" />
                      <CaretBottom v-else />
                    </el-icon>
                    <span :class="row.growth >= 0 ? 'positive' : 'negative'">
                      {{ row.growth >= 0 ? '+' : '' }}{{ row.growth }}%
                    </span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="80" align="center">
                <template #default="{ row }">
                  <el-button type="text" size="small" @click="startLearning(row)">学习</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 图表区域 -->
        <el-row :gutter="20" class="chart-row">
          <el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="never">
              <template #header>
                <div class="chart-header">
                  <h3>学习进度</h3>
                  <div class="chart-actions">
                    <el-tooltip content="查看详细报告">
                      <el-button type="text" @click="viewLearningReport">
                        <el-icon><Document /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="刷新数据">
                      <el-button type="text" @click="refreshChart('progress')">
                        <el-icon><Refresh /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                </div>
              </template>
              <div class="chart-content">
                <div class="progress-chart" ref="progressChartRef"></div>
                <div class="progress-stats">
                  <div class="stat-item">
                    <div class="stat-value">{{ learningStats.completed }}/{{ learningStats.total }}</div>
                    <div class="stat-label">课程完成</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-value">{{ learningStats.hours }}h</div>
                    <div class="stat-label">学习时长</div>
                  </div>
                  <div class="stat-item">
                    <div class="stat-value">{{ learningStats.completionRate }}%</div>
                    <div class="stat-label">完成率</div>
                  </div>
                </div>
              </div>
              <div class="progress-details">
                <div class="detail-item" v-for="item in learningProgress" :key="item.name">
                  <div class="detail-header">
                    <span class="detail-name">{{ item.name }}</span>
                    <span class="detail-percent">{{ item.value }}%</span>
                  </div>
                  <el-progress 
                    :percentage="item.value" 
                    :stroke-width="6" 
                    :color="getProgressColor(item.value)"
                    :show-text="false"
                  />
                  <div class="detail-info">
                    <span class="detail-sub">{{ item.completed }}/{{ item.total }}课时</span>
                    <el-button v-if="item.value < 100" type="text" size="small" @click="continueLearning(item)">
                      继续学习
                    </el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="12" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="never">
              <template #header>
                <div class="chart-header">
                  <h3>能力评估</h3>
                  <div class="chart-actions">
                    <el-select v-model="compareMode" size="small" style="width: 120px">
                      <el-option label="个人评估" value="personal" />
                      <el-option label="院系对比" value="department" />
                      <el-option label="全校对比" value="university" />
                    </el-select>
                    <el-tooltip content="生成报告">
                      <el-button type="text" @click="generateAssessmentReport">
                        <el-icon><Document /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                </div>
              </template>
              <div class="chart-content">
                <div class="radar-chart" ref="radarChartRef"></div>
              </div>
              <div class="assessment-summary">
                <div class="summary-item">
                  <div class="summary-label">综合得分</div>
                  <div class="summary-value">{{ abilityStats.overallScore }}</div>
                  <div class="summary-tag" :class="getScoreClass(abilityStats.overallScore)">
                    {{ getScoreLevel(abilityStats.overallScore) }}
                  </div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">能力排名</div>
                  <div class="summary-value">{{ abilityStats.ranking }}</div>
                  <div class="summary-sub">前{{ abilityStats.percentile }}%</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>

      <!-- 右侧侧边栏 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="7" :xl="7" class="right-column">
        <!-- 快速检索 -->
        <el-card class="quick-search-card" shadow="never">
          <el-button type="primary" plain round @click="searchCourseIdeology" style="width: 100%;">
            <el-icon><Search /></el-icon>
            课程思政元素检索
          </el-button>
        </el-card>

        <!-- 通知公告 -->
        <el-card class="notice-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>通知公告</h3>
              <el-badge :value="unreadCount" :max="99" v-if="unreadCount > 0">
                <el-button type="text" @click="viewAllNotices">查看全部</el-button>
              </el-badge>
            </div>
          </template>
          <div class="notice-list">
            <div class="notice-item" 
                 v-for="notice in notices" 
                 :key="notice.id"
                 :class="['notice-type-' + notice.type, { 'unread': !notice.read }]"
                 @click="viewNoticeDetail(notice)">
              <div class="notice-icon">
                <el-icon :size="16">
                  <component :is="notice.icon" />
                </el-icon>
              </div>
              <div class="notice-content">
                <div class="notice-title">{{ notice.title }}</div>
                <div class="notice-meta">
                  <span class="notice-time">{{ notice.time }}</span>
                  <span class="notice-department">{{ notice.department }}</span>
                </div>
              </div>
              <el-tag v-if="notice.priority === 'high'" size="small" effect="dark" type="danger">紧急</el-tag>
            </div>
          </div>
        </el-card>

        <!-- 最近动态 -->
        <el-card class="activity-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>最近动态</h3>
              <el-button type="text" @click="viewAllActivities">查看更多</el-button>
            </div>
          </template>
          <div class="activity-list">
            <div class="activity-item" v-for="(activity, index) in activities" :key="index">
              <div class="activity-time">{{ activity.timestamp }}</div>
              <div class="activity-content">
                <div class="activity-icon">
                  <el-icon :size="14">
                    <component :is="activity.icon || 'MoreFilled'" />
                  </el-icon>
                </div>
                <div class="activity-text">{{ activity.content }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.home-container {
  min-height: calc(100vh - 64px);
  
  .el-card {
    border-radius: 5px;
    margin-bottom: 15px;
    border: 1px solid #ebeef5;
    box-shadow: none; // 保持扁平

    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;
    }
    
    &:deep(.el-card__body) {
      padding: 10px 20px;
    }
  }
}

.welcome-card {
  .welcome-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .welcome-title {
      h2 {
        margin: 0;
        font-size: 24px;
        color: #303133;
        font-weight: 600;
      }
      
      .welcome-subtitle {
        margin: 5px 0 0;
        color: #909399;
        font-size: 14px;
      }
    }
    
    .el-button {
      padding: 10px 20px;
    }
  }
  
  .carousel-container {
    position: relative;
    
    .carousel-image {
      width: 100%;
      height: 300px;
      object-fit: cover;
      border-radius: 4px;
    }
    
    .carousel-overlay {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      padding: 20px;
      background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
      color: white;
      border-radius: 0 0 4px 4px;
      
      h3 {
        margin: 0 0 8px;
        font-size: 18px;
      }
      
      p {
        margin: 0;
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }
}

.main-content {
  .left-column {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 16px;
        color: #303133;
        font-weight: 600;
      }
    }
  }
  
  .right-column {
    .quick-search-card {
      margin-bottom: 20px;
      
      .el-button {
        padding: 12px 0;
        font-size: 16px;
      }
    }
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 16px;
        color: #303133;
        font-weight: 600;
      }
    }
  }
}

.course-table-container {
  .course-table {
    &:deep(.el-table__header) {
      th {
        background-color: #f8f9fa;
        color: #606266;
        font-weight: 600;
      }
    }
    
    &:deep(.el-table__row) {
      &.first-row {
        .rank-1 {
          background-color: #ff4d4f;
          color: white;
        }
      }
      
      &.second-row {
        .rank-2 {
          background-color: #fa8c16;
          color: white;
        }
      }
      
      &.third-row {
        .rank-3 {
          background-color: #fadb14;
          color: white;
        }
      }
    }
  }

  .rank {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background-color: #f5f5f5;
  color: #999;
  font-weight: bold;
  }
  
  .course-title {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    
    .title-text {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      
      &:hover {
        color: #409eff;
      }
    }
    
    .el-tag {
      flex-shrink: 0;
    }
  }
  
  .views-count {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #606266;
    
    .el-icon {
      color: #909399;
    }
  }
  
  .growth-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    
    .el-icon {
      &.growth-up {
        color: #f56c6c;
      }
      
      &.growth-down {
        color: #67c23a;
      }
    }
    
    .positive {
      color: #f56c6c;
    }
    
    .negative {
      color: #67c23a;
    }
  }
}

.chart-row {
  .chart-card {
    height: 100%;
    
    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 16px;
        color: #303133;
        font-weight: 600;
      }
      
      .chart-actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
    
    .chart-content {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 200px;
      
      .progress-chart {
        width: 150px;
        height: 150px;
      }
      
      .radar-chart {
        width: 100%;
        height: 200px;
      }
      
      .progress-stats {
        margin-left: 20px;
        display: flex;
        flex-direction: column;
        gap: 20px;
        
        .stat-item {
          text-align: center;
          
          .stat-value {
            font-size: 20px;
            font-weight: bold;
            color: #409eff;
            margin-bottom: 4px;
          }
          
          .stat-label {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
    
    .progress-details {
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid #ebeef5;
      
      .detail-item {
        margin-bottom: 10px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .detail-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          
          .detail-name {
            font-size: 14px;
            color: #606266;
          }
          
          .detail-percent {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
          }
        }
        
        .detail-info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 5px;
          
          .detail-sub {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
    
    .assessment-summary {
      margin-top: 15px;
      padding: 15px;
      background: #f8f9fa;
      border-radius: 6px;
      display: flex;
      justify-content: space-around;
      
      .summary-item {
        text-align: center;
        
        .summary-label {
          font-size: 12px;
          color: #909399;
          margin-bottom: 5px;
        }
        
        .summary-value {
          font-size: 20px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 5px;
        }
        
        .summary-tag {
          display: inline-block;
          padding: 2px 8px;
          border-radius: 10px;
          font-size: 12px;
          color: white;
          
          &.score-excellent {
            background: #67c23a;
          }
          
          &.score-good {
            background: #409eff;
          }
          
          &.score-average {
            background: #e6a23c;
          }
          
          &.score-poor {
            background: #f56c6c;
          }
        }
        
        .summary-sub {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

.notice-card {
  .notice-list {
    .notice-item {
      padding: 12px;
      margin-bottom: 10px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.3s;
      display: flex;
      align-items: flex-start;
      gap: 12px;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      &.unread {
        background-color: #f0f9ff;
        border-left: 3px solid #409eff;
      }
      
      &.notice-type-policy {
        border-left: 3px solid #67c23a;
      }
      
      &.notice-type-system {
        border-left: 3px solid #909399;
      }
      
      &.notice-type-warning {
        border-left: 3px solid #e6a23c;
      }
      
      &.notice-type-reminder {
        border-left: 3px solid #409eff;
      }
      
      .notice-icon {
        flex-shrink: 0;
        margin-top: 2px;
        
        .el-icon {
          color: #909399;
        }
      }
      
      .notice-content {
        flex: 1;
        
        .notice-title {
          font-size: 14px;
          color: #303133;
          margin-bottom: 4px;
          line-height: 1.4;
        }
        
        .notice-meta {
          display: flex;
          gap: 12px;
          font-size: 12px;
          color: #909399;
        }
      }
      
      .el-tag {
        flex-shrink: 0;
        align-self: flex-start;
      }
    }
  }
}

.activity-card {
  .activity-list {
    .activity-item {
      padding: 6px 0;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .activity-time {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }
      
      .activity-content {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        
        .activity-icon {
          flex-shrink: 0;
          margin-top: 2px;
          
          .el-icon {
            color: #409eff;
          }
        }
        
        .activity-text {
          flex: 1;
          font-size: 14px;
          color: #606266;
          line-height: 1.5;
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .main-content {
    .left-column,
    .right-column {
      width: 100%;
    }
  }
  
  .chart-row {
    .el-col {
      width: 100%;
      margin-bottom: 15px;
    }
  }
}

@media (max-width: 768px) {
  .home-container {
    padding: 10px;
  }
  
  .welcome-card {
    .welcome-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 15px;
      
      .el-button {
        align-self: stretch;
      }
    }
  }
  
  .course-table-container {
    overflow-x: auto;
  }
  
  .chart-content {
    flex-direction: column;
    
    .progress-stats {
      margin-left: 0;
      margin-top: 15px;
      flex-direction: row;
      width: 100%;
    }
  }
}
</style>