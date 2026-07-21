<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { RecommendationCard } from '@/types'
import { formatMoney } from '@/utils'

const props = defineProps<{
  modelValue: boolean
  product?: RecommendationCard
  initialQuantity?: number | null
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const router = useRouter()
const selectedSkuId = ref('')
const quantity = ref(1)
const skus = computed(() => props.product?.storeSkus || [])
const selectedSku = computed(() => skus.value.find(sku => sku.skuId === selectedSkuId.value))
const canContinue = computed(() => Boolean(selectedSku.value?.enabled && selectedSku.value.availableQuantity >= quantity.value))

watch(() => [props.modelValue, props.product] as const, () => {
  if (!props.modelValue || !props.product) return
  selectedSkuId.value = skus.value.find(sku => sku.availableQuantity > 0)?.skuId
    || skus.value[0]?.skuId || ''
  quantity.value = Math.max(1, props.initialQuantity || 1)
}, { immediate: true })

async function continueToCheckout() {
  if (!selectedSku.value) return
  emit('update:modelValue', false)
  await router.push({ name: 'checkout', params: { skuId: selectedSku.value.skuId }, query: { quantity: quantity.value } })
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="product?.name || '选择商品规格'" width="min(520px, calc(100vw - 28px))" @update:model-value="emit('update:modelValue', $event)">
    <div v-if="product" class="quick-purchase">
      <p>{{ product.brand }} · {{ product.model }}</p>
      <div v-if="skus.length" class="sku-options">
        <button v-for="sku in skus" :key="sku.skuId" type="button" class="sku-option" :class="{ active: selectedSkuId === sku.skuId }" @click="selectedSkuId = sku.skuId">
          <span><strong>{{ sku.skuName }}</strong><small>库存 {{ sku.availableQuantity }} 件</small></span>
          <strong>{{ formatMoney(sku.salePrice) }}</strong>
        </button>
      </div>
      <el-empty v-else description="当前商品暂未配置可售 SKU" :image-size="64" />
      <div v-if="selectedSku" class="purchase-summary">
        <div><span>数量</span><el-input-number v-model="quantity" :min="1" :max="Math.max(1, selectedSku.availableQuantity)" /></div>
        <div><span>小计</span><strong>{{ formatMoney((Number(selectedSku.salePrice) * quantity).toFixed(2)) }}</strong></div>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button v-if="product && !skus.length" type="primary" @click="router.push(`/products/${product.productId}`)">查看商品详情</el-button>
      <el-button v-else type="primary" :disabled="!canContinue" @click="continueToCheckout">去结算</el-button>
    </template>
  </el-dialog>
</template>
