/**
 * Vue Router 路由配置
 *
 * 三种角色对应的路由视图：
 *   ADMIN    - 完整后台管理（设备/告警/能耗/维护/用户/营收/系统设置）
 *   OPERATOR - 运维人员（系统首页/告警处理/我的任务/通知中心）
 *   USER     - 普通用户/船东（充电桩首页/设备列表/预约/船舶/个人中心）
 *
 * RBAC 路由守卫（beforeEach）：
 *   1. 未登录 → 重定向到 /login
 *   2. 已登录访问 /login → 跳转到角色首页
 *   3. 路由配置了 meta.roles 但当前角色不在允许列表中 → 跳转角色首页
 *   4. 其余情况正常放行
 */
import { createRouter, createWebHistory } from 'vue-router'

/** 安全解析 sessionStorage 中的用户信息，损坏 JSON 时返回空对象 */
function safeUserInfo() {
  try {
    const raw = sessionStorage.getItem('userInfo')
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: () => {
      const userInfo = safeUserInfo()
      if (userInfo.role === 'ADMIN') return '/dashboard'
      if (userInfo.role === 'OPERATOR') return '/dashboard'
      return '/home'
    },
    children: [
      { path: 'dashboard',           component: () => import('../views/device/monitor.vue'), meta: { roles: ['ADMIN','OPERATOR'], title: '系统首页' } },
      { path: 'home',                component: () => import('../views/user/dashboard.vue'), meta: { title: '充电桩预约' } },
      { path: 'device',              component: () => import('../views/device/index.vue'), meta: { roles: ['ADMIN'], title: '设备管理' } },
      { path: 'alarm',               component: () => import('../views/alarm/index.vue'), meta: { roles: ['ADMIN','OPERATOR'], title: '故障预警' } },
      { path: 'energy',              component: () => import('../views/energy/index.vue'), meta: { roles: ['ADMIN'], title: '能耗中心' } },
      { path: 'maintenance',         component: () => import('../views/maintenance/index.vue'), meta: { roles: ['ADMIN','OPERATOR'], title: '维护调度' } },
      { path: 'user',                component: () => import('../views/user/index.vue'), meta: { roles: ['ADMIN'], title: '用户管理' } },
      { path: 'finance',             component: () => import('../views/finance/index.vue'), meta: { roles: ['ADMIN'], title: '营收统计' } },
      { path: 'system/config',       component: () => import('../views/system/config.vue'), meta: { roles: ['ADMIN'], title: '系统设置' } },
      { path: 'profile',             component: () => import('../views/profile/index.vue'), meta: { title: '个人中心' } },
      { path: 'user/devices',        component: () => import('../views/user/device-list.vue'), meta: { title: '充电桩列表' } },
      { path: 'user/device/:id',     component: () => import('../views/user/device-detail.vue'), meta: { title: '设备详情' } },
      { path: 'user/reservations',   component: () => import('../views/user/reservation.vue'), meta: { title: '我的预约' } },

      { path: 'user/ship',           component: () => import('../views/user/ship.vue'), meta: { title: '我的船舶' } },
      { path: 'operator/tasks',      component: () => import('../views/operator/tasks.vue'), meta: { roles: ['OPERATOR'], title: '我的任务' } },
      { path: 'operator/alarms',     component: () => import('../views/operator/alarms.vue'), meta: { roles: ['OPERATOR'], title: '告警处理' } },
      { path: 'operator/notifications', component: () => import('../views/operator/notifications.vue'), meta: { roles: ['OPERATOR'], title: '通知中心' } }
    ]
  },
  // 404 兜底：未匹配任意路由时展示错误页（未登录用户由守卫先送往 /login）
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  /** 切页回到顶部；浏览器前进/后退时恢复原滚动位置 */
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

/** 切页同步浏览器标签标题 */
router.afterEach(to => {
  const base = '长江岸电码头运维系统'
  document.title = to.meta.title ? `${to.meta.title} · ${base}` : base
})

/**
 * 路由前置守卫 —— RBAC 权限控制
 *
 * 守卫逻辑（顺序判断）：
 *   1. 未登录（无 Token）访问非登录页 → 强制跳转到 /login
 *   2. 已登录（有 Token）访问 /login → 跳转到角色首页
 *   3. 路由配置了 meta.roles（白名单）但当前角色不在白名单中 → 跳转角色首页
 *   4. 其余情况正常放行
 *
 * 角色首页映射：
 *   ADMIN    → /dashboard（实时监控大屏）
 *   OPERATOR → /dashboard（实时监控大屏）
 *   USER     → /home（充电桩预约首页）
 */
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  const userInfo = safeUserInfo()
  const role = userInfo.role || ''

  // 规则1：未登录访问非登录页 → 去登录
  if (to.path !== '/login' && !token) {
    next('/login')
  // 规则2：已登录访问登录页 → 去角色首页
  } else if (to.path === '/login' && token) {
    next('/')
  // 规则3：路由配置了角色白名单，但当前角色不在其中 → 跳转角色首页
  } else if (to.meta.roles && !to.meta.roles.includes(role)) {
    if (role === 'ADMIN') next('/dashboard')
    else if (role === 'OPERATOR') next('/operator/tasks')
    else next('/home')
  // 规则4：正常放行
  } else {
    next()
  }
})

export default router
