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
    agents.value = await fetchAgents({ department: 'BOSS决策部门', isPreset: true })
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AgentGroupPage
    title="BOSS决策部门"
    description="商业参谋、数据分析与战略规划。"
    :agents="agents"
    :loading="loading"
    :error="error"
  />
</template>
