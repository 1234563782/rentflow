import axios from 'axios'

export const TOKEN_KEY = 'rentflow.accessToken'
export const USER_KEY = 'rentflow.user'

export const http = axios.create({
  baseURL: import.meta.env.VITE_RENTFLOW_API_BASE_URL || 'http://localhost:8080',
  timeout: 12_000,
  headers: { Accept: 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.dispatchEvent(new CustomEvent('rentflow:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export const gearmateBaseUrl = import.meta.env.VITE_GEARMATE_API_BASE_URL || 'http://localhost:8000'
