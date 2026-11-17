<script setup lang="ts">
import { computed } from 'vue'

import AppHeader from '../components/dashboard/AppHeader.vue'
import AppSidebar from '../components/dashboard/AppSidebar.vue'
import { agentTile } from '../constants/nav'

// 流量部门目前只承载一个功能入口：灵感中心
const tile = computed(() => agentTile('灵感中心', '流量部门', 0))
</script>

<template>
  <div class="page">
    <AppSidebar />

    <div class="main">
      <AppHeader title="流量部门" />

      <div class="content">
        <header class="page-head">
          <div>
            <h1>流量部门</h1>
            <p class="sub">负责选题、热点追踪与流量型内容生产。</p>
          </div>
          <span class="tag">1 个</span>
        </header>

        <div class="agent-grid">
          <router-link class="agent-card card" to="/inspiration">
            <div class="agent-top">
              <span class="agent-avatar icon-tile" :style="{ '--tone': tile.tone }">
                <component :is="tile.icon" />
              </span>
              <div class="agent-heading">
                <h3 class="agent-name">灵感中心</h3>
                <p class="agent-dept">流量部门</p>
              </div>
            </div>

            <p class="agent-desc">按行业关键词挖掘抖音低粉高赞爆款视频，为选题与内容生产提供热点灵感。</p>

            <div class="agent-tags">
              <span class="tag">流量部门</span>
              <span class="tag tag-accent">预置</span>
            </div>
          </router-link>
        </div>
      </div>
    </div>
  </div>
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
