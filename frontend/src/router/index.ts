import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue'), meta: { requiresAuth: true } },
      { path: 'prompt', name: 'prompt', component: () => import('../views/PromptView.vue'), meta: { requiresAuth: true } },
      { path: 'requirement', name: 'requirement', component: () => import('../views/RequirementView.vue'), meta: { requiresAuth: true } },
      { path: 'history', name: 'history', component: () => import('../views/HistoryView.vue'), meta: { requiresAuth: true } },
      { path: 'admin/config', name: 'adminConfig', component: () => import('../views/admin/ModelConfigView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/stats', name: 'adminStats', component: () => import('../views/admin/StatsView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
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
  if (to.meta.requiresAdmin) {
    if (!auth.user) await auth.fetchMe()
    if (!auth.isAdmin) return { name: 'home' }
  }
  return true
})

export default router
