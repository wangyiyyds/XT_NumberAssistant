<template>
  <div class="insp">
    <div class="insp-head">
      <h2>灵感中心</h2>
      <p class="dim">添加关键词追踪热点视频，每个关键词最多50条，按点赞排序，可筛选低粉高赞。</p>
    </div>

    <div class="toolbar card">
      <div class="chips">
        <span v-for="(kw, i) in keywords" :key="kw._id" class="chip" :class="{ active: i === selected }"
          @click="selected = i">
          {{ kw.keyword }}
          <button class="chip-del" title="删除关键词" @click.stop="removeKeyword(kw)">✕</button>
        </span>
        <span v-if="keywords.length === 0" class="faint">还没有关键词，先在右侧添加一个吧</span>
      </div>
      <div class="add-row">
        <input v-model="newKeyword" placeholder="输入关键词" maxlength="30"
          @keydown.enter="addKeyword" />
        <button class="btn btn-primary btn-sm" :disabled="busy" @click="addKeyword">添加</button>
      </div>
    </div>

    <p class="cost faint">最多5项。手动更新：每个关键词60积分/次。</p>
    <p v-if="error" class="error-text">{{ error }}</p>

    <template v-if="current">
      <div class="list-head">
        <h3>热点视频 · {{ current.keyword }}</h3>
        <div class="list-actions">
          <button class="btn btn-sm" :class="{ 'filter-on': lowFansOnly }" @click="lowFansOnly = !lowFansOnly">
            低粉高赞
          </button>
          <button class="btn btn-sm" :disabled="busy" @click="refreshKeyword(current)">
            点此更新（60积分）
          </button>
        </div>
      </div>

      <div v-if="visibleVideos.length === 0" class="dim empty-videos">
        暂无视频数据，点击「点此更新」获取（演示环境将填充示例数据）。
      </div>
      <div v-else class="video-grid">
        <div v-for="v in visibleVideos" :key="v._id" class="video-card card">
          <div class="cover">
            <span class="duration">{{ v.duration }}</span>
            <span v-if="v.lowFansHighLikes" class="low-fans">低粉高赞</span>
          </div>
          <div class="video-body">
            <p class="video-title" :title="v.title">{{ v.title }}</p>
            <div class="video-meta">
              <span class="author">{{ v.author }}</span>
              <span class="faint">粉丝 {{ v.fans }}</span>
            </div>
            <div class="video-meta">
              <span class="faint">{{ v.publishDate }}</span>
              <span class="likes">❤ {{ v.likes }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { InspirationKeywordVo } from '../types'

const auth = useAuthStore()
const keywords = ref<InspirationKeywordVo[]>([])
const selected = ref(0)
const newKeyword = ref('')
const lowFansOnly = ref(false)
const busy = ref(false)
const error = ref('')

const current = computed(() => keywords.value[selected.value] || null)
const visibleVideos = computed(() => {
  if (!current.value) return []
  const vids = current.value.videos
  return lowFansOnly.value ? vids.filter((v) => v.lowFansHighLikes) : vids
})

async function load() {
  const res = await api.get<{ data: { keywords: InspirationKeywordVo[] } }>('/api/inspiration')
  keywords.value = res.data.keywords
  if (selected.value >= keywords.value.length) selected.value = 0
}

async function addKeyword() {
  const kw = newKeyword.value.trim()
  if (!kw || busy.value) return
  busy.value = true
  error.value = ''
  try {
    await api.post('/api/inspiration/keywords', { keyword: kw })
    newKeyword.value = ''
    await load()
    selected.value = keywords.value.length - 1
  } catch (e: any) {
    error.value = e?.message || '添加失败'
  } finally {
    busy.value = false
  }
}

async function removeKeyword(kw: InspirationKeywordVo) {
  busy.value = true
  error.value = ''
  try {
    await api.del(`/api/inspiration/keywords/${kw._id}`)
    await load()
  } catch (e: any) {
    error.value = e?.message || '删除失败'
  } finally {
    busy.value = false
  }
}

async function refreshKeyword(kw: InspirationKeywordVo) {
  busy.value = true
  error.value = ''
  try {
    await api.post(`/api/inspiration/keywords/${kw._id}/refresh`)
    await load()
    auth.loadBalance()
  } catch (e: any) {
    error.value = e?.message || '刷新失败'
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  load().catch((e) => (error.value = e?.message || '加载失败'))
})
</script>

<style scoped>
.insp { flex: 1; overflow-y: auto; padding: 28px 32px; }
.insp-head h2 { font-size: 20px; margin-bottom: 4px; }
.insp-head p { font-size: 13px; margin-bottom: 18px; }

.toolbar { padding: 14px; display: flex; flex-direction: column; gap: 12px; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 12px; border-radius: 999px;
  background: var(--panel-2); color: var(--text-dim);
  cursor: pointer; font-size: 13px;
}
.chip.active { background: var(--accent); color: #fff; }
.chip-del { font-size: 10px; opacity: 0.6; }
.chip-del:hover { opacity: 1; }
.add-row { display: flex; gap: 8px; }
.add-row input { flex: 1; max-width: 320px; }

.cost { margin: 10px 0 4px; font-size: 12px; }
.error-text { margin-bottom: 8px; font-size: 13px; }

.list-head {
  display: flex; align-items: center; justify-content: space-between;
  margin: 20px 0 12px;
}
.list-head h3 { font-size: 16px; }
.list-actions { display: flex; gap: 8px; }
.filter-on { background: var(--accent); color: #fff; }

.empty-videos { padding: 30px; text-align: center; }
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 14px;
}
.video-card { overflow: hidden; display: flex; flex-direction: column; }
.cover {
  height: 120px;
  background: linear-gradient(135deg, #2c3e50, #4ca1af);
  position: relative;
}
.duration {
  position: absolute; right: 8px; bottom: 8px;
  background: rgba(0, 0, 0, 0.7); color: #fff;
  font-size: 11px; padding: 2px 6px; border-radius: 4px;
}
.low-fans {
  position: absolute; left: 8px; top: 8px;
  background: var(--warn); color: #000;
  font-size: 11px; padding: 2px 8px; border-radius: 999px; font-weight: 600;
}
.video-body { padding: 10px 12px; display: flex; flex-direction: column; gap: 6px; }
.video-title {
  font-size: 13px; line-height: 1.45;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  min-height: 37px;
}
.video-meta { display: flex; justify-content: space-between; font-size: 12px; }
.author { color: var(--text-dim); }
.likes { color: #f43f5e; }
</style>
