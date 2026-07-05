import { getToken } from '@/stores/auth'

const buildWsBase = () => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
    if (baseUrl.startsWith('http://') || baseUrl.startsWith('https://')) {
        const wsBase = baseUrl.replace(/^http/, 'ws')
        return wsBase.replace(/\/api\/?$/, '')
    }
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return `${protocol}://${window.location.host}`
}

export const createMailSocket = () => {
    const token = getToken()
    if (!token) return null
    const base = buildWsBase()
    const url = `${base}/ws?token=${encodeURIComponent(token)}`
    return new WebSocket(url)
}
