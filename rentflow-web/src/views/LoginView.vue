<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils'

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function submit() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    ElMessage.success(`欢迎回来，${auth.user?.nickname}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/products'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(apiErrorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-page">
    <div class="login-scene" aria-hidden="true">
      <div class="scene-grid"></div>
      <div class="device-silhouette device-silhouette--camera"><span></span></div>
      <div class="device-silhouette device-silhouette--lens"></div>
      <div class="device-silhouette device-silhouette--audio"></div>
      <div class="login-brand-copy">
        <span class="brand-mark brand-mark--large">RF</span>
        <h1>RentFlow</h1>
        <p>需要的时候，设备正好在。</p>
      </div>
    </div>
    <div class="login-panel">
      <div class="login-form-wrap">
        <span class="login-kicker">设备租赁工作台</span>
        <h2>登录账户</h2>
        <p class="muted">查看库存、锁定报价并管理你的订单。</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" placeholder="请输入账号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password autocomplete="current-password" placeholder="请输入密码" @keyup.enter="submit" />
          </el-form-item>
          <el-button class="login-submit" type="primary" native-type="submit" :loading="loading">
            登录 <el-icon v-if="!loading"><ArrowRight /></el-icon>
          </el-button>
        </el-form>
        <button class="browse-link" type="button" @click="router.push('/products')">先浏览设备</button>
      </div>
    </div>
  </section>
</template>
