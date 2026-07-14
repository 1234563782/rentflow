<script setup lang="ts">
import { computed } from 'vue'
import { Camera, Headset, Microphone, Monitor, PictureRounded, VideoCamera } from '@element-plus/icons-vue'

const props = defineProps<{ name: string; brand?: string }>()
const visual = computed(() => {
  const value = `${props.name} ${props.brand || ''}`.toLowerCase()
  if (/相机|camera|sony|canon|nikon|fuji/.test(value)) return { icon: Camera, tone: 'forest' }
  if (/镜头|lens/.test(value)) return { icon: PictureRounded, tone: 'blue' }
  if (/麦克风|microphone|rode/.test(value)) return { icon: Microphone, tone: 'coral' }
  if (/耳机|headset/.test(value)) return { icon: Headset, tone: 'violet' }
  if (/显示|monitor|屏/.test(value)) return { icon: Monitor, tone: 'amber' }
  return { icon: VideoCamera, tone: 'graphite' }
})
</script>

<template>
  <div class="product-visual" :class="`product-visual--${visual.tone}`">
    <el-icon><component :is="visual.icon" /></el-icon>
    <span>{{ brand || 'RentFlow Select' }}</span>
  </div>
</template>
