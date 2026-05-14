<script setup>
import { Camera, Edit, Setting, ArrowRight, More, Reading, School, User, Setting as SettingIcon, Trophy, Clock, Bell, Message, Calendar, Document,
  Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

import { ref, onMounted, computed, onUnmounted} from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()

import useUserInfoStore from '@/stores/userInfo.js'
const userInfoStore = useUserInfoStore()
const userInfo = ref({ ...userInfoStore.info })
const rules = {
  identity_card: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    {
      pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))((0[1-9])|([12][0-9])|(3[01]))\d{3}[\dXx]$/,
      message: '请输入有效的身份证号',
      trigger: 'blur'
    }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}
const userTypeMap = {
  '0': '科研型教师',
  '1': '教学型教师',
  '2': '混合型教师'
}
const avatarUrl = ref('/src/assets/avatar.png')

const performanceChart = ref(null)
let chartInstance = null

// 师德维度数据
const ethicsDimensions = ref([
  { name: '敬业爱岗', value: 92, color: '#409EFF' },
  { name: '德高身正', value: 88, color: '#67C23A' },
  { name: '爱护学生', value: 85, color: '#E6A23C' },
  { name: '求真创新', value: 90, color: '#F56C6C' },
  { name: '谦逊自律', value: 87, color: '#909399' }
])
// 学习进度
const learningProgress = ref([
  { id: 1, name: '科研诚信专题', progress: 85 },
  { id: 2, name: '课程思政设计', progress: 70 },
  { id: 3, name: '师生关系处理', progress: 95 }
])
// 初始化图表
const initCharts = () => {
  if (!performanceChart.value) return
  
  chartInstance = echarts.init(performanceChart.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}分 ({d}%)'
    },
    legend: { show: false },
    series: [{
      name: '师德维度',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {show: false},
      emphasis: {
        label: {
          show: true,
          fontSize: '16',
          fontWeight: 'bold'
        }
      },
      data: ethicsDimensions.value.map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: { color: item.color }
      }))
    }]
  }
  
  chartInstance.setOption(option)
  
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
}
// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#67C23A'
  if (percentage >= 70) return '#E6A23C'
  return '#F56C6C'
}
// 生命周期钩子
onMounted(() => {
  initCharts()
})
onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', () => {})
})

const drawer = ref(false)
const loading = ref(false)
import { userInfoUpdateService } from '@/api/user.js'
const updateUserInfo = async () => {
  try {
    await ElMessageBox.confirm('确定要提交修改吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    loading.value = true
    const result = await userInfoUpdateService(userInfo.value)
    ElMessage.success(result.msg || '修改成功!')
    // 修改 Pinia 中的个人信息
    userInfoStore.setInfo(userInfo.value)
    drawer.value = false
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('修改失败，请稍后再试!')
      console.error('修改失败:', error)
    }
  } finally {
    loading.value = false
  }
}

// 头像上传
const uploadAvatar = () => {
  ElMessage.info('头像上传功能开发中...')
}

// 查看详细分析
const viewDetailAnalysis = () => {
  router.push('/assessment')
}

// 跳转到页面
const goToPage = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="personal-center">
    <div class="user-profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-avatar :size="100" :src="avatarUrl" />
            <div class="avatar-upload" @click="uploadAvatar">
              <el-icon><Camera /></el-icon>
            </div>
          </div>
          <div class="user-basic">
            <h2>{{ userInfoStore.info.realName }}</h2>
            <p class="user-title">{{ userInfoStore.info.title }}</p>
            <div class="user-tags">
              <el-tag type="primary" size="small">{{ userTypeMap[userInfoStore.info.type] }}</el-tag>
              <el-tag type="success" size="small">{{ userInfoStore.info.expertise }}</el-tag>
              <el-tag type="info" size="small">{{ userInfoStore.info.hireDate }}入职</el-tag>
            </div>
          </div>
        </div>
        <div class="action-buttons">
          <el-button type="primary" @click="drawer = true">
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
          <el-button>
            <el-icon><Setting /></el-icon>
            设置
          </el-button>
        </div>
      </div>

      <div class="profile-stats">
        <div class="stat-item">
          <div class="stat-value">98</div>
          <div class="stat-label">师德指数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">12</div>
          <div class="stat-label">课程数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">48</div>
          <div class="stat-label">学习时长</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">前15%</div>
          <div class="stat-label">平台排名</div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>个人信息</h3>
              <el-button type="text" @click="drawer = true">
                <el-icon><Edit /></el-icon>
              </el-button>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="工号" label-align="right">
              <span class="info-value">{{ userInfoStore.info.username }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="性别" label-align="right">
              <span class="info-value">{{ userInfoStore.info.sex === '0' ? '男' : '女' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="身份证号" label-align="right">
              <span class="info-value">{{ userInfoStore.info.identityCard }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="入职日期" label-align="right">
              <span class="info-value">{{ userInfoStore.info.hireDate }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="联系电话" label-align="right">
              <span class="info-value">{{ userInfoStore.info.phone }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="邮箱" label-align="right">
              <span class="info-value">{{ userInfoStore.info.email }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <div class="bio-section">
            <h4>个人简介</h4>
            <p class="bio-content">{{ userInfoStore.info.bio }}</p>
          </div>
        </el-card>
      </div>

      <div class="right-panel">
        <el-card class="progress-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>学习进度</h3>
            </div>
          </template>
          <div class="progress-content">
            <div v-for="course in learningProgress" :key="course.id" class="course-progress">
              <div class="course-header">
                <span class="course-name">{{ course.name }}</span>
              </div>
              <el-progress :percentage="course.progress" :stroke-width="8" :color="getProgressColor(course.progress)"/>
            </div>
          </div>
        </el-card>

        <el-card class="analysis-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h3>师德维度分析</h3>
              <el-button type="text" @click="viewDetailAnalysis">
                查看详情 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          
          <div class="analysis-content">
            <div ref="performanceChart" class="chart-container"></div>
            <div class="analysis-legend">
              <div v-for="item in ethicsDimensions" :key="item.name" class="legend-item">
                <div class="legend-color" :style="{backgroundColor: item.color}"></div>
                <span class="legend-text">{{ item.name }}</span>
                <span class="legend-value">{{ item.value }}分</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-drawer v-model="drawer" title="编辑个人资料" size="40%">
      <template #default>
        <el-form :model="userInfo" :rules="rules" label-width="100px" class="edit-form">
          <el-form-item label="工号">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="userInfo.realName" disabled />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="userInfo.sex">
              <el-radio value="0">男</el-radio>
              <el-radio value="1">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="身份证号" prop="identityCard">
            <el-input v-model="userInfo.identityCard" />
          </el-form-item>
          <el-form-item label="入职日期">
            <el-date-picker v-model="userInfo.hireDate" type="date" placeholder="选择入职日期" 
              value-format="YYYY-MM-DD" style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userInfo.email" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="userInfo.phone" />
          </el-form-item>
          <el-form-item label="职称">
            <el-input v-model="userInfo.title" />
          </el-form-item>
          <el-form-item label="教师类型">
            <el-radio-group v-model="userInfo.type">
              <el-radio value="0">科研型</el-radio>
              <el-radio value="1">教学型</el-radio>
              <el-radio value="2">混合型</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="专业方向">
            <el-input v-model="userInfo.expertise" />
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input  v-model="userInfo.bio"  type="textarea" :rows="6" maxlength="600" show-word-limit/>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawer = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="updateUserInfo()">保存修改</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
.personal-center {
  min-height: calc(100vh - 64px);

  .el-card {
    border-radius: 5px;
    margin-bottom: 15px;
    border: 1px solid #ebeef5;
    
    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;
    }
    
    &:deep(.el-card__body) {
      padding: 20px;
    }
  }
}

.user-profile-card {
  background: linear-gradient(135deg, #9cb2fa 0%, #80a2ff 100%);
  border-radius: 8px;
  padding: 20px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.2);

  .profile-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;

    .avatar-section {
      display: flex;
      align-items: center;
      gap: 24px;
      .avatar-wrapper {
        position: relative;
        .avatar-upload {
          position: absolute;
          bottom: 0;
          right: 0;
          width: 32px;
          height: 32px;
          background: white;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #4f8cff;
          cursor: pointer;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
          transition: all 0.3s;
          &:hover {
            transform: scale(1.1);
          }
        }
      }

      .user-basic {
        h2 {
          margin: 0 0 4px 0;
          font-size: 28px;
          font-weight: 600;
        }
        .user-title {
          margin: 0 0 12px 0;
          font-size: 16px;
          opacity: 0.9;
        }
        .user-tags {
          display: flex;
          gap: 8px;
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 12px;

      .el-button {
        background: rgba(255, 255, 255, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;
        border-radius: 10px;
        padding: 10px 20px;
        &:hover {
          background: rgba(255, 255, 255, 0.3);
        }
      }
    }
  }

  .profile-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    padding-top: 10px;
    border-top: 1px solid rgba(255, 255, 255, 0.2);
    .stat-item {
      text-align: center;
      .stat-value {
        font-size: 28px;
        font-weight: 700;
        margin-bottom: 2px;
      }
      .stat-label {
        font-size: 14px;
        opacity: 0.8;
      }
    }
  }
}

.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  .left-panel, .right-panel{
    display: flex;
    flex-direction: column;
    gap: 0px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
  .el-button{
    font-size: 16px;
  }
}

.info-card{
  &:deep(.el-descriptions) {
    margin-bottom: 20px;
  }
  &:deep(.el-descriptions__label) {
    width: 100px;
    color: #666;
    font-weight: 500;
  }
  &:deep(.el-descriptions__content) {
    color: #333;
  }
  .info-value {
    font-weight: 500;
  }
  .bio-section {
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;
    h4 {
      margin: 0 0 12px 0;
      font-size: 16px;
      color: #303133;
    }
    .bio-content {
      margin-bottom: 10px;
      color: #666;
      line-height: 1.6;
    }
  }
}

.analysis-content {
  display: flex;
  gap: 24px;
  align-items: center;
}

@media (max-width: 1200px) {
  .analysis-content {
    flex-direction: column;
  }
}

.chart-container {
  flex: 1;
  height: 300px;
  min-width: 300px;
}

.analysis-legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 200px;
  .legend-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    border-radius: 8px;
    background: #f8f9fa;
    .legend-color {
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }
    .legend-text {
      flex: 1;
      font-size: 14px;
      color: #333;
    }
    .legend-value {
      font-size: 14px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }
}

.course-progress {
  margin-bottom: 15px;
  :last-child {
    margin-bottom: 0;
  }
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  .course-name {
    font-size: 14px;
    color: #333;
    font-weight: 500;
  }
}

.edit-form {
  padding: 10px;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 10px;
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  .profile-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .personal-center {
    padding: 12px;
  }
  .user-profile-card {
    padding: 20px;
  }
  .profile-header {
    flex-direction: column;
    gap: 20px;
  }
  .avatar-section {
    flex-direction: column;
    text-align: center;
  }
  .user-tags {
    justify-content: center;
  }
  .profile-stats {
    grid-template-columns: 1fr;
  }
}
</style>