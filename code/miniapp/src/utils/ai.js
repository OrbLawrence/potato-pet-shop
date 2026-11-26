import { BASE_URL } from '../config'
import { getToken } from './request'

function arrayBufferToString(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.length; i += 1) {
    binary += String.fromCharCode(bytes[i])
  }
  try {
    return decodeURIComponent(escape(binary))
  } catch (e) {
    return binary
  }
}

function parsePayload(raw, handlers, state) {
  let payload
  try {
    payload = JSON.parse(raw)
  } catch (e) {
    // 兼容旧版纯文本消息
    if (raw && handlers.onMessage) handlers.onMessage(raw)
    return
  }

  if (!payload || typeof payload !== 'object') return

  if (payload.type === 'message' && handlers.onMessage) {
    handlers.onMessage(payload.content || '')
  } else if (payload.type === 'conversation' && handlers.onConversation) {
    handlers.onConversation(payload.conversationId || '')
  } else if (payload.type === 'done' && !state.finished) {
    state.finished = true
    if (handlers.onDone) handlers.onDone()
  }
}

/**
 * 微信小程序流式 AI 对话封装：
 * 优先使用 enableChunked + onChunkReceived 解析后端 SSE 数据。
 * 若当前环境不支持分块接收（success 返回完整响应但没有触发 chunk），
 * 由调用方根据是否收到内容决定回退到 /ai/chat/blocking。
 */
export function streamAiChat({ query, conversationId, onMessage, onConversation, onDone, onError }) {
  const state = { finished: false, stopped: false }
  const handlers = { onMessage, onConversation, onDone, onError }
  const decoder = typeof TextDecoder !== 'undefined' ? new TextDecoder('utf-8') : null

  let buffer = ''
  let dataLines = []

  const flush = () => {
    if (!dataLines.length) return
    const raw = dataLines.join('\n').trim()
    dataLines = []
    if (raw) parsePayload(raw, handlers, state)
  }

  const handleText = (text) => {
    if (!text) return
    buffer += text
    let index
    while ((index = buffer.indexOf('\n')) !== -1) {
      let line = buffer.slice(0, index)
      buffer = buffer.slice(index + 1)
      if (line.endsWith('\r')) line = line.slice(0, -1)
      if (line === '') {
        flush()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }
  }

  const finalize = () => {
    if (decoder) handleText(decoder.decode())
    if (buffer) handleText('\n')
    flush()
    if (!state.finished) {
      state.finished = true
      if (handlers.onDone) handlers.onDone()
    }
  }

  const task = uni.request({
    url: `${BASE_URL}/ai/chat`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`
    },
    data: {
      query,
      conversationId: conversationId || ''
    },
    enableChunked: true,
    success: () => {
      finalize()
    },
    fail: (err) => {
      if (state.stopped) return
      console.error('[PetShop] AI 请求失败:', `${BASE_URL}/ai/chat`, err)
      state.finished = true
      if (handlers.onError) handlers.onError(err)
    },
    onChunkReceived: (res) => {
      const chunk = res && res.data
      if (chunk == null) return
      let text
      if (typeof chunk === 'string') {
        text = chunk
      } else if (decoder) {
        text = decoder.decode(chunk, { stream: true })
      } else {
        text = arrayBufferToString(chunk)
      }
      handleText(text)
    }
  })

  return {
    abort() {
      state.stopped = true
      state.finished = true
      if (task && typeof task.abort === 'function') task.abort()
    }
  }
}
