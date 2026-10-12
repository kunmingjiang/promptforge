<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BrandLogo from '../components/BrandLogo.vue'
import BrandFooter from '../components/BrandFooter.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/', label: '首页', icon: 'HomeFilled' },
  { path: '/prompt', label: '提示词生成', icon: 'MagicStick' },
  { path: '/requirement', label: '需求助手', icon: 'Document' },
  { path: '/history', label: '对话历史', icon: 'Clock' }
]

const adminMenus = [
  { path: '/admin/config', label: '模型配置', icon: 'Setting' },
  { path: '/admin/stats', label: '使用统计', icon: 'DataAnalysis' }
]

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="216px" class="aside">
      <BrandLogo />
      <div class="nav-label">工作台</div>
      <el-menu router :default-active="route.path" class="menu">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </el-menu-item>
      </el-menu>
      <template v-if="auth.isAdmin">
        <div class="nav-label">管理</div>
        <el-menu router :default-active="route.path" class="menu">
          <el-menu-item v-for="m in adminMenus" :key="m.path" :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.label }}</span>
          </el-menu-item>
        </el-menu>
      </template>
      <div class="aside-foot">
        <span class="status-dot"></span>
        <span class="mono-tag">SYSTEM ONLINE</span>
      </div>
    </el-aside>
    <el-container class="body-col">
      <el-header class="header" height="52px">
        <span class="crumb mono-tag">{{ route.meta?.title || 'PROMPTFORGE' }}</span>
        <el-dropdown>
          <span class="user-name">
            <span class="avatar">{{ (auth.user?.username || 'U').slice(0, 1).toUpperCase() }}</span>
            {{ auth.user?.username || '用户' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
      <BrandFooter />
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-right: 1px solid var(--line);
}

.nav-label {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--text-3);
  padding: 16px 20px 8px;
}

.menu {
  border-right: none;
  background: transparent;
  padding: 0 10px;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  color: var(--text-2);
  border-radius: var(--radius-sm);
  margin-bottom: 3px;
  font-size: 15.5px;
  transition: background-color 0.18s var(--ease), color 0.18s var(--ease);
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(37, 99, 235, 0.08);
  color: var(--text-1);
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.13);
  color: var(--interactive);
  font-weight: 600;
  box-shadow: inset 2px 0 0 var(--interactive);
}

.aside-foot {
  margin-top: auto;
  padding: 16px 20px;
  border-top: 1px solid var(--line);
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
}

.crumb {
  letter-spacing: 2px;
  font-size: 12.5px;
}

.user-name {
  cursor: pointer;
  color: var(--text-2);
  font-size: 15.5px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color 0.18s var(--ease);
}

.user-name:hover {
  color: var(--text-1);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.main {
  background: transparent;
  overflow-y: auto;
}
</style>
