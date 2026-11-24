<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const title = computed(() => route.meta.title ?? '')
const adminName = computed(() => auth.admin?.name || auth.admin?.username || '管理员')
</script>

<template>
  <header class="header">
    <h1 class="title">{{ title }}</h1>
    <div class="who">
      <span class="avatar sm">{{ adminName.slice(0, 1) }}</span>
      <span class="name dim">{{ adminName }}</span>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
}

.title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.who {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar.sm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
}

.name {
  font-size: 13px;
}
</style>
