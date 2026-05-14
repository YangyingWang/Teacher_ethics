<template>
  <div class="admin-home-page">
    <!-- 页面头部（卡片化） -->
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">管理首页</h2>
        <p class="page-subtitle">查看教师总体情况、模块参与概况与近期动态</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Refresh" @click="loadData" :loading="loading">刷新数据</el-button>
      </div>
    </div>

    <!-- 数据总览卡片 -->
    <el-row :gutter="20" class="summary-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" v-for="item in summaryCards" :key="item.key">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-card__label">{{ item.label }}</div>
          <div class="summary-card__value">{{ item.value }}</div>
          <div class="summary-card__icon">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行1：教师类型分布 + 院系分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">教师类型分布</div>
          </template>
          <div ref="typeChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">院系分布</div>
          </template>
          <div ref="departmentChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行2：模块参与概况 + 近期动态 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">模块参与概况</div>
          </template>
          <div ref="moduleChartRef" class="chart-box chart-box--large"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">近期动态</div>
          </template>
          <div class="activity-list" v-loading="loading">
            <div v-if="!overview.recentActivities.length" class="empty-text">暂无动态数据</div>
            <div v-for="(item, index) in overview.recentActivities" :key="index" class="activity-item">
              <div class="activity-item__title">{{ item.title }}</div>
              <div class="activity-item__content">{{ item.content }}</div>
              <div class="activity-item__time">{{ item.time }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, User, Reading, Headset, Edit, DataBoard, Star } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAdminOverviewService } from '@/api/admin'

const loading = ref(false)
const typeChartRef = ref(null)
const departmentChartRef = ref(null)
const moduleChartRef = ref(null)

let typeChart = null
let departmentChart = null
let moduleChart = null

const overview = ref({
  summary: {
    teacherTotal: 0,
    courseLearnerCount: 0,
    simulationParticipantCount: 0,
    evaluationCount: 0,
    assessmentCount: 0,
    avgAssessmentScore: 0
  },
  teacherTypeStats: [],
  departmentStats: [],
  moduleStats: [],
  recentActivities: []
})

const iconMap = {
  teacherTotal: User,
  courseLearnerCount: Reading,
  simulationParticipantCount: Headset,
  evaluationCount: Edit,
  assessmentCount: DataBoard,
  avgAssessmentScore: Star
}

const summaryCards = computed(() => {
  const s = overview.value.summary || {}
  return [
    { key: 'teacherTotal', label: '教师总数', value: s.teacherTotal ?? 0, icon: iconMap.teacherTotal },
    { key: 'courseLearnerCount', label: '课程学习人数', value: s.courseLearnerCount ?? 0, icon: iconMap.courseLearnerCount },
    { key: 'simulationParticipantCount', label: '治理研修人数', value: s.simulationParticipantCount ?? 0, icon: iconMap.simulationParticipantCount },
    { key: 'evaluationCount', label: '决策评估次数', value: s.evaluationCount ?? 0, icon: iconMap.evaluationCount },
    { key: 'assessmentCount', label: '多维评估次数', value: s.assessmentCount ?? 0, icon: iconMap.assessmentCount },
    { key: 'avgAssessmentScore', label: '平均综合得分', value: s.avgAssessmentScore ?? 0, icon: iconMap.avgAssessmentScore }
  ]
})

const initCharts = () => {
  if (typeChartRef.value && !typeChart) {
    typeChart = echarts.init(typeChartRef.value)
  }
  if (departmentChartRef.value && !departmentChart) {
    departmentChart = echarts.init(departmentChartRef.value)
  }
  if (moduleChartRef.value && !moduleChart) {
    moduleChart = echarts.init(moduleChartRef.value)
  }
}

const renderCharts = () => {
  initCharts()

  const typeStats = overview.value.teacherTypeStats || []
  const departmentStats = overview.value.departmentStats || []
  const moduleStats = overview.value.moduleStats || []

  typeChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#606266' } },
    series: [{
      name: '教师类型',
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '45%'],
      data: typeStats.map(item => ({ name: item.name, value: item.value })),
      label: { formatter: '{b}\n{c}人', color: '#606266' },
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }
    }]
  })

  departmentChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      axisLabel: { rotate: 25, color: '#606266' },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      data: departmentStats.map(item => item.name)
    },
    yAxis: {
      type: 'value',
      name: '人数',
      nameTextStyle: { color: '#909399' },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      data: departmentStats.map(item => item.value),
      barMaxWidth: 42,
      itemStyle: { color: '#409eff', borderRadius: [6, 6, 0, 0] }
    }]
  })

  moduleChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 40, top: 30, bottom: 30 },
    xAxis: {
      type: 'value',
      name: '数量',
      nameTextStyle: { color: '#909399' },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: moduleStats.map(item => item.name),
      axisLabel: { color: '#606266' },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar',
      data: moduleStats.map(item => item.value),
      label: { show: true, position: 'right', color: '#303133' },
      barMaxWidth: 28,
      itemStyle: { color: '#409eff', borderRadius: [0, 6, 6, 0] }
    }]
  })
}

const resizeCharts = () => {
  typeChart?.resize()
  departmentChart?.resize()
  moduleChart?.resize()
}

const loadData = async () => {
  loading.value = true
  try {
    const result = await getAdminOverviewService()
    overview.value = result.data || overview.value
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error?.message || '管理首页数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  typeChart?.dispose()
  departmentChart?.dispose()
  moduleChart?.dispose()
})
</script>

<style scoped lang="scss">
.admin-home-page {
  min-height: calc(100vh - 64px);
}

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

  .header-info {
    .page-title {
      margin: 0 0 6px;
      font-size: 22px;
      color: #1f2f3e;
      font-weight: 650;
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: '';
        width: 5px;
        height: 22px;
        background: #409eff;
        border-radius: 3px;
        display: inline-block;
      }
    }

    .page-subtitle {
      margin: 0;
      color: #6b7a8a;
      font-size: 14px;
    }
  }

  .header-actions {
    .el-button {
      border-radius: 6px;
      font-weight: 500;
      padding: 10px 18px;
    }
  }
}

.summary-row,
.chart-row {
  margin-bottom: 20px;
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

  .summary-card__label {
    color: #7a8b9b;
    font-size: 14px;
    margin-bottom: 8px;
  }

  .summary-card__value {
    color: #1f2f3e;
    font-size: 32px;
    font-weight: 700;
    line-height: 1.2;
  }

  .summary-card__icon {
    position: absolute;
    right: 16px;
    bottom: 16px;
    color: rgba(64, 158, 255, 0.15);
    font-size: 48px;
  }
}

.panel-card {
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

  .panel-title {
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
  height: 300px;
  background: #fbfdff;
  border-radius: 8px;
  padding: 8px;

  &--large {
    height: 340px;
  }
}

.activity-list {
  min-height: 340px;

  .activity-item {
    padding: 14px 0;
    border-bottom: 1px solid #f0f5fa;

    &:last-child {
      border-bottom: none;
    }

    &__title {
      font-size: 15px;
      font-weight: 600;
      color: #1f2f3e;
      margin-bottom: 6px;
    }

    &__content {
      font-size: 13px;
      color: #566b7c;
      line-height: 1.6;
      margin-bottom: 4px;
    }

    &__time {
      font-size: 12px;
      color: #909399;
    }
  }

  .empty-text {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 340px;
    color: #909399;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .admin-home-page {
    padding: 16px;
  }
}
</style>