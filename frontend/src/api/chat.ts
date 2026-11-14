import type { ChatStreamPayload } from '../types/chat'
import { streamSse } from '../utils/sse'

export interface ChatStreamHandlers {
  onConversation: (data: { conversationId: string; title?: string | null }) => void
  onMessage: (data: { messageId: string; conversationId: string; text: string }) => void
  onFinal: (data: { messageId: string; text: string }) => void
  onError: (message: string) => void
}

export async function streamChat(
  payload: ChatStreamPayload,
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  await streamSse(
    '/api/agents/chat/agents',
    payload,
    {
      onEvent: (event, data) => {
        if (event === 'conversation') {
          const value = data as { conversationId?: string; title?: string | null }
          if (value.conversationId) {
            handlers.onConversation({
              conversationId: value.conversationId,
              title: value.title,
            })
          }
          return
        }

        if (event === 'message') {
          const value = data as {
            messageId?: string
            conversationId?: string
            text?: string
          }
          if (value.messageId && value.conversationId && typeof value.text === 'string') {
            handlers.onMessage({
              messageId: value.messageId,
              conversationId: value.conversationId,
              text: value.text,
            })
          }
          return
        }

        if (event === 'final') {
          const value = data as { messageId?: string; text?: string }
          if (value.messageId && typeof value.text === 'string') {
            handlers.onFinal({ messageId: value.messageId, text: value.text })
          }
          return
        }

        if (event === 'error') {
          const value = data as { message?: string }
          handlers.onError(value.message || '生成失败')
        }
      },
    },
    signal,
  )
}
