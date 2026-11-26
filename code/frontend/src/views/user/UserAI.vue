<template>
  <div class="ai-page">
    <div class="ai-header">
      <div class="ai-avatar">🤖</div>
      <div class="ai-heading">
        <h2>智能客服</h2>
        <p>基于DeepSeek大模型与土豆宠物之家知识库，解答门店与宠物养护等问题</p>
      </div>
      <el-button text class="new-chat-btn" :disabled="sending" @click="handleNewChat">
        🔄 开启新对话
      </el-button>
    </div>

    <div class="chat-panel">
      <div ref="messageListRef" class="message-list">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">🐾</div>
          <h3>你好，我是您的智能客服，可以为您解答各项门店与爱宠的相关问题</h3>
          <p>你可以问我门店、洗护、寄养、充值或宠物养护问题</p>
          <div class="quick-questions">
            <button
              v-for="item in quickQuestions"
              :key="item"
              type="button"
              class="quick-question"
              @click="sendMessage(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>

        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message-row"
          :class="message.role"
        >
          <div class="bubble">
            {{ message.content }}
            <span v-if="message.loading" class="typing-dots">
              <span></span><span></span><span></span>
            </span>
          </div>
        </div>
      </div>

      <div class="input-bar">
        <el-input
          v-model="draft"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          class="ai-input"
          placeholder="输入你的问题，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="handleSend"
        />
        <el-button
          v-if="sending"
          type="danger"
          class="send-btn"
          @click="handleStop"
        >
          停止
        </el-button>
        <el-button
          v-else
          type="primary"
          class="send-btn"
          :disabled="!draft.trim()"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { streamAiChat } from '../../utils/aiStream'

const quickQuestions = [
  '门店营业时间是几点？',
  '小型犬洗澡多少钱？',
  '猫咪寄养多少钱一晚？',
  '充值支持哪些支付方式？',
  '宠物抽搐应该怎么处理？'
]

const draft = ref('')
const sending = ref(false)
const conversationId = ref('')
const messages = ref([])
const messageListRef = ref(null)
let abortStream = null

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const ensureLastMessage = () => {
  if (messages.value.length === 0) {
    messages.value.push({ role: 'assistant', content: '', loading: true })
  }
  return messages.value[messages.value.length - 1]
}

const handleSend = () => {
  const question = draft.value.trim()
  if (!question || sending.value) return
  draft.value = ''
  sendMessage(question)
}

const sendMessage = async question => {
  messages.value.push({ role: 'user', content: question, loading: false })
  messages.value.push({ role: 'assistant', content: '', loading: true })
  sending.value = true
  await scrollToBottom()

  abortStream = streamAiChat({
    query: question,
    conversationId: conversationId.value,
    onMessage: text => {
      const last = ensureLastMessage()
      last.loading = false
      last.content += text
      scrollToBottom()
    },
    onConversation: id => {
      if (id) conversationId.value = id
    },
    onDone: () => {
      sending.value = false
      ensureLastMessage().loading = false
      scrollToBottom()
    },
    onError: error => {
      sending.value = false
      const last = ensureLastMessage()
      last.loading = false
      if (!last.content) {
        last.content = '抱歉，AI 服务暂时不可用，请稍后重试。'
      }
      ElMessage.error(error.message || '网络异常，请稍后重试')
      scrollToBottom()
    }
  })
}

const handleStop = () => {
  if (abortStream) {
    abortStream()
    abortStream = null
  }
  sending.value = false
  const last = messages.value[messages.value.length - 1]
  if (last && last.loading) {
    last.loading = false
    if (!last.content) last.content = '已停止生成。'
  }
}

const handleNewChat = () => {
  if (sending.value) {
    handleStop()
  }
  messages.value = []
  conversationId.value = ''
  draft.value = ''
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: calc(100vh - 160px);
  min-height: 520px;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 14px;
  background: white;
  border-radius: 18px;
  padding: 16px 20px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.05);
}

.ai-avatar {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
}

.ai-heading {
  flex: 1;
}

.ai-heading h2 {
  margin: 0;
  font-size: 20px;
  color: #5a3e2b;
}

.ai-heading p {
  margin: 4px 0 0;
  color: #999;
  font-size: 13px;
}

.new-chat-btn {
  color: #ff7d45 !important;
  font-weight: 600;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  background: linear-gradient(180deg, #fffaf4 0%, #ffffff 100%);
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 30px 16px;
}

.empty-icon {
  font-size: 56px;
  animation: pawBounce 2s ease-in-out infinite;
}

@keyframes pawBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.empty-state h3 {
  margin: 12px 0 6px;
  color: #5a3e2b;
}

.empty-state p {
  margin: 0;
  color: #999;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 22px;
  max-width: 620px;
}

.quick-question {
  border: 1px solid #ffe0c2;
  background: #fff8f0;
  color: #b86a35;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.quick-question:hover {
  background: #ffefe0;
  transform: translateY(-2px);
}

.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.user .bubble {
  background: linear-gradient(135deg, #ff9a56, #ff6b6b);
  color: white;
  border-bottom-right-radius: 6px;
}

.assistant .bubble {
  background: white;
  border: 1px solid #f2e4d4;
  color: #4b3a2e;
  border-bottom-left-radius: 6px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.typing-dots {
  display: inline-flex;
  gap: 4px;
  margin-left: 8px;
  vertical-align: middle;
}

.typing-dots span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #ff9a56;
  animation: blink 1s infinite;
}

.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.25; }
  40% { opacity: 1; }
}

.input-bar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f5eadb;
  background: white;
}

.ai-input {
  flex: 1;
}

.send-btn {
  min-width: 76px;
  height: 40px;
}

@media (max-width: 768px) {
  .ai-page {
    height: calc(100vh - 140px);
    min-height: 480px;
  }

  .bubble {
    max-width: 90%;
  }
}
</style>
