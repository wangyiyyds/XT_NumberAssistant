<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { fetchAgents } from '../api/agents'
import AgentCard from '../components/dashboard/AgentCard.vue'
import AppHeader from '../components/dashboard/AppHeader.vue'
import AppSidebar from '../components/dashboard/AppSidebar.vue'
import type { Agent } from '../types/agent'

const agents = ref<Agent[]>([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const notice = ref('')

const filteredAgents = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return agents.value
  return agents.value.filter((agent) => {
    return (
      agent.name.toLowerCase().includes(query) ||
      agent.description.toLowerCase().includes(query) ||
      agent.category.toLowerCase().includes(query)
    )
  })
})

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    agents.value = await fetchAgents()
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载智能体失败'
  } finally {
    loading.value = false
  }
}

function createAgent(): void {
  notice.value = '演示环境暂未开放创建智能体'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <AppSidebar />

    <div class="main">
      <AppHeader title="智能体中心" />

      <div class="content">
        <header class="page-head">
          <div>
            <h1>智能体中心</h1>
            <p class="sub">浏览全部智能体，点击卡片直接开启对话。</p>
          </div>

          <input v-model="keyword" class="search-input" placeholder="搜索智能体..." />
        </header>

        <p v-if="notice" class="notice">{{ notice }}</p>
        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="loading" class="empty-state">加载中…</p>

        <div class="agent-grid">
          <button type="button" class="create-card" @click="createAgent">
            <span class="create-plus">＋</span>
            <span>创建智能体</span>
          </button>

          <AgentCard v-for="agent in filteredAgents" :key="agent.id" :agent="agent" />
        </div>

        <div v-if="!loading && filteredAgents.length === 0" class="empty-state">
          {{ keyword ? '没有匹配的智能体' : '暂无智能体' }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-input {
  width: 260px;
}

.notice {
  margin: 0 0 10px;
  color: var(--warn);
  font-size: 13px;
}

.create-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 166px;
  padding: 16px;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-faint);
  font-size: 13px;
  transition: border-color 0.18s, color 0.18s, background 0.18s;
}

.create-card:hover {
  border-color: var(--accent);
  background: var(--panel);
  color: var(--accent);
}

.create-plus {
  font-size: 22px;
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>
