import { useAuthStore } from '../stores/auth'

export interface SseHandlers {
  onMessage?: (delta: string) => void
  onDone?: (data: any) => void
  onError?: (message: string) => void
}

/**
 * 基于 fetch 的 SSE 客户端：EventSource 无法自定义请求头，
 * 且我们的生成接口是 POST 携带 JSON 请求体，因此手动解析事件流
 */
export async function streamSse(url: string, body: unknown, handlers: SseHandlers) {
  const auth = useAuthStore()
  let response: Response
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: `Bearer ${auth.accessToken}`
      },
      body: JSON.stringify(body)
    })
  } catch {
    handlers.onError?.('网络异常，请检查后端服务是否可用')
    return
  }

  if (!response.ok || !response.body) {
    let message = `请求失败（HTTP ${response.status}）`
    try {
      const result = await response.json()
      message = result.message || message
    } catch {
      // 非 JSON 响应保持默认提示
    }
    if (response.status === 401) {
      auth.logout()
    }
    handlers.onError?.(message)
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatch = (block: string) => {
    let event = 'message'
    const dataLines: string[] = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }
    if (!dataLines.length) return
    let payload: any
    try {
      payload = JSON.parse(dataLines.join('\n'))
    } catch {
      payload = { message: dataLines.join('\n') }
    }
    if (event === 'message') {
      handlers.onMessage?.(payload.delta ?? '')
    } else if (event === 'done') {
      handlers.onDone?.(payload)
    } else if (event === 'error') {
      handlers.onError?.(payload.message || '生成失败')
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let index: number
    while ((index = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, index)
      buffer = buffer.slice(index + 2)
      dispatch(block)
    }
  }
}
