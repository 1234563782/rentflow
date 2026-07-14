<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, CircleClose, Promotion, Tools } from '@element-plus/icons-vue'
import { cancelRun, createConversation, sendMessage, streamRun, type StreamEvent } from '@/services/gearmate'
import { useAuthStore } from '@/stores/auth'

interface ChatMessage { id: string; role: 'user' | 'assistant'; content: string }
interface ToolActivity { id: string; name: string; status: 'running' | 'success' | 'failed' }

const auth = useAuthStore()
const input = ref('')
const messages = ref<ChatMessage[]>([])
const tools = ref<ToolActivity[]>([])
const conversationId = ref('')
const runId = ref('')
const running = ref(false)
const lastEventId = ref('')
const messageList = ref<HTMLElement>()
let controller: AbortController | null = null

const canSend = computed(() => input.value.trim().length > 0 && !running.value)
function scrollBottom() { void nextTick(() => messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' })) }

function textFrom(data: Record<string, unknown>) {
  const value = data.delta ?? data.text ?? data.content ?? ''
  return typeof value === 'string' ? value : ''
}

function handleEvent(event: StreamEvent, assistantId: string) {
  if (event.id) lastEventId.value = event.id
  if (event.type === 'message.delta' || event.type === 'assistant.delta') {
    const target = messages.value.find((message) => message.id === assistantId)
    if (target) target.content += textFrom(event.data)
  }
  if (event.type === 'assistant.completed') {
    const target = messages.value.find((message) => message.id === assistantId)
    if (target && !target.content) target.content = textFrom(event.data)
  }
  if (event.type === 'tool.started') {
    tools.value.push({ id: event.id, name: String(event.data.toolName || event.data.tool || event.data.name || 'RentFlow 工具'), status: 'running' })
  }
  if (event.type === 'tool.completed' || event.type === 'tool.failed') {
    const toolName = String(event.data.toolName || event.data.tool || event.data.name || '')
    const target = [...tools.value].reverse().find((tool) => !toolName || tool.name === toolName)
    if (target) target.status = event.type === 'tool.completed' ? 'success' : 'failed'
  }
  if (['run.completed', 'run.cancelled', 'run.failed'].includes(event.type)) running.value = false
  scrollBottom()
}

async function send() {
  const content = input.value.trim()
  if (!content || running.value) return
  input.value = ''
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content })
  const assistantId = crypto.randomUUID()
  messages.value.push({ id: assistantId, role: 'assistant', content: '' })
  running.value = true
  tools.value = []
  scrollBottom()
  try {
    if (!conversationId.value) conversationId.value = (await createConversation(auth.user?.timezone || 'Asia/Shanghai')).id
    const run = await sendMessage(conversationId.value, content)
    runId.value = run.runId
    lastEventId.value = ''
    controller = new AbortController()
    await streamRun(run.runId, (event) => handleEvent(event, assistantId), controller.signal)
  } catch (error) {
    if (!(error instanceof DOMException && error.name === 'AbortError')) {
      const target = messages.value.find((message) => message.id === assistantId)
      if (target && !target.content) target.content = 'GearMate 当前不可用，请稍后再试。'
      ElMessage.error(error instanceof Error ? error.message : 'GearMate 请求失败')
    }
  } finally { running.value = false; controller = null }
}

async function stop() {
  controller?.abort()
  if (runId.value) await cancelRun(runId.value).catch(() => undefined)
  running.value = false
}

onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <section class="chat-page">
    <header class="chat-header"><div><span class="eyebrow">租赁助手</span><h1>GearMate</h1></div><span class="service-status"><i></i>连接真实库存与报价</span></header>
    <div class="chat-workspace">
      <div ref="messageList" class="message-list">
        <div v-if="!messages.length" class="chat-empty"><div class="chat-empty-icon"><el-icon><ChatDotRound /></el-icon></div><h2>今天需要什么设备？</h2><p>描述用途和时间，GearMate 会查询 RentFlow 的实时设备、库存和报价。</p><div class="prompt-chips"><button @click="input = '下周末拍活动，有哪些相机可以租？'">活动摄影相机</button><button @click="input = '我需要直播设备，预算每天 500 元以内'">直播设备组合</button><button @click="input = '帮我查周五晚上可租的麦克风'">周五麦克风库存</button></div></div>
        <template v-for="message in messages" :key="message.id"><div class="message" :class="`message--${message.role}`"><span class="message-role">{{ message.role === 'user' ? auth.user?.nickname : 'GearMate' }}</span><div class="message-bubble">{{ message.content || '正在思考…' }}</div></div></template>
      </div>
      <aside class="tool-rail"><div class="tool-rail-heading"><el-icon><Tools /></el-icon><span>工具活动</span></div><div v-if="!tools.length" class="tool-idle">等待需要实时数据的请求</div><div v-for="tool in tools" :key="tool.id" class="tool-item"><span class="tool-dot" :class="`is-${tool.status}`"></span><div><strong>{{ tool.name }}</strong><span>{{ tool.status === 'running' ? '执行中' : tool.status === 'success' ? '已完成' : '失败' }}</span></div></div></aside>
    </div>
    <div class="composer"><el-input v-model="input" type="textarea" :autosize="{ minRows: 1, maxRows: 5 }" maxlength="2000" resize="none" placeholder="询问设备、租期、库存或报价" @keydown.enter.exact.prevent="send" /><el-button v-if="running" class="composer-button" :icon="CircleClose" circle type="danger" title="停止" aria-label="停止生成" @click="stop" /><el-button v-else class="composer-button" :icon="Promotion" circle type="primary" :disabled="!canSend" title="发送" aria-label="发送消息" @click="send" /></div>
  </section>
</template>
