<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { Expand, Fold, SwitchButton } from '@element-plus/icons-vue'

import { resolveIcon } from '../../constants/icons'
import { useAuthStore } from '../../stores/auth'
import { useMenuStore } from '../../stores/menu'

const menu = useMenuStore()
const menus = computed(() => menu.menus)

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

onMounted(() => {
  if (!menu.loaded) void menu.load()
})

function isActive(path: string): boolean {
  // 子页面（如详情页 /agents/:id）保持父菜单高亮
  return route.path === path || route.path.startsWith(path + '/')
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
      <span class="brand-mark">先</span>
      <span v-if="!collapsed" class="brand-text">管理端</span>
    </div>

    <nav class="menu">
      <button
        v-for="item in menus"
        :key="item.path"
        type="button"
        class="menu-item"
        :class="{ active: isActive(item.path) }"
        :title="item.name"
        @click="go(item.path)"
      >
        <span class="menu-icon app-icon"><component :is="resolveIcon(item.icon)" /></span>
        <span v-if="!collapsed" class="menu-label">{{ item.name }}</span>
      </button>
    </nav>

    <div class="sidebar-foot">
      <button
        type="button"
        class="menu-item"
        :title="collapsed ? '展开侧栏' : '收起侧栏'"
        @click="collapsed = !collapsed"
      >
        <span class="menu-icon app-icon"><component :is="collapsed ? Expand : Fold" /></span>
        <span v-if="!collapsed" class="menu-label">收起侧栏</span>
      </button>
      <button type="button" class="menu-item danger" title="退出登录" @click="handleLogout">
        <span class="menu-icon app-icon"><SwitchButton /></span>
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
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.menu-item.danger:hover {
  color: var(--danger);
}

.menu-icon {
  flex: 0 0 18px;
  font-size: 17px;
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
