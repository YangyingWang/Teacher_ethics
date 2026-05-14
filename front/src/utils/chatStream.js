import { useTokenStore } from '@/stores/token.js'

export async function chatStream(message, { onToken, signal } = {}) {
  const tokenStore = useTokenStore()

  const url = `/api/study/chat/stream?message=${encodeURIComponent(message ?? '')}`

  const headers = { Accept: 'text/event-stream' }

  if (tokenStore.token) headers.Authorization = tokenStore.token

  const resp = await fetch(url, { method: 'GET', headers, signal })

  if (!resp.ok) throw new Error(`HTTP ${resp.status} ${resp.statusText}`)
  if (!resp.body) throw new Error('Response body 为空')

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')

  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // 兼容 SSE：事件以 \n\n 分隔
    while (buffer.includes('\n\n')) {
      const idx = buffer.indexOf('\n\n')
      const eventBlock = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)

      const lines = eventBlock.split('\n')
      let hit = false
      for (const line of lines) {
        if (line.startsWith('data:')) {
          hit = true
          const t = line.slice(5).trimStart()
          if (t && t !== '[DONE]') onToken?.(t)
        }
      }
      // 不是 SSE 格式时按纯文本处理
      if (!hit && eventBlock.trim()) onToken?.(eventBlock)
    }

    // 兜底：后端若不是标准 SSE 分隔，也能逐步输出
    if (buffer.length > 4096 && !buffer.includes('\n\n')) {
      onToken?.(buffer)
      buffer = ''
    }
  }

  if (buffer.trim()) onToken?.(buffer)
}