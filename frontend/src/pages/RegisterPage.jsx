import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import { HiEye, HiEyeOff, HiLockClosed, HiUser, HiMail } from 'react-icons/hi'
import { authApi } from '../api/authApi'
import { Spinner } from '../components/Loader'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [showPw, setShowPw] = useState(false)
  const [loading, setLoading] = useState(false)

  const { register, handleSubmit, watch, formState: { errors } } = useForm()
  const pw = watch('password')

  const onSubmit = async (data) => {
    setLoading(true)
    try {
      await authApi.register({ username: data.username, email: data.email, password: data.password })
      toast.success('Account created! Please sign in.')
      navigate('/login')
    } catch (err) {
      toast.error(err?.response?.data?.error || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold text-white">Create account</h1>
        <p className="text-slate-400 mt-2 text-sm">Join the EDIP platform</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="label text-slate-400">Username</label>
          <div className="relative">
            <HiUser className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input {...register('username', { required: 'Required', minLength: { value: 3, message: 'Min 3 chars' } })}
              placeholder="username" className={`input pl-10 bg-surface-900 border-white/10 text-white placeholder:text-slate-600 focus:border-brand-500 ${errors.username ? 'input-error' : ''}`} />
          </div>
          {errors.username && <p className="text-xs text-red-400 mt-1">{errors.username.message}</p>}
        </div>

        <div>
          <label className="label text-slate-400">Email</label>
          <div className="relative">
            <HiMail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input {...register('email', { required: 'Required', pattern: { value: /\S+@\S+\.\S+/, message: 'Invalid email' } })}
              placeholder="you@company.com" className={`input pl-10 bg-surface-900 border-white/10 text-white placeholder:text-slate-600 focus:border-brand-500 ${errors.email ? 'input-error' : ''}`} />
          </div>
          {errors.email && <p className="text-xs text-red-400 mt-1">{errors.email.message}</p>}
        </div>

        <div>
          <label className="label text-slate-400">Password</label>
          <div className="relative">
            <HiLockClosed className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input {...register('password', {
              required: 'Required',
              minLength: { value: 8, message: 'Min 8 characters' },
              pattern: {
                value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/,
                message: 'Must include uppercase, lowercase, number and one of @ $ ! % * ? &'
              }
            })}
              type={showPw ? 'text' : 'password'} placeholder="e.g. Admin@1234"
              className={`input pl-10 pr-10 bg-surface-900 border-white/10 text-white placeholder:text-slate-600 focus:border-brand-500 ${errors.password ? 'input-error' : ''}`} />
            <button type="button" onClick={() => setShowPw(p => !p)} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
              {showPw ? <HiEyeOff className="w-4 h-4" /> : <HiEye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password
            ? <p className="text-xs text-red-400 mt-1">{errors.password.message}</p>
            : <p className="text-xs text-slate-500 mt-1">Min 8 chars · uppercase · lowercase · digit · special char from @ $ ! % * ? &</p>
          }
        </div>

        <div>
          <label className="label text-slate-400">Confirm Password</label>
          <div className="relative">
            <HiLockClosed className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input {...register('confirm', { required: 'Required', validate: v => v === pw || 'Passwords do not match' })}
              type="password" placeholder="••••••••"
              className={`input pl-10 bg-surface-900 border-white/10 text-white placeholder:text-slate-600 focus:border-brand-500 ${errors.confirm ? 'input-error' : ''}`} />
          </div>
          {errors.confirm && <p className="text-xs text-red-400 mt-1">{errors.confirm.message}</p>}
        </div>

        <motion.button type="submit" disabled={loading} whileTap={{ scale: 0.98 }} className="btn-primary w-full py-3 text-base mt-2">
          {loading ? <><Spinner size="sm" /> Creating account…</> : 'Create account'}
        </motion.button>
      </form>

      <p className="text-center text-sm text-slate-500 mt-6">
        Already have an account?{' '}
        <Link to="/login" className="text-brand-400 hover:text-brand-300 font-semibold">Sign in</Link>
      </p>
    </div>
  )
}
