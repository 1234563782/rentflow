<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Clock, RefreshRight } from '@element-plus/icons-vue'
import ProductVisual from '@/components/ProductVisual.vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { catalogApi, orderApi, quoteApi, reviewApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Availability, ProductDetail, ProductReview, Quote, ReviewStatistics } from '@/types'
import { apiErrorMessage, defaultRentalPeriod, formatDateTime, formatMoney, newIdempotencyKey, toIsoPeriod } from '@/utils'

const REVIEW_PAGE_SIZE = 10

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
const reviews = ref<ProductReview[]>([])
const reviewStatistics = ref<ReviewStatistics>()
const reviewPage = ref(1)
const reviewTotal = ref(0)
const reviewsLoading = ref(true)
const reviewsError = ref('')
const reviewRating = ref(0)
const reviewContent = ref('')
const submittingReview = ref(false)
let orderKey: string | null = null
let reviewKey: string | null = null
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

async function loadReviews() {
  reviewsLoading.value = true
  reviewsError.value = ''
  try {
    const response = await reviewApi.list(String(route.params.productId), { page: reviewPage.value - 1, size: REVIEW_PAGE_SIZE })
    reviews.value = response.items
    reviewStatistics.value = response.statistics
    reviewTotal.value = response.totalElements
  } catch (cause) {
    reviewsError.value = apiErrorMessage(cause)
  } finally {
    reviewsLoading.value = false
  }
}

function changeReviewPage(page: number) {
  reviewPage.value = page
  void loadReviews()
}

function reviewEligibilityMessage(cause: unknown) {
  const status = (cause as { response?: { status?: number } }).response?.status
  return status === 403 ? '仅确认收货的用户可以发布评价。' : apiErrorMessage(cause)
}

async function submitReview() {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!reviewRating.value) return ElMessage.warning('请选择 1 至 5 星评分')
  if (!reviewContent.value.trim()) return ElMessage.warning('请填写评价内容')

  submittingReview.value = true
  reviewKey ||= newIdempotencyKey()
  try {
    await reviewApi.create(String(route.params.productId), {
      rating: reviewRating.value,
      content: reviewContent.value.trim(),
    }, reviewKey)
    reviewKey = null
    reviewRating.value = 0
    reviewContent.value = ''
    reviewPage.value = 1
    ElMessage.success('评价发布成功')
    await loadReviews()
  } catch (cause) {
    ElMessage.error(reviewEligibilityMessage(cause))
  } finally {
    submittingReview.value = false
  }
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
  void loadReviews()
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

          <section id="reviews" class="review-section" aria-labelledby="review-heading">
            <header class="review-heading">
              <div><span class="eyebrow">用户评价</span><h2 id="review-heading">来自真实租赁用户的反馈</h2></div>
              <div v-if="reviewStatistics" class="review-summary">
                <el-rate :model-value="reviewStatistics.averageRating" disabled allow-half />
                <strong>{{ reviewStatistics.averageRating.toFixed(1) }}</strong><span>{{ reviewStatistics.totalReviews }} 条评价</span>
              </div>
            </header>

            <div v-if="auth.isAuthenticated" class="review-composer">
              <div class="review-composer-heading"><strong>写评价</strong><span>确认收货后即可发布</span></div>
              <el-rate v-model="reviewRating" aria-label="评分" />
              <el-input v-model="reviewContent" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="分享设备使用感受，帮助下一位租户做决定。" />
              <div class="review-composer-actions"><span>请文明评价，发布后不可修改。</span><el-button type="primary" :loading="submittingReview" @click="submitReview">发布评价</el-button></div>
            </div>
            <div v-else class="review-login-prompt"><div><strong>登录后写评价</strong><span>确认收货的用户可分享真实使用体验。</span></div><el-button @click="router.push({ name: 'login', query: { redirect: route.fullPath } })">去登录</el-button></div>

            <div v-if="reviewsLoading" class="review-skeleton"><el-skeleton animated :rows="5" /></div>
            <div v-else-if="reviewsError" class="review-error"><strong>评价加载失败</strong><span>{{ reviewsError }}</span><el-button text type="primary" :icon="RefreshRight" @click="loadReviews">重新加载</el-button></div>
            <div v-else-if="reviews.length" class="review-list">
              <article v-for="review in reviews" :key="review.reviewId" class="review-item">
                <header><strong>{{ review.reviewerName }}</strong><span>{{ formatDateTime(review.createdAt, auth.user?.timezone) }}</span></header>
                <el-rate :model-value="review.rating" disabled />
                <p>{{ review.content }}</p>
              </article>
            </div>
            <el-empty v-else description="暂时还没有评价，期待第一条真实反馈。" :image-size="76" />
            <el-pagination v-if="reviewTotal > REVIEW_PAGE_SIZE" class="review-pagination" background layout="prev, pager, next" :current-page="reviewPage" :page-size="REVIEW_PAGE_SIZE" :total="reviewTotal" @current-change="changeReviewPage" />
          </section>
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
            <div class="quote-expiry" :class="{ expired: quoteExpired }"><span>{{ quoteExpired ? '报价已过期' : '报价有效期' }}</span><strong>{{ quoteExpired ? '请重新查询' : `${Math.floor(quoteSeconds / 60).toString().padStart(2, '0')}:${(quoteSeconds % 60).toString().padStart(2, '0')}` }}</strong></div>
            <PriceBreakdown :snapshot="quote.priceSnapshot" />
            <el-button class="full-button" type="primary" :disabled="!canReserve" :loading="reserving" @click="reserve">立即预订</el-button>
            <p class="quote-note">预订后将锁定具体设备 15 分钟，并创建一张待确认订单。</p>
          </div>
        </aside>
      </div>
    </template>
  </section>
</template>
