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
  return `按 24 小时向上取整 · ${snapshot.billingDays} 个计费日 · 固定押金`
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
  const start = dayjs().add(1, 'day').hour(10).minute(0).second(0).millisecond(0)
  return [start.toDate(), start.add(1, 'day').toDate()]
}

export function toIsoPeriod(value: [Date, Date] | null) {
  if (!value) return null
  return { startAt: dayjs(value[0]).toISOString(), endAt: dayjs(value[1]).toISOString() }
}
