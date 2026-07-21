import { http } from './http'
import type {
  Category, CreateReviewRequest, LoginResponse, Notification, Page, ProductDetail,
  ProductReview, ProductSummary, ShippingAddress, StoreOrder, StoreReviewPage,
  StoreSku, UnreadNotificationCount,
} from '@/types'

export const authApi = {
  async login(username: string, password: string) {
    return (await http.post<LoginResponse>('/api/v1/auth/login', { username, password })).data
  },
}

export const catalogApi = {
  async categories() {
    return (await http.get<Category[]>('/api/v1/categories')).data
  },
  async products(params: Record<string, string | number | undefined>) {
    return (await http.get<Page<ProductSummary>>('/api/v1/products', { params })).data
  },
  async product(productId: string) {
    return (await http.get<ProductDetail>(`/api/v1/products/${productId}`)).data
  },
}

export const notificationApi = {
  async list(params: { unreadOnly?: boolean; page: number; size: number }) {
    return (await http.get<Page<Notification>>('/api/v1/notifications', { params })).data
  },
  async unreadCount() {
    return (await http.get<UnreadNotificationCount>('/api/v1/notifications/unread-count')).data
  },
  async markRead(notificationId: string) {
    await http.post(`/api/v1/notifications/${notificationId}/read`)
  },
}

export const storeApi = {
  async skus(productId: string) {
    return (await http.get<StoreSku[]>(`/api/v1/store/products/${productId}/skus`)).data
  },
  async sku(skuId: string) {
    return (await http.get<StoreSku>(`/api/v1/store/skus/${skuId}`)).data
  },
  async checkout(items: Array<{ skuId: string; quantity: number }>, address: ShippingAddress, idempotencyKey: string) {
    return (await http.post<StoreOrder>('/api/v1/store/orders/checkout', { items, address }, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async pay(orderId: string, idempotencyKey: string) {
    return (await http.post<StoreOrder>(`/api/v1/store/orders/${orderId}/pay`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async cancel(orderId: string, idempotencyKey: string) {
    return (await http.post<StoreOrder>(`/api/v1/store/orders/${orderId}/cancel`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async receive(orderId: string, idempotencyKey: string) {
    return (await http.post<StoreOrder>(`/api/v1/store/orders/${orderId}/receive`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async listOrders(params: { status?: string; page: number; size: number }) {
    return (await http.get<Page<StoreOrder>>('/api/v1/store/orders', { params })).data
  },
  async order(orderId: string) {
    return (await http.get<StoreOrder>(`/api/v1/store/orders/${orderId}`)).data
  },
}

export const storeReviewApi = {
  async list(productId: string, params: { page: number; size: number }) {
    return (await http.get<StoreReviewPage>(`/api/v1/store/products/${productId}/reviews`, { params })).data
  },
  async create(productId: string, request: CreateReviewRequest, idempotencyKey: string) {
    return (await http.post<ProductReview>(`/api/v1/store/products/${productId}/reviews`, request, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
}
