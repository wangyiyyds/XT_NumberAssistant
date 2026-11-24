<script setup lang="ts">
import { computed } from 'vue'

import { agentTile } from '../../constants/nav'
import type { Agent } from '../../types/agent'

const props = defineProps<{
  agent: Agent
  /** 部门内序号（列表页用 departmentRanks() 算好传入），保证同部门图标不重样 */
  rank?: number
}>()

const tile = computed(() => agentTile(props.agent.name, props.agent.department, props.rank))
const destination = computed(() =>
  props.agent.name === '文案提取助手'
    ? `/copywriting-extract/${props.agent.id}`
    : `/chat/${props.agent.id}`,
)
</script>

<template>
  <router-link class="agent-card card" :to="destination">
    <div class="agent-top">
      <span class="agent-avatar icon-tile" :style="{ '--tone': tile.tone }">
        <component :is="tile.icon" />
      </span>
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
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.agent-top {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.agent-avatar {
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  font-size: 20px;
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
