<script setup>
import { Download, Share, Document, Calendar, CaretTop, CaretBottom, ArrowRight, Check, Warning, Star, TrendCharts, User, Reading,
    VideoPlay, Setting, School, Flag, Clock, MagicStick} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
  
// 图表引用
const radarChartRef = ref(null)
const trendChartRef = ref(null)
const comparisonChartRef = ref(null)
// 图表实例
let radarChart = null
let trendChart = null
let comparisonChart = null
// 状态控制
const radarCompareMode = ref('personal')
const trendTimeRange = ref('quarter')
const activeDimension = ref('道德认知')
const comparisonType = ref('department')
// 模拟报告数据
const reportData = ref({
  reportId: 'DIAG-20240315-001',
  generateTime: '2024-03-15 14:30:24',
  overallScore: 85,
  overallLevel: '良好',
  percentile: 78,
  testCount: 12,
  studyDays: 45,
  
  dimensions: [
    {
      name: '道德认知',
      score: 90,
      trend: 8,
      percentile: 85,
      testCount: 6,
      correctRate: 92,
      strengths: '在道德原则理解方面表现突出，能够准确识别基本道德规范',
      suggestions: [
        '深入学习道德哲学理论',
        '参与道德困境案例分析讨论',
        '定期进行道德认知自评'
      ]
    },{
      name: '情景应对',
      score: 82,
      trend: 12,
      percentile: 75,
      testCount: 8,
      correctRate: 85,
      strengths: '对突发情景反应迅速，决策思路清晰',
      suggestions: [
        '增加复杂情景模拟练习',
        '学习危机处理心理学知识',
        '参与跨学科情景研讨会'
      ]
    },{
      name: '课堂融入',
      score: 88,
      trend: 5,
      percentile: 82,
      testCount: 5,
      correctRate: 90,
      strengths: '能够将思政元素自然融入教学，学生接受度高',
      suggestions: [
        '学习更多课程思政案例',
        '尝试创新教学方法',
        '加强课堂互动技巧'
      ]
    },{
      name: '学术诚信',
      score: 76,
      trend: 15,
      percentile: 65,
      testCount: 7,
      correctRate: 80,
      strengths: '对学术规范了解全面，注重数据真实性',
      suggestions: [
        '加强科研伦理系统学习',
        '参与学术不端案例研讨',
        '建立个人学术诚信档案'
      ]
    },{
      name: '社会责任',
      score: 79,
      trend: -2,
      percentile: 70,
      testCount: 4,
      correctRate: 78,
      strengths: '关注社会需求，积极参与公益活动',
      suggestions: [
        '深入研究社会责任理论',
        '参与社会服务实践项目',
        '加强公共服务意识培养'
      ]
    },{
      name: '师生关系',
      score: 91,
      trend: 6,
      percentile: 88,
      testCount: 5,
      correctRate: 93,
      strengths: '能够建立平等互信的师生关系，沟通能力突出',
      suggestions: [
        '学习师生沟通心理学',
        '定期进行师生关系评估',
        '参与师生互动工作坊'
      ]
    }
  ],
  scenarioRankings: [
    { id: 1, name: '学术署名权纠纷', score: 92, difficulty: 2 },
    { id: 2, name: '师生关系边界', score: 88, difficulty: 3 },
    { id: 3, name: '招生利益冲突', score: 85, difficulty: 2 },
    { id: 4, name: '课堂言论引导', score: 82, difficulty: 1 },
    { id: 5, name: '科研数据真实性', score: 79, difficulty: 3 }
  ],
  learningSuggestions: [
    '学术诚信维度有待加强，建议完成相关专题学习',
    '情景应对能力提升明显，继续保持练习频率',
    '社会责任维度略有下降，建议关注最新社会需求',
    '师生关系处理能力优秀，可分享经验帮助他人'
  ],
  comparisonInsights: [
    {
      id: 1,
      type: 'strength',
      title: '道德认知超越90%同行',
      description: '您在道德原则理解和应用方面表现突出'
    },{
      id: 2,
      type: 'improvement',
      title: '学术诚信需重点关注',
      description: '该维度相对薄弱，建议加强学习'
    },{
      id: 3,
      type: 'strength',
      title: '师生关系处理能力优秀',
      description: '您的师生关系处理能力在院系中名列前茅'
    }
  ],
  // 模拟趋势数据
  trendData: {
    month: [
      { date: '02-15', 道德认知: 85, 情景应对: 78, 课堂融入: 82, 学术诚信: 72, 社会责任: 80, 师生关系: 88 },
      { date: '02-22', 道德认知: 86, 情景应对: 80, 课堂融入: 83, 学术诚信: 73, 社会责任: 81, 师生关系: 89 },
      { date: '02-29', 道德认知: 87, 情景应对: 81, 课堂融入: 84, 学术诚信: 74, 社会责任: 80, 师生关系: 90 },
      { date: '03-07', 道德认知: 88, 情景应对: 82, 课堂融入: 85, 学术诚信: 75, 社会责任: 79, 师生关系: 90 },
      { date: '03-15', 道德认知: 90, 情景应对: 82, 课堂融入: 88, 学术诚信: 76, 社会责任: 79, 师生关系: 91 }
    ],
    quarter: [
      // 更多数据...
    ],
    year: [
      // 更多数据...
    ]
  }
})
  
const getScoreType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 80) return 'warning'
  return 'danger'
}

const getScoreColor = (score) => {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#e6a23c'
  return '#f56c6c'
}

const getPercentileColor = (percentile) => {
  if (percentile >= 80) return '#67c23a'
  if (percentile >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getDimensionClass = (score) => {
  if (score >= 90) return 'dimension-excellent'
  if (score >= 80) return 'dimension-good'
  return 'dimension-improve'
}
  
const getDimensionIcon = (name) => {
  const iconMap = {
    '道德认知': Setting,
    '情景应对': Setting,
    '课堂融入': School,
    '学术诚信': Document,
    '社会责任': Flag,
    '师生关系': User
  }
  return iconMap[name] || Star
}

const getDifficultyType = (difficulty) => {
  const types = ['success', 'warning', 'danger']
  return types[difficulty - 1] || 'info'
}

const getDifficultyText = (difficulty) => {
  const texts = ['简单', '中等', '困难']
  return texts[difficulty - 1] || '未知'
}

const getSuggestionColor = (index) => {
  const colors = ['#67c23a', '#e6a23c', '#409eff', '#f56c6c']
  return colors[index % colors.length]
}

const getSuggestionIcon = (index) => {
  const icons = [Check, Star, TrendCharts, Reading]
  return icons[index % icons.length]
}
  
const getOrderClass = (order) => {
  if (order === 1) return 'order-first'
  if (order === 2) return 'order-second'
  if (order === 3) return 'order-third'
  return 'order-normal'
}

// 交互函数
const scrollToDimension = (dimensionName) => {
  activeDimension.value = dimensionName
  const element = document.querySelector('.dimension-tabs')
  element?.scrollIntoView({ behavior: 'smooth' })
}

const generateReport = () => {
  ElMessage.success('诊断报告下载中...')
  // 实际开发中这里会调用API生成PDF报告
}

const shareReport = () => {
  ElMessage.info('分享功能开发中...')
}

const viewDetailedAnalysis = () => {
  router.push('/test/analysis')
}

const viewAllScenarios = () => {
  router.push('/test/home')
}

const viewScenarioDetail = (scenarioId) => {
  router.push({ path: '/test/scene', query: { id: scenarioId } })
}

const createLearningPlan = () => {
  ElMessage.success('学习计划生成中...')
  // 实际开发中这里会跳转到学习计划页面
}
  
// 初始化图表
const initCharts = () => {
  // 雷达图
  if (radarChartRef.value) {
    radarChart = echarts.init(radarChartRef.value)
    updateRadarChart()
  }
  // 趋势图
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    updateTrendChart()
  }
  // 对比图
  if (comparisonChartRef.value) {
    comparisonChart = echarts.init(comparisonChartRef.value)
    updateComparisonChart()
  }
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
}
  
// 更新雷达图
const updateRadarChart = () => {
  const dimensions = reportData.value.dimensions.map(d => d.name)
  const personalData = reportData.value.dimensions.map(d => d.score)
  const departmentData = reportData.value.dimensions.map(d => d.score - 5 + Math.random() * 10)
  const universityData = reportData.value.dimensions.map(d => d.score - 10 + Math.random() * 20)
  
  let compareData = []
  if (radarCompareMode.value === 'personal') {
    compareData = [{ name: '个人能力', value: personalData }]
  } else if (radarCompareMode.value === 'department') {
    compareData = [
      { name: '个人能力', value: personalData },
      { name: '院系平均', value: departmentData }
    ]
  } else {
    compareData = [
      { name: '个人能力', value: personalData },
      { name: '全校平均', value: universityData }
    ]
  }
  
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      bottom: 10,
      textStyle: {
        fontSize: 12,
        color: '#909399'
      }
    },
    radar: {
      indicator: dimensions.map(name => ({
        name,
        max: 100
      })),
      center: ['50%', '50%'],
      radius: '65%',
      splitArea: {
        areaStyle: {
          color: ['rgba(240, 242, 245, 0.8)', 'rgba(240, 242, 245, 0.2)']
        }
      },
      splitLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      }
    },
    series: [{
      type: 'radar',
      data: compareData.map((item, index) => ({
        value: item.value,
        name: item.name,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 2,
          type: index === 0 ? 'solid' : 'dashed'
        },
        areaStyle: {
          opacity: index === 0 ? 0.4 : 0.1
        },
        itemStyle: {
          color: index === 0 ? '#409eff' : (radarCompareMode.value === 'department' ? '#67c23a' : '#e6a23c')
        }
      }))
    }]
  }
  
  radarChart.setOption(option)
}
// 更新趋势图
const updateTrendChart = () => {
  const timeRange = trendTimeRange.value
  const data = reportData.value.trendData[timeRange] || reportData.value.trendData.month
  
  const dimensions = ['道德认知', '情景应对', '课堂融入', '学术诚信', '社会责任', '师生关系']
  const series = dimensions.map(dimension => ({
    name: dimension,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 8,
    data: data.map(item => item[dimension])
  }))
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      top: 10,
      textStyle: {
        fontSize: 12,
        color: '#909399'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(item => item.date),
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      }
    },
    yAxis: {
      type: 'value',
      min: 60,
      max: 100,
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f2f5',
          type: 'dashed'
        }
      }
    },
    series
  }
  
  trendChart.setOption(option)
}
// 更新对比图
const updateComparisonChart = () => {
  const comparisonTypeMap = {
    department: { name: '院系平均', color: '#67c23a' },
    university: { name: '全校平均', color: '#e6a23c' },
    excellent: { name: '优秀教师', color: '#f56c6c' }
  }
  
  const current = comparisonType.value
  const compareInfo = comparisonTypeMap[current]
  
  const dimensions = reportData.value.dimensions
  const personalScores = dimensions.map(d => d.score)
  const compareScores = dimensions.map(d => {
    let base = 85
    if (current === 'department') base = 82
    if (current === 'university') base = 80
    if (current === 'excellent') base = 95
    return base + Math.random() * 10 - 5
  })
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      top: 10,
      textStyle: {
        fontSize: 12,
        color: '#909399'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dimensions.map(d => d.name),
      axisLabel: {
        interval: 0,
        rotate: 30
      },
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
      }
    },
    yAxis: {
      type: 'value',
      min: 60,
      max: 100,
      axisLine: {
        lineStyle: {
          color: '#dcdfe6'
        }
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
        name: '个人能力',
        type: 'bar',
        barWidth: '30%',
        itemStyle: {
          color: '#409eff'
        },
        data: personalScores
      },
      {
        name: compareInfo.name,
        type: 'bar',
        barWidth: '30%',
        itemStyle: {
          color: compareInfo.color
        },
        data: compareScores
      }
    ]
  }
  
  comparisonChart.setOption(option)
}
  
// 处理窗口大小变化
const handleResize = () => {
  if (radarChart) radarChart.resize()
  if (trendChart) trendChart.resize()
  if (comparisonChart) comparisonChart.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
  })
})
onBeforeUnmount(() => {
  if (radarChart) radarChart.dispose()
  if (trendChart) trendChart.dispose()
  if (comparisonChart) comparisonChart.dispose()
  window.removeEventListener('resize', handleResize)
})  
watch(radarCompareMode, () => {
  updateRadarChart()
})
watch(trendTimeRange, () => {
  updateTrendChart()
})
watch(comparisonType, () => {
  updateComparisonChart()
})
</script>
  
<template>
    <div class="diagnosis-report">
      <el-card class="overview-card" shadow="never">
        <template #header>
          <div class="overview-header">
            <h2>师德能力诊断报告</h2>
            <div class="report-info">
              <span class="report-date">生成时间：{{ reportData.generateTime }}</span>
              <span class="report-id">报告编号：{{ reportData.reportId }}</span>
            </div>
            <div class="header-actions">
                <el-button type="primary" plain @click="generateReport"><el-icon><Download /></el-icon>下载报告</el-button>
                <el-button type="info" plain @click="shareReport"><el-icon><Share /></el-icon>分享报告</el-button>
            </div>
          </div>
        </template>  
        <div class="overview-content">
          <div class="overall-score">
            <div class="score-circle">
              <div class="score-number">{{ reportData.overallScore }}</div>
              <div class="score-label">综合得分</div>
              <div class="score-level">{{ reportData.overallLevel }}</div>
            </div>
            <div class="score-breakdown">
              <div class="breakdown-item">
                <div class="breakdown-label">能力评级</div>
                <div class="breakdown-value">
                  <el-tag :type="getScoreType(reportData.overallScore)" size="large">
                    {{ reportData.overallLevel }}
                  </el-tag>
                </div>
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">超越同行</div>
                <div class="breakdown-value">
                  <div class="percentile">
                    <span class="percentile-value">{{ reportData.percentile }}%</span>
                    <el-progress
                      :percentage="reportData.percentile"
                      :stroke-width="8"
                      :show-text="false"
                      :color="getPercentileColor(reportData.percentile)"
                    />
                  </div>
                </div>
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">测试总数</div>
                <div class="breakdown-value">
                  <div class="test-count">
                    <el-icon><Document /></el-icon>
                    <span>{{ reportData.testCount }} 次</span>
                  </div>
                </div>
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">持续学习</div>
                <div class="breakdown-value">
                  <div class="study-days">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ reportData.studyDays }} 天</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
  
          <div class="dimension-quickview">
            <h3>能力维度概览</h3>
            <div class="dimension-badges">
              <div
                v-for="dimension in reportData.dimensions"
                :key="dimension.name"
                class="dimension-badge"
                :class="getDimensionClass(dimension.score)"
                @click="scrollToDimension(dimension.name)"
              >
                <div class="badge-icon">
                  <el-icon :size="20">
                    <component :is="getDimensionIcon(dimension.name)" />
                  </el-icon>
                </div>
                <div class="badge-content">
                  <div class="badge-name">{{ dimension.name }}</div>
                  <div class="badge-score">{{ dimension.score }}分</div>
                </div>
                <div class="badge-trend" v-if="dimension.trend">
                  <el-icon :class="dimension.trend > 0 ? 'trend-up' : 'trend-down'">
                    <CaretTop v-if="dimension.trend > 0" />
                    <CaretBottom v-else />
                  </el-icon>
                  <span>{{ Math.abs(dimension.trend) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
  
      <div class="chart-area">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="never">
              <template #header>
                <div class="chart-header">
                  <h3>能力雷达图</h3>
                  <el-radio-group v-model="radarCompareMode" size="small">
                    <el-radio-button label="personal">个人评估</el-radio-button>
                    <el-radio-button label="department">院系对比</el-radio-button>
                    <el-radio-button label="university">全校对比</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              <div class="chart-content">
                <div class="radar-chart" ref="radarChartRef"></div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="never">
              <template #header>
                <div class="chart-header">
                  <h3>能力成长趋势</h3>
                  <el-select v-model="trendTimeRange" size="small" style="width: 120px">
                    <el-option label="近一月" value="month" />
                    <el-option label="近三月" value="quarter" />
                    <el-option label="近一年" value="year" />
                  </el-select>
                </div>
              </template>
              <div class="chart-content">
                <div class="trend-chart" ref="trendChartRef"></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
  
      <div class="analysis-area">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
            <el-card class="analysis-card" shadow="never">
              <template #header>
                <div class="analysis-header">
                  <h3>维度详细分析</h3>
                  <el-button type="text" @click="viewDetailedAnalysis">查看完整分析 <el-icon><ArrowRight /></el-icon></el-button>
                </div>
              </template>
              <div class="analysis-content">
                <div class="dimension-tabs">
                  <el-tabs v-model="activeDimension" type="border-card">
                    <el-tab-pane v-for="dimension in reportData.dimensions" :key="dimension.name"
                      :label="dimension.name" :name="dimension.name"
                    >
                      <div class="dimension-detail">
                        <div class="dimension-header">
                          <div class="dimension-score">
                            <div class="current-score">
                              <span class="score-label">当前得分</span>
                              <span class="score-value">{{ dimension.score }}分</span>
                            </div>
                            <el-progress :percentage="dimension.score" :stroke-width="12" :color="getScoreColor(dimension.score)" />
                          </div>
                          <div class="dimension-stats">
                            <div class="stat-item">
                              <div class="stat-label">相对水平</div>
                              <div class="stat-value">{{ dimension.percentile }}%</div>
                            </div>
                            <div class="stat-item">
                              <div class="stat-label">测试次数</div>
                              <div class="stat-value">{{ dimension.testCount }}</div>
                            </div>
                            <div class="stat-item">
                              <div class="stat-label">正确率</div>
                              <div class="stat-value">{{ dimension.correctRate }}%</div>
                            </div>
                          </div>
                        </div>
                        <div class="dimension-analysis">
                          <h4>优势分析</h4>
                          <p>{{ dimension.strengths }}</p>
                          <h4>改进建议</h4>
                          <ul class="suggestions-list">
                            <li v-for="(suggestion, index) in dimension.suggestions" :key="index">
                              <el-icon><Check /></el-icon>{{ suggestion }}
                            </li>
                          </ul>
                        </div>
                      </div>
                    </el-tab-pane>
                  </el-tabs>
                </div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="ranking-card" shadow="never">
              <template #header>
                <div class="ranking-header">
                  <h3>情景表现排行</h3>
                  <el-button type="text" @click="viewAllScenarios">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
                </div>
              </template>
              <div class="ranking-content">
                <div class="scenario-rankings">
                  <div v-for="(scenario, index) in reportData.scenarioRankings" :key="scenario.id"
                    class="ranking-item" @click="viewScenarioDetail(scenario.id)"
                  >
                    <div class="ranking-order">
                      <span class="order-number" :class="getOrderClass(index + 1)">{{ index + 1 }}</span>
                    </div>
                    <div class="ranking-info">
                      <div class="scenario-name">{{ scenario.name }}</div>
                      <div class="scenario-score">
                        <el-progress :percentage="scenario.score" :stroke-width="6" :show-text="false" :color="getScoreColor(scenario.score)"/>
                        <span class="score-text">{{ scenario.score }}分</span>
                      </div>
                    </div>
                    <div class="ranking-tag">
                      <el-tag :type="getDifficultyType(scenario.difficulty)" size="small">{{ getDifficultyText(scenario.difficulty) }}</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div class="analysis-area">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
            <el-card class="comparison-card" shadow="never">
              <template #header>
                <div class="comparison-header">
                  <h3>对比分析</h3>
                    <div class="comparison-options">
                      <el-select v-model="comparisonType" size="small" style="width: 140px">
                        <el-option label="与院系平均对比" value="department" />
                        <el-option label="与全校平均对比" value="university" />
                        <el-option label="与优秀教师对比" value="excellent" />
                      </el-select>
                    </div>
                </div>
              </template>
              <div class="comparison-content">
                <div class="comparison-chart" ref="comparisonChartRef"></div>
                <div class="comparison-insights">
                  <h4>关键发现</h4>
                  <div class="insight-item" v-for="insight in reportData.comparisonInsights" :key="insight.id">
                    <div class="insight-icon">
                      <el-icon :size="16"><component :is="insight.type === 'strength' ? 'Check' : 'Warning'" /></el-icon>
                    </div>
                    <div class="insight-content">
                      <div class="insight-title">{{ insight.title }}</div>
                      <div class="insight-description">{{ insight.description }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="suggestion-card" shadow="hover">
              <template #header>
                <div class="suggestion-header">
                  <h3>个性化学习建议</h3>
                </div>
              </template>
              <div class="suggestion-content">
                <div class="suggestion-list">
                  <div class="suggestion-item" v-for="(suggestion, index) in reportData.learningSuggestions" :key="index">
                    <div class="suggestion-icon">
                      <el-icon :size="20" :color="getSuggestionColor(index)">
                        <component :is="getSuggestionIcon(index)" />
                      </el-icon>
                    </div>
                    <div class="suggestion-text">{{ suggestion }}</div>
                  </div>
                </div>
                <div class="suggestion-actions">
                  <el-button type="primary" @click="createLearningPlan">
                    <el-icon><Calendar /></el-icon>生成学习计划
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      <el-alert title="报告说明" type="info" :closable="false" class="report-note">
        <template #default>
          <p>1. 本报告基于您已完成的{{ reportData.testCount }}次情景测试数据分析生成</p>
          <p>2. 数据更新至{{ reportData.generateTime }}，建议定期查看以获得最新诊断结果</p>
          <p>3. 所有数据仅用于个人能力提升，系统将严格保护您的隐私信息</p>
        </template>
      </el-alert>
    </div>
</template>

<style lang="scss" scoped>
.diagnosis-report {
    min-height: calc(100vh - 64px);
}
  
.report-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .el-breadcrumb {
      font-size: 14px;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
}
  
.overview-card {
    margin-bottom: 15px;
    border-radius: 5px;
    
    &:deep(.el-card__header) {
      padding: 20px;
      border-bottom: 1px solid #ebeef5;
    }
    
    &:deep(.el-card__body) {
      padding: 20px;
    }
}
  
.overview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h2 {
      margin: 0 0 5px;
      font-size: 24px;
      color: #303133;
      font-weight: 600;
    }
    .report-info {
      display: flex;
      gap: 20px;
      font-size: 14px;
      color: #909399;
      .report-date, .report-id {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
    .header-actions {
      display: flex;
      gap: 12px;
    }
}
  
.overview-content {
    .overall-score {
      display: flex;
      align-items: center;
      gap: 50px;
      margin-bottom: 15px;
      padding-bottom: 30px;
      border-bottom: 1px solid #ebeef5;
      
      @media (max-width: 768px) {
        flex-direction: column;
        gap: 20px;
      }
      
      .score-circle {
        position: relative;
        width: 150px;
        height: 150px;
        border-radius: 50%;
        background: linear-gradient(135deg, #9cb2fa 0%, #80a2ff 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: white;
        
        .score-number {
          font-size: 48px;
          font-weight: bold;
          line-height: 1;
        }
        
        .score-label {
          font-size: 14px;
          opacity: 0.9;
          margin-top: 8px;
        }
        
        .score-level {
          position: absolute;
          bottom: -10px;
          background: white;
          color: #409eff;
          padding: 4px 16px;
          border-radius: 20px;
          font-size: 14px;
          font-weight: 500;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
        }
      }
      
      .score-breakdown {
        flex: 1;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 20px;
        
        .breakdown-item {
          .breakdown-label {
            font-size: 14px;
            color: #909399;
            margin-bottom: 8px;
          }
          
          .breakdown-value {
            .percentile {
              .percentile-value {
                font-size: 20px;
                font-weight: bold;
                color: #303133;
                display: block;
                margin-bottom: 8px;
              }
            }
            
            .test-count,
            .study-days {
              display: flex;
              align-items: center;
              gap: 8px;
              font-size: 20px;
              font-weight: bold;
              color: #303133;
              
              .el-icon {
                color: #409eff;
              }
            }
          }
        }
      }
    }
}
  
.dimension-quickview {
    h3 {
      margin: 0 0 20px;
      font-size: 18px;
      color: #303133;
      font-weight: 600;
    }
    
    .dimension-badges {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      
      .dimension-badge {
        display: flex;
        align-items: center;
        padding: 12px;
        border-radius: 12px;
        background: white;
        border: 1px solid #ebeef5;
        cursor: pointer;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }
        
        &.dimension-excellent {
          border-left: 4px solid #67c23a;
        }
        
        &.dimension-good {
          border-left: 4px solid #e6a23c;
        }
        
        &.dimension-improve {
          border-left: 4px solid #f56c6c;
        }
        
        .badge-icon {
          margin-right: 12px;
          
          .el-icon {
            color: #409eff;
          }
        }
        
        .badge-content {
          flex: 1;
          
          .badge-name {
            font-size: 14px;
            color: #606266;
            margin-bottom: 4px;
          }
          
          .badge-score {
            font-size: 18px;
            font-weight: bold;
            color: #303133;
          }
        }
        
        .badge-trend {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          
          .el-icon {
            &.trend-up {
              color: #67c23a;
            }
            
            &.trend-down {
              color: #f56c6c;
            }
          }
        }
      }
    }
}
  
.chart-area,.analysis-area {
    margin-bottom: 15px;
}
  
.chart-card,.analysis-card,.ranking-card,.suggestion-card,.comparison-card {
    border-radius: 5px;
    height: 100%;
    
    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;
    }
    
    &:deep(.el-card__body) {
      padding: 20px;
    }
}
  
.chart-header,.analysis-header, .ranking-header,.suggestion-header, .comparison-header{
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
  
.chart-content {
    .radar-chart,.trend-chart,.comparison-chart {
      width: 100%;
      height: 300px;
    }
}
  
.analysis-content {
    .dimension-tabs {
      &:deep(.el-tabs__nav-wrap) {
        border-bottom: 1px solid #ebeef5;
      }
      
      &:deep(.el-tabs__item) {
        font-weight: 500;
      }
    }
    
    .dimension-detail {
      .dimension-header {
        display: flex;
        gap: 40px;
        margin-bottom: 14px;
        
        @media (max-width: 768px) {
          flex-direction: column;
          gap: 20px;
        }
        
        .dimension-score {
          flex: 1;
          
          .current-score {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 12px;
            
            .score-label {
              font-size: 14px;
              color: #909399;
            }
            
            .score-value {
              font-size: 24px;
              font-weight: bold;
              color: #303133;
            }
          }
        }
        
        .dimension-stats {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 20px;
          
          .stat-item {
            text-align: center;
            
            .stat-label {
              font-size: 12px;
              color: #909399;
              margin-bottom: 4px;
            }
            
            .stat-value {
              font-size: 20px;
              font-weight: bold;
              color: #303133;
            }
          }
        }
      }
      
      .dimension-analysis {
        h4 {
          margin: 0 0 8px;
          font-size: 16px;
          color: #303133;
          font-weight: 600;
          
          &:not(:first-child) {
            margin-top: 8px;
          }
        }
        
        p {
          margin: 0 0 16px;
          color: #606266;
          line-height: 1.6;
        }
        
        .suggestions-list {
          list-style: none;
          padding: 0;
          margin: 0;
          
          li {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            padding: 8px 0;
            color: #606266;
            line-height: 1.5;
            
            .el-icon {
              color: #67c23a;
              flex-shrink: 0;
              margin-top: 2px;
            }
          }
        }
      }
    }
}
  
.ranking-content {
    .scenario-rankings {
      .ranking-item {
        display: flex;
        align-items: center;
        padding: 12px;
        margin-bottom: 10px;
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
        
        .ranking-order {
          margin-right: 12px;
          
          .order-number {
            display: inline-block;
            width: 28px;
            height: 28px;
            line-height: 28px;
            text-align: center;
            border-radius: 50%;
            font-weight: bold;
            
            &.order-first {
              background: #ffd700;
              color: #333;
            }
            
            &.order-second {
              background: #c0c0c0;
              color: #333;
            }
            
            &.order-third {
              background: #cd7f32;
              color: white;
            }
            
            &.order-normal {
              background: #f0f2f5;
              color: #606266;
            }
          }
        }
        
        .ranking-info {
          flex: 1;
          
          .scenario-name {
            font-size: 14px;
            color: #303133;
            font-weight: 500;
            margin-bottom: 8px;
          }
          
          .scenario-score {
            display: flex;
            align-items: center;
            gap: 12px;
            
            .el-progress {
              flex: 1;
            }
            
            .score-text {
              font-size: 14px;
              color: #909399;
              min-width: 40px;
              text-align: right;
            }
          }
        }
        
        .ranking-tag {
          margin-left: 12px;
        }
      }
    }
}
  
.suggestion-content {
    .suggestion-list {
      margin-bottom: 20px;
      
      .suggestion-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px;
        margin-bottom: 10px;
        border-radius: 8px;
        background: #f8f9fa;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .suggestion-icon {
          flex-shrink: 0;
          margin-top: 2px;
        }
        
        .suggestion-text {
          flex: 1;
          font-size: 14px;
          color: #606266;
          line-height: 1.5;
        }
      }
    }
    
    .suggestion-actions {
      text-align: center;
      
      .el-button {
        width: 100%;
      }
    }
}
  
.comparison-content {
    display: flex;
    gap: 40px;
    
    @media (max-width: 768px) {
      flex-direction: column;
      gap: 20px;
    }
    
    .comparison-chart {
      flex: 1;
      min-height: 300px;
    }
    
    .comparison-insights {
      width: 300px;
      
      @media (max-width: 768px) {
        width: 100%;
      }
      
      h4 {
        margin: 0 0 16px;
        font-size: 16px;
        color: #303133;
        font-weight: 600;
      }
      
      .insight-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px;
        margin-bottom: 12px;
        border-radius: 8px;
        background: #f8f9fa;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .insight-icon {
          flex-shrink: 0;
          margin-top: 2px;
          
          .el-icon {
            color: #67c23a;
            
            &.el-icon-warning {
              color: #e6a23c;
            }
          }
        }
        
        .insight-content {
          flex: 1;
          
          .insight-title {
            font-size: 14px;
            color: #303133;
            font-weight: 500;
            margin-bottom: 4px;
          }
          
          .insight-description {
            font-size: 13px;
            color: #606266;
            line-height: 1.5;
          }
        }
      }
    }
  }
  
.report-note {
    margin-top: 20px;
    
    p {
      margin: 4px 0;
      font-size: 14px;
      color: #606266;
    }
}
</style>