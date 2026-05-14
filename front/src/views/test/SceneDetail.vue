<script setup>
import { ArrowLeft, VideoPlay, Document, Aim, CircleCheck, Close, InfoFilled, Clock, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'

import { ref, onMounted, computed } from 'vue'
const sceneInfo = ref({})
const sceneCategories = ref([])
const relatedScenes  = ref([])
const loading = ref(false)

import { useRouter } from 'vue-router'
import { sceneListService, sceneInfoService, sceneCategoriesService, createQuizService} from '@/api/test.js'
const router = useRouter()
const id = router.currentRoute.value.query.id

const loadCategories = async () => {
  try {
    const result = await sceneCategoriesService()
    if (result.code === 0) {
      sceneCategories.value = result.data
    }
  } catch (error) {
    console.error('加载分类数据失败:', error)
  }
}

const loadRelatedScenes = async () => {
  try {
    const result = await sceneListService()
    if (result.code === 0) {
      relatedScenes.value = result.data.filter(scene => scene.id !== id)
    }
  } catch (error) {
    console.error('加载相关情景数据失败:', error)
  }
}

const init = async () => {
    try {
      loading.value = true
      const result = await sceneInfoService(id)
      sceneInfo.value = result.data
      loadCategories()
      loadRelatedScenes()
    } catch (error) {
      console.error('获取情景信息失败:', error)
      ElMessage({
        type: 'error',
        message: '加载情景信息失败，请稍后重试!',
        duration: 3000
      })
    } finally {
      loading.value = false
    }
}
onMounted(() => {
    init()
})

const findCategoryName = (categoryId) => {
  if (!categoryId || !sceneCategories.value.length) return ''
  
  const category = sceneCategories.value.find(cat => cat.id === categoryId)
  return category ? category.name : ''
}

const getDifficultyType = (difficulty) => {
    const types = ['', 'success', 'warning', 'danger']
    return types[difficulty] || 'info'
}
const getDifficultyText = (difficulty) => {
    const texts = ['初级', '中级', '高级', '专家级']
    return texts[difficulty - 1] || '未知'
}
const parsePoints = (text) => {
    if (!text) return []
    return text.split(/[,，、]/).map(item => item.trim()).filter(item => item)
}
  
const goBack = () => {
    router.push('/test/home')
}
  
const viewRelatedScene = (sceneId) => {
    router.push({ path: '/test/scene', query: { id: sceneId } })
}

const startTest = () => {
    ElMessageBox.confirm(
      '开始测试后，请注意：\n\n1. 测试中途退出将无法保存进度\n2. 建议在无干扰环境下完成\n3. 预计用时：15-20分钟\n\n是否确认开始测试？',
      '开始情景测试',
      {
        confirmButtonText: '开始测试',
        cancelButtonText: '再考虑下',
        type: 'warning',
        customClass: 'start-test-dialog'
      }
    ).then(async () => {
      const loading = ElLoading.service({
        lock: true,
        text: '正在自动生成测试题目，请耐心等待...',
        background: 'rgba(0, 0, 0, 0.45)'
      })

      try {
        // 创建答卷（后端返回：{qnId,title,totalScore}）
        const result = await createQuizService(id)
        const qnId = result.data.qnId
        const title = result.data.title
        const totalScore = result.data.totalScore

        ElMessage.success('生成题目成功，即将进入答题...')
        router.push({ path: '/test/quiz', query: { id: qnId, title: title, totalScore: totalScore} })
      } catch (e) {
        console.error(e)
        ElMessage.error(e?.message || '创建测试失败，请稍后重试')
      } finally {
        loading.close()
      }
    }).catch(() => {
      ElMessage.info('已取消测试')
    })
}
</script>

<template>
    <div class="scene-detail-container">
      <el-card class="scene-card" shadow="never">
        <template #header>
          <div class="scene-card-header">
            <div class="scene-title-section">
              <h1 class="scene-title">{{ sceneInfo.title }}</h1>
              <div class="scene-meta">
                <el-tag :type="getDifficultyType(sceneInfo.difficulty)">{{ getDifficultyText(sceneInfo.difficulty) }}</el-tag>
                <el-tag type="info" size="small">{{ findCategoryName(sceneInfo.categoryId) }}</el-tag>
                <span class="scene-time" v-if="sceneInfo.estimatedTime">
                  <el-icon><Clock /></el-icon> 预计用时：{{ sceneInfo.estimatedTime }}分钟
                </span>
              </div>
            </div>
            <div class="scene-actions">
              <el-button type="primary" @click="startTest" :loading="loading"><el-icon><VideoPlay /></el-icon>开始测试</el-button>
              <el-button plain @click="goBack"><el-icon><ArrowLeft /></el-icon>返回主页</el-button>
            </div>
          </div>
        </template>
  
        <div class="scene-content" v-loading="loading">
          <section class="scene-section">
            <div class="section-header">
              <el-icon class="section-icon"><Document /></el-icon><h2>场景描述</h2>
            </div>
            <div class="section-content">
              <div class="scene-description">
                <p>{{ sceneInfo.description }}</p>
              </div>
            </div>
          </section>
          <section class="scene-section">
            <div class="section-header">
              <el-icon class="section-icon"><Aim /></el-icon><h2>关注要点</h2>
            </div>
            <div class="section-content">
              <div class="focus-points">
                <el-tag v-for="(point, index) in parsePoints(sceneInfo.focus)" :key="index" class="focus-tag" effect="plain">
                  {{ point }}
                </el-tag>
              </div>
            </div>
          </section>
          <section class="scene-section">
            <div class="section-header">
              <el-icon class="section-icon"><InfoFilled /></el-icon><h2>关键分析</h2>
            </div>
            <div class="section-content">
              <div class="analysis-content">
                <p>{{ sceneInfo.analysis }}</p>
              </div>
            </div>
          </section>
          <section class="scene-section">
            <div class="section-header">
              <el-icon class="section-icon"><CircleCheck /></el-icon><h2>正确做法</h2>
              <el-tag type="success" size="small" effect="dark" class="correct-tag">推荐</el-tag>
            </div>
            <div class="section-content">
              <div class="approach-content correct-approach">
                <div class="approach-icon">
                  <el-icon><Check /></el-icon>
                </div>
                <div class="approach-text">
                  <p>{{ sceneInfo.correctApproach }}</p>
                </div>
              </div>
            </div>
          </section>
          <section class="scene-section">
            <div class="section-header">
              <el-icon class="section-icon"><Close /></el-icon><h2>常见误区</h2>
              <el-tag type="danger" size="small" effect="dark" class="error-tag">避免</el-tag>
            </div>
            <div class="section-content">
              <div class="approach-content incorrect-approach">
                <div class="approach-icon">
                  <el-icon><Close /></el-icon>
                </div>
                <div class="approach-text">
                  <p>{{ sceneInfo.incorrectApproach }}</p>
                </div>
              </div>
            </div>
          </section>
        </div>
  
        <template #footer>
          <div class="scene-footer">
            <div class="footer-actions">
              <el-button type="primary" size="large" @click="startTest" :loading="loading">
                <el-icon><VideoPlay /></el-icon>立即开始测试 </el-button>
              <el-button plain size="large" @click="goBack">
                <el-icon><ArrowLeft /></el-icon>返回情景主页</el-button>
            </div>
            <div class="footer-tips">
              <el-alert title="温馨提示" type="info" :closable="false" show-icon>
                <ul class="rules-list">
                  <li>测试过程中请勿刷新页面或关闭浏览器，否则需要重新开始</li>
                  <li>中途退出测试将无法保存进度</li>
                  <li>测试完成后会获得详细的分析报告</li>
                  <li>建议在无干扰环境下完成测试</li>
                </ul>
              </el-alert>
            </div>
          </div>
        </template>
      </el-card>
  
      <div class="related-scenes" v-if="relatedScenes.length > 0">
        <h3 class="related-title">相关情景推荐</h3>
        <el-row :gutter="20">
          <el-col v-for="scene in relatedScenes.slice(0, 3)" :key="scene.id" :xs="24" :sm="12" :md="8" :lg="8">
            <el-card class="related-card" shadow="hover" @click="viewRelatedScene(scene.id)">
              <div class="related-card-content">
                <h4>{{ scene.title }}</h4>
                <div class="related-meta">
                  <el-tag size="small" :type="getDifficultyType(scene.difficulty)">{{ getDifficultyText(scene.difficulty) }}</el-tag>
                  <span class="related-time">{{ scene.estimatedTime }}分钟</span>
                </div>
                <p class="related-desc">{{ scene.description.substring(0, 60) }}...</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
</template>

<style lang="scss" scoped>
.scene-detail-container {
    min-height: calc(100vh - 64px);
}
  
.scene-card {
    border-radius: 5px;
    margin-bottom: 15px;
    transition: all 0.3s ease;
    
    &:deep(.el-card__header) {
      padding: 10px 20px;
      border-bottom: 1px solid #ebeef5;
    }
    &:deep(.el-card__body) {
      padding: 20px;
    }
    &:deep(.el-card__footer) {
      padding: 10px 20px;
      border-top: 1px solid #ebeef5;
    }
}
  
.scene-card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 20px;
    
    .scene-title-section {
      flex: 1;
      min-width: 300px;
      
      .scene-title {
        margin: 0;
        font-size: 24px;
        color: #303133;
        font-weight: 600;
        line-height: 1.3;
      }
      .scene-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-top: 4px;
        .el-tag {
          font-weight: 500;
        }
        .scene-time {
          display: flex;
          align-items: center;
          gap: 4px;
          color: #909399;
          font-size: 14px;
          .el-icon {
            font-size: 16px;
          }
        }
      }
    }
    
    .scene-actions {
      display: flex;
      gap: 12px;
      
      .el-button {
        padding: 10px 20px;
        font-weight: 500;
        .el-icon {
          margin-right: 6px;
        }
      }
    }
}
  
.scene-content {
    padding: 0 20px;
}
  
.scene-section {
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
      margin-bottom: 0;
    }
    
    .section-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
      .section-icon {
        font-size: 20px;
        color: #409eff;
      }
      h2 {
        margin: 0;
        font-size: 20px;
        color: #303133;
        font-weight: 600;
      }
      .correct-tag, .error-tag {
        margin-left: 4px;
      }
    }
    
    .section-content {
      padding-left: 24px;
    }
}
  
.scene-description {
    background: #f8f9fa;
    padding: 20px;
    border-radius: 8px;
    border-left: 4px solid #409eff;
    p {
      margin: 0;
      font-size: 16px;
      line-height: 1.8;
      color: #606266;
    }
}
  
.focus-points {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    
    .focus-tag {
      padding: 8px 16px;
      font-size: 16px;
      border-radius: 10px;
      background-color: #ecf5ff;
      color: #409eff;
      border: none;
    }
}
  
.analysis-content {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    p {
      margin: 0;
      font-size: 16px;
      line-height: 1.8;
      color: #606266;
    }
}
  
.approach-content {
    display: flex;
    gap: 16px;
    padding: 20px;
    border-radius: 8px;
    
    .approach-icon {
      flex-shrink: 0;
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      .el-icon {
        font-size: 20px;
        color: white;
      }
    }
    
    .approach-text {
      flex: 1;
      p {
        margin: 0;
        font-size: 16px;
        line-height: 1.8;
      }
    }
}
  
.correct-approach {
    background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
    border-left: 4px solid #67c23a;
    .approach-icon {
      background-color: #67c23a;
    }
}
  
.incorrect-approach {
    background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
    border-left: 4px solid #f56c6c;  
    .approach-icon {
      background-color: #f56c6c;
    }
}
  
.scene-footer {
    .footer-actions {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-bottom: 10px;
      
      .el-button {
        min-width: 160px;
        padding: 12px 32px;
        font-size: 16px;
        .el-icon {
          margin-right: 8px;
        }
      }
    }
    
    .footer-tips {
      max-width: 800px;
      margin: 0 auto;
      .rules-list {
        list-style: none;
        padding: 0;
        margin: 0;
        li {
          padding: 3px 0;
          padding-left: 24px;
          position: relative;
          &:before {
            content: '•';
            position: absolute;
            left: 8px;
            color: #8d8d8d;
            font-size: 20px;
          }
        }
      }
    }
}

.related-scenes {
    margin-top: 10px;
    
    .related-title {
      margin: 0 0 10px;
      font-size: 20px;
      color: #303133;
      font-weight: 600;
    }
    .related-card {
      cursor: pointer;
      transition: transform 0.3s ease;
      height: 100%;
      &:hover {
        transform: translateY(-4px);
      }
      
      .related-card-content {
        h4 {
          margin: 0 0 10px;
          font-size: 16px;
          color: #303133;
          font-weight: 500;
        }
        .related-meta {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 10px;
          .related-time {
            color: #909399;
            font-size: 14px;
          }
        }
        .related-desc {
          margin: 0;
          font-size: 14px;
          color: #606266;
          line-height: 1.6;
        }
      }
    }
}

.start-test-dialog {
    .el-message-box__content {
      padding: 20px;
    }
    
    .confirm-content {
      h3 {
        margin: 0 0 15px;
        color: #303133;
        font-size: 18px;
      }
      p {
        margin: 10px 0;
        color: #606266;
      }
      ul {
        margin: 10px 0 20px;
        padding-left: 20px;
        li {
          margin-bottom: 5px;
          color: #606266;
        }
      }
    }
}

@media (max-width: 768px) {
    .scene-detail-container {
      padding: 15px;
    }
    
    .scene-card-header {
      flex-direction: column;
      
      .scene-actions {
        width: 100%;
        justify-content: flex-start;
      }
    }
    
    .scene-title {
      font-size: 24px !important;
    }
    
    .scene-content {
      padding: 0 16px;
    }
    
    .section-content {
      padding-left: 0 !important;
    }
    
    .footer-actions {
      flex-direction: column;
      
      .el-button {
        width: 100%;
      }
    }
    
    .approach-content {
      flex-direction: column;
      gap: 12px;
      
      .approach-icon {
        width: 36px;
        height: 36px;
      }
    }
}
</style>

<style lang="scss">
.start-test-dialog {
    .el-message-box__content {
      padding: 20px;
    }
    
    .el-message-box__message {
      line-height: 1.6;
      white-space: pre-line;
    }
}
</style>