import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: 'login.tabLogin' }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue'), meta: { requiresAuth: true, title: 'menu.home' } },
      { path: 'prompt', name: 'prompt', component: () => import('../views/PromptView.vue'), meta: { requiresAuth: true, title: 'menu.prompt' } },
      { path: 'requirement', name: 'requirement', component: () => import('../views/RequirementView.vue'), meta: { requiresAuth: true, title: 'menu.requirement' } },
      { path: 'history', name: 'history', component: () => import('../views/HistoryView.vue'), meta: { requiresAuth: true, title: 'menu.history' } },
      { path: 'admin/config', name: 'adminConfig', component: () => import('../views/admin/ModelConfigView.vue'), meta: { requiresAuth: true, requiresAdmin: true, title: 'menu.adminConfig' } },
      { path: 'admin/stats', name: 'adminStats', component: () => import('../views/admin/StatsView.vue'), meta: { requiresAuth: true, requiresAdmin: true, title: 'menu.adminStats' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // token 仍在但用户信息缺失（如浏览器恢复会话）时补拉一次
  if (to.meta.requiresAuth && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      /* 拉取失败不阻断导航，页面按未取到用户信息渲染 */
    }
  }
  if (to.meta.requiresAdmin) {
    if (!auth.user) await auth.fetchMe()
    if (!auth.isAdmin) return { name: 'home' }
  }
  return true
})

export default router
