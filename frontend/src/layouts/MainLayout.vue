<template>
  <div class="layout">
    <AppSidebar />
    <div class="main">
      <header class="header">
        <div class="header-title">{{ pageTitle }}</div>
        <div class="header-right">
          <span class="balance" title="Token 积分余额">⚡ {{ auth.balance?.tokenCredits ?? '—' }}</span>
          <button class="icon-btn" title="通知">🔔</button>
          <div class="user-chip" :title="auth.user?.email">
            <span class="avatar">{{ (auth.user?.name || '用')[0] }}</span>
            <span class="uname">你好！{{ auth.user?.name || '用户' }}</span>
          </div>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '../components/AppSidebar.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()

const pageTitle = computed(() => {
  if (route.path.startsWith('/chat')) return '对话'
  if (route.path.startsWith('/inspiration')) return '灵感中心'
  return '珠峰AI超级系统'
})

onMounted(() => {
  if (!auth.loaded) auth.load()
})
</script>

<style scoped>
.layout { display: flex; height: 100%; }
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.header {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
}
.header-title { font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; }
.balance { color: var(--warn); font-weight: 600; }
.icon-btn { font-size: 16px; padding: 4px; border-radius: 6px; }
.icon-btn:hover { background: var(--panel-2); }
.user-chip { display: flex; align-items: center; gap: 8px; }
.avatar {
  width: 28px; height: 28px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
}
.uname { color: var(--text-dim); }
.content { flex: 1; min-height: 0; display: flex; }
</style>
