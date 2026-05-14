<script setup>
import {
  Download, Refresh, Clock, Check, Trophy, TrendCharts, ArrowRight, Document, Lightning, Warning,
  Plus, Share, User, Setting, Aim, DataAnalysis, MagicStick, Medal, InfoFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getEvaluationHomeService } from '@/api/simulate.js'

const router = useRouter()

const comparisonChartRef = ref(null)
const radarChartRef = ref(null)
const patternChartRef = ref(null)
const trendChartRef = ref(null)
let comparisonChart = null
let radarChart = null
let patternChart = null
let trendChart = null

const comparisonGroup = ref('department')
const generatingReport = ref(false)
const pageLoading = ref(false)

const createDefaultAssessmentData = () => ({
  overallScore: 0,
  decisionSpeed: 0,
  decisionQuality: 0,
  moralJudgment: 0,
  scenariosCompleted: 0,
  correctDecisionsRate: 0,
  ranking: '--',
  improvement: 0,
  percentile: 0,
  dimensions: [],
  scenarioPerformances: [],
  decisionPattern: {
    primaryStyle: '--',
    consistency: 0,
    avgTime: 0
  },
  strengths: [],
  improvements: [],
  learningDays: 0,
  criticalMoments: [],
  focusAreas: []
})

const assessmentData = ref(createDefaultAssessmentData())

const developmentPlan = ref([
  {
    id: 1,
    title: '基础能力强化',
    duration: '第1-10天',
    status: 'in-progress',
    objectives: [
      '完成危机识别专项训练',
      '学习风险评估方法论',
      '掌握快速决策技巧'
    ],
    resources: [
      { id: 1, name: '危机管理实战案例集' },
      { id: 2, name: '决策风险评估指南' }
    ]
  },
  {
    id: 2,
    title: '专项技能提升',
    duration: '第11-20天',
    status: 'pending',
    objectives: [
      '参与复杂情景模拟演练',
      '学习专家决策模式',
      '完成决策效果评估训练'
    ],
    resources: [
      { id: 3, name: '高级决策心理学' },
      { id: 4, name: '领导力提升课程' }
    ]
  },
  {
    id: 3,
    title: '综合应用实践',
    duration: '第21-30天',
    status: 'pending',
    objectives: [
      '综合处理多类型师德事件',
      '优化个人决策模式',
      '形成决策反思习惯'
    ],
    resources: [
      { id: 5, name: '师德治理最佳实践' },
      { id: 6, name: '决策反思模板' }
    ]
  }
])

const getScoreLevel = (score) => {
  const s = Number(score || 0)
  if (s >= 90) return '优秀'
  if (s >= 80) return '良好'
  if (s >= 70) return '中等'
  return '待提升'
}

const getScoreColor = (score) => {
  const s = Number(score || 0)
  if (s >= 90) return '#67c23a'
  if (s >= 80) return '#e6a23c'
  if (s >= 70) return '#409eff'
  return '#f56c6c'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return String(dateStr).slice(5, 10)
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const formatTime = (timeStr) => {
  if (!timeStr) return '--'
  const date = new Date(timeStr)
  if (Number.isNaN(date.getTime())) return String(timeStr).slice(11, 16)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const getDecisionType = (decision) => {
  const text = String(decision || '')
  if (text.includes('及时') || text.includes('规范') || text.includes('依法') || text.includes('沟通')) return 'success'
  if (text.includes('风险') || text.includes('预警') || text.includes('审慎')) return 'warning'
  return 'info'
}

const getMomentType = (impact) => {
  const map = { positive: 'success', neutral: 'primary', negative: 'danger' }
  return map[impact] || 'info'
}

const getMomentIcon = (type) => {
  const map = { crisis: Warning, conflict: User, ethics: Medal }
  return map[type] || InfoFilled
}

const getMomentImpactType = (impact) => {
  const map = { positive: 'success', neutral: 'info', negative: 'danger' }
  return map[impact] || 'info'
}

const getMomentImpactText = (impact) => {
  const map = { positive: '正面影响', neutral: '中性影响', negative: '负面影响' }
  return map[impact] || '未知'
}

const getPhaseStatusText = (status) => {
  const map = { completed: '已完成', 'in-progress': '进行中', pending: '待开始' }
  return map[status] || '未知'
}

const tableRowClassName = ({ row }) => {
  const score = Number(row.score || 0)
  if (score >= 85) return 'excellent-row'
  if (score >= 70) return 'good-row'
  return 'improve-row'
}

const disposeCharts = () => {
  if (comparisonChart) {
    comparisonChart.dispose()
    comparisonChart = null
  }
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
  if (patternChart) {
    patternChart.dispose()
    patternChart = null
  }
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
}

const initCharts = () => {
  if (comparisonChartRef.value) {
    if (!comparisonChart) comparisonChart = echarts.init(comparisonChartRef.value)
    updateComparisonChart()
  }
  if (radarChartRef.value) {
    if (!radarChart) radarChart = echarts.init(radarChartRef.value)
    updateRadarChart()
  }
  if (patternChartRef.value) {
    if (!patternChart) patternChart = echarts.init(patternChartRef.value)
    updatePatternChart()
  }
  if (trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value)
    updateTrendChart()
  }
}

const updateComparisonChart = () => {
  if (!comparisonChart) return
  const mine = [
    Number(assessmentData.value.decisionSpeed || 0),
    Number(assessmentData.value.decisionQuality || 0),
    Number(assessmentData.value.moralJudgment || 0),
    Number(assessmentData.value.dimensions?.[0]?.score || 0),
    Number(assessmentData.value.dimensions?.[1]?.score || 0)
  ]
  const peerFactor = comparisonGroup.value === 'expert' ? 1.08 : comparisonGroup.value === 'university' ? 0.96 : 0.92
  const peer = mine.map(v => Math.max(40, Math.min(100, Math.round(v * peerFactor))))

  comparisonChart.setOption({
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c}分' },
    radar: {
      indicator: [
        { name: '决策速度', max: 100 },
        { name: '决策质量', max: 100 },
        { name: '道德判断', max: 100 },
        { name: '危机识别', max: 100 },
        { name: '沟通协调', max: 100 }
      ],
      center: ['50%', '50%'],
      radius: '60%'
    },
    series: [{
      name: '对比分析',
      type: 'radar',
      data: [
        {
          value: mine,
          name: '您的表现',
          itemStyle: { color: '#409eff' },
          areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }
        },
        {
          value: peer,
          name: comparisonGroup.value === 'expert' ? '专家水平' : comparisonGroup.value === 'university' ? '全校平均' : '院系平均',
          itemStyle: { color: '#e6a23c' },
          areaStyle: { color: 'rgba(230, 162, 60, 0.2)' }
        }
      ]
    }]
  })
}

const updateRadarChart = () => {
  if (!radarChart) return
  const dimensions = (assessmentData.value.dimensions || []).slice(0, 5)
  radarChart.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator: dimensions.map(dim => ({ name: dim.name, max: 100 })),
      center: ['50%', '50%'],
      radius: '65%',
      splitArea: { areaStyle: { color: ['rgba(240, 242, 245, 0.8)', 'rgba(240, 242, 245, 0.2)'] } },
      splitLine: { lineStyle: { color: '#dcdfe6' } },
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: dimensions.map(d => Number(d.score || 0)),
        name: '能力维度',
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
        lineStyle: { width: 2 }
      }],
      symbol: 'circle',
      symbolSize: 6
    }]
  })
}

const updatePatternChart = () => {
  if (!patternChart) return
  const styleName = assessmentData.value.decisionPattern?.primaryStyle || '权衡型决策'
  const styleMap = {
    '审慎型决策': [62, 18, 8, 4, 8],
    '果断型决策': [20, 60, 8, 4, 8],
    '民主型决策': [18, 14, 56, 4, 8],
    '直觉型决策': [16, 14, 10, 52, 8],
    '分析型决策': [18, 16, 10, 6, 50],
    '稳健型决策': [60, 18, 10, 4, 8],
    '权衡型决策': [22, 18, 42, 6, 12],
    '激进型决策': [18, 56, 8, 10, 8]
  }
  const chartData = styleMap[styleName] || [22, 18, 42, 6, 12]

  patternChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['审慎型', '果断型', '民主型', '直觉型', '分析型'],
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      name: '使用频率',
      type: 'bar',
      barWidth: '60%',
      itemStyle: { color: '#409eff' },
      data: chartData
    }]
  })
}

const updateTrendChart = () => {
  if (!trendChart) return
  const rows = [...(assessmentData.value.scenarioPerformances || [])].reverse()
  trendChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: rows.map(item => formatDate(item.completedAt)),
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      name: '决策能力得分',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#409eff' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0)' }
          ]
        }
      },
      data: rows.map(item => Number(item.score || 0))
    }]
  })
}

const handleResize = () => {
  comparisonChart?.resize()
  radarChart?.resize()
  patternChart?.resize()
  trendChart?.resize()
}

const loadHomeData = async () => {
  pageLoading.value = true
  try {
    const res = await getEvaluationHomeService()
    assessmentData.value = Object.assign(createDefaultAssessmentData(), res.data || {})
    await nextTick()
    initCharts()
  } catch (err) {
    console.error('获取评估主页数据失败', err)
    ElMessage.error('获取评估主页数据失败')
  } finally {
    pageLoading.value = false
  }
}

const generateReport = () => {
  generatingReport.value = true
  ElMessage.success('评估报告生成中，请稍候...')
  setTimeout(() => {
    generatingReport.value = false
    ElMessage.success('当前版本已完成主页联调，导出功能可继续接后端文件流接口')
  }, 800)
}

const retakeAssessment = () => {
  ElMessageBox.confirm(
    '重新评估将使用新的情景测试，确定要重新开始吗？',
    '重新评估确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    router.push('/study/chat')
    ElMessage.success('即将开始新的评估')
  })
}

const viewDetailedAnalysis = () => {
  const row = assessmentData.value.scenarioPerformances?.[0]
  if (!row?.sessionId) {
    ElMessage.info('暂无可查看的评估详情')
    return
  }
  router.push({ path: '/evaluation/detail', query: { sessionId: row.sessionId, evaluationId: row.evaluationId }})
}

const viewScenarioDetail = (row) => {
  if (!row?.sessionId) {
    ElMessage.info('当前记录缺少详情参数')
    return
  }
  router.push({ path: '/evaluation/detail', query: { sessionId: row.sessionId, evaluationId: row.id } })
}

const reviewScenario = (row) => {
  if (!row?.sessionId) {
    ElMessage.info('当前记录缺少会话参数')
    return
  }
  router.push({
    path: '/simulate/chat',
    query: { sessionId: row.sessionId }
  })
}

const viewAllMoments = () => {
  ElMessage.info('已展示最近评估中的关键时刻，可继续按需扩展独立页面')
}

const createLearningPlan = () => {
  ElMessage.success('学习计划创建成功')
}

const shareAssessment = () => {
  ElMessage.info('分享评估结果')
}

const viewResource = (resource) => {
  ElMessage.info(`查看资源：${resource.name}`)
}

onMounted(async () => {
  await loadHomeData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  disposeCharts()
  window.removeEventListener('resize', handleResize)
})

watch(comparisonGroup, () => {
  updateComparisonChart()
})

watch(() => assessmentData.value, async () => {
  await nextTick()
  initCharts()
}, { deep: true })
</script>
  
<template>
    <div class="decision-assessment">
      <div class="page-header">
        <el-card class="action-card" shadow="never">
          <div class="action-content">
            <div class="action-info">
              <h4>评估完成</h4>
              <p>基于{{ assessmentData.scenariosCompleted }}个情景的深度分析，为您提供了个性化的决策能力评估</p>
            </div>
            <div class="action-buttons">
              <el-button type="primary" @click="generateReport" :loading="generatingReport">
                <el-icon><Download /></el-icon>
                下载评估报告
              </el-button>
              <el-button type="success" plain @click="shareAssessment">
                <el-icon><Share /></el-icon>
                分享评估结果
              </el-button>
              <el-button type="info" plain @click="retakeAssessment">
                <el-icon><Refresh /></el-icon>
                重新评估
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <div class="assessment-overview">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="overview-card score-card" shadow="never">
              <div class="overview-content">
                <div class="overall-score">
                  <div class="score-circle">
                    <div class="score-number">{{ assessmentData.overallScore }}</div>
                    <div class="score-label">决策能力得分</div>
                    <div class="score-level">{{ getScoreLevel(assessmentData.overallScore) }}</div>
                  </div>
                </div>
                <div class="score-breakdown">
                  <div class="breakdown-item">
                    <div class="breakdown-label">决策速度</div>
                    <div class="breakdown-value">{{ assessmentData.decisionSpeed }}/100</div>
                    <el-progress 
                      :percentage="assessmentData.decisionSpeed" 
                      :stroke-width="6" 
                      :color="getScoreColor(assessmentData.decisionSpeed)"
                    />
                  </div>
                  <div class="breakdown-item">
                    <div class="breakdown-label">决策质量</div>
                    <div class="breakdown-value">{{ assessmentData.decisionQuality }}/100</div>
                    <el-progress 
                      :percentage="assessmentData.decisionQuality" 
                      :stroke-width="6" 
                      :color="getScoreColor(assessmentData.decisionQuality)"
                    />
                  </div>
                  <div class="breakdown-item">
                    <div class="breakdown-label">道德判断</div>
                    <div class="breakdown-value">{{ assessmentData.moralJudgment }}/100</div>
                    <el-progress 
                      :percentage="assessmentData.moralJudgment" 
                      :stroke-width="6" 
                      :color="getScoreColor(assessmentData.moralJudgment)"
                    />
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="overview-card stats-card" shadow="never">
              <template #header>
                <h3>评估统计数据</h3>
              </template>
              <div class="stats-content">
                <div class="stat-item">
                  <div class="stat-icon">
                    <el-icon><Clock /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ assessmentData.scenariosCompleted }}</div>
                    <div class="stat-label">完成情景数</div>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-icon">
                    <el-icon><Check /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ assessmentData.correctDecisionsRate }}%</div>
                    <div class="stat-label">正确决策率</div>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-icon">
                    <el-icon><Trophy /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ assessmentData.ranking }}</div>
                    <div class="stat-label">在同行中排名</div>
                  </div>
                </div>
                <div class="stat-item">
                  <div class="stat-icon">
                    <el-icon><TrendCharts /></el-icon>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ assessmentData.improvement }}%</div>
                    <div class="stat-label">相比上次提升</div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="overview-card comparison-card" shadow="never">
              <template #header>
                <div class="comparison-header">
                  <h3>与同行对比</h3>
                  <el-select v-model="comparisonGroup" size="small" style="width: 120px">
                    <el-option label="院系同行" value="department" />
                    <el-option label="全校同行" value="university" />
                    <el-option label="专家水平" value="expert" />
                  </el-select>
                </div>
              </template>
              <div class="comparison-content">
                <div class="comparison-chart" ref="comparisonChartRef"></div>
                <div class="comparison-insight">
                  <span>您的决策质量超过了{{ assessmentData.percentile }}%的同行</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
  
      <div class="dimension-analysis">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
            <el-card class="analysis-card" shadow="never">
              <template #header>
                <h3>决策能力维度分析</h3>
                <el-button type="text" @click="viewDetailedAnalysis">
                  查看详细分析 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </template>
              <div class="analysis-content">
                <div class="radar-chart-container">
                  <div class="radar-chart" ref="radarChartRef"></div>
                  <div class="dimension-legend">
                    <div class="legend-item" v-for="dimension in assessmentData.dimensions" :key="dimension.name">
                      <div class="legend-color" :style="{ backgroundColor: dimension.color }"></div>
                      <div class="legend-name">{{ dimension.name }}</div>
                      <div class="legend-score">{{ dimension.score }}/100</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="trend-card" shadow="never">
              <template #header>
                <h3>能力成长趋势</h3>
              </template>
              <div class="trend-content">
                <div class="trend-chart" ref="trendChartRef"></div>
                <div class="trend-summary">
                  <div class="trend-item">
                    <div class="trend-label">最近提升</div>
                    <div class="trend-value">+{{ assessmentData.improvement }}%</div>
                  </div>
                  <div class="trend-item">
                    <div class="trend-label">持续学习</div>
                    <div class="trend-value">{{ assessmentData.learningDays }}天</div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
  
      <div class="scenario-analysis">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
            <el-card class="scenario-performance-card" shadow="never">
              <template #header>
                <h3>情景表现分析</h3>
              </template>
              <div class="scenario-content">
                <el-table :data="assessmentData.scenarioPerformances" style="width: 100%"
                  :row-class-name="tableRowClassName" @row-click="viewScenarioDetail"
                >
                  <el-table-column label="训练主题" width="200">
                    <template #default="{ row }">
                      <div class="scenario-name">
                        <el-icon><Document /></el-icon>
                        <span>{{ row.sessionTitle }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="完成时间" width="120">
                    <template #default="{ row }">
                      {{ formatDate(row.completedAt) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="决策耗时" width="100">
                    <template #default="{ row }">
                      {{ row.decisionTime }}秒
                    </template>
                  </el-table-column>
                  <el-table-column label="决策评分" width="150">
                    <template #default="{ row }">
                      <div class="decision-score">
                        <el-progress  :percentage="row.score" :stroke-width="8" :color="getScoreColor(row.score)" :show-text="false" />
                        <span>{{ row.score }}/100</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="关键结论" width="180">
                    <template #default="{ row }">
                      <div class="key-decisions">
                        <el-tag v-for="decision in row.keyDecisions" :key="decision" size="small" :type="getDecisionType(decision)">
                          {{ decision }}
                        </el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="160">
                    <template #default="{ row }">
                      <el-button type="text" size="small" @click.stop="reviewScenario(row)">回顾</el-button>
                      <el-button type="text" size="small" @click.stop="viewScenarioDetail(row)">查看详情</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="strengths-card" shadow="never">
              <template #header>
                <h3>决策优势</h3>
              </template>
              <div class="strengths-content">
                <div class="strength-list">
                  <div class="strength-item" v-for="strength in assessmentData.strengths" :key="strength.id">
                    <div class="strength-icon">
                      <el-icon><Check /></el-icon>
                    </div>
                    <div class="strength-content">
                      <div class="strength-title">{{ strength.title }}</div>
                      <div class="strength-desc">{{ strength.description }}</div>
                      <div class="strength-evidence" v-if="strength.evidence">
                        证据：{{ strength.evidence }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="scenario-analysis">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
            <el-card class="pattern-analysis-card" shadow="never">
              <template #header>
                <h3>决策模式分析</h3>
              </template>
              <div class="pattern-content">
                <div class="pattern-chart" ref="patternChartRef"></div>
                <div class="pattern-insights">
                  <div class="insight-item">
                    <div class="insight-icon">
                      <el-icon><Lightning /></el-icon>
                    </div>
                    <div class="insight-content">
                      <div class="insight-title">主要决策风格</div>
                      <div class="insight-desc">{{ assessmentData.decisionPattern.primaryStyle }}</div>
                    </div>
                  </div>
                  <div class="insight-item">
                    <div class="insight-icon">
                      <el-icon><TrendCharts /></el-icon>
                    </div>
                    <div class="insight-content">
                      <div class="insight-title">决策一致性</div>
                      <div class="insight-desc">{{ assessmentData.decisionPattern.consistency }}%</div>
                    </div>
                  </div>
                  <div class="insight-item">
                    <div class="insight-icon">
                      <el-icon><Clock /></el-icon>
                    </div>
                    <div class="insight-content">
                      <div class="insight-title">平均决策时间</div>
                      <div class="insight-desc">{{ assessmentData.decisionPattern.avgTime }}秒</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="improvements-card" shadow="never">
              <template #header>
                <h3>改进建议</h3>
              </template>
              <div class="improvements-content">
                <div class="improvement-list">
                  <div class="improvement-item" v-for="improvement in assessmentData.improvements" :key="improvement.id">
                    <div class="improvement-content">
                      <div class="improvement-title">{{ improvement.title }}</div>
                      <div class="improvement-desc">{{ improvement.description }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
  
      <div class="critical-moments">
        <el-card class="moments-card" shadow="never">
          <template #header>
            <h3>关键时刻分析</h3>
            <el-button type="text" @click="viewAllMoments">
              查看全部关键时刻 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </template>
          <div class="moments-content">
            <el-timeline>
              <el-timeline-item
                v-for="moment in assessmentData.criticalMoments"
                :key="moment.id"
                :timestamp="formatTime(moment.timestamp)"
                placement="top"
                :type="getMomentType(moment.impact)"
                :icon="getMomentIcon(moment.type)"
              >
                <div class="moment-item">
                  <div class="moment-header">
                    <span class="moment-scenario">{{ moment.scenario }}</span>
                    <el-tag size="small" :type="getMomentImpactType(moment.impact)">
                      {{ getMomentImpactText(moment.impact) }}
                    </el-tag>
                  </div>
                  <div class="moment-content">
                    <p>{{ moment.description }}</p>
                  </div>
                  <div class="moment-analysis">
                    <div class="analysis-item">
                      <span class="analysis-label">您的决策：</span>
                      <span class="analysis-value">{{ moment.yourDecision }}</span>
                    </div>
                    <div class="analysis-item" v-if="moment.recommendedDecision">
                      <span class="analysis-label">推荐决策：</span>
                      <span class="analysis-value">{{ moment.recommendedDecision }}</span>
                    </div>
                    <div class="analysis-item" v-if="moment.lesson">
                      <span class="analysis-label">经验总结：</span>
                      <span class="analysis-value">{{ moment.lesson }}</span>
                    </div>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </div>
  
      <div class="development-plan">
        <el-card class="plan-card" shadow="never">
          <template #header>
            <h3>个性化发展计划</h3>
            <el-button type="primary" @click="createLearningPlan">
              <el-icon><Plus /></el-icon>
              创建学习计划
            </el-button>
          </template>
          <div class="plan-content">
            <div class="plan-summary">
              <div class="summary-item">
                <div class="summary-label">计划周期</div>
                <div class="summary-value">30天</div>
              </div>
              <div class="summary-item">
                <div class="summary-label">目标提升</div>
                <div class="summary-value">+15%</div>
              </div>
              <div class="summary-item">
                <div class="summary-label">重点能力</div>
                <div class="summary-value">{{ assessmentData.focusAreas.join('、') }}</div>
              </div>
            </div>
            
            <div class="plan-details">
              <div class="plan-phase" v-for="phase in developmentPlan" :key="phase.id">
                <div class="phase-header">
                  <div class="phase-title">
                    <h4>{{ phase.title }}</h4>
                    <div class="phase-duration">{{ phase.duration }}</div>
                  </div>
                  <el-tag :type="phase.status === 'completed' ? 'success' : 'primary'" size="small">
                    {{ getPhaseStatusText(phase.status) }}
                  </el-tag>
                </div>
                <div class="phase-content">
                  <div class="phase-objectives">
                    <div class="objective" v-for="objective in phase.objectives" :key="objective">
                      <el-icon><Check /></el-icon>
                      <span>{{ objective }}</span>
                    </div>
                  </div>
                  <div class="phase-resources">
                    <div class="resource" v-for="resource in phase.resources" :key="resource.id">
                      <el-icon><Document /></el-icon>
                      <span>{{ resource.name }}</span>
                      <el-button type="text" size="small" @click="viewResource(resource)">
                        查看
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
</template>
 
<style lang="scss" scoped>
.decision-assessment {
  min-height: calc(100vh - 64px);
}

.page-header {
  margin-bottom: 15px;

  .action-card {
    border-radius: 5px;
    border: 1px solid #ebeef5;

    .action-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0px 20px;

      @media (max-width: 768px) {
        flex-direction: column;
        gap: 16px;
        text-align: center;
      }

      .action-info {
        flex: 1;

        h4 {
          margin: 0 0 3px;
          font-size: 18px;
          color: #1f2f3e;
          font-weight: 650;
          display: flex;
          align-items: center;
          gap: 8px;

          &::before {
            content: '';
            width: 4px;
            height: 18px;
            background: #409eff;
            border-radius: 2px;
            display: inline-block;
          }
        }

        p {
          margin: 0;
          color: #6b7a8a;
          font-size: 14px;
        }
      }

      .action-buttons {
        display: flex;
        gap: 12px;

        .el-button {
          border-radius: 5px;
          font-weight: 500;

          &.el-button--primary {
            background: #409eff;
            border: none;

            &:hover {
              background: #66b1ff;
            }
          }

          &.el-button--success.is-plain {
            border-color: #67c23a;
            color: #67c23a;

            &:hover {
              background: #67c23a;
              color: #fff;
            }
          }

          &.el-button--info.is-plain {
            border-color: #909399;
            color: #606266;

            &:hover {
              background: #909399;
              color: #fff;
            }
          }
        }
      }
    }
  }
}

.assessment-overview {
  margin-bottom: 15px;

  .overview-card {
    border-radius: 5px;
    border: 1px solid #ebeef5;
    height: 100%;

    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #1f2f3e;
        font-weight: 650;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          background: #409eff;
          border-radius: 2px;
          display: inline-block;
        }
      }
    }

    &:deep(.el-card__body) {
      padding: 20px;
    }
  }

  .score-card {
    .overview-content {
      .overall-score {
        text-align: center;
        margin-bottom: 12px;

        .score-circle {
          position: relative;
          display: inline-block;
          width: 120px;
          height: 120px;
          border-radius: 50%;
          background: linear-gradient(135deg, #9cb2fa 0%, #80a2ff 100%);
          color: white;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);

          .score-number {
            font-size: 36px;
            font-weight: 700;
            line-height: 1;
          }

          .score-label {
            font-size: 13px;
            opacity: 0.9;
            margin-top: 6px;
          }

          .score-level {
            position: absolute;
            bottom: -10px;
            background: white;
            color: #409eff;
            padding: 4px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
          }
        }
      }

      .score-breakdown {
        .breakdown-item {
          margin-bottom: 15px;

          &:last-child {
            margin-bottom: 0;
          }

          .breakdown-label {
            display: flex;
            justify-content: space-between;
            font-size: 13px;
            color: #6b7a8a;
            margin-bottom: 8px;
          }

          .breakdown-value {
            font-weight: 600;
            color: #1f2f3e;
          }

          .el-progress {
            :deep(.el-progress-bar__outer) {
              background: #eef2f7;
              border-radius: 10px;
            }
            :deep(.el-progress-bar__inner) {
              border-radius: 10px;
            }
          }
        }
      }
    }
  }

  .stats-card {
    .stats-content {
      .stat-item {
        display: flex;
        align-items: center;
        padding: 10px;
        margin-bottom: 10px;
        border-radius: 5px;
        background: #fafafa;
        border-left: 4px solid #409eff;
        transition: background 0.2s;

        &:hover {
          background: #f5faff;
        }

        &:last-child {
          margin-bottom: 0;
        }

        .stat-icon {
          margin-right: 16px;

          .el-icon {
            font-size: 22px;
            color: #409eff;
          }
        }

        .stat-info {
          flex: 1;

          .stat-value {
            font-size: 20px;
            font-weight: 700;
            color: #1f2f3e;
            margin-bottom: 2px;
          }

          .stat-label {
            font-size: 13px;
            color: #7a8b9b;
          }
        }
      }
    }
  }

  .comparison-card {
    .comparison-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .comparison-content {
      .comparison-chart {
        width: 100%;
        height: 200px;
        margin-bottom: 30px;
        background: #fbfdff;
        border-radius: 5px;
      }

      .comparison-insight {
        display: flex;
        align-items: center;
        padding: 15px 20px;
        background: #ecf5ff;
        border-radius: 5px;
        font-size: 14px;
        color: #409eff;
        border-left: 4px solid #409eff;
      }
    }
  }
}

.dimension-analysis {
  margin-bottom: 15px;

  .analysis-card,
  .trend-card {
    border-radius: 5px;
    border: 1px solid #ebeef5;
    height: 100%;

    &:deep(.el-card__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #1f2f3e;
        font-weight: 650;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          background: #409eff;
          border-radius: 2px;
          display: inline-block;
        }
      }
    }

    &:deep(.el-card__body) {
      padding: 20px;
    }
  }

  .analysis-content {
    .radar-chart-container {
      display: flex;

      @media (max-width: 992px) {
        flex-direction: column;
      }

      .radar-chart {
        flex: 1;
        width: 100%;
        min-height: 300px;
        background: #fbfdff;
        border-radius: 5px;

        @media (max-width: 992px) {
          min-height: 250px;
        }
      }

      .dimension-legend {
        width: 150px;
        margin-left: 40px;

        @media (max-width: 992px) {
          width: 100%;
          margin-left: 0;
          margin-top: 20px;
        }

        .legend-item {
          display: flex;
          align-items: center;
          margin-bottom: 14px;
          padding: 8px 8px;
          border-radius: 5px;
          transition: background 0.2s;

          &:hover {
            background: #f5f7fa;
          }

          &:last-child {
            margin-bottom: 0;
          }

          .legend-color {
            width: 14px;
            height: 14px;
            border-radius: 4px;
            margin-right: 10px;
          }

          .legend-name {
            flex: 1;
            font-size: 13px;
            color: #566b7c;
          }

          .legend-score {
            font-size: 14px;
            font-weight: 600;
            color: #1f2f3e;
          }
        }
      }
    }
  }

  .trend-content {
    .trend-chart {
      width: 100%;
      height: 200px;
      margin-bottom: 30px;
      background: #fbfdff;
      border-radius: 5px;
    }

    .trend-summary {
      display: flex;
      justify-content: space-around;

      .trend-item {
        text-align: center;

        .trend-label {
          font-size: 13px;
          color: #7a8b9b;
          margin-bottom: 4px;
        }

        .trend-value {
          font-size: 20px;
          font-weight: 700;
          color: #1f2f3e;
        }
      }
    }
  }
}

.scenario-analysis {
  margin-bottom: 15px;

  .scenario-performance-card,
  .pattern-analysis-card,
  .strengths-card,
  .improvements-card {
    border-radius: 5px;
    border: 1px solid #ebeef5;

    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #1f2f3e;
        font-weight: 650;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          background: #409eff;
          border-radius: 2px;
          display: inline-block;
        }
      }
    }

    &:deep(.el-card__body) {
      padding: 20px;
    }
  }

  .scenario-content {
    .scenario-name {
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        color: #409eff;
      }
    }

    .decision-score {
      display: flex;
      align-items: center;
      gap: 12px;

      .el-progress {
        flex: 1;

        :deep(.el-progress-bar__outer) {
          background: #eef2f7;
          border-radius: 10px;
        }
        :deep(.el-progress-bar__inner) {
          border-radius: 10px;
        }
      }

      span {
        font-size: 14px;
        color: #1f2f3e;
        font-weight: 500;
        min-width: 30px;
        text-align: right;
      }
    }

    .key-decisions {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    :deep(.excellent-row) {
      background-color: #f0f9ff !important;
    }
    :deep(.good-row) {
      background-color: #fff8e1 !important;
    }
    :deep(.improve-row) {
      background-color: #fef0f0 !important;
    }

    :deep(.el-table__row:hover > td) {
      background-color: #f5f7fa !important;
    }
  }

  .pattern-analysis-card {
    .pattern-content {
      display: flex;

      @media (max-width: 768px) {
        flex-direction: column;
      }

      .pattern-chart {
        flex: 1;
        min-height: 200px;
        background: #fbfdff;
        border-radius: 5px;
      }

      .pattern-insights {
        width: 200px;
        margin-left: 40px;

        @media (max-width: 768px) {
          width: 100%;
          margin-left: 0;
          margin-top: 20px;
        }

        .insight-item {
          display: flex;
          align-items: center;
          padding: 16px;
          margin-bottom: 12px;
          border-radius: 5px;
          background: #fafafa;
          border-left: 4px solid #409eff;

          &:last-child {
            margin-bottom: 0;
          }

          .insight-icon {
            margin-right: 12px;

            .el-icon {
              font-size: 20px;
              color: #409eff;
            }
          }

          .insight-content {
            flex: 1;

            .insight-title {
              font-size: 14px;
              font-weight: 600;
              color: #1f2f3e;
              margin-bottom: 4px;
            }

            .insight-desc {
              font-size: 14px;
              color: #6b7a8a;
            }
          }
        }
      }
    }
  }

  .strengths-content {
    .strength-list {
      .strength-item {
        display: flex;
        padding: 12px;
        margin-bottom: 10px;
        border-radius: 5px;
        background: #f0f9ff;
        border-left: 4px solid #67c23a;

        &:last-child {
          margin-bottom: 0;
        }

        .strength-icon {
          margin-right: 12px;

          .el-icon {
            font-size: 20px;
            color: #67c23a;
          }
        }

        .strength-content {
          flex: 1;

          .strength-title {
            font-size: 15px;
            font-weight: 650;
            color: #1f2f3e;
            margin-bottom: 4px;
          }

          .strength-desc {
            font-size: 14px;
            color: #566b7c;
            margin-bottom: 4px;
            line-height: 1.5;
          }

          .strength-evidence {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 13px;
            color: #7a8b9b;
          }
        }
      }
    }
  }

  .improvements-content {
    .improvement-list {
      .improvement-item {
        display: flex;
        padding: 12px;
        margin-bottom: 10px;
        border-radius: 5px;
        background: #fef0f0;
        border-left: 4px solid #f56c6c;

        &:last-child {
          margin-bottom: 0;
        }

        .improvement-content {
          flex: 1;

          .improvement-title {
            font-size: 15px;
            font-weight: 650;
            color: #1f2f3e;
            margin-bottom: 4px;
          }

          .improvement-desc {
            font-size: 14px;
            color: #566b7c;
            line-height: 1.5;
          }
        }
      }
    }
  }
}

.critical-moments {
  margin-bottom: 15px;

  .moments-card {
    border-radius: 5px;
    border: 1px solid #ebeef5;

    &:deep(.el-card__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #1f2f3e;
        font-weight: 650;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          background: #409eff;
          border-radius: 2px;
          display: inline-block;
        }
      }
    }

    &:deep(.el-card__body) {
      padding: 20px;
    }

    .moments-content {
      .moment-item {
        .moment-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          .moment-scenario {
            font-size: 15px;
            font-weight: 650;
            color: #1f2f3e;
          }
        }

        .moment-content {
          p {
            margin: 0 0 8px;
            color: #566b7c;
            line-height: 1.5;
          }
        }

        .moment-analysis {
          .analysis-item {
            margin-bottom: 4px;

            &:last-child {
              margin-bottom: 0;
            }

            .analysis-label {
              font-weight: 600;
              color: #1f2f3e;
            }

            .analysis-value {
              color: #6b7a8a;
            }
          }
        }
      }
    }
  }
}

.development-plan {
  margin-bottom: 0;

  .plan-card {
    border-radius: 5px;
    border: 1px solid #ebeef5;

    &:deep(.el-card__header) {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 20px;
      border-bottom: 1px solid #ebeef5;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #1f2f3e;
        font-weight: 650;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 16px;
          background: #409eff;
          border-radius: 2px;
          display: inline-block;
        }
      }
    }

    &:deep(.el-card__body) {
      padding: 20px;
    }

    .plan-content {
      .plan-summary {
        display: flex;
        justify-content: space-around;
        padding: 20px;
        margin-bottom: 20px;
        background: #f8fafc;
        border-radius: 5px;
        border: 1px solid #eef2f7;

        .summary-item {
          text-align: center;

          .summary-label {
            font-size: 13px;
            color: #7a8b9b;
            margin-bottom: 8px;
          }

          .summary-value {
            font-size: 20px;
            font-weight: 700;
            color: #1f2f3e;
          }
        }
      }

      .plan-details {
        .plan-phase {
          padding: 20px;
          margin-bottom: 16px;
          border: 1px solid #eef2f7;
          border-radius: 5px;
          background: #fff;

          &:last-child {
            margin-bottom: 0;
          }

          .phase-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;

            .phase-title {
              flex: 1;

              h4 {
                margin: 0 0 4px;
                font-size: 16px;
                color: #1f2f3e;
                font-weight: 650;
              }

              .phase-duration {
                font-size: 13px;
                color: #909399;
              }
            }
          }

          .phase-content {
            .phase-objectives {
              margin-bottom: 16px;

              .objective {
                display: flex;
                align-items: center;
                gap: 8px;
                padding: 6px 0;
                color: #566b7c;

                .el-icon {
                  color: #67c23a;
                  flex-shrink: 0;
                }
              }
            }

            .phase-resources {
              .resource {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 10px 0;
                border-top: 1px solid #f0f2f5;

                &:first-child {
                  border-top: none;
                }

                .el-icon {
                  color: #409eff;
                  margin-right: 8px;
                }
              }
            }
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .decision-assessment {
    padding: 16px;
  }

  .page-header {
    .action-content {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
    }
  }

  .assessment-overview {
    .overview-card {
      margin-bottom: 16px;
    }
  }
}
</style>
