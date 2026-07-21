<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Goods, RefreshRight } from '@element-plus/icons-vue'
import ProductVisual from '@/components/ProductVisual.vue'
import { catalogApi, storeApi, storeReviewApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ProductDetail, ProductReview, StoreReviewPage, StoreSku } from '@/types'
import { apiErrorMessage, formatDateTime, formatMoney, newIdempotencyKey } from '@/utils'

const REVIEW_PAGE_SIZE = 10
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const product = ref<ProductDetail>()
const skus = ref<StoreSku[]>([])
const selectedSkuId = ref('')
const quantity = ref(1)
const loading = ref(true)
const error = ref('')
const reviews = ref<ProductReview[]>([])
const reviewMeta = ref<StoreReviewPage['statistics']>({ averageRating: 0, reviewCount: 0 })
const reviewPage = ref(1)
const reviewTotal = ref(0)
const reviewsLoading = ref(true)
const reviewRating = ref(0)
const reviewContent = ref('')
const submittingReview = ref(false)
let reviewKey: string | null = null

const selectedSku = computed(() => skus.value.find(sku => sku.skuId === selectedSkuId.value))
const canBuy = computed(() => Boolean(selectedSku.value?.enabled && selectedSku.value.availableQuantity >= quantity.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const productId = String(route.params.productId)
    const [detail, skuItems] = await Promise.all([catalogApi.product(productId), storeApi.skus(productId)])
    product.value = detail
    skus.value = skuItems
    selectedSkuId.value = skuItems.find(sku => sku.availableQuantity > 0)?.skuId || skuItems[0]?.skuId || ''
  } catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}

async function loadReviews() {
  reviewsLoading.value = true
  try {
    const response = await storeReviewApi.list(String(route.params.productId), { page: reviewPage.value - 1, size: REVIEW_PAGE_SIZE })
    reviews.value = response.items
    reviewMeta.value = response.statistics
    reviewTotal.value = response.totalElements
  } catch { reviews.value = [] }
  finally { reviewsLoading.value = false }
}

async function buyNow() {
  if (!selectedSku.value) return ElMessage.warning('请选择商品规格')
  if (!canBuy.value) return ElMessage.warning('当前规格库存不足')
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  await router.push({ name: 'checkout', params: { skuId: selectedSku.value.skuId }, query: { quantity: quantity.value } })
}

async function submitReview() {
  if (!auth.isAuthenticated) return void router.push({ name: 'login', query: { redirect: route.fullPath } })
  if (!reviewRating.value || !reviewContent.value.trim()) return ElMessage.warning('请填写评分和评价内容')
  submittingReview.value = true
  reviewKey ||= newIdempotencyKey()
  try {
    await storeReviewApi.create(String(route.params.productId), { rating: reviewRating.value, content: reviewContent.value.trim() }, reviewKey)
    reviewKey = null
    reviewRating.value = 0
    reviewContent.value = ''
    reviewPage.value = 1
    ElMessage.success('评价发布成功')
    await loadReviews()
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { submittingReview.value = false }
}

function changeReviewPage(page: number) { reviewPage.value = page; void loadReviews() }
function specText(specs: Record<string, unknown>) {
  const values = Object.entries(specs).map(([key, value]) => `${key}: ${String(value)}`)
  return values.length ? values.join(' · ') : '标准配置'
}

onMounted(() => { void load(); void loadReviews() })
</script>

<template>
  <section class="content-page detail-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.back()">返回商品列表</el-button>
    <div v-if="loading" class="detail-grid"><el-skeleton animated :rows="10" /></div>
    <div v-else-if="error" class="state-panel"><h2>商品信息加载失败</h2><p>{{ error }}</p><el-button :icon="RefreshRight" @click="load">重试</el-button></div>
    <template v-else-if="product">
      <div class="detail-grid">
        <div class="detail-product">
          <ProductVisual :name="product.name" :brand="product.brand" />
          <span class="eyebrow">{{ product.brand }} · {{ product.model }}</span>
          <h1>{{ product.name }}</h1>
          <p class="product-description">{{ product.description }}</p>
          <dl class="spec-list">
            <div><dt>品牌</dt><dd>{{ product.brand }}</dd></div>
            <div><dt>型号</dt><dd>{{ product.model }}</dd></div>
            <div><dt>可选规格</dt><dd>{{ skus.length }} 种</dd></div>
          </dl>

          <section id="reviews" class="review-section" aria-labelledby="review-heading">
            <header class="review-heading">
              <div><span class="eyebrow">已购评价</span><h2 id="review-heading">真实购买用户的反馈</h2></div>
              <div class="review-summary"><el-rate :model-value="reviewMeta.averageRating" disabled allow-half /><strong>{{ reviewMeta.averageRating.toFixed(1) }}</strong><span>{{ reviewMeta.reviewCount }} 条</span></div>
            </header>
            <div v-if="auth.isAuthenticated" class="review-composer">
              <div class="review-composer-heading"><strong>写评价</strong><span>确认收货后可发布，每件订单商品限一次</span></div>
              <el-rate v-model="reviewRating" />
              <el-input v-model="reviewContent" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="分享实际使用体验" />
              <div class="review-composer-actions"><span></span><el-button type="primary" :loading="submittingReview" @click="submitReview">发布评价</el-button></div>
            </div>
            <div v-if="reviewsLoading"><el-skeleton animated :rows="4" /></div>
            <div v-else-if="reviews.length" class="review-list">
              <article v-for="review in reviews" :key="review.reviewId" class="review-item">
                <header><strong>{{ review.reviewerName }}</strong><span>{{ formatDateTime(review.createdAt, auth.user?.timezone) }}</span></header>
                <el-rate :model-value="review.rating" disabled /><p>{{ review.content }}</p>
              </article>
            </div>
            <el-empty v-else description="暂时还没有已购评价" :image-size="72" />
            <el-pagination v-if="reviewTotal > REVIEW_PAGE_SIZE" class="review-pagination" background layout="prev, pager, next" :current-page="reviewPage" :page-size="REVIEW_PAGE_SIZE" :total="reviewTotal" @current-change="changeReviewPage" />
          </section>
        </div>

        <aside class="purchase-tool">
          <div class="tool-heading"><span>选择规格</span><el-icon><Goods /></el-icon></div>
          <div v-if="skus.length" class="sku-options">
            <button v-for="sku in skus" :key="sku.skuId" type="button" class="sku-option" :class="{ active: selectedSkuId === sku.skuId }" :disabled="!sku.enabled" @click="selectedSkuId = sku.skuId">
              <span><strong>{{ sku.skuName }}</strong><small>{{ specText(sku.specs) }}</small></span>
              <span><strong>{{ formatMoney(sku.salePrice) }}</strong><small>库存 {{ sku.availableQuantity }}</small></span>
            </button>
          </div>
          <el-empty v-else description="该商品暂未配置 SKU" :image-size="64" />
          <div v-if="selectedSku" class="purchase-summary">
            <div><span>售价</span><strong>{{ formatMoney(selectedSku.salePrice) }}</strong></div>
            <div><span>数量</span><el-input-number v-model="quantity" :min="1" :max="Math.max(1, selectedSku.availableQuantity)" /></div>
          </div>
          <el-button class="full-button" type="primary" :disabled="!canBuy" @click="buyNow">{{ selectedSku?.availableQuantity ? '立即购买' : '暂时缺货' }}</el-button>
          <p class="purchase-note">提交订单后保留 15 分钟支付时间，超时自动释放库存。</p>
        </aside>
      </div>
    </template>
  </section>
</template>
