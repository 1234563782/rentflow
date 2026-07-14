import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/services/api'
import { TOKEN_KEY, USER_KEY } from '@/services/http'
import type { UserSummary } from '@/types'

function loadUser() {
  try { return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null') as UserSummary | null }
  catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserSummary | null>(loadUser())
  const token = ref(sessionStorage.getItem(TOKEN_KEY))
  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  async function login(username: string, password: string) {
    const response = await authApi.login(username, password)
    token.value = response.accessToken
    user.value = response.user
    sessionStorage.setItem(TOKEN_KEY, response.accessToken)
    sessionStorage.setItem(USER_KEY, JSON.stringify(response.user))
  }

  function logout() {
    token.value = null
    user.value = null
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
    Object.keys(sessionStorage).filter((key) => key.startsWith('rentflow.attempt.'))
      .forEach((key) => sessionStorage.removeItem(key))
  }

  return { user, token, isAuthenticated, login, logout }
})
