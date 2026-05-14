import router from '@/router'
import { ElMessage, ElMessageBox } from 'element-plus'

// 路由错误处理工具类
class RouterErrorHandler {
  constructor() {
    this.errorQueue = []
    this.isHandling = false
  }

  // 处理路由错误
  handleRouteError(error, routeInfo) {
    const errorObj = {
      type: 'ROUTE_ERROR',
      error,
      routeInfo,
      timestamp: new Date().toISOString()
    }

    // 添加到错误队列
    this.errorQueue.push(errorObj)

    // 记录到控制台
    console.error('路由错误:', errorObj)

    // 如果是开发环境，显示详细错误
    if (process.env.NODE_ENV === 'development') {
      console.error('详细错误信息:', error.stack)
    }

    // 根据错误类型显示不同提示
    this.showErrorMessage(error, routeInfo)
  }

  // 显示错误信息
  showErrorMessage(error, routeInfo) {
    // 避免重复提示
    if (this.isHandling) return
    
    this.isHandling = true

    // 根据错误类型决定显示什么消息
    let message = '页面加载失败，请稍后重试'
    
    if (error.message.includes('NetworkError')) {
      message = '网络连接失败，请检查网络设置'
    } else if (error.message.includes('timeout')) {
      message = '请求超时，请检查网络连接'
    } else if (error.message.includes('404')) {
      // 404错误已经由路由配置处理，这里不需要额外提示
      this.isHandling = false
      return
    }

    // 显示错误提示
    ElMessageBox.confirm(
      `${message}，是否重试？`,
      '页面加载失败',
      {
        confirmButtonText: '重试',
        cancelButtonText: '返回首页',
        type: 'error',
        closeOnClickModal: false,
        closeOnPressEscape: false
      }
    ).then(() => {
      // 重试当前路由
      router.push(routeInfo.fullPath)
    }).catch(() => {
      // 返回首页
      router.push('/home')
    }).finally(() => {
      this.isHandling = false
    })
  }

  // 处理API错误（可选）
  handleApiError(error) {
    const errorObj = {
      type: 'API_ERROR',
      error,
      timestamp: new Date().toISOString()
    }

    console.error('API错误:', errorObj)

    // 显示错误提示
    ElMessage.error({
      message: '请求失败，请检查网络连接',
      duration: 3000,
      showClose: true
    })
  }

  // 处理全局未捕获的Promise错误
  handleUnhandledRejection(event) {
    console.error('未处理的Promise错误:', event.reason)
    
    // 如果是路由相关的错误
    if (event.reason && event.reason.message && 
        (event.reason.message.includes('route') || 
         event.reason.message.includes('router'))) {
      this.handleRouteError(event.reason, {})
    }
  }

  // 初始化错误监听
  init() {
    // 监听未处理的Promise错误
    window.addEventListener('unhandledrejection', this.handleUnhandledRejection.bind(this))

    // 监听Vue错误（如果在main.js中注册）
    if (window.Vue) {
      window.Vue.config.errorHandler = (err, vm, info) => {
        console.error('Vue错误:', err, info)
      }
    }
  }

  // 清理
  destroy() {
    window.removeEventListener('unhandledrejection', this.handleUnhandledRejection)
  }
}

// 创建单例
const routerErrorHandler = new RouterErrorHandler()

export default routerErrorHandler