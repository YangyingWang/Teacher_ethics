<script setup>
import { Clock, House, ArrowLeft, Help, Reading, Headset, Monitor } from '@element-plus/icons-vue'
import { ref, onMounted, onUnmounted, computed} from 'vue'
import { useRouter, useRoute } from 'vue-router'
import useUserInfoStore from '@/stores/userInfo.js' 
const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()
const userInfo = computed(() => userInfoStore.info)
  
// 响应式数据
const countdown = ref(10)
const errorPath = ref('')
const errorTime = ref('')
const isDevelopment = ref(import.meta.env.MODE === 'development')
let timer = null

// 初始化错误信息
const initErrorInfo = () => {
    errorPath.value = route.fullPath
    errorTime.value = new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    })
}
  
// 开始倒计时
const startCountdown = () => {
    timer = setInterval(() => {
        if (countdown.value > 0) {
            countdown.value--
        } else {
            clearInterval(timer)
            goHome()
        }
    }, 1000)
}
  
const goHome = () => {
    clearInterval(timer)
    router.push('/home')
}
  
const goBack = () => {
    clearInterval(timer)
    router.go(-1)
}
  
// 组件挂载时执行
onMounted(() => {
    initErrorInfo()
    startCountdown()
})
  
// 组件卸载时清理定时器
onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
    }
})
</script>

<template>
    <div class="not-found-container">
      <div class="not-found-content">
        <div class="error-illustration">
          <div class="error-number">
            <span class="number">4</span>
            <div class="planet">
              <div class="crater"></div>
              <div class="crater small"></div>
              <div class="crater medium"></div>
            </div>
            <span class="number">4</span>
          </div>
        </div>
  
        <div class="error-message">
          <h1 class="error-title">抱歉，您访问的页面不存在</h1>
          <p class="error-description">
            可能的原因：页面已被移除、URL输入错误或页面暂时无法访问
          </p>
          <div class="countdown-container">
            <el-icon class="countdown-icon"><Clock /></el-icon>
            <span class="countdown-text">
              <span class="countdown-number">{{ countdown }}</span> 秒后自动返回首页
            </span>
          </div>
        </div>
  
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="goHome" class="home-btn">
            <el-icon><House /></el-icon>立即返回首页
          </el-button>
          <el-button size="large" @click="goBack" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>返回上一页
          </el-button>
        </div>
  
        <!-- 错误详情（开发模式下显示） -->
        <div v-if="isDevelopment" class="error-details">
          <el-collapse>
            <el-collapse-item title="错误详情">
              <div class="error-info">
                <p><strong>错误路径：</strong> {{ errorPath }}</p>
                <p><strong>错误时间：</strong> {{ errorTime }}</p>
                <p><strong>用户信息：</strong> {{ userInfo?.username || '未登录' }}</p>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </div>
</template>
  
<style lang="scss" scoped>
.not-found-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #f5f7fa 0%, #eef1f5 100%);
    display: flex;
    align-items: center;
    justify-content: center;
}
  
.not-found-content {
    max-width: 800px;
    width: 100%;
    text-align: center;
}
  
.error-illustration {
    margin-bottom: 30px;
    .error-number {
        display: inline-flex;
        align-items: center;
        gap: 30px;
        position: relative;
    }
    .number {
        font-size: 120px;
        font-weight: 900;
        color: #1890ff;
        text-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
    }
    .planet {
        width: 80px;
        height: 80px;
        background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
        border-radius: 50%;
        position: relative;
        animation: float 3s ease-in-out infinite;
    }
    .planet::before {
        content: '';
        position: absolute;
        top: 10px;
        left: 20px;
        width: 20px;
        height: 20px;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 50%;
    }
  
    .crater {
        position: absolute;
        background: rgba(0, 0, 0, 0.1);
        border-radius: 50%;
    }
    .crater:nth-child(1) {
        width: 20px;
        height: 20px;
        top: 15px;
        right: 20px;
    }
    .crater.small {
        width: 12px;
        height: 12px;
        bottom: 20px;
        left: 15px;
    }
    .crater.medium {
        width: 16px;
        height: 16px;
        bottom: 30px;
        right: 10px;
    }
}
@keyframes float {
    0%, 100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-20px);
    }
}
  
.error-message {
    margin-bottom: 30px;
    .error-title {
        font-size: 28px;
        font-weight: 600;
        color: #1a1a1a;
        margin-bottom: 16px;
    }
    .error-description {
        font-size: 16px;
        color: #666;
        margin-bottom: 24px;
        line-height: 1.6;
    }
    .countdown-container {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 12px 24px;
        background: #e6f7ff;
        border-radius: 20px;
        border: 1px solid #91d5ff;
    }
    .countdown-icon {
        color: #1890ff;
        font-size: 18px;
    }
   .countdown-text {
        font-size: 16px;
        color: #1890ff;
    }
    .countdown-number {
        font-weight: 700;
        font-size: 18px;
    }
}

.action-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-bottom: 40px;
    flex-wrap: wrap;
}
  
.home-btn, .back-btn {
    padding: 12px 24px;
    border-radius: 8px;
    font-weight: 500;
}
.home-btn {
    background: #1890ff;
    border: none;
    color: white;
}
.back-btn {
    border-color: #d9d9d9;
}
  
.error-details {
    margin-top: 10px;
    padding: 10px 30px;
    background: white;
    border-radius: 8px;
    border: 1px solid #f0f0f0;

    &:deep(.el-collapse) {
        border: none;
    }
    &:deep(.el-collapse-item__header) {
        font-size: 14px;
        font-weight: 500;
        color: #666;
        border: none;
        background: transparent;
        padding: 0;
    }
    &:deep(.el-collapse-item__wrap) {
        border: none;
        background: transparent;
    }
    &:deep(.el-collapse-item__content) {
        padding: 0;
        color: #666;
    }
}
  
.error-info p {
    margin: 3px 0;
    font-size: 14px;
    line-height: 1.6;
}
  
.error-info strong {
    color: #1a1a1a;
}
  
@media (max-width: 768px) {
    .error-illustration {
      margin-bottom: 30px;
    }
    .number {
      font-size: 80px;
    }
    .planet {
      width: 60px;
      height: 60px;
    }
    .error-title {
      font-size: 24px;
    }
    .action-buttons {
      flex-direction: column;
    }
}
  
@media (max-width: 480px) {
    .not-found-container {
      padding: 20px 16px;
    }
    .number {
      font-size: 60px;
    }
    .planet {
      width: 40px;
      height: 40px;
    }
    .error-title {
      font-size: 20px;
    }
    .countdown-container {
      padding: 8px 16px;
    }
}
</style>