<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ArrowLeft, Plus, Search, Timer } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchAgent } from '../api/agents'
import { streamChat } from '../api/chat'
import { deleteConversation, fetchConversations, fetchMessages } from '../api/conversations'
import type { Agent } from '../types/agent'
import type { Conversation, Message } from '../types/chat'
import AppHeader from '../components/dashboard/AppHeader.vue'
import AppSidebar from '../components/dashboard/AppSidebar.vue'

const route = useRoute()
const router = useRouter()
const agent = ref<Agent | null>(null)
const conversations = ref<Conversation[]>([])
const selectedId = ref<string | null>(null)
const messages = ref<Message[]>([])
const search = ref('')
const input = ref('')
const creating = ref(false)
const loading = ref(false)
const error = ref('')
let controller: AbortController | null = null

const agentId = computed(() => String(route.params.agentId ?? ''))
const selected = computed(() => conversations.value.find((item) => item.id === selectedId.value) ?? null)
const sourceMessage = computed(() => messages.value.find((item) => item.sender === 'user') ?? null)
const resultMessage = computed(() => [...messages.value].reverse().find((item) => item.sender === 'agent') ?? null)
const filteredConversations = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return keyword
    ? conversations.value.filter((item) => (item.title ?? '').toLowerCase().includes(keyword))
    : conversations.value
})

function uuid(): string {
  return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function displayTitle(conversation: Conversation): string {
  return conversation.title?.trim() || '未命名提取'
}

function statusOf(): '处理中' | '提取失败' | '提取完成' {
  if (creating.value) return '处理中'
  if (resultMessage.value?.error) return '提取失败'
  return resultMessage.value ? '提取完成' : '未完成'
}

function isFailure(): boolean {
  return Boolean(resultMessage.value?.error)
}

async function loadConversations(): Promise<void> {
  conversations.value = await fetchConversations(agentId.value)
}

async function selectConversation(conversation: Conversation): Promise<void> {
  if (creating.value) return
  selectedId.value = conversation.id
  error.value = ''
  loading.value = true
  try {
    messages.value = await fetchMessages(conversation.id)
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载提取详情失败'
  } finally {
    loading.value = false
  }
}

function newExtraction(): void {
  if (creating.value) return
  selectedId.value = null
  messages.value = []
  input.value = ''
  error.value = ''
  nextTick(() => document.querySelector<HTMLInputElement>('.extract-url-input')?.focus())
}

async function submit(): Promise<void> {
  const text = input.value.trim()
  if (!text || creating.value || !agent.value) return

  creating.value = true
  error.value = ''
  input.value = ''
  const userMessage: Message = {
    id: uuid(), conversationId: '', parentMessageId: null, sender: 'user', text, error: false, createdAt: new Date().toISOString(),
  }
  const assistantMessage: Message = {
    id: uuid(), conversationId: '', parentMessageId: null, sender: 'agent', text: '', error: false, createdAt: new Date().toISOString(),
  }
  messages.value = [userMessage, assistantMessage]
  controller = new AbortController()

  try {
    await streamChat(
      { agent_id: agent.value.id, text, messageId: userMessage.id },
      {
        onConversation: ({ conversationId }) => {
          selectedId.value = conversationId
          userMessage.conversationId = conversationId
          assistantMessage.conversationId = conversationId
          void loadConversations()
        },
        onMessage: ({ text: chunk }) => { assistantMessage.text += chunk },
        onFinal: ({ messageId, text: finalText }) => {
          assistantMessage.id = messageId
          assistantMessage.text = finalText
          void loadConversations()
        },
        onError: (message) => {
          assistantMessage.error = true
          assistantMessage.text = assistantMessage.text || message
          error.value = message
        },
      },
      controller.signal,
    )
  } catch (submitError) {
    if (!(submitError instanceof DOMException && submitError.name === 'AbortError')) {
      assistantMessage.error = true
      assistantMessage.text ||= submitError instanceof Error ? submitError.message : '提取请求失败'
      error.value = assistantMessage.text
    }
  } finally {
    creating.value = false
    controller = null
  }
}

async function retry(): Promise<void> {
  if (sourceMessage.value) {
    input.value = sourceMessage.value.text
    await submit()
  }
}

async function removeConversation(conversation: Conversation): Promise<void> {
  try {
    await deleteConversation(conversation.id)
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id)
    if (selectedId.value === conversation.id) newExtraction()
  } catch (removeError) {
    error.value = removeError instanceof Error ? removeError.message : '删除记录失败'
  }
}

watch(agentId, async () => {
  loading.value = true
  error.value = ''
  try {
    agent.value = await fetchAgent(agentId.value)
    await loadConversations()
    if (conversations.value[0]) await selectConversation(conversations.value[0])
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载文案提取失败'
  } finally {
    loading.value = false
  }
}, { immediate: true })

onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <div class="page">
    <AppSidebar />
    <div class="main">
      <AppHeader title="文案提取" />
      <main class="extract-page">
        <aside class="history-panel">
      <h2><span class="app-icon"><Timer /></span>历史记录</h2>
      <label class="history-search">
        <span class="app-icon"><Search /></span>
        <input v-model="search" type="search" placeholder="搜索文案或链接" />
      </label>

      <div class="history-list">
        <button
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="history-item"
          :class="{ active: conversation.id === selectedId }"
          type="button"
          @click="selectConversation(conversation)"
        >
          <span class="history-title">{{ displayTitle(conversation) }}</span>
          <span class="history-meta">抖音 · <em> {{ conversation.id === selectedId && creating ? '处理中' : '已保存' }}</em></span>
          <span class="history-delete" title="删除记录" @click.stop="removeConversation(conversation)">×</span>
        </button>
        <p v-if="!loading && filteredConversations.length === 0" class="history-empty">暂无提取记录</p>
      </div>
        </aside>

        <section class="extract-workspace">
      <header class="extract-header">
        <button class="back-button" type="button" title="返回文案编导组" @click="router.push('/copywriting')">
          <ArrowLeft />
        </button>
        <div>
          <h1>文案提取</h1>
          <p>提取抖音、小红书文案 · 仅支持5分钟以内的视频</p>
        </div>
        <button class="new-button" type="button" :disabled="creating" @click="newExtraction">
          <Plus /> 新建提取
        </button>
      </header>

      <div class="extract-content">
        <section v-if="!selectedId && !creating" class="new-extraction-card">
          <h2>新建提取</h2>
          <p>粘贴公开视频链接，系统将自动识别视频口播内容。</p>
          <div class="url-form">
            <input v-model="input" class="extract-url-input" placeholder="请输入抖音或小红书视频链接" @keydown.enter.prevent="submit" />
            <button class="new-button" type="button" :disabled="!input.trim()" @click="submit">开始提取</button>
          </div>
          <p v-if="error" class="form-error">{{ error }}</p>
        </section>

        <template v-else>
          <h2 class="details-heading">提取详情</h2>
          <section class="detail-card">
            <div class="detail-source">
              <div>
                <strong>抖音</strong>
                <p>{{ sourceMessage?.text || '正在提交视频链接…' }}</p>
              </div>
              <span class="extract-status" :class="{ failed: isFailure(), pending: creating }">{{ statusOf() }}</span>
            </div>

            <div class="detail-result">
              <template v-if="creating">
                <h3>正在提取文案</h3>
                <p>正在识别视频中的口播内容，请稍候…</p>
              </template>
              <template v-else-if="isFailure()">
                <h3 class="failure-title">这条文案未能提取</h3>
                <p>{{ resultMessage?.text || '提取失败，请检查链接后重试。' }}</p>
                <div class="result-actions">
                  <button class="outline-button" type="button" @click="retry">重新提取</button>
                  <button class="outline-button" type="button" @click="newExtraction">修改链接</button>
                </div>
              </template>
              <template v-else-if="resultMessage">
                <h3>提取结果</h3>
                <p class="copywriting-result">{{ resultMessage.text }}</p>
                <div class="result-actions">
                  <button class="outline-button" type="button" @click="navigator.clipboard?.writeText(resultMessage?.text || '')">复制文案</button>
                  <button class="outline-button" type="button" @click="newExtraction">新建提取</button>
                </div>
              </template>
              <template v-else>
                <h3>这条文案未能提取</h3>
                <p>{{ error || '当前记录没有可用的提取结果。' }}</p>
                <div class="result-actions"><button class="outline-button" type="button" @click="retry">重新提取</button></div>
              </template>
            </div>
          </section>
        </template>
      </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.extract-page { display: flex; flex: 1; min-height: 0; background: #f4f4f5; }
.history-panel { display: flex; flex: 0 0 230px; flex-direction: column; min-height: 0; border-right: 1px solid var(--border); background: var(--panel); padding: 16px 10px; }
.history-panel h2 { display: flex; align-items: center; gap: 8px; margin: 0 8px 14px; color: var(--text); font-size: 15px; font-weight: 600; }.history-panel h2 .app-icon { font-size: 17px; }
.history-search { display: flex; align-items: center; gap: 8px; padding: 0 10px; border: 1px solid var(--border-strong); border-radius: var(--radius); color: var(--text-dim); }.history-search input { width: 100%; height: 38px; padding: 0; border: 0; font-size: 13px; }.history-search input:focus { border: 0; }
.history-list { display: flex; flex-direction: column; gap: 2px; margin-top: 12px; overflow-y: auto; }.history-item { position: relative; padding: 10px 12px; border-left: 3px solid transparent; border-radius: 8px; text-align: left; }.history-item:hover, .history-item.active { background: var(--panel-2); }.history-item.active { border-left-color: var(--accent); }.history-title { display: block; overflow: hidden; color: var(--text); font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.history-meta { display: block; margin-top: 6px; color: var(--text-dim); font-size: 12px; }.history-meta em { color: var(--text-faint); font-style: normal; }.history-delete { position: absolute; top: 5px; right: 8px; display: none; color: var(--text-faint); font-size: 16px; }.history-item:hover .history-delete { display: block; }.history-empty { padding: 20px; color: var(--text-faint); font-size: 13px; text-align: center; }
.extract-workspace { display: flex; flex: 1; flex-direction: column; min-width: 0; background: var(--bg); }.extract-header { display: flex; align-items: center; gap: 12px; padding: 18px 24px; border-bottom: 1px solid var(--border); background: var(--panel); }.back-button { display: grid; width: 40px; height: 40px; place-items: center; border: 1px solid var(--border-strong); border-radius: var(--radius); color: var(--text); }.back-button svg, .new-button svg { width: 18px; height: 18px; }.extract-header h1 { margin: 0; color: var(--text); font-size: 19px; font-weight: 600; }.extract-header p { margin: 3px 0 0; color: var(--text-faint); font-size: 12px; }.new-button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; margin-left: auto; padding: 9px 16px; border-radius: var(--radius); background: var(--accent); color: #fff; font-size: 14px; font-weight: 500; }.new-button:disabled { opacity: .55; cursor: not-allowed; }
.extract-content { width: min(100%, 1120px); padding: 28px 48px; overflow-y: auto; }.details-heading { margin: 0 0 16px; color: var(--text); font-size: 16px; font-weight: 600; }.detail-card, .new-extraction-card { overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius); background: var(--panel); }.detail-source { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 16px 20px; background: var(--panel-2); }.detail-source strong { font-size: 14px; }.detail-source p { max-width: 780px; margin: 6px 0 0; overflow: hidden; color: var(--text-dim); font-size: 13px; line-height: 1.5; text-overflow: ellipsis; white-space: nowrap; }.extract-status { flex: 0 0 auto; color: var(--success); font-size: 13px; font-weight: 500; }.extract-status.failed { color: var(--danger); }.extract-status.pending { color: var(--warn); }.detail-result { min-height: 180px; padding: 26px 30px; }.detail-result h3 { margin: 0; color: var(--text); font-size: 18px; font-weight: 600; }.detail-result p { margin: 14px 0 0; color: var(--text-dim); font-size: 14px; line-height: 1.7; white-space: pre-wrap; }.detail-result .failure-title, .form-error { color: var(--danger); }.copywriting-result { max-width: 900px; }.result-actions { display: flex; gap: 10px; margin-top: 22px; }.outline-button { padding: 8px 14px; border: 1px solid var(--border-strong); border-radius: var(--radius); background: var(--panel); color: var(--text-dim); font-size: 14px; }.outline-button:hover { border-color: var(--accent); background: var(--accent-soft); color: var(--accent); }.new-extraction-card { padding: 28px; }.new-extraction-card h2 { margin: 0; color: var(--text); font-size: 18px; font-weight: 600; }.new-extraction-card > p { margin: 8px 0 18px; color: var(--text-dim); font-size: 14px; }.url-form { display: flex; gap: 10px; }.extract-url-input { flex: 1; height: 40px; font-size: 14px; }.url-form .new-button { margin-left: 0; padding-block: 8px; }.form-error { margin: 12px 0 0; font-size: 13px; }
@media (max-width: 900px) { .history-panel { flex-basis: 260px; }.extract-header { padding: 20px; }.extract-content { padding: 28px; }.extract-header h1 { font-size: 24px; }.history-title { font-size: 14px; }.detail-source p { white-space: normal; } }
</style>
