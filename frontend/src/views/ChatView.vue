<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { fetchAgent, fetchAgents } from '../api/agents'
import { streamChat } from '../api/chat'
import { deleteConversation, fetchConversations, fetchMessages } from '../api/conversations'
import { Close, Plus } from '@element-plus/icons-vue'

import AppHeader from '../components/dashboard/AppHeader.vue'
import AppSidebar from '../components/dashboard/AppSidebar.vue'
import { agentTile, departmentRanks } from '../constants/nav'
import { useAuthStore } from '../stores/auth'
import type { Agent } from '../types/agent'
import type { ChatMessage, Conversation } from '../types/chat'

const route = useRoute()
const auth = useAuthStore()

const agent = ref<Agent | null>(null)
// 欢迎区图标要和智能体卡片一致，所以也要拿到该智能体在部门内的序号
const agentRank = ref<number | undefined>(undefined)
const welcomeTile = computed(() =>
  agentTile(agent.value?.name ?? '', agent.value?.department, agentRank.value),
)
const conversations = ref<Conversation[]>([])
const currentConversationId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const input = ref('')
const sending = ref(false)
const loading = ref(false)
const error = ref('')
const messagesEl = ref<HTMLElement | null>(null)

let controller: AbortController | null = null

const agentId = computed(() => String(route.params.agentId ?? ''))

function uuid(): string {
  return crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function scrollBottom(): Promise<void> {
  await nextTick()
  const element = messagesEl.value
  if (element) {
    element.scrollTop = element.scrollHeight
  }
}

async function loadAgent(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    agent.value = await fetchAgent(agentId.value)
    await Promise.all([loadConversations(), loadDepartmentRank()])
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载智能体失败'
  } finally {
    loading.value = false
  }
}

async function loadDepartmentRank(): Promise<void> {
  const department = agent.value?.department
  if (!department) return

  try {
    // 与部门页取同一份成员列表来定位序号，图标才能和卡片一致；失败就退回按名字哈希
    const peers = await fetchAgents({ department, isPreset: true })
    agentRank.value = departmentRanks(peers).get(agent.value?.id ?? '')
  } catch {
    // 图标序号拿不到不影响对话
  }
}

async function loadConversations(): Promise<void> {
  if (!agent.value) return
  try {
    conversations.value = await fetchConversations(agent.value.id)
  } catch {
    // 会话列表失败不阻塞对话
  }
}

async function selectConversation(conversation: Conversation): Promise<void> {
  if (sending.value) return

  currentConversationId.value = conversation.id
  try {
    const list = await fetchMessages(conversation.id)
    messages.value = list.map((message) => ({
      id: message.id,
      sender: message.sender,
      text: message.text,
      error: message.error,
    }))
    await scrollBottom()
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '加载消息失败'
  }
}

function newChat(): void {
  if (sending.value) return
  currentConversationId.value = null
  messages.value = []
  error.value = ''
}

async function removeConversation(conversation: Conversation): Promise<void> {
  try {
    await deleteConversation(conversation.id)
    conversations.value = conversations.value.filter((item) => item.id !== conversation.id)
    if (currentConversationId.value === conversation.id) {
      newChat()
    }
  } catch (removeError) {
    error.value = removeError instanceof Error ? removeError.message : '删除会话失败'
  }
}

async function send(preset?: string): Promise<void> {
  const text = (preset ?? input.value).trim()
  if (!text || sending.value || !agent.value) return

  input.value = ''
  error.value = ''
  sending.value = true

  const userMessage: ChatMessage = {
    id: uuid(),
    sender: 'user',
    text,
    error: false,
  }
  const assistant = reactive<ChatMessage>({
    id: uuid(),
    sender: 'agent',
    text: '',
    error: false,
    streaming: true,
  })
  messages.value.push(userMessage, assistant)
  await scrollBottom()

  controller = new AbortController()

  try {
    await streamChat(
      {
        agent_id: agent.value.id,
        text,
        conversationId: currentConversationId.value ?? undefined,
        messageId: userMessage.id,
      },
      {
        onConversation: (data) => {
          currentConversationId.value = data.conversationId
          void loadConversations()
        },
        onMessage: (data) => {
          assistant.text += data.text
          void scrollBottom()
        },
        onFinal: (data) => {
          assistant.id = data.messageId
          assistant.text = data.text
          assistant.streaming = false
          void loadConversations()
          auth.loadBalance()
        },
        onError: (message) => {
          assistant.streaming = false
          assistant.error = true
          assistant.text = assistant.text || message
          error.value = message
        },
      },
      controller.signal,
    )
  } catch (sendError) {
    assistant.streaming = false
    if (sendError instanceof DOMException && sendError.name === 'AbortError') {
      return
    }
    assistant.error = true
    assistant.text = assistant.text || (sendError instanceof Error ? sendError.message : '请求失败')
    error.value = sendError instanceof Error ? sendError.message : '请求失败'
  } finally {
    assistant.streaming = false
    sending.value = false
    controller = null
    await scrollBottom()
  }
}

function stop(): void {
  controller?.abort()
  controller = null
}

function onKeydown(event: KeyboardEvent): void {
  if (event.isComposing) return
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void send()
  }
}

watch(
  agentId,
  async () => {
    controller?.abort()
    controller = null
    sending.value = false
    agent.value = null
    conversations.value = []
    currentConversationId.value = null
    messages.value = []
    error.value = ''
    await loadAgent()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  controller?.abort()
})
</script>

<template>
  <div class="page">
    <AppSidebar />

    <div class="main">
      <AppHeader title="对话" />

      <div class="chat-layout">
        <aside class="conv-panel">
          <button type="button" class="btn btn-primary new-chat" @click="newChat">
            <span class="app-icon new-chat-icon"><Plus /></span>
            新对话
          </button>

          <div class="conv-list">
            <button
              v-for="conversation in conversations"
              :key="conversation.id"
              type="button"
              class="conv-item"
              :class="{ active: conversation.id === currentConversationId }"
              @click="selectConversation(conversation)"
            >
              <span class="conv-title">{{ conversation.title || '新对话' }}</span>
              <span class="conv-del app-icon" @click.stop="removeConversation(conversation)">
                <Close />
              </span>
            </button>

            <div v-if="conversations.length === 0" class="empty-text">暂无历史对话</div>
          </div>
        </aside>

        <section class="chat-main">
          <div ref="messagesEl" class="messages">
            <div v-if="loading" class="empty-state">加载中…</div>

            <div v-else-if="messages.length === 0 && agent" class="welcome">
              <div class="welcome-logo icon-tile" :style="{ '--tone': welcomeTile.tone }">
                <component :is="welcomeTile.icon" />
              </div>
              <h2>{{ agent.name }}</h2>
              <p class="sub">{{ agent.description || '开始一段新的对话' }}</p>

              <div v-if="agent.starters.length" class="starters">
                <button
                  v-for="(starter, index) in agent.starters"
                  :key="index"
                  type="button"
                  class="starter card"
                  :disabled="sending"
                  @click="send(starter)"
                >
                  {{ starter }}
                </button>
              </div>
            </div>

            <div
              v-for="message in messages"
              :key="message.id"
              class="msg-row"
              :class="message.sender"
            >
              <div class="msg-bubble" :class="{ error: message.error }">
                <div class="msg-sender">
                  {{ message.sender === 'user' ? '我' : agent?.name || '智能体' }}
                </div>
                <div class="msg-text text">
                  {{ message.text }}<span v-if="message.streaming" class="cursor">▍</span>
                </div>
              </div>
            </div>
          </div>

          <div class="composer">
            <p v-if="error" class="error-text composer-error">{{ error }}</p>

            <div class="composer-row">
              <textarea
                v-model="input"
                rows="1"
                placeholder="给智能体发消息…（Enter 发送，Shift+Enter 换行）"
                :disabled="sending"
                @keydown="onKeydown"
              ></textarea>

              <button v-if="sending" type="button" class="btn send" @click="stop">停止</button>
              <button
                v-else
                type="button"
                class="btn btn-primary send"
                :disabled="!input.trim()"
                @click="send()"
              >
                发送
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-layout {
  display: flex;
  flex: 1;
  min-height: 0;
}

.conv-panel {
  display: flex;
  flex: 0 0 230px;
  flex-direction: column;
  gap: 10px;
  width: 230px;
  padding: 10px;
  border-right: 1px solid var(--border);
  background: var(--panel);
}

.new-chat {
  width: 100%;
}

.new-chat-icon {
  font-size: 13px;
}

.conv-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  color: var(--text-dim);
  font-size: 13px;
  text-align: left;
}

.conv-item:hover {
  background: var(--panel-2);
}

.conv-item.active {
  background: var(--panel-2);
  color: var(--text);
  font-weight: 600;
}

.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-del {
  font-size: 13px;
  opacity: 0;
}

.conv-item:hover .conv-del {
  opacity: 0.6;
}

.conv-del:hover {
  color: var(--danger);
  opacity: 1;
}

.empty-text {
  padding: 12px;
  color: var(--text-faint);
  font-size: 13px;
  text-align: center;
}

.chat-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  background: var(--bg);
}

.messages {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}

.welcome {
  max-width: 640px;
  margin: 48px auto;
  text-align: center;
}

.welcome-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  border-radius: 18px;
  font-size: 30px;
}

.welcome h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.starters {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 22px;
}

.starter {
  padding: 12px 16px;
  color: var(--text-dim);
  text-align: left;
}

.starter:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.msg-row {
  display: flex;
  margin-bottom: 14px;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.agent {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: 14px;
}

.msg-row.user .msg-bubble {
  border-bottom-right-radius: 4px;
  background: var(--accent);
  color: #fff;
}

.msg-row.agent .msg-bubble {
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  background: var(--panel);
}

.msg-bubble.error {
  border-color: var(--danger);
}

.msg-sender {
  margin-bottom: 4px;
  font-size: 11px;
  opacity: 0.65;
}

.msg-text {
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.6;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.composer {
  padding: 12px 24px 18px;
  border-top: 1px solid var(--border);
  background: var(--panel);
}

.composer-error {
  margin: 0 0 6px;
  font-size: 13px;
}

.composer-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.composer-row textarea {
  flex: 1;
  min-width: 0;
  max-height: 160px;
  resize: none;
}

.send {
  height: 40px;
}

@media (max-width: 820px) {
  .conv-panel {
    flex-basis: 180px;
    width: 180px;
  }
}
</style>
