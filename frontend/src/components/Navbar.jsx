import { useNavigate, useLocation } from 'react-router-dom'
import { HiMenu, HiSun, HiMoon, HiLogout, HiBell } from 'react-icons/hi'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

const TITLES = {
  '/dashboard': 'Dashboard',
  '/data-sources': 'Data Sources',
  '/ingestion': 'Ingestion Jobs',
  '/transformation': 'Transformation Rules',
  '/sync': 'Synchronization',
  '/audit': 'Audit Logs',
  '/users': 'User Management',
  '/notifications': 'Notifications',
}

export function Navbar({ onMenuClick }) {
  const { logout, user } = useAuth()
  const { dark, toggle } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()

  const title = Object.entries(TITLES).find(([k]) => location.pathname.startsWith(k))?.[1] ?? 'EDIP'

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="sticky top-0 z-30 flex items-center gap-4 px-4 sm:px-6 h-16 glass-card !rounded-none !border-x-0 !border-t-0">
      {/* Mobile menu toggle */}
      <button onClick={onMenuClick} className="btn-icon lg:hidden">
        <HiMenu className="w-5 h-5" />
      </button>

      {/* Page title */}
      <h1 className="text-base font-bold flex-1 truncate" style={{ color: 'var(--text-primary)' }}>
        {title}
      </h1>

      {/* Right actions */}
      <div className="flex items-center gap-1">
        {/* Notifications */}
        <button className="btn-icon relative" onClick={() => navigate('/notifications')}>
          <HiBell className="w-5 h-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full" style={{ background: 'var(--text-primary)' }} />
        </button>

        {/* Theme toggle */}
        <button onClick={toggle} className="btn-icon">
          {dark ? <HiSun className="w-5 h-5" /> : <HiMoon className="w-5 h-5" />}
        </button>

        {/* User avatar + logout */}
        <div className="flex items-center gap-2 ml-1 pl-3 border-l" style={{ borderColor: 'var(--glass-border)' }}>
          <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)', color: 'var(--text-primary)' }}>
            {user?.username?.[0]?.toUpperCase() ?? 'U'}
          </div>
          <button onClick={handleLogout} className="btn-icon" title="Sign out">
            <HiLogout className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  )
}
