<script setup>
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { ref, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Refresh,
  Trophy,
  Lightning,
  Warning,
  InfoFilled,
  Back
} from '@element-plus/icons-vue'

import {
  getEvaluationDetailService,
  getEvaluationDimensionsService
} from '@/api/simulate.js'

const router = useRouter()
const route = useRoute()

const sessionId = computed(() => route.query.sessionId || route.query.id || '')

const radarChartRef = ref(null)
let radarChart = null

const loading = ref(true)
const error = ref('')
const evaluationDetail = ref({})
const dimensionMap = ref({})

const loadDimensions = async () => {
  try {
    const res = await getEvaluationDimensionsService()
    const dims = res.data || []
    const map = {}
    dims.forEach(d => {
      map[String(d.id)] = {
        name: d.name,
        type: d.type
      }
    })
    dimensionMap.value = map
  } catch (err) {
    console.error('获取维度列表失败', err)
    ElMessage.error('获取维度列表失败，部分维度名称可能无法显示')
  }
}

const loadEvaluationDetail = async () => {
  if (!sessionId.value) {
    error.value = '缺少 sessionId 参数'
    loading.value = false
    return
  }

  try {
    const res = await getEvaluationDetailService(sessionId.value)
    evaluationDetail.value = res.data || {}
    error.value = ''
  } catch (err) {
    console.error('获取评估详情失败', err)
    error.value = err?.message || '加载失败，请稍后重试'
    ElMessage.error('加载评估详情失败')
  } finally {
    loading.value = false
  }
}

const getLevel = (level) => {
  const l = Number(level || 0)
  if (l == 0) return '优秀'
  if (l == 1) return '良好'
  if (l == 2) return '合格'
  return '需改进'
}

const getScoreColor = (score) => {
  const s = Number(score || 0)
  if (s >= 90) return '#67c23a'
  if (s >= 80) return '#e6a23c'
  if (s >= 70) return '#409eff'
  return '#f56c6c'
}

const getDimName = (dimensionId) => {
  if (dimensionId === null || dimensionId === undefined || dimensionId === '') return ''
  const id = String(dimensionId)
  return dimensionMap.value[id]?.name || `维度${dimensionId}`
}

const getDimType = (dimensionId) => {
  if (dimensionId === null || dimensionId === undefined || dimensionId === '') return undefined
  const id = String(dimensionId)
  return dimensionMap.value[id]?.type
}

const dimScores = computed(() => evaluationDetail.value.dimScores || [])

const radarData = computed(() => {
  return dimScores.value.map(ds => ({
    name: getDimName(ds.id),
    value: Number(ds.score || 0),
    type: getDimType(ds.id)
  }))
})

const mainDimScores = computed(() => dimScores.value.filter(ds => getDimType(ds.id) === 0))
const processDimScores = computed(() => dimScores.value.filter(ds => getDimType(ds.id) === 1))

const initRadarChart = () => {
  if (!radarChartRef.value || radarData.value.length === 0) return
  if (radarChart) radarChart.dispose()
  radarChart = echarts.init(radarChartRef.value)

  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, data: ['维度得分'] },
    radar: {
      indicator: radarData.value.map(item => ({ name: item.name, max: 100 })),
      center: ['50%', '46%'],
      radius: '62%',
      splitNumber: 5,
      splitArea: { areaStyle: { color: ['rgba(64, 158, 255, 0.05)', 'rgba(64, 158, 255, 0.02)'] } },
      splitLine: { lineStyle: { color: '#dcdfe6' } },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      name: { color: '#606266', fontSize: 13 }
    },
    series: [{
      name: '维度得分',
      type: 'radar',
      data: [{
        value: radarData.value.map(item => item.value),
        name: '维度得分',
        areaStyle: { color: 'rgba(64, 158, 255, 0.18)' },
        lineStyle: { width: 2, color: '#409eff' },
        itemStyle: { color: '#409eff' },
        symbol: 'circle',
        symbolSize: 6
      }]
    }]
  }
  radarChart.setOption(option)
}

const handleResize = () => radarChart?.resize()
const handleRefresh = async () => {
  loading.value = true
  error.value = ''
  await loadDimensions()
  await loadEvaluationDetail()
  await nextTick()
  initRadarChart()
}
const goBack = () => router.back()

onMounted(async () => {
  await loadDimensions()
  await loadEvaluationDetail()
  await nextTick()
  initRadarChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
  window.removeEventListener('resize', handleResize)
})

watch(radarData, async () => {
  await nextTick()
  initRadarChart()
}, { deep: true })
</script>

<template>
  <div class="evaluation-detail">
    <!-- 头部操作栏 -->
    <div class="page-header">
      <div class="header-info">
        <h4>单次训练评估</h4>
        <p>基于本次模拟推演生成的详细决策评估报告</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>返回
        </el-button>
      </div>
    </div>

    <!-- 加载/错误状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>
    <div v-else-if="error" class="error-container">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="handleRefresh">重试</el-button>
        </template>
      </el-result>
    </div>

    <div v-else class="detail-content">
      <!-- 概览区域：总分 + 关键指标 -->
      <div class="overview-section">
        <div class="score-box">
          <div class="score-circle">
            <div class="score-number">{{ evaluationDetail.overallScore ?? '--' }}</div>
            <div class="score-label">决策能力得分</div>
            <div class="score-level">{{ getLevel(evaluationDetail.overallLevel) }}</div>
          </div>
        </div>

        <div class="metrics-box">
          <div class="metric-item">
            <div class="metric-icon"><el-icon><Trophy /></el-icon></div>
            <div class="metric-content">
              <div class="metric-label">决策风格</div>
              <div class="metric-value">{{
                evaluationDetail.style === 0 ? '稳健型' :
                evaluationDetail.style === 1 ? '权衡型' :
                evaluationDetail.style === 2 ? '激进型' : '--'
              }}</div>
            </div>
          </div>
          <div class="metric-item">
            <div class="metric-icon"><el-icon><Lightning /></el-icon></div>
            <div class="metric-content">
              <div class="metric-label">风险等级</div>
              <div class="metric-value">{{
                evaluationDetail.riskLevel === 1 ? '高' :
                evaluationDetail.riskLevel === 2 ? '中' :
                evaluationDetail.riskLevel === 3 ? '低' : '--'
              }}</div>
            </div>
          </div>
          <div class="metric-item">
            <div class="metric-icon"><el-icon><Warning /></el-icon></div>
            <div class="metric-content">
              <div class="metric-label">评估等级</div>
              <div class="metric-value">{{
                evaluationDetail.overallLevel === 0 ? '优秀' :
                evaluationDetail.overallLevel === 1 ? '良好' :
                evaluationDetail.overallLevel === 2 ? '合格' : '需改进'
              }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 综合总结 -->
      <div class="summary-section">
        <div class="section-title">
          <span>综合总结</span>
        </div>
        <div class="summary-text">{{ evaluationDetail.summary || '暂无总结' }}</div>
      </div>

      <!-- 雷达图 -->
      <div v-if="radarData.length > 0" class="radar-section">
        <div class="section-title">
          <span>维度能力雷达图</span>
        </div>
        <div ref="radarChartRef" class="radar-chart"></div>
      </div>

      <!-- 维度得分列表（主维度/过程维度） -->
      <div v-if="dimScores.length" class="dimensions-section">
        <div class="section-title">
          <span>各维度得分详情</span>
        </div>

        <!-- 主维度 -->
        <div v-if="mainDimScores.length" class="dim-group">
          <div class="dim-group-title">主维度</div>
          <div class="dimension-list">
            <div v-for="ds in mainDimScores" :key="ds.id" class="dimension-row">
              <div class="dimension-info">
                <span class="dimension-name">{{ getDimName(ds.id) }}</span>
                <el-tag size="small" type="primary">主维度</el-tag>
              </div>
              <div class="dimension-progress">
                <el-progress
                  :percentage="Number(ds.score || 0)"
                  :color="getScoreColor(ds.score)"
                  :stroke-width="8"
                  :show-text="false"
                />
                <span class="progress-value">{{ ds.score }}/100</span>
              </div>
              <div class="dimension-meta">
                <span>权重：{{ ds.weight ?? '--' }}</span>
                <span v-if="ds.comment" class="dimension-comment">评语：{{ ds.comment }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 过程维度 -->
        <div v-if="processDimScores.length" class="dim-group">
          <div class="dim-group-title">过程维度</div>
          <div class="dimension-list">
            <div v-for="ds in processDimScores" :key="ds.id" class="dimension-row">
              <div class="dimension-info">
                <span class="dimension-name">{{ getDimName(ds.id) }}</span>
                <el-tag size="small" type="success">过程维度</el-tag>
              </div>
              <div class="dimension-progress">
                <el-progress
                  :percentage="Number(ds.score || 0)"
                  :color="getScoreColor(ds.score)"
                  :stroke-width="8"
                  :show-text="false"
                />
                <span class="progress-value">{{ ds.score }}/100</span>
              </div>
              <div class="dimension-meta">
                <span>权重：{{ ds.weight ?? '--' }}</span>
                <span v-if="ds.comment" class="dimension-comment">评语：{{ ds.comment }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 优势/风险/关键节点三栏 -->
      <div class="triple-section">
        <div v-if="evaluationDetail.strengths?.length" class="triple-card">
          <div class="card-header">
            <h5>优势表现</h5>
          </div>
          <div class="card-body">
            <div v-for="item in evaluationDetail.strengths" :key="item.id" class="card-item">
              <div class="item-tag">
                <el-tag size="small" :type="item.level === 1 ? 'success' : item.level === 2 ? 'warning' : 'info'">
                  {{ item.level === 1 ? '高' : item.level === 2 ? '中' : '低' }}
                </el-tag>
                <span class="item-dim">{{ getDimName(item.dimensionId) }}</span>
              </div>
              <p class="item-content">{{ item.content }}</p>
              <div v-for="(ev, idx) in (item.evidences || [])" :key="idx" class="evidence">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ ev.reason }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="evaluationDetail.risks?.length" class="triple-card">
          <div class="card-header">
            <h5>风险提示</h5>
          </div>
          <div class="card-body">
            <div v-for="item in evaluationDetail.risks" :key="item.id" class="card-item">
              <div class="item-tag">
                <el-tag size="small" :type="item.level === 1 ? 'danger' : item.level === 2 ? 'warning' : 'info'">
                  {{ item.level === 1 ? '高' : item.level === 2 ? '中' : '低' }}
                </el-tag>
                <span class="item-dim">{{ getDimName(item.dimensionId) }}</span>
              </div>
              <p class="item-content">{{ item.content }}</p>
              <div v-for="(ev, idx) in (item.evidences || [])" :key="idx" class="evidence">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ ev.reason }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="evaluationDetail.criticalMoments?.length" class="triple-card">
          <div class="card-header">
            <h5>关键节点</h5>
          </div>
          <div class="card-body">
            <div v-for="item in evaluationDetail.criticalMoments" :key="item.id" class="card-item">
              <div class="item-tag">
                <el-tag size="small" :type="item.level === 1 ? 'danger' : item.level === 2 ? 'warning' : 'info'">
                  {{ item.level === 1 ? '高' : item.level === 2 ? '中' : '低' }}
                </el-tag>
                <span class="item-dim">{{ getDimName(item.dimensionId) }}</span>
              </div>
              <p class="item-content">{{ item.content }}</p>
              <div v-for="(ev, idx) in (item.evidences || [])" :key="idx" class="evidence">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ ev.reason }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 改进建议 -->
      <div v-if="evaluationDetail.suggestions?.length" class="suggestions-section">
        <div class="section-title">
          <span>改进建议</span>
        </div>
        <div class="suggestions-list">
          <div v-for="sug in evaluationDetail.suggestions" :key="sug.id" class="suggestion-item">
            <div class="suggestion-priority">
              <el-tag size="small" :type="sug.priority === 1 ? 'danger' : sug.priority === 2 ? 'warning' : 'info'">
                {{ sug.priority === 1 ? '高' : sug.priority === 2 ? '中' : '低' }}
              </el-tag>
            </div>
            <div class="suggestion-body">
              <div class="suggestion-header">
                <span class="suggestion-title">{{ sug.title }}</span>
                <span class="suggestion-dim">{{ sug.dimensionId == null ? '通用' : getDimName(sug.dimensionId) }}</span>
              </div>
              <p class="suggestion-content">{{ sug.content }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="
          !dimScores.length &&
          !evaluationDetail.strengths?.length &&
          !evaluationDetail.risks?.length &&
          !evaluationDetail.criticalMoments?.length &&
          !evaluationDetail.suggestions?.length
        "
        description="暂无评估数据"
      />
    </div>
  </div>
</template>

<style scoped>
.evaluation-detail {
  min-height: calc(100vh - 64px);
}

/* 头部操作栏 - 增加渐变和质感 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 28px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 8px;
  border: 1px solid #e8edf5;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  transition: box-shadow 0.3s ease;
}
.page-header:hover {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.08);
}
.header-info h4 {
  margin: 0 0 6px;
  font-size: 20px;
  color: #1f2f3e;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.header-info p {
  margin: 0;
  font-size: 14px;
  color: #6b7a8a;
  display: flex;
  align-items: center;
  gap: 6px;
}
.header-info p::before {
  content: '';
  width: 4px;
  height: 14px;
  background: #409eff;
  border-radius: 2px;
  display: inline-block;
}
.header-actions {
  display: flex;
  gap: 12px;
}
.header-actions .el-button {
  font-weight: 500;
  border-radius: 6px;
  padding: 10px 18px;
  transition: all 0.2s;
}
.header-actions .el-button--primary {
  background: linear-gradient(135deg, #409eff 0%, #5b8cff 100%);
  border: none;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}
.header-actions .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.4);
}

.loading-container,
.error-container {
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8edf5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 概览区域 - 提升质感 */
.overview-section {
  display: flex;
  gap: 32px;
  background: #fff;
  padding: 28px 32px;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  transition: box-shadow 0.3s;
}
.overview-section:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
}

.score-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 180px;
}
.score-circle {
  position: relative;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: linear-gradient(145deg, #9cb2fa, #6e9eff);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.25), inset 0 -2px 4px rgba(0,0,0,0.05);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  transition: transform 0.2s;
}
.score-circle:hover {
  transform: scale(1.02);
}
.score-number {
  font-size: 46px;
  font-weight: 700;
  line-height: 1;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.score-label {
  margin-top: 6px;
  font-size: 14px;
  opacity: 0.95;
  letter-spacing: 0.5px;
}
.score-level {
  position: absolute;
  bottom: -12px;
  padding: 5px 18px;
  border-radius: 30px;
  background: #fff;
  color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.25);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  border: 1px solid rgba(64, 158, 255, 0.1);
}
.score-desc {
  margin-top: 12px;
  font-size: 15px;
  color: #566b7c;
}
.score-desc strong {
  color: #1f2f3e;
  font-weight: 700;
}

.metrics-box {
  flex: 1;
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.metric-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}
.metric-item:hover {
  background: #fafcff;
}
.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ecf5ff 0%, #e4f0ff 100%);
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  box-shadow: inset 0 1px 2px rgba(255,255,255,0.8), 0 2px 4px rgba(64, 158, 255, 0.1);
}
.metric-content {
  display: flex;
  flex-direction: column;
}
.metric-label {
  font-size: 13px;
  color: #7a8b9b;
  margin-bottom: 4px;
  letter-spacing: 0.3px;
}
.metric-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2f3e;
  line-height: 1.2;
}

/* 综合总结 */
.summary-section {
  background: #fff;
  padding: 22px 28px;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}
.section-title {
  margin-bottom: 16px;
  font-size: 17px;
  font-weight: 650;
  color: #1f2f3e;
  display: flex;
  align-items: center;
}
.section-title span {
  position: relative;
  padding-left: 14px;
}
.section-title span::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  bottom: 3px;
  width: 4px;
  background: linear-gradient(to bottom, #409eff, #80b4ff);
  border-radius: 4px;
}
.summary-text {
  color: #475e6e;
  line-height: 1.9;
  font-size: 15px;
  background: #fafdff;
  padding: 16px 18px;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

/* 雷达图区域 */
.radar-section {
  background: #fff;
  padding: 22px 28px;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}
.radar-chart {
  width: 100%;
  height: 380px;
  margin-top: 8px;
  background: #fbfdff;
  border-radius: 8px;
  padding: 8px;
}

/* 维度得分区域 */
.dimensions-section {
  background: #fff;
  padding: 22px 28px;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}
.dim-group + .dim-group {
  margin-top: 32px;
}
.dim-group-title {
  font-size: 16px;
  font-weight: 650;
  color: #1f2f3e;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dim-group-title::before {
  content: '';
  width: 6px;
  height: 18px;
  background: #409eff;
  border-radius: 3px;
  display: inline-block;
}
.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.dimension-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
  padding: 14px 0;
  border-bottom: 1px solid #f0f5fa;
  transition: background 0.2s;
}
.dimension-row:hover {
  background: #fafdff;
  border-radius: 8px;
  padding-left: 12px;
  padding-right: 12px;
}
.dimension-row:last-child {
  border-bottom: none;
}
.dimension-info {
  min-width: 160px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.dimension-name {
  font-weight: 650;
  color: #1f2f3e;
  font-size: 15px;
}
.dimension-progress {
  flex: 1;
  min-width: 220px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.progress-value {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  min-width: 55px;
  text-align: right;
}
.dimension-meta {
  width: 100%;
  margin-left: 172px;
  font-size: 13px;
  color: #7a8b9b;
  display: flex;
  gap: 28px;
}
.dimension-comment {
  color: #566b7c;
  font-style: italic;
}

/* 三栏卡片区域 */
.triple-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.triple-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}
.triple-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.08);
}
.card-header {
  padding: 18px 22px 12px;
  border-bottom: 1px solid #eef2f7;
}
.card-header h5 {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
  color: #1f2f3e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-header h5::before {
  content: '';
  width: 5px;
  height: 18px;
  background: #409eff;
  border-radius: 3px;
  display: inline-block;
}
.card-body {
  padding: 18px 22px 22px;
  flex: 1;
}
.card-item {
  margin-bottom: 22px;
}
.card-item:last-child {
  margin-bottom: 0;
}
.item-tag {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.item-dim {
  font-size: 14px;
  font-weight: 600;
  color: #1f2f3e;
}
.item-content {
  margin: 10px 0;
  color: #475e6e;
  font-size: 14px;
  line-height: 1.7;
}
.evidence {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  margin-top: 10px;
  background: #f8fbfe;
  border-radius: 8px;
  font-size: 13px;
  color: #566b7c;
  line-height: 1.6;
  border-left: 3px solid #409eff;
}
.evidence .el-icon {
  margin-top: 2px;
  color: #409eff;
  flex-shrink: 0;
  font-size: 15px;
}

/* 改进建议区域 */
.suggestions-section {
  background: #fff;
  padding: 22px 28px;
  border-radius: 10px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}
.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.suggestion-item {
  display: flex;
  gap: 20px;
  padding: 18px 0;
  border-bottom: 1px solid #f0f5fa;
  transition: background 0.2s;
}
.suggestion-item:hover {
  background: #fafdff;
  border-radius: 8px;
  padding-left: 16px;
  padding-right: 16px;
}
.suggestion-item:last-child {
  border-bottom: none;
}
.suggestion-priority {
  min-width: 70px;
}
.suggestion-body {
  flex: 1;
}
.suggestion-header {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 8px;
}
.suggestion-title {
  font-size: 16px;
  font-weight: 650;
  color: #1f2f3e;
}
.suggestion-dim {
  font-size: 13px;
  color: #7a8b9b;
  background: #f0f5fa;
  padding: 3px 10px;
  border-radius: 30px;
}
.suggestion-content {
  margin: 0;
  color: #475e6e;
  font-size: 14px;
  line-height: 1.8;
}

/* 响应式微调 */
@media (max-width: 1200px) {
  .triple-section {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 992px) {
  .overview-section {
    flex-direction: column;
    align-items: center;
  }
  .metrics-box {
    width: 100%;
    justify-content: space-evenly;
  }
  .dimension-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .dimension-meta {
    margin-left: 0;
  }
}
@media (max-width: 768px) {
  .evaluation-detail {
    padding: 16px;
  }
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
  }
  .header-actions {
    width: 100%;
  }
  .metrics-box {
    flex-direction: column;
    gap: 18px;
  }
  .metric-item {
    width: 100%;
  }
  .radar-chart {
    height: 320px;
  }
  .suggestion-item {
    flex-direction: column;
    gap: 8px;
  }
}
</style>