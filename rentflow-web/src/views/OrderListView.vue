<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Box, Refresh } from '@element-plus/icons-vue'
import { storeApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Page, StoreOrder, StoreOrderStatus } from '@/types'
import { apiErrorMessage, formatDateTime, formatMoney } from '@/utils'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const result = ref<Page<StoreOrder>>({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
const query = reactive({ status: '', page: 0, size: 10 })
const now = ref(Date.now())
let timer: number | undefined

const statusMeta: Record<StoreOrderStatus, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  PENDING_PAYMENT: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'success' },
  SHIPPED: { label: '待收货', type: 'success' },
  RECEIVED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  CLOSED: { label: '已关闭', type: 'danger' },
}

async function load() {
  loading.value = true
  error.value = ''
  try { result.value = await storeApi.listOrders({ status: query.status || undefined, page: query.page, size: query.size }) }
  catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}
function filter() { query.page = 0; void load() }
function changePage(page: number) { query.page = page - 1; void load() }
function remaining(order: StoreOrder) {
  if (order.status !== 'PENDING_PAYMENT') return ''
  const seconds = Math.max(0, Math.ceil((new Date(order.paymentExpiresAt).getTime() - now.value) / 1000))
  return `剩余 ${Math.floor(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}`
}
function title(order: StoreOrder) { return order.items[0]?.productName || '商城订单' }
function subtitle(order: StoreOrder) {
  const first = order.items[0]
  if (!first) return '暂无商品明细'
  return `${first.skuName} × ${first.quantity}${order.items.length > 1 ? ` 等 ${order.items.length} 件商品` : ''}`
}

onMounted(() => { void load(); timer = window.setInterval(() => { now.value = Date.now() }, 1000) })
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <section class="content-page">
    <header class="page-heading"><div><span class="eyebrow">个人中心</span><h1>我的订单</h1><p>查看支付、发货、物流与收货状态。</p></div>
      <el-select v-model="query.status" placeholder="全部状态" clearable @change="filter">
        <el-option v-for="(meta, status) in statusMeta" :key="status" :label="meta.label" :value="status" />
      </el-select>
    </header>
    <div class="result-meta"><span>{{ result.totalElements }} 笔订单</span></div>
    <div v-if="error" class="state-panel"><el-icon><Refresh /></el-icon><h2>订单加载失败</h2><p>{{ error }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="loading" class="order-list"><div v-for="item in 4" :key="item" class="order-row"><el-skeleton animated :rows="2" /></div></div>
    <div v-else-if="result.items.length" class="order-list">
      <article v-for="order in result.items" :key="order.orderId" class="order-row" @click="router.push(`/orders/${order.orderId}`)">
        <div class="order-icon"><el-icon><Box /></el-icon></div>
        <div class="order-main"><span>{{ subtitle(order) }}</span><h2>{{ title(order) }}</h2><p>{{ formatDateTime(order.createdAt, auth.user?.timezone) }}</p></div>
        <div class="order-amount"><el-tag :type="statusMeta[order.status].type" effect="plain">{{ statusMeta[order.status].label }}</el-tag><strong>{{ formatMoney(order.payableAmount) }}</strong><span>{{ remaining(order) || `${order.items.length} 件商品` }}</span></div>
      </article>
    </div>
    <el-empty v-else description="还没有商城订单" />
    <el-pagination v-if="result.totalPages > 1" class="page-pagination" background layout="prev, pager, next" :current-page="result.page + 1" :page-size="result.size" :total="result.totalElements" @current-change="changePage" />
  </section>
</template>
