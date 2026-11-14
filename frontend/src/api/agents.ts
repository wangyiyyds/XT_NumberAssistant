import type {
  Agent,
  AgentDto,
  AgentListResponse,
  Department,
  DepartmentDto,
} from '../types/agent'
import { request } from '../utils/request'

function toDepartment(dto: DepartmentDto): Department {
  return {
    id: dto._id,
    name: dto.name,
    enabled: dto.enabled,
    brandOnly: dto.brandOnly,
    sortOrder: dto.sortOrder,
  }
}

function toAgent(dto: AgentDto): Agent {
  return {
    id: dto.agent_id || dto._id,
    name: dto.name,
    description: dto.description ?? '',
    avatar: dto.avatar,
    category: dto.category,
    department: dto.department,
    isPreset: dto.isPreset,
    starters: dto.conversation_starters ?? [],
    instructions: dto.instructions ?? '',
    createdAt: dto.createdAt,
  }
}

export async function fetchDepartments(): Promise<Department[]> {
  const data = await request<DepartmentDto[]>('GET', '/api/departments')
  return data.map(toDepartment)
}

export interface FetchAgentsOptions {
  department?: string
  isPreset?: boolean
}

export async function fetchAgents(options: FetchAgentsOptions = {}): Promise<Agent[]> {
  const params = new URLSearchParams()
  if (options.department) params.set('department', options.department)
  if (options.isPreset) params.set('isPreset', '1')

  const query = params.toString()
  const data = await request<AgentListResponse>(
    'GET',
    `/api/agents${query ? `?${query}` : ''}`,
  )
  return data.data.map(toAgent)
}

export async function fetchAgent(publicId: string): Promise<Agent> {
  const data = await request<AgentDto>('GET', `/api/agents/${publicId}`)
  return toAgent(data)
}
