import { NavLink } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  HiViewGrid, HiDatabase, HiDownload, HiLightningBolt,
  HiRefresh, HiClipboardList, HiUsers, HiBell, HiX,
  HiChip,
} from 'react-icons/hi'
import { useAuth } from '../context/AuthContext'
import clsx from 'clsx'

const NAV = [
  { to: '/dashboard',       icon: HiViewGrid,       label: 'Dashboard',        roles: ['ADMIN','ANALYST','OPERATOR'] },
  { to: '/data-sources',    icon: HiDatabase,        label: 'Data Sources',     roles: ['ADMIN','ANALYST','OPERATOR'] },
  { to: '/ingestion',       icon: HiDownload,        label: 'Ingestion Jobs',   roles: ['ADMIN','ANALYST','OPERATOR'] },
  { to: '/transformation',  icon: HiLightningBolt,   label: 'Transformation',   roles: ['ADMIN','ANALYST'] },
  { to: '/sync',            icon: HiRefresh,         label: 'Synchronization',  roles: ['ADMIN','ANALYST','OPERATOR'] },
  { to: '/audit',           icon: HiClipboardList,   label: 'Audit Logs',       roles: ['ADMIN','ANALYST','OPERATOR'] },
  { to: '/users',           icon: HiUsers,           label: 'User Management',  roles: ['ADMIN'] },
  { to: '/notifications',   icon: HiBell,            label: 'Notifications',    roles: ['ADMIN','ANALYST','OPERATOR'] },
]

export function Sidebar({ open, onClose }) {
  const { user } = useAuth()

  const visible = NAV.filter(n => n.roles.includes(user?.role))

  const content = (
    <div className="flex flex-col h-full">
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-white/10">
        <div className="w-9 h-9 rounded-xl bg-gradient-brand flex items-center justify-center shadow-glow-sm flex-shrink-0">
          <HiChip className="w-5 h-5 text-white" />
        </div>
        <div className="min-w-0">
          <p className="text-sm font-bold text-white leading-tight truncate">EDIP</p>
          <p className="text-[10px] text-slate-400 truncate">Data Integration</p>
        </div>
        {/* Mobile close */}
        <button onClick={onClose} className="ml-auto lg:hidden text-slate-400 hover:text-white">
          <HiX className="w-5 h-5" />
        </button>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        <p className="px-3 mb-2 text-[10px] font-bold uppercase tracking-widest text-slate-500">
          Navigation
        </p>
        {visible.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onClose}
            className={({ isActive }) =>
              clsx('nav-item', isActive && 'active')
            }
          >
            <Icon className="w-4 h-4 flex-shrink-0" />
            <span className="truncate">{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User chip */}
      <div className="px-3 py-4 border-t border-white/10">
        <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-white/5">
          <div className="w-8 h-8 rounded-full bg-gradient-brand flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
            {user?.username?.[0]?.toUpperCase() ?? 'U'}
          </div>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-white truncate">{user?.username}</p>
            <p className="text-[10px] text-slate-400 truncate">{user?.role}</p>
          </div>
        </div>
      </div>
    </div>
  )

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden lg:flex flex-col w-60 bg-surface-900 border-r border-white/10 h-screen sticky top-0 flex-shrink-0">
        {content}
      </aside>

      {/* Mobile drawer */}
      <AnimatePresence>
        {open && (
          <>
            <motion.div
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
              onClick={onClose}
            />
            <motion.aside
              initial={{ x: -280 }} animate={{ x: 0 }} exit={{ x: -280 }}
              transition={{ type: 'spring', stiffness: 400, damping: 35 }}
              className="fixed left-0 top-0 z-50 w-64 h-full bg-surface-900 border-r border-white/10 lg:hidden"
            >
              {content}
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </>
  )
}
