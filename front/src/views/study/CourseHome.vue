<script setup>
import { Star, Document, Notebook, Folder, Search, StarFilled, Clock, User, ArrowRight, VideoPlay, RefreshRight, Collection,
  Delete, ArrowLeft, Calendar, Check, VideoCameraFilled, Reading, Edit, School, Flag, Setting, Trophy,
  Refresh, Medal, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  courseHomeService,
  courseStartService,
  courseReviewService,
  courseToggleFavoriteService,
  courseRemoveMyCourseService
} from '@/api/study.js'

const router = useRouter()

const activeTab = ref('recommended')
const searchKeyword = ref('')
const filterDifficulty = ref('all')
const filterCategory = ref('all')
const myCoursesFilter = ref('all')
const recordsFilter = ref('all')
const currentMonth = ref(formatMonthKey(new Date()))
const loading = ref(false)

const durationChartRef = ref(null)
const timeChartRef = ref(null)
let durationChart = null
let timeChart = null
let searchTimer = null

const stats = ref({
  completedCourses: 0,
  studyHours: 0,
  continuityDays: 0
})

const myCoursesStats = ref({
  total: 0,
  completed: 0,
  inProgress: 0,
  notStarted: 0,
  completionRate: 0
})

const calendarStats = ref({
  thisMonth: {
    totalHours: 0,
    completedCourses: 0
  }
})

const efficiencyStats = ref({
  focus: 0,
  completion: 0,
  comprehension: 0
})

const featuredCourses = ref([])
const popularCourses = ref([])
const myCourses = ref([])
const learningRecords = ref([])
const categories = ref([])
const durationDistribution = ref([])
const timeDistribution = ref([])
const calendarDayMap = ref({})

const categoryMeta = {
  1: { icon: Document, color: '#409eff' },
  2: { icon: Flag, color: '#67c23a' },
  3: { icon: School, color: '#e6a23c' },
  4: { icon: School, color: '#f56c6c' },
  5: { icon: Setting, color: '#909399' },
  6: { icon: Setting, color: '#409799' }
}

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const filteredMyCourses = computed(() => {
  if (myCoursesFilter.value === 'all') return myCourses.value
  return myCourses.value.filter(course => {
    if (myCoursesFilter.value === 'in-progress') return course.progress > 0 && course.progress < 100
    if (myCoursesFilter.value === 'completed') return course.progress === 100
    if (myCoursesFilter.value === 'not-started') return course.progress === 0
    return true
  })
})

const filteredLearningRecords = computed(() => {
  if (recordsFilter.value === 'all') return learningRecords.value
  return learningRecords.value.filter(record => record.type === recordsFilter.value)
})

const calendarDays = computed(() => {
  const days = []
  const date = new Date(currentMonth.value + '-01')
  const year = date.getFullYear()
  const month = date.getMonth()

  const firstDay = new Date(year, month, 1).getDay()
  const lastMonthLastDate = new Date(year, month, 0).getDate()
  for (let i = firstDay - 1; i >= 0; i--) {
    days.push({
      day: lastMonthLastDate - i,
      date: `${year}-${String(month).padStart(2, '0')}-${String(lastMonthLastDate - i).padStart(2, '0')}`,
      studyHours: 0,
      currentMonth: false
    })
  }

  const daysInMonth = new Date(year, month + 1, 0).getDate()
  for (let i = 1; i <= daysInMonth; i++) {
    const dateKey = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({
      day: i,
      date: dateKey,
      studyHours: calendarDayMap.value[dateKey] || 0,
      currentMonth: true
    })
  }

  return days
})

function formatMonthKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function toMinutes(seconds) {
  if (!seconds) return 0
  return Math.max(1, Math.round(seconds / 60))
}

function getStaticBaseUrl() {
  return (import.meta.env.VITE_FILE_BASE_URL || import.meta.env.VITE_SERVER_ORIGIN || 'http://localhost:8081').replace(/\/$/, '')
}

function normalizeCover(url, seed) {
  if (!url) return `https://picsum.photos/400/240?random=${seed}`
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  const base = getStaticBaseUrl()
  if (!base) return url.startsWith('/') ? url : `/${url}`
  return url.startsWith('/') ? `${base}${url}` : `${base}/${url}`
}

function decorateCategories(list = []) {
  return list.map(item => {
    const meta = categoryMeta[item.id] || { icon: Document, color: '#909399' }
    return {
      ...item,
      description: item.description || '暂无专题说明',
      icon: meta.icon,
      color: meta.color,
      courseCount: item.courseCount || 0,
      studyHours: item.studyHours || 0
    }
  })
}

function mapCourseCard(item, index) {
  return {
    ...item,
    cover: normalizeCover(item.cover, item.id || index + 1),
    videoUrl: item.videoUrl || '',
    duration: toMinutes(item.duration),
    enrollment: item.enrollment || 0,
    progress: item.progress || 0,
    favorite: !!item.favorite,
    category: item.categoryName || item.categoryId || ''
  }
}

function mapMyCourse(item, index) {
  return {
    ...item,
    cover: normalizeCover(item.cover, 100 + (item.id || index + 1)),
    videoUrl: item.videoUrl || '',
    duration: toMinutes(item.duration),
    progress: item.progress || 0,
    favorite: !!item.favorite,
    category: item.categoryName || item.categoryId || '',
    lastStudyTime: item.lastStudyTime || null
  }
}

function mapLearningRecord(item) {
  return {
    ...item,
    type: 'video',
    duration: item.duration || 0,
    timestamp: item.timestamp,
    content: item.content || '完成一次视频学习'
  }
}

function buildCalendarDayMap(list = []) {
  const map = {}
  list.forEach(item => {
    if (!item || !item.date) return
    map[item.date] = item.studyHours || 0
  })
  return map
}

const fetchHomeData = async () => {
  loading.value = true
  try {
    const res = await courseHomeService({
      keyword: searchKeyword.value?.trim() || undefined,
      difficulty: filterDifficulty.value,
      categoryId: filterCategory.value === 'all' ? undefined : filterCategory.value,
      month: currentMonth.value
    })
    const data = res.data || {}

    stats.value = { ...stats.value, ...(data.stats || {}) }
    myCoursesStats.value = { ...myCoursesStats.value, ...(data.myCoursesStats || {}) }
    calendarStats.value = {
      thisMonth: {
        totalHours: data.calendarStats?.thisMonth?.totalHours || 0,
        completedCourses: data.calendarStats?.thisMonth?.completedCourses || 0
      }
    }
    efficiencyStats.value = { ...efficiencyStats.value, ...(data.efficiencyStats || {}) }

    categories.value = decorateCategories(data.categories || [])
    featuredCourses.value = (data.featuredCourses || []).map(mapCourseCard)
    popularCourses.value = (data.popularCourses || []).map(mapCourseCard)
    myCourses.value = (data.myCourses || []).map(mapMyCourse)
    learningRecords.value = (data.learningRecords || []).map(mapLearningRecord)
    durationDistribution.value = data.durationDistribution || []
    timeDistribution.value = data.timeDistribution || []
    calendarDayMap.value = buildCalendarDayMap(data.calendarDays || [])

    await nextTick()
    initCharts()
  } catch (error) {
    console.error('获取课程首页数据失败', error)
    ElMessage.error(error?.message || '获取课程首页数据失败')
  } finally {
    loading.value = false
  }
}

const getDifficultyType = (difficulty) => {
  const types = ['success', 'warning', 'danger']
  return types[(difficulty || 1) - 1] || 'info'
}

const getDifficultyText = (difficulty) => {
  const texts = ['初级', '中级', '高级']
  return texts[(difficulty || 1) - 1] || '未知'
}

const getCategoryText = (category) => {
  if (category === null || category === undefined || category === '') return '未分类'
  if (typeof category === 'string' && Number.isNaN(Number(category))) {
    return category
  }
  const categoryId = Number(category)
  const matched = categories.value.find(item => item.id === categoryId)
  return matched?.name || String(category)
}

const getProgressColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  if (percentage >= 40) return '#409eff'
  return '#f56c6c'
}

const getEfficiencyColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getRecordType = (type) => {
  if (type === 'video') return 'primary'
  return 'info'
}

const getRecordIcon = (type) => {
  if (type === 'video') return VideoCameraFilled
  return Document
}

const getRecordTypeText = (type) => {
  if (type === 'video') return '视频学习'
  return '学习记录'
}

const getCalendarDayClass = (day) => {
  return {
    'current-month': day.currentMonth,
    'studied': day.studyHours > 0
  }
}

const formatNumber = (num) => {
  const value = Number(num) || 0
  if (value >= 10000) return `${(value / 10000).toFixed(1)}万`
  if (value >= 1000) return `${(value / 1000).toFixed(1)}千`
  return value
}

const formatHours = (value) => {
  const num = Number(value || 0)
  if (Number.isInteger(num)) return num
  return Number(num.toFixed(1))
}

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const formatLastStudyTime = (time) => {
  if (!time) return '未开始'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return '未开始'
  const now = new Date()
  const diff = now - date

  if (diff < 24 * 60 * 60 * 1000) {
    return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (diff < 2 * 24 * 60 * 60 * 1000) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const formatRecordTime = (time) => {
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const tableRowClassName = ({ row }) => {
  if (row.progress === 100) return 'completed-row'
  if (row.progress > 0 && row.progress < 100) return 'in-progress-row'
  return 'not-started-row'
}

const handleTabClick = (tab) => {
  activeTab.value = tab.paneName
  if (tab.paneName === 'records') {
    nextTick(() => {
      initCharts()
      durationChart?.resize()
      timeChart?.resize()
    })
  }
}

const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    fetchHomeData()
  }, 300)
}

const handleFilterChange = () => {
  fetchHomeData()
}

const resetFilters = () => {
  searchKeyword.value = ''
  filterDifficulty.value = 'all'
  filterCategory.value = 'all'
  fetchHomeData()
}

const viewCourseDetail = (courseId) => {
  router.push({ path: '/study/course/learning', query: { id: courseId } })
}

const startLearning = (course) => {
  ElMessageBox.confirm(
    `开始学习《${course.title}》？\n\n预计时长：${course.duration}分钟`,
    '开始学习',
    {
      confirmButtonText: '开始学习',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    await courseStartService(course.id)
    router.push({ path: '/study/course/learning', query: { id: course.id } })
    ElMessage.success('开始学习，祝您学有所获！')
    fetchHomeData()
  }).catch(() => {})
}

const continueLearning = (course) => {
  router.push({ path: '/study/course/learning', query: { id: course.id } })
  ElMessage.info(`继续学习《${course.title}》`)
}

const reviewCourse = (course) => {
  ElMessageBox.confirm(
    '重新学习将清空当前进度，确定要重新学习吗？',
    '重新学习',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await courseReviewService(course.id)
    router.push({ path: '/study/course/learning', query: { id: course.id, review: true } })
    ElMessage.success('开始重新学习')
    fetchHomeData()
  }).catch(() => {})
}

const addToFavorite = async (course) => {
  try {
    const res = await courseToggleFavoriteService(course.id)
    const favorite = !!res.data?.favorite
    course.favorite = favorite
    ElMessage.success(favorite ? `收藏成功：${course.title}` : `已取消收藏：${course.title}`)
  } catch (error) {
    console.error('收藏操作失败', error)
    ElMessage.error(error?.message || '操作失败')
  }
}

const viewAllPopularCourses = () => {
  ElMessage.info('当前已展示热门课程，可继续通过筛选条件查看')
}

const removeFromMyCourses = (course) => {
  ElMessageBox.confirm(
    `确定要将《${course.title}》从我的课程中移除吗？`,
    '移除课程',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await courseRemoveMyCourseService(course.id)
    ElMessage.success('课程已移除')
    fetchHomeData()
  }).catch(() => {})
}

const changeCalendarMonth = (direction) => {
  const date = new Date(currentMonth.value + '-01')
  date.setMonth(date.getMonth() + direction)
  currentMonth.value = formatMonthKey(date)
  fetchHomeData()
}

const viewDayRecords = (day) => {
  if (day.studyHours > 0) {
    activeTab.value = 'records'
    ElMessage.info(`查看${day.date}的学习记录`)
    nextTick(() => initCharts())
  }
}

const viewRecordDetail = (record) => {
  ElMessage.info(`学习记录：${record.courseTitle}`)
}

const viewCategoryCourses = (categoryId) => {
  filterCategory.value = categoryId
  activeTab.value = 'recommended'
  fetchHomeData()
}

const initCharts = () => {
  if (durationChartRef.value) {
    if (!durationChart) {
      durationChart = echarts.init(durationChartRef.value)
    }
    durationChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { top: '5%', left: 'center' },
      series: [
        {
          name: '学习时长分布',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: '14', fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data: durationDistribution.value.length
            ? durationDistribution.value.map(item => ({ value: item.value || 0, name: item.label || '未分类' }))
            : [{ value: 1, name: '暂无数据' }]
        }
      ]
    })
  }

  if (timeChartRef.value) {
    if (!timeChart) {
      timeChart = echarts.init(timeChartRef.value)
    }
    timeChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: timeDistribution.value.length
          ? timeDistribution.value.map(item => item.label)
          : ['暂无数据'],
        axisLine: {
          lineStyle: { color: '#dcdfe6' }
        }
      },
      yAxis: {
        type: 'value',
        axisLine: {
          lineStyle: { color: '#dcdfe6' }
        },
        splitLine: {
          lineStyle: {
            color: '#f0f2f5',
            type: 'dashed'
          }
        }
      },
      series: [
        {
          name: '学习次数',
          type: 'bar',
          barWidth: '60%',
          itemStyle: { color: '#409eff' },
          data: timeDistribution.value.length
            ? timeDistribution.value.map(item => item.value || 0)
            : [0]
        }
      ]
    })
  }
}

const handleResize = () => {
  if (durationChart) durationChart.resize()
  if (timeChart) timeChart.resize()
}

watch([durationDistribution, timeDistribution], () => {
  nextTick(() => {
    initCharts()
    if (durationChart) durationChart.resize()
    if (timeChart) timeChart.resize()
  })
})

onMounted(async () => {
  await fetchHomeData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (durationChart) durationChart.dispose()
  if (timeChart) timeChart.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
    <div class="course-learning">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">学习筑基</h1>
          <p class="page-subtitle">夯实师德基础，提升专业素养</p>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <div class="stat-value">{{ stats.completedCourses }}</div>
            <div class="stat-label">已完成课程</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatHours(stats.studyHours) }}h</div>
            <div class="stat-label">学习时长</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.continuityDays }}天</div>
            <div class="stat-label">连续学习</div>
          </div>
        </div>
      </div>

      <div class="learning-nav">
        <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
          <el-tab-pane label="推荐课程" name="recommended">
            <template #label>
              <span class="tab-label">
                <el-icon><Star /></el-icon>推荐课程
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="我的课程" name="mycourses">
            <template #label>
              <span class="tab-label">
                <el-icon><Document /></el-icon>我的课程
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="学习记录" name="records">
            <template #label>
              <span class="tab-label">
                <el-icon><Notebook /></el-icon>学习记录
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="专题分类" name="categories">
            <template #label>
              <span class="tab-label">
                <el-icon><Folder /></el-icon>专题分类
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
  
      <div class="main-content">
        <div v-show="activeTab === 'recommended'">
          <div class="filter-section">
            <div class="filter-left">
              <el-input v-model="searchKeyword" placeholder="搜索课程标题、描述或关键词" class="search-input" :prefix-icon="Search" clearable @input="handleSearch"/>
              <el-select v-model="filterDifficulty" placeholder="难度筛选" class="filter-select" @change="handleFilterChange">
                <el-option label="全部" value="all" />
                <el-option label="初级" value="beginner" />
                <el-option label="中级" value="intermediate" />
                <el-option label="高级" value="advanced" />
              </el-select>
              <el-select v-model="filterCategory" placeholder="分类筛选" class="filter-select" @change="handleFilterChange">
                <el-option label="全部" value="all" />
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </div>
            <div class="filter-right">
              <el-button type="primary" plain @click="resetFilters">
                <el-icon><Refresh /></el-icon>重置筛选
              </el-button>
            </div>
          </div>
  
          <div class="featured-courses">
            <h3 class="section-title">
              <el-icon><StarFilled /></el-icon>精选推荐
            </h3>
            <el-carousel :interval="5000" height="320px" type="card" arrow="always"class="featured-carousel">
              <el-carousel-item v-for="course in featuredCourses" :key="course.id" @click="viewCourseDetail(course.id)">
                <div class="featured-course">
                  <img :src="course.cover" :alt="course.title" class="course-cover" />
                  <div class="favorite-badge" v-if="course.favorite">
                    <el-icon><StarFilled /></el-icon>
                  </div>
                  <div class="course-overlay">
                    <div class="course-tags">
                      <el-tag :type="getDifficultyType(course.difficulty)" size="small">
                        {{ getDifficultyText(course.difficulty) }}
                      </el-tag>
                      <el-tag v-if="course.category" type="info" size="small">
                        {{ getCategoryText(course.category) }}
                      </el-tag>
                    </div>
                    <div class="course-info">
                      <h3>{{ course.title }}</h3>
                      <p class="course-description">{{ course.description }}</p>
                      <div class="course-stats">
                        <span class="stat-item">
                          <el-icon><Clock /></el-icon> {{ course.duration }}分钟
                        </span>
                        <span class="stat-item">
                          <el-icon><User /></el-icon> {{ formatNumber(course.enrollment) }}人学习
                        </span>
                        <span class="stat-item">
                          <el-icon><Star /></el-icon> {{ course.rating }}
                        </span>
                      </div>
                      <div class="course-actions">
                        <el-button v-if="course.progress === 0" type="primary" @click.stop="startLearning(course)">
                          <el-icon><VideoPlay /></el-icon>开始学习
                        </el-button>
                        <el-button v-else-if="course.progress < 100" type="primary" @click.stop="continueLearning(course)">
                          <el-icon><VideoPlay /></el-icon>继续学习
                        </el-button>
                        <el-button v-else type="success" @click.stop="reviewCourse(course)">
                          <el-icon><RefreshRight /></el-icon>重新学习
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>
  
          <div class="popular-courses">
            <div class="section-header">
              <h3 class="section-title">热门课程</h3>
              <el-button type="text" @click="viewAllPopularCourses">
                查看更多 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
            <el-row :gutter="20" class="course-grid">
              <el-col v-for="course in popularCourses" :key="course.id"
                :xs="24":sm="12":md="8":lg="6":xl="6">
                <el-card class="course-card" shadow="hover" @click="viewCourseDetail(course.id)">
                  <div class="course-card-content">
                    <div class="course-image-container">
                      <img :src="course.cover" :alt="course.title" class="course-image" />
                      <div class="course-status" v-if="course.progress">
                        <el-progress :percentage="course.progress" :stroke-width="4" :show-text="false" color="#67c23a" />
                      </div>
                    </div>
                    <div class="course-body">
                      <div class="course-header">
                        <h4 class="course-title">{{ course.title }}</h4>
                        <div class="course-tags">
                          <el-tag size="small" :type="getDifficultyType(course.difficulty)">
                            {{ getDifficultyText(course.difficulty) }}
                          </el-tag>
                        </div>
                      </div>
                      <p class="course-desc">{{ truncateText(course.description, 60) }}</p>
                      <div class="course-meta">
                        <span class="meta-item">
                          <el-icon><Clock /></el-icon> {{ course.duration }}分钟
                        </span>
                        <span class="meta-item">
                          <el-icon><User /></el-icon> {{ formatNumber(course.enrollment) }}
                        </span>
                      </div>
                      <div class="course-actions">
                        <el-button v-if="course.progress && course.progress < 100" type="primary" size="small" @click.stop="continueLearning(course)">
                          <el-icon><VideoPlay /></el-icon>继续学习
                        </el-button>
                        <el-button v-else-if="!course.progress" type="primary" plain size="small" @click.stop="startLearning(course)">
                          <el-icon><VideoPlay /></el-icon>开始学习
                        </el-button>
                        <el-button v-else type="success" plain size="small" @click.stop="reviewCourse(course)">
                          <el-icon><RefreshRight /></el-icon> 重新学习
                        </el-button>
                        <el-button type="text" size="small" @click.stop="addToFavorite(course)">
                          <el-icon>
                            <StarFilled v-if="course.favorite" />
                            <Star v-else />
                          </el-icon>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
  
        <div v-show="activeTab === 'mycourses'">
          <div class="progress-overview">
            <h3 class="section-title">学习进度概览</h3>
            <div class="progress-content">
              <div class="progress-summary">
                <div class="summary-item">
                  <div class="summary-label">总课程数</div>
                  <div class="summary-value">{{ myCoursesStats.total }}</div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">已完成</div>
                  <div class="summary-value">{{ myCoursesStats.completed }}</div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">进行中</div>
                  <div class="summary-value">{{ myCoursesStats.inProgress }}</div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">未开始</div>
                  <div class="summary-value">{{ myCoursesStats.notStarted }}</div>
                </div>
              </div>
              <el-progress :percentage="myCoursesStats.completionRate" :stroke-width="12"
                :color="getProgressColor(myCoursesStats.completionRate)" :format="(percent) => `总完成率：${percent}%`"/>
            </div>
          </div>

          <div class="my-courses-list">
            <div class="list-header">
              <h3 class="section-title">我的课程列表</h3>
              <el-select v-model="myCoursesFilter" placeholder="筛选学习状态" size="small" style="width: 120px">
                <el-option label="全部" value="all" />
                <el-option label="进行中" value="in-progress" />
                <el-option label="已完成" value="completed" />
                <el-option label="未开始" value="not-started" />
              </el-select>
            </div>
            <el-table :data="filteredMyCourses" style="width: 100%" :row-class-name="tableRowClassName">
              <el-table-column label="课程" width="400">
                <template #default="{ row }">
                  <div class="course-cell" @click="viewCourseDetail(row.id)">
                    <img :src="row.cover" :alt="row.title" class="course-thumb" />
                    <div class="course-info">
                      <div class="course-title">{{ row.title }}</div>
                      <div class="course-category">
                        <el-tag size="small">{{ getCategoryText(row.category) }}</el-tag>
                        <el-tag v-if="row.favorite" size="small" type="warning" effect="plain">已收藏</el-tag>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="学习进度" width="300">
                <template #default="{ row }">
                  <div class="progress-cell">
                    <el-progress :percentage="row.progress || 0" :stroke-width="8" :color="getProgressColor(row.progress)":show-text="row.progress > 0"/>
                    <span v-if="row.progress === 0" class="not-started">未开始</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="最近学习" width="200">
                <template #default="{ row }">
                  <div class="last-study">
                    {{ formatLastStudyTime(row.lastStudyTime) }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-button v-if="row.progress === 0"  type="primary" size="small" @click="startLearning(row)">开始学习</el-button>
                    <el-button v-else-if="row.progress < 100" type="primary" size="small" @click="continueLearning(row)">继续学习</el-button>
                    <el-button v-else type="success" size="small" @click="reviewCourse(row)">重新学习</el-button>
                    <el-button type="text" size="small" @click="removeFromMyCourses(row)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
  
        <div v-show="activeTab === 'records'">
          <el-card class="study-calendar" shadow="never">
            <template #header>
              <div class="card-header">
                <h3>学习日历</h3>
                <div class="calendar-actions">
                  <el-button-group>
                    <el-button @click="changeCalendarMonth(-1)"><el-icon><ArrowLeft /></el-icon></el-button>
                    <el-button @click="changeCalendarMonth(1)"><el-icon><ArrowRight /></el-icon></el-button>
                  </el-button-group>
                </div>
              </div>
            </template>
            <div class="calendar-content">
              <div class="calendar-header">
                <span class="current-month">{{ currentMonth }}</span>
                <span class="study-summary">
                  本月学习：{{ formatHours(calendarStats.thisMonth.totalHours) }}小时，
                  {{ calendarStats.thisMonth.completedCourses }}门课程
                </span>
              </div>
              <div class="calendar-grid">
                <div class="calendar-weekday" v-for="day in weekdays" :key="day"> {{ day }} </div>
                <div v-for="day in calendarDays" :key="day.date" class="calendar-day" :class="getCalendarDayClass(day)" @click="viewDayRecords(day)">
                  <div class="day-number">{{ day.day }}</div>
                  <div v-if="day.studyHours > 0" class="day-studied">
                    <el-progress :percentage="Math.min(day.studyHours * 10, 100)" :stroke-width="4" :show-text="false" color="#409eff"/>
                  </div>
                </div>
              </div>
            </div>
          </el-card>

          <el-card class="records-list" shadow="never">
            <template #header>
              <div class="card-header">
                <h3>详细学习记录</h3>
                <el-select v-model="recordsFilter" placeholder="筛选记录类型" size="small" style="width: 120px">
                  <el-option label="全部" value="all" />
                  <el-option label="视频学习" value="video" />
                </el-select>
              </div>
            </template>
            <div class="records-content">
              <el-timeline>
                <el-timeline-item v-for="record in filteredLearningRecords" :key="record.id"
                  :timestamp="formatRecordTime(record.timestamp)" placement="top"
                  :type="getRecordType(record.type)" :icon="getRecordIcon(record.type)"
                >
                  <div class="record-item">
                    <div class="record-header">
                      <span class="record-title">{{ record.courseTitle }}</span>
                      <el-tag size="small" :type="getRecordType(record.type)">{{ getRecordTypeText(record.type) }}</el-tag>
                    </div>
                    <div class="record-content"><p>{{ record.content }}</p></div>
                    <div class="record-meta">
                      <span class="meta-item"><el-icon><Clock /></el-icon>{{ record.duration }}分钟</span>
                                            <el-button type="text" size="small" @click="viewRecordDetail(record)">查看详情</el-button>
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <div v-if="filteredLearningRecords.length === 0" class="empty-records">
                <el-empty description="暂无学习记录" />
              </div>
            </div>
          </el-card>

          <el-row :gutter="20" class="learning-stats">
            <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="8">
              <el-card class="stat-card" shadow="hover">
                <template #header>
                  <h3>学习时长分布</h3>
                </template>
                <div class="stat-chart" ref="durationChartRef"></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="8">
              <el-card class="stat-card" shadow="hover">
                <template #header>
                  <h3>学习时间段</h3>
                </template>
                <div class="stat-chart" ref="timeChartRef"></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
              <el-card class="stat-card" shadow="hover">
                <template #header>
                  <h3>学习效率</h3>
                </template>
                <div class="efficiency-stats">
                  <div class="efficiency-item">
                    <div class="efficiency-label">专注度</div>
                    <div class="efficiency-value">{{ efficiencyStats.focus }}%</div>
                    <el-progress :percentage="efficiencyStats.focus" :stroke-width="8" :color="getEfficiencyColor(efficiencyStats.focus)"/>
                  </div>
                  <div class="efficiency-item">
                    <div class="efficiency-label">完成率</div>
                    <div class="efficiency-value">{{ efficiencyStats.completion }}%</div>
                    <el-progress :percentage="efficiencyStats.completion" :stroke-width="8" :color="getEfficiencyColor(efficiencyStats.completion)"/>
                  </div>
                  <div class="efficiency-item">
                    <div class="efficiency-label">理解度</div>
                    <div class="efficiency-value">{{ efficiencyStats.comprehension }}%</div>
                    <el-progress :percentage="efficiencyStats.comprehension" :stroke-width="8" :color="getEfficiencyColor(efficiencyStats.comprehension)"/>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
  
        <div v-show="activeTab === 'categories'">
          <div class="categories-section">
            <div class="categories-header">
              <h3>课程专题分类</h3>
              <p>根据师德师风建设要求，系统化学习各类专题课程</p>
            </div>
            <div class="categories-grid">
              <div v-for="category in categories" :key="category.id" class="category-card" @click="viewCategoryCourses(category.id)">
                <div class="category-icon" :style="{ backgroundColor: category.color }">
                  <el-icon :size="32" color="white">
                    <component :is="category.icon" />
                  </el-icon>
                </div>
                <div class="category-content">
                  <h4>{{ category.name }}</h4>
                  <p>{{ category.description }}</p>
                  <div class="category-stats">
                    <span>{{ category.courseCount }}门课程</span>
                    <span>{{ formatHours(category.studyHours) }}小时</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
</template>

<style lang="scss" scoped>
.course-learning {
  min-height: calc(100vh - 64px);
}

.page-header {
  background: linear-gradient(135deg, #9cb2fa 0%, #80a2ff 100%);
  border-radius: 8px;
  padding: 20px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.2);
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .header-content {
    .page-title {
      margin: 0 0 5px;
      font-size: 24px;
      font-weight: 600;
    }
    .page-subtitle {
      margin: 0;
      font-size: 14px;
      opacity: 0.9;
    }
  }
  
  .header-stats {
    display: flex;
    gap: 40px;  
    .stat-item {
      text-align: center;
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        margin-bottom: 2px;
      }
      .stat-label {
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 20px;
    text-align: center;
    
    .header-stats {
      gap: 20px;
      .stat-item {
        .stat-value {
          font-size: 28px;
        }
      }
    }
  }
}

.learning-nav {
  background: white;
  border-radius: 5px;
  margin-bottom: 0px;
  
  .tab-label {
    display: flex;
    align-items: center;
    gap: 8px;
    .el-icon {
      font-size: 16px;
    }
  }
  &:deep(.el-tabs__content) {
    padding: 0 !important;
  }
}

.main-content {
    background: white;
    border-radius: 5px;
    padding: 0 20px 20px 20px;

    .el-card {
      border-radius: 5px;
      margin-bottom: 15px;
      border: 1px solid #ebeef5;
      
      &:deep(.el-card__header) {
        padding: 10px 20px;
        border-bottom: 1px solid #ebeef5;
      }
      
      &:deep(.el-card__body) {
        padding: 20px;
      }
    }
}
  
.filter-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebeef5;
    
    .filter-left {
      display: flex;
      gap: 12px;
      flex: 1;
      max-width: 600px;
      .search-input {
        flex: 1;
        min-width: 200px;
      }
      .filter-select {
        width: 120px;
      }
    }
}

.featured-courses {
    margin-bottom: 20px;
    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 10px;
      font-size: 20px;
      color: #303133;
      font-weight: 600;
      .el-icon {
        color: #ffc107;
      }
    }
    
    .featured-carousel {
      .featured-course {
        position: relative;
        height: 100%;
        border-radius: 8px;
        overflow: hidden;
        cursor: pointer;
        .course-cover {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      .favorite-badge {
        position: absolute;
        top: 16px;
        right: 16px;
        z-index: 2;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        color: #e6a23c;
        background: rgba(255,255,255,0.92);
        box-shadow: 0 2px 8px rgba(0,0,0,0.12);
      }

        .course-overlay {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
          padding: 24px;
          color: white;
          .course-tags {
            position: absolute;
            top: 16px;
            right: 16px;
            display: flex;
            gap: 8px;
          }
          .course-info {
            h3 {
              margin: 0 0 12px;
              font-size: 24px;
              font-weight: 600;
            }
            .course-description {
              margin: 0 0 16px;
              font-size: 14px;
              opacity: 0.9;
              line-height: 1.5;
            }
            .course-stats {
              display: flex;
              gap: 20px;
              margin-bottom: 20px;
              
              .stat-item {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 14px;
                opacity: 0.9;
              }
            }
          }
        }
      }
    }
}

.section-title {
  display: flex;
  align-items: center;
  margin: 0 0 10px;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.popular-courses {
    margin-bottom: 20px;
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }
    
    .course-grid {
      .course-card {
        border-radius: 8px;
        margin-bottom: 15px;
        cursor: pointer;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
        }
        
        .course-card-content {
          .course-image-container {
            position: relative;
            height: 150px;
            overflow: hidden;
            border-radius: 8px;
            margin-bottom: 16px;
            .course-image {
              width: 100%;
              height: 100%;
              object-fit: cover;
              transition: transform 0.5s ease;
            }
            .course-status {
              position: absolute;
              bottom: 0;
              left: 0;
              right: 0;
              background: rgba(0, 0, 0, 0.5);
              padding: 4px;
            }
          }
          
          .course-body {
            .course-header {
              display: flex;
              justify-content: space-between;
              align-items: flex-start;
              margin-bottom: 10px;
              .course-title {
                margin: 0;
                font-size: 16px;
                color: #303133;
                font-weight: 600;
                line-height: 1.4;
                flex: 1;
              }
            }
            .course-desc {
              margin: 0 0 10px;
              font-size: 14px;
              color: #606266;
              line-height: 1.5;
              height: 42px;
              overflow: hidden;
            }
            .course-meta {
              display: flex;
              gap: 16px;
              margin-bottom: 10px;
              .meta-item {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 13px;
                color: #909399;
                .el-icon {
                  font-size: 14px;
                }
              }
            }
            
            .course-actions {
              display: flex;
              justify-content: space-between;
              align-items: center;
            }
          }
        }
      }
    }
}
  
.progress-overview {
  margin-bottom: 20px;
  
  .progress-content {
    .progress-summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 20px;
      margin-bottom: 20px;
      
      .summary-item {
        text-align: center;
        padding: 10px;
        background: #f8f9fa;
        border-radius: 8px;
        .summary-label {
          font-size: 14px;
          color: #858689;
          margin-bottom: 5px;
        }
        .summary-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;
        }
      }
    }
  }
}

.my-courses-list {
  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .course-cell {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    
    .course-thumb {
      width: 80px;
      height: 50px;
      object-fit: cover;
      border-radius: 4px;
    } 
    .course-info {
      .course-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 2px;
      }
      
      .course-category {
        .el-tag {
          font-size: 12px;
        }
      }
    }
  }
  .progress-cell {
    .not-started {
      color: #909399;
      font-size: 13px;
    }
  }
  .last-study {
    font-size: 14px;
    color: #606266;
  }
  .action-buttons {
    display: flex;
    gap: 8px;
    .el-icon{
      font-size: 16px;
    }
  }
  
  :deep(.completed-row) {
    background-color: #e1f0fb;
  }
  :deep(.in-progress-row) {
    background-color: #fff8e1;
  } 
  :deep(.not-started-row) {
    background-color: #f8f9fa;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  h3 {
    margin: 0;
    font-size: 20px;
    color: #303133;
    font-weight: 600;
  }
}

.study-calendar {
  .calendar-content {
    .calendar-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      .current-month {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
      .study-summary {
        color: #606266;
        font-size: 14px;
      }
    }
    
    .calendar-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 8px;
      
      .calendar-weekday {
        text-align: center;
        padding: 8px;
        font-weight: 500;
        color: #606266;
        background: #f8f9fa;
        border-radius: 4px;
      }
      
      .calendar-day {
        text-align: center;
        padding: 8px;
        border: 1px solid #ebeef5;
        border-radius: 4px;
        cursor: pointer;
        min-height: 50px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        
        &:not(.current-month) {
          color: #c0c4cc;
          background: #f8f9fa;
        }
        &.studied {
          background: #ecf5ff;
          border-color: #409eff;
        }
        .day-number {
          font-size: 14px;
          font-weight: 500;
        }
        .day-studied {
          .el-progress {
            margin-top: 4px;
          }
        }
      }
    }
  }
}
    
.records-list {
  .records-content {
    .record-item {
      .record-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        .record-title {
          font-size: 16px;
          font-weight: 500;
          color: #303133;
        }
      }
      
      .record-content {
        p {
          margin: 0 0 4px;
          color: #606266;
        }
      }
      
      .record-meta {
        display: flex;
        gap: 16px;
        align-items: center;
        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 13px;
          color: #909399;
        }
      }
    }
    
    .empty-records {
      padding: 40px 0;
      text-align: center;
    }
  }
}

.learning-stats {
  .stat-card {
    height: 100%;
    &:deep(.el-card__header) {
      padding: 0px 20px;
    }

    &:deep(.el-card__body) {
      padding: 10px;
    }
    
    .stat-chart {
      flex: 1;
      min-height: 200px;
      min-width: 300px;
    }
    
    .efficiency-stats {
      .efficiency-item {
        margin-bottom: 10px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .efficiency-label {
          font-size: 14px;
          color: #606266;
          margin-bottom: 4px;
        }
        
        .efficiency-value {
          font-size: 16px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 4px;
        }
      }
    }
  }
}
  
.categories-section {
    .categories-header {
      text-align: center;
      margin-bottom: 20px;
      
      h3 {
        margin: 0 0 8px;
        font-size: 24px;
        color: #303133;
        font-weight: 600;
      }
      p {
        margin: 0;
        color: #606266;
        font-size: 16px;
      }
    }
    
    .categories-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 24px;
      .category-card {
        display: flex;
        padding: 20px;
        border-radius: 8px;
        background: white;
        border: 1px solid #ebeef5;
        cursor: pointer;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
        }
        
        .category-icon {
          width: 60px;
          height: 60px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 20px;
          flex-shrink: 0;
        }
        
        .category-content {
          flex: 1;
          h4 {
            margin: 0 0 8px;
            font-size: 18px;
            color: #303133;
            font-weight: 600;
          }      
          p {
            margin: 0 0 8px;
            font-size: 14px;
            color: #606266;
            line-height: 1.5;
          }
          .category-stats {
            display: flex;
            gap: 16px;
            font-size: 13px;
            color: #909399;
          }
        }
      }
    }
}
  
@media (max-width: 768px) {
    .course-learning {
      padding: 15px;
    }
    
    .page-header {
      padding: 20px;
      flex-direction: column;
      text-align: center;
      gap: 20px;
      
      .header-stats {
        width: 100%;
        justify-content: space-around;
      }
    }
    
    .main-content {
      padding: 16px;
    }
    
    .filter-section {
      flex-direction: column;
      gap: 12px;
      
      .filter-left {
        width: 100%;
        flex-direction: column;
        
        .search-input,.filter-select {
          width: 100%;
        }
      }
    }
    
    .featured-courses {
      .featured-course {
        .course-overlay {
          padding: 16px;
          .course-info {
            h3 {
              font-size: 18px;
            }
          }
        }
      }
    }
    
    .calendar-content {
      .calendar-header {
        flex-direction: column;
        gap: 8px;
        text-align: center;
      }
    }
}
</style>