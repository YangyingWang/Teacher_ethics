<script setup>
import { User, Lock, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { userRegisterService, userLoginService } from '@/api/user.js'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { useRouter } from 'vue-router'

const isRegister = ref(false)
const registerStep = ref(1)
const captchaUrl = ref('')
const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const registerStepOneRef = ref()
const registerStepTwoRef = ref()
const loginFormRef = ref()

const registerData = ref({
  username: '',
  password: '',
  rePassword: '',
  realName: '',
  sex: 1,
  identityCard: '',
  birthday: '',
  email: '',
  phone: ''
})

const loginUserData = ref({
  username: '',
  password: '',
  verCode: ''
})

// 校验密码的函数
const checkRePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次确认密码！'))
  } else if (value !== registerData.value.password) {
    callback(new Error('请确保两次输入的密码一致！'))
  } else {
    callback()
  }
}

// 登录校验规则
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  verCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 注册第一步规则
const registerStepOneRules = {
  username: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[0-9]{7}$/, message: '长度为7位数字', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '长度为6~16位非空字符', trigger: 'blur' }
  ],
  rePassword: [{ validator: checkRePassword, trigger: 'blur' }]
}

// 注册第二步规则
const registerStepTwoRules = {
  birthday: [{ required: true, message: '请选择生日', trigger: 'change' }],
  identityCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6}$/,
      message: '请输入正确的邮箱格式',
      trigger: 'blur'
    }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

const nextStep = async () => {
  if (!registerStepOneRef.value) return
  try {
    await registerStepOneRef.value.validate()
    registerStep.value = 2
  } catch (error) {
    ElMessage.warning('请先完善必填信息')
  }
}

const prevStep = () => {
  registerStep.value = 1
}

const register = async () => {
  if (!registerStepTwoRef.value) return
  try {
    await registerStepTwoRef.value.validate()
    const result = await userRegisterService(registerData.value)
    ElMessage.success(result.message || '注册成功！')
    isRegister.value = false
    registerStep.value = 1
    clearRegisterData()
    await loadCaptcha()
  } catch (error) {
    ElMessage.error(error?.message || '注册失败，请检查填写信息！')
  }
}

const login = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
    const result = await userLoginService(loginUserData.value)
    const auth = result.data || {}

    tokenStore.setAuth(auth)
    userInfoStore.setInfo({
      username: auth.username,
      realName: auth.realName,
      role: auth.role
    })

    ElMessage.success(result.message || '登录成功！')
    router.push(auth.role === 'admin' ? '/admin/home' : '/home')
  } catch (error) {
    await loadCaptcha()
    ElMessage.error(error?.message || '登录失败，请检查账号、密码或验证码！')
  }
}

const loadCaptcha = async () => {
  try {
    const { data } = await axios.get('/api/user/captcha')
    captchaUrl.value = data.imgBase64
  } catch (error) {
    console.error('验证码加载失败', error)
  }
}

onMounted(() => {
  loadCaptcha()
})

const clickImg = () => {
  loadCaptcha()
}

const clearRegisterData = () => {
  registerData.value = {
    username: '',
    password: '',
    rePassword: '',
    realName: '',
    sex: 1,
    identityCard: '',
    birthday: '',
    email: '',
    phone: ''
  }
}

const clearLoginUserData = () => {
  loginUserData.value = {
    username: '',
    password: '',
    verCode: ''
  }
}
</script>

<template>
  <div class="login-page">
    <div class="left-panel">
      <div class="left-content">
        <div class="system-title">基于AI技术的师德师风教育管理系统</div>
        <div class="system-desc">
          面向高校教师发展与管理场景，覆盖学习筑基、能力提升、治理研修、多维评估等核心模块，
          支撑教师成长与学校治理的全过程数字化建设。
          </div>
          <div class="system-tags">
            <span>学习筑基</span>
            <span>能力提升</span>
            <span>治理研修</span>
            <span>多维评估</span>
          </div>
      </div>
    </div>

    <div class="form">
      <div class="card">
        <!-- 注册表单 -->
        <template v-if="isRegister">
          <!-- 第一步 -->
          <el-form
            v-if="registerStep === 1"
            ref="registerStepOneRef"
            size="large"
            autocomplete="off"
            :model="registerData"
            :rules="registerStepOneRules"
          >
            <div class="title">欢迎注册</div>
            <div class="title1">请先完成基础信息填写</div>

            <div class="step-bar">
              <div class="step-item active">
                <span>1</span>
                <em>基础信息</em>
              </div>
              <div class="step-line"></div>
              <div class="step-item">
                <span>2</span>
                <em>完善信息</em>
              </div>
            </div>

            <el-form-item label="账号" prop="username">
              <el-input :prefix-icon="User" placeholder="请输入工号" v-model="registerData.username" />
            </el-form-item>

            <el-form-item label="姓名" prop="realName">
              <el-input v-model="registerData.realName" placeholder="请输入真实姓名" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input :prefix-icon="Lock" type="password" placeholder="请输入密码" v-model="registerData.password" show-password />
            </el-form-item>

            <el-form-item prop="rePassword">
              <el-input :prefix-icon="Lock" type="password" placeholder="请确认密码" v-model="registerData.rePassword" show-password />
            </el-form-item>

            <el-form-item>
              <el-button class="button" type="primary" @click="nextStep" auto-insert-space>下一步</el-button>
            </el-form-item>

            <el-form-item class="flex">
              <el-link type="info" :underline="false" @click="isRegister = false; registerStep = 1; clearRegisterData()">← 返回登录</el-link>
            </el-form-item>
          </el-form>

          <!-- 第二步 -->
          <el-form
            v-else
            ref="registerStepTwoRef"
            size="large"
            autocomplete="off"
            :model="registerData"
            :rules="registerStepTwoRules"
          >
            <div class="title">注册</div>
            <div class="title1">请继续补充个人资料并完成注册</div>

            <div class="step-bar">
              <div class="step-item finished">
                <span>1</span>
                <em>基础信息</em>
              </div>
              <div class="step-line active"></div>
              <div class="step-item active">
                <span>2</span>
                <em>完善信息</em>
              </div>
            </div>

            <el-form-item label="身份证号" prop="identityCard">
              <el-input v-model="registerData.identityCard" placeholder="请输入身份证号码" />
            </el-form-item>

            <el-form-item label="生日" prop="birthday">
              <el-date-picker
                v-model="registerData.birthday"
                type="date"
                placeholder="选择出生日期"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              />
            </el-form-item>

            <el-form-item label="性别">
              <el-radio-group v-model="registerData.sex">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerData.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="手机" prop="phone">
              <el-input v-model="registerData.phone" placeholder="请输入手机号" />
            </el-form-item>

            <div class="double-btn">
              <el-button class="back-btn" @click="prevStep">上一步</el-button>
              <el-button class="button" type="primary" @click="register" auto-insert-space>注册</el-button>
            </div>

            <el-form-item class="flex">
              <el-link type="info" :underline="false" @click="isRegister = false; registerStep = 1; clearRegisterData()">← 返回登录</el-link>
            </el-form-item>
          </el-form>
        </template>

        <!-- 登录表单 -->
        <el-form
          v-else
          ref="loginFormRef"
          size="large"
          autocomplete="off"
          :model="loginUserData"
          :rules="loginRules"
        >
          <div class="title">欢迎回来</div>
          <el-form-item>
            <div class="title1">登录后开启您的学习与成长之旅</div>
          </el-form-item>

          <el-form-item prop="username">
            <el-input :prefix-icon="User" placeholder="请输入账号（教师工号或管理员账号）" v-model="loginUserData.username" />
          </el-form-item>

          <el-form-item prop="password">
            <el-input :prefix-icon="Lock" type="password" placeholder="请输入密码" v-model="loginUserData.password" show-password />
          </el-form-item>

          <el-form-item prop="verCode">
            <div class="captcha-row">
              <el-input
                v-model="loginUserData.verCode"
                :prefix-icon="Check"
                class="captcha-input"
                placeholder="请输入验证码"
              />
              <img :src="captchaUrl" alt="图片无法加载" @click="clickImg" class="captcha-img" />
            </div>
          </el-form-item>

          <el-form-item class="flex">
            <div class="flex">
              <el-checkbox>记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button class="button" type="primary" auto-insert-space @click="login">登录</el-button>
          </el-form-item>

          <el-form-item class="flex">
            <el-link type="info" :underline="false">还没有账户？</el-link>
            <el-link type="primary" :underline="false" @click="isRegister = true; registerStep = 1; clearLoginUserData()">注册 →</el-link>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  display: flex;
  align-items: stretch;
  width: 100%;
  min-height: 100vh;
  background: url('@/assets/login.png') no-repeat center / cover;
}

.left-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  padding: 0 72px;
  box-sizing: border-box;
}

.left-content {
  max-width: 620px;
  color: #fff;
}

.system-title {
  font-size: 38px;
  font-weight: bold;
  line-height: 1.35;
  margin-bottom: 18px;
  text-shadow: 0 6px 18px rgba(0, 0, 0, 0.16);
}

.system-desc {
  font-size: 16px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.92);
  margin-bottom: 28px;
  max-width: 620px;
}

.system-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.form {
  width: 100%;
  max-width: 460px;
  min-height: 100vh;
  background: rgba(255,255,255,0.6);
  backdrop-filter: blur(10px);
  box-shadow: 0 0 15px rgba(0,0,0,0.3);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card {
  width: 400px;
  max-width: calc(100% - 40px);
  padding: 30px;
  border-radius: 12px;
  margin: 0 auto;
  box-sizing: border-box;
}

.title {
  margin: 0 auto 0 auto;
  text-align: center;
  font-size: 24px;
  font-weight: bold;
}

.title1 {
  margin: 0 auto 0 auto;
  text-align: center;
  font-size: 15px;
  font-weight: normal;
}

.button {
  width: 100%;
  background-color: #002e5d;
  color: #fff;
  border-radius: 20px;
}

.flex {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

.step-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 4px 0 20px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #7c8696;
}

.step-item span {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(0, 46, 93, 0.10);
  color: #002e5d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.step-item em {
  font-style: normal;
  font-size: 12px;
}

.step-item.active {
  color: #002e5d;
  font-weight: 600;
}

.step-item.active span {
  background: #002e5d;
  color: #fff;
}

.step-item.finished {
  color: #002e5d;
}

.step-line {
  width: 56px;
  height: 2px;
  background: rgba(0, 46, 93, 0.18);
  margin: 0 10px 18px;
}

.step-line.active {
  background: #002e5d;
}

.double-btn {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.double-btn .button,
.double-btn .back-btn {
  flex: 1;
}

.back-btn {
  border-radius: 20px;
}

.captcha-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 140px;
  height: 35px;
  flex-shrink: 0;
  cursor: pointer;
  object-fit: cover;
}

@media (max-width: 1200px) {
  .left-panel {
    padding: 0 40px;
  }

  .system-title {
    font-size: 32px;
  }

  .form {
    max-width: 430px;
  }
}

@media (max-width: 900px) {
  .left-panel {
    padding: 0 28px;
  }

  .system-title {
    font-size: 26px;
  }

  .system-desc {
    font-size: 14px;
  }

  .form {
    max-width: 400px;
  }

  .card {
    width: 360px;
  }
}

@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .left-panel {
    min-height: 220px;
    align-items: flex-end;
    padding: 28px 20px 16px;
  }

  .left-content {
    max-width: 100%;
  }

  .system-title {
    font-size: 22px;
    margin-bottom: 10px;
  }

  .system-desc {
    font-size: 13px;
    line-height: 1.7;
  }

  .form {
    max-width: none;
    width: 100%;
    min-height: calc(100vh - 220px);
    justify-content: flex-start;
    padding-top: 10px;
  }

  .card {
    width: 100%;
    max-width: 420px;
    padding: 24px 20px;
  }

  .captcha-row {
    flex-direction: column;
    align-items: stretch;
  }

  .captcha-img {
    width: 100%;
    height: 40px;
  }

  .double-btn {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .left-panel {
    min-height: 190px;
    padding: 20px 16px 12px;
  }

  .system-title {
    font-size: 20px;
  }

  .system-desc {
    font-size: 12px;
  }

  .card {
    padding: 20px 14px;
    max-width: calc(100% - 16px);
  }
}
</style>