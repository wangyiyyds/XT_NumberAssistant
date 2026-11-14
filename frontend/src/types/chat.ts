export interface Conversation {
  id: string
  title: string | null
  endpoint: string
  createdAt: string
  updatedAt: string
}

export interface ConversationDto {
  _id: string
  conversationId: string
  title: string | null
  endpoint: string
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: string
  conversationId: string
  parentMessageId: string | null
  sender: 'user' | 'agent'
  text: string
  error: boolean
  createdAt: string
}

export interface MessageDto {
  _id: string
  messageId: string
  conversationId: string
  parentMessageId: string | null
  sender: 'user' | 'agent'
  isCreatedByUser: boolean
  text: string
  error: boolean
  createdAt: string
}

export interface ChatMessage {
  id: string
  sender: 'user' | 'agent'
  text: string
  error: boolean
  streaming?: boolean
}

export interface ChatStreamPayload {
  agent_id: string
  text: string
  conversationId?: string
  messageId?: string
  parentMessageId?: string
}
