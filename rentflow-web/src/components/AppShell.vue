<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Goods, List, SwitchButton, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

function logout() {
  auth.logout()
  router.push('/login')
}

function unauthorized() {
  auth.logout()
  ElMessage.warning('登录状态已失效，请重新登录')
  router.replace({ name: 'login', query: { redirect: route.fullPath } })
}

onMounted(() => window.addEventListener('rentflow:unauthorized', unauthorized))
onBeforeUnmount(() => window.removeEventListener('rentflow:unauthorized', unauthorized))
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
