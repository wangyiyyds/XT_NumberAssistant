import type { AuthResponse, AuthResponseDto, Balance, User, UserDto } from '../types/user'
import { request } from '../utils/request'

function toUser(dto: UserDto): User {
  return {
    id: dto._id,
    brand: dto.brand,
    email: dto.email,
    name: dto.name,
    avatar: dto.avatar,
    emailVerified: dto.emailVerified,
    disabled: dto.disabled,
    contractReadAt: dto.contractReadAt,
    createdAt: dto.createdAt,
  }
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const data = await request<AuthResponseDto>('POST', '/api/auth/login', {
    brand: 'zhufeng',
    email,
    password,
  })
  return { token: data.token, user: toUser(data.user) }
}

export async function register(
  email: string,
  password: string,
  name: string,
): Promise<AuthResponse> {
  const data = await request<AuthResponseDto>('POST', '/api/auth/register', {
    brand: 'zhufeng',
    email,
    password,
    name,
  })
  return { token: data.token, user: toUser(data.user) }
}

export async function fetchMe(): Promise<User> {
  const data = await request<UserDto>('GET', '/api/user')
  return toUser(data)
}

export async function fetchBalance(): Promise<Balance> {
  return request<Balance>('GET', '/api/balance')
}

export async function logout(): Promise<void> {
  await request<void>('POST', '/api/auth/logout')
}
