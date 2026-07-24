import { motion } from 'framer-motion'

export function Spinner({ size = 'md', className = '' }) {
  const s = { sm: 'w-4 h-4', md: 'w-6 h-6', lg: 'w-10 h-10' }[size]
  return (
    <svg className={`animate-spin ${s} ${className}`} style={{ color: 'var(--text-primary)' }} fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  )
}

export function PageLoader() {
  return (
    <div className="flex items-center justify-center h-64">
      <motion.div
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        className="flex flex-col items-center gap-3"
      >
        <Spinner size="lg" />
        <p className="text-sm font-medium" style={{ color: 'var(--text-secondary)' }}>Loading…</p>
      </motion.div>
    </div>
  )
}

export function SkeletonRow({ cols = 5 }) {
  return (
    <tr>
      {Array.from({ length: cols }).map((_, i) => (
        <td key={i} className="px-4 py-3">
          <div className="h-4 rounded animate-pulse" style={{ background: 'var(--glass-fill)' }} />
        </td>
      ))}
    </tr>
  )
}

export function SkeletonCard() {
  return (
    <div className="card p-5 space-y-3 animate-pulse">
      <div className="h-3 w-24 rounded" style={{ background: 'var(--glass-fill)' }} />
      <div className="h-8 w-32 rounded" style={{ background: 'var(--glass-fill)' }} />
      <div className="h-3 w-16 rounded" style={{ background: 'var(--glass-fill)' }} />
    </div>
  )
}
