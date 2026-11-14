export interface Department {
  id: string
  name: string
  enabled: boolean
  brandOnly: boolean
  sortOrder: number
}

export interface DepartmentDto {
  _id: string
  name: string
  enabled: boolean
  brandOnly: boolean
  sortOrder: number
  extraTools?: unknown[]
  __v?: number
}

export interface Agent {
  id: string
  name: string
  description: string
  avatar: string | null
  category: string
  department: string | null
  isPreset: boolean
  starters: string[]
  instructions: string
  createdAt: string
}

export interface AgentDto {
  _id: string
  agent_id: string
  name: string
  description: string | null
  avatar: string | null
  category: string
  department: string | null
  isPreset: boolean
  conversation_starters: string[] | null
  instructions: string | null
  createdAt: string
}

export interface AgentListResponse {
  object: string
  data: AgentDto[]
  first_id: string | null
  last_id: string | null
  has_more: boolean
  after: string | null
}
