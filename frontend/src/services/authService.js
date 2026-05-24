/**
 * authService — thin wrapper kept for potential future use.
 * Uses raw localStorage (no JSON encoding) to stay consistent
 * with AuthContext and axiosInstance.
 */
import { authApi } from '../api/authApi'

export const authService = {
  async login(username, password) {
    const data = await authApi.login({ username, password })
    // Store raw strings — NOT JSON-encoded — so axiosInstance can read them directly
    localStorage.setItem('accessToken',  data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify({
      username: data.username,
      role:     data.role,
      email:    data.email,
    }))
    return data
  },

  logout() {
    authApi.logout().catch(() => {})
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  },

  getUser()  {
    try { return JSON.parse(localStorage.getItem('user')) } catch { return null }
  },
  getToken() { return localStorage.getItem('accessToken') },
  isAuth()   { return !!localStorage.getItem('accessToken') },
}
