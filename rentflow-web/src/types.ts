export interface ApiErrorBody {
  code: string
  message: string
  correlationId: string
  details: Record<string, unknown>
}

export interface UserSummary {
  userId: string
  nickname: string
  role: 'USER' | 'ADMIN'
  timezone: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserSummary
}

export interface Category {
  categoryId: string
  name: string
  sortOrder: number
}

export interface ProductSummary {
  productId: string
  categoryId: string
  name: string
  brand: string
  model: string
  dailyRate: string
  fixedDeposit: string
  availableCount?: number
}

export interface ProductDetail extends ProductSummary {
  description: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Availability {
  productId: string
  startAt: string
  endAt: string
  available: boolean
  availableCount: number
  checkedAt: string
}

export interface PriceSnapshot {
  currency: 'CNY'
  pricingVersion: number
  pricingRule: string
  billingDays: number
  dailyRate: string
  rentalAmount: string
  depositAmount: string
  totalAmount: string
  roundingMode: 'HALF_UP'
}

export interface Quote {
  quoteId: string
  productId: string
  startAt: string
  endAt: string
  expiresAt: string
  priceSnapshot: PriceSnapshot
}

export type ReservationStatus = 'ACTIVE' | 'CONSUMED' | 'RELEASED' | 'EXPIRED'

export interface Reservation {
  reservationId: string
  sourceQuoteId: string
  productId: string
  equipmentDisplayCode: string
  startAt: string
  endAt: string
  expiresAt: string
  status: ReservationStatus
  effectiveStatus: ReservationStatus
  priceSnapshot: PriceSnapshot
}

export type OrderStatus = 'PENDING_CONFIRMATION' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED'

export interface Order {
  orderId: string
  sourceReservationId: string
  productId: string
  productName: string
  productModel: string
  equipmentDisplayCode: string
  status: OrderStatus
  effectiveStatus: OrderStatus
  startAt: string
  endAt: string
  expiresAt: string
  priceSnapshot: PriceSnapshot
  createdAt: string
  confirmedAt: string | null
  cancelledAt: string | null
  expiredAt: string | null
}

export interface OrderHistory {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  reason: string | null
  createdAt: string
}

export interface OrderDetail extends Order {
  statusHistory: OrderHistory[]
}

export interface ConversationCreated { id: string }
export interface MessageRun {
  runId: string
  conversationId: string
  status: string
  stopReason: string | null
}
