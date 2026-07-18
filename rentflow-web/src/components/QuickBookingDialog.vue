<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Clock } from '@element-plus/icons-vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { catalogApi, orderApi, quoteApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Availability, Quote, RecommendationCard, RentalPeriodValue } from '@/types'
import {
  apiErrorMessage,
  defaultRentalPeriod,
  formatDateTime,
  formatMoney,
  isDateBeforeRentalStartInShanghai,
  newIdempotencyKey,
  toDateRange,
} from '@/utils'

const props = defineProps<{
  modelValue: boolean
  product?: RecommendationCard
  initialPeriod?: RentalPeriodValue | null
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const router = useRouter()
const auth = useAuthStore()
const period = ref<[Date, Date] | null>(defaultRentalPeriod())
const availability = ref<Availability>()
const quote = ref<Quote>()
const flowLoading = ref(false)
const reserving = ref(false)
const quoteSeconds = ref(0)
let orderKey: string | null = null
let timer: number | undefined

const quoteExpired = computed(() => quote.value ? quoteSeconds.value <= 0 : false)
const canReserve = computed(() => Boolean(
  quote.value && availability.value?.available && !quoteExpired.value,
))

function updateQuoteClock() {
  quoteSeconds.value = quote.value
    ? Math.max(0, Math.ceil((new Date(quote.value.expiresAt).getTime() - Date.now()) / 1000))
    : 0
}

function close() {
  emit('update:modelValue', false)
}

async function checkAndQuote() {
  const selected = toDateRange(period.value)
  if (!selected || !props.product) return ElMessage.warning('请选择完整租期')
  flowLoading.value = true
  availability.value = undefined
  quote.value = undefined
  orderKey = null
  try {
    const [availabilityResult, quoteResult] = await Promise.all([
      catalogApi.availability(props.product.productId, selected.startDate, selected.endDate),
      quoteApi.create(props.product.productId, selected.startDate, selected.endDate),
    ])
    availability.value = availabilityResult
    quote.value = quoteResult
    updateQuoteClock()
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause))
  } finally {
    flowLoading.value = false
  }
}

async function reserve() {
  if (!quote.value || !canReserve.value) return
  reserving.value = true
  orderKey ||= newIdempotencyKey()
  try {
    const order = await orderApi.create(quote.value.quoteId, orderKey)
    orderKey = null
    close()
    await router.push(`/orders/${order.orderId}/confirm`)
  } catch (cause) {
    ElMessage.error(apiErrorMessage(cause))
  } finally {
    reserving.value = false
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    const initial = props.initialPeriod
    period.value = initial
      ? [new Date(`${initial.startDate}T00:00:00`), new Date(`${initial.endDate}T00:00:00`)]
      : defaultRentalPeriod()
    availability.value = undefined
    quote.value = undefined
    orderKey = null
    await nextTick()
    if (initial) await checkAndQuote()
  },
)

timer = window.setInterval(updateQuoteClock, 1000)
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="min(560px, calc(100vw - 24px))"
    destroy-on-close
    append-to-body
    class="quick-booking-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <div v-if="product" class="quick-booking-product">
        <span>{{ product.brand }} · {{ product.model }}</span>
        <strong>{{ product.name }}</strong>
        <div>{{ formatMoney(product.dailyRate) }}<small>/天</small></div>
      </div>
    </template>

    <div v-if="product" class="quick-booking-tool">
      <div class="tool-heading"><span>租期与报价</span><el-icon><Clock /></el-icon></div>
      <label class="field-label">租赁日期</label>
      <el-date-picker
        v-model="period"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        :disabled-date="isDateBeforeRentalStartInShanghai"
        :clearable="false"
      />
      <p class="field-help">按自然日整天起租，结束日期包含在租期内。</p>
      <el-button class="full-button" type="primary" :loading="flowLoading" @click="checkAndQuote">
        查询库存并获取报价
      </el-button>

      <div v-if="availability" class="availability-line" :class="availability.available ? 'is-available' : 'is-unavailable'">
        <el-icon><Check /></el-icon>
        <div>
          <strong>{{ availability.available ? `当前可租 ${availability.availableCount} 台` : '当前租期暂无可租' }}</strong>
          <span>核验于 {{ formatDateTime(availability.checkedAt, auth.user?.timezone) }}</span>
        </div>
      </div>

      <div v-if="quote" class="quote-result">
        <div class="quote-expiry" :class="{ expired: quoteExpired }">
          <span>{{ quoteExpired ? '报价已过期' : '报价有效期' }}</span>
          <strong>{{ quoteExpired ? '请重新查询' : `${Math.floor(quoteSeconds / 60).toString().padStart(2, '0')}:${(quoteSeconds % 60).toString().padStart(2, '0')}` }}</strong>
        </div>
        <PriceBreakdown :snapshot="quote.priceSnapshot" />
        <el-button class="full-button" type="primary" :disabled="!canReserve" :loading="reserving" @click="reserve">
          立即预订
        </el-button>
        <p class="quote-note">预订后将锁定设备容量 15 分钟，并创建一张待确认订单。</p>
      </div>
    </div>
  </el-dialog>
</template>
