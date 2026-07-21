<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Location, Wallet } from '@element-plus/icons-vue'
import { catalogApi, storeApi } from '@/services/api'
import type { ProductDetail, ShippingAddress, StoreOrder, StoreSku } from '@/types'
import { apiErrorMessage, formatMoney, isNetworkError, newIdempotencyKey } from '@/utils'

const route = useRoute()
const router = useRouter()
const sku = ref<StoreSku>()
const product = ref<ProductDetail>()
const order = ref<StoreOrder>()
const loading = ref(true)
const submitting = ref(false)
const paying = ref(false)
const error = ref('')
const quantity = ref(Math.max(1, Number(route.query.quantity) || 1))
const address = reactive<ShippingAddress>({
  recipientName: '', recipientPhone: '', province: '', city: '', district: '', addressLine: '',
})

const subtotal = computed(() => sku.value ? (Number(sku.value.salePrice) * quantity.value).toFixed(2) : '0.00')
const checkoutStorageKey = computed(() => `rentflow.attempt.store.checkout.${String(route.params.skuId)}`)

async function load() {
  loading.value = true
  error.value = ''
  try {
    sku.value = await storeApi.sku(String(route.params.skuId))
    product.value = await catalogApi.product(sku.value.productId)
    quantity.value = Math.min(quantity.value, Math.max(1, sku.value.availableQuantity))
  } catch (cause) { error.value = apiErrorMessage(cause) }
  finally { loading.value = false }
}

function validateAddress() {
  if (!address.recipientName.trim() || !address.recipientPhone.trim() || !address.province.trim()
    || !address.city.trim() || !address.district.trim() || !address.addressLine.trim()) {
    ElMessage.warning('请填写完整收货信息')
    return false
  }
  return true
}

async function createOrder() {
  if (!sku.value || !validateAddress()) return
  let key = sessionStorage.getItem(checkoutStorageKey.value)
  if (!key) { key = newIdempotencyKey(); sessionStorage.setItem(checkoutStorageKey.value, key) }
  submitting.value = true
  try {
    order.value = await storeApi.checkout([{ skuId: sku.value.skuId, quantity: quantity.value }], { ...address }, key)
    sessionStorage.removeItem(checkoutStorageKey.value)
    ElMessage.success('订单已创建，请在 15 分钟内完成支付')
  } catch (cause) {
    ElMessage[isNetworkError(cause) ? 'warning' : 'error'](isNetworkError(cause) ? '网络响应中断，可使用原请求安全重试' : apiErrorMessage(cause))
  } finally { submitting.value = false }
}

async function pay() {
  if (!order.value) return
  const storageKey = `rentflow.attempt.store.pay.${order.value.orderId}`
  const key = sessionStorage.getItem(storageKey) || newIdempotencyKey()
  sessionStorage.setItem(storageKey, key)
  paying.value = true
  try {
    order.value = await storeApi.pay(order.value.orderId, key)
    sessionStorage.removeItem(storageKey)
    ElMessage.success('模拟支付成功')
    await router.replace(`/orders/${order.value.orderId}`)
  } catch (cause) { ElMessage.error(apiErrorMessage(cause)) }
  finally { paying.value = false }
}

onMounted(() => { void load() })
</script>

<template>
  <section class="content-page narrow-page">
    <el-button class="back-button" text :icon="ArrowLeft" @click="router.back()">返回商品</el-button>
    <header class="page-heading"><div><span class="eyebrow">确认订单</span><h1>核对商品与收货信息</h1><p>创建待支付订单时会预占对应 SKU 库存。</p></div></header>
    <div v-if="loading" class="checkout-sheet"><el-skeleton animated :rows="10" /></div>
    <div v-else-if="error" class="state-panel"><h2>结算信息加载失败</h2><p>{{ error }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="sku && product" class="checkout-sheet store-checkout">
      <div class="checkout-section checkout-product-line">
        <div><span class="section-label">商品</span><h2>{{ product.name }}</h2><p>{{ sku.skuName }} · {{ product.brand }} {{ product.model }}</p></div>
        <strong>{{ formatMoney(sku.salePrice) }}</strong>
      </div>
      <div class="checkout-section">
        <span class="section-label"><el-icon><Location /></el-icon> 收货信息</span>
        <div class="address-form">
          <el-input v-model="address.recipientName" maxlength="64" placeholder="收货人" />
          <el-input v-model="address.recipientPhone" maxlength="32" placeholder="联系电话" />
          <el-input v-model="address.province" maxlength="64" placeholder="省份" />
          <el-input v-model="address.city" maxlength="64" placeholder="城市" />
          <el-input v-model="address.district" maxlength="64" placeholder="区县" />
          <el-input v-model="address.addressLine" maxlength="255" placeholder="详细地址" />
        </div>
      </div>
      <div class="checkout-section checkout-total">
        <div><span>购买数量</span><el-input-number v-model="quantity" :min="1" :max="Math.max(1, sku.availableQuantity)" :disabled="Boolean(order)" /></div>
        <div><span>运费</span><strong>{{ formatMoney('0.00') }}</strong></div>
        <div><span>应付金额</span><strong>{{ formatMoney(order?.payableAmount || subtotal) }}</strong></div>
      </div>
      <div class="checkout-actions">
        <el-button @click="router.push('/products')">继续逛逛</el-button>
        <el-button v-if="!order" type="primary" :loading="submitting" :disabled="sku.availableQuantity < quantity" @click="createOrder">提交订单</el-button>
        <el-button v-else type="primary" :icon="Wallet" :loading="paying" @click="pay">模拟支付</el-button>
      </div>
    </div>
  </section>
</template>
