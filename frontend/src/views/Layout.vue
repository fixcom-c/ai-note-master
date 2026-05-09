<template>
  <div class="layout-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-badge">AI</div>
        <div>
          <div class="brand-title">AI 行动管家</div>
          <div class="brand-subtitle">Capture. Focus. Deliver.</div>
        </div>
      </div>

      <nav class="nav">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          active-class="active"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <div>
            <span>{{ item.label }}</span>
            <small>{{ item.description }}</small>
          </div>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="profile-card">
          <div class="profile-avatar">{{ userInitial }}</div>
          <div>
            <div class="profile-name">{{ userStore.username || '当前用户' }}</div>
            <div class="profile-role">Personal AI OS</div>
          </div>
        </div>
        <el-button class="logout-btn" text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div>
          <p class="topbar-kicker">工作空间</p>
          <h1>{{ currentSection.title }}</h1>
        </div>
        <div class="topbar-meta">
          <div class="meta-pill">
            <span class="dot online"></span>
            {{ currentSection.subtitle }}
          </div>
        </div>
      </header>

      <section class="page-slot">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  HomeFilled,
  Edit,
  List,
  Reading,
  Document,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { to: '/', label: '首页', description: '总览与快速捕捉', icon: HomeFilled },
  { to: '/notes', label: '快速记录', description: '输入即沉淀', icon: Edit },
  { to: '/tasks', label: '任务列表', description: '推进每个待办', icon: List },
  { to: '/knowledge', label: '知识库', description: '沉淀你的资产', icon: Reading },
  { to: '/reports', label: '日报', description: '追踪每天产出', icon: Document }
]

const sectionMap: Record<string, { title: string; subtitle: string }> = {
  '/': { title: '智能工作台', subtitle: '今日任务与知识节奏' },
  '/notes': { title: '快速记录', subtitle: '把想法变成结构化行动' },
  '/tasks': { title: '任务列表', subtitle: '保持聚焦与推进' },
  '/knowledge': { title: '知识库', subtitle: '复用经验与洞察' },
  '/reports': { title: '智能日报', subtitle: '复盘完成与计划' }
}

const currentSection = computed(() => {
  const matchedKey = Object.keys(sectionMap).find((key) => route.path === key || route.path.startsWith(`${key}/`))
  return matchedKey ? sectionMap[matchedKey] : sectionMap['/']
})

const userInitial = computed(() => (userStore.username || 'AI').slice(0, 1).toUpperCase())

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  })
}
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  padding: 20px;
}

.sidebar,
.main-panel {
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(20px);
}

.sidebar {
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(17, 24, 39, 0.96) 0%, rgba(22, 34, 57, 0.92) 100%);
  color: rgba(255, 255, 255, 0.92);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-badge {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  font-weight: 700;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #1f6feb, #13b8a6);
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
  letter-spacing: 0.05em;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  color: rgba(255, 255, 255, 0.74);
  text-decoration: none;
  transition: transform 0.24s ease, background 0.24s ease, color 0.24s ease;
}

.nav-item small {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.nav-item:hover,
.nav-item.active {
  transform: translateX(4px);
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.18), rgba(19, 184, 166, 0.14));
  color: #fff;
}

.nav-item.active small,
.nav-item:hover small {
  color: rgba(255, 255, 255, 0.72);
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 28px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
}

.profile-avatar {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.14);
}

.profile-name {
  font-weight: 600;
}

.profile-role {
  margin-top: 3px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.56);
}

.logout-btn {
  justify-content: flex-start;
  color: rgba(255, 255, 255, 0.76);
}

.main-panel {
  min-width: 0;
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.56);
  overflow: hidden;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 12px;
}

.topbar-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--brand-primary);
}

.topbar h1 {
  font-size: 30px;
  line-height: 1.1;
}

.topbar-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-secondary);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.dot.online {
  background: #13b8a6;
  box-shadow: 0 0 0 6px rgba(19, 184, 166, 0.12);
}

.page-slot {
  height: calc(100vh - 108px);
  overflow-y: auto;
  padding: 8px 8px 24px;
}

@media (max-width: 1080px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .page-slot {
    height: auto;
    min-height: calc(100vh - 180px);
  }
}

@media (max-width: 720px) {
  .layout-shell {
    padding: 12px;
    gap: 12px;
  }

  .sidebar,
  .main-panel {
    border-radius: 22px;
  }

  .topbar {
    padding: 20px 18px 8px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
