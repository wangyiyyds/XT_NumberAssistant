export interface UserVo {
  _id: string
  brand: string
  email: string
  name: string
  avatar: string | null
  emailVerified: boolean
  disabled: boolean
  contractReadAt: string | null
  createdAt: string
}

export interface BalanceVo {
  tokenCredits: number
  autoRefillEnabled: boolean
  expireAt: string | null
  user: string
}

export interface DepartmentVo {
  _id: string
  name: string
  enabled: boolean
  brandOnly: boolean
  sortOrder: number
  extraTools: unknown[]
}

export interface AgentVo {
  _id: string
  agent_id: string
  name: string
  description: string
  avatar: string | null
  category: string
  department: string | null
  isPreset: boolean
  conversation_starters: string[]
  instructions: string
  createdAt: string
}

export interface ConversationVo {
  _id: string
  conversationId: string
  title: string
  endpoint: string
  createdAt: string
  updatedAt: string
  agent_id?: string
}

export interface MessageVo {
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

export interface InspirationVideoVo {
  _id: number
  author: string
  fans: string
  duration: string
  publishDate: string
  title: string
  likes: string
  coverUrl: string | null
  videoUrl: string | null
  lowFansHighLikes: boolean
}

export interface InspirationKeywordVo {
  _id: number
  keyword: string
  order: number
  cacheExpiresAt: string | null
  videos: InspirationVideoVo[]
}
