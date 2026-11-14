export interface User {
  id: string
  brand: string
  email: string
  name: string
  avatar: string | null
  emailVerified: boolean
  disabled: boolean
  contractReadAt: string | null
  createdAt: string
}

export interface UserDto {
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

export interface Balance {
  tokenCredits: number
  autoRefillEnabled: boolean
  expireAt: string | null
  user: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface AuthResponseDto {
  token: string
  user: UserDto
}
