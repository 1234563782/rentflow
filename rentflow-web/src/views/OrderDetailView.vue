<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Check, RefreshRight } from '@element-plus/icons-vue'
import PriceBreakdown from '@/components/PriceBreakdown.vue'
import { orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { OrderDetail } from '@/types'
import { apiErrorMessage, formatDateTime } from '@/utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const order = ref<OrderDetail>()
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try { order.value = await orderApi.get(String(route.params.orderId)) }
  catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="content-page narrow-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.push('/orders')">返回订单</el-button>
    <div v-if="loading" class="order-detail-sheet"><el-skeleton animated :rows="12" /></div>
    <div v-else-if="error" class="state-panel"><h2>订单加载失败</h2><p>{{ error }}</p><el-button :icon="RefreshRight" @click="load">重试</el-button></div>
    <template v-else-if="order">
      <header class="order-detail-header"><div><span class="eyebrow">订单详情</span><h1>{{ order.productName }}</h1><p>订单号 {{ order.orderId }}</p></div><el-tag size="large" type="success" effect="dark">已创建</el-tag></header>
      <div class="order-detail-sheet">
        <div class="order-product-summary"><div class="order-icon"><el-icon><Check /></el-icon></div><div><span>{{ order.productModel }}</span><h2>{{ order.productName }}</h2><p>设备编号 {{ order.equipmentDisplayCode }}</p></div></div>
        <div class="checkout-section period-summary"><div><span>开始时间</span><strong>{{ formatDateTime(order.startAt, auth.user?.timezone) }}</strong></div><div><span>结束时间</span><strong>{{ formatDateTime(order.endAt, auth.user?.timezone) }}</strong></div></div>
        <div class="checkout-section"><span class="section-label">订单金额</span><PriceBreakdown :snapshot="order.priceSnapshot" /></div>
        <div class="checkout-section"><span class="section-label">状态记录</span><el-timeline class="status-timeline"><el-timeline-item v-for="item in order.statusHistory" :key="item.createdAt" type="success" :timestamp="formatDateTime(item.createdAt, auth.user?.timezone)"><strong>{{ item.toStatus === 'CREATED' ? '订单已创建' : item.toStatus }}</strong><p>{{ item.reason || '状态已更新' }}</p></el-timeline-item></el-timeline></div>
      </div>
    </template>
  </section>
</template>
