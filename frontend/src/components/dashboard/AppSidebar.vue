<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../../stores/auth'

interface MenuItem {
  label: string
  path: string
  icon: string
}

const menus: MenuItem[] = [
  { label: '首页', path: '/', icon: '🏠' },
  { label: '流量部门', path: '/traffic', icon: '📈' },
  { label: '视频部门', path: '/video', icon: '🎬' },
  { label: '文案编导组', path: '/copywriting', icon: '✍️' },
  { label: '直播增长组', path: '/live', icon: '📊' },
  { label: 'AI销售部门', path: '/sales', icon: '💰' },
  { label: '职能成本部门', path: '/cost', icon: '🧾' },
  { label: 'BOSS决策部门', path: '/boss', icon: '🏆' },
  { label: '智能体中心', path: '/agent-center', icon: '🤖' },
  { label: '灵感中心', path: '/inspiration', icon: '💡' },
]

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  if (path === '/agent-center') {
    return route.path === '/agent-center' || route.path.startsWith('/chat')
  }
  return route.path === path
}

function go(path: string): void {
  if (route.path !== path) {
    router.push(path)
  }
}

function handleLogout(): void {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="brand">
      <span class="brand-mark">珠</span>
      <span v-if="!collapsed" class="brand-text">珠峰AI</span>
    </div>

    <nav class="menu">
      <button
        v-for="item in menus"
        :key="item.path"
        type="button"
        class="menu-item"
        :class="{ active: isActive(item.path) }"
        :title="item.label"
        @click="go(item.path)"
      >
        <span class="menu-icon">{{ item.icon }}</span>
        <span v-if="!collapsed" class="menu-label">{{ item.label }}</span>
      </button>
    </nav>

    <div class="sidebar-foot">
      <button
        type="button"
        class="menu-item"
        :title="collapsed ? '展开侧栏' : '收起侧栏'"
        @click="collapsed = !collapsed"
      >
        <span class="menu-icon">{{ collapsed ? '»' : '«' }}</span>
        <span v-if="!collapsed" class="menu-label">收起侧栏</span>
      </button>
      <button type="button" class="menu-item danger" title="退出登录" @click="handleLogout">
        <span class="menu-icon">⏻</span>
        <span v-if="!collapsed" class="menu-label">退出登录</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex: 0 0 176px;
  flex-direction: column;
  width: 176px;
  height: 100vh;
  padding: 10px 8px 12px;
  overflow: hidden;
  border-right: 1px solid var(--border);
  background: var(--panel);
  transition: width 0.2s ease, flex-basis 0.2s ease;
}

.sidebar.collapsed {
  flex-basis: 62px;
  width: 62px;
  padding: 10px 6px 12px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 44px;
  padding: 0 8px 12px;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.brand-mark {
  display: inline-flex;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.brand-text {
  font-size: 15px;
  font-weight: 700;
}

.menu {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  padding: 10px 0;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-dim);
  font-size: 13px;
  text-align: left;
  white-space: nowrap;
}

.menu-item:hover {
  background: var(--panel-2);
  color: var(--text);
}

.menu-item.active {
  background: rgba(16, 163, 127, 0.12);
  color: var(--accent);
  font-weight: 600;
}

.menu-item.danger:hover {
  color: var(--danger);
}

.menu-icon {
  flex: 0 0 18px;
  font-size: 15px;
  text-align: center;
}

.menu-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-foot {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.collapsed .menu-item {
  justify-content: center;
  padding: 9px 0;
}
</style>
