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
  equipmentDisplayCode: string | null
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
  equipmentDisplayCode: string | null
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

export interface ConversationCreated {
  id: string
  timezone: string
  title: string | null
  createdAt: string
  updatedAt: string
}
export interface ConversationMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
  presentation?: RecommendationPresentation | null
}
export interface ProductUseCase {
  id: string
  code: string
  name: string
  weight: number
}
export interface RecommendationCard {
  productId: string
  name: string
  brand: string
  model: string
  dailyRate: string
  fixedDeposit: string
  availableCount: number | null
  useCases: ProductUseCase[]
}
export interface RecommendationSection {
  useCaseId: string | null
  title: string
  products: RecommendationCard[]
}
export interface FollowUpOption { value: string; label: string }
export interface FollowUpQuestion {
  field: 'use_case' | 'rental_period'
  text: string
  options: FollowUpOption[]
}
export interface RecommendationPresentation {
  mode: 'explore' | 'recommend'
  sections: RecommendationSection[]
  rentalPeriod: RentalPeriodValue | null
  followUp: FollowUpQuestion | null
}
export interface RentalPeriodValue { startAt: string; endAt: string }
export interface MessageRun {
  runId: string
  conversationId: string
  status: string
  stopReason: string | null
}
