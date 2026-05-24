import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { HiHome } from 'react-icons/hi'

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-surface-950 p-6">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center space-y-6"
      >
        <div className="text-[120px] font-extrabold leading-none bg-gradient-brand bg-clip-text text-transparent">
          404
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Page not found</h1>
          <p className="text-slate-400 mt-2">The page you're looking for doesn't exist or has been moved.</p>
        </div>
        <Link to="/dashboard" className="btn-primary inline-flex">
          <HiHome className="w-4 h-4" /> Back to Dashboard
        </Link>
      </motion.div>
    </div>
  )
}
