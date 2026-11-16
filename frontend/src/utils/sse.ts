import { brandData, brandText } from './brand'
import { getToken } from './storage'

export interface SseHandlers {
  onEvent: (event: string, data: unknown) => void
}

function dispatchFrame(frame: string, handlers: SseHandlers): void {
  let event = 'message'
  const dataLines: string[] = []

  for (const line of frame.split('\n')) {
    if (line.startsWith(':')) continue

    const colon = line.indexOf(':')
    const field = colon === -1 ? line : line.slice(0, colon)
    let value = colon === -1 ? '' : line.slice(colon + 1)
    if (value.startsWith(' ')) value = value.slice(1)

    if (field === 'event') {
      event = value
    } else if (field === 'data') {
      dataLines.push(value)
    }
  }

  if (dataLines.length === 0) return

  const raw = dataLines.join('\n')
  let data: unknown = raw
  try {
    data = JSON.parse(raw)
  } catch {
    // 保留原始文本
  }
  handlers.onEvent(event, brandData(data))
}

export async function streamSse(
  path: string,
  body: unknown,
  handlers: SseHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = getToken()
  const response = await fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
    signal,
  })

  if (!response.ok || !response.body) {
    let message = `请求失败 (${response.status})`
    try {
      const data = (await response.json()) as { message?: string }
      if (data?.message) message = brandText(data.message)
    } catch {
      // 非 JSON 错误体
    }
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')

    let index: number
    while ((index = buffer.indexOf('\n\n')) >= 0) {
      const frame = buffer.slice(0, index)
      buffer = buffer.slice(index + 2)
      dispatchFrame(frame, handlers)
    }
  }

  buffer += decoder.decode()
  buffer = buffer.replace(/\r\n/g, '\n')
  if (buffer.trim()) {
    dispatchFrame(buffer, handlers)
  }
}
