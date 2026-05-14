<script setup>
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { Star, Clock, User, ArrowRight, VideoPlay, InfoFilled, TrendCharts, Reading, Refresh } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router' 
const router = useRouter()

import { sceneListService, sceneCategoriesService, createQuizService, recentRecordsService, recentReportsService} from '@/api/test.js'

const allScenarios = ref([])
const sceneCategories = ref([])
const selectedScenarioId = ref('')
const testRecords = ref([])
const testReports = ref([])

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_APP_BASE_API || 'http://localhost:8081'
const getImageUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  if (url.startsWith('//')) return `http:${url}`
  return `${API_BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}

const loadRecordList = async () => {
  try {
    const res = await recentRecordsService()
    testRecords.value = res.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '测试记录加载失败，请稍后重试')
  }
}

const loadReportList = async () => {
  try {
    const res = await recentReportsService()
    testReports.value = res.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '报告列表加载失败，请稍后重试')
  }
}

const loadCategories = async () => {
  try {
    const result = await sceneCategoriesService()
    sceneCategories.value = result.data
  } catch (e) {
    ElMessage.error(e?.message || '情景分类数据加载失败')
  }
}

const loadScenes = async () => {
  try {
    const result = await sceneListService()
    allScenarios.value = result.data
  } catch (e) {
    ElMessage.error(e?.message || '情景数据加载失败')
  }
}

const findCategoryName = (categoryId) => {
  if (!categoryId || !sceneCategories.value.length) return ''
  
  const category = sceneCategories.value.find(cat => cat.id === categoryId)
  return category ? category.name : ''
}

const getDifficultyType = (difficulty) => {
  const types = ['success', 'warning', 'danger']
  return types[difficulty - 1] || 'info'
}
const getDifficultyText = (difficulty) => {
  const texts = ['简单', '中等', '困难']
  return texts[difficulty - 1] || '未知'
}

const getScoreType = (score, totalScore) => {
  if (score >= totalScore*0.8) return 'success'
  if (score >= totalScore*0.6) return 'warning'
  return 'danger'
}
  
const formatParticipants = (num) => {
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}万`
  }
  return num
}
  
const formatTime = (timeStr) => {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  // 如果是今天
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 如果是昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }
  
  // 一周内
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }
  
  // 显示日期
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const viewScenarioDetail = (scenarioId) => {
  router.push({ path: '/test/scene', query: { id: scenarioId } })
}
  
const startTest = (scenarioId) => {
  ElMessageBox.confirm(
    '开始测试后，请注意：\n\n1. 测试中途退出将无法保存进度\n2. 建议在无干扰环境下完成\n3. 预计用时：15-20分钟\n\n是否确认开始测试？',
    '开始情景测试',
    {
      confirmButtonText: '开始测试',
      cancelButtonText: '再考虑下',
      type: 'warning',
      customClass: 'start-test-dialog'
    }
  ).then(async () => {
    const loading = ElLoading.service({
      lock: true,
      text: '正在自动生成测试题目，请耐心等待...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    try {
      // 创建答卷（后端返回：{qnId,title,totalScore,totalCount}）
      const result = await createQuizService(scenarioId)
      const qnId = result.data.qnId
      const title = result.data.title
      const totalScore = result.data.totalScore
      const totalCount = result.data.totalCount

      ElMessage.success('生成题目成功，即将进入答题...')
      router.push({ path: '/test/quiz', query: { id: qnId, title: title, totalScore: totalScore, totalCount: totalCount} })
    } catch (e) {
      console.error(e)
      ElMessage.error(e?.message || '创建测试失败，请稍后重试')
    } finally {
      loading.close()
    }
  }).catch(() => {
    ElMessage.info('已取消测试')
  })
}

const startQuickTest = () => {
  if (!selectedScenarioId.value) {
    ElMessage.warning('请先选择测试情景')
    return
  }
    
  startTest(selectedScenarioId.value)
}
  
const startFirstTest = () => {
  const firstScene = allScenarios.value[0]
  if (firstScene) {
    startTest(firstScene.id)
  } else {
    ElMessage.warning('暂无推荐情景')
  }
}
  
const viewQuizResult = (qnId) => {
  ElMessage.info('查看测试详情')
  router.push({ path: '/test/result', query: { id: qnId } })
}

const viewReportDetail = (rId) => {
  ElMessage.info('查看此次测试报告详情')
  router.push({ path: '/report/detail', query: { id: rId } })
}

onMounted(() => {
  try {
    loadCategories()
    loadRecordList()
    loadReportList()
    loadScenes()
  } catch (e) {
    ElMessage.error(e?.message || '情景分类数据加载失败')
  }
})
</script>

<template>
    <div class="scenario-home">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
          <div class="scenario-section">
            <div class="section-header">
              <h2 class="section-title"><el-icon><Star /></el-icon>推荐情景</h2>
              <div class="section-description">精选典型师德情境，帮助您掌握职业伦理决策要点</div>
            </div>

            <el-row :gutter="20" class="scenario-grid">
              <el-col  v-for="scenario in allScenarios.slice(0, 4)" :key="scenario.id"
                :xs="24" :sm="12":md="12":lg="12" :xl="12">
                <el-card class="scenario-card" shadow="hover" :body-style="{ padding: '0' }" @click="viewScenarioDetail(scenario.id)">
                  <div class="scenario-image-container">
                    <img :src="getImageUrl(scenario.imgUrl)" :alt="scenario.title" class="scenario-image"/>
                    <div class="scenario-overlay">
                      <div class="scenario-tags">
                        <el-tag size="small" :type="getDifficultyType(scenario.difficulty)">{{ getDifficultyText(scenario.difficulty) }}</el-tag>
                        <el-tag size="small" type="info">{{ findCategoryName(scenario.categoryId) }}</el-tag>
                      </div>
                    </div>
                  </div>
                  <div class="scenario-content">
                    <h3 class="scenario-title">{{ scenario.title }}</h3>
                    <p class="scenario-description">{{ scenario.description }}</p>
                    <div class="scenario-meta">
                      <span class="meta-item">
                        <el-icon><Clock /></el-icon>{{ scenario.estimatedTime }}分钟
                      </span>
                      <span class="meta-item">
                        <el-icon><User /></el-icon> {{ formatParticipants(scenario.participants) }}人参与
                      </span>
                    </div>
                    <div class="scenario-actions">
                      <el-button type="primary" plain size="small" @click.stop="viewScenarioDetail(scenario.id)">查看详情</el-button>
                      <el-button type="primary" size="small" @click.stop="startTest(scenario.id)">开始测试</el-button>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
          <el-card class="sidebar-card quick-test-card" shadow="never">
            <template #header>
              <div class="card-header">
                <h3>快速测试</h3>
              </div>
            </template>
            <div class="card-content">
              <div class="test-selector">
                <div class="selector-label">选择测试情景</div>
                <el-select v-model="selectedScenarioId" placeholder="请选择情景" size="large" class="scenario-select">
                  <el-option v-for="scenario in allScenarios":key="scenario.id" :label="scenario.title" :value="scenario.id">
                    <div class="scenario-option">
                      <span class="option-title">{{ scenario.title }}</span>
                      <span class="option-meta">
                        <el-tag size="small" :type="getDifficultyType(scenario.difficulty)">{{ getDifficultyText(scenario.difficulty) }}</el-tag>
                      </span>
                    </div>
                  </el-option>
                </el-select>
                <div class="test-tips">
                  <el-icon><InfoFilled /></el-icon>
                  <span>选择情景后可直接开始测试</span>
                </div>
              </div>
              <el-button type="primary" size="large" :disabled="!selectedScenarioId" @click="startQuickTest()" class="start-test-btn">
                开始情景测试
              </el-button>
            </div>
          </el-card>

          <el-card class="sidebar-card test-history-card" shadow="never">
            <template #header>
              <div class="card-header">
                <h3>测试记录</h3>
                <el-tag v-if="testRecords.length > 0" type="info" size="small">最近{{ testRecords.length }}次</el-tag>
                <el-button type="text" @click="router.push('/test/list')">查看全部记录 <el-icon><ArrowRight /></el-icon></el-button>
              </div>
            </template>
            <div class="card-content">
              <div v-if="testRecords.length > 0" class="history-list">
                <div v-for="record in testRecords" :key="record.id"
                  class="history-item" @click="viewQuizResult(record.id)"
                >
                  <div class="history-main">
                    <div class="history-title">{{ record.title }}</div>
                    <div class="history-time">{{ formatTime(record.submittedAt) }}</div>
                  </div>
                  <div class="history-score">
                    <el-tag size="small" :type="getScoreType(record.userTotalScore, record.totalScore)"> {{ record.userTotalScore }}分</el-tag>
                  </div>
                </div>
              </div>
              <div v-else class="empty-history">
                <el-empty description="暂无测试记录" :image-size="80">
                  <el-button type="primary" @click="startFirstTest">
                    开始第一次测试
                  </el-button>
                </el-empty>
              </div>
            </div>
          </el-card>

          <el-card class="sidebar-card test-history-card" shadow="never">
            <template #header>
              <div class="card-header">
                <h3>诊断报告</h3>
                <el-tag v-if="testReports.length > 0" type="info" size="small">最近{{ testReports.length }}次</el-tag>
                <el-button type="text" @click="router.push('/report/list')">查看更多报告 <el-icon><ArrowRight /></el-icon></el-button>
              </div>
            </template>
            <div class="card-content">
              <div v-if="testReports.length > 0" class="history-list">
                <div v-for="r in testReports" :key="r.id"
                  class="history-item" @click="viewReportDetail(r.id)"
                >
                  <div class="history-main">
                    <div class="history-title">{{ r.updatedAt }}</div>
                    <div class="history-time">{{ formatTime(r.updatedAt) }}</div>
                  </div>
                  <div class="history-score">
                    <el-tag size="small" :type="getScoreType(r.overallScore, 100)"> {{ r.overallScore }}分</el-tag>
                  </div>
                </div>
              </div>
              <div v-else class="empty-history">
                <el-empty description="暂无诊断报告" :image-size="80">
                  <el-button type="primary" @click="startFirstTest">
                    去测试并生成报告
                  </el-button>
                </el-empty>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </template>

<style lang="scss" scoped>
.scenario-home {
    min-height: calc(100vh - 64px);
}

.scenario-section {
    background: white;
    border-radius: 5px;
    padding: 10px 20px;
    
    .section-header {
      margin-bottom: 15px;
      .section-title {
        display: flex;
        align-items: center;
        gap: 15px;
        margin: 0 0 5px;
        font-size: 20px;
        color: #303133;
        font-weight: 600;
        .el-icon {
          color: #ffc107;
        }
      }
      
      .section-description {
        color: #909399;
        font-size: 12px;
      }
    }
}
  
.scenario-grid {
    .scenario-card {
      margin-bottom: 20px;
      border-radius: 8px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 20px rgba(0, 0, 0, 0.1);
        
        .scenario-image {
          transform: scale(1.05);
        }
      }
      .scenario-image-container {
        position: relative;
        height: 200px;
        overflow: hidden;
        .scenario-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s ease;
        }
        .scenario-overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: linear-gradient(to bottom, transparent 60%, rgba(0, 0, 0, 0.5));
          .scenario-tags {
            position: absolute;
            top: 12px;
            right: 12px;
            display: flex;
            gap: 6px;
          }
        }
      }
      .scenario-content {
        padding: 15px;
        .scenario-title {
          margin: 0 0 5px;
          font-size: 18px;
          color: #303133;
          font-weight: 600;
          line-height: 1.4;
        }
        
        .scenario-description {
          margin: 0 0 5px;
          font-size: 14px;
          color: #606266;
          line-height: 1.6;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        
        .scenario-meta {
          display: flex;
          gap: 16px;
          margin-bottom: 15px;
          
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
        
        .scenario-actions {
          display: flex;
          gap: 10px;
          
          .el-button {
            flex: 1;
          }
        }
      }
    }
}

.sidebar-card {
    margin-bottom: 15px;
    border-radius: 5px;
    
    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;
    }
    &:deep(.el-card__body) {
      padding: 10px 20px;
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
  
.quick-test-card {
    .card-content {
      .test-selector {
        margin-bottom: 10px;
        
        .selector-label {
          margin-bottom: 8px;
          font-size: 14px;
          color: #606266;
          font-weight: 500;
        }
        
        .scenario-select {
          width: 100%;
          
          .scenario-option {
            display: flex;
            justify-content: space-between;
            align-items: center;
            .option-title {
              flex: 1;
            }
            .option-meta {
              margin-left: 8px;
            }
          }
        }
        
        .test-tips {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
          
          .el-icon {
            font-size: 14px;
          }
        }
      }
      
      .start-test-btn {
        width: 100%;
        padding: 12px 0;
        font-weight: 500;
        
        .el-icon {
          margin-right: 6px;
        }
      }
    }
}
  
.test-history-card {
    .history-list {
      .history-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px;
        margin-bottom: 8px;
        border-radius: 8px;
        background: #f8f9fa;
        cursor: pointer;
        transition: all 0.3s ease;
        
        &:hover {
          background: #f0f2f5;
        }
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .history-main {
          flex: 1;
          
          .history-title {
            font-size: 14px;
            color: #303133;
            font-weight: 500;
            margin-bottom: 4px;
          }
          
          .history-time {
            font-size: 12px;
            color: #909399;
          }
        }
        
        .history-score {
          .el-tag {
            min-width: 60px;
            text-align: center;
          }
        }
      }
    }
    
    .empty-history {
      padding: 0;
    }
}

@media (max-width: 768px) {
    .scenario-home {
      padding: 10px;
    }
    
    .scenario-section {
      padding: 16px;
    }
    
    .scenario-card {
      margin-bottom: 16px;
    }
    
    .scenario-image-container {
      height: 180px !important;
    }
    
    .scenario-content {
      padding: 16px !important;
    }
}
</style>
  
<style lang="scss">
.start-test-dialog {
    .el-message-box__content {
      padding: 20px;
    }
    
    .el-message-box__message {
      line-height: 1.6;
      white-space: pre-line;
    }
}
</style>