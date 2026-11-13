<template>
  <div class="home">
    <div class="home-head">
      <h2>{{ heading }}</h2>
      <p class="dim">选择一个智能体开始对话</p>
    </div>
    <div v-if="loading" class="dim">加载中…</div>
    <div v-else-if="agents.length === 0" class="dim">该部门暂无智能体</div>
    <div v-else class="grid">
      <button v-for="a in agents" :key="a.agent_id" class="agent-card card" @click="openChat(a)">
        <div class="agent-top">
          <span class="agent-avatar">🤖</span>
          <span class="agent-name">{{ a.name }}</span>
          <span v-if="a.department" class="agent-dept">{{ a.department }}</span>
        </div>
        <p class="agent-desc dim">{{ a.description }}</p>
        <div class="agent-foot">
          <span class="badge">{{ a.category }}</span>
          <span v-if="a.isPreset" class="badge preset">预置</span>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/client'
import type { AgentVo } from '../types'

interface AgentsPage { object: string; data: AgentVo[]; has_more: boolean }

const route = useRoute()
const router = useRouter()
const agents = ref<AgentVo[]>([])
const loading = ref(false)

const deptQuery = computed(() => (route.query.department as string) || '')
const heading = computed(() => {
  if (deptQuery.value === '__all__') return '智能体中心'
  if (deptQuery.value) return deptQuery.value
  return '首页 · 全部智能体'
})

async function load() {
  loading.value = true
  try {
    const dept = deptQuery.value
    const path = dept && dept !== '__all__'
      ? `/api/agents?department=${encodeURIComponent(dept)}&isPreset=1`
      : '/api/agents?requiredPermission=1'
    const res = await api.get<AgentsPage>(path)
    agents.value = res.data
  } catch {
    agents.value = []
  } finally {
    loading.value = false
  }
}

function openChat(a: AgentVo) {
  router.push(`/chat/${a.agent_id}`)
}

watch(deptQuery, load, { immediate: true })
</script>

<style scoped>
.home { flex: 1; overflow-y: auto; padding: 28px 32px; }
.home-head { margin-bottom: 20px; }
.home-head h2 { font-size: 20px; margin-bottom: 4px; }
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.agent-card {
  text-align: left;
  padding: 16px;
  display: flex; flex-direction: column; gap: 10px;
  transition: border-color 0.15s, transform 0.15s;
}
.agent-card:hover { border-color: var(--accent); transform: translateY(-2px); }
.agent-top { display: flex; align-items: center; gap: 8px; }
.agent-avatar { font-size: 20px; }
.agent-name { font-weight: 600; }
.agent-dept { margin-left: auto; font-size: 12px; color: var(--text-faint); }
.agent-desc {
  font-size: 13px; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}
.agent-foot { display: flex; gap: 6px; }
.badge {
  font-size: 11px; padding: 2px 8px; border-radius: 999px;
  background: var(--panel-2); color: var(--text-dim);
}
.badge.preset { background: rgba(16, 163, 127, 0.15); color: var(--accent); }
</style>
