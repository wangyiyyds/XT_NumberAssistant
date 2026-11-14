<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchAgents } from '../api/agents'
import AgentGroupPage from '../components/dashboard/AgentGroupPage.vue'
import type { Agent } from '../types/agent'

const agents = ref<Agent[]>([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  try {
    agents.value = await fetchAgents({ department: '流量部门', isPreset: true })
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AgentGroupPage
    title="流量部门"
    description="负责选题、热点追踪与流量型内容生产。"
    :agents="agents"
    :loading="loading"
    :error="error"
  />
</template>
