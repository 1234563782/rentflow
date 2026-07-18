<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, RefreshRight } from '@element-plus/icons-vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { OrderDetail, OrderStatus } from '@/types'
import { apiErrorMessage, formatDate, formatDateTime, newIdempotencyKey } from '@/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const order = ref<OrderDetail>()
const loading = ref(true)
const error = ref('')
const now = ref(Date.now())
const cancelling = ref(false)
const receiving = ref(false)
let timer: number | undefined

const statusMeta: Record<OrderStatus, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  PENDING_CONFIRMATION: { label: '待确认', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'success' },
  RECEIVED: { label: '已收货', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  EXPIRED: { label: '已过期', type: 'danger' },
}
const secondsLeft = computed(() => order.value?.effectiveStatus === 'PENDING_CONFIRMATION'
  ? Math.max(0, Math.ceil((new Date(order.value.expiresAt).getTime() - now.value) / 1000))
  : 0)
const countdown = computed(() => `${Math.floor(secondsLeft.value / 60).toString().padStart(2, '0')}:${(secondsLeft.value % 60).toString().padStart(2, '0')}`)
const needsUrgentConfirmation = computed(() => order.value?.effectiveStatus === 'PENDING_CONFIRMATION'
  && secondsLeft.value > 0 && secondsLeft.value <= 5 * 60)
const canReceive = computed(() => order.value?.effectiveStatus === 'CONFIRMED'
  && now.value >= new Date(`${order.value.startDate}T00:00:00`).getTime())

async function load() {
  loading.value = true
  error.value = ''
  try { order.value = await orderApi.get(String(route.params.orderId)) }
  catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}
async function cancelOrder() {
  if (!order.value || order.value.effectiveStatus !== 'PENDING_CONFIRMATION') return
  const confirmed = await ElMessageBox.confirm('取消后设备会立即释放。', '取消待确认订单', {
    type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '继续保留',
  }).then(() => true).catch(() => false)
  if (!confirmed) return
  cancelling.value = true
  const storageKey = `rentflow.attempt.cancel.${order.value.orderId}`
  const key = sessionStorage.getItem(storageKey) || newIdempotencyKey()
  sessionStorage.setItem(storageKey, key)
  try {
    await orderApi.cancel(order.value.orderId, key)
    sessionStorage.removeItem(storageKey)
    ElMessage.success('订单已取消')
    await load()
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { cancelling.value = false }
}

async function receiveOrder() {
  if (!order.value || !canReceive.value) return
  receiving.value = true
  const storageKey = `rentflow.attempt.receive.${order.value.orderId}`
  const key = sessionStorage.getItem(storageKey) || newIdempotencyKey()
  sessionStorage.setItem(storageKey, key)
  try {
    await orderApi.receive(order.value.orderId, key)
    sessionStorage.removeItem(storageKey)
    ElMessage.success('已确认收货')
    await load()
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { receiving.value = false }
}

onMounted(() => { void load(); timer = window.setInterval(() => { now.value = Date.now() }, 1000) })
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <section class="content-page narrow-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.push('/orders')">返回订单</el-button>
    <div v-if="loading" class="order-detail-sheet"><el-skeleton animated :rows="12" /></div>
    <div v-else-if="error" class="state-panel"><h2>订单加载失败</h2><p>{{ error }}</p><el-button :icon="RefreshRight" @click="load">重试</el-button></div>
    <template v-else-if="order">
      <header class="order-detail-header" :class="{ 'order-detail-header--urgent': needsUrgentConfirmation }">
        <div><span class="eyebrow">订单详情</span><h1>{{ order.productName }}</h1><p>订单号 {{ order.orderId }}</p></div>
        <div class="order-header-actions">
          <el-tag size="large" :type="statusMeta[order.effectiveStatus].type" effect="dark">{{ statusMeta[order.effectiveStatus].label }}</el-tag>
          <template v-if="order.effectiveStatus === 'PENDING_CONFIRMATION'">
            <span class="header-countdown" :class="{ 'header-countdown--urgent': needsUrgentConfirmation }">{{ countdown }}</span>
            <span v-if="needsUrgentConfirmation" class="urgent-confirmation">即将过期，请尽快确认</span>
            <el-button @click="cancelOrder" :loading="cancelling">取消</el-button>
            <el-button type="primary" @click="router.push(`/orders/${order.orderId}/confirm`)">继续确认</el-button>
          </template>
          <el-button v-else-if="canReceive" type="primary" :loading="receiving" @click="receiveOrder">确认收货</el-button>
          <el-button v-else-if="order.effectiveStatus === 'RECEIVED'" type="primary" plain @click="router.push(`/products/${order.productId}#reviews`)">去评价</el-button>
        </div>
      </header>
      <div class="order-detail-sheet">
        <div class="order-product-summary"><div class="order-icon"><el-icon><Check /></el-icon></div><div><span>{{ order.productModel }}</span><h2>{{ order.productName }}</h2><p>{{ order.equipmentDisplayCode ? `设备编号 ${order.equipmentDisplayCode}` : '具体设备将在出库前分配' }}</p></div></div>
        <div class="checkout-section period-summary"><div><span>开始日期</span><strong>{{ formatDate(order.startDate) }}</strong></div><div><span>结束日期</span><strong>{{ formatDate(order.endDate) }}</strong></div></div>
        <div class="checkout-section"><span class="section-label">订单金额</span><PriceBreakdown :snapshot="order.priceSnapshot" /></div>
        <div class="checkout-section"><span class="section-label">状态记录</span><el-timeline class="status-timeline"><el-timeline-item v-for="item in order.statusHistory" :key="item.createdAt" type="success" :timestamp="formatDateTime(item.createdAt, auth.user?.timezone)"><strong>{{ statusMeta[item.toStatus].label }}</strong><p>{{ item.reason || '状态已更新' }}</p></el-timeline-item></el-timeline></div>
      </div>
    </template>
  </section>
</template>
