<script setup lang="ts">
import { computed } from 'vue'

import type { Agent } from '../../types/agent'

import { departmentRanks } from '../../constants/nav'
import AgentCard from './AgentCard.vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    agents: Agent[]
    loading?: boolean
    error?: string
  }>(),
  {
    description: '',
    loading: false,
    error: '',
  },
)

// 这里传进来的就是该部门的全量列表，序号与首页 / 智能体中心算法一致
const ranks = computed(() => departmentRanks(props.agents))
</script>

<template>
  <div class="page">
    <AppSidebar />

    <div class="main">
      <AppHeader :title="title" />

      <div class="content">
        <header class="page-head">
          <div>
            <h1>{{ title }}</h1>
            <p class="sub">{{ description || `共 ${agents.length} 个智能体` }}</p>
          </div>
          <span class="tag">{{ agents.length }} 个</span>
        </header>

        <p v-if="error" class="error-text">{{ error }}</p>

        <div v-if="loading" class="empty-state">加载中…</div>
        <div v-else class="agent-grid">
          <AgentCard
            v-for="agent in agents"
            :key="agent.id"
            :agent="agent"
            :rank="ranks.get(agent.id)"
          />
        </div>

        <div v-if="!loading && !error && agents.length === 0" class="empty-state">
          该部门暂无智能体
        </div>
      </div>
    </div>
  </div>
</template>
