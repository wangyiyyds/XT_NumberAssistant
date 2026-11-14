<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  addKeyword,
  deleteKeyword,
  fetchInspiration,
  refreshKeyword,
} from '../api/inspiration'
import AppHeader from '../components/dashboard/AppHeader.vue'
import AppSidebar from '../components/dashboard/AppSidebar.vue'
import { useAuthStore } from '../stores/auth'
import type { InspirationKeyword } from '../types/inspiration'

const auth = useAuthStore()

const keywords = ref<InspirationKeyword[]>([])
const selected = ref(0)
const newKeyword = ref('')
const lowFansOnly = ref(false)
const busy = ref(false)
const loading = ref(false)
const error = ref('')

const current = computed(() => keywords.value[selected.value] ?? null)
const currentVideos = computed(() => current.value?.videos ?? [])
const visibleVideos = computed(() => {
  return lowFansOnly.value
    ? currentVideos.value.filter((video) => video.lowFansHighLikes)
    : currentVideos.value
})

async function load(): Promise<void> {
  loading.value = true
  try {
    keywords.value = await fetchInspiration()
    if (selected.value >= keywords.value.length) {
      selected.value = 0
    }
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载灵感数据失败'
  } finally {
    loading.value = false
  }
}

async function add(): Promise<void> {
  const keyword = newKeyword.value.trim()
  if (!keyword || busy.value) return
  if (keywords.value.length >= 5) {
    error.value = '最多只能添加 5 个关键词'
    return
  }

  busy.value = true
  error.value = ''
  try {
    await addKeyword(keyword)
    newKeyword.value = ''
    await load()
    selected.value = Math.max(0, keywords.value.length - 1)
  } catch (addError) {
    error.value = addError instanceof Error ? addError.message : '添加失败'
  } finally {
    busy.value = false
  }
}

async function remove(keyword: InspirationKeyword): Promise<void> {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await deleteKeyword(keyword.id)
    await load()
  } catch (removeError) {
    error.value = removeError instanceof Error ? removeError.message : '删除失败'
  } finally {
    busy.value = false
  }
}

async function refreshCurrent(): Promise<void> {
  const keyword = current.value
  if (!keyword || busy.value) return

  busy.value = true
  error.value = ''
  try {
    const updated = await refreshKeyword(keyword.id)
    const index = keywords.value.findIndex((item) => item.id === keyword.id)
    if (index >= 0) {
      keywords.value[index] = updated
    }
    auth.loadBalance()
  } catch (refreshError) {
    error.value = refreshError instanceof Error ? refreshError.message : '更新失败'
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <AppSidebar />

    <div class="main">
      <AppHeader title="灵感中心" />

      <div class="content">
        <header class="page-head">
          <div>
            <h1>灵感中心</h1>
            <p class="sub">添加关键词，追踪热点视频；手动更新 60 积分/次。</p>
          </div>
        </header>

        <section class="toolbar panel">
          <div class="chips">
            <button
              v-for="(keyword, index) in keywords"
              :key="keyword.id"
              type="button"
              class="keyword-chip"
              :class="{ active: selected === index }"
              @click="selected = index"
            >
              <span>{{ keyword.keyword }}</span>
              <span class="chip-close" @click.stop="remove(keyword)">✕</span>
            </button>
            <span v-if="keywords.length === 0" class="faint">还没有关键词，先添加一个</span>
          </div>

          <div class="add-row">
            <input
              v-model="newKeyword"
              class="add-input"
              placeholder="输入关键词，例如 Aigc培训"
              maxlength="30"
              @keyup.enter="add"
            />
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="busy || !newKeyword.trim() || keywords.length >= 5"
              @click="add"
            >
              添加
            </button>
          </div>

          <p class="hint">最多 5 个关键词；更新后 24 小时内缓存有效。</p>
        </section>

        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="loading" class="empty-state">加载中…</p>

        <template v-if="current">
          <header class="list-head">
            <div>
              <h2>热点视频 · {{ current.keyword }}</h2>
              <p class="sub">共 {{ currentVideos.length }} 条</p>
            </div>

            <div class="list-actions">
              <button
                type="button"
                class="btn btn-sm"
                :class="{ 'filter-on': lowFansOnly }"
                @click="lowFansOnly = !lowFansOnly"
              >
                低粉高赞
              </button>
              <button type="button" class="btn btn-sm" :disabled="busy" @click="refreshCurrent">
                点此更新（60 积分）
              </button>
            </div>
          </header>

          <div v-if="visibleVideos.length" class="video-grid">
            <article v-for="video in visibleVideos" :key="video.id" class="video-card card">
              <div class="cover">
                <span class="cover-icon">🎬</span>
                <span class="duration">{{ video.duration }}</span>
                <span v-if="video.lowFansHighLikes" class="low-fans">低粉高赞</span>
              </div>

              <div class="video-body">
                <p class="video-title" :title="video.title">{{ video.title }}</p>
                <div class="meta">
                  <span>{{ video.author }}</span>
                  <span>粉丝 {{ video.fans }}</span>
                </div>
                <div class="meta">
                  <span>{{ video.publishDate }}</span>
                  <span class="likes">赞 {{ video.likes }}</span>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="empty-state">
            {{ lowFansOnly ? '暂无低粉高赞视频' : '暂无视频，点击更新获取' }}
          </div>
        </template>

        <div v-else-if="!loading" class="empty-state">先添加一个关键词</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  padding: 14px;
  margin-bottom: 14px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 30px;
}

.keyword-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--panel-2);
  color: var(--text-dim);
  font-size: 13px;
}

.keyword-chip.active {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.chip-close {
  font-size: 11px;
  opacity: 0.65;
}

.chip-close:hover {
  opacity: 1;
}

.add-row {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.add-input {
  flex: 1;
  max-width: 320px;
}

.hint {
  margin: 10px 0 0;
  color: var(--text-faint);
  font-size: 12px;
}

.list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 20px 0 12px;
}

.list-head h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}

.list-actions {
  display: flex;
  gap: 8px;
}

.filter-on {
  background: var(--accent);
  color: #fff;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 14px;
}

.video-card {
  overflow: hidden;
  transition: border-color 0.18s, transform 0.18s;
}

.video-card:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
}

.cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 132px;
  background: #252a3d;
}

.cover-icon {
  font-size: 32px;
  opacity: 0.45;
}

.duration {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 11px;
}

.low-fans {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--warn);
  color: #1f2233;
  font-size: 11px;
  font-weight: 600;
}

.video-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px 12px;
}

.video-title {
  display: -webkit-box;
  min-height: 38px;
  margin: 0;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.meta {
  display: flex;
  justify-content: space-between;
  color: var(--text-faint);
  font-size: 12px;
}

.likes {
  color: #f43f5e;
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .list-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .add-row {
    flex-direction: column;
  }

  .add-input {
    max-width: none;
  }
}
</style>
