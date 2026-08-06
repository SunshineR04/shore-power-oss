<template>
  <div class="nf-page">
    <!-- 浅色装饰层：网格 + 光斑（与登录页同体系） -->
    <div class="bg-decor">
      <div class="bg-grid"></div>
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
    </div>

    <header class="nf-bar">
      <div class="nf-bar__mark">
        <div class="nf-bar__icon">
          <el-icon :size="20" color="#ffffff"><Ship /></el-icon>
        </div>
        <span class="nf-bar__text">长江岸电码头运维系统</span>
      </div>
    </header>

    <main class="nf-stage">
      <div class="nf-card">
        <div class="nf-illustration">
          <el-icon :size="72" color="var(--primary-light)"><Compass /></el-icon>
          <div class="nf-illustration__ring"></div>
        </div>
        <p class="nf-code">404</p>
        <h1 class="nf-title">页面不存在</h1>
        <p class="nf-desc">您访问的地址不存在或已被移除，请检查链接是否正确</p>
        <div class="nf-actions">
          <el-button type="primary" size="large" @click="goHome">
            <el-icon class="nf-btn-icon"><HomeFilled /></el-icon>
            返回首页
          </el-button>
          <el-button size="large" @click="goBack">返回上一页</el-button>
        </div>
      </div>
    </main>

    <footer class="nf-foot">长江岸电码头 · 智能运维</footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

/** 按当前角色返回对应首页 */
function goHome() {
  let home = '/home'
  try {
    const raw = sessionStorage.getItem('userInfo')
    if (raw) {
      const info = JSON.parse(raw)
      if (info.role === 'ADMIN' || info.role === 'OPERATOR') home = '/dashboard'
    }
  } catch { /* 解析失败按默认首页 */ }
  router.replace(home)
}

function goBack() {
  if (window.history.length > 1) router.back()
  else goHome()
}
</script>

<style scoped>
.nf-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(160deg, #eef4ff 0%, #f8fafc 45%, #eef2f7 100%);
  position: relative;
  overflow: hidden;
}

/* ====== 装饰层（与登录页同体系） ====== */
.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 90% 70% at 50% 40%, #000 30%, transparent 75%);
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
}

.blob-1 {
  width: 420px;
  height: 420px;
  left: -120px;
  top: -120px;
  background: rgba(37, 99, 235, 0.18);
}

.blob-2 {
  width: 380px;
  height: 380px;
  right: -100px;
  bottom: -80px;
  background: rgba(14, 165, 233, 0.16);
}

/* ====== 顶部品牌条 ====== */
.nf-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: 22px 32px;
}

.nf-bar__mark {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nf-bar__icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary), var(--accent));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.28);
}

.nf-bar__text {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.04em;
}

/* ====== 主体 ====== */
.nf-stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.nf-card {
  text-align: center;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.1);
  padding: 56px 64px;
  max-width: 520px;
  width: 100%;
  animation: nf-rise 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.nf-illustration {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nf-illustration__ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px dashed rgba(37, 99, 235, 0.3);
  animation: nf-spin 14s linear infinite;
}

.nf-code {
  font-family: var(--font-display);
  font-size: 72px;
  font-weight: 800;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  line-height: 1;
  margin-bottom: 12px;
}

.nf-title {
  font-size: var(--font-size-2xl);
  color: var(--text-primary);
  margin-bottom: 10px;
}

.nf-desc {
  font-size: var(--font-size-base);
  color: var(--text-muted);
  margin-bottom: 32px;
}

.nf-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.nf-btn-icon {
  margin-right: 4px;
}

.nf-foot {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 20px;
  font-size: var(--font-size-sm);
  color: var(--text-placeholder);
  letter-spacing: 0.08em;
}

@keyframes nf-rise {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes nf-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 480px) {
  .nf-card {
    padding: 40px 24px;
  }
  .nf-code {
    font-size: 56px;
  }
}
</style>
