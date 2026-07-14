<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Box, Refresh } from '@element-plus/icons-vue'
import { orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Order, Page } from '@/types'
import { apiErrorMessage, formatDateTime, formatMoney } from '@/utils'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const result = ref<Page<Order>>({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
const query = reactive({ status: '', page: 0, size: 10 })

async function load() {
  loading.value = true
  error.value = ''
  try { result.value = await orderApi.list({ status: query.status || undefined, page: query.page, size: query.size }) }
  catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}
function filter() { query.page = 0; void load() }
function changePage(page: number) { query.page = page - 1; void load() }
onMounted(load)
</script>

<template>
  <section class="content-page">
    <header class="page-heading"><div><span class="eyebrow">我的租赁</span><h1>订单</h1><p>查看已创建订单及其状态变化。</p></div>
      <el-select v-model="query.status" placeholder="全部状态" clearable @change="filter"><el-option label="已创建" value="CREATED" /></el-select>
    </header>
    <div class="result-meta"><span>{{ result.totalElements }} 笔订单</span></div>
    <div v-if="error" class="state-panel"><el-icon><Refresh /></el-icon><h2>订单加载失败</h2><p>{{ error }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="loading" class="order-list"><div v-for="item in 4" :key="item" class="order-row"><el-skeleton animated :rows="2" /></div></div>
    <div v-else-if="result.items.length" class="order-list">
      <article v-for="order in result.items" :key="order.orderId" class="order-row" @click="router.push(`/orders/${order.orderId}`)">
        <div class="order-icon"><el-icon><Box /></el-icon></div>
        <div class="order-main"><span>{{ order.productModel }} · {{ order.equipmentDisplayCode }}</span><h2>{{ order.productName }}</h2><p>{{ formatDateTime(order.startAt, auth.user?.timezone) }} 至 {{ formatDateTime(order.endAt, auth.user?.timezone) }}</p></div>
        <div class="order-amount"><el-tag type="success" effect="plain">已创建</el-tag><strong>{{ formatMoney(order.priceSnapshot.totalAmount) }}</strong><span>{{ formatDateTime(order.createdAt, auth.user?.timezone) }}</span></div>
      </article>
    </div>
    <el-empty v-else description="还没有订单" />
    <el-pagination v-if="result.totalPages > 1" class="page-pagination" background layout="prev, pager, next" :current-page="result.page + 1" :page-size="result.size" :total="result.totalElements" @current-change="changePage" />
  </section>
</template>
