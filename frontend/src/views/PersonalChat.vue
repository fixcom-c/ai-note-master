<template>
  <div class="chat-page">
    <div class="page-header">
      <div>
        <p class="lead-copy">你可以直接问最近最该聚焦什么、哪些主题在反复出现、下一步该怎么推进。我会参考你的个人画像、日常记录、长期主题和最近任务来回答。</p>
      </div>
    </div>

    <div class="chat-layout">
      <div class="left-column">
        <el-card class="context-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><DataLine /></el-icon> 我会从这些内容里理解你</span>
            </div>
          </template>

          <div class="context-list">
            <div class="context-item">
              <strong>个人画像</strong>
              <p>你是谁、当前重点、长期目标、工作风格、AI 偏好。</p>
            </div>
            <div class="context-item">
              <strong>最近笔记</strong>
              <p>包括快速记录、日计划、晚间复盘和周复盘。</p>
            </div>
            <div class="context-item">
              <strong>长期主题</strong>
              <p>你最近反复在思考、沉淀和投入的方向。</p>
            </div>
            <div class="context-item">
              <strong>最近任务</strong>
              <p>判断你当前已经推进到哪一步、还有哪些事待收敛。</p>
            </div>
          </div>
        </el-card>

        <el-card class="prompt-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><MagicStick /></el-icon> 可以直接这样问</span>
            </div>
          </template>

          <div class="prompt-list">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt"
              type="button"
              class="prompt-chip"
              @click="askPrompt(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </el-card>
      </div>

      <el-card class="chat-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><ChatDotRound /></el-icon> 问问最近的自己</span>
            <el-button text @click="resetChat">重新开始</el-button>
          </div>
        </template>

        <div ref="messageListRef" class="message-list">
          <div
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            class="message-row"
            :class="message.role"
          >
            <div class="message-bubble">
              <strong class="message-role">{{ message.role === 'assistant' ? 'AI 助手' : '你' }}</strong>
              <p class="message-content">{{ message.content }}</p>

              <div v-if="message.contextSummary" class="message-meta">
                {{ message.contextSummary }}
              </div>

              <div v-if="message.references?.length" class="reference-list">
                <div v-for="reference in message.references" :key="`${reference.type}-${reference.title}`" class="reference-card">
                  <el-tag size="small" effect="plain">{{ referenceTypeLabelMap[reference.type] || reference.type }}</el-tag>
                  <strong>{{ reference.title }}</strong>
                  <p>{{ reference.summary }}</p>
                </div>
              </div>

              <div v-if="message.suggestions?.length" class="suggestion-row">
                <button
                  v-for="item in message.suggestions"
                  :key="item"
                  type="button"
                  class="suggestion-chip"
                  @click="askPrompt(item)"
                >
                  {{ item }}
                </button>
              </div>

              <div v-if="message.model" class="message-model">
                {{ message.model === 'MockAI' ? '本地模拟' : message.model }}
              </div>
            </div>
          </div>
        </div>

        <div class="composer">
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="例如：我最近最该聚焦什么？ / 最近反复出现的主题是什么？ / 帮我把最近记录整理成一段周复盘思路。"
            @keydown.ctrl.enter.prevent="sendQuestion"
          />
          <div class="composer-actions">
            <span class="composer-hint">`Ctrl + Enter` 发送</span>
            <el-button type="primary" :loading="sending" :disabled="!question.trim()" @click="sendQuestion">
              发送问题
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiAPI } from '@/api'
import type { PersonalChatReference, PersonalChatReply } from '@/api/types'
import { ChatDotRound, DataLine, MagicStick } from '@element-plus/icons-vue'

type ChatMessage = {
  role: 'assistant' | 'user'
  content: string
  contextSummary?: string
  model?: string
  references?: PersonalChatReference[]
  suggestions?: string[]
}

const quickPrompts = [
  '我最近最该聚焦什么？',
  '最近反复出现的主题是什么？',
  '结合我的记录，下一步最值得推进什么？',
  '帮我把最近内容整理成一段周复盘思路'
]

const referenceTypeLabelMap: Record<string, string> = {
  note: '笔记',
  theme: '主题',
  task: '任务'
}

const question = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const messages = ref<ChatMessage[]>([
  {
    role: 'assistant',
    content: '可以直接问我最近最该聚焦什么、长期主线是什么、下一步该推进什么。我会优先参考你的个人画像、最近笔记、长期主题和最近任务来回答。'
  }
])

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const pushAssistantReply = (reply: PersonalChatReply) => {
  messages.value.push({
    role: 'assistant',
    content: reply.answer,
    contextSummary: reply.contextSummary,
    model: reply.model,
    references: reply.references,
    suggestions: reply.suggestions
  })
}

const sendQuestion = async () => {
  if (!question.value.trim()) return

  const currentQuestion = question.value.trim()
  messages.value.push({
    role: 'user',
    content: currentQuestion
  })
  question.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const reply = await aiAPI.personalChat({ question: currentQuestion })
    pushAssistantReply(reply)
    await scrollToBottom()
  } catch {
    ElMessage.error('对话失败，请稍后再试')
  } finally {
    sending.value = false
  }
}

const askPrompt = async (prompt: string) => {
  question.value = prompt
  await sendQuestion()
}

const resetChat = () => {
  messages.value = [
    {
      role: 'assistant',
      content: '新的对话已经开始。继续问我最近的主线、下一步动作，或者让我直接整理成周复盘思路。'
    }
  ]
  question.value = ''
}
</script>

<style scoped>
.chat-page {
  padding: 24px;
  max-width: 1360px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.lead-copy {
  max-width: 840px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 20px;
}

.left-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.context-card,
.prompt-card,
.chat-card {
  border-radius: 24px;
  box-shadow: var(--shadow-card);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.context-list,
.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.context-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
}

.context-item strong,
.reference-card strong {
  display: block;
  margin-bottom: 6px;
}

.context-item p,
.reference-card p,
.message-meta,
.message-model {
  color: var(--text-secondary);
  line-height: 1.8;
}

.prompt-chip,
.suggestion-chip {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.24s ease;
}

.prompt-chip:hover,
.suggestion-chip:hover {
  border-color: rgba(31, 111, 235, 0.24);
  transform: translateY(-1px);
}

.chat-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 720px;
}

.message-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 620px;
  overflow-y: auto;
  padding-right: 4px;
}

.message-row {
  display: flex;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-bubble {
  width: min(100%, 760px);
  padding: 16px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.message-row.user .message-bubble {
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.14), rgba(19, 184, 166, 0.12));
}

.message-role {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.85;
}

.message-meta {
  margin-top: 10px;
  font-size: 13px;
}

.reference-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.reference-card {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
}

.reference-card :deep(.el-tag) {
  margin-bottom: 8px;
}

.suggestion-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.suggestion-chip {
  padding: 9px 12px;
  border-radius: 999px;
}

.message-model {
  margin-top: 10px;
  font-size: 12px;
}

.composer {
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  padding-top: 16px;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  gap: 16px;
}

.composer-hint {
  color: var(--text-secondary);
  font-size: 13px;
}

@media (max-width: 960px) {
  .chat-page {
    padding: 16px;
  }

  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-card :deep(.el-card__body) {
    min-height: auto;
  }

  .message-list {
    max-height: none;
  }

  .composer-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
