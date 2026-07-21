import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import type { AxiosError } from 'axios'
import type { ApiErrorBody } from './types'

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
