<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Clock, Warning } from '@element-plus/icons-vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { catalogApi, orderApi, reservationApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ProductDetail, Reservation } from '@/types'
import { apiErrorMessage, formatDateTime, isNetworkError, newIdempotencyKey } from '@/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const reservation = ref<Reservation>()
const product = ref<ProductDetail>()
const loading = ref(true)
const submitting = ref(false)
const releasing = ref(false)
const error = ref('')
const secondsLeft = ref(0)
let timer: number | undefined

const attemptKeyName = computed(() => `rentflow.attempt.order.${route.params.reservationId}`)
const isActive = computed(() => reservation.value?.effectiveStatus === 'ACTIVE' && secondsLeft.value > 0)
const countdown = computed(() => `${Math.floor(secondsLeft.value / 60).toString().padStart(2, '0')}:${(secondsLeft.value % 60).toString().padStart(2, '0')}`)

function tick() {
  secondsLeft.value = reservation.value ? Math.max(0, Math.ceil((new Date(reservation.value.expiresAt).getTime() - Date.now()) / 1000)) : 0
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    reservation.value = await reservationApi.get(String(route.params.reservationId))
    product.value = await catalogApi.product(reservation.value.productId)
    tick()
  } catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}

async function createOrder() {
  if (!reservation.value || !isActive.value) return
  submitting.value = true
  let key = sessionStorage.getItem(attemptKeyName.value)
  if (!key) {
    key = newIdempotencyKey()
    sessionStorage.setItem(attemptKeyName.value, key)
  }
  try {
    const order = await orderApi.create(reservation.value.reservationId, key)
    sessionStorage.removeItem(attemptKeyName.value)
    ElMessage.success('订单创建成功')
    await router.replace(`/orders/${order.orderId}`)
  } catch (cause) {
    if (isNetworkError(cause)) ElMessage.warning('网络响应中断，可使用原请求安全重试')
    else ElMessage.error(apiErrorMessage(cause))
  } finally { submitting.value = false }
}

async function release() {
  if (!reservation.value) return
  await ElMessageBox.confirm('释放后需要重新获取报价和预占，确认继续？', '释放预占', { type: 'warning', confirmButtonText: '确认释放', cancelButtonText: '暂不释放' }).catch(() => false)
    .then(async (confirmed) => {
      if (!confirmed) return
      releasing.value = true
      try {
        reservation.value = await reservationApi.release(reservation.value!.reservationId)
        sessionStorage.removeItem(attemptKeyName.value)
        tick()
        ElMessage.success('预占已释放')
      } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
      finally { releasing.value = false }
    })
}

onMounted(() => { void load(); timer = window.setInterval(tick, 1000) })
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <section class="content-page narrow-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.back()">返回</el-button>
    <header class="page-heading"><div><span class="eyebrow">下单确认</span><h1>核对预占与价格</h1><p>订单将完整保存当前商品与价格快照。</p></div></header>
    <div v-if="loading" class="checkout-sheet"><el-skeleton animated :rows="10" /></div>
    <div v-else-if="error" class="state-panel"><h2>预占加载失败</h2><p>{{ error }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="reservation" class="checkout-sheet">
      <div class="reservation-clock" :class="{ expired: !isActive }">
        <el-icon><component :is="isActive ? Clock : Warning" /></el-icon>
        <div><span>{{ isActive ? '设备预占剩余' : '预占已失效' }}</span><strong>{{ isActive ? countdown : reservation.effectiveStatus }}</strong></div>
      </div>
      <div class="checkout-section">
        <span class="section-label">商品</span>
        <h2>{{ product?.name || reservation.productId }}</h2>
        <p>{{ product?.brand }} {{ product?.model }} · 设备编号 {{ reservation.equipmentDisplayCode }}</p>
      </div>
      <div class="checkout-section period-summary">
        <div><span>开始时间</span><strong>{{ formatDateTime(reservation.startAt, auth.user?.timezone) }}</strong></div>
        <div><span>结束时间</span><strong>{{ formatDateTime(reservation.endAt, auth.user?.timezone) }}</strong></div>
      </div>
      <div class="checkout-section"><span class="section-label">价格明细</span><PriceBreakdown :snapshot="reservation.priceSnapshot" /></div>
      <div class="checkout-actions">
        <el-button :loading="releasing" :disabled="reservation.effectiveStatus === 'CONSUMED'" @click="release">释放预占</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!isActive" @click="createOrder">确认下单</el-button>
      </div>
    </div>
  </section>
</template>
