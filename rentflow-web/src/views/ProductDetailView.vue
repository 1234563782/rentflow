<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Clock, RefreshRight } from '@element-plus/icons-vue'
import ProductVisual from '@/components/ProductVisual.vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { catalogApi, orderApi, quoteApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Availability, ProductDetail, Quote } from '@/types'
import { apiErrorMessage, defaultRentalPeriod, formatDateTime, formatMoney, newIdempotencyKey, toIsoPeriod } from '@/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const product = ref<ProductDetail>()
const loading = ref(true)
const flowLoading = ref(false)
const reserving = ref(false)
const error = ref('')
const period = ref<[Date, Date] | null>(defaultRentalPeriod())
const availability = ref<Availability>()
const quote = ref<Quote>()
const quoteSeconds = ref(0)
let orderKey: string | null = null
let timer: number | undefined

const quoteExpired = computed(() => quote.value ? quoteSeconds.value <= 0 : false)
const canReserve = computed(() => Boolean(quote.value && availability.value?.available && !quoteExpired.value))

function updateQuoteClock() {
  quoteSeconds.value = quote.value ? Math.max(0, Math.ceil((new Date(quote.value.expiresAt).getTime() - Date.now()) / 1000)) : 0
}

async function loadProduct() {
  loading.value = true
  error.value = ''
  try { product.value = await catalogApi.product(String(route.params.productId)) }
  catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}

async function checkAndQuote() {
  const selected = toIsoPeriod(period.value)
  if (!selected || !product.value) return ElMessage.warning('请选择完整租期')
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  flowLoading.value = true
  quote.value = undefined
  availability.value = undefined
  orderKey = null
  try {
    const [availabilityResult, quoteResult] = await Promise.all([
      catalogApi.availability(product.value.productId, selected.startAt, selected.endAt),
      quoteApi.create(product.value.productId, selected.startAt, selected.endAt),
    ])
    availability.value = availabilityResult
    quote.value = quoteResult
    updateQuoteClock()
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { flowLoading.value = false }
}

async function reserve() {
  if (!quote.value || !canReserve.value) return
  reserving.value = true
  orderKey ||= newIdempotencyKey()
  try {
    const order = await orderApi.create(quote.value.quoteId, orderKey)
    orderKey = null
    await router.push(`/orders/${order.orderId}/confirm`)
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { reserving.value = false }
}

onMounted(() => {
  void loadProduct()
  timer = window.setInterval(updateQuoteClock, 1000)
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <section class="content-page detail-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.back()">返回设备列表</el-button>
    <div v-if="loading" class="detail-grid"><el-skeleton animated :rows="10" /></div>
    <div v-else-if="error" class="state-panel"><h2>设备信息加载失败</h2><p>{{ error }}</p><el-button :icon="RefreshRight" @click="loadProduct">重试</el-button></div>
    <template v-else-if="product">
      <div class="detail-grid">
        <div class="detail-product">
          <ProductVisual :name="product.name" :brand="product.brand" />
          <span class="eyebrow">{{ product.brand }} · {{ product.model }}</span>
          <h1>{{ product.name }}</h1>
          <p class="product-description">{{ product.description }}</p>
          <dl class="spec-list">
            <div><dt>型号</dt><dd>{{ product.model }}</dd></div>
            <div><dt>参考日租</dt><dd>{{ formatMoney(product.dailyRate) }}</dd></div>
            <div><dt>固定押金</dt><dd>{{ formatMoney(product.fixedDeposit) }}</dd></div>
          </dl>
        </div>

        <aside class="booking-tool">
          <div class="tool-heading"><span>租期与报价</span><el-icon><Clock /></el-icon></div>
          <label class="field-label">租赁时间</label>
          <el-date-picker v-model="period" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" format="YYYY-MM-DD HH:mm" :clearable="false" />
          <p class="field-help">最短可租 1 小时，最小按 1 天计费，最长 30 天。</p>
          <el-button class="full-button" type="primary" :loading="flowLoading" @click="checkAndQuote">查询库存并获取报价</el-button>

          <div v-if="availability" class="availability-line" :class="availability.available ? 'is-available' : 'is-unavailable'">
            <el-icon><Check /></el-icon>
            <div><strong>{{ availability.available ? `当前可租 ${availability.availableCount} 台` : '当前租期暂无可租' }}</strong><span>核验于 {{ formatDateTime(availability.checkedAt, auth.user?.timezone) }}</span></div>
          </div>

          <div v-if="quote" class="quote-result">
            <div class="quote-expiry" :class="{ expired: quoteExpired }">
              <span>{{ quoteExpired ? '报价已过期' : '报价有效期' }}</span>
              <strong>{{ quoteExpired ? '请重新查询' : `${Math.floor(quoteSeconds / 60).toString().padStart(2, '0')}:${(quoteSeconds % 60).toString().padStart(2, '0')}` }}</strong>
            </div>
            <PriceBreakdown :snapshot="quote.priceSnapshot" />
            <el-button class="full-button" type="primary" :disabled="!canReserve" :loading="reserving" @click="reserve">立即预订</el-button>
            <p class="quote-note">预订后将锁定具体设备 15 分钟，并创建一张待确认订单。</p>
          </div>
        </aside>
      </div>
    </template>
  </section>
</template>
