<template>
  <div class="wharf-login">
    <!-- 夜港场景层 -->
    <div class="wharf-scene">
      <div class="sky"></div>
      <div class="moon"></div>
      <div class="stars">
        <span class="star" style="top:6%;left:8%"></span>
        <span class="star" style="top:11%;left:22%"></span>
        <span class="star" style="top:5%;left:38%"></span>
        <span class="star" style="top:14%;left:52%"></span>
        <span class="star" style="top:8%;left:68%"></span>
        <span class="star" style="top:12%;left:82%"></span>
        <span class="star" style="top:18%;left:15%"></span>
        <span class="star" style="top:20%;left:74%"></span>
        <span class="star" style="top:16%;left:90%"></span>
        <span class="star" style="top:22%;left:45%"></span>
      </div>

      <!-- 天际线：集装箱桥吊 + 船舶剪影 + 岸电缆 -->
      <svg class="skyline" viewBox="0 0 1440 220" preserveAspectRatio="xMidYEnd meet">
        <defs>
          <linearGradient id="power-grad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="rgba(34,197,94,0.65)" />
            <stop offset="50%" stop-color="rgba(6,182,212,0.55)" />
            <stop offset="100%" stop-color="rgba(34,197,94,0.65)" />
          </linearGradient>
        </defs>
        <path class="ship-hull" d="M 0 195 L 140 195 L 160 215 L 0 215 Z" />
        <path class="ship-hull" d="M 1180 195 L 1340 195 L 1360 215 L 1180 215 Z" />
        <path class="crane" d="M 180 210 L 180 80 L 360 80 L 360 210 M 200 80 L 200 58 L 340 58 L 340 80 M 270 58 L 270 28 M 248 28 L 292 28 M 270 80 L 270 210" />
        <path class="crane" d="M 560 210 L 560 90 L 760 90 L 760 210 M 580 90 L 580 68 L 740 68 L 740 90 M 660 68 L 660 36 M 638 36 L 682 36 M 660 90 L 660 210" />
        <path class="crane" d="M 980 210 L 980 85 L 1160 85 L 1160 210 M 1000 85 L 1000 63 L 1140 63 L 1140 85 M 1070 63 L 1070 32 M 1048 32 L 1092 32 M 1070 85 L 1070 210" />
        <path class="power-cable" d="M 270 28 Q 700 130 1070 32" />
      </svg>

      <div class="horizon-glow"></div>
      <div class="water"></div>
      <div class="water-shimmer"></div>
    </div>

    <!-- 顶部品牌条 -->
    <header class="brand-bar">
      <div class="brand-mark">
        <div class="brand-mark__icon">
          <el-icon :size="20" color="#22c55e"><Ship /></el-icon>
        </div>
        <span class="brand-mark__text">长江岸电码头运维系统</span>
      </div>
      <span class="brand-mark__sub">Yangtze Shore Power O&amp;M</span>
    </header>

    <!-- 居中玻璃登录卡 -->
    <main class="login-stage">
      <div class="login-card glass">
        <div class="login-header">
          <div class="login-icon-wrap">
            <el-icon :size="26" color="#22c55e"><Ship /></el-icon>
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

// 粒子动画样式生成
const getParticleStyle = (n) => {
  const size = Math.random() * 3 + 1
  const left = Math.random() * 100
  const delay = Math.random() * 8
  const duration = Math.random() * 6 + 4
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}
</script>

<style scoped>
/* ====== 夜港登录容器 ====== */
.wharf-login {
  height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  background: #060a13;
  font-family: var(--font-body);
  color: var(--text-primary);
}

/* ====== 场景层 ====== */
.wharf-scene {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.sky {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 67%;
  background: linear-gradient(180deg, #050810 0%, #0a0f1a 55%, #0d1424 100%);
}

.moon {
  position: absolute;
  top: 7%;
  right: 11%;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(241, 245, 249, 0.14) 0%, rgba(241, 245, 249, 0.04) 45%, transparent 70%);
  filter: blur(6px);
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
}

.star {
  position: absolute;
  width: 2px;
  height: 2px;
  background: rgba(241, 245, 249, 0.7);
  border-radius: 50%;
  box-shadow: 0 0 4px rgba(241, 245, 249, 0.5);
  animation: twinkle 4s ease-in-out infinite;
}
.star:nth-child(2n) { animation-delay: 1s; width: 1.5px; height: 1.5px; }
.star:nth-child(3n) { animation-delay: 2s; }
.star:nth-child(5n) { animation-delay: 3s; width: 2.5px; height: 2.5px; }

@keyframes twinkle {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.skyline {
  position: absolute;
  bottom: 33%;
  left: 0;
  width: 100%;
  height: 150px;
  opacity: 0.55;
}

.ship-hull {
  fill: rgba(6, 10, 19, 0.92);
}

.crane {
  fill: none;
  stroke: rgba(15, 23, 42, 0.9);
  stroke-width: 2;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.power-cable {
  fill: none;
  stroke: url(#power-grad);
  stroke-width: 1.5;
  stroke-dasharray: 10 6;
  animation: cable-flow 3s linear infinite;
  filter: drop-shadow(0 0 4px rgba(34, 197, 94, 0.5));
}

@keyframes cable-flow {
  to { stroke-dashoffset: -32; }
}

.horizon-glow {
  position: absolute;
  bottom: 33%;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(34, 197, 94, 0.35) 30%, rgba(6, 182, 212, 0.3) 70%, transparent);
  box-shadow: 0 0 24px rgba(34, 197, 94, 0.25);
}

.water {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 33%;
  background: linear-gradient(180deg, #080d16 0%, #050810 100%);
}

.water-shimmer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 33%;
  background: repeating-linear-gradient(
    180deg,
    transparent 0,
    transparent 6px,
    rgba(34, 197, 94, 0.04) 6px,
    rgba(34, 197, 94, 0.04) 7px
  );
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, black 30%, black 70%, transparent 100%);
  mask-image: linear-gradient(180deg, transparent 0%, black 30%, black 70%, transparent 100%);
  animation: shimmer 6s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { opacity: 0.6; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-2px); }
}

/* ====== 顶部品牌条 ====== */
.brand-bar {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 40px;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark__icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.18) 0%, rgba(6, 182, 212, 0.1) 100%);
  border: 1px solid rgba(34, 197, 94, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 16px rgba(34, 197, 94, 0.12);
}

.brand-mark__text {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.04em;
  color: var(--text-primary);
}

.brand-mark__sub {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--text-muted);
  text-transform: uppercase;
}

/* ====== 居中玻璃登录卡 ====== */
.login-stage {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px 60px;
}

.login-card.glass {
  width: 420px;
  max-width: calc(100vw - 48px);
  padding: 40px 36px;
  background: rgba(20, 28, 46, 0.55);
  backdrop-filter: blur(20px) saturate(140%);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  animation: card-enter 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes card-enter {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.12) 0%, rgba(6, 182, 212, 0.06) 100%);
  border: 1px solid rgba(34, 197, 94, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}

.login-title {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-display);
  letter-spacing: 0.02em;
}

.login-subtitle {
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 0.06em;
  margin: 0;
  font-family: var(--font-mono);
}

/* ====== 输入框（暗色玻璃内嵌） ====== */
.login-card :deep(.el-input__wrapper) {
  background: rgba(15, 21, 37, 0.6) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
  border-radius: var(--radius-sm) !important;
  transition: all 0.25s ease;
  padding: 6px 16px;
}

.login-card :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.16) !important;
}

.login-card :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary) !important;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.15) !important;
}

.login-card :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-size: 14px;
  height: 44px;
}

.login-card :deep(.el-input__inner::placeholder) {
  color: var(--text-placeholder) !important;
  font-size: 13px;
}

.login-card :deep(.el-input__prefix .el-icon) {
  color: var(--primary);
  font-size: 17px;
  transition: all 0.25s ease;
}

.login-card :deep(.el-input__wrapper.is-focus .el-input__prefix .el-icon) {
  transform: scale(1.05);
}

.login-card :deep(.el-input__suffix .el-icon) {
  color: var(--text-muted);
}

.login-card :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-card :deep(.el-form-item__error) {
  color: var(--danger);
  font-size: 12px;
  padding-top: 4px;
}

/* ====== 登录按钮 ====== */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm) !important;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.25);
  transition: all 0.25s ease;
  letter-spacing: 4px;
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  transition: left 0.5s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.35);
}

.login-btn:hover::before {
  left: 100%;
}

.login-btn:active {
  transform: translateY(0);
}

.btn-text {
  position: relative;
  z-index: 1;
}

.btn-icon {
  margin-left: 8px;
  position: relative;
  z-index: 1;
  transition: transform 0.25s ease;
}

.login-btn:hover .btn-icon {
  transform: translateX(4px);
}

/* ====== 切换模式 ====== */
.toggle-mode {
  text-align: center;
  margin-top: 20px;
  color: var(--text-muted);
  font-size: 13px;
}

.toggle-mode span {
  color: var(--text-muted);
}

.toggle-mode :deep(.el-button) {
  font-weight: 600;
  font-size: 13px;
}

.toggle-mode :deep(.el-button:hover) {
  color: var(--primary) !important;
}

/* ====== 底部脚注 ====== */
.scene-caption {
  position: relative;
  z-index: 2;
  text-align: center;
  padding: 0 0 22px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--text-placeholder);
  text-transform: uppercase;
}

/* ====== 入场动画 ====== */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in-up {
  animation: fadeInUp 0.5s ease both;
}

/* ====== 响应式 ====== */
@media (max-width: 520px) {
  .brand-bar {
    padding: 18px 20px;
  }
  .brand-mark__sub {
    display: none;
  }
  .login-card.glass {
    width: 100%;
    padding: 32px 24px;
    border-radius: var(--radius-md);
  }
  .moon {
    width: 80px;
    height: 80px;
  }
}
</style>
