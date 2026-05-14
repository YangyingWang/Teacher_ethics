<script setup>
import {
  House, User, Bell, EditPen, SwitchButton, ChatDotRound, Link, Reading, Connection,
  PieChart, Edit, VideoCamera, Headset, Cellphone, Monitor, Search, FullScreen,
  Refresh, Sunny, Moon, Lock, Fold, Expand, Setting, Message, Notification,
  ArrowDown, Help, DataBoard, Histogram
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { computed, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { userInfoService, userPwdUpdateService } from '@/api/user.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { useTokenStore } from '@/stores/token.js'

const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const router = useRouter()
const route = useRoute()

const role = computed(() => tokenStore.role || 'teacher')
const isTeacher = computed(() => role.value === 'teacher')
const defaultActive = computed(() => route.path)

const teacherMenus = [
  { type: 'item', index: '/home', icon: House, title: '首页' },
  {
    type: 'sub', index: '1', icon: Reading, title: '学习筑基', children: [
      { index: '/study/course/home', icon: Cellphone, title: '课程学习' },
      { index: '/study/search/home', icon: Link, title: '思政检索' }
    ]
  },
  {
    type: 'sub', index: '2', icon: Headset, title: '能力提升', children: [
      { index: '/test/home', icon: Connection, title: '情景测试' },
      { index: '/report/home', icon: ChatDotRound, title: '能力诊断' }
    ]
  },
  {
    type: 'sub', index: '3', icon: Edit, title: '治理研修', children: [
      { index: '/simulate/chat', icon: VideoCamera, title: '沙盘演练' },
      { index: '/evaluation/home', icon: PieChart, title: '决策评估' }
    ]
  },
  { type: 'item', index: '/assessment/home', icon: Monitor, title: '多维评估' }
]

const adminMenus = [
  { type: 'item', index: '/admin/home', icon: DataBoard, title: '管理首页' },
  { type: 'item', index: '/admin/teachers', icon: User, title: '教师信息管理' },
  { type: 'item', index: '/admin/statistics', icon: Histogram, title: '分类统计分析' }
]

const menus = computed(() => (isTeacher.value ? teacherMenus : adminMenus))

const getUserInfo = async () => {
  if (!tokenStore.token) return
  if (!isTeacher.value) {
    userInfoStore.setInfo({
      username: tokenStore.username,
      realName: tokenStore.realName,
      role: tokenStore.role
    })
    return
  }
  try {
    const result = await userInfoService()
    userInfoStore.setInfo({ ...result.data, role: tokenStore.role })
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 修改密码相关
const dialogVisible = ref(false)
const userPwd = ref({ oldPwd: '', newPwd: '', rePwd: '' })
const rules = {
  oldPwd: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { pattern: /^\S{6,16}$/, message: '密码必须是6-16位的非空字符串', trigger: 'blur' }
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^\S{6,16}$/, message: '密码必须是6-16位的非空字符串', trigger: 'blur' }
  ],
  rePwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { pattern: /^\S{6,16}$/, message: '密码必须是6-16位的非空字符串', trigger: 'blur' }
  ]
}
const updatePwd = async () => {
  if (!isTeacher.value) {
    ElMessage.info('管理员修改密码接口暂未接入')
    return
  }
  try {
    const result = await userPwdUpdateService(userPwd.value)
    ElMessage.success(result.message || '密码修改成功!')
    dialogVisible.value = false
    clearUserPwd()
  } catch (error) {
    ElMessage.error(error?.message || '密码修改失败，请稍后重试!')
  }
}

const clearUserPwd = () => {
  userPwd.value = { oldPwd: '', newPwd: '', rePwd: '' }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确认退出登录吗？', '温馨提示', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      //1.清空pinia中存储的token以及个人信息
      tokenStore.removeToken()
      userInfoStore.removeInfo()
      //2.跳转到登录页面
      router.push('/login')
      ElMessage.success('退出登录成功!')
    }).catch(() => {})
  } else if (command === 'pwd') {
    dialogVisible.value = true
  } else if (command === 'info') {
    if (isTeacher.value) {
      router.push('/user/info')
    } else {
      ElMessage.info('管理员个人中心暂未接入')
    }
  }
}

// 侧边栏折叠
const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
// 主题切换
const isDarkMode = ref(false)
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  if (isDarkMode.value) {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}
// 全屏切换
const isFullScreen = ref(false)
const updateFullScreenIcon = () => {
  isFullScreen.value = !!document.fullscreenElement
}
const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else if (document.exitFullscreen) {
    document.exitFullscreen()
  }
}
// 刷新页面
const refreshPage = () => {
  router.go(0)
}

onMounted(() => {
  getUserInfo()
  // 监听全屏变化
  document.addEventListener('fullscreenchange', updateFullScreenIcon)
})

watch(() => tokenStore.role, () => {
  getUserInfo()
})
</script>

<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <img src="/logo.png" class="logo" alt="系统Logo" />
        <h1 class="system-title">师德师风教育管理系统</h1>
      </div>
      <div class="header-right">
        <el-tooltip content="搜索" placement="bottom">
          <el-button :icon="Search" circle class="header-btn" />
        </el-tooltip>
        <el-dropdown placement="bottom-end" class="notification-dropdown">
          <el-badge :value="3" :max="99" class="notification-badge">
            <el-button :icon="Bell" circle class="header-btn" />
          </el-badge>
          <template #dropdown>
            <el-dropdown-menu class="notification-menu">
              <div class="notification-header">
                <h3>系统通知</h3>
                <el-button type="text" size="small">全部已读</el-button>
              </div>
              <el-dropdown-item>
                <div class="notification-item">
                  <el-icon class="notification-icon"><Message /></el-icon>
                  <div class="notification-content">
                    <div class="notification-title">师德评估报告生成</div>
                    <div class="notification-time">10分钟前</div>
                  </div>
                </div>
              </el-dropdown-item>
              <el-dropdown-item>
                <div class="notification-item">
                  <el-icon class="notification-icon"><Notification /></el-icon>
                  <div class="notification-content">
                    <div class="notification-title">欢迎使用系统</div>
                    <div class="notification-time">1小时前</div>
                  </div>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-tooltip :content="isFullScreen ? '退出全屏' : '全屏显示'" placement="bottom">
          <el-button :icon="FullScreen" circle class="header-btn" @click="toggleFullScreen" />
        </el-tooltip>
        <el-tooltip content="刷新页面" placement="bottom">
          <el-button :icon="Refresh" circle class="header-btn" @click="refreshPage" />
        </el-tooltip>
        <el-tooltip :content="isDarkMode ? '切换浅色模式' : '切换深色模式'" placement="bottom">
          <el-switch v-model="isDarkMode" :active-icon="Moon" :inactive-icon="Sunny" size="large" @change="toggleTheme" />
        </el-tooltip>
        <el-dropdown placement="bottom-end" @command="handleCommand" class="user-dropdown">
          <div class="user-info">
            <el-avatar :src="userInfoStore.info.avatar || '/src/assets/avatar.png'" class="user-avatar" />
            <div class="user-name">{{ userInfoStore.info.realName || tokenStore.realName || '用户' }}</div>
            <el-tag size="small" type="primary">{{ isTeacher ? '教师' : '管理员' }}</el-tag>
            <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="user-menu">
              <el-dropdown-item command="info" class="menu-item" :icon="User">个人中心</el-dropdown-item>
              <el-dropdown-item command="pwd" class="menu-item" :icon="EditPen">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout" class="menu-item logout-item" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <el-aside :width="isCollapse ? '64px' : '200px'" class="layout-sidebar">
        <el-menu :collapse="isCollapse" 
          :default-active="defaultActive" router 
          class="sidebar-menu" 
          background-color="#d2e6fa" 
          text-color="#303133" 
          active-text-color="#1890ff"
        >
          <template v-for="menu in menus" :key="menu.index">
            <el-menu-item v-if="menu.type === 'item'" :index="menu.index">
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="menu.index">
              <template #title>
                <el-icon><component :is="menu.icon" /></el-icon>
                <span>{{ menu.title }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.index" :index="child.index">
                <el-icon><component :is="child.icon" /></el-icon>
                <span>{{ child.title }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>

        <div class="sidebar-footer" @click="toggleCollapse">
          <el-tooltip :content="isCollapse ? '展开菜单' : '收起菜单'" placement="right">
            <el-icon class="collapse-icon"><component :is="isCollapse ? Expand : Fold" /></el-icon>
          </el-tooltip>
        </div>
      </el-aside>

      <el-container class="content-container">
        <el-main class="page-main">
          <router-view />
        </el-main>
        <el-footer class="layout-footer">
          <div class="footer-content">
            <div class="footer-links">
              <a href="#" class="footer-link">关于我们</a>
              <span class="footer-separator">|</span>
              <a href="#" class="footer-link">帮助文档</a>
              <span class="footer-separator">|</span>
              <a href="#" class="footer-link">隐私政策</a>
              <span class="footer-separator">|</span>
              <a href="#" class="footer-link">联系我们</a>
            </div>
            <div class="copyright">
              上海海洋大学 © 2026 师德师风教育管理系统
            </div>
          </div>
        </el-footer>
      </el-container>
    </el-container>
  </el-container>
  <!-- 修改密码对话框 -->
  <el-dialog v-model="dialogVisible" title="修改密码" width="400px" center class="password-dialog">
    <el-form :model="userPwd" :rules="rules" label-width="100px" label-position="top" size="large" @submit.prevent>
      <el-form-item label="原密码" prop="oldPwd">
        <el-input :prefix-icon="Lock" type="password" v-model="userPwd.oldPwd" placeholder="请输入原密码" show-password/>
      </el-form-item>
      <el-form-item label="新密码" prop="newPwd">
        <el-input :prefix-icon="Lock" type="password" v-model="userPwd.newPwd" placeholder="请输入新密码" show-password/>
      </el-form-item>
      <el-form-item label="确认新密码" prop="rePwd">
        <el-input :prefix-icon="Lock" type="password" v-model="userPwd.rePwd" placeholder="请再次输入新密码" show-password/>
      </el-form-item>
      <div class="password-tips">
        <el-icon><Help /></el-icon>
        <span>密码应为6-16位，包含字母、数字或特殊字符</span>
      </div>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false; clearUserPwd()">取消</el-button>
        <el-button type="primary" @click="updatePwd()" :loading="false">确认修改</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.layout-container { height: 100vh; }

.layout-header {
  height: 64px;
  background: linear-gradient(135deg, #aad1f8 0%, #4ea5f7 100%);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;

  .logo {
    height: 50px;
    width: auto;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  }
  .system-title {
    margin: 0;
    font-size: 32px;
    font-weight: 600;
    color: #fff;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    letter-spacing: 1px;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;

  .header-btn {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: white;
    transition: all 0.3s ease;
    &:hover {
      background: rgba(255, 255, 255, 0.2);
      transform: translateY(-1px);
    }
  }

  .notification-dropdown {
    margin: 0 5px;
    &:deep(.el-badge__content) {
      transform: scale(0.8);
      transform-origin: 100% 0;
    }
  }

  .theme-switch {
    --el-switch-on-color: #1677ff;
     --el-switch-off-color: #d9d9d9;
  }

  .user-dropdown {
    margin-left: 8px;
    cursor: pointer;
    .user-info {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 4px 8px;
        border-radius: 6px;
        transition: background-color 0.3s;
        &:hover {
            background: rgba(255, 255, 255, 0.1);
        }
        
        .user-avatar {
          border: 2px solid rgba(255, 255, 255, 0.3);
        }
        .user-name {
          font-size: 14px;
          font-weight: 600;
          color: white;
        }
        .dropdown-arrow {
          color: white;
          font-size: 12px;
          margin-left: 4px;
        }
    }
  }
}

.notification-menu {
  width: 320px;
  padding: 0;
  .notification-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    h3 {
      margin: 0;
      font-size: 16px;
      color: #303133;
    }
  }

  .notification-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0px 16px;
    cursor: pointer;
    transition: background-color 0.2s;

    .notification-icon {
        color: #1890ff;
        font-size: 18px;
    }

    .notification-content {
        flex: 1;
        .notification-title {
          font-size: 14px;
          color: #303133;
          margin-bottom: 2px;
        }
        .notification-time {
          font-size: 12px;
          color: #909399;
        }
    }
  }

  .notification-view-all {
    text-align: center;
    padding: 0;
  }
}

.user-menu {
    width: 120px;
    padding: 0;
    .menu-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        font-size: 16px;
    }
}

.logout-item {
  color: #ff4d4f;
  &:hover {
    color: #ff7875;
    background-color: #fff2f0;
  }
}

.main-container { height: calc(100vh - 64px); }

.layout-sidebar {
  background: #d2e6fa;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #c8def5;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  .sidebar-menu {
    flex: 1;
    border-right: none;

    &:deep(.el-menu-item), &:deep(.el-sub-menu__title) {
        height: 50px;
        line-height: 50px;
        margin: 4px 8px;
        border-radius: 6px;
    }
    &:deep(.el-menu-item.is-active) {
        background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
        color: #1890ff;
        font-weight: 500;
        border-left: 3px solid #1890ff;
    }

    &:deep(.el-menu-item:hover), &:deep(.el-sub-menu__title:hover) {
        background-color: #f5f7fa;
    }
  }

  .sidebar-footer {
    height: 42px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border-top: 1px solid #c8def5;
    color: #000000;
    font-size: 14px;
    .collapse-icon {
        font-size: 18px;
    }
  }
}

.page-main { 
  background: #f5f7fa; 
  padding: 10px 10px 0 10px;
}

.layout-footer {
    height: 42px;
    background: #ebf2f7;
    display: flex;
    align-items: center;
    justify-content: center;
    .footer-content{
        .footer-links {
            .footer-link {
              color: #666;
              text-decoration: none;
              font-size: 12px;
              margin: 0 8px;
              transition: color 0.3s;
            &:hover {
              color: #1890ff;
            }
            }
            .footer-separator {
              color: #666;
              font-size: 12px;
            }
        }
        .copyright {
            color: #666;
            font-size: 12px;
        }
    }
}

.password-dialog {
  &:deep(.el-dialog__header){
    padding-bottom: 16px;
    margin-bottom: 20px;
  }
  &:deep(.el-dialog__title) {
    font-size: 20px;
    font-weight: 800;
  }
}

.password-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  color: #52c41a;
  font-size: 13px;
  margin-top: 16px;
  .password-tips .el-icon {
    font-size: 16px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
