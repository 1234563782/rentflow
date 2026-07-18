<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Clock, Warning } from '@element-plus/icons-vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Order } from '@/types'
import { apiErrorMessage, formatDate, isNetworkError, newIdempotencyKey } from '@/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const order = ref<Order>()
const loading = ref(true)
const submitting = ref(false)
const cancelling = ref(false)
const error = ref('')
const secondsLeft = ref(0)
let timer: number | undefined

const orderId = computed(() => String(route.params.orderId))
const confirmKeyName = computed(() => `rentflow.attempt.confirm.${orderId.value}`)
const cancelKeyName = computed(() => `rentflow.attempt.cancel.${orderId.value}`)
const isPending = computed(() => order.value?.effectiveStatus === 'PENDING_CONFIRMATION' && secondsLeft.value > 0)
const countdown = computed(() => `${Math.floor(secondsLeft.value / 60).toString().padStart(2, '0')}:${(secondsLeft.value % 60).toString().padStart(2, '0')}`)

function tick() {
  secondsLeft.value = order.value
    ? Math.max(0, Math.ceil((new Date(order.value.expiresAt).getTime() - Date.now()) / 1000))
    : 0
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    order.value = await orderApi.get(orderId.value)
    tick()
  } catch (cause) {
    error.value = apiErrorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function confirmOrder() {
  if (!order.value || !isPending.value) return
  submitting.value = true
  let key = sessionStorage.getItem(confirmKeyName.value)
  if (!key) {
    key = newIdempotencyKey()
    sessionStorage.setItem(confirmKeyName.value, key)
  }
  try {
    order.value = await orderApi.confirm(order.value.orderId, key)
    sessionStorage.removeItem(confirmKeyName.value)
    ElMessage.success('订单确认成功')
    await router.replace(`/orders/${order.value.orderId}`)
  } catch (cause) {
    if (isNetworkError(cause)) ElMessage.warning('网络响应中断，可使用原请求安全重试')
    else ElMessage.error(apiErrorMessage(cause))
  } finally {
    submitting.value = false
  }
}

async function cancelOrder() {
  if (!order.value || !isPending.value) return
  const confirmed = await ElMessageBox.confirm(
    '取消后设备会立即释放，订单无法再次确认。',
    '取消待确认订单',
    { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '继续保留' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return

  cancelling.value = true
  let key = sessionStorage.getItem(cancelKeyName.value)
  if (!key) {
    key = newIdempotencyKey()
    sessionStorage.setItem(cancelKeyName.value, key)
  }
  try {
    order.value = await orderApi.cancel(order.value.orderId, key)
    sessionStorage.removeItem(cancelKeyName.value)
    sessionStorage.removeItem(confirmKeyName.value)
    tick()
    ElMessage.success('订单已取消，设备预占已释放')
    await router.replace(`/orders/${order.value.orderId}`)
  } catch (cause) {
    if (isNetworkError(cause)) ElMessage.warning('网络响应中断，可使用原请求安全重试')
    else ElMessage.error(apiErrorMessage(cause))
  } finally {
    cancelling.value = false
  }
}

onMounted(() => {
  void load()
  timer = window.setInterval(tick, 1000)
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <section class="content-page narrow-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.push('/orders')">返回订单</el-button>
    <header class="page-heading">
      <div><span class="eyebrow">待确认订单</span><h1>确认本次设备预订</h1><p>设备与当前价格会保留至倒计时结束。</p></div>
    </header>
    <div v-if="loading" class="checkout-sheet"><el-skeleton animated :rows="10" /></div>
    <div v-else-if="error" class="state-panel"><h2>订单加载失败</h2><p>{{ error }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="order" class="checkout-sheet">
      <div class="reservation-clock" :class="{ expired: !isPending }">
        <el-icon><component :is="isPending ? Clock : Warning" /></el-icon>
        <div><span>{{ isPending ? '订单确认剩余' : '当前订单状态' }}</span><strong>{{ isPending ? countdown : order.effectiveStatus }}</strong></div>
      </div>
      <div class="checkout-section">
        <span class="section-label">已锁定商品容量</span>
        <h2>{{ order.productName }}</h2>
        <p>{{ order.productModel }} · {{ order.equipmentDisplayCode ? `设备编号 ${order.equipmentDisplayCode}` : '具体设备将在出库前分配' }}</p>
      </div>
      <div class="checkout-section period-summary">
        <div><span>开始日期</span><strong>{{ formatDate(order.startDate) }}</strong></div>
        <div><span>结束日期</span><strong>{{ formatDate(order.endDate) }}</strong></div>
      </div>
      <div class="checkout-section"><span class="section-label">冻结价格</span><PriceBreakdown :snapshot="order.priceSnapshot" /></div>
      <div class="checkout-actions">
        <el-button :loading="cancelling" :disabled="!isPending" @click="cancelOrder">取消订单</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!isPending" @click="confirmOrder">确认预订</el-button>
      </div>
    </div>
  </section>
</template>
