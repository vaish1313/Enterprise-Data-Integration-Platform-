import axios from 'axios'
import toast from 'react-hot-toast'
import { redirect, triggerLogout } from '../utils/navigationService'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
})

/* ── Request interceptor: attach JWT ──────────────────────────────────── */
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/* ── Response interceptor: handle errors globally ─────────────────────── */
api.interceptors.response.use(
  res => res,
  async err => {
    const status  = err.response?.status
    const message = err.response?.data?.error || err.response?.data?.message || err.message

    if (status === 401) {
      /* Try refresh once */
      const refresh = localStorage.getItem('refreshToken')
      if (refresh && !err.config._retry) {
        err.config._retry = true
        try {
          const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken: refresh })
          localStorage.setItem('accessToken', data.data.accessToken)
          err.config.headers.Authorization = `Bearer ${data.data.accessToken}`
          return api(err.config)
        } catch {
          triggerLogout('Session expired. Please sign in again.')
          redirect('/login')
        }
      } else {
        triggerLogout('Session expired. Please sign in again.')
        redirect('/login')
      }
    }

    if (status === 403) toast.error('Access denied — insufficient permissions')
    if (status === 404) toast.error('Resource not found')
    if (status >= 500)  toast.error('Server error — please try again')

    return Promise.reject(err)
  }
)

export default api
