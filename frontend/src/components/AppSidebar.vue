<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="brand">
      <span class="brand-logo">🏔</span>
      <span v-if="!collapsed" class="brand-text">珠峰AI超级系统</span>
    </div>
    <nav class="nav">
      <router-link to="/" class="nav-item" :class="{ active: isHome && !deptFilter }">
        <span class="nav-icon">🏠</span><span v-if="!collapsed">首页</span>
      </router-link>

      <template v-for="d in departments" :key="d._id">
        <router-link
          :to="{ path: '/', query: { department: d.name } }"
          class="nav-item"
          :class="{ active: isHome && deptFilter === d.name }"
        >
          <span class="nav-icon">📁</span><span v-if="!collapsed">{{ d.name }}</span>
        </router-link>
      </template>

      <router-link to="/" class="nav-item" :class="{ active: isHome && deptFilter === '__all__' }"
        @click.prevent="goAgentCenter">
        <span class="nav-icon">🤖</span><span v-if="!collapsed">智能体中心</span>
      </router-link>
      <router-link to="/inspiration" class="nav-item" :class="{ active: route.path === '/inspiration' }">
        <span class="nav-icon">💡</span><span v-if="!collapsed">灵感中心</span>
      </router-link>
      <div class="nav-item disabled">
        <span class="nav-icon">🤝</span><span v-if="!collapsed">协作项目</span>
        <span v-if="!collapsed" class="soon">敬请期待</span>
      </div>
    </nav>
    <div class="sidebar-footer">
      <button class="nav-item" @click="collapsed = !collapsed">
        <span class="nav-icon">{{ collapsed ? '»' : '«' }}</span>
        <span v-if="!collapsed">关闭侧边栏</span>
      </button>
      <button class="nav-item logout" @click="onLogout">
        <span class="nav-icon">⏻</span><span v-if="!collapsed">退出登录</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { DepartmentVo } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const departments = ref<DepartmentVo[]>([])
const collapsed = ref(false)

const isHome = computed(() => route.path === '/')
const deptFilter = computed(() => (route.query.department as string) || '')

function goAgentCenter() {
  router.push({ path: '/', query: { department: '__all__' } })
}

function onLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  try {
    departments.value = await api.get<DepartmentVo[]>('/api/departments')
  } catch { /* ignore */ }
})
</script>

<style scoped>
.sidebar {
  width: 230px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--panel);
  border-right: 1px solid var(--border);
  transition: width 0.15s;
}
.sidebar.collapsed { width: 52px; }
.brand {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 12px;
  font-weight: 700;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
  overflow: hidden;
}
.brand-logo { font-size: 18px; }
.nav { flex: 1; overflow-y: auto; padding: 8px; display: flex; flex-direction: column; gap: 2px; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  color: var(--text-dim);
  white-space: nowrap;
  overflow: hidden;
}
.nav-item:hover { background: var(--panel-2); color: var(--text); }
.nav-item.active { background: var(--panel-2); color: var(--text); font-weight: 600; }
.nav-item.disabled { opacity: 0.45; cursor: not-allowed; }
.nav-icon { width: 18px; text-align: center; flex-shrink: 0; }
.soon { margin-left: auto; font-size: 11px; color: var(--text-faint); }
.sidebar-footer { padding: 8px; border-top: 1px solid var(--border); }
.logout:hover { color: var(--danger); }
</style>
