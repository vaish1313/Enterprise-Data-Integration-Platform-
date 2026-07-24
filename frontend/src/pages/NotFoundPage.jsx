import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { HiHome } from 'react-icons/hi'

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--bg-base)' }}>
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center space-y-6"
      >
        <div className="text-[120px] font-extrabold leading-none" style={{ color: 'var(--text-primary)' }}>
          404
        </div>
        <div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--text-primary)' }}>Page not found</h1>
          <p className="mt-2" style={{ color: 'var(--text-secondary)' }}>The page you're looking for doesn't exist or has been moved.</p>
        </div>
        <Link to="/dashboard" className="btn-primary inline-flex">
          <HiHome className="w-4 h-4" /> Back to Dashboard
        </Link>
      </motion.div>
    </div>
  )
}
