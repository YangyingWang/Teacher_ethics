<script setup>
import { Download, Share, ArrowLeft, Document, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { getReportDetailService, getReportDimensionsService } from '@/api/test.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const rId = router.currentRoute.value.query.rId || router.currentRoute.value.query.id

// ====== 页面数据 ======
const loading = ref(false)
const reportData = ref({
  reportId: null,
  qnId: null,
  sceneId: null,
  totalScore: null,
  userTotalScore: null,
  timeSpent: null,
  overallScore: null,
  overallLevel: null,
  summary: '',
  code: '',
  dimScores: [],
  strengths: [],
  risks: [],
  suggestions: []
})
const dimList = ref([])

const dimNameMap = computed(() => {
  const map = {}
  ;(dimList.value || []).forEach(item => {
    map[item.id] = item.name
  })
  return map
})

const dimensions = computed(() => {
  const ds = reportData.value?.dimScores || []
  const sugs = reportData.value?.suggestions || []
  return ds.map(d => {
    const dimId = d.id
    return {
      id: dimId,
      name: dimNameMap.value[dimId] || `维度${dimId}`,
      score: d.score,
      questionCount: d.questionCount,
      wrongCount: d.wrongCount,
      lowCount: d.lowCount,
      suggestions: sugs
        .filter(s => s.dimensionId === dimId)
        .slice()
        .sort((a, b) => (a.priority ?? 3) - (b.priority ?? 3))
    }
  })
})

const activeDimensionId = ref(null)
watch(dimensions, (val) => {
  if (!val?.length) {
    activeDimensionId.value = null
    return
  }
  if (!activeDimensionId.value || !val.some(v => v.id === activeDimensionId.value)) {
    activeDimensionId.value = val[0].id
  }
}, { immediate: true })

// ====== 雷达图（单次测试：只画本次维度得分） ======
const radarChartRef = ref(null)
let radarChart = null

const initRadar = () => {
  if (!radarChartRef.value) return
  if (!radarChart) radarChart = echarts.init(radarChartRef.value)
  updateRadar()
  window.addEventListener('resize', handleResize)
}

const updateRadar = () => {
  if (!radarChart) return

  const ds = dimensions.value || []
  if (!ds.length) {
    radarChart.clear()
    return
  }

  const indicator = ds.map(x => ({ name: x.name, max: 100 }))
  const value = ds.map(x => x.score)

  radarChart.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator,
      radius: '70%',
      splitLine: { lineStyle: { color: '#e4e7ed' } },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      splitArea: { areaStyle: { color: ['rgba(245,247,250,0.9)', 'rgba(245,247,250,0.2)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        name: '本次测试',
        value,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.25 },
        itemStyle: { color: '#409eff' }
      }]
    }]
  })
}

const handleResize = () => radarChart?.resize()

onBeforeUnmount(() => {
  radarChart?.dispose()
  radarChart = null
  window.removeEventListener('resize', handleResize)
})

// ============ 页面逻辑 ============
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

const getLevelTag = (level) => {
  if (level === 0 || level === '0') return '优秀'
  if (level === 1 || level === '1') return '良好'
  if (level === 2 || level === '2') return '合格'
  if (level === 3 || level === '3') return '需改进'
  return String(level ?? '')
}

const levelTagType = (level) => {
  const t = getLevelTag(level)
  if (t.includes('优')) return 'success'
  if (t.includes('良')) return 'warning'
  if (t.includes('合')) return 'info'
  return 'danger'
}

const getItemTagType = (level) => {
  // strength/risk 的 level：1高 2中 3低
  if (level === 1) return 'danger'
  if (level === 2) return 'warning'
  return 'info'
}

const getDimensionClass = (score) => {
  if (score >= 90) return 'dimension-excellent'
  if (score >= 80) return 'dimension-good'
  return 'dimension-improve'
}

onMounted(async () => {
  if (!rId) {
    ElMessage.warning('请选择测试记录以查看报告')
    router.push('/test/home')
    return
  }

  loading.value = true
  try {
    const [rDetail, rDims] = await Promise.all([
      getReportDetailService(rId),
      getReportDimensionsService()
    ])
    reportData.value = rDetail.data || {}
    dimList.value = rDims.data || []
    console.log('报告数据：', reportData.value)
    await nextTick()
    initRadar()
  } catch (e) {
    console.error(e)
    ElMessage.error('加载报告失败')
    router.push('/test/home')
  } finally {
    loading.value = false
  }
})

watch(dimensions, async () => {
  await nextTick()
  updateRadar()
}, { deep: true })

const goBack = () => router.back()

const downloadReport = () => {
  ElMessage.info('下载功能可对接后端 PDF 导出接口')
}

const shareReport = () => {
  ElMessage.info('分享功能开发中')
}

const viewQuizResult = (qnId) => {
  ElMessage.info('查看测试详情')
  router.push({ path: '/test/result', query: { id: qnId } })
}
</script>

<template> 
    <div class="diagnosis-report" v-loading="loading">
      <el-card class="overview-card" shadow="never">
        <template #header>
          <div class="overview-header">
            <div>
              <h2>单次测试诊断报告</h2>
              <div class="report-info">
                <span class="report-id">报告编号：{{ reportData.code }}</span>
              </div>
            </div>
            <div class="header-actions">
                <el-button type="primary" @click="goBack"><el-icon><ArrowLeft /></el-icon>返回</el-button>
                <el-button type="primary" plain @click="viewQuizResult(reportData.qnId)"><el-icon><Document /></el-icon>查看答题详情</el-button>
                <el-button type="primary" plain @click="downloadReport"><el-icon><Download /></el-icon>下载报告</el-button>
                <el-button type="info" plain @click="shareReport"><el-icon><Share /></el-icon>分享</el-button>
            </div>
          </div>
        </template>

        <div class="overview-content">
          <div class="overall-score">
            <div class="score-circle">
              <div class="score-number">{{ reportData?.overallScore ?? '-' }}</div>
              <div class="score-label">综合得分</div>
              <div class="score-level">{{ getLevelTag(reportData?.overallLevel) }}</div>
            </div>
            <div class="score-breakdown">
              <div class="breakdown-item">
                <div class="breakdown-label">能力评级</div>
                <div class="breakdown-value">
                  <el-tag :type="levelTagType(reportData?.overallLevel)" size="large">
                    {{ getLevelTag(reportData?.overallLevel) || '-' }}
                  </el-tag>
                </div>
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">本次得分</div>
                <div class="breakdown-value">
                  <div class="test-count">
                    <el-icon><Document /></el-icon>
                    <span>{{ reportData?.userTotalScore ?? '-' }} / {{ reportData?.totalScore ?? '-' }}</span>
                  </div>
                </div>
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">作答用时</div>
                <div class="breakdown-value">
                  <div class="test-count">
                    <el-icon><Clock /></el-icon>
                    <span>{{ reportData?.timeSpent ?? '-' }} 秒</span>
                  </div>
                </div>
              </div>
              <div class="breakdown-item">
                <div class="breakdown-label">报告摘要</div>
                <div class="breakdown-value">
                  <div class="summary-text">{{ reportData?.summary || '—' }}</div>
                </div>
              </div>
            </div>
          </div>
  
          <div class="dimension-quickview" v-if="dimensions.length">
            <h3>能力维度概览</h3>
            <div class="dimension-badges">
              <div v-for="dimension in dimensions" :key="dimension.id"
                class="dimension-badge" :class="getDimensionClass(dimension.score)"
              >
                <div class="badge-content">
                  <div class="badge-name">{{ dimension.name }}</div>
                  <div class="badge-score">{{ dimension.score }}分</div>
                </div>
                <div class="badge-meta">
                  <el-tag size="small" :type="getScoreType(dimension.score)">{{ dimension.questionCount }}题</el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
  
      <div class="chart-area" v-if="dimensions.length">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
            <el-card class="chart-card" shadow="never">
              <template #header>
                <div class="chart-header">
                  <h3>能力雷达图</h3>
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
                  <h3>优势与风险</h3>
                </div>
              </template>

              <div class="sr-area">
                <div class="sr-block">
                  <div class="sr-title">优势</div>
                  <div v-if="(reportData?.strengths || []).length" class="sr-list">
                    <div v-for="item in reportData.strengths" :key="item.id" class="sr-item">
                      <div class="sr-head">
                        <el-tag size="small" :type="getItemTagType(item.level)">优</el-tag>
                        <span class="sr-text">{{ item.content }}</span>
                      </div>
                      <div v-if="item.evidences?.length" class="sr-evidences">
                        <el-tag
                          v-for="(ev, idx) in item.evidences"
                          :key="idx"
                          size="small"
                          type="info"
                          class="ev-tag"
                        >
                          Q{{ ev.questionId }}：{{ ev.reason }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                  <el-empty v-else description="暂无优势条目" />
                </div>

                <div class="sr-block">
                  <div class="sr-title">风险</div>
                  <div v-if="(reportData?.risks || []).length" class="sr-list">
                    <div v-for="item in reportData.risks" :key="item.id" class="sr-item">
                      <div class="sr-head">
                        <el-tag size="small" :type="getItemTagType(item.level)">险</el-tag>
                        <span class="sr-text">{{ item.content }}</span>
                      </div>
                      <div v-if="item.evidences?.length" class="sr-evidences">
                        <el-tag
                          v-for="(ev, idx) in item.evidences"
                          :key="idx"
                          size="small"
                          type="warning"
                          class="ev-tag"
                        >
                          Q{{ ev.questionId }}：{{ ev.reason }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                  <el-empty v-else description="暂无风险条目" />
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
  
      <div class="analysis-area" v-if="dimensions.length">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
            <el-card class="analysis-card" shadow="never">
              <template #header>
                <div class="analysis-header">
                  <h3>维度详细分析</h3>
                </div>
              </template>
  
              <div class="analysis-content">
                <div class="dimension-tabs">
                  <el-tabs v-model="activeDimensionId" type="border-card">
                    <el-tab-pane
                      v-for="dimension in dimensions"
                      :key="dimension.id"
                      :label="dimension.name"
                      :name="dimension.id"
                    >
                      <div class="dimension-detail">
                        <div class="dimension-header">
                          <div class="dimension-score">
                            <div class="current-score">
                              <span class="score-label">当前得分</span>
                              <span class="score-value">{{ dimension.score }}分</span>
                            </div>
                            <el-progress
                              :percentage="dimension.score"
                              :stroke-width="12"
                              :color="getScoreColor(dimension.score)"
                            />
                          </div>
                          <div class="dimension-stats">
                            <div class="stat-item">
                              <div class="stat-label">题量</div>
                              <div class="stat-value">{{ dimension.questionCount }}</div>
                            </div>
                            <div class="stat-item">
                              <div class="stat-label">客观错题</div>
                              <div class="stat-value">{{ dimension.wrongCount }}</div>
                            </div>
                            <div class="stat-item">
                              <div class="stat-label">主观低分</div>
                              <div class="stat-value">{{ dimension.lowCount }}</div>
                            </div>
                          </div>
                        </div>
  
                        <div class="dimension-analysis">
                          <h4>改进建议</h4>
                          <div v-if="dimension.suggestions?.length" class="suggestions-list">
                            <div v-for="s in dimension.suggestions" :key="s.id" class="suggestion-item">
                              <div class="suggestion-text">
                                <div class="suggestion-title">
                                  <el-tag size="small" :type="getItemTagType(s.priority)">P{{ s.priority }}</el-tag>
                                  <span>{{ s.title }}</span>
                                </div>
                                <div class="suggestion-body">
                                  <div class="c">{{ s.content }}</div>
                                </div>
                              </div>
                            </div>
                          </div>
                          <el-empty v-else description="该维度暂无建议" />
                        </div>
                      </div>
                    </el-tab-pane>
                  </el-tabs>
                </div>
              </div>
            </el-card>
          </el-col>
  
          <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
            <el-card class="suggestion-card" shadow="hover">
              <template #header>
                <div class="suggestion-header">
                  <h3>三条行动建议（本次）</h3>
                </div>
              </template>

              <div class="suggestion-content">
                <div v-if="(reportData?.suggestions || []).length" class="suggestion-list">
                  <div class="suggestion-item" v-for="s in reportData.suggestions" :key="s.id">
                    <div class="suggestion-text">
                      <div class="suggestion-title">
                        <el-tag size="small" :type="getItemTagType(s.priority)">P{{ s.priority }}</el-tag>
                        <span>{{ dimNameMap[s.dimensionId] || ('维度' + s.dimensionId) }}</span>
                      </div>
                      <div class="suggestion-body">
                        <div class="t">{{ s.title }}</div>
                        <div class="c">{{ s.content }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="暂无建议" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <el-alert title="报告说明" type="info" :closable="false" class="report-note">
        <template #default>
          <p>1. 本报告仅基于单次情景测试（测试编号：{{ reportData.qnId }}）生成</p>
          <p>2. 若你修改了测试或重新提交，请重新生成报告以获取最新结果</p>
          <p>3. 所有数据仅用于个人能力提升，系统将严格保护您的隐私信息</p>
        </template>
      </el-alert>
    </div>
</template>

<style lang="scss" scoped>
.diagnosis-report {
    min-height: calc(100vh - 64px);
}
  
.overview-card {
    margin-bottom: 15px;
    border-radius: 5px;
    border: 1px solid #eef2f7;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
    transition: box-shadow 0.3s;
    
    &:deep(.el-card__header) {
      padding: 20px;
      border-bottom: 1px solid #eef2f7;
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
      margin: 0 0 6px;
      font-size: 22px;
      color: #1f2f3e;
      font-weight: 650;
      letter-spacing: -0.01em;
    }
    .report-info {
      display: flex;
      gap: 20px;
      font-size: 14px;
      color: #6b7a8a;

      .report-date, .report-id {
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }
    .header-actions {
      display: flex;
      gap: 12px;

      .el-button {
        font-weight: 500;
        border-radius: 6px;
        padding: 10px 18px;
        transition: all 0.2s;
      }
    }
}
  
.overview-content {
    .overall-score {
      display: flex;
      align-items: center;
      gap: 50px;
      margin-bottom: 15px;
      padding-bottom: 30px;
      border-bottom: 1px solid #eef2f7;
      
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
        transition: transform 0.2s;

        &:hover {
          transform: scale(1.02);
        }
        
        .score-number {
          font-size: 48px;
          font-weight: bold;
          line-height: 1;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
        
        .score-label {
          font-size: 14px;
          opacity: 0.95;
          margin-top: 8px;
          letter-spacing: 0.5px;
        }
        
        .score-level {
          position: absolute;
          bottom: -10px;
          background: white;
          color: #409eff;
          padding: 5px 18px;
          border-radius: 30px;
          font-size: 14px;
          font-weight: 600;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
          border: 1px solid rgba(64, 158, 255, 0.1);
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
            color: #7a8b9b;
            margin-bottom: 8px;
            letter-spacing: 0.3px;
          }
          
          .breakdown-value {
            .test-count {
              display: flex;
              align-items: center;
              gap: 10px;
              font-size: 20px;
              font-weight: 700;
              color: #1f2f3e;
              
              .el-icon {
                color: #409eff;
                font-size: 22px;
              }
            }

            .summary-text {
              font-size: 15px;
              color: #475e6e;
              line-height: 1.6;
              max-width: 300px;
            }
          }
        }
      }
    }
}

.dimension-quickview {
    h3 {
      margin: 0 0 20px;
      font-size: 17px;
      color: #1f2f3e;
      font-weight: 650;
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: '';
        width: 5px;
        height: 18px;
        background: #409eff;
        border-radius: 3px;
        display: inline-block;
      }
    }
    
    .dimension-badges {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      
      .dimension-badge {
        display: flex;
        align-items: center;
        padding: 14px 16px;
        border-radius: 10px;
        background: #fff;
        border: 1px solid #eef2f7;
        cursor: pointer;
        transition: all 0.25s ease;
        
        &:hover {
          transform: translateY(-3px);
          box-shadow: 0 8px 16px rgba(64, 158, 255, 0.08);
          border-color: #d0e2ff;
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
        
        .badge-content {
          flex: 1;
          
          .badge-name {
            font-size: 14px;
            color: #566b7c;
            margin-bottom: 4px;
            font-weight: 500;
          }
          
          .badge-score {
            font-size: 18px;
            font-weight: 700;
            color: #1f2f3e; 
          }
        }
        
        .badge-meta {
          margin-left: 8px;
        }
      }
    }
}
  
.chart-area,.analysis-area {
    margin-bottom: 15px;
}

.chart-card,.analysis-card,.suggestion-card {
    border-radius: 5px;
    border: 1px solid #eef2f7;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
    height: 100%;
    transition: box-shadow 0.3s, transform 0.2s;
    
    &:hover {
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
    }
    
    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #eef2f7;
    }
    
    &:deep(.el-card__body) {
      padding: 20px;
    }
}
  
.chart-header,.analysis-header, .suggestion-header{
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h3 {
      margin: 0;
      font-size: 17px;
      color: #1f2f3e;
      font-weight: 650;
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: '';
        width: 5px;
        height: 18px;
        background: #409eff;
        border-radius: 3px;
        display: inline-block;
      }
    }
}
  
.chart-content {
  .radar-chart {
    width: 100%;
    height: 300px;
    background: #fbfdff;
    border-radius: 8px;
    padding: 8px;
  }
}

// 优势与风险区域
.sr-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sr-block {
  .sr-title {
    font-size: 16px;
    font-weight: 650;
    color: #1f2f3e;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 6px;

    &::before {
      content: '';
      width: 4px;
      height: 16px;
      background: #409eff;
      border-radius: 2px;
      display: inline-block;
    }
  }

  .sr-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .sr-item {
    padding: 10px;
    background: #fafdff;
    border-radius: 8px;
    border-left: 4px solid #409eff;
    transition: background 0.2s;

    &:hover {
      background: #f5faff;
    }

    .sr-head {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      margin-bottom: 5px;

      .sr-text {
        font-size: 15px;
        color: #1f2f3e;
        font-weight: 500;
        line-height: 1.5;
        flex: 1;
      }
    }

    .sr-evidences {
      display: flex;
      flex-wrap: wrap;
      gap: 5px;
      padding-left: 5px;

      .ev-tag {
        background: #f0f5fa;
        border: none;
        color: #566b7c;
        font-size: 12px;
        padding: 2px;
        border-radius: 30px;
      }
    }
  }
}

.analysis-content {
    .dimension-tabs {
      &:deep(.el-tabs__nav-wrap) {
        border-bottom: 1px solid #eef2f7;
      }
      
      &:deep(.el-tabs__item) {
        font-weight: 500;
        color: #566b7c;

        &.is-active {
          color: #409eff;
          font-weight: 650;
        }
      }

      &:deep(.el-tabs__active-bar) {
        background: #409eff;
        height: 3px;
      }
    }

    .dimension-detail {
      padding: 4px 0;

      .dimension-header {
        display: flex;
        gap: 40px;
        margin-bottom: 20px;

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
              color: #7a8b9b;
            }

            .score-value {
              font-size: 26px;
              font-weight: 700;
              color: #1f2f3e;
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
              color: #7a8b9b;
              margin-bottom: 6px;
            }

            .stat-value {
              font-size: 22px;
              font-weight: 700;
              color: #1f2f3e;
            }
          }
        }
      }

      .dimension-analysis {
        h4 {
          margin: 20px 0 12px;
          font-size: 16px;
          color: #1f2f3e;
          font-weight: 650;
        }

        .suggestions-list {
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .suggestion-item {
          padding: 16px;
          background: #fafdff;
          border-radius: 8px;
          border-left: 4px solid #409eff;

          .suggestion-text {
            .suggestion-title {
              display: flex;
              align-items: center;
              gap: 10px;
              margin-bottom: 8px;

              span {
                font-size: 16px;
                font-weight: 650;
                color: #1f2f3e;
              }
            }

            .suggestion-body {
              padding-left: 34px;

              .c {
                color: #475e6e;
                line-height: 1.7;
                font-size: 14px;
              }
            }
          }
        }
    }
  }
}
  
.suggestion-content {
    .suggestion-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      
      .suggestion-item {
        padding: 10px;
        background: #fafdff;
        border-radius: 8px;
        border-left: 4px solid #409eff;
        transition: all 0.2s;

        &:hover {
          background: #f5faff;
          transform: translateX(2px);
        }
        
        .suggestion-text {
          .suggestion-title {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 5px;

            span {
              font-size: 15px;
              font-weight: 650;
              color: #1f2f3e;
            }
          }
          .suggestion-body {
            padding-left: 5px;

            .t {
              font-size: 15px;
              font-weight: 600;
              color: #1f2f3e;
              margin-bottom: 5px;
            }

            .c {
              color: #475e6e;
              line-height: 1.5;
              font-size: 14px;
            }
          }
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

.report-note {
  margin-top: 20px;

  p {
    margin: 4px 0;
    font-size: 14px;
    color: #606266;
  }
}

/* 响应式微调 */
@media (max-width: 992px) {
  .diagnosis-report {
    padding: 16px;
  }

  .overview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;

    .header-actions {
      width: 100%;
      flex-wrap: wrap;
    }
  }

  .chart-area .el-col {
    margin-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .overall-score {
    .score-circle {
      width: 140px;
      height: 140px;

      .score-number {
        font-size: 40px;
      }
    }
  }

  .dimension-badges {
    grid-template-columns: 1fr !important;
  }

  .radar-chart {
    height: 260px !important;
  }
}
</style>