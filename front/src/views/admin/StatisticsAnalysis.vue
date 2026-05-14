<template>
  <div class="statistics-page">
    <!-- 筛选卡片 -->
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">分类统计分析</span>
          <div class="header-actions">
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="教师类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 180px">
            <el-option label="教学型" :value="0" />
            <el-option label="科研型" :value="1" />
            <el-option label="综合型" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="filters.depId" placeholder="全部" clearable style="width: 220px">
            <el-option
              v-for="item in departmentOptions"
              :key="item.value"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据总览卡片 -->
    <el-row :gutter="20" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-title">教师总数</div>
          <div class="summary-value">{{ overview.summary.teacherTotal }}</div>
          <div class="summary-icon"><el-icon><User /></el-icon></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-title">评估记录数</div>
          <div class="summary-value">{{ overview.summary.assessmentCount }}</div>
          <div class="summary-icon"><el-icon><DataBoard /></el-icon></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-title">治理研修会话数</div>
          <div class="summary-value">{{ overview.summary.simulationCount }}</div>
          <div class="summary-icon"><el-icon><Headset /></el-icon></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-title">平均综合得分</div>
          <div class="summary-value">{{ overview.summary.avgAssessmentScore }}</div>
          <div class="summary-icon"><el-icon><Star /></el-icon></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行1：教师类型分布 + 院系教师分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">教师类型分布</span></template>
          <div ref="teacherTypeChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">院系教师分布</span></template>
          <div ref="departmentChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行2：评估等级分布 + 近6个月评估趋势 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">评估等级分布</span></template>
          <div ref="levelChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">近6个月评估趋势</span></template>
          <div ref="assessmentTrendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行3：近6个月教师注册趋势 + 治理研修参与情况 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">近6个月教师注册趋势</span></template>
          <div ref="registerTrendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">治理研修参与情况</span></template>
          <div ref="simulationChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, DataBoard, Headset, Star } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getStatisticsService, getDepartmentOptionsService } from '@/api/admin'

const filters = reactive({
  type: undefined,
  depId: undefined
})

const departmentOptions = ref([])
const overview = reactive({
  summary: {
    teacherTotal: 0,
    assessmentCount: 0,
    simulationCount: 0,
    avgAssessmentScore: 0
  },
  teacherTypeStats: [],
  departmentStats: [],
  levelStats: [],
  assessmentTrend: [],
  registerTrend: [],
  simulationStats: []
})

const teacherTypeChartRef = ref()
const departmentChartRef = ref()
const levelChartRef = ref()
const assessmentTrendChartRef = ref()
const registerTrendChartRef = ref()
const simulationChartRef = ref()

let teacherTypeChart = null
let departmentChart = null
let levelChart = null
let assessmentTrendChart = null
let registerTrendChart = null
let simulationChart = null

const loadDepartmentOptions = async () => {
  const res = await getDepartmentOptionsService()
  departmentOptions.value = res.data || []
}

const loadData = async () => {
  try {
    const res = await getStatisticsService({
      type: filters.type,
      depId: filters.depId
    })

    Object.assign(overview.summary, res.data.summary || {})
    overview.teacherTypeStats = res.data.teacherTypeStats || []
    overview.departmentStats = res.data.departmentStats || []
    overview.levelStats = res.data.levelStats || []
    overview.assessmentTrend = res.data.assessmentTrend || []
    overview.registerTrend = res.data.registerTrend || []
    overview.simulationStats = res.data.simulationStats || []

    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error?.message || '统计数据加载失败')
  }
}

const resetFilters = () => {
  filters.type = undefined
  filters.depId = undefined
  loadData()
}

const initCharts = () => {
  teacherTypeChart = echarts.init(teacherTypeChartRef.value)
  departmentChart = echarts.init(departmentChartRef.value)
  levelChart = echarts.init(levelChartRef.value)
  assessmentTrendChart = echarts.init(assessmentTrendChartRef.value)
  registerTrendChart = echarts.init(registerTrendChartRef.value)
  simulationChart = echarts.init(simulationChartRef.value)
}

const renderCharts = () => {
  if (!teacherTypeChart) initCharts()

  teacherTypeChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#606266' } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      data: overview.teacherTypeStats.map(item => ({ name: item.name, value: item.value })),
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }
    }]
  })

  departmentChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      axisLabel: { rotate: 30, color: '#606266' },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      data: overview.departmentStats.map(item => item.name)
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: overview.departmentStats.map(item => item.value),
      barMaxWidth: 40,
      itemStyle: { color: '#409eff', borderRadius: [6, 6, 0, 0] }
    }]
  })

  levelChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#606266' } },
    series: [{
      type: 'pie',
      radius: '60%',
      data: overview.levelStats.map(item => ({ name: item.name, value: item.value })),
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }
    }]
  })

  assessmentTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: overview.assessmentTrend.map(item => item.name),
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      name: '评估次数',
      type: 'line',
      smooth: true,
      data: overview.assessmentTrend.map(item => item.value),
      lineStyle: { color: '#409eff', width: 3 },
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#409eff' }
    }]
  })

  registerTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: overview.registerTrend.map(item => item.name),
      axisLine: { lineStyle: { color: '#dcdfe6' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      name: '注册人数',
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' },
      data: overview.registerTrend.map(item => item.value),
      lineStyle: { color: '#409eff', width: 3 },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#409eff' }
    }]
  })

  simulationChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      axisLabel: { rotate: 20, color: '#606266' },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      data: overview.simulationStats.map(item => item.name)
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      name: '会话数',
      type: 'bar',
      data: overview.simulationStats.map(item => item.value),
      barMaxWidth: 40,
      itemStyle: { color: '#409eff', borderRadius: [6, 6, 0, 0] }
    }]
  })
}

const handleResize = () => {
  teacherTypeChart?.resize()
  departmentChart?.resize()
  levelChart?.resize()
  assessmentTrendChart?.resize()
  registerTrendChart?.resize()
  simulationChart?.resize()
}

onMounted(async () => {
  await loadDepartmentOptions()
  await nextTick()
  initCharts()
  await loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  teacherTypeChart?.dispose()
  departmentChart?.dispose()
  levelChart?.dispose()
  assessmentTrendChart?.dispose()
  registerTrendChart?.dispose()
  simulationChart?.dispose()
})
</script>

<style scoped lang="scss">
.statistics-page {
  min-height: calc(100vh - 64px);
}

.filter-card {
  border-radius: 8px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  margin-bottom: 24px;

  &:deep(.el-card__header) {
    padding: 16px 24px;
    border-bottom: 1px solid #eef2f7;
  }

  &:deep(.el-card__body) {
    padding: 20px 24px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 18px;
      font-weight: 650;
      color: #1f2f3e;
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

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.filter-form {
  margin-bottom: -18px;
}

.summary-row,
.chart-row {
  margin-top: 20px;
}

.summary-card {
  border-radius: 8px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.08);
  }

  &:deep(.el-card__body) {
    padding: 20px 18px;
  }

  .summary-title {
    font-size: 14px;
    color: #7a8b9b;
    margin-bottom: 8px;
  }

  .summary-value {
    font-size: 32px;
    font-weight: 700;
    color: #1f2f3e;
  }

  .summary-icon {
    position: absolute;
    right: 16px;
    bottom: 16px;
    color: rgba(64, 158, 255, 0.15);
    font-size: 40px;
  }
}

.chart-card {
  border-radius: 8px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  height: 100%;

  &:deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #eef2f7;
  }

  &:deep(.el-card__body) {
    padding: 16px;
  }

  .chart-title {
    font-size: 16px;
    font-weight: 650;
    color: #1f2f3e;
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

.chart-box {
  height: 320px;
  background: #fbfdff;
  border-radius: 8px;
  padding: 8px;
}
</style>