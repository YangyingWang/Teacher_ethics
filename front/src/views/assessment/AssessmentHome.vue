<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  RefreshRight,
  Download,
  DataLine,
  TrendCharts,
  Reading,
  MagicStick,
  ArrowDown,
  ArrowUp,
  Top,
  Histogram,
  Monitor,
  Opportunity
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  assessmentHomeService,
  assessmentRefreshService,
  assessmentExportService
} from '@/api/assessment.js'

const loading = ref(false)

const timeRange = ref('quarter')
const trendRange = ref('3m')
const comparisonTarget = ref('department')
const expandedDimensions = ref(false)

const homeData = ref({
  lastUpdateDays: 0,
  totalActivities: 0,
  improvementRate: 0,
  rankingPercentile: 0,
  overallScore: 0,
  overallGrade: '未知',
  learningInvestment: 0,
  abilityPerformance: 0,
  governanceLevel: 0,
  monthlyChange: 0,
  dimensions: [],
  modules: {},
  aiAdvice: {
    summary: '',
    suggestions: []
  },
  comparisonInsights: [],
  trendData: {
    learning: [],
    ability: [],
    governance: []
  },
  comparisonDimensions: []
})

const trendChartRef = ref(null)
const radarChartRef = ref(null)
const compareChartRef = ref(null)

let trendChart = null
let radarChart = null
let compareChart = null

const moduleList = computed(() => {
  const modules = homeData.value?.modules || {}
  return Object.entries(modules).map(([key, value]) => ({
    key,
    ...value,
    metrics: value?.metrics || []
  }))
})

const sortedDimensions = computed(() => {
  const list = [...(homeData.value?.dimensions || [])]
  return list.sort((a, b) => (b.score || 0) - (a.score || 0))
})

const visibleDimensions = computed(() => {
  const list = sortedDimensions.value
  return expandedDimensions.value ? list : list.slice(0, 8)
})

const hasMoreDimensions = computed(() => sortedDimensions.value.length > 8)

const strongestDimension = computed(() => {
  return sortedDimensions.value.length ? sortedDimensions.value[0] : null
})

const weakestDimension = computed(() => {
  const list = sortedDimensions.value
  return list.length ? list[list.length - 1] : null
})

const summaryCards = computed(() => ([
  {
    key: 'overall',
    label: '综合得分',
    value: `${homeData.value?.overallScore || 0}分`,
    sub: homeData.value?.overallGrade || '未知',
    icon: DataLine,
    type: 'primary'
  },
  {
    key: 'activities',
    label: '总活动数',
    value: `${homeData.value?.totalActivities || 0}`,
    sub: '阶段累计',
    icon: Histogram,
    type: 'success'
  },
  {
    key: 'growth',
    label: '提升率',
    value: `${homeData.value?.improvementRate || 0}%`,
    sub: '较上次评估',
    icon: TrendCharts,
    type: 'warning'
  },
  {
    key: 'ranking',
    label: '排名百分位',
    value: `${homeData.value?.rankingPercentile || 0}%`,
    sub: '当前位置',
    icon: Top,
    type: 'danger'
  }
]))

const fetchHomeData = async () => {
  loading.value = true
  try {
    const res = await assessmentHomeService({
      timeRange: timeRange.value,
      trendRange: trendRange.value,
      comparisonTarget: comparisonTarget.value
    })
    homeData.value = normalizeHomeData(res.data || {})
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('加载多维评估首页失败：', error)
    ElMessage.error(error?.message || '加载多维评估首页失败')
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  loading.value = true
  try {
    const res = await assessmentRefreshService({
      timeRange: timeRange.value,
      trendRange: trendRange.value,
      comparisonTarget: comparisonTarget.value
    })
    homeData.value = normalizeHomeData(res.data || {})
    ElMessage.success('评估数据已刷新')
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('刷新评估数据失败：', error)
    ElMessage.error(error?.message || '刷新评估数据失败')
  } finally {
    loading.value = false
  }
}

const exportExcel = async () => {
  try {
    const { blob, fileName } = await assessmentExportService({
      timeRange: timeRange.value,
      trendRange: trendRange.value,
      comparisonTarget: comparisonTarget.value
    })

    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName || 'assessment.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出数据失败：', error)
    ElMessage.error(error?.message || '导出数据失败')
  }
}

function normalizeHomeData(raw) {
  return {
    lastUpdateDays: raw.lastUpdateDays ?? 0,
    totalActivities: raw.totalActivities ?? 0,
    improvementRate: raw.improvementRate ?? 0,
    rankingPercentile: raw.rankingPercentile ?? 0,
    overallScore: raw.overallScore ?? 0,
    overallGrade: raw.overallGrade || '未知',
    learningInvestment: raw.learningInvestment ?? 0,
    abilityPerformance: raw.abilityPerformance ?? 0,
    governanceLevel: raw.governanceLevel ?? 0,
    monthlyChange: raw.monthlyChange ?? 0,
    dimensions: Array.isArray(raw.dimensions) ? raw.dimensions : [],
    modules: raw.modules || {},
    aiAdvice: {
      summary: raw.aiAdvice?.summary || '暂无建议摘要',
      suggestions: Array.isArray(raw.aiAdvice?.suggestions) ? raw.aiAdvice.suggestions : []
    },
    comparisonInsights: Array.isArray(raw.comparisonInsights) ? raw.comparisonInsights : [],
    trendData: raw.trendData || {
      learning: [],
      ability: [],
      governance: []
    },
    comparisonDimensions: Array.isArray(raw.comparisonDimensions) ? raw.comparisonDimensions : []
  }
}

const initCharts = () => {
  initTrendChart()
  initRadarChart()
  initCompareChart()
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const learning = homeData.value?.trendData?.learning || []
  const ability = homeData.value?.trendData?.ability || []
  const governance = homeData.value?.trendData?.governance || []

  const labels = Array.from(new Set([
    ...learning.map(i => i.label),
    ...ability.map(i => i.label),
    ...governance.map(i => i.label)
  ]))

  const toSeries = (source) => labels.map(label => {
    const found = source.find(item => item.label === label)
    return found ? Number(found.score || 0) : 0
  })

  trendChart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      top: 0,
      itemWidth: 12,
      itemHeight: 8,
      textStyle: {
        color: '#606266'
      }
    },
    grid: {
      left: 36,
      right: 20,
      top: 44,
      bottom: 28
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      interval: 20,
      axisLine: { show: false },
      splitLine: {
        lineStyle: { color: '#ebeef5' }
      },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '学习筑基',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: toSeries(learning)
      },
      {
        name: '能力提升',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: toSeries(ability)
      },
      {
        name: '治理研修',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: toSeries(governance)
      }
    ]
  })
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (!radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }

  const dimensions = sortedDimensions.value
  const indicators = dimensions.map(item => ({
    name: item.name,
    max: 100
  }))
  const values = dimensions.map(item => Number(item.score || 0))

  radarChart.setOption({
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: 0,
      icon: 'roundRect',
      itemWidth: 18,
      itemHeight: 10
    },
    radar: {
      radius: '68%',
      center: ['50%', '46%'],
      splitNumber: 5,
      axisName: {
        color: '#606266',
        fontSize: 12
      },
      splitLine: {
        lineStyle: { color: '#dfe6f3' }
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(64,158,255,0.05)', 'rgba(64,158,255,0.02)']
        }
      },
      axisLine: {
        lineStyle: { color: '#dfe6f3' }
      },
      indicator: indicators
    },
    series: [
      {
        name: '个人能力',
        type: 'radar',
        areaStyle: {
          opacity: 0.18
        },
        data: [
          {
            value: values,
            name: '个人能力'
          }
        ]
      }
    ]
  })
}

const initCompareChart = () => {
  if (!compareChartRef.value) return
  if (!compareChart) {
    compareChart = echarts.init(compareChartRef.value)
  }

  const list = [...(homeData.value?.comparisonDimensions || [])]
    .sort((a, b) => (b.personalScore || 0) - (a.personalScore || 0))

  compareChart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      top: 0,
      itemWidth: 12,
      itemHeight: 8
    },
    grid: {
      left: 36,
      right: 20,
      top: 42,
      bottom: 80
    },
    xAxis: {
      type: 'category',
      data: list.map(item => item.name),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#606266',
        interval: 0,
        rotate: 35
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      interval: 20,
      splitLine: {
        lineStyle: { color: '#ebeef5' }
      },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '个人得分',
        type: 'bar',
        barMaxWidth: 18,
        data: list.map(item => Number(item.personalScore || 0))
      },
      {
        name: comparisonTargetText(comparisonTarget.value),
        type: 'bar',
        barMaxWidth: 18,
        data: list.map(item => Number(item.compareScore || 0))
      }
    ]
  })
}

const resizeCharts = () => {
  trendChart?.resize()
  radarChart?.resize()
  compareChart?.resize()
}

const destroyCharts = () => {
  trendChart?.dispose()
  radarChart?.dispose()
  compareChart?.dispose()
  trendChart = null
  radarChart = null
  compareChart = null
}

const handleResize = () => {
  resizeCharts()
}

const scoreTagType = (score) => {
  const s = Number(score || 0)
  if (s >= 90) return 'success'
  if (s >= 80) return 'primary'
  if (s >= 70) return 'warning'
  return 'danger'
}

const trendText = (trend) => {
  const t = Number(trend || 0)
  if (t > 0) return `+${t}%`
  return `${t}%`
}

const trendClass = (trend) => {
  const t = Number(trend || 0)
  if (t > 0) return 'up'
  if (t < 0) return 'down'
  return 'flat'
}

const moduleIcon = (key) => {
  const map = {
    learning: Reading,
    ability: MagicStick,
    governance: Monitor
  }
  return map[key] || Opportunity
}

const comparisonTargetText = (value) => {
  const map = {
    department: '院系平均',
    university: '校级平均',
    excellent: '优秀群体'
  }
  return map[value] || '对比对象'
}

watch([timeRange, trendRange, comparisonTarget], async () => {
  await fetchHomeData()
})

watch(
  () => [sortedDimensions.value.length, homeData.value?.comparisonDimensions?.length],
  async () => {
    await nextTick()
    resizeCharts()
  }
)

onMounted(async () => {
  await fetchHomeData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  destroyCharts()
})
</script>

<template>
  <div class="assessment-page" v-loading="loading">
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="title">多维评估</h1>
          <p class="subtitle">基于学习筑基、能力提升与治理研修数据，形成阶段性成长画像</p>
        </div>
        <div class="header-actions">
          <div class="date-selector">
            <el-select v-model="timeRange" style="width: 120px">
              <el-option label="近三个月" value="quarter" />
              <el-option label="本学期" value="semester" />
              <el-option label="本年度" value="year" />
            </el-select>
          </div>
          <div class="date-selector">
            <el-select v-model="trendRange" style="width: 120px">
              <el-option label="近3个月" value="3m" />
              <el-option label="近6个月" value="6m" />
              <el-option label="近1年" value="1y" />
            </el-select>
          </div>
          <div class="date-selector">
            <el-select v-model="comparisonTarget" style="width: 120px">
              <el-option label="院系平均" value="department" />
              <el-option label="校级平均" value="university" />
              <el-option label="优秀群体" value="excellent" />
            </el-select>
          </div>
          <div class="action-buttons">
            <el-button type="primary" @click="refreshData">
              <el-icon><RefreshRight /></el-icon>刷新数据
            </el-button>
            <el-button @click="exportExcel">
              <el-icon><Download /></el-icon>导出数据
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="summary-grid">
      <el-card v-for="card in summaryCards" :key="card.key" class="summary-card" shadow="never">
        <div class="summary-icon" :class="card.type">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="summary-content">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-sub">{{ card.sub }}</div>
        </div>
      </el-card>
    </div>

    <div class="overview-grid">
      <el-card class="main-panel score-panel" shadow="never">
        <template #header>
          <div class="card-title">
            <span>综合概览</span>
            <el-tag :type="scoreTagType(homeData.overallScore)">
              {{ homeData.overallGrade }}
            </el-tag>
          </div>
        </template>

        <div class="score-main">
          <div class="score-ring">
            <el-progress
              type="dashboard"
              :percentage="homeData.overallScore || 0"
              :stroke-width="10"
              :width="160"
            >
              <template #default="{ percentage }">
                <div class="dashboard-text">
                  <div class="num">{{ percentage }}</div>
                  <div class="unit">综合得分</div>
                </div>
              </template>
            </el-progress>
          </div>

          <div class="score-meta">
            <div class="meta-item">
              <div class="meta-label">学习筑基</div>
              <div class="meta-value">{{ homeData.learningInvestment }}分</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">能力提升</div>
              <div class="meta-value">{{ homeData.abilityPerformance }}分</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">治理研修</div>
              <div class="meta-value">{{ homeData.governanceLevel }}分</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">较上次变化</div>
              <div class="meta-value" :class="trendClass(homeData.monthlyChange)">
                {{ homeData.monthlyChange > 0 ? '+' : '' }}{{ homeData.monthlyChange }}
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-label">距离上次更新</div>
              <div class="meta-value">{{ homeData.lastUpdateDays }}天</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">当前排名百分位</div>
              <div class="meta-value">{{ homeData.rankingPercentile }}%</div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="main-panel trend-panel" shadow="never">
        <template #header>
          <div class="card-title">
            <span>成长趋势</span>
            <span class="card-subtitle">近阶段三大模块变化情况</span>
          </div>
        </template>
        <div ref="trendChartRef" class="chart trend-chart"></div>
      </el-card>
    </div>

    <div class="module-grid">
      <el-card
        v-for="module in moduleList"
        :key="module.key"
        class="module-card"
        shadow="never"
      >
        <div class="module-top">
          <div class="module-icon">
            <el-icon><component :is="moduleIcon(module.key)" /></el-icon>
          </div>
          <div class="module-info">
            <div class="module-name">{{ module.name }}</div>
            <div class="module-desc">{{ module.description }}</div>
          </div>
          <div class="module-score">{{ module.score }}分</div>
        </div>

        <div class="module-metrics">
          <div v-for="metric in module.metrics" :key="metric.name" class="metric-row">
            <div class="metric-main">
              <span class="metric-name">{{ metric.name }}</span>
              <span class="metric-value">{{ metric.value }}</span>
            </div>
            <el-progress :percentage="metric.percentage || 0" :show-text="false" :stroke-width="6" />
            <div class="metric-extra">
              <span>目标：{{ metric.target }}</span>
              <span :class="['metric-trend', trendClass(metric.trend)]">
                <el-icon v-if="(metric.trend || 0) > 0"><ArrowUp /></el-icon>
                <el-icon v-else-if="(metric.trend || 0) < 0"><ArrowDown /></el-icon>
                {{ trendText(metric.trend) }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="content-grid">
      <div class="content-main">
        <el-card class="section-card dimension-card" shadow="never">
          <template #header>
            <div class="card-title">
              <span>能力维度</span>
              <span class="card-subtitle">雷达图与维度排行榜联动展示</span>
            </div>
          </template>

          <div class="dimension-layout">
            <div class="dimension-left">
              <div ref="radarChartRef" class="chart radar-chart"></div>
            </div>

            <div class="dimension-right">
              <div class="dimension-summary">
                <div class="summary-block success" v-if="strongestDimension">
                  <div class="summary-block-label">当前优势维度</div>
                  <div class="summary-block-title">{{ strongestDimension.name }}</div>
                  <div class="summary-block-score">{{ strongestDimension.score }}分</div>
                </div>
                <div class="summary-block warning" v-if="weakestDimension">
                  <div class="summary-block-label">当前待提升维度</div>
                  <div class="summary-block-title">{{ weakestDimension.name }}</div>
                  <div class="summary-block-score">{{ weakestDimension.score }}分</div>
                </div>
              </div>

              <div class="dimension-list">
                <div
                  v-for="item in visibleDimensions"
                  :key="item.name"
                  class="dimension-row"
                >
                  <div class="dimension-row-head">
                    <span class="dimension-name">{{ item.name }}</span>
                    <span class="dimension-score">{{ item.score }}分</span>
                  </div>
                  <el-progress
                    :percentage="item.score || 0"
                    :show-text="false"
                    :stroke-width="8"
                    :status="item.score >= 90 ? 'success' : undefined"
                  />
                </div>
              </div>

              <div class="dimension-expand" v-if="hasMoreDimensions">
                <el-button text @click="expandedDimensions = !expandedDimensions">
                  {{ expandedDimensions ? '收起部分维度' : '展开全部维度' }}
                </el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="section-card compare-card" shadow="never">
          <template #header>
            <div class="card-title">
              <span>对比分析</span>
              <el-tag>{{ comparisonTargetText(comparisonTarget) }}</el-tag>
            </div>
          </template>

          <div class="compare-layout">
            <div class="compare-left">
              <div ref="compareChartRef" class="chart compare-chart"></div>
            </div>

            <div class="compare-right">
              <div class="insight-list">
                <div
                  v-for="item in homeData.comparisonInsights"
                  :key="item.id"
                  class="insight-item"
                  :class="item.type"
                >
                  <div class="insight-header">
                    <span class="insight-type">{{ item.type === 'strength' ? '优势' : '短板' }}</span>
                    <span class="insight-title">{{ item.title }}</span>
                  </div>
                  <div class="insight-detail">{{ item.detail }}</div>
                </div>

                <el-empty
                  v-if="!homeData.comparisonInsights?.length"
                  description="暂无对比结论"
                />
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div class="content-side">
        <el-card class="advice-card" shadow="never">
          <template #header>
            <div class="card-title">
              <span>智能建议</span>
              <span class="card-subtitle">基于当前评估结果生成</span>
            </div>
          </template>

          <div class="advice-summary">
            {{ homeData.aiAdvice?.summary || '暂无建议摘要' }}
          </div>

          <div class="advice-list">
            <div
              v-for="(item, index) in homeData.aiAdvice?.suggestions || []"
              :key="`${item.title}-${index}`"
              class="advice-item"
            >
              <div class="advice-item-header">
                <el-tag size="small" :type="item.priority === '高' ? 'danger' : item.priority === '中' ? 'warning' : 'info'">
                  {{ item.priority }}优先级
                </el-tag>
                <span class="advice-item-title">{{ item.title }}</span>
              </div>
              <div class="advice-item-desc">{{ item.description }}</div>
            </div>

            <el-empty
              v-if="!(homeData.aiAdvice?.suggestions || []).length"
              description="暂无建议"
            />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.assessment-page {
  min-height: calc(100vh - 64px);
}

.page-header {
  background: linear-gradient(135deg, #9cb2fa 0%, #80a2ff 100%);
  border-radius: 8px;
  padding: 20px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.2);
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;

    .header-left {
      flex: 1;
      .title {
        font-size: 24px;
        font-weight: 600;
        margin: 0 0 5px;
        display: flex;
        align-items: center;
      }
      .subtitle {
        font-size: 14px;
        opacity: 0.9;
        margin: 0;
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      .date-selector {
        background: rgba(255, 255, 255, 0.2);
        color: white;
        opacity: 0.7;
        // width: 200px;
      }
    
      .action-buttons {
        display: flex;
        gap: 12px;
        .el-button {
          background: rgba(255, 255, 255, 0.2);
          border: 1px solid rgba(255, 255, 255, 0.3);
          color: white;
          border-radius: 10px;
          border-radius: 10px;
          padding: 10px 20px;
          &:hover {
            background: rgba(255, 255, 255, 0.3);
          }

          &.el-button--primary {
            background: white;
            color: #409eff;
            border: none;

            &:hover {
              background: #ecf5ff;
            }
          }
        }
      }
    }
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.summary-card {
  border: none;
  border-radius: 12px;

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
  }

  .summary-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;

    &.primary {
      background: rgba(64, 158, 255, 0.12);
      color: #409eff;
    }

    &.success {
      background: rgba(103, 194, 58, 0.12);
      color: #67c23a;
    }

    &.warning {
      background: rgba(230, 162, 60, 0.12);
      color: #e6a23c;
    }

    &.danger {
      background: rgba(245, 108, 108, 0.12);
      color: #f56c6c;
    }
  }

  .summary-content {
    min-width: 0;
    flex: 1;
  }

  .summary-label {
    color: #909399;
    font-size: 13px;
    margin-bottom: 6px;
  }

  .summary-value {
    color: #303133;
    font-size: 26px;
    line-height: 1.2;
    font-weight: 700;
    margin-bottom: 4px;
  }

  .summary-sub {
    color: #606266;
    font-size: 13px;
  }
}

.overview-grid {
  display: grid;
  grid-template-columns: 1.05fr 1.45fr;
  gap: 20px;
  margin-bottom: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.main-panel,.section-card,.module-card,.advice-card {
  border-bottom: 1px solid #eef2f7;
  border-radius: 8px;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;

  .card-subtitle {
    font-size: 13px;
    color: #909399;
    font-weight: 400;
  }
}

.score-panel {
  .score-main {
    display: grid;
    grid-template-columns: 220px 1fr;
    gap: 24px;
    align-items: center;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }

  .score-ring {
    display: flex;
    align-items: center;
    justify-content: center;

    .dashboard-text {
      text-align: center;

      .num {
        font-size: 28px;
        font-weight: 700;
        color: #303133;
        line-height: 1;
      }

      .unit {
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .score-meta {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;

    .meta-item {
      padding: 14px 16px;
      border-radius: 10px;
      background: #f8fafc;

      .meta-label {
        font-size: 13px;
        color: #909399;
        margin-bottom: 6px;
      }

      .meta-value {
        font-size: 20px;
        font-weight: 700;
        color: #303133;

        &.up {
          color: #67c23a;
        }

        &.down {
          color: #f56c6c;
        }

        &.flat {
          color: #909399;
        }
      }
    }
  }
}

.chart {
  width: 100%;
}

.trend-chart {
  height: 320px;
}

.radar-chart {
  height: 420px;
}

.compare-chart {
  height: 360px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  @media (max-width: 1280px) {
    grid-template-columns: 1fr;
  }
}

.module-card {
  border-bottom: 1px solid #eef2f7;
  border-radius: 8px;
  
  .module-top {
    display: grid;
    grid-template-columns: 52px 1fr auto;
    gap: 14px;
    align-items: center;
    margin-bottom: 18px;
  }

  .module-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    background: rgba(64, 158, 255, 0.08);
    color: #409eff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
  }

  .module-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  .module-desc {
    font-size: 13px;
    color: #909399;
    line-height: 1.5;
  }

  .module-score {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    white-space: nowrap;
  }

  .module-metrics {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .metric-row {
    padding: 14px;
    border-radius: 10px;
    background: #f8fafc;
  }

  .metric-main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .metric-name {
      color: #606266;
      font-size: 14px;
    }

    .metric-value {
      color: #303133;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .metric-extra {
    margin-top: 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #909399;
    font-size: 12px;

    .metric-trend {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      font-weight: 600;

      &.up {
        color: #67c23a;
      }

      &.down {
        color: #f56c6c;
      }

      &.flat {
        color: #909399;
      }
    }
  }
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 20px;

  @media (max-width: 1400px) {
    grid-template-columns: 1fr;
  }
}

.content-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-side {
  .advice-card {
    position: sticky;
    top: 20px;
  }
}

.dimension-layout {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(320px, 420px);
  gap: 20px;

  @media (max-width: 1280px) {
    grid-template-columns: 1fr;
  }
}

.dimension-right {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dimension-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.summary-block {
  padding: 14px 16px;
  border-radius: 10px;

  &.success {
    background: rgba(103, 194, 58, 0.08);
    border: 1px solid rgba(103, 194, 58, 0.18);
  }

  &.warning {
    background: rgba(230, 162, 60, 0.08);
    border: 1px solid rgba(230, 162, 60, 0.18);
  }

  .summary-block-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 6px;
  }

  .summary-block-title {
    color: #303133;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .summary-block-score {
    color: #303133;
    font-size: 22px;
    font-weight: 700;
  }
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dimension-row {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fafc;

  .dimension-row-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    gap: 12px;
  }

  .dimension-name {
    color: #606266;
    font-size: 14px;
    line-height: 1.4;
  }

  .dimension-score {
    color: #303133;
    font-size: 15px;
    font-weight: 700;
    white-space: nowrap;
  }
}

.dimension-expand {
  text-align: center;
}

.compare-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;

  @media (max-width: 1280px) {
    grid-template-columns: 1fr;
  }
}

.insight-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.insight-item {
  border-radius: 10px;
  padding: 14px 16px;
  background: #f8fafc;
  border-left: 4px solid #dcdfe6;

  &.strength {
    border-left-color: #67c23a;
  }

  &.weakness {
    border-left-color: #f56c6c;
  }

  .insight-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  .insight-type {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 44px;
    height: 22px;
    padding: 0 8px;
    border-radius: 12px;
    font-size: 12px;
    color: #fff;
    background: #909399;
  }

  &.strength .insight-type {
    background: #67c23a;
  }

  &.weakness .insight-type {
    background: #f56c6c;
  }

  .insight-title {
    color: #303133;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.5;
  }

  .insight-detail {
    color: #606266;
    font-size: 14px;
    line-height: 1.75;
  }
}

.advice-card {
  .advice-summary {
    padding: 14px 16px;
    border-radius: 10px;
    background: #f8fafc;
    color: #606266;
    line-height: 1.8;
    font-size: 14px;
    margin-bottom: 16px;
  }

  .advice-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .advice-item {
    padding: 16px;
    border-radius: 10px;
    background: #fff;
    border: 1px solid #ebeef5;
    transition: all 0.2s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
    }
  }

  .advice-item-header {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 10px;
  }

  .advice-item-title {
    color: #303133;
    font-size: 16px;
    font-weight: 600;
    line-height: 1.6;
  }

  .advice-item-desc {
    color: #606266;
    font-size: 14px;
    line-height: 1.9;
  }
}

:deep(.el-card__header) {
  padding: 10px 20px;
  border-bottom: 1px solid #eef2f7;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-progress-bar__outer) {
  background: #ebeef5;
}
</style>