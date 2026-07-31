import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import AppRoutes from './routes'
import { setNavigate } from './utils/navigationService'

/**
 * Registers React Router's navigate function with the navigationService
 * singleton so axios interceptors can redirect without window.location.
 * Must be rendered inside <BrowserRouter> (which wraps this in main.jsx).
 */
function NavigationWatcher() {
  const navigate = useNavigate()
  useEffect(() => {
    setNavigate(navigate)
  }, [navigate])
  return null
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <NavigationWatcher />
        <AppRoutes />
      </AuthProvider>
    </ThemeProvider>
  )
}

