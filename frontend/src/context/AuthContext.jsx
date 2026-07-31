import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { authApi } from '../api/authApi'
import toast from 'react-hot-toast'
import { setOnLogout } from '../utils/navigationService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // Initialise synchronously from localStorage — no async, no useEffect needed
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  })

  const [token, setToken] = useState(() => localStorage.getItem('accessToken'))

  // loading is only true on the very first mount while we validate the stored session
  // We set it false immediately because we read synchronously above — no async needed
  const [loading, setLoading] = useState(false)

  // ── Login ──────────────────────────────────────────────────────────────
  const login = useCallback(async (username, password) => {
    const data = await authApi.login({ username, password })
    const { accessToken, refreshToken, username: uname, role, email } = data

    // Persist to localStorage
    localStorage.setItem('accessToken',  accessToken)
    localStorage.setItem('refreshToken', refreshToken)

    const userObj = { username: uname, role, email }
    localStorage.setItem('user', JSON.stringify(userObj))

    // Update state — both in the same synchronous batch so isAuth flips to true at once
    setToken(accessToken)
    setUser(userObj)

    return userObj
  }, [])

  // ── Logout ─────────────────────────────────────────────────────────────
  const logout = useCallback(async (msg) => {
    try {
      // Invalidate refresh token server-side to prevent replay attacks
      await authApi.logout()
    } catch (error) {
      console.warn('Server-side logout failed, falling back to local logout', error)
    } finally {
      // Always clean up local state, even if the server is unreachable
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      setToken(null)
      setUser(null)
      if (msg && typeof msg === 'string') {
        toast.error(msg)
      } else {
        toast.success('Signed out successfully')
      }
    }
  }, [])

  useEffect(() => {
    setOnLogout(logout)
    return () => {
      setOnLogout(null)
    }
  }, [logout])

  // ── Role helpers ───────────────────────────────────────────────────────
  const isAdmin    = user?.role === 'ADMIN'
  const isAnalyst  = user?.role === 'ANALYST'
  const isOperator = user?.role === 'OPERATOR'
  const canWrite   = isAdmin || isAnalyst
  const isAuth     = !!token && !!user

  return (
    <AuthContext.Provider value={{
      user, token, loading,
      login, logout,
      isAdmin, isAnalyst, isOperator, canWrite, isAuth,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
