import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', redirect: '/products' },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
    { path: '/products', name: 'products', component: () => import('@/views/ProductListView.vue') },
    { path: '/products/:productId', name: 'product-detail', component: () => import('@/views/ProductDetailView.vue') },
    { path: '/checkout/:skuId', name: 'checkout', component: () => import('@/views/CheckoutView.vue'), meta: { auth: true } },
    { path: '/orders', name: 'orders', component: () => import('@/views/OrderListView.vue'), meta: { auth: true } },
    { path: '/orders/:orderId', name: 'order-detail', component: () => import('@/views/OrderDetailView.vue'), meta: { auth: true } },
    { path: '/gearmate', name: 'gearmate', component: () => import('@/views/GearMateView.vue'), meta: { auth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/products' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.guest && auth.isAuthenticated) return { name: 'products' }
})

export default router
