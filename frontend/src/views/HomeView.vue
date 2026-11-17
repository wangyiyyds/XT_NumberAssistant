<script setup lang="ts">
import type { Component } from 'vue'
import { computed, onMounted, ref } from 'vue'

import {
  ArrowRight,
  ChatDotRound,
  Grid,
  Lightning,
  Monitor,
} from '@element-plus/icons-vue'

import { fetchAgents, fetchDepartments } from '../api/agents'
import { fetchConversations } from '../api/conversations'
import AgentCard from '../components/dashboard/AgentCard.vue'
import AppHeader from '../components/dashboard/AppHeader.vue'
import AppSidebar from '../components/dashboard/AppSidebar.vue'
import { QUICK_ENTRIES, TONE, departmentRanks } from '../constants/nav'
import { useAuthStore } from '../stores/auth'
import type { Agent, Department } from '../types/agent'
import type { Conversation } from '../types/chat'

interface Stat {
  label: string
  value: number
  unit: string
  icon: Component
  tone: string
}

const quickLinks = QUICK_ENTRIES

const auth = useAuthStore()

const agents = ref<Agent[]>([])
const departments = ref<Department[]>([])
const conversations = ref<Conversation[]>([])
const loading = ref(false)
const error = ref('')

const hotAgents = computed(() => agents.value.slice(0, 8))
// 序号按完整列表算，切片展示不影响同一个智能体的图标
const ranks = computed(() => departmentRanks(agents.value))
const stats = computed<Stat[]>(() => [
  {
    label: '智能体总数',
    value: agents.value.length,
    unit: '个',
    icon: Monitor,
    tone: TONE.blue,
  },
  {
    label: '部门数',
    value: departments.value.length,
    unit: '个',
    icon: Grid,
    tone: TONE.green,
  },
  {
    label: '积分余额',
    value: auth.balance?.tokenCredits ?? 0,
    unit: '',
    icon: Lightning,
    tone: TONE.amber,
  },
  {
    label: '会话数',
    value: conversations.value.length,
    unit: '个',
    icon: ChatDotRound,
    tone: TONE.red,
  },
])

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const [agentList, departmentList, conversationList] = await Promise.all([
      fetchAgents(),
      fetchDepartments(),
      fetchConversations(),
    ])
    agents.value = agentList
    departments.value = departmentList
    conversations.value = conversationList
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '首页数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!auth.loaded) {
    auth.load()
  }
  load()
})
</script>

<template>
  <div class="page">
    <AppSidebar />

    <div class="main">
      <AppHeader title="首页" />

      <div class="content">
        <section class="hero">
          <div>
            <p class="hero-kicker">AI 智能体工作台</p>
            <h1>先霆AI超级系统</h1>
            <p class="hero-desc">10 个业务页面，覆盖流量、视频、文案、直播、销售、职能与决策。</p>
          </div>
          <router-link class="btn btn-primary" to="/agent-center">进入智能体中心</router-link>
        </section>

        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="loading" class="empty-state">加载中…</p>

        <section class="stat-grid">
          <div v-for="item in stats" :key="item.label" class="stat-card">
            <span class="stat-icon icon-tile" :style="{ '--tone': item.tone }">
              <component :is="item.icon" />
            </span>
            <div>
              <p class="stat-label">{{ item.label }}</p>
              <p class="stat-value">
                {{ item.value.toLocaleString('zh-CN') }}
                <span v-if="item.unit" class="stat-unit">{{ item.unit }}</span>
              </p>
            </div>
          </div>
        </section>

        <div class="columns">
          <section class="panel">
            <header class="panel-head">
              <h2>常用智能体</h2>
              <router-link class="panel-more" to="/agent-center">
                全部
                <span class="app-icon more-icon"><ArrowRight /></span>
              </router-link>
            </header>

            <div class="agent-grid">
              <AgentCard
                v-for="agent in hotAgents"
                :key="agent.id"
                :agent="agent"
                :rank="ranks.get(agent.id)"
              />
            </div>

            <div v-if="!loading && hotAgents.length === 0" class="empty-state">暂无智能体</div>
          </section>

          <aside class="side">
            <section class="panel">
              <header class="panel-head">
                <h2>快捷入口</h2>
              </header>

              <div class="quick-grid">
                <router-link
                  v-for="item in quickLinks"
                  :key="item.path"
                  class="quick-item"
                  :to="item.path"
                >
                  <span class="app-icon quick-icon"><component :is="item.icon" /></span>
                  <span>{{ item.label }}</span>
                </router-link>
              </div>
            </section>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
  margin-bottom: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--hero-gradient);
}

.hero-kicker {
  margin: 0 0 4px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}

.hero h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.hero-desc {
  margin: 6px 0 0;
  color: var(--text-faint);
  font-size: 13px;
}

.quick-icon {
  font-size: 15px;
}

.panel-more {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.more-icon {
  font-size: 13px;
}

.columns {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.columns .panel {
  flex: 1;
  min-width: 0;
}

.side {
  flex: 0 0 300px;
}

@media (max-width: 1100px) {
  .columns {
    flex-direction: column;
  }

  .side {
    flex-basis: auto;
    width: 100%;
  }
}

@media (max-width: 680px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
