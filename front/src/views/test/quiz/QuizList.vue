<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Delete } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { recordsService, abandonService } from '@/api/test.js'
import { useRouter } from 'vue-router'
const router = useRouter()

//分页条数据模型
const pageNum = ref(1)//当前页
const total = ref(10)//总条数
const pageSize = ref(3)//每页条数
//当每页条数发生了变化，调用此函数
const onSizeChange = (size) => {
    pageSize.value = size
    loadRecords()
}
//当前页码发生变化，调用此函数
const onCurrentChange = (num) => {
    pageNum.value = num
    loadRecords()
}

const loading = ref(false)
const qnList = ref([])

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const d = new Date(timeStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDuration = (seconds) => {
  if (seconds === null || seconds === undefined) return '-'
  const s = Number(seconds)
  const mm = Math.floor(s / 60)
  const ss = s % 60
  return `${String(mm).padStart(2, '0')}分钟${String(ss).padStart(2, '0')}秒`
}

const viewTestResult = (row) => {
  ElMessage.info('查看测试详情')
  router.push({ path: '/test/result', query: { id: row.id } })
}

const deleteRecord = (row) => {
  ElMessageBox.confirm('你确认要删除该测试记录吗?', '温馨提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(async () => {
      const result = await abandonService(row.id)
      ElMessage.success('删除成功！')
      loadRecords()
    }).catch(() => {
      ElMessage.info('取消删除')
    })
}
const loadRecords = async () => {
  try {
    loading.value = true
    let params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    const result = await recordsService(params)
    //渲染视图
    total.value = result.data.total;
    qnList.value = result.data.items;
  }catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '加载测试记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
</script>

<template>
  <div class="records-page">
    <el-card class="records-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title">全部测试记录</div>
          <el-button type="primary" plain @click="router.push('/test/home')">返回主页</el-button>
        </div>
      </template>
      <!-- 搜索表单 -->
      <!-- <el-form inline>
        <el-form-item label="待办分类：">
            <el-select placeholder="请选择" v-model="categoryId" style="width: 240px">
                <el-option 
                    v-for="c in category" 
                    :key="c.id" 
                    :label="c.categoryName" 
                    :value="c.id">
                </el-option>
            </el-select>
            </el-form-item>
            <el-form-item label="完成状态：">
                <el-select placeholder="请选择" v-model="state" style="width: 240px">
                    <el-option label="已完成" value="已完成"></el-option>
                    <el-option label="未完成" value="未完成"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="todoList">搜索</el-button>
                <el-button @click="categoryId = ''; state = ''">重置</el-button>
            </el-form-item>
        </el-form> -->
      <el-table v-loading="loading" :data="qnList" stripe class="records-table" @row-click="viewTestResult">
        <el-table-column label="答卷标题" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <div class="main">{{ row.title || '-' }}</div>
              <div class="sub">情景：{{ row.sceneTitle || row.sceneId || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="140" align="center" sortable>
          <template #default="{ row }">
            <span class="score">{{ row.userTotalScore }} / {{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用时" width="120" align="center" sortable>
          <template #default="{ row }">
            {{ formatDuration(row.timeSpent) }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="200" align="center" sortable>
          <template #default="{ row }">
            {{ formatTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button :icon="View" circle plain type="primary" @click.stop="viewTestResult(row)"></el-button>
            <el-button :icon="Delete" circle plain type="danger" @click.stop="deleteRecord(row)"></el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && qnList.length === 0" class="empty">
        <el-empty description="暂无已完成的测试记录" />
      </div>
      <!-- 分页条 -->
      <div class="pager">
        <el-pagination :current-page="pageNum" :page-size="pageSize" :page-sizes="[3, 5, 10, 15]"
            layout="jumper, total, sizes, prev, pager, next" background :total="total" @size-change="onSizeChange"
            @current-change="onCurrentChange" style="margin-top: 20px; justify-content: flex-end" 
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.records-page {
  min-height: calc(100vh - 64px);
}

.records-card {
  border-radius: 8px;

  &:deep(.el-card__header) {
    padding: 12px 18px;
  }
  &:deep(.el-card__body) {
    padding: 12px 18px 18px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .main {
    font-weight: 600;
    color: #303133;
    line-height: 1.3;
  }
  .sub {
    font-size: 12px;
    color: #909399;
  }
}

.score {
  font-weight: 600;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.empty {
  padding: 20px 0 10px;
}
</style>