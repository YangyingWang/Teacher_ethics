<script setup>
import {
  Search,
  ArrowDown,
  Filter,
  Refresh,
  Star,
  Menu,
  Grid,
  Collection,
  Plus,
  Monitor,
  Cpu,
  Lightning,
  Setting,
  Document,
  Flag,
  School,
  Trophy,
  Reading,
  Medal
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref, computed, onMounted } from 'vue'
import { ideologyHomeService, ideologyPageService, ideologyDetailService,
  ideologyFavoritesService, ideologyTeachingCoursesService, ideologyToggleFavoriteService
} from '@/api/study.js'

const searchQuery = ref('')
const searching = ref(false)
const hasSearched = ref(false)
const showFilters = ref(false)
const showAIRecommendation = ref(true)
const showApplyDialog = ref(false)
const showDetailDialog = ref(false)
const detailLoading = ref(false)

const filters = ref({
  disciplineId: '',
  elementTypeId: '',
  courseTypeId: '',
  difficulty: '',
  onlyFavorite: false
})

const viewMode = ref('grid')
const sortBy = ref('relevance')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const searchStats = ref({
  totalElements: 0,
  totalCourses: 0,
  favoriteCount: 0
})

const suggestedTags = ref([])
const disciplines = ref([])
const ideologyTypes = ref([])
const courseTypes = ref([])
const aiRecommendations = ref([])
const searchResults = ref([])
const myCollections = ref([])
const myCourses = ref([])
const currentElement = ref(null)
const detailData = ref(null)

const applyForm = ref({
  courseId: '',
  method: 'reference',
  notes: ''
})

const difficultyOptions = [
  { label: '初级', value: 1 },
  { label: '中级', value: 2 },
  { label: '高级', value: 3 }
]

const pagedResults = computed(() => searchResults.value)

const typeThemeMap = {
  法治意识: { color: '#67c23a', icon: Document },
  爱国主义: { color: '#f56c6c', icon: Trophy },
  文化自信: { color: '#e6a23c', icon: Flag },
  职业道德: { color: '#409eff', icon: Medal },
  社会责任: { color: '#36cfc9', icon: Collection },
  工匠精神: { color: '#909399', icon: Setting },
  创新精神: { color: '#409eff', icon: Cpu },
  生态文明: { color: '#67c23a', icon: School },
  人文素养: { color: '#722ed1', icon: Reading },
  工程伦理: { color: '#13c2c2', icon: Monitor }
}

function getTheme(typeName) {
  return typeThemeMap[typeName] || { color: '#409eff', icon: Document }
}

function normalizeElement(item = {}) {
  const theme = getTheme(item.ideologyType)
  return {
    id: item.id,
    title: item.title || '',
    description: item.description || item.summary || '',
    difficulty: Number(item.difficulty || 2),
    ideologyType: item.ideologyType || '',
    suitableCourses: Array.isArray(item.suitableCourses) ? item.suitableCourses : [],
    keywords: Array.isArray(item.keywords) ? item.keywords : [],
    recommendationScore: Number(Number(item.recommendationScore || 0).toFixed(1)),
    collected: !!item.collected,
    popularity: Number(item.popularity || 0),
    color: theme.color,
    icon: theme.icon
  }
}

function normalizeDetail(item = {}) {
  const theme = getTheme(item.ideologyType)
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    content: item.content || '',
    difficulty: Number(item.difficulty || 2),
    ideologyType: item.ideologyType || '',
    disciplines: Array.isArray(item.disciplines) ? item.disciplines : [],
    courseTypes: Array.isArray(item.courseTypes) ? item.courseTypes : [],
    suitableCourses: Array.isArray(item.suitableCourses) ? item.suitableCourses : [],
    keywords: Array.isArray(item.keywords) ? item.keywords : [],
    viewCount: Number(item.viewCount || 0),
    favoriteCount: Number(item.favoriteCount || 0),
    useCount: Number(item.useCount || 0),
    popularity: Number(item.popularity || 0),
    collected: !!item.collected,
    color: theme.color,
    icon: theme.icon
  }
}

function getDifficultyType(difficulty) {
  const types = ['success', 'warning', 'danger']
  return types[(difficulty || 1) - 1] || 'info'
}

function getDifficultyText(difficulty) {
  const texts = ['初级', '中级', '高级']
  return texts[(difficulty || 1) - 1] || '未知'
}

function truncateText(text, length) {
  if (!text) return ''
  return text.length > length ? `${text.substring(0, length)}...` : text
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (Number.isNaN(diff)) return timeStr
  if (diff < 24 * 60 * 60 * 1000) return '今天'
  if (diff < 2 * 24 * 60 * 60 * 1000) return '昨天'
  return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
}

function hasAnyCondition() {
  return !!(
    searchQuery.value.trim() ||
    filters.value.disciplineId ||
    filters.value.elementTypeId ||
    filters.value.courseTypeId ||
    filters.value.difficulty ||
    filters.value.onlyFavorite
  )
}

function buildQueryParams() {
  return {
    keyword: searchQuery.value.trim() || undefined,
    disciplineId: filters.value.disciplineId || undefined,
    elementTypeId: filters.value.elementTypeId || undefined,
    courseTypeId: filters.value.courseTypeId || undefined,
    difficulty: filters.value.difficulty || undefined,
    onlyFavorite: filters.value.onlyFavorite || false,
    sortBy: sortBy.value,
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }
}

async function loadHomeData() {
  try {
    const res = await ideologyHomeService()
    const data = res.data || {}

    searchStats.value = {
      totalElements: data.stats?.totalElements || 0,
      totalCourses: data.stats?.totalCourses || 0,
      favoriteCount: data.stats?.favoriteCount || 0
    }

    disciplines.value = data.disciplines || []
    ideologyTypes.value = data.elementTypes || []
    courseTypes.value = data.courseTypes || []
    myCourses.value = data.teachingCourses || []

    suggestedTags.value = (data.suggestedKeywords || []).map((text, index) => ({
      id: index + 1,
      text
    }))

    aiRecommendations.value = (data.recommendations || []).map(normalizeElement)
    myCollections.value = data.favoriteList || []
  } catch (error) {
    console.error('加载课程思政首页数据失败', error)
    ElMessage.error('加载检索首页数据失败，请稍后重试')
  }
}

async function loadFavorites() {
  try {
    const res = await ideologyFavoritesService()
    myCollections.value = res.data || []
  } catch (error) {
    console.error('加载收藏列表失败', error)
  }
}

async function loadTeachingCourses() {
  try {
    const res = await ideologyTeachingCoursesService()
    myCourses.value = res.data || []
  } catch (error) {
    console.error('加载教学课程失败', error)
  }
}

async function fetchSearchPage(showTip = true) {
  searching.value = true
  hasSearched.value = true
  showAIRecommendation.value = false
  try {
    const res = await ideologyPageService(buildQueryParams())
    const data = res.data || {}
    total.value = data.total || 0
    searchResults.value = (data.list || []).map(normalizeElement)

    if (showTip) {
      if (total.value > 0) {
        ElMessage.success(`找到 ${total.value} 个相关结果`)
      } else {
        ElMessage.info('未找到符合条件的思政元素')
      }
    }
  } catch (error) {
    console.error('检索思政元素失败', error)
    ElMessage.error('检索失败，请稍后重试')
  } finally {
    searching.value = false
  }
}

async function handleSearch() {
  currentPage.value = 1
  await fetchSearchPage(true)
}

function handleClearSearch() {
  searchQuery.value = ''
  total.value = 0
  searchResults.value = []
  hasSearched.value = false
  showAIRecommendation.value = true
  currentPage.value = 1
  sortBy.value = 'relevance'
  resetFilters()
}

function handleTagClick(tag) {
  searchQuery.value = tag.text
  handleSearch()
}

function toggleAdvancedFilters() {
  showFilters.value = !showFilters.value
}

function handleKeywordQuickSearch(keyword) {
  searchQuery.value = keyword
  handleSearch()
}

async function handleFilterSearch() {
  currentPage.value = 1
  await fetchSearchPage(true)
}

function resetFilters() {
  filters.value = {
    disciplineId: '',
    elementTypeId: '',
    courseTypeId: '',
    difficulty: '',
    onlyFavorite: false
  }
}

async function refreshRecommendations() {
  await loadHomeData()
  ElMessage.success('推荐内容已更新')
}

function handleQuickSearch(keyword) {
  searchQuery.value = keyword
  handleSearch()
}

async function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  if (hasSearched.value) {
    await fetchSearchPage(false)
  }
}

async function handleCurrentChange(page) {
  currentPage.value = page
  if (hasSearched.value) {
    await fetchSearchPage(false)
  }
}

async function changeSort() {
  currentPage.value = 1
  if (hasSearched.value) {
    await fetchSearchPage(false)
  }
}

async function viewElementDetail(element) {
  showDetailDialog.value = true
  detailLoading.value = true
  try {
    const res = await ideologyDetailService(element.id)
    detailData.value = normalizeDetail(res.data || {})
    syncFavoriteState(element.id, detailData.value.collected)
  } catch (error) {
    console.error('获取思政元素详情失败', error)
    ElMessage.error('加载详情失败，请稍后重试')
    showDetailDialog.value = false
  } finally {
    detailLoading.value = false
  }
}

function syncFavoriteState(elementId, collected) {
  searchResults.value.forEach(item => {
    if (item.id === elementId) item.collected = collected
  })
  aiRecommendations.value.forEach(item => {
    if (item.id === elementId) item.collected = collected
  })
  if (detailData.value && detailData.value.id === elementId) {
    detailData.value.collected = collected
  }
}

async function addToCollection(element) {
  try {
    const res = await ideologyToggleFavoriteService(element.id)
    const collected = !!res.data
    syncFavoriteState(element.id, collected)

    if (detailData.value && detailData.value.id === element.id) {
      detailData.value.favoriteCount = Math.max(
        0,
        (detailData.value.favoriteCount || 0) + (collected ? 1 : -1)
      )
    }

    searchStats.value.favoriteCount = Math.max(
      0,
      (searchStats.value.favoriteCount || 0) + (collected ? 1 : -1)
    )

    await loadFavorites()
    ElMessage.success(collected ? '已添加到我的收藏' : '已从我的收藏移除')
  } catch (error) {
    console.error('收藏操作失败', error)
    ElMessage.error('收藏操作失败，请稍后重试')
  }
}

function applyToCourse(element) {
  currentElement.value = element
  applyForm.value = {
    courseId: '',
    method: 'reference',
    notes: ''
  }
  showApplyDialog.value = true
}

function confirmApply() {
  if (!applyForm.value.courseId) {
    ElMessage.warning('请选择要应用的课程')
    return
  }
  const course = myCourses.value.find(item => item.id === applyForm.value.courseId)
  ElMessage.success(`已将“${currentElement.value.title}”应用到《${course?.name || ''}》课程`)
  showApplyDialog.value = false
}

function searchByKeyword(keyword) {
  searchQuery.value = keyword
  handleSearch()
}

function resetSearch() {
  handleClearSearch()
}

function viewAllCollections() {
  filters.value.onlyFavorite = true
  showFilters.value = true
  handleSearch()
}

function openMyFavoriteSearch() {
  filters.value.onlyFavorite = true
  currentPage.value = 1
  handleSearch()
}

onMounted(async () => {
  await loadHomeData()
  await loadTeachingCourses()
})
</script>

<template>
  <div class="ideology-search">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">面向课程教学场景的思政元素检索与推荐</h1>
        <p class="page-subtitle">基于关键词与条件筛选，快速定位适合当前课程教学场景的思政元素</p>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <div class="stat-value">{{ searchStats.totalElements }}</div>
          <div class="stat-label">思政元素总数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ searchStats.totalCourses }}</div>
          <div class="stat-label">关联课程数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ searchStats.favoriteCount }}</div>
          <div class="stat-label">我的收藏</div>
        </div>
      </div>
    </div>

    <div class="search-section">
      <el-card class="search-card" shadow="never">
        <div class="search-container">
          <div class="main-search">
            <el-input
              v-model="searchQuery"
              placeholder="请输入课程名称、专业关键词或思政元素..."
              size="large"
              class="search-input"
              :prefix-icon="Search"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleClearSearch"
            >
              <template #append>
                <el-button type="primary" @click="handleSearch" :loading="searching">
                  <el-icon><Search /></el-icon>
                  智能检索
                </el-button>
              </template>
            </el-input>
          </div>

          <div class="suggested-tags" v-if="suggestedTags.length > 0">
            <div class="tags-title">热门搜索：</div>
            <div class="tags-list">
              <el-tag
                v-for="tag in suggestedTags"
                :key="tag.id"
                class="suggested-tag"
                effect="plain"
                @click="handleTagClick(tag)"
              >
                {{ tag.text }}
              </el-tag>
            </div>
          </div>

          <div class="advanced-filters">
            <div class="filters-header" @click="toggleAdvancedFilters">
              <span>高级筛选</span>
              <el-icon :class="{ 'rotate-180': showFilters }"><ArrowDown /></el-icon>
            </div>
            <el-collapse-transition>
              <div v-show="showFilters" class="filters-content">
                <el-row :gutter="20">
                  <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
                    <div class="filter-item">
                      <label class="filter-label">学科门类</label>
                      <el-select v-model="filters.disciplineId" placeholder="选择学科门类" size="small" clearable class="filter-select">
                        <el-option v-for="discipline in disciplines" :key="discipline.id" :label="discipline.name" :value="discipline.id" />
                      </el-select>
                    </div>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
                    <div class="filter-item">
                      <label class="filter-label">思政类型</label>
                      <el-select v-model="filters.elementTypeId" placeholder="选择思政类型" size="small" clearable class="filter-select">
                        <el-option v-for="type in ideologyTypes" :key="type.id" :label="type.name" :value="type.id" />
                      </el-select>
                    </div>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
                    <div class="filter-item">
                      <label class="filter-label">课程类型</label>
                      <el-select v-model="filters.courseTypeId" placeholder="选择课程类型" size="small" clearable class="filter-select">
                        <el-option v-for="type in courseTypes" :key="type.id" :label="type.name" :value="type.id" />
                      </el-select>
                    </div>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
                    <div class="filter-item">
                      <label class="filter-label">融合难度</label>
                      <el-select v-model="filters.difficulty" placeholder="选择融合难度" size="small" clearable class="filter-select">
                        <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
                      </el-select>
                    </div>
                  </el-col>
                </el-row>

                <div class="keyword-filters">
                  <label class="filter-label">关键词快捷搜索</label>
                  <div class="keyword-tags">
                    <el-tag
                      v-for="tag in suggestedTags"
                      :key="tag.id"
                      class="keyword-tag clickable"
                      @click="handleKeywordQuickSearch(tag.text)"
                    >
                      {{ tag.text }}
                    </el-tag>
                  </div>
                </div>

                <div class="favorite-filter">
                  <el-checkbox v-model="filters.onlyFavorite">仅看我的收藏</el-checkbox>
                </div>

                <div class="filter-actions">
                  <el-button type="primary" plain @click="handleFilterSearch">
                    <el-icon><Filter /></el-icon>
                    筛选检索
                  </el-button>
                  <el-button @click="resetFilters">
                    <el-icon><Refresh /></el-icon>
                    重置筛选
                  </el-button>
                </div>
              </div>
            </el-collapse-transition>
          </div>
        </div>
      </el-card>
    </div>

    <div class="ai-recommendation" v-if="showAIRecommendation && aiRecommendations.length > 0">
      <el-card class="recommendation-card" shadow="never">
        <template #header>
          <div class="recommendation-header">
            <div class="header-left">
              <h3>智能推荐</h3>
              <span class="ai-subtitle">基于热门元素、收藏偏好与当前教师画像生成</span>
            </div>
            <el-button type="text" @click="refreshRecommendations">
              <el-icon><Refresh /></el-icon>
              换一批
            </el-button>
          </div>
        </template>
        <div class="recommendation-content">
          <div class="recommendation-items">
            <div v-for="item in aiRecommendations" :key="item.id" class="recommendation-item" @click="viewElementDetail(item)">
              <div class="recommendation-icon">
                <el-icon :size="24" :color="item.color"><component :is="item.icon" /></el-icon>
              </div>
              <div class="recommendation-info">
                <div class="recommendation-title">{{ item.title }}</div>
                <div class="recommendation-desc">{{ item.description }}</div>
                <div class="recommendation-tags">
                  <el-tag size="small" v-for="tag in item.keywords.slice(0, 4)" :key="tag">{{ tag }}</el-tag>
                </div>
              </div>
              <div class="recommendation-actions">
                <el-button type="text" size="small" @click.stop="addToCollection(item)">
                  <el-icon><Star /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="search-results">
      <div class="results-header" v-if="hasSearched">
        <div class="results-info">
          <h3 v-if="total > 0">
            找到 {{ total }} 个相关思政元素
            <span class="search-query" v-if="searchQuery">“{{ searchQuery }}”</span>
          </h3>
          <h3 v-else>未找到相关结果</h3>
          <div class="sort-options">
            <el-select v-model="sortBy" size="small" placeholder="排序方式" @change="changeSort">
              <el-option label="相关度排序" value="relevance" />
              <el-option label="热门度排序" value="popularity" />
              <el-option label="最新添加" value="newest" />
              <el-option label="融合难度" value="difficulty" />
            </el-select>
          </div>
        </div>
        <div class="view-options">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="list"><el-icon><Menu /></el-icon>列表视图</el-radio-button>
            <el-radio-button label="grid"><el-icon><Grid /></el-icon>网格视图</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="results-content">
        <div v-if="viewMode === 'list' && pagedResults.length > 0" class="list-view">
          <div class="ideology-list">
            <div v-for="element in pagedResults" :key="element.id" class="ideology-item" @click="viewElementDetail(element)">
              <div class="item-header">
                <div class="item-title">
                  <h4>{{ element.title }}</h4>
                  <div class="item-meta">
                    <el-tag size="small" :type="getDifficultyType(element.difficulty)">{{ getDifficultyText(element.difficulty) }}</el-tag>
                    <el-tag size="small" type="info">{{ element.ideologyType }}</el-tag>
                  </div>
                </div>
                <div class="item-actions">
                  <el-button type="text" size="small" @click.stop="addToCollection(element)">
                    <el-icon><Star /></el-icon>
                    {{ element.collected ? '已收藏' : '收藏' }}
                  </el-button>
                  <el-button type="text" size="small" @click.stop="applyToCourse(element)">
                    <el-icon><Plus /></el-icon>
                    应用
                  </el-button>
                </div>
              </div>
              <div class="item-content">
                <p class="item-description">{{ element.description }}</p>
                <div class="item-details">
                  <div class="detail-item">
                    <el-icon><Collection /></el-icon>
                    <span>适用课程：{{ element.suitableCourses.join('、') || '暂无' }}</span>
                  </div>
                  <div class="detail-item">
                    <span>推荐指数：{{ element.recommendationScore }}/10</span>
                  </div>
                  <div class="detail-item">
                    <span>热度：{{ element.popularity }}</span>
                  </div>
                </div>
                <div class="item-keywords">
                  <span class="keywords-label">关键词：</span>
                  <el-tag
                    v-for="keyword in element.keywords"
                    :key="keyword"
                    size="small"
                    class="keyword-tag clickable"
                    @click.stop="searchByKeyword(keyword)"
                  >
                    {{ keyword }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="viewMode === 'grid' && pagedResults.length > 0" class="grid-view">
          <el-row :gutter="20">
            <el-col v-for="element in pagedResults" :key="element.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
              <el-card class="ideology-card" shadow="hover" @click="viewElementDetail(element)">
                <div class="card-content">
                  <div class="card-header">
                    <div class="card-icon">
                      <el-icon :size="32" :color="element.color"><component :is="element.icon" /></el-icon>
                    </div>
                    <div class="card-title">
                      <h4>{{ element.title }}</h4>
                      <div class="card-tags">
                        <el-tag size="small" :type="getDifficultyType(element.difficulty)">
                          {{ getDifficultyText(element.difficulty) }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                  <p class="card-description">{{ truncateText(element.description, 80) }}</p>
                  <div class="card-meta">
                    <div class="meta-item">
                      <el-icon><Collection /></el-icon>
                      <span>{{ element.suitableCourses.length }}门课程</span>
                    </div>
                    <div class="meta-item">
                      <el-icon><Star /></el-icon>
                      <span>{{ element.recommendationScore }}/10</span>
                    </div>
                  </div>
                  <div class="card-keywords">
                    <el-tag
                      v-for="keyword in element.keywords.slice(0, 3)"
                      :key="keyword"
                      size="small"
                      class="keyword-tag clickable"
                      @click.stop="searchByKeyword(keyword)"
                    >
                      {{ keyword }}
                    </el-tag>
                    <span v-if="element.keywords.length > 3" class="more-keywords">+{{ element.keywords.length - 3 }}个</span>
                  </div>
                  <div class="card-actions">
                    <el-button type="primary" plain size="small" @click.stop="applyToCourse(element)">
                      <el-icon><Plus /></el-icon>
                      应用
                    </el-button>
                    <el-button :type="element.collected ? 'warning' : 'text'" size="small" @click.stop="addToCollection(element)">
                      <el-icon><Star /></el-icon>
                    </el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <div v-else-if="hasSearched && total === 0" class="empty-results">
          <el-empty description="未找到相关思政元素">
            <div class="empty-tips">
              <p>建议：</p>
              <ul>
                <li>尝试其他关键词或更简短的关键词</li>
                <li>检查筛选条件是否过于严格</li>
                <li>查看首页推荐元素</li>
              </ul>
            </div>
            <el-button type="primary" @click="resetSearch">
              <el-icon><Refresh /></el-icon>
              重新搜索
            </el-button>
          </el-empty>
        </div>

        <div v-else class="initial-state">
          <div class="welcome-message">
            <div class="welcome-icon">
              <el-icon :size="64" color="#409eff"><Search /></el-icon>
            </div>
            <h3>开始您的思政元素检索</h3>
            <p>输入课程名称、专业或关键词，系统将为您推荐适合当前教学场景的思政元素</p>

            <div class="quick-start">
              <h4>快速开始：</h4>
              <div class="quick-options">
                <div class="quick-option" @click="handleQuickSearch('计算机')">
                  <el-icon><Monitor /></el-icon>
                  <span>计算机专业</span>
                </div>
                <div class="quick-option" @click="handleQuickSearch('人工智能')">
                  <el-icon><Cpu /></el-icon>
                  <span>人工智能</span>
                </div>
                <div class="quick-option" @click="handleQuickSearch('创新精神')">
                  <el-icon><Lightning /></el-icon>
                  <span>创新精神</span>
                </div>
                <div class="quick-option" @click="handleQuickSearch('工程伦理')">
                  <el-icon><Setting /></el-icon>
                  <span>工程伦理</span>
                </div>
              </div>
            </div>

            <div class="favorite-entry" v-if="searchStats.favoriteCount > 0">
              <el-button type="warning" plain @click="openMyFavoriteSearch">查看我的收藏元素</el-button>
            </div>
          </div>
        </div>

        <div class="pagination-container" v-if="hasSearched && total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36, 48]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="showDetailDialog" title="思政元素详情" width="760px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-dialog" v-if="detailData">
        <div class="detail-title-row">
          <div class="detail-title-left">
            <el-icon :size="28" :color="detailData.color"><component :is="detailData.icon" /></el-icon>
            <div>
              <h3>{{ detailData.title }}</h3>
              <div class="detail-tags">
                <el-tag :type="getDifficultyType(detailData.difficulty)">{{ getDifficultyText(detailData.difficulty) }}</el-tag>
                <el-tag type="info">{{ detailData.ideologyType }}</el-tag>
              </div>
            </div>
          </div>
          <el-button :type="detailData.collected ? 'warning' : 'primary'" plain @click="addToCollection(detailData)">
            <el-icon><Star /></el-icon>
            {{ detailData.collected ? '取消收藏' : '加入收藏' }}
          </el-button>
        </div>

        <div class="detail-section">
          <div class="section-title">摘要</div>
          <div class="section-text">{{ detailData.summary || '暂无摘要' }}</div>
        </div>

        <div class="detail-section">
          <div class="section-title">详细内容</div>
          <div class="section-text content-text">{{ detailData.content || '暂无内容' }}</div>
        </div>

        <div class="detail-grid">
          <div class="detail-section">
            <div class="section-title">适用课程</div>
            <div class="section-tags">
              <el-tag v-for="course in detailData.suitableCourses" :key="course">{{ course }}</el-tag>
            </div>
          </div>
          <div class="detail-section">
            <div class="section-title">适用学科</div>
            <div class="section-tags">
              <el-tag v-for="item in detailData.disciplines" :key="item">{{ item }}</el-tag>
            </div>
          </div>
          <div class="detail-section">
            <div class="section-title">课程类型</div>
            <div class="section-tags">
              <el-tag v-for="item in detailData.courseTypes" :key="item">{{ item }}</el-tag>
            </div>
          </div>
          <div class="detail-section">
            <div class="section-title">关键词</div>
            <div class="section-tags">
              <el-tag
                v-for="keyword in detailData.keywords"
                :key="keyword"
                class="clickable"
                @click="searchByKeyword(keyword)"
              >
                {{ keyword }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="detail-stats">
          <div class="detail-stat-item">
            <div class="detail-stat-value">{{ detailData.viewCount }}</div>
            <div class="detail-stat-label">浏览次数</div>
          </div>
          <div class="detail-stat-item">
            <div class="detail-stat-value">{{ detailData.favoriteCount }}</div>
            <div class="detail-stat-label">收藏次数</div>
          </div>
          <div class="detail-stat-item">
            <div class="detail-stat-value">{{ detailData.useCount }}</div>
            <div class="detail-stat-label">使用次数</div>
          </div>
          <div class="detail-stat-item">
            <div class="detail-stat-value">{{ detailData.popularity }}</div>
            <div class="detail-stat-label">热度</div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDetailDialog = false">关闭</el-button>
          <el-button type="primary" @click="applyToCourse(detailData)">应用到课程</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="showApplyDialog" title="应用到课程" width="500px" :close-on-click-modal="false">
      <div class="apply-dialog">
        <el-form :model="applyForm" label-width="80px">
          <el-form-item label="选择课程">
            <el-select v-model="applyForm.courseId" placeholder="请选择要应用的课程" style="width: 100%">
              <el-option v-for="course in myCourses" :key="course.id" :label="course.name" :value="course.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="应用方式">
            <el-radio-group v-model="applyForm.method">
              <el-radio label="full">完整应用</el-radio>
              <el-radio label="reference">参考借鉴</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="applyForm.notes" type="textarea" :rows="3" placeholder="可选：添加应用说明或修改建议" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showApplyDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmApply">确认应用</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.ideology-search {
  min-height: calc(100vh - 64px);
}

.page-header {
  background: linear-gradient(135deg, #9cb2fa 0%, #80a2ff 100%);
  border-radius: 8px;
  padding: 20px;
  color: #fff;
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
}

.search-section {
  margin-bottom: 15px;
  .search-card {
    border-radius: 5px;
    &:deep(.el-card__body) {
      padding: 15px;
    }
  }
}

.search-container {
  .main-search {
    margin-bottom: 10px;
    .search-input {
      :deep(.el-input-group__append) {
        background-color: #409eff;
        border-color: #409eff;
        .el-button {
          color: white;
        }
      }
    }
  }

  .suggested-tags {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    flex-wrap: wrap;

    .tags-title {
      color: #606266;
      font-size: 14px;
      white-space: nowrap;
    }
  }
  .tags-list,.keyword-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .suggested-tag,.keyword-tag {
    cursor: pointer;
    transition: all 0.3s;
    &:hover {
      transform: translateY(-2px);
    }
  }
}

.advanced-filters {
  .filters-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #409eff;
    cursor: pointer;
    padding: 12px 0;
    border-top: 1px solid #ebeef5;
  }

  .rotate-180 {
    transform: rotate(180deg);
    transition: transform 0.3s;
  }

  .filters-content {
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
  }

  .filter-item {
    margin-bottom: 12px;
    .filter-label {
      display: block;
      margin-bottom: 8px;
      font-size: 14px;
      color: #606266;
      font-weight: 500;
    }
    .filter-select {
      width: 100%;
    }
  }

  .favorite-filter {
    margin: 8px 0 14px;
  }

  .filter-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
  }
}

.ai-recommendation {
  margin-bottom: 15px;
  .recommendation-card {
    border-radius: 5px;
    &:deep(.el-card__header) {
      padding: 15px;
    }
    &:deep(.el-card__body) {
      padding: 15px;
    }
  }
}
.recommendation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0 0 4px;
  }

  .ai-subtitle {
    color: #909399;
    font-size: 13px;
  }
}

.recommendation-items {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.recommendation-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  }

  .recommendation-info {
    flex: 1;
  }

  .recommendation-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 5px;
  }

  .recommendation-desc {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
  }

  .recommendation-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;

  .results-info {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
    h3 {
      margin: 0;
    }

    .search-query {
      color: #409eff;
      font-weight: 500;
    }
  }
}

.ideology-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ideology-item {
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  background: white;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: #409eff;
  }

  .item-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 10px;
  }

  .item-title h4 {
    margin: 0 0 8px;
    font-size: 18px;
  }

  .item-meta,.item-keywords,.detail-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 14px;
  }

  .item-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }

  .item-description {
    margin: 0 0 12px;
    color: #606266;
    line-height: 1.5;
  }

  .item-details {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 12px;
    color: #606266;
  }

  .detail-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.ideology-card {
  border-radius: 8px;

  .card-header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }

  .card-title h4 {
    margin: 0 0 8px;
    font-size: 16px;
    color: #303133;
    font-weight: 600;
    line-height: 1.4;
  }

  .card-description {
    color: #606266;
    line-height: 1.5;
    min-height: 66px;
    margin: 0 0 8px;
  }

  .card-meta {
    display: flex;
    justify-content: space-between;
    margin: 12px 0;
    color: #606266;
  }

  .meta-item, .card-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .card-keywords {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    min-height: 32px;
    margin-bottom: 12px;
  }

  .card-actions {
    justify-content: space-between;
  }
}

.initial-state,.empty-results {
  padding: 24px 0;
}

.welcome-message {
  text-align: center;

  h3 {
    margin: 0;
    font-size: 24px;
  }

  p {
    color: #606266;
  }
}

.quick-start {
  margin-top: 0px;
}

.quick-options {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.quick-option {
  width: 140px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
    transform: translateY(-2px);
  }
}

.favorite-entry {
  margin-top: 20px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-dialog {
  .detail-title-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .detail-title-left {
    display: flex;
    gap: 12px;
    align-items: flex-start;
  }

  h3 {
    margin: 0 0 8px;
    font-size: 20px;
  }

  .detail-section {
    margin-bottom: 16px;
  }

  .section-title {
    font-weight: 600;
    margin-bottom: 8px;
    color: #303133;
  }

  .section-text {
    color: #606266;
    line-height: 1.9;
  }

  .content-text {
    white-space: pre-wrap;
  }

  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .section-tags {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .detail-stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    margin-top: 8px;
  }

  .detail-stat-item {
    text-align: center;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 12px;
  }

  .detail-stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #409eff;
  }

  .detail-stat-label {
    margin-top: 4px;
    color: #909399;
    font-size: 13px;
  }
}

.clickable {
  cursor: pointer;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .recommendation-items {
    grid-template-columns: 1fr;
  }

  .detail-dialog {
    .detail-grid,
    .detail-stats {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 768px) {
  .results-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .item-header {
    flex-direction: column;
  }
}
</style>
