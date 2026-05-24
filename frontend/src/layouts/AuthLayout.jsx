import { Outlet, Navigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../context/AuthContext'
import { HiChip } from 'react-icons/hi'

export default function AuthLayout() {
  const { isAuth } = useAuth()

  // Already authenticated → go straight to dashboard
  if (isAuth) return <Navigate to="/dashboard" replace />

  return (
    <div className="min-h-screen flex bg-surface-950">
      {/* Left decorative panel */}
      <div className="hidden lg:flex flex-col justify-between w-[45%] bg-gradient-dark p-12 relative overflow-hidden">
        {/* Glow orbs */}
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-brand-600/20 rounded-full blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-purple-600/20 rounded-full blur-3xl" />

        {/* Logo */}
        <div className="flex items-center gap-3 relative z-10">
          <div className="w-10 h-10 rounded-xl bg-gradient-brand flex items-center justify-center shadow-glow">
            <HiChip className="w-6 h-6 text-white" />
          </div>
          <div>
            <p className="text-white font-bold text-lg leading-tight">EDIP</p>
            <p className="text-slate-400 text-xs">Enterprise Data Integration</p>
          </div>
        </div>

        {/* Hero text */}
        <div className="relative z-10 space-y-6">
          <h2 className="text-4xl font-extrabold text-white leading-tight">
            Unify your data.<br />
            <span className="text-transparent bg-clip-text bg-gradient-brand">
              Accelerate insights.
            </span>
          </h2>
          <p className="text-slate-400 text-base leading-relaxed max-w-sm">
            Ingest, transform, and synchronize data across all your enterprise systems
            from a single, powerful platform.
          </p>

          {/* Stats row */}
          <div className="flex gap-8 pt-4">
            {[['99.9%', 'Uptime SLA'], ['10M+', 'Records/day'], ['< 5min', 'Sync latency']].map(([v, l]) => (
              <div key={l}>
                <p className="text-2xl font-bold text-white">{v}</p>
                <p className="text-xs text-slate-400 mt-0.5">{l}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Bottom tagline */}
        <p className="text-xs text-slate-600 relative z-10">
          © {new Date().getFullYear()} EDIP Platform. All rights reserved.
        </p>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-6">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="w-full max-w-md"
        >
          {/* Mobile logo */}
          <div className="flex items-center gap-3 mb-8 lg:hidden">
            <div className="w-9 h-9 rounded-xl bg-gradient-brand flex items-center justify-center shadow-glow-sm">
              <HiChip className="w-5 h-5 text-white" />
            </div>
            <p className="text-white font-bold text-lg">EDIP</p>
          </div>

          <Outlet />
        </motion.div>
      </div>
    </div>
  )
}
