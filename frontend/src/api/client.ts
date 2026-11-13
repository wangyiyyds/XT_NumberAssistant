const TOKEN_KEY = 'zf-auth-token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

function authHeader(): Record<string, string> {
  const t = getToken()
  return t ? { Authorization: `Bearer ${t}` } : {}
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(path, {
    method,
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  if (res.status === 401) {
    setToken(null)
    if (!location.pathname.startsWith('/login')) location.href = '/login'
    throw new Error('未登录或登录已过期')
  }
  if (!res.ok) {
    let message = `请求失败 (${res.status})`
    try {
      const j = await res.json()
      if (j && j.message) message = j.message
    } catch { /* ignore */ }
    throw new Error(message)
  }
  return res.json() as Promise<T>
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body ?? {}),
  del: <T>(path: string) => request<T>('DELETE', path)
}

export interface SseHandlers {
  onEvent: (event: string, data: any) => void
  signal?: AbortSignal
}

/** POST chat request and consume the SSE stream via fetch ReadableStream. */
export async function sseChat(body: unknown, handlers: SseHandlers): Promise<void> {
  const res = await fetch('/api/agents/chat/agents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify(body),
    signal: handlers.signal
  })
  if (!res.ok || !res.body) {
    let message = `聊天请求失败 (${res.status})`
    try {
      const j = await res.json()
      if (j && j.message) message = j.message
    } catch { /* ignore */ }
    throw new Error(message)
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    let idx: number
    while ((idx = buf.indexOf('\n\n')) >= 0) {
      const frame = buf.slice(0, idx)
      buf = buf.slice(idx + 2)
      let event = 'message'
      const dataLines: string[] = []
      for (const line of frame.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      if (dataLines.length === 0) continue
      try {
        handlers.onEvent(event, JSON.parse(dataLines.join('')))
      } catch { /* ignore malformed frame */ }
    }
  }
}
