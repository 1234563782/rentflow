<script setup lang="ts">
import type { PriceSnapshot } from '@/types'
import { formatMoney, pricingRuleLabel } from '@/utils'

defineProps<{ snapshot: PriceSnapshot }>()
</script>

<template>
  <div class="price-breakdown">
    <div><span>日租金</span><strong>{{ formatMoney(snapshot.dailyRate) }}</strong></div>
    <div><span>计费天数</span><strong>{{ snapshot.billingDays }} 天</strong></div>
    <div><span>租金小计</span><strong>{{ formatMoney(snapshot.rentalAmount) }}</strong></div>
    <div><span>押金</span><strong>{{ formatMoney(snapshot.depositAmount) }}</strong></div>
    <div class="price-total"><span>合计</span><strong>{{ formatMoney(snapshot.totalAmount) }}</strong></div>
    <p>{{ pricingRuleLabel(snapshot) }} · {{ snapshot.currency }} · {{ snapshot.roundingMode }}</p>
  </div>
</template>
