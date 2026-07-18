<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, ChatDotRound, Goods, List, SwitchButton, User } from '@element-plus/icons-vue'
import { notificationApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Notification } from '@/types'
import { formatDateTime } from '@/utils'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const notificationLoading = ref(false)
let notificationTimer: number | undefined

function logout() {
  auth.logout()
  router.push('/login')
}

function unauthorized() {
  auth.logout()
  ElMessage.warning('登录状态已失效，请重新登录')
  router.replace({ name: 'login', query: { redirect: route.fullPath } })
}

async function refreshNotifications() {
  if (!auth.isAuthenticated) return
  notificationLoading.value = true
  try {
    const [page, count] = await Promise.all([
      notificationApi.list({ unreadOnly: true, page: 0, size: 20 }),
      notificationApi.unreadCount(),
    ])
    notifications.value = page.items
    unreadCount.value = count.count
  } catch {
    // Notification refresh is non-blocking; the HTTP interceptor handles authentication expiry.
  } finally {
    notificationLoading.value = false
  }
}

async function markNotificationRead(notification: Notification) {
  if (notification.readAt) return true
  try {
    await notificationApi.markRead(notification.id)
    notifications.value = notifications.value.filter(({ id }) => id !== notification.id)
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    return true
  } catch {
    ElMessage.error('通知状态更新失败，请稍后重试')
    return false
  }
}

async function handleNotificationClick(notification: Notification) {
  if (!await markNotificationRead(notification)) return

  if (
    notification.type === 'ORDER_CONFIRMATION_REMINDER' &&
    notification.aggregateType === 'ORDER' &&
    notification.aggregateId !== null &&
    /^[0-9A-HJKMNP-TV-Z]{26}$/.test(notification.aggregateId)
  ) {
    await router.push(`/orders/${notification.aggregateId}/confirm`)
  }
}

function handleWindowFocus() {
  void refreshNotifications()
}

watch(() => auth.isAuthenticated, (isAuthenticated) => {
  if (isAuthenticated) void refreshNotifications()
  else {
    notifications.value = []
    unreadCount.value = 0
  }
})

onMounted(() => {
  window.addEventListener('rentflow:unauthorized', unauthorized)
  window.addEventListener('focus', handleWindowFocus)
  void refreshNotifications()
  notificationTimer = window.setInterval(() => { void refreshNotifications() }, 60_000)
})
onBeforeUnmount(() => {
  window.removeEventListener('rentflow:unauthorized', unauthorized)
  window.removeEventListener('focus', handleWindowFocus)
  window.clearInterval(notificationTimer)
})
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--guest': route.name === 'login' }">
    <header v-if="route.name !== 'login'" class="topbar">
      <router-link to="/products" class="brand" aria-label="RentFlow 商品列表">
        <span class="brand-mark">RF</span>
        <span>RentFlow</span>
      </router-link>
      <nav class="desktop-nav" aria-label="主导航">
        <router-link to="/products"><el-icon><Goods /></el-icon>设备</router-link>
        <router-link v-if="auth.isAuthenticated" to="/orders"><el-icon><List /></el-icon>订单</router-link>
        <router-link v-if="auth.isAuthenticated" to="/gearmate"><el-icon><ChatDotRound /></el-icon>GearMate</router-link>
      </nav>
      <div class="topbar-actions">
        <template v-if="auth.isAuthenticated">
          <el-popover placement="bottom-end" :width="360" trigger="click" popper-class="notification-popover" @show="refreshNotifications">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
                <el-button :icon="Bell" circle text title="通知" aria-label="通知" />
              </el-badge>
            </template>
            <div class="notification-panel">
              <div class="notification-panel__header"><strong>通知</strong><span v-if="unreadCount">{{ unreadCount }} 条未读</span></div>
              <div v-if="notificationLoading && !notifications.length" class="notification-panel__loading"><el-skeleton animated :rows="3" /></div>
              <el-empty v-else-if="!notifications.length" :image-size="56" description="暂无未读通知" />
              <div v-else class="notification-list">
                <button v-for="notification in notifications" :key="notification.id" class="notification-item" type="button" @click="handleNotificationClick(notification)">
                  <strong>{{ notification.title }}</strong>
                  <span>{{ notification.content }}</span>
                  <time>{{ formatDateTime(notification.createdAt, auth.user?.timezone) }}</time>
                </button>
              </div>
            </div>
          </el-popover>
          <span class="user-chip"><el-icon><User /></el-icon>{{ auth.user?.nickname }}</span>
          <el-button :icon="SwitchButton" circle text title="退出登录" aria-label="退出登录" @click="logout" />
        </template>
        <el-button v-else type="primary" @click="router.push('/login')">登录</el-button>
      </div>
    </header>
    <main :class="route.name === 'login' ? 'login-main' : 'page-main'">
      <router-view />
    </main>
    <nav v-if="route.name !== 'login' && auth.isAuthenticated" class="mobile-nav" aria-label="移动端导航">
      <router-link to="/products"><el-icon><Goods /></el-icon><span>设备</span></router-link>
      <router-link to="/orders"><el-icon><List /></el-icon><span>订单</span></router-link>
      <router-link to="/gearmate"><el-icon><ChatDotRound /></el-icon><span>GearMate</span></router-link>
    </nav>
  </div>
</template>
