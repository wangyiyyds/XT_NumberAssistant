import type {
  Conversation,
  ConversationDto,
  Message,
  MessageDto,
} from '../types/chat'
import { request } from '../utils/request'

function toConversation(dto: ConversationDto): Conversation {
  return {
    id: dto.conversationId || dto._id,
    title: dto.title,
    endpoint: dto.endpoint,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  }
}

function toMessage(dto: MessageDto): Message {
  return {
    id: dto.messageId || dto._id,
    conversationId: dto.conversationId,
    parentMessageId: dto.parentMessageId,
    sender: dto.sender,
    text: dto.text,
    error: dto.error,
    createdAt: dto.createdAt,
  }
}

export async function fetchConversations(agentId?: string): Promise<Conversation[]> {
  const query = agentId ? `?agent_id=${encodeURIComponent(agentId)}` : ''
  const data = await request<{ conversations: ConversationDto[] }>(
    'GET',
    `/api/convos${query}`,
  )
  return data.conversations.map(toConversation)
}

export async function fetchMessages(conversationId: string): Promise<Message[]> {
  const data = await request<MessageDto[]>(
    'GET',
    `/api/convos/${conversationId}/messages`,
  )
  return data.map(toMessage)
}

export async function renameConversation(
  conversationId: string,
  title: string,
): Promise<Conversation> {
  const data = await request<ConversationDto>(
    'PATCH',
    `/api/convos/${conversationId}`,
    { title },
  )
  return toConversation(data)
}

export async function deleteConversation(conversationId: string): Promise<void> {
  await request<void>('DELETE', `/api/convos/${conversationId}`)
}
