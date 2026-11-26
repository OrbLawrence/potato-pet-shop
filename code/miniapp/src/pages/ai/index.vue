<template>
  <view class="ai-page">
    <!-- 顶部工具栏 -->
    <view class="ai-toolbar">
      <view class="ai-title">🐾 AI 宠物专家</view>
      <button class="new-chat-btn" @tap="startNewChat">开启新对话</button>
    </view>

    <!-- 快捷问题 -->
    <scroll-view class="quick-scroll" scroll-x enable-flex>
      <view class="quick-question" v-for="(q, i) in quickQuestions" :key="i" @tap="send(q)">
        {{ q }}
      </view>
    </scroll-view>

    <!-- 对话列表 -->
    <scroll-view
      class="chat-scroll"
      scroll-y
      :scroll-into-view="scrollToId"
      scroll-with-animation
    >
      <view class="chat-inner">
        <view v-if="messages.length === 0" class="empty-chat">
          <view class="empty-emoji">🦴</view>
          <text class="empty-text">我是 AI 宠物专家，随时为你解答门店与养宠问题</text>
        </view>

        <view
          v-for="(msg, i) in messages"
          :key="msg.id"
          :id="msg.id"
          class="msg-row"
          :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'"
        >
          <view class="bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
            <view
              v-if="generating && i === messages.length - 1 && msg.role === 'ai' && !msg.content"
              class="thinking-box"
            >
              <view class="thinking-dots">
                <text class="dot"></text>
                <text class="dot"></text>
                <text class="dot"></text>
              </view>
              <text class="thinking-text">正在思考…</text>
            </view>
            <text v-else class="bubble-text">{{ msg.content }}</text>
            <text v-if="generating && i === messages.length - 1 && msg.role === 'ai' && msg.content" class="cursor">▍</text>
          </view>
        </view>

        <view class="chat-bottom-space"></view>
      </view>
    </scroll-view>

    <!-- 底部输入栏 -->
    <view class="input-bar">
      <input
        class="chat-input"
        v-model="inputValue"
        :disabled="generating"
        placeholder="请输入问题…"
        placeholder-class="chat-placeholder"
        confirm-type="send"
        :maxlength="500"
        @confirm="send(inputValue)"
      />
      <button v-if="!generating" class="send-btn" :disabled="!inputValue.trim()" @tap="send(inputValue)">
        发送
      </button>
      <button v-else class="stop-btn" @tap="stopGenerate">停止</button>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { request } from '../../utils/request'
import { streamAiChat } from '../../utils/ai'

const quickQuestions = [
  '新养的小狗到家要注意什么？',
  '猫咪挑食怎么办？',
  '如何给宠物科学洗澡？',
  '狗狗疫苗什么时候打？'
]

const messages = ref([])
const inputValue = ref('')
const conversationId = ref(uni.getStorageSync('conversationId') || '')
const generating = ref(false)
const scrollToId = ref('')

let streamTask = null
let stopped = false
let messageSeq = 0

function setConversation(id) {
  if (!id) return
  conversationId.value = id
  uni.setStorageSync('conversationId', id)
}

function scrollToBottom(id) {
  scrollToId.value = ''
  nextTick(() => {
    scrollToId.value = id || `msg-${messages.value.length - 1}`
  })
}

function appendAssistant() {
  const id = `msg-${messageSeq}`
  messageSeq += 1
  messages.value.push({ id, role: 'ai', content: '' })
  return id
}

function updateAssistant(id, content) {
  const msg = messages.value.find((m) => m.id === id)
  if (msg) msg.content = content
  scrollToBottom(id)
}

function startNewChat() {
  if (generating.value) return
  conversationId.value = ''
  uni.removeStorageSync('conversationId')
  messages.value = []
  inputValue.value = ''
  scrollToId.value = ''
}

async function blockingReply(query) {
  generating.value = true
  const id = appendAssistant()
  scrollToBottom(id)
  try {
    const data = await request({
      url: '/ai/chat/blocking',
      method: 'POST',
      data: { query, conversationId: conversationId.value || '' }
    })
    const content = (data && data.content) || 'AI 暂时没有返回内容，请稍后再试。'
    setConversation(data && data.conversationId)
    updateAssistant(id, content)
  } catch (e) {
    updateAssistant(id, 'AI 服务暂时不可用，请稍后再试。')
  } finally {
    generating.value = false
  }
}

function send(text) {
  const query = (text || inputValue.value).trim()
  if (!query || generating.value) return

  inputValue.value = ''
  stopped = false

  const userId = `msg-${messageSeq}`
  messages.value.push({ id: userId, role: 'user', content: query })
  messageSeq += 1
  scrollToBottom(userId)

  const assistantId = appendAssistant()
  let received = false

  generating.value = true
  streamTask = streamAiChat({
    query,
    conversationId: conversationId.value || '',
    onMessage: (chunk) => {
      received = true
      const msg = messages.value.find((m) => m.id === assistantId)
      if (msg) updateAssistant(assistantId, msg.content + chunk)
    },
    onConversation: (id) => setConversation(id),
    onDone: () => {
      generating.value = false
      if (!received && !stopped) {
        messages.value = messages.value.filter((m) => m.id !== assistantId)
        blockingReply(query)
      }
    },
    onError: () => {
      generating.value = false
      if (!received && !stopped) {
        messages.value = messages.value.filter((m) => m.id !== assistantId)
        blockingReply(query)
      }
    }
  })
}

function stopGenerate() {
  stopped = true
  if (streamTask && typeof streamTask.abort === 'function') streamTask.abort()
  generating.value = false
}
</script>

<style lang="scss" scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 112rpx - env(safe-area-inset-bottom));
  background: #fff8ef;
}

.ai-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 28rpx;
  background: #ffffff;
  border-bottom: 2rpx solid #f3e7d8;
}

.ai-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #4a3728;
}

.new-chat-btn {
  padding: 14rpx 28rpx;
  border-radius: 30rpx;
  background: #fff1e0;
  color: #e8873a;
  font-size: 26rpx;
  font-weight: 600;
}

.quick-scroll {
  width: 100%;
  white-space: nowrap;
  background: #ffffff;
  padding: 18rpx 24rpx 22rpx;
}

.quick-question {
  display: inline-flex;
  align-items: center;
  height: 62rpx;
  padding: 0 28rpx;
  margin-right: 16rpx;
  border-radius: 31rpx;
  background: #fff8ef;
  border: 2rpx solid #f2dcc0;
  color: #8a5a33;
  font-size: 26rpx;
}

.chat-scroll {
  flex: 1;
  overflow: hidden;
}

.chat-inner {
  padding: 28rpx 24rpx 8rpx;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
}

.empty-emoji {
  font-size: 96rpx;
}

.empty-text {
  margin-top: 24rpx;
  font-size: 28rpx;
  color: #9b8574;
}

.msg-row {
  display: flex;
  margin-bottom: 28rpx;
}

.msg-user {
  justify-content: flex-end;
}

.msg-ai {
  justify-content: flex-start;
}

.bubble {
  max-width: 76%;
  padding: 22rpx 26rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-user {
  background: linear-gradient(135deg, #ff9d4d 0%, #e8873a 100%);
  color: #ffffff;
  border-bottom-right-radius: 6rpx;
}

.bubble-ai {
  background: #ffffff;
  color: #4a3728;
  border-bottom-left-radius: 6rpx;
  box-shadow: 0 6rpx 22rpx rgba(194, 132, 63, 0.10);
}

.cursor {
  color: #e8873a;
}

.thinking-box {
  display: flex;
  flex-direction: column;
}

.thinking-dots {
  display: flex;
  gap: 8rpx;
}

.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #e8873a;
  animation: dot-blink 1.2s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-blink {
  0%, 80%, 100% { opacity: 0.25; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-4rpx); }
}

.thinking-text {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #9b8574;
}

.chat-bottom-space {
  height: 20rpx;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx 20rpx;
  background: #ffffff;
  border-top: 2rpx solid #f3e7d8;
}

.chat-input {
  flex: 1;
  height: 78rpx;
  padding: 0 28rpx;
  border-radius: 39rpx;
  background: #fff3e6;
  font-size: 28rpx;
  color: #4a3728;
}

.chat-placeholder {
  color: #c6b6a4;
}

.send-btn {
  width: 128rpx;
  height: 78rpx;
  line-height: 78rpx;
  text-align: center;
  border-radius: 39rpx;
  background: linear-gradient(135deg, #ff9d4d 0%, #e8873a 100%);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
}

.send-btn[disabled] {
  opacity: 0.5;
}

.stop-btn {
  width: 128rpx;
  height: 78rpx;
  line-height: 78rpx;
  text-align: center;
  border-radius: 39rpx;
  background: #fdeeee;
  color: #d9534f;
  font-size: 28rpx;
  font-weight: 700;
}
</style>



