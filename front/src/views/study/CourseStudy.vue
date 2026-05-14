<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Star, StarFilled, Clock, User, Document, Edit } from '@element-plus/icons-vue'
import {
  courseDetailService,
  courseProgressService,
  courseReviewService,
  courseToggleFavoriteService,
  courseNoteListService,
  courseNoteAddService,
  courseNoteUpdateService,
  courseNoteDeleteService
} from '@/api/study.js'

const route = useRoute()
const router = useRouter()
const videoRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('intro')
const currentCourse = ref(null)
const notes = ref([])
const noteContent = ref('')
const editingNoteId = ref(null)

let timer = null
let lastReportSec = 0
let sessionStartTime = ''

const courseId = computed(() => Number(route.query.id || 0))

function getStaticBaseUrl() {
  return (import.meta.env.VITE_FILE_BASE_URL || import.meta.env.VITE_SERVER_ORIGIN || '').replace(/\/$/, '')
}

function normalizeMediaUrl(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  const base = getStaticBaseUrl()
  if (!base) return url.startsWith('/') ? url : `/${url}`
  return url.startsWith('/') ? `${base}${url}` : `${base}/${url}`
}

function formatLocalDateTime(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const displayCourse = computed(() => {
  if (!currentCourse.value) return null
  return {
    ...currentCourse.value,
    cover: normalizeMediaUrl(currentCourse.value.cover),
    videoUrl: normalizeMediaUrl(currentCourse.value.videoUrl)
  }
})

const progressPercent = computed(() => Number(currentCourse.value?.progress || 0))
const durationMinutes = computed(() => Math.max(1, Math.round((currentCourse.value?.duration || 0) / 60)))

const getDifficultyType = (difficulty) => {
  const map = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyText = (difficulty) => {
  const map = { 1: '初级', 2: '中级', 3: '高级' }
  return map[difficulty] || '未知'
}

const formatNumber = (num) => {
  const value = Number(num) || 0
  if (value >= 10000) return `${(value / 10000).toFixed(1)}万`
  if (value >= 1000) return `${(value / 1000).toFixed(1)}千`
  return `${value}`
}

const formatSeconds = (seconds) => {
  const sec = Number(seconds) || 0
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const clearReportTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const startProgressSession = () => {
  sessionStartTime = formatLocalDateTime(new Date())
  lastReportSec = Number(currentCourse.value?.lastSec || 0)
  clearReportTimer()
  timer = window.setInterval(() => {
    reportProgress(false)
  }, 10000)
}

const fetchCourseDetail = async () => {
  if (!courseId.value) {
    ElMessage.error('课程ID无效')
    router.back()
    return
  }
  loading.value = true
  try {
    const [detailRes, noteRes] = await Promise.all([
      courseDetailService(courseId.value),
      courseNoteListService(courseId.value)
    ])
    currentCourse.value = detailRes.data
    notes.value = noteRes.data || []
    startProgressSession()
  } catch (error) {
    console.error('加载课程详情失败', error)
    ElMessage.error(error?.message || '加载课程详情失败')
  } finally {
    loading.value = false
  }
}

const reportProgress = async (force = false) => {
  if (!videoRef.value || !currentCourse.value) return

  const currentSec = Math.floor(videoRef.value.currentTime || 0)
  const delta = Math.max(0, currentSec - lastReportSec)

  if (!force && delta < 10) return
  if (force && delta <= 0) return

  if (!sessionStartTime) {
    sessionStartTime = formatLocalDateTime(new Date())
  }

  const progress = videoRef.value.duration > 0
    ? Math.min(100, Math.round((currentSec / videoRef.value.duration) * 100))
    : progressPercent.value

  try {
    const res = await courseProgressService({
      courseId: currentCourse.value.id,
      lastSec: currentSec,
      studySec: delta,
      progressPercent: progress,
      startTime: sessionStartTime,
      endTime: formatLocalDateTime(new Date())
    })

    currentCourse.value.progress = Number(res.data?.progress || progress)
    currentCourse.value.lastSec = currentSec
    currentCourse.value.studyTotal = Number(res.data?.studyTotal || currentCourse.value.studyTotal || 0)
    lastReportSec = currentSec
  } catch (error) {
    console.error('进度上报失败', error)
  }
}

const handleLoadedMetadata = () => {
  if (videoRef.value && currentCourse.value?.lastSec > 0) {
    videoRef.value.currentTime = Number(currentCourse.value.lastSec)
  }
}

const handlePause = () => {
  reportProgress(true)
}

const handleEnded = async () => {
  await reportProgress(true)
  ElMessage.success('课程学习完成')
}

const toggleFavorite = async () => {
  if (!currentCourse.value) return
  try {
    const res = await courseToggleFavoriteService(currentCourse.value.id)
    currentCourse.value.favorite = !!res.data?.favorite
    ElMessage.success(currentCourse.value.favorite ? '已加入收藏' : '已取消收藏')
  } catch (error) {
    ElMessage.error(error?.message || '收藏操作失败')
  }
}

const reviewCourse = () => {
  if (!currentCourse.value) return
  ElMessageBox.confirm('重新学习将清空当前进度，是否继续？', '重新学习', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await courseReviewService(currentCourse.value.id)
    ElMessage.success('已重置学习进度')
    await fetchCourseDetail()
    if (videoRef.value) {
      videoRef.value.currentTime = 0
    }
  }).catch(() => {})
}

const saveNote = async () => {
  const content = noteContent.value.trim()
  if (!content) {
    ElMessage.warning('笔记内容不能为空')
    return
  }
  saving.value = true
  try {
    if (editingNoteId.value) {
      await courseNoteUpdateService({ id: editingNoteId.value, courseId: courseId.value, content })
      ElMessage.success('笔记已更新')
    } else {
      await courseNoteAddService({ courseId: courseId.value, content })
      ElMessage.success('笔记已保存')
    }
    noteContent.value = ''
    editingNoteId.value = null
    const noteRes = await courseNoteListService(courseId.value)
    notes.value = noteRes.data || []
  } catch (error) {
    ElMessage.error(error?.message || '保存笔记失败')
  } finally {
    saving.value = false
  }
}

const editNote = (note) => {
  editingNoteId.value = note.id
  noteContent.value = note.content
  activeTab.value = 'notes'
}

const deleteNote = (note) => {
  ElMessageBox.confirm('确定删除这条笔记吗？', '删除笔记', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await courseNoteDeleteService(note.id)
    ElMessage.success('删除成功')
    const noteRes = await courseNoteListService(courseId.value)
    notes.value = noteRes.data || []
  }).catch(() => {})
}

const cancelEdit = () => {
  editingNoteId.value = null
  noteContent.value = ''
}

const goBack = async () => {
  await reportProgress(true)
  router.back()
}

const handleBeforeUnload = () => {
  reportProgress(true)
}

watch(() => route.query.id, async () => {
  clearReportTimer()
  sessionStartTime = ''
  await fetchCourseDetail()
})

onMounted(async () => {
  await fetchCourseDetail()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(async () => {
  clearReportTimer()
  window.removeEventListener('beforeunload', handleBeforeUnload)
  await reportProgress(true)
})
</script>

<template>
  <div class="course-study" v-loading="loading">
    <!-- 头部卡片（保持原有风格） -->
    <div class="page-header" v-if="displayCourse">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <div class="title-wrap">
          <h2>{{ displayCourse.title }}</h2>
          <p>{{ displayCourse.categoryName || '课程学习' }}</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="toggleFavorite">
          <el-icon><StarFilled v-if="displayCourse.favorite" /><Star v-else /></el-icon>
          {{ displayCourse.favorite ? '已收藏' : '收藏' }}
        </el-button>
        <el-button type="warning" plain @click="reviewCourse">重新学习</el-button>
      </div>
    </div>

    <!-- 主体：左右两列布局 -->
    <div class="study-main" v-if="displayCourse">
      <!-- 左侧：视频卡片（含进度条） -->
      <div class="left-column">
        <el-card class="video-card" shadow="never">
          <video
            ref="videoRef"
            class="video-player"
            :poster="displayCourse.cover"
            :src="displayCourse.videoUrl"
            controls
            preload="metadata"
            @loadedmetadata="handleLoadedMetadata"
            @pause="handlePause"
            @ended="handleEnded"
          ></video>
          <div class="progress-section">
            <div class="progress-header">
              <span>学习进度</span>
              <span class="progress-percent">{{ progressPercent }}%</span>
            </div>
            <el-progress :percentage="progressPercent" :stroke-width="8" :show-text="false" />
            <div class="progress-meta">
              <span>上次位置：{{ formatSeconds(displayCourse.lastSec) }}</span>
              <span>累计学习：{{ formatSeconds(displayCourse.studyTotal) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：Tab 卡片（详细介绍 / 笔记） -->
      <div class="right-column">
        <el-card class="tab-card" shadow="never">
          <el-tabs v-model="activeTab" class="detail-tabs">
            <!-- 详细介绍 Tab：包含封面、标题、元数据、描述等课程信息 -->
            <el-tab-pane label="详细介绍" name="intro">
              <div class="course-intro-content">
                <img :src="displayCourse.cover" :alt="displayCourse.title" class="intro-cover" />
                <h3 class="intro-title">{{ displayCourse.title }}</h3>
                <div class="intro-meta">
                  <span><el-icon><Clock /></el-icon> {{ durationMinutes }}分钟</span>
                  <span><el-icon><User /></el-icon> {{ formatNumber(displayCourse.enrollment) }}人学习</span>
                  <span><el-icon><Document /></el-icon> {{ displayCourse.categoryName || '未分类' }}</span>
                  <el-tag :type="getDifficultyType(displayCourse.difficulty)" size="small">{{ getDifficultyText(displayCourse.difficulty) }}</el-tag>
                </div>
                <div class="intro-description">
                  <h4>课程简介</h4>
                  <p>{{ displayCourse.description || '暂无详细介绍' }}</p>
                </div>
              </div>
            </el-tab-pane>

            <!-- 课程笔记 Tab -->
            <el-tab-pane label="课程笔记" name="notes">
              <div class="notes-content">
                <el-input
                  v-model="noteContent"
                  type="textarea"
                  :rows="4"
                  placeholder="记录您的学习心得..."
                  class="note-input"
                />
                <div class="note-actions">
                  <el-button type="primary" :loading="saving" @click="saveNote">
                    <el-icon><Edit /></el-icon>{{ editingNoteId ? '更新笔记' : '保存笔记' }}
                  </el-button>
                  <el-button v-if="editingNoteId" @click="cancelEdit">取消</el-button>
                </div>
                <div class="note-list">
                  <div v-for="item in notes" :key="item.id" class="note-item">
                    <div class="note-content">{{ item.content }}</div>
                    <div class="note-footer">
                      <span class="note-time">{{ item.updatedAt || item.createdAt }}</span>
                      <div class="note-actions-mini">
                        <el-button text @click="editNote(item)">编辑</el-button>
                        <el-button text type="danger" @click="deleteNote(item)">删除</el-button>
                      </div>
                    </div>
                  </div>
                  <el-empty v-if="!notes.length" description="暂无课程笔记，快来记录吧" />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 12px 24px;
  background: #fff;
  border-radius: 5px;
  border: 1px solid #ebeef5;

  .header-left {
    display: flex;
    align-items: center;
    gap: 30px;

    .el-button {
      font-weight: 500;
      color: #606266;
      padding: 8px 12px;
      border-radius: 5px;

      &:hover {
        background: #ecf5ff;
        color: #409eff;
      }
    }

    .title-wrap {
      h2 {
        margin: 0;
        font-size: 20px;
        color: #303133;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 8px;

        &::before {
          content: '';
          width: 4px;
          height: 18px;
          background: #409eff;
          border-radius: 2px;
          display: inline-block;
        }
      }

      p {
        margin: 4px 0 0;
        color: #909399;
        font-size: 13px;
      }
    }
  }

  .header-actions {
    display: flex;
    gap: 12px;

    .el-button {
      border-radius: 5px;
      font-weight: 500;
      padding: 9px 16px;

      .el-icon {
        margin-right: 4px;
      }
    }
  }
}

.study-main {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.left-column {
  display: flex;
  flex-direction: column;
}

.video-card {
  border-radius: 5px;
  border: 1px solid #ebeef5;

  &:deep(.el-card__body) {
    padding: 0;
  }

  .video-player {
    width: 100%;
    aspect-ratio: 16 / 9;
    display: block;
    background: #000;
    border-radius: 5px 5px 0 0;
  }

  .progress-section {
    padding: 18px 20px;
    background: #fff;
    border-radius: 0 0 5px 5px;

    .progress-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
      color: #303133;
      font-size: 16px;
      font-weight: 600;

      .progress-percent {
        color: #409eff;
      }
    }

    .el-progress {
      &:deep(.el-progress-bar__outer) {
        background: #f0f2f5;
        border-radius: 10px;
      }
      &:deep(.el-progress-bar__inner) {
        background: #409eff;
        border-radius: 10px;
      }
    }

    .progress-meta {
      display: flex;
      justify-content: space-between;
      margin-top: 12px;
      color: #909399;
      font-size: 13px;
    }
  }
}

.right-column {
  display: flex;
  flex-direction: column;
}

.tab-card {
  height: 100%;
  border-radius: 5px;
  border: 1px solid #ebeef5;

  &:deep(.el-card__body) {
    padding: 10px 15px;
  }

  .detail-tabs {
    &:deep(.el-tabs__header) {
      margin-bottom: 15px;
    }
    &:deep(.el-tabs__item) {
      font-weight: 500;
      color: #606266;
      &.is-active {
        color: #409eff;
      }
    }
    &:deep(.el-tabs__active-bar) {
      background: #409eff;
      height: 3px;
    }
  }

  .course-intro-content {
    .intro-cover {
      width: 100%;
      max-height: 200px;
      object-fit: cover;
      border-radius: 5px;
      margin-bottom: 5px;
    }

    .intro-title {
      margin: 0 0 8px;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .intro-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 20px;
      margin-bottom: 15px;
      padding-bottom: 8px;
      border-bottom: 1px solid #ebeef5;
      color: #606266;
      font-size: 14px;

      span {
        display: flex;
        align-items: center;
        gap: 6px;

        .el-icon {
          color: #409eff;
          font-size: 16px;
        }
      }

      .el-tag {
        border-radius: 20px;
        padding: 2px 10px;
        font-weight: 500;
      }
    }

    .intro-description {
      h4 {
        margin: 0 0 5px;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
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

      p {
        color: #606266;
        line-height: 1.5;
        font-size: 14px;
        margin: 0;
      }
    }
  }

  .notes-content {
    .note-input {
      margin-bottom: 15px;
      &:deep(.el-textarea__inner) {
        border-radius: 5px;
        border-color: #dcdfe6;
        &:focus {
          border-color: #409eff;
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
        }
      }
    }

    .note-actions {
      display: flex;
      gap: 12px;
      margin-bottom: 10px;

      .el-button--primary {
        background: #409eff;
        border: none;
        &:hover {
          background: #66b1ff;
        }
      }
    }

    .note-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
      max-height: 400px;
      overflow-y: auto;
    }

    .note-item {
      padding: 10px;
      border-radius: 5px;
      background: #fafafa;
      border-left: 4px solid #409eff;

      .note-content {
        color: #303133;
        line-height: 1.5;
        margin-bottom: 8px;
        white-space: pre-wrap;
        font-size: 14px;
      }

      .note-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .note-time {
          color: #909399;
          font-size: 12px;
        }

        .note-actions-mini {
          display: flex;
          gap: 8px;

          .el-button {
            padding: 4px 6px;
            color: #606266;
            &:hover {
              color: #409eff;
            }
            &.el-button--danger:hover {
              color: #f56c6c;
            }
          }
        }
      }
    }
  }
}

.el-button {
  border-radius: 5px;
  font-weight: 500;

  .el-icon {
    margin-right: 4px;
  }
}
</style>