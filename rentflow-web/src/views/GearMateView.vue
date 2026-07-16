<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, CircleClose, Promotion, Tools } from '@element-plus/icons-vue'
import {
  cancelRun,
  createConversation,
  listConversationMessages,
  listConversations,
  sendMessage,
  streamRun,
  type StreamEvent,
} from '@/services/gearmate'
import { useAuthStore } from '@/stores/auth'
import ProductVisual from '@/components/ProductVisual.vue'
import QuickBookingDialog from '@/components/QuickBookingDialog.vue'
import type {
  RecommendationCard,
  RecommendationPresentation,
  RentalPeriodValue,
} from '@/types'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  presentation?: RecommendationPresentation | null
}
interface ToolActivity { id: string; name: string; status: 'running' | 'success' | 'failed' }

const auth = useAuthStore()
const input = ref('')
const messages = ref<ChatMessage[]>([])
const tools = ref<ToolActivity[]>([])
const conversationId = ref('')
const runId = ref('')
const running = ref(false)
const restoring = ref(true)
const bookingOpen = ref(false)
const bookingProduct = ref<RecommendationCard>()
const bookingPeriod = ref<RentalPeriodValue | null>(null)
const lastEventId = ref('')
const messageList = ref<HTMLElement>()
let controller: AbortController | null = null

const conversationStorageKey = computed(() => (
  auth.user ? `rentflow.gearmate.conversation.${auth.user.userId}` : ''
))
const canSend = computed(() => input.value.trim().length > 0 && !running.value && !restoring.value)
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
  if (event.type === 'recommendation.presented') {
    const target = messages.value.find((message) => message.id === assistantId)
    if (target) target.presentation = event.data as unknown as RecommendationPresentation
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
  if (!content || running.value || restoring.value) return
  input.value = ''
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content })
  const assistantId = crypto.randomUUID()
  messages.value.push({ id: assistantId, role: 'assistant', content: '' })
  running.value = true
  tools.value = []
  scrollBottom()
  try {
    if (!conversationId.value) {
      conversationId.value = (await createConversation(auth.user?.timezone || 'Asia/Shanghai')).id
      if (conversationStorageKey.value) {
        sessionStorage.setItem(conversationStorageKey.value, conversationId.value)
      }
    }
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

function openBooking(product: RecommendationCard, rentalPeriod: RentalPeriodValue | null) {
  bookingProduct.value = product
  bookingPeriod.value = rentalPeriod
  bookingOpen.value = true
}

async function restoreConversation() {
  try {
    const conversations = await listConversations()
    const storedId = conversationStorageKey.value
      ? sessionStorage.getItem(conversationStorageKey.value)
      : null
    const selected = conversations.find((conversation) => conversation.id === storedId)
      ?? conversations[0]
    if (!selected) return
    conversationId.value = selected.id
    if (conversationStorageKey.value) {
      sessionStorage.setItem(conversationStorageKey.value, selected.id)
    }
    const history = await listConversationMessages(selected.id)
    messages.value = history.map((message) => ({
      id: message.id,
      role: message.role,
      content: message.content,
      presentation: message.presentation,
    }))
    scrollBottom()
  } catch (error) {
    if (conversationStorageKey.value) sessionStorage.removeItem(conversationStorageKey.value)
    ElMessage.error(error instanceof Error ? error.message : 'GearMate history restore failed')
  } finally {
    restoring.value = false
  }
}

onMounted(() => void restoreConversation())
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <section class="chat-page">
    <header class="chat-header"><div><span class="eyebrow">租赁助手</span><h1>GearMate</h1></div><span class="service-status"><i></i>连接真实库存与报价</span></header>
    <div class="chat-workspace">
      <div ref="messageList" class="message-list">
        <div v-if="!restoring && !messages.length" class="chat-empty"><div class="chat-empty-icon"><el-icon><ChatDotRound /></el-icon></div><h2>今天需要什么设备？</h2><p>描述用途和时间，GearMate 会查询 RentFlow 的实时设备、库存和报价。</p><div class="prompt-chips"><button @click="input = '下周末拍活动，有哪些相机可以租？'">活动摄影相机</button><button @click="input = '我需要直播设备，预算每天 500 元以内'">直播设备组合</button><button @click="input = '帮我查周五晚上可租的麦克风'">周五麦克风库存</button></div></div>
        <template v-for="message in messages" :key="message.id">
          <div class="message" :class="`message--${message.role}`">
            <span class="message-role">{{ message.role === 'user' ? auth.user?.nickname : 'GearMate' }}</span>
            <div v-if="!message.presentation?.intro" class="message-bubble">{{ message.content || '正在思考…' }}</div>
            <div v-if="message.presentation" class="recommendation-presentation">
              <p v-if="message.presentation.intro" class="recommendation-copy recommendation-copy--intro">
                {{ message.presentation.intro }}
              </p>
              <section v-for="section in message.presentation.sections" :key="section.useCaseId || section.title" class="recommendation-section">
                <h3>{{ section.title }}</h3>
                <p v-if="section.description" class="recommendation-copy recommendation-section-copy">
                  {{ section.description }}
                </p>
                <div class="recommendation-products">
                  <button
                    v-for="product in section.products"
                    :key="product.productId"
                    type="button"
                    class="recommendation-product"
                    @click="openBooking(product, message.presentation.rentalPeriod)"
                  >
                    <ProductVisual :name="product.name" :brand="product.brand" />
                    <div class="recommendation-product-body">
                      <strong>{{ product.name }}</strong>
                      <span>{{ product.brand }} · {{ product.model }}</span>
                      <div class="recommendation-price">¥{{ product.dailyRate }}<small>/天</small></div>
                      <span>押金 ¥{{ product.fixedDeposit }}</span>
                      <span v-if="product.availableCount !== null" class="availability-label">可租 {{ product.availableCount }} 台</span>
                      <span v-else class="availability-label is-unchecked">尚未查询档期</span>
                    </div>
                  </button>
                </div>
              </section>
              <div v-if="message.presentation.followUp" class="recommendation-followup">
                <span>{{ message.presentation.followUp.text }}</span>
                <div v-if="message.presentation.followUp.options.length" class="followup-options">
                  <button v-for="option in message.presentation.followUp.options" :key="option.value" @click="input = option.label">
                    {{ option.label }}
                  </button>
                </div>
              </div>
              <p v-if="message.presentation.closing" class="recommendation-copy recommendation-copy--closing">
                {{ message.presentation.closing }}
              </p>
            </div>
          </div>
        </template>
      </div>
      <aside class="tool-rail"><div class="tool-rail-heading"><el-icon><Tools /></el-icon><span>工具活动</span></div><div v-if="!tools.length" class="tool-idle">等待需要实时数据的请求</div><div v-for="tool in tools" :key="tool.id" class="tool-item"><span class="tool-dot" :class="`is-${tool.status}`"></span><div><strong>{{ tool.name }}</strong><span>{{ tool.status === 'running' ? '执行中' : tool.status === 'success' ? '已完成' : '失败' }}</span></div></div></aside>
    </div>
    <div class="composer"><el-input v-model="input" type="textarea" :autosize="{ minRows: 1, maxRows: 5 }" maxlength="2000" resize="none" placeholder="询问设备、租期、库存或报价" @keydown.enter.exact.prevent="send" /><el-button v-if="running" class="composer-button" :icon="CircleClose" circle type="danger" title="停止" aria-label="停止生成" @click="stop" /><el-button v-else class="composer-button" :icon="Promotion" circle type="primary" :disabled="!canSend" title="发送" aria-label="发送消息" @click="send" /></div>
    <QuickBookingDialog v-model="bookingOpen" :product="bookingProduct" :initial-period="bookingPeriod" />
  </section>
</template>
