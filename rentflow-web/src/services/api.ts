import { http } from './http'
import type {
  Availability, Category, LoginResponse, Order, OrderDetail, Page, ProductDetail,
  ProductSummary, Quote, Reservation,
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

export const quoteApi = {
  async create(productId: string, startAt: string, endAt: string) {
    return (await http.post<Quote>('/api/v1/quotes', { productId, startAt, endAt })).data
  },
}

export const reservationApi = {
  async create(quoteId: string, idempotencyKey: string) {
    return (await http.post<Reservation>('/api/v1/reservations', { quoteId }, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })).data
  },
  async get(reservationId: string) {
    return (await http.get<Reservation>(`/api/v1/reservations/${reservationId}`)).data
  },
  async release(reservationId: string) {
    return (await http.delete<Reservation>(`/api/v1/reservations/${reservationId}`)).data
  },
}

export const orderApi = {
  async create(reservationId: string, idempotencyKey: string) {
    return (await http.post<Order>('/api/v1/orders', { reservationId }, {
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
