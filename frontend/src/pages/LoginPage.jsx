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
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold text-white">Sign in</h1>
        <p className="text-slate-400 mt-2 text-sm">
          Enter your credentials to access the platform
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Username */}
        <div>
          <label className="label text-slate-400">Username</label>
          <div className="relative">
            <HiUser className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input
              {...register('username', { required: 'Username is required' })}
              placeholder="your.username"
              className={`input pl-10 bg-surface-900 border-white/10 text-white placeholder:text-slate-600
                          focus:border-brand-500 ${errors.username ? 'input-error' : ''}`}
            />
          </div>
          {errors.username && (
            <p className="text-xs text-red-400 mt-1">{errors.username.message}</p>
          )}
        </div>

        {/* Password */}
        <div>
          <label className="label text-slate-400">Password</label>
          <div className="relative">
            <HiLockClosed className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input
              {...register('password', { required: 'Password is required' })}
              type={showPw ? 'text' : 'password'}
              placeholder="••••••••"
              className={`input pl-10 pr-10 bg-surface-900 border-white/10 text-white placeholder:text-slate-600
                          focus:border-brand-500 ${errors.password ? 'input-error' : ''}`}
            />
            <button
              type="button"
              onClick={() => setShowPw(p => !p)}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
            >
              {showPw ? <HiEyeOff className="w-4 h-4" /> : <HiEye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="text-xs text-red-400 mt-1">{errors.password.message}</p>
          )}
        </div>

        {/* Submit */}
        <motion.button
          type="submit"
          disabled={loading}
          whileTap={{ scale: 0.98 }}
          className="btn-primary w-full py-3 text-base mt-2"
        >
          {loading ? <><Spinner size="sm" /> Signing in…</> : 'Sign in'}
        </motion.button>
      </form>

      <p className="text-center text-sm text-slate-500 mt-6">
        Don't have an account?{' '}
        <Link to="/register" className="text-brand-400 hover:text-brand-300 font-semibold">
          Register
        </Link>
      </p>
    </div>
  )
}
