const AI_STREAM_URL = '/api/ai/chat'

function getToken() {
  return localStorage.getItem('token') || ''
}

function handlePayload(raw, handlers, state) {
  let payload
  try {
    payload = JSON.parse(raw)
  } catch (error) {
    // 兼容旧版纯文本消息
    if (handlers.onMessage && raw) handlers.onMessage(raw)
    return
  }

  if (!payload || typeof payload !== 'object') return

  if (payload.type === 'message' && handlers.onMessage) {
    handlers.onMessage(payload.content || '')
  } else if (payload.type === 'conversation' && handlers.onConversation) {
    handlers.onConversation(payload.conversationId || '')
  } else if (payload.type === 'done' && !state.finished) {
    state.finished = true
    handlers.onDone()
  }
}

export function streamAiChat({ query, conversationId, onMessage, onConversation, onDone, onError }) {
  const controller = new AbortController()
  const state = { finished: false }
  const handlers = { onMessage, onConversation, onDone }

  fetch(AI_STREAM_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`
    },
    body: JSON.stringify({ query, conversationId: conversationId || '' }),
    signal: controller.signal
  })
    .then(async response => {
      if (!response.ok || !response.body) {
        throw new Error(response.status === 401 ? '登录已过期，请重新登录' : 'AI 服务暂时不可用')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let dataLines = []

      const flush = () => {
        if (dataLines.length === 0) return
        const raw = dataLines.join('\n').trim()
        dataLines = []
        if (raw) handlePayload(raw, handlers, state)
      }

      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const newlineIndex = buffer.lastIndexOf('\n')
        if (newlineIndex === -1) continue
        const complete = buffer.slice(0, newlineIndex + 1)
        buffer = buffer.slice(newlineIndex + 1)

        for (const line of complete.split(/\r?\n/)) {
          if (line === '') {
            flush()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim())
          }
        }
      }

      if (buffer) {
        for (const line of (buffer + '\n').split(/\r?\n/)) {
          if (line === '') flush()
          else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
        }
      }
      flush()

      if (!state.finished) {
        state.finished = true
        handlers.onDone()
      }
    })
    .catch(error => {
      if (error.name === 'AbortError') return
      if (!state.finished) {
        state.finished = true
        handlers.onDone()
      }
      if (onError) onError(error)
    })

  return () => controller.abort()
}
