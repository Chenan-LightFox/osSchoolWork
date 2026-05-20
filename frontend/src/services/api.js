import axios from 'axios'

import { clearAuth, getToken } from '@/stores/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 200) {
        return payload.data
      }
      const error = new Error(payload.msg || '请求失败')
      error.code = payload.code
      throw error
    }
    return payload
  },
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
    }
    const message =
      error.response?.data?.msg || error.response?.data?.message || error.message || '请求失败'
    const err = new Error(message)
    err.status = error.response?.status
    err.data = error.response?.data
    throw err
  }
)

export default api
