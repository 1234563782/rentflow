import { http } from './http'
import type {
  Availability, Category, CreateReviewRequest, LoginResponse, Notification, Order, OrderDetail, Page, ProductDetail,
  ProductReview, ProductSummary, Quote, Reservation, ReviewPage, UnreadNotificationCount,
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
  async availability(productId: string, startAt: string, endAt: string) {
    return (await http.post<Availability>('/api/v1/availability/search', { productId, startAt, endAt })).data
  },
}

export const reviewApi = {
  async list(productId: string, params: { page: number; size: number }) {
    return (await http.get<ReviewPage>(`/api/v1/products/${productId}/reviews`, { params })).data
  },
  async create(productId: string, request: CreateReviewRequest, idempotencyKey: string) {
    return (await http.post<ProductReview>(`/api/v1/products/${productId}/reviews`, request, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
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

export const quoteApi = {
  async create(productId: string, startAt: string, endAt: string) {
    return (await http.post<Quote>('/api/v1/quotes', { productId, startAt, endAt })).data
  },
}

export const reservationApi = {
  async get(reservationId: string) {
    return (await http.get<Reservation>(`/api/v1/reservations/${reservationId}`)).data
  },
}

export const orderApi = {
  async create(quoteId: string, idempotencyKey: string) {
    return (await http.post<Order>('/api/v1/orders', { quoteId }, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async confirm(orderId: string, idempotencyKey: string) {
    return (await http.post<Order>(`/api/v1/orders/${orderId}/confirm`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async cancel(orderId: string, idempotencyKey: string) {
    return (await http.post<Order>(`/api/v1/orders/${orderId}/cancel`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async receive(orderId: string, idempotencyKey: string) {
    return (await http.post<Order>(`/api/v1/orders/${orderId}/receive`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async list(params: { status?: string; page: number; size: number }) {
    return (await http.get<Page<Order>>('/api/v1/orders', { params })).data
  },
  async get(orderId: string) {
    return (await http.get<OrderDetail>(`/api/v1/orders/${orderId}`)).data
  },
}
