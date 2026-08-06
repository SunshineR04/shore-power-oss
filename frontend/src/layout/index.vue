<template>
  <el-container style="height: 100%">
    <el-aside :width="isCollapse ? '64px' : '240px'" class="app-sidebar">
      <div class="sidebar-head">
        <div class="brand-mark" :class="{ collapsed: isCollapse }">
          <div class="brand-mark__icon">
            <el-icon :size="22" color="#2563eb"><Ship /></el-icon>
          </div>
          <transition name="fade">
            <span v-show="!isCollapse" class="brand-mark__text">岸电运维</span>
          </transition>
        </div>
      </div>
      <el-menu :default-active="$route.path" router background-color="transparent"
               :collapse="isCollapse" :collapse-transition="false"
               class="sidebar-menu">
        <template v-if="store.isSuperAdmin()">
          <el-menu-item index="/dashboard" class="menu-item">
            <el-icon><Monitor /></el-icon><span>系统首页</span>
          </el-menu-item>
          <el-menu-item index="/device" class="menu-item">
            <el-icon><SetUp /></el-icon><span>设备管理</span>
          </el-menu-item>
          <el-menu-item index="/alarm" class="menu-item">
            <el-icon><Bell /></el-icon><span>故障预警</span>
          </el-menu-item>
          <el-menu-item index="/energy" class="menu-item">
            <el-icon><TrendCharts /></el-icon><span>能耗中心</span>
          </el-menu-item>
          <el-menu-item index="/maintenance" class="menu-item">
            <el-icon><Tools /></el-icon><span>维护调度</span>
          </el-menu-item>
          <el-menu-item index="/user" class="menu-item">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/finance" class="menu-item">
            <el-icon><Money /></el-icon><span>营收统计</span>
          </el-menu-item>
          <el-menu-item index="/system/config" class="menu-item">
            <el-icon><Setting /></el-icon><span>系统设置</span>
          </el-menu-item>
        </template>

        <template v-else-if="store.isOperator()">
          <el-menu-item index="/dashboard" class="menu-item">
            <el-icon><Monitor /></el-icon><span>设备监测</span>
          </el-menu-item>
          <el-menu-item index="/operator/tasks" class="menu-item">
            <el-icon><Tools /></el-icon><span>我的任务</span>
          </el-menu-item>
          <el-menu-item index="/operator/alarms" class="menu-item">
            <el-icon><Bell /></el-icon><span>告警处理</span>
          </el-menu-item>
        </template>

        <template v-else>
          <el-menu-item index="/home" class="menu-item">
            <el-icon><HomeFilled /></el-icon><span>首页</span>
          </el-menu-item>
          <el-menu-item index="/user/ship" class="menu-item">
            <el-icon><Ship /></el-icon><span>我的船舶</span>
          </el-menu-item>
          <el-menu-item index="/user/reservations" class="menu-item">
            <el-icon><Calendar /></el-icon><span>我的预约</span>
          </el-menu-item>

        </template>
      </el-menu>

      <div class="sidebar-foot" :class="{ collapsed: isCollapse }">
        <div class="status-strip">
          <span class="status-dot"></span>
          <transition name="fade">
            <span v-show="!isCollapse" class="status-text">系统在线</span>
          </transition>
        </div>
        <div class="cable-accent"></div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <button class="collapse-btn" @click="manualToggle = true; isCollapse = !isCollapse">
            <el-icon :size="18"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </button>
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <div v-if="store.isAdmin()" class="alarm-badge" role="button" tabindex="0"
               @click="$router.push('/alarm')" @keydown.enter="$router.push('/alarm')">
            <el-badge :value="pendingAlarms" :hidden="!pendingAlarms" :max="99">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </div>
          <div v-if="store.isOperator()" class="alarm-badge" role="button" tabindex="0"
               @click="$router.push('/operator/notifications')" @keydown.enter="$router.push('/operator/notifications')">
            <el-badge :value="unreadNotifs" :hidden="!unreadNotifs" :max="99">
              <el-icon :size="18"><Message /></el-icon>
            </el-badge>
          </div>
          <el-dropdown @command="handleCmd" class="user-dropdown">
            <div class="user-info">
              <el-avatar :size="34" :src="store.userInfo.avatar || ''" class="user-avatar">
                {{ store.userInfo.realName?.[0] || 'U' }}
              </el-avatar>
              <div class="user-detail">
                <span class="user-name">{{ store.userInfo.realName || store.userInfo.username }}</span>
                <el-tag size="small" :type="store.isSuperAdmin() ? 'danger' : store.isOperator() ? 'warning' : 'info'" effect="plain" class="user-role-tag">
                  {{ store.isSuperAdmin() ? '管理员' : store.isOperator() ? '运维人员' : '普通用户' }}
                </el-tag>
              </div>
              <el-icon :size="14" color="var(--text-muted)"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { alarmApi, userApi } from '../api'
import request from '../utils/request'

const store = useUserStore()
const router = useRouter()
const isCollapse = ref(false)
// 移动端视口自适应：<768px 自动折叠为图标栏；用户手动切换后不再自动干预
const manualToggle = ref(false)
const applyViewport = () => {
  if (manualToggle.value) return
  isCollapse.value = window.innerWidth < 768
}
const pendingAlarms = ref(0)
const unreadNotifs = ref(0)

let timer = null
// 待处理告警为运维数据：仅 ADMIN/OPERATOR 轮询，普通用户跳过（避免 403 刷屏）
const isStaff = ['ADMIN', 'OPERATOR'].includes(store.userInfo?.role)
const fetchAlarms = async () => {
  if (!isStaff) return
  try {
    const res = await alarmApi.pendingCount()
    pendingAlarms.value = res.data || 0
  } catch {}
}

const fetchNotifs = async () => {
  try {
    const res = await request.get('/notification/unread-count')
    unreadNotifs.value = res.data || 0
  } catch {}
}

const fetchUserInfo = async () => {
  try {
    const res = await userApi.info()
    const data = res.data || {}
    store.userInfo = data
    sessionStorage.setItem('userInfo', JSON.stringify(data))
  } catch {}
}

onMounted(() => {
  applyViewport()
  window.addEventListener('resize', applyViewport)
  fetchAlarms()
  fetchUserInfo()
  fetchNotifs()
  timer = setInterval(() => {
    fetchAlarms()
    fetchNotifs()
  }, 30000)
})
onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('resize', applyViewport)
})

const handleCmd = cmd => {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
/* ====== 侧边栏 ====== */
.app-sidebar {
  background: var(--sidebar-bg);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  border-right: 1px solid var(--border-light);
}

.sidebar-head {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark.collapsed {
  justify-content: center;
  padding: 0;
}

.brand-mark__icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12) 0%, rgba(14, 165, 233, 0.06) 100%);
  border: 1px solid rgba(37, 99, 235, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(37, 99, 235, 0.08);
}

.brand-mark__text {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  font-family: var(--font-display);
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding: 12px 8px;
}

.sidebar-menu::-webkit-scrollbar {
  width: 0;
}

.menu-item {
  margin-bottom: 2px;
  border-radius: var(--radius-sm) !important;
  height: 42px !important;
  line-height: 42px !important;
  transition: all 0.2s ease !important;
  color: var(--sidebar-text) !important;
  font-size: 13px;
}

.menu-item:hover {
  background: var(--sidebar-hover) !important;
  color: var(--sidebar-text-active) !important;
}

.menu-item.is-active {
  background: rgba(37, 99, 235, 0.08) !important;
  color: var(--primary) !important;
  position: relative;
  font-weight: 600;
}

.menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--primary);
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(37, 99, 235, 0.3);
}

.menu-item .el-icon {
  font-size: 17px;
}

/* ====== 侧边栏底部状态条 ====== */
.sidebar-foot {
  flex-shrink: 0;
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
}

.sidebar-foot.collapsed {
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.status-strip {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 8px rgba(37, 99, 235, 0.4);
  animation: pulse-dot 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.status-text {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.08em;
}

.cable-accent {
  height: 2px;
  margin-top: 10px;
  border-radius: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.3) 30%, rgba(14, 165, 233, 0.2) 70%, transparent);
}

.sidebar-foot.collapsed .cable-accent {
  width: 24px;
  margin: 10px auto 0;
}

/* ====== 顶部 header（浅色玻璃） ====== */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px) saturate(140%);
  -webkit-backdrop-filter: blur(12px) saturate(140%);
  border-bottom: 1px solid var(--header-border);
  padding: 0 24px;
  height: 56px !important;
  position: relative;
}

.app-header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.18) 50%, transparent);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  border-radius: var(--radius-xs);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.header-breadcrumb {
  font-size: 14px;
}

.header-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-display);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.alarm-badge {
  cursor: pointer;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-xs);
  transition: all var(--transition-fast);
  color: var(--text-muted);
}

.alarm-badge:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 4px 4px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.user-info:hover {
  background: var(--bg-hover);
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%) !important;
  font-weight: 600;
  font-family: var(--font-display);
  font-size: 13px;
}

.user-detail {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.user-role-tag {
  font-size: 10px !important;
  height: 18px !important;
  padding: 0 5px !important;
  line-height: 18px !important;
}


/* ====== 移动端 Header 精简 ====== */
@media (max-width: 768px) {
  .header-breadcrumb {
    flex: 1;
    min-width: 0;
  }
  .header-breadcrumb :deep(.el-breadcrumb__inner) {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 40vw;
  }
  .user-name,
  .user-role-tag {
    display: none !important;
  }
  .header-right {
    gap: 8px;
    padding-right: 4px;
  }
  .app-header {
    padding: 0 12px;
  }
}

/* ====== 主内容区（中性背景，无噪声） ====== */
.app-main {
  background: var(--bg-main);
  padding: 20px;
  overflow-y: auto;
  position: relative;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.el-aside::-webkit-scrollbar {
  display: none;
}
</style>
