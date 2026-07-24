import { Outlet, Navigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../context/AuthContext'
import { HiChip } from 'react-icons/hi'

export default function AuthLayout() {
  const { isAuth } = useAuth()

  // Already authenticated → go straight to dashboard
  if (isAuth) return <Navigate to="/dashboard" replace />

  return (
    <div className="min-h-screen flex" style={{ background: 'var(--bg-gradient)' }}>
      {/* Left decorative panel */}
      <div className="hidden lg:flex flex-col justify-between w-[45%] p-12 relative overflow-hidden" style={{ background: '#0d0d0d', borderRight: '1px solid rgba(255,255,255,0.05)' }}>
        {/* Glow orbs */}
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full blur-3xl" style={{ background: 'rgba(255,255,255,0.02)' }} />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full blur-3xl" style={{ background: 'rgba(255,255,255,0.02)' }} />

        {/* Logo */}
        <div className="flex items-center gap-3 relative z-10">
          <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)' }}>
            <HiChip className="w-6 h-6" style={{ color: '#f5f5f5' }} />
          </div>
          <div>
            <p className="font-bold text-lg leading-tight" style={{ color: '#f5f5f5' }}>EDIP</p>
            <p className="text-xs" style={{ color: '#a3a3a0' }}>Enterprise Data Integration</p>
          </div>
        </div>

        {/* Hero text */}
        <div className="relative z-10 space-y-6">
          <h2 className="text-4xl font-extrabold leading-tight" style={{ color: '#f5f5f5' }}>
            Unify your data.<br />
            <span style={{ color: '#a3a3a0' }}>
              Accelerate insights.
            </span>
          </h2>
          <p className="text-base leading-relaxed max-w-sm" style={{ color: '#a3a3a0' }}>
            Ingest, transform, and synchronize data across all your enterprise systems
            from a single, powerful platform.
          </p>

          {/* Stats row */}
          <div className="flex gap-8 pt-4">
            {[['99.9%', 'Uptime SLA'], ['10M+', 'Records/day'], ['< 5min', 'Sync latency']].map(([v, l]) => (
              <div key={l}>
                <p className="text-2xl font-bold" style={{ color: '#f5f5f5' }}>{v}</p>
                <p className="text-xs mt-0.5" style={{ color: '#a3a3a0' }}>{l}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Bottom tagline */}
        <p className="text-xs relative z-10" style={{ color: '#737373' }}>
          © {new Date().getFullYear()} EDIP Platform. All rights reserved.
        </p>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-6 relative overflow-hidden" style={{ background: '#1c1c1c' }}>
        {/* Colorful Glow Blobs */}
        <div className="absolute rounded-full" style={{ width: '380px', height: '380px', background: 'radial-gradient(circle, rgba(120,120,255,0.35), transparent 70%)', top: '-100px', right: '-80px', filter: 'blur(10px)', zIndex: 0 }} />
        <div className="absolute rounded-full" style={{ width: '320px', height: '320px', background: 'radial-gradient(circle, rgba(255,255,255,0.15), transparent 70%)', bottom: '-80px', left: '40px', filter: 'blur(10px)', zIndex: 0 }} />
        <div className="absolute rounded-full" style={{ width: '280px', height: '280px', background: 'radial-gradient(circle, rgba(180,140,255,0.25), transparent 70%)', top: '40%', left: '10%', filter: 'blur(10px)', zIndex: 0 }} />

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="w-full max-w-md relative z-10"
        >
          {/* Mobile logo */}
          <div className="flex items-center gap-3 mb-8 lg:hidden">
            <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
              <HiChip className="w-5 h-5" style={{ color: 'var(--text-primary)' }} />
            </div>
            <p className="font-bold text-lg" style={{ color: 'var(--text-primary)' }}>EDIP</p>
          </div>

          <Outlet />
        </motion.div>
      </div>
    </div>
  )
}
