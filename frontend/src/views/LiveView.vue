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
    agents.value = await fetchAgents({ department: '直播增长组', isPreset: true })
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AgentGroupPage
    title="直播增长组"
    description="负责直播话术、直播间策划与增长。"
    :agents="agents"
    :loading="loading"
    :error="error"
  />
</template>
