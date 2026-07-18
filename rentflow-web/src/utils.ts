import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import type { AxiosError } from 'axios'
import type { ApiErrorBody, PriceSnapshot } from './types'

dayjs.extend(utc)
dayjs.extend(timezone)

export function formatDateTime(value: string, zone?: string) {
  const parsed = dayjs(value)
  return zone ? parsed.tz(zone).format('YYYY-MM-DD HH:mm') : parsed.format('YYYY-MM-DD HH:mm')
}

export function formatMoney(value: string | number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency', currency: 'CNY', minimumFractionDigits: 2,
  }).format(Number(value))
}

export function pricingRuleLabel(snapshot: PriceSnapshot) {
  return `按自然日计费 · 共 ${snapshot.billingDays} 天 · 固定押金`
}

export function apiErrorMessage(error: unknown) {
  const candidate = error as AxiosError<ApiErrorBody>
  return candidate.response?.data?.message || candidate.message || '请求失败，请稍后重试'
}

export function isNetworkError(error: unknown) {
  const candidate = error as AxiosError
  return !candidate.response || candidate.code === 'ECONNABORTED'
}

export function newIdempotencyKey() {
  return crypto.randomUUID()
}

export function defaultRentalPeriod(): [Date, Date] {
  const start = dayjs().tz('Asia/Shanghai').add(2, 'day').startOf('day')
  return [start.toDate(), start.add(1, 'day').toDate()]
}

export function toDateRange(value: [Date, Date] | null) {
  if (!value) return null
  return {
    startDate: dayjs(value[0]).tz('Asia/Shanghai').format('YYYY-MM-DD'),
    endDate: dayjs(value[1]).tz('Asia/Shanghai').format('YYYY-MM-DD'),
  }
}

export function formatDate(value: string) {
  return dayjs(value).format('YYYY-MM-DD')
}

export function isDateBeforeRentalStartInShanghai(value: Date) {
  const earliestRentalDate = dayjs().tz('Asia/Shanghai').add(2, 'day').startOf('day')
  return dayjs(value).tz('Asia/Shanghai').isBefore(earliestRentalDate, 'day')
}
