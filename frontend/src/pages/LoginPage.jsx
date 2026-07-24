import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import { HiEye, HiEyeOff, HiLockClosed, HiUser } from 'react-icons/hi'
import { useAuth } from '../context/AuthContext'
import { Spinner } from '../components/Loader'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate  = useNavigate()
  const [showPw, setShowPw] = useState(false)
  const [loading, setLoading] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm()

  const onSubmit = async ({ username, password }) => {
    setLoading(true)
    try {
      await login(username, password)
      toast.success(`Welcome back, ${username}!`)
      navigate('/dashboard')
    } catch (err) {
      toast.error(err?.response?.data?.error || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      background: 'rgba(255, 255, 255, 0.08)',
      backdropFilter: 'blur(24px) saturate(180%)',
      WebkitBackdropFilter: 'blur(24px) saturate(180%)',
      border: '1px solid rgba(255, 255, 255, 0.18)',
      borderRadius: '20px',
      boxShadow: '0 8px 32px rgba(0, 0, 0, 0.37), inset 0 1px 0 rgba(255,255,255,0.1)',
      padding: '2rem 2.25rem',
      position: 'relative',
      zIndex: 1
    }}>
      <div className="mb-8">
        <h1 className="text-3xl font-medium" style={{ color: '#f5f5f5' }}>Sign in</h1>
        <p className="mt-2 text-sm" style={{ color: '#a3a3a0' }}>
          Enter your credentials to access the platform
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Username */}
        <div>
          <label className="label uppercase" style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', letterSpacing: '0.06em' }}>USERNAME</label>
          <div className="relative mt-1.5">
            <HiUser className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <input
              {...register('username', { required: 'Username is required' })}
              placeholder="your.username"
              className={`w-full pl-10 px-4 py-2.5 text-sm transition-all duration-200 outline-none focus:bg-[rgba(255,255,255,0.1)] focus:border-[rgba(255,255,255,0.3)] placeholder-[rgba(255,255,255,0.35)] ${errors.username ? 'input-error' : ''}`}
              style={{ background: 'rgba(255, 255, 255, 0.06)', backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', color: '#fff', '--autofill-color': '#fff' }}
            />
          </div>
          {errors.username && (
            <p className="text-xs mt-1" style={{ color: '#a3a3a0' }}>{errors.username.message}</p>
          )}
        </div>

        {/* Password */}
        <div>
          <label className="label uppercase" style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', letterSpacing: '0.06em' }}>PASSWORD</label>
          <div className="relative mt-1.5">
            <HiLockClosed className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <input
              {...register('password', { required: 'Password is required' })}
              type={showPw ? 'text' : 'password'}
              placeholder="••••••••"
              className={`w-full pl-10 pr-10 px-4 py-2.5 text-sm transition-all duration-200 outline-none focus:bg-[rgba(255,255,255,0.1)] focus:border-[rgba(255,255,255,0.3)] placeholder-[rgba(255,255,255,0.35)] ${errors.password ? 'input-error' : ''}`}
              style={{ background: 'rgba(255, 255, 255, 0.06)', backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', color: '#fff', '--autofill-color': '#fff' }}
            />
            <button
              type="button"
              onClick={() => setShowPw(p => !p)}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 hover:opacity-80" style={{ color: 'rgba(255,255,255,0.5)' }}
            >
              {showPw ? <HiEyeOff className="w-4 h-4" /> : <HiEye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="text-xs mt-1" style={{ color: '#a3a3a0' }}>{errors.password.message}</p>
          )}
        </div>

        {/* Submit */}
        <motion.button
          type="submit"
          disabled={loading}
          whileTap={{ scale: 0.98 }}
          className="w-full py-3 text-base mt-4 transition-colors disabled:opacity-50"
          style={{ background: '#f5f5f5', color: '#0a0a0a', borderRadius: '10px', fontWeight: 500, backdropFilter: 'none', WebkitBackdropFilter: 'none' }}
        >
          {loading ? <div className="flex items-center justify-center gap-2"><Spinner size="sm" /> Signing in…</div> : 'Sign in'}
        </motion.button>
      </form>

      <p className="text-center text-sm mt-6" style={{ color: '#a3a3a0' }}>
        Don't have an account?{' '}
        <Link to="/register" className="font-medium hover:underline hover:opacity-100" style={{ color: '#f5f5f5' }}>
          Register
        </Link>
      </p>
    </div>
  )
}
