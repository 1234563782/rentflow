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

export interface ProductReview {
  reviewId: string
  rating: number
  content: string
  reviewerName: string
  createdAt: string
}

export interface ReviewStatistics {
  averageRating: number
  totalReviews: number
}

export interface CreateReviewRequest {
  rating: number
  content: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ReviewPage extends Page<ProductReview> {
  statistics: ReviewStatistics
}

export interface StoreSku {
  skuId: string
  productId: string
  skuCode: string
  skuName: string
  specs: Record<string, unknown>
  salePrice: string
  availableQuantity: number
  enabled: boolean
}

export type StoreOrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'RECEIVED' | 'CANCELLED' | 'CLOSED'

export interface StoreOrderItem {
  orderItemId: string
  productId: string
  skuId: string
  productName: string
  skuName: string
  specs: Record<string, unknown>
  unitPrice: string
  quantity: number
  subtotal: string
}

export interface StoreOrder {
  orderId: string
  status: StoreOrderStatus
  currency: 'CNY'
  itemAmount: string
  shippingAmount: string
  payableAmount: string
  paymentExpiresAt: string
  createdAt: string
  paidAt: string | null
  shippedAt: string | null
  receivedAt: string | null
  cancelledAt: string | null
  closedAt: string | null
  carrier: string | null
  trackingNumber: string | null
  items: StoreOrderItem[]
}

export interface ShippingAddress {
  recipientName: string
  recipientPhone: string
  province: string
  city: string
  district: string
  addressLine: string
}

export interface StoreReviewPage extends Page<ProductReview> {
  statistics: { averageRating: number; reviewCount: number }
}

export interface Notification {
  id: string
  type: string
  aggregateType: string | null
  aggregateId: string | null
  title: string
  content: string
  readAt: string | null
  createdAt: string
}

export interface UnreadNotificationCount {
  count: number
}

export interface Availability {
  productId: string
  startDate: string
  endDate: string
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
  startDate: string
  endDate: string
  expiresAt: string
  priceSnapshot: PriceSnapshot
}

export type ReservationStatus = 'ACTIVE' | 'CONSUMED' | 'RELEASED' | 'EXPIRED'

export interface Reservation {
  reservationId: string
  sourceQuoteId: string
  productId: string
  equipmentDisplayCode: string | null
  startDate: string
  endDate: string
  expiresAt: string
  status: ReservationStatus
  effectiveStatus: ReservationStatus
  priceSnapshot: PriceSnapshot
}

export type OrderStatus = 'PENDING_CONFIRMATION' | 'CONFIRMED' | 'RECEIVED' | 'CANCELLED' | 'EXPIRED'

export interface Order {
  orderId: string
  sourceReservationId: string
  productId: string
  productName: string
  productModel: string
  equipmentDisplayCode: string | null
  status: OrderStatus
  effectiveStatus: OrderStatus
  startDate: string
  endDate: string
  expiresAt: string
  priceSnapshot: PriceSnapshot
  createdAt: string
  confirmedAt: string | null
  receivedAt: string | null
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
  salePrice: string | null
  availableQuantity: number | null
  storeSkus: StoreSku[]
  dailyRate?: string | null
  fixedDeposit?: string | null
  availableCount: number | null
  useCases: ProductUseCase[]
}
export interface RecommendationSection {
  useCaseId: string | null
  title: string
  description?: string
  products: RecommendationCard[]
}
export interface FollowUpOption { value: string; label: string }
export interface FollowUpQuestion {
  field: 'use_case' | 'rental_period'
  text: string
  options: FollowUpOption[]
}
export interface RecommendationPresentation {
  mode: 'explore' | 'recommend' | 'purchase'
  intro?: string
  sections: RecommendationSection[]
  rentalPeriod: RentalPeriodValue | null
  followUp: FollowUpQuestion | null
  closing?: string | null
  purchaseQuantity?: number | null
}
export interface RentalPeriodValue { startDate: string; endDate: string }
export interface MessageRun {
  runId: string
  conversationId: string
  status: string
  stopReason: string | null
}
