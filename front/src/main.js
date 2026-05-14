import './assets/main.css'

import { createApp } from 'vue'
import ElementPlus from'element-plus'
import 'element-plus/dist/index.css'

import router from '@/router'
import App from './App.vue'
import {createPinia} from 'pinia'
import { createPersistedState } from 'pinia-persistedstate-plugin'
import locale from 'element-plus/dist/locale/zh-cn.js'
// import Antd from 'ant-design-x-vue'
// import 'ant-design-x-vue/dist/antd.css'

import routerErrorHandler from '@/utils/routerError.js'
// 初始化错误处理
routerErrorHandler.init()

const app = createApp(App)
const pinia = createPinia()
const persist = createPersistedState()

pinia.use(persist)
app.use(pinia)
app.use(router)
app.use(ElementPlus,{locale})
// app.use(Antd)

// Vue错误处理
app.config.errorHandler = (err, vm, info) => {
    console.error('Vue全局错误:', err, info)
    
    // 如果是路由错误
    if (err.message && err.message.includes('route')) {
      routerErrorHandler.handleRouteError(err, router.currentRoute.value)
    }
    
    // 生产环境中可以发送错误到监控平台
    if (process.env.NODE_ENV === 'production') {
      // sendErrorToMonitoring(err)
    }
}
// 全局未捕获错误处理
window.addEventListener('error', (event) => {
    console.error('全局未捕获错误:', event.error)
    // 防止错误页面重复跳转
    if (event.error && event.error.message && 
        event.error.message.includes('route')) {
        event.preventDefault()
    }
})

app.mount('#app')
