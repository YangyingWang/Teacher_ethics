<template>
  <div class="teacher-manage-page">
    <!-- 搜索卡片 -->
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">教师信息管理</span>
          <span class="header-desc">支持按工号、姓名、教师类型与院系进行查询</span>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="工号">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入教师工号"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input
            v-model="searchForm.realName"
            placeholder="请输入教师姓名"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="教师类型">
          <el-select v-model="searchForm.type" placeholder="请选择教师类型" clearable style="width: 180px">
            <el-option label="教学型" :value="0" />
            <el-option label="科研型" :value="1" />
            <el-option label="综合型" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="searchForm.depId" placeholder="请选择院系" clearable filterable style="width: 200px">
            <el-option
              v-for="item in departmentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <span class="table-title">教师列表</span>
          <div class="table-header-right">
            <span class="total-text">共 {{ pageState.total }} 名教师</span>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe border class="teacher-table">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="username" label="工号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column label="性别" width="80" align="center">
          <template #default="scope">
            {{ formatSex(scope.row.sex) }}
          </template>
        </el-table-column>
        <el-table-column prop="depName" label="院系" min-width="140" />
        <el-table-column label="教师类型" min-width="110">
          <template #default="scope">
            <el-tag :type="typeTagType(scope.row.type)" effect="light">
              {{ formatTeacherType(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="职称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="多维评估次数" width="120" align="center">
          <template #default="scope">
            {{ scope.row.assessmentCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="最近评估分" width="120" align="center">
          <template #default="scope">
            <span :style="{ color: getScoreColor(scope.row.latestAssessmentScore) }">
              {{ scope.row.latestAssessmentScore ?? '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="治理研修次数" width="120" align="center">
          <template #default="scope">
            {{ scope.row.simulationCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="170" />
        <el-table-column label="操作" fixed="right" width="100" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="openDetail(scope.row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageState.pageNum"
          v-model:page-size="pageState.pageSize"
          :page-sizes="[10, 15, 20, 30]"
          :background="true"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pageState.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="教师详情" size="620px" class="teacher-drawer">
      <div v-loading="detailLoading" class="detail-wrapper" v-if="detailData">
        <div class="detail-top">
          <div class="teacher-name">{{ detailData.realName }}</div>
          <div class="teacher-subtitle">工号：{{ detailData.username }}</div>
        </div>

        <el-row :gutter="16" class="summary-row">
          <el-col :span="8">
            <div class="summary-box">
              <div class="summary-label">多维评估次数</div>
              <div class="summary-value">{{ detailData.assessmentCount ?? 0 }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-box">
              <div class="summary-label">最近评估分</div>
              <div class="summary-value" :style="{ color: getScoreColor(detailData.latestAssessmentScore) }">
                {{ detailData.latestAssessmentScore ?? '--' }}
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-box">
              <div class="summary-label">治理研修次数</div>
              <div class="summary-value">{{ detailData.simulationCount ?? 0 }}</div>
            </div>
          </el-col>
        </el-row>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="姓名">{{ detailData.realName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="工号">{{ detailData.username || '--' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ formatSex(detailData.sex) }}</el-descriptions-item>
          <el-descriptions-item label="教师类型">{{ formatTeacherType(detailData.type) }}</el-descriptions-item>
          <el-descriptions-item label="院系">{{ detailData.depName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="职称">{{ detailData.title || '--' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailData.phone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailData.email || '--' }}</el-descriptions-item>
          <el-descriptions-item label="入职日期">{{ detailData.hireDate || '--' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detailData.createdAt || '--' }}</el-descriptions-item>
          <el-descriptions-item label="研究方向" :span="2">{{ detailData.expertise || '--' }}</el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">{{ detailData.bio || '--' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getTeacherPageService,
  getTeacherDetailService,
  getDepartmentOptionsService
} from '@/api/admin'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const tableData = ref([])
const detailData = ref(null)
const departmentOptions = ref([])

const searchForm = reactive({
  username: '',
  realName: '',
  type: undefined,
  depId: undefined
})

const pageState = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formatTeacherType = (type) => {
  const map = { 0: '教学型', 1: '科研型', 2: '综合型' }
  return map[type] ?? '未设置'
}

const typeTagType = (type) => {
  const map = { 0: 'success', 1: 'warning', 2: 'primary' }
  return map[type] ?? 'info'
}

const formatSex = (sex) => {
  const map = { 0: '女', 1: '男' }
  return map[sex] ?? '--'
}

const getScoreColor = (score) => {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#e6a23c'
  if (score >= 70) return '#409eff'
  return '#f56c6c'
}

const buildParams = () => ({
  pageNum: pageState.pageNum,
  pageSize: pageState.pageSize,
  username: searchForm.username || undefined,
  realName: searchForm.realName || undefined,
  type: searchForm.type,
  depId: searchForm.depId
})

const loadTeacherPage = async () => {
  loading.value = true
  try {
    const result = await getTeacherPageService(buildParams())
    tableData.value = result.data?.items || []
    pageState.total = result.data?.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '教师列表加载失败')
  } finally {
    loading.value = false
  }
}

const loadDepartmentOptions = async () => {
  try {
    const result = await getDepartmentOptionsService()
    departmentOptions.value = (result.data || []).map(item => ({
      label: item.name,
      value: item.value
    }))
  } catch (error) {
    console.error('加载院系列表失败', error)
  }
}

const onSearch = () => {
  pageState.pageNum = 1
  loadTeacherPage()
}

const onReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.type = undefined
  searchForm.depId = undefined
  pageState.pageNum = 1
  loadTeacherPage()
}

const handleSizeChange = (size) => {
  pageState.pageSize = size
  pageState.pageNum = 1
  loadTeacherPage()
}

const handleCurrentChange = (page) => {
  pageState.pageNum = page
  loadTeacherPage()
}

const openDetail = async (id) => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const result = await getTeacherDetailService(id)
    detailData.value = result.data
  } catch (error) {
    ElMessage.error(error?.message || '教师详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadDepartmentOptions()
  loadTeacherPage()
})
</script>

<style scoped lang="scss">
.teacher-manage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: calc(100vh - 64px);
}

.search-card,
.table-card {
  border-radius: 8px;
  border: 1px solid #eef2f7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);

  &:deep(.el-card__header) {
    padding: 16px 24px;
    border-bottom: 1px solid #eef2f7;
  }

  &:deep(.el-card__body) {
    padding: 20px 24px;
  }
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 16px;

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

  .header-desc {
    color: #909399;
    font-size: 14px;
  }
}

.search-form {
  margin-bottom: -18px;

  .el-button--primary {
    background: #409eff;
    border: none;
    &:hover { background: #66b1ff; }
  }
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .table-title {
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

  .total-text {
    color: #909399;
    font-size: 14px;
  }
}

.teacher-table {
  &:deep(.el-table__header th) {
    background: #fafdff;
    color: #1f2f3e;
    font-weight: 600;
  }

  &:deep(.el-table__row:hover > td) {
    background: #f5faff !important;
  }

  .el-button.is-link {
    color: #409eff;
    font-weight: 500;
    &:hover { color: #66b1ff; }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.teacher-drawer {
  &:deep(.el-drawer__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 0;
  }

  &:deep(.el-drawer__title) {
    font-size: 18px;
    font-weight: 650;
    color: #1f2f3e;
  }

  &:deep(.el-drawer__body) {
    padding: 20px 24px;
  }
}

.detail-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-top {
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;

  .teacher-name {
    font-size: 26px;
    font-weight: 700;
    color: #1f2f3e;
  }

  .teacher-subtitle {
    margin-top: 8px;
    color: #909399;
    font-size: 15px;
  }
}

.summary-row {
  .summary-box {
    padding: 18px 16px;
    border-radius: 8px;
    background: #fafdff;
    border: 1px solid #eef2f7;
    text-align: center;

    .summary-label {
      font-size: 13px;
      color: #7a8b9b;
      margin-bottom: 10px;
    }

    .summary-value {
      font-size: 28px;
      font-weight: 700;
      color: #1f2f3e;
    }
  }
}

.detail-descriptions {
  &:deep(.el-descriptions__label) {
    background: #fafdff;
    color: #566b7c;
    font-weight: 500;
  }

  &:deep(.el-descriptions__content) {
    color: #1f2f3e;
  }
}
</style>