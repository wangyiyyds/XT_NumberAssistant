<script setup lang="ts">
import { computed, onMounted } from 'vue'

import { useAuthStore } from '../../stores/auth'

defineProps<{
  title?: string
}>()

const auth = useAuthStore()

const userInitial = computed(() => (auth.user?.name || '用').slice(0, 1))
const credits = computed(() =>
  auth.balance ? auth.balance.tokenCredits.toLocaleString('zh-CN') : '—',
)

onMounted(() => {
  if (!auth.loaded) {
    auth.load()
  }
})
</script>

<template>
  <header class="app-header">
    <div class="header-title">{{ title || '先霆AI超级系统' }}</div>

    <div class="header-right">
      <span class="balance" title="Token 积分余额">⚡ {{ credits }}</span>
      <button type="button" class="icon-btn" title="通知">🔔</button>
      <div class="user-chip">
        <span class="avatar">{{ userInitial }}</span>
        <span class="user-name">你好，{{ auth.user?.name || '用户' }}</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  flex: 0 0 56px;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 22px;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
}

.header-title {
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.balance {
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
}

.icon-btn {
  font-size: 14px;
  opacity: 0.8;
}

.icon-btn:hover {
  opacity: 1;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.user-name {
  color: var(--text-dim);
  font-size: 13px;
}

@media (max-width: 720px) {
  .user-name {
    display: none;
  }
}
</style>
