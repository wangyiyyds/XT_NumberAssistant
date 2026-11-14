<script setup lang="ts">
import { computed } from 'vue'

import type { Agent } from '../../types/agent'

const props = defineProps<{
  agent: Agent
}>()

const initial = computed(() => props.agent.name.slice(0, 1))
</script>

<template>
  <router-link class="agent-card card" :to="`/chat/${agent.id}`">
    <div class="agent-top">
      <span class="agent-avatar">{{ initial }}</span>
      <div class="agent-heading">
        <h3 class="agent-name">{{ agent.name }}</h3>
        <p v-if="agent.department" class="agent-dept">{{ agent.department }}</p>
      </div>
    </div>

    <p class="agent-desc">{{ agent.description || '暂无描述' }}</p>

    <div class="agent-tags">
      <span v-if="agent.category" class="tag">{{ agent.category }}</span>
      <span v-if="agent.isPreset" class="tag tag-accent">预置</span>
    </div>
  </router-link>
</template>

<style scoped>
.agent-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 166px;
  padding: 16px;
  transition: border-color 0.18s, transform 0.18s, box-shadow 0.18s;
}

.agent-card:hover {
  border-color: var(--accent);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.18);
  transform: translateY(-2px);
}

.agent-top {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.agent-avatar {
  display: inline-flex;
  flex: 0 0 42px;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: rgba(16, 163, 127, 0.15);
  color: var(--accent);
  font-size: 15px;
  font-weight: 600;
}

.agent-heading {
  min-width: 0;
}

.agent-name {
  margin: 0;
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-dept {
  margin: 2px 0 0;
  color: var(--text-faint);
  font-size: 11px;
}

.agent-desc {
  display: -webkit-box;
  flex: 1;
  margin: 0;
  overflow: hidden;
  color: var(--text-dim);
  font-size: 12px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.agent-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
