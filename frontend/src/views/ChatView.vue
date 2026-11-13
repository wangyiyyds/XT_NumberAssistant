<template>
  <div class="chat-page">
    <div class="conv-panel">
      <button class="btn btn-primary new-chat" @click="newChat">＋ 新对话</button>
      <div class="conv-list">
        <div v-for="c in conversations" :key="c.conversationId" class="conv-item"
          :class="{ active: c.conversationId === currentConversationId }" @click="selectConversation(c)">
          <span class="conv-title">{{ c.title || '新对话' }}</span>
          <button class="conv-del" title="删除对话" @click.stop="removeConversation(c)">✕</button>
        </div>
        <div v-if="conversations.length === 0" class="faint empty">暂无历史对话</div>
      </div>
    </div>

    <div class="chat-main">
      <div ref="scrollEl" class="messages">
        <template v-if="messages.length === 0 && agent">
          <div class="welcome">
            <div class="welcome-logo">🤖</div>
            <h2>{{ agent.name }}</h2>
            <p class="dim">{{ agent.description }}</p>
            <div v-if="agent.conversation_starters?.length" class="starters">
              <button v-for="(s, i) in agent.conversation_starters" :key="i" class="starter card"
                :disabled="sending" @click="send(s)">
                {{ s }}
              </button>
            </div>
          </div>
        </template>

        <div v-for="m in messages" :key="m.messageId" class="msg-row" :class="m.sender">
          <div class="msg-bubble" :class="{ error: m.error }">
            <div class="msg-sender">{{ m.sender === 'user' ? '我' : agent?.name || '智能体' }}</div>
            <div class="msg-text">{{ m.text }}<span v-if="m.streaming" class="cursor">▍</span></div>
          </div>
        </div>
      </div>

      <div class="composer">
        <p v-if="sendError" class="error-text composer-error">{{ sendError }}</p>
        <div class="composer-row">
          <textarea v-model="input" rows="1" placeholder="给智能体发消息…（Enter 发送，Shift+Enter 换行）"
            :disabled="sending" @keydown="onKeydown"></textarea>
          <button class="btn btn-primary send" :disabled="sending || !input.trim()" @click="send()">
            {{ sending ? '生成中…' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, sseChat } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { AgentVo, ConversationVo, MessageVo } from '../types'

interface ChatMessage {
  messageId: string
  sender: 'user' | 'agent'
  text: string
  error: boolean
  streaming?: boolean
}

const route = useRoute()
const auth = useAuthStore()

const agent = ref<AgentVo | null>(null)
const conversations = ref<ConversationVo[]>([])
const currentConversationId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const input = ref('')
const sending = ref(false)
const sendError = ref('')
const scrollEl = ref<HTMLElement | null>(null)

function uuid(): string {
  return crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random()
}

async function scrollBottom() {
  await nextTick()
  if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
}

async function loadConversations() {
  if (!agent.value) return
  const res = await api.get<{ conversations: ConversationVo[] }>(
    `/api/convos?agent_id=${encodeURIComponent(agent.value.agent_id)}`)
  conversations.value = res.conversations
}

async function selectConversation(c: ConversationVo) {
  currentConversationId.value = c.conversationId
  const list = await api.get<MessageVo[]>(`/api/convos/${c.conversationId}/messages`)
  messages.value = list.map((m) => ({
    messageId: m.messageId, sender: m.sender, text: m.text, error: m.error
  }))
  scrollBottom()
}

async function removeConversation(c: ConversationVo) {
  try {
    await api.del(`/api/convos/${c.conversationId}`)
    conversations.value = conversations.value.filter((x) => x.conversationId !== c.conversationId)
    if (currentConversationId.value === c.conversationId) newChat()
  } catch (e: any) {
    sendError.value = e?.message || '删除失败'
  }
}

function newChat() {
  currentConversationId.value = null
  messages.value = []
  sendError.value = ''
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

async function send(preset?: string) {
  const text = (preset ?? input.value).trim()
  if (!text || sending.value || !agent.value) return
  input.value = ''
  sendError.value = ''
  sending.value = true

  messages.value.push({ messageId: uuid(), sender: 'user', text, error: false })
  const assistant: ChatMessage = { messageId: uuid(), sender: 'agent', text: '', error: false, streaming: true }
  messages.value.push(assistant)
  scrollBottom()

  try {
    await sseChat(
      {
        agent_id: agent.value.agent_id,
        conversationId: currentConversationId.value,
        messageId: messages.value[messages.value.length - 2].messageId,
        text
      },
      {
        onEvent: (event, data) => {
          if (event === 'conversation') {
            currentConversationId.value = data.conversationId
            loadConversations()
          } else if (event === 'message') {
            assistant.text += data.text || ''
            scrollBottom()
          } else if (event === 'final') {
            assistant.streaming = false
            auth.loadBalance()
            loadConversations()
          } else if (event === 'error') {
            assistant.streaming = false
            assistant.error = true
            assistant.text = assistant.text || data.message || '生成失败'
            sendError.value = data.message || '生成失败'
          }
        }
      }
    )
  } catch (e: any) {
    assistant.streaming = false
    assistant.error = true
    assistant.text = assistant.text || e?.message || '请求失败'
    sendError.value = e?.message || '请求失败'
  } finally {
    assistant.streaming = false
    sending.value = false
    scrollBottom()
  }
}

watch(
  () => route.params.agentId,
  async (agentId) => {
    if (!agentId) return
    newChat()
    try {
      agent.value = await api.get<AgentVo>(`/api/agents/${agentId}`)
      await loadConversations()
    } catch (e: any) {
      sendError.value = e?.message || '加载智能体失败'
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.chat-page { flex: 1; display: flex; min-width: 0; }
.conv-panel {
  width: 230px; flex-shrink: 0;
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  padding: 10px;
}
.new-chat { width: 100%; margin-bottom: 10px; }
.conv-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; }
.conv-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 10px; border-radius: 8px;
  color: var(--text-dim); cursor: pointer;
}
.conv-item:hover { background: var(--panel-2); }
.conv-item.active { background: var(--panel-2); color: var(--text); }
.conv-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.conv-del { color: var(--text-faint); font-size: 11px; padding: 2px; border-radius: 4px; visibility: hidden; }
.conv-item:hover .conv-del { visibility: visible; }
.conv-del:hover { color: var(--danger); }
.empty { padding: 12px; font-size: 13px; text-align: center; }

.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.messages { flex: 1; overflow-y: auto; padding: 20px 24px; }

.welcome { max-width: 640px; margin: 40px auto; text-align: center; }
.welcome-logo { font-size: 42px; margin-bottom: 10px; }
.welcome h2 { margin-bottom: 6px; }
.starters { display: flex; flex-direction: column; gap: 8px; margin-top: 22px; }
.starter { padding: 12px 16px; text-align: left; color: var(--text-dim); }
.starter:hover { border-color: var(--accent); color: var(--text); }

.msg-row { display: flex; margin-bottom: 14px; }
.msg-row.user { justify-content: flex-end; }
.msg-row.agent { justify-content: flex-start; }
.msg-bubble { max-width: 76%; padding: 10px 14px; border-radius: 14px; }
.msg-row.user .msg-bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 4px; }
.msg-row.agent .msg-bubble { background: var(--panel); border: 1px solid var(--border); border-bottom-left-radius: 4px; }
.msg-bubble.error { border-color: var(--danger); }
.msg-sender { font-size: 11px; opacity: 0.65; margin-bottom: 4px; }
.msg-text { white-space: pre-wrap; word-break: break-word; line-height: 1.6; }
.cursor { animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0; } }

.composer { padding: 12px 24px 18px; }
.composer-error { font-size: 13px; margin-bottom: 6px; }
.composer-row { display: flex; gap: 10px; align-items: flex-end; }
.composer-row textarea { flex: 1; resize: none; max-height: 160px; line-height: 1.5; }
.send { height: 42px; }
</style>
