import { computed, reactive } from 'vue'

const STORAGE_KEY = 'osSchoolWork.auth'

const state = reactive({
  token: null,
  user: null,
})

const loadStoredAuth = () => {
  // 从 localStorage 读取登录态
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

const stored = loadStoredAuth()
if (stored?.token) {
  // 启动时恢复登录态
  state.token = stored.token
  state.user = stored.user ?? null
}

const persist = () => {
  // 同步保存登录态
  if (!state.token) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      token: state.token,
      user: state.user,
    })
  )
}

export const isAuthenticated = computed(() => Boolean(state.token))
export const currentUser = computed(() => state.user)

export const setAuth = (payload) => {
  state.token = payload?.token ?? null
  state.user = payload?.user ?? null
  persist()
}

export const clearAuth = () => {
  state.token = null
  state.user = null
  persist()
}

export const getToken = () => state.token
