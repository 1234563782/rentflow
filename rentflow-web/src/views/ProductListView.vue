<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Refresh, Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/services/api'
import type { Category, Page, ProductSummary } from '@/types'
import { apiErrorMessage, defaultRentalPeriod, formatMoney, isDateBeforeRentalStartInShanghai, toDateRange } from '@/utils'
import ProductVisual from '@/components/ProductVisual.vue'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const categories = ref<Category[]>([])
const result = ref<Page<ProductSummary>>({ items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 })
const filters = reactive({ keyword: '', categoryId: '', period: defaultRentalPeriod() as [Date, Date] | null, page: 0, size: 12 })
const disablePastDates = isDateBeforeRentalStartInShanghai

async function load() {
  loading.value = true
  error.value = ''
  const period = toDateRange(filters.period)
  try {
    result.value = await catalogApi.products({
      keyword: filters.keyword.trim() || undefined,
      categoryId: filters.categoryId || undefined,
      startDate: period?.startDate,
      endDate: period?.endDate,
      page: filters.page,
      size: filters.size,
    })
  } catch (cause) {
    error.value = apiErrorMessage(cause)
  } finally {
    loading.value = false
  }
}

function submitSearch() { filters.page = 0; void load() }
function changePage(page: number) { filters.page = page - 1; void load() }

onMounted(async () => {
  try { categories.value = await catalogApi.categories() } catch { categories.value = [] }
  await load()
})
</script>

<template>
  <section class="content-page">
    <header class="page-heading product-heading">
      <div><span class="eyebrow">设备目录</span><h1>选择下一次创作装备</h1><p>按租期查看实时库存，报价以提交时的价格快照为准。</p></div>
      <el-button :icon="ChatDotRound" @click="router.push('/gearmate')">问 GearMate</el-button>
    </header>

    <form class="filter-band" @submit.prevent="submitSearch">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索名称、品牌或型号" />
      <el-select v-model="filters.categoryId" clearable placeholder="全部类目">
        <el-option v-for="category in categories" :key="category.categoryId" :label="category.name" :value="category.categoryId" />
      </el-select>
      <el-date-picker v-model="filters.period" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" format="YYYY-MM-DD" :disabled-date="disablePastDates" :clearable="true" />
      <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
    </form>

    <div class="result-meta">
      <span>{{ result.totalElements }} 件设备</span>
      <span v-if="filters.period" class="period-hint">已按所选租期核验库存</span>
    </div>

    <div v-if="error" class="state-panel">
      <el-icon><Refresh /></el-icon><h2>暂时无法加载设备</h2><p>{{ error }}</p><el-button @click="load">重新加载</el-button>
    </div>
    <div v-else-if="loading" class="product-grid">
      <div v-for="item in 6" :key="item" class="product-card skeleton-card"><el-skeleton animated :rows="4" /></div>
    </div>
    <div v-else-if="result.items.length" class="product-grid">
      <article v-for="product in result.items" :key="product.productId" class="product-card" tabindex="0" @click="router.push(`/products/${product.productId}`)" @keyup.enter="router.push(`/products/${product.productId}`)">
        <ProductVisual :name="product.name" :brand="product.brand" />
        <div class="product-card-body">
          <span class="model-label">{{ product.brand }} · {{ product.model }}</span>
          <h2>{{ product.name }}</h2>
          <div class="product-card-footer">
            <div><strong>{{ formatMoney(product.dailyRate) }}</strong><span>/ 天</span></div>
            <el-tag v-if="product.availableCount !== undefined" :type="product.availableCount > 0 ? 'success' : 'danger'" effect="plain">
              {{ product.availableCount > 0 ? `可租 ${product.availableCount} 台` : '暂无可租' }}
            </el-tag>
          </div>
        </div>
      </article>
    </div>
    <el-empty v-else description="没有找到符合条件的设备" />

    <el-pagination v-if="result.totalPages > 1" class="page-pagination" background layout="prev, pager, next" :current-page="result.page + 1" :page-size="result.size" :total="result.totalElements" @current-change="changePage" />
  </section>
</template>
