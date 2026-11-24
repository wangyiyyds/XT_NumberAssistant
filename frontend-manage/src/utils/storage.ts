// 管理端 token 用独立 key，与客户端（xt-auth-token）同域部署时互不干扰
const TOKEN_KEY = 'xt-manage-token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
