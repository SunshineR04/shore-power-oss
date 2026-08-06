<template>
  <div class="brand-login">
    <!-- 浅色装饰层：柔和渐变 + 网格 + 光斑 -->
    <div class="bg-decor">
      <div class="bg-grid"></div>
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
      <div class="bg-blob blob-3"></div>
    </div>

    <!-- 顶部品牌条 -->
    <header class="brand-bar">
      <div class="brand-mark">
        <div class="brand-mark__icon">
          <el-icon :size="20" color="#ffffff"><Ship /></el-icon>
        </div>
        <span class="brand-mark__text">长江岸电码头运维系统</span>
      </div>
      <span class="brand-mark__sub">Yangtze Shore Power O&amp;M</span>
    </header>

    <!-- 居中登录卡 -->
    <main class="login-stage">
      <div class="login-card">
        <div class="login-header">
          <div class="login-icon-wrap">
            <el-icon :size="26" color="#2563eb"><Ship /></el-icon>
          </div>
          <h2 class="login-title">欢迎登录</h2>
          <p class="login-subtitle">智能岸电设备管理平台</p>
        </div>

        <div v-show="mode === 'login'" class="animate-fade-in-up">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" prefix-icon="User" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" prefix-icon="Lock" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
                <span class="btn-text">登 录</span>
                <el-icon class="btn-icon"><ArrowRight /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>
          <div class="toggle-mode">
            <span>还没有账号？</span>
            <el-button type="primary" link @click="mode = 'register'">立即注册</el-button>
          </div>
        </div>

        <div v-show="mode === 'register'" class="animate-fade-in-up">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" prefix-icon="User" placeholder="请输入用户名（2-20位）" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" prefix-icon="Lock" type="password" show-password placeholder="请输入密码（6-20位）" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" prefix-icon="UserFilled" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" prefix-icon="Phone" placeholder="请输入手机号（选填）" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" prefix-icon="Message" placeholder="请输入邮箱（选填）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="registerLoading" @click="handleRegister">
                <span class="btn-text">注 册</span>
                <el-icon class="btn-icon"><ArrowRight /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>
          <div class="toggle-mode">
            <span>已有账号？</span>
            <el-button type="primary" link @click="mode = 'login'">去登录</el-button>
          </div>
        </div>
      </div>
    </main>

    <!-- 底部脚注 -->
    <footer class="scene-caption">长江岸电码头 · 智能运维</footer>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../../api'
import { useUserStore } from '../../store/user'

const router = useRouter()
const store = useUserStore()
const mode = ref('login')

const loginFormRef = ref()
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.login(loginForm)
    store.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {} finally {
    loading.value = false
  }
}

const registerFormRef = ref()
const registerLoading = ref(false)
const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: ''
})
const validatePhone = (rule, value, callback) => {
  if (value && !/^1\d{10}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度2-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await registerFormRef.value.validate()
  registerLoading.value = true
  try {
    await authApi.register(registerForm)
    ElMessage.success('注册成功，请登录')
    mode.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
  } catch {} finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
/* ====== 浅色蓝调品牌登录页 ====== */
.brand-login {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, #eef4ff 0%, #f8fafc 45%, #eef2f7 100%);
  font-family: var(--font-body);
  color: var(--text-primary);
  overflow: hidden;
}

/* 背景装饰 */
.bg-decor {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse at 50% 30%, black 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at 50% 30%, black 30%, transparent 75%);
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.blob-1 {
  width: 480px;
  height: 480px;
  top: -140px;
  right: -120px;
  background: rgba(37, 99, 235, 0.18);
}

.blob-2 {
  width: 420px;
  height: 420px;
  bottom: -160px;
  left: -100px;
  background: rgba(14, 165, 233, 0.16);
}

.blob-3 {
  width: 260px;
  height: 260px;
  top: 38%;
  left: 62%;
  background: rgba(99, 102, 241, 0.12);
}

/* 顶部品牌条 */
.brand-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark__icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.brand-mark__text {
  font-size: 16px;
  font-weight: 700;
  font-family: var(--font-display);
  letter-spacing: 0.02em;
}

.brand-mark__sub {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.06em;
  font-family: var(--font-mono);
}

/* 登录舞台 */
.login-stage {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px) saturate(160%);
  -webkit-backdrop-filter: blur(16px) saturate(160%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.12), 0 4px 12px rgba(15, 23, 42, 0.05);
  padding: 40px 36px 32px;
  animation: cardIn 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-icon-wrap {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.1) 0%, rgba(14, 165, 233, 0.08) 100%);
  border: 1px solid rgba(37, 99, 235, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-display);
  margin: 0 0 6px;
  color: var(--text-primary);
}

.login-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

/* 表单 */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2em;
  border-radius: 10px !important;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
  border: none !important;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.28);
  transition: all 0.2s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.35);
}

.btn-icon {
  margin-left: 4px;
  transition: transform 0.2s ease;
}

.login-btn:hover .btn-icon {
  transform: translateX(3px);
}

.toggle-mode {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-muted);
}

/* 底部脚注 */
.scene-caption {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 20px;
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.08em;
  font-family: var(--font-mono);
}

/* 响应式 */
@media (max-width: 480px) {
  .brand-bar {
    padding: 16px 20px;
  }

  .brand-mark__sub {
    display: none;
  }

  .login-card {
    padding: 32px 24px 24px;
  }
}
</style>
