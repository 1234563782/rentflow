<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Box, RefreshRight, Van } from '@element-plus/icons-vue'
import { storeApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { StoreOrder, StoreOrderStatus } from '@/types'
import { apiErrorMessage, formatDateTime, formatMoney, newIdempotencyKey } from '@/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const order = ref<StoreOrder>()
const loading = ref(true)
const error = ref('')
const actionLoading = ref(false)

const statusMeta: Record<StoreOrderStatus, { label: string; type: 'success' | 'warning' | 'info' | 'danger'; description: string }> = {
  PENDING_PAYMENT: { label: '待支付', type: 'warning', description: '库存已预占，请在支付截止时间前完成支付' },
  PAID: { label: '待发货', type: 'success', description: '支付成功，正在等待商家发货' },
  SHIPPED: { label: '待收货', type: 'success', description: '商品已发出，请留意物流进度' },
  RECEIVED: { label: '已完成', type: 'success', description: '订单已确认收货' },
  CANCELLED: { label: '已取消', type: 'info', description: '订单已取消，预占库存已释放' },
  CLOSED: { label: '已关闭', type: 'danger', description: '订单支付超时，预占库存已释放' },
}
const firstProduct = computed(() => order.value?.items[0])

async function load() {
  loading.value = true
  error.value = ''
  try { order.value = await storeApi.order(String(route.params.orderId)) }
  catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}

async function runAction(action: 'pay' | 'cancel' | 'receive') {
  if (!order.value) return
  if (action === 'cancel') {
    const confirmed = await ElMessageBox.confirm('取消后会立即释放预占库存。', '取消订单', { type: 'warning' }).then(() => true).catch(() => false)
    if (!confirmed) return
  }
  const storageKey = `rentflow.attempt.store.${action}.${order.value.orderId}`
  const key = sessionStorage.getItem(storageKey) || newIdempotencyKey()
  sessionStorage.setItem(storageKey, key)
  actionLoading.value = true
  try {
    order.value = await storeApi[action](order.value.orderId, key)
    sessionStorage.removeItem(storageKey)
    ElMessage.success(action === 'pay' ? '模拟支付成功' : action === 'cancel' ? '订单已取消' : '已确认收货')
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { actionLoading.value = false }
}

onMounted(() => { void load() })
</script>

<template>
  <section class="content-page narrow-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.push('/orders')">返回订单</el-button>
    <div v-if="loading" class="order-detail-sheet"><el-skeleton animated :rows="12" /></div>
    <div v-else-if="error" class="state-panel"><h2>订单加载失败</h2><p>{{ error }}</p><el-button :icon="RefreshRight" @click="load">重试</el-button></div>
    <template v-else-if="order">
      <header class="order-detail-header">
        <div><span class="eyebrow">订单详情</span><h1>{{ statusMeta[order.status].label }}</h1><p>{{ statusMeta[order.status].description }}</p></div>
        <div class="order-header-actions">
          <el-tag size="large" :type="statusMeta[order.status].type" effect="dark">{{ statusMeta[order.status].label }}</el-tag>
          <template v-if="order.status === 'PENDING_PAYMENT'">
            <el-button :loading="actionLoading" @click="runAction('cancel')">取消订单</el-button>
            <el-button type="primary" :loading="actionLoading" @click="runAction('pay')">模拟支付</el-button>
          </template>
          <el-button v-else-if="order.status === 'SHIPPED'" type="primary" :loading="actionLoading" @click="runAction('receive')">确认收货</el-button>
          <el-button v-else-if="order.status === 'RECEIVED' && firstProduct" type="primary" plain @click="router.push(`/products/${firstProduct.productId}#reviews`)">评价商品</el-button>
        </div>
      </header>
      <div class="order-detail-sheet">
        <div class="checkout-section">
          <span class="section-label">商品明细</span>
          <div class="store-order-items">
            <div v-for="item in order.items" :key="item.orderItemId" class="store-order-item">
              <div class="order-icon"><el-icon><Box /></el-icon></div>
              <div><strong>{{ item.productName }}</strong><span>{{ item.skuName }} · 数量 {{ item.quantity }}</span></div>
              <strong>{{ formatMoney(item.subtotal) }}</strong>
            </div>
          </div>
        </div>
        <div v-if="order.carrier || order.trackingNumber" class="checkout-section shipping-summary">
          <span class="section-label"><el-icon><Van /></el-icon> 物流信息</span>
          <div><span>承运商</span><strong>{{ order.carrier }}</strong></div><div><span>物流单号</span><strong>{{ order.trackingNumber }}</strong></div>
        </div>
        <div class="checkout-section checkout-total">
          <div><span>商品金额</span><strong>{{ formatMoney(order.itemAmount) }}</strong></div>
          <div><span>运费</span><strong>{{ formatMoney(order.shippingAmount) }}</strong></div>
          <div><span>实付金额</span><strong>{{ formatMoney(order.payableAmount) }}</strong></div>
        </div>
        <div class="checkout-section order-time-grid">
          <div><span>下单时间</span><strong>{{ formatDateTime(order.createdAt, auth.user?.timezone) }}</strong></div>
          <div v-if="order.paidAt"><span>支付时间</span><strong>{{ formatDateTime(order.paidAt, auth.user?.timezone) }}</strong></div>
          <div v-if="order.shippedAt"><span>发货时间</span><strong>{{ formatDateTime(order.shippedAt, auth.user?.timezone) }}</strong></div>
          <div v-if="order.receivedAt"><span>收货时间</span><strong>{{ formatDateTime(order.receivedAt, auth.user?.timezone) }}</strong></div>
        </div>
      </div>
    </template>
  </section>
</template>
