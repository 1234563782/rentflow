import type { ConversationCreated, MessageRun } from '@/types'
import { gearmateBaseUrl, TOKEN_KEY } from './http'

function headers(extra: HeadersInit = {}) {
  const token = sessionStorage.getItem(TOKEN_KEY)
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}`, ...extra }
}

async function jsonRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${gearmateBaseUrl}${path}`, { ...init, headers: headers(init.headers) })
  if (response.status === 401) window.dispatchEvent(new CustomEvent('rentflow:unauthorized'))
  if (!response.ok) throw new Error(`GearMate 请求失败 (${response.status})`)
  return response.json() as Promise<T>
}

export async function createConversation(timezone: string) {
  return jsonRequest<ConversationCreated>('/api/v1/conversations', {
    method: 'POST', body: JSON.stringify({ timezone }),
  })
}

export async function sendMessage(conversationId: string, content: string) {
  return jsonRequest<MessageRun>(`/api/v1/conversations/${conversationId}/runs`, {
    method: 'POST', body: JSON.stringify({ message: content }),
  })
}

export async function cancelRun(runId: string) {
  return jsonRequest<MessageRun>(`/api/v1/runs/${runId}/cancel`, {
    method: 'POST', body: '{}',
  })
}

export interface StreamEvent {
  id: string
  type: string
  data: Record<string, unknown>
}

export async function streamRun(
  runId: string,
  onEvent: (event: StreamEvent) => void,
  signal: AbortSignal,
  lastEventId?: string,
) {
  const response = await fetch(`${gearmateBaseUrl}/api/v1/runs/${runId}/events`, {
    headers: headers(lastEventId ? { 'Last-Event-ID': lastEventId } : {}), signal,
  })
  if (response.status === 401) window.dispatchEvent(new CustomEvent('rentflow:unauthorized'))
  if (!response.ok || !response.body) throw new Error(`事件流连接失败 (${response.status})`)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const block = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      const lines = block.split('\n')
      const id = lines.find((line) => line.startsWith('id:'))?.slice(3).trim() || ''
      const type = lines.find((line) => line.startsWith('event:'))?.slice(6).trim() || 'message'
      const raw = lines.filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).join('\n')
      let data: Record<string, unknown> = {}
      try { data = raw ? JSON.parse(raw) as Record<string, unknown> : {} } catch { data = { text: raw } }
      onEvent({ id, type, data })
      boundary = buffer.indexOf('\n\n')
    }
  }
}
