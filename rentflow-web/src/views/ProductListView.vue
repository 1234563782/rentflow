<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Refresh, Search } from '@element-plus/icons-vue'
import { catalogApi, storeApi } from '@/services/api'
import type { Category, Page, ProductSummary, StoreSku } from '@/types'
import { apiErrorMessage, formatMoney } from '@/utils'
import ProductVisual from '@/components/ProductVisual.vue'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const categories = ref<Category[]>([])
const result = ref<Page<ProductSummary>>({ items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 })
const skuMap = ref<Record<string, StoreSku[]>>({})
const filters = reactive({ keyword: '', categoryId: '', page: 0, size: 12 })

async function load() {
  loading.value = true
  error.value = ''
  try {
    result.value = await catalogApi.products({
      keyword: filters.keyword.trim() || undefined,
      categoryId: filters.categoryId || undefined,
      page: filters.page,
      size: filters.size,
    })
    const entries = await Promise.all(result.value.items.map(async product => {
      try { return [product.productId, await storeApi.skus(product.productId)] as const }
      catch { return [product.productId, []] as const }
    }))
    skuMap.value = Object.fromEntries(entries)
  } catch (cause) {
    error.value = apiErrorMessage(cause)
  } finally {
    loading.value = false
  }
}

function lowestPrice(productId: string) {
  const prices = (skuMap.value[productId] || []).map(sku => Number(sku.salePrice))
  return prices.length ? Math.min(...prices).toFixed(2) : null
}
function stock(productId: string) {
  return (skuMap.value[productId] || []).reduce((sum, sku) => sum + sku.availableQuantity, 0)
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
      <div><span class="eyebrow">数码潮玩商城</span><h1>发现值得入手的新装备</h1><p>手机、影像、电脑与智能设备，按真实 SKU 库存下单。</p></div>
      <el-button :icon="ChatDotRound" @click="router.push('/gearmate')">问 GearMate</el-button>
    </header>

    <form class="filter-band store-filter-band" @submit.prevent="submitSearch">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索名称、品牌或型号" />
      <el-select v-model="filters.categoryId" clearable placeholder="全部类目">
        <el-option v-for="category in categories" :key="category.categoryId" :label="category.name" :value="category.categoryId" />
      </el-select>
      <el-button type="primary" native-type="submit" :loading="loading">搜索商品</el-button>
    </form>

    <div class="result-meta"><span>{{ result.totalElements }} 件商品</span><span>库存与售价来自当前 SKU</span></div>

    <div v-if="error" class="state-panel">
      <el-icon><Refresh /></el-icon><h2>暂时无法加载商品</h2><p>{{ error }}</p><el-button @click="load">重新加载</el-button>
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
            <div v-if="lowestPrice(product.productId)"><strong>{{ formatMoney(lowestPrice(product.productId)!) }}</strong><span>起</span></div>
            <div v-else><strong>查看详情</strong></div>
            <el-tag :type="stock(product.productId) > 0 ? 'success' : 'info'" effect="plain">
              {{ stock(product.productId) > 0 ? `现货 ${stock(product.productId)} 件` : '暂时缺货' }}
            </el-tag>
          </div>
        </div>
      </article>
    </div>
    <el-empty v-else description="没有找到符合条件的商品" />

    <el-pagination v-if="result.totalPages > 1" class="page-pagination" background layout="prev, pager, next" :current-page="result.page + 1" :page-size="result.size" :total="result.totalElements" @current-change="changePage" />
  </section>
</template>
