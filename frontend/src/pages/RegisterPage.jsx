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
        <h1 className="text-3xl font-medium" style={{ color: '#f5f5f5' }}>Create account</h1>
        <p className="mt-2 text-sm" style={{ color: '#a3a3a0' }}>Join the EDIP platform</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="label uppercase" style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', letterSpacing: '0.06em' }}>USERNAME</label>
          <div className="relative mt-1.5">
            <HiUser className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <input {...register('username', { required: 'Required', minLength: { value: 3, message: 'Min 3 chars' } })}
              placeholder="username" className={`w-full pl-10 px-4 py-2.5 text-sm transition-all duration-200 outline-none focus:bg-[rgba(255,255,255,0.1)] focus:border-[rgba(255,255,255,0.3)] placeholder-[rgba(255,255,255,0.35)] ${errors.username ? 'input-error' : ''}`}
              style={{ background: 'rgba(255, 255, 255, 0.06)', backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', color: '#fff', '--autofill-color': '#fff' }} />
          </div>
          {errors.username && <p className="text-xs mt-1" style={{ color: '#a3a3a0' }}>{errors.username.message}</p>}
        </div>

        <div>
          <label className="label uppercase" style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', letterSpacing: '0.06em' }}>EMAIL</label>
          <div className="relative mt-1.5">
            <HiMail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <input {...register('email', { required: 'Required', pattern: { value: /\S+@\S+\.\S+/, message: 'Invalid email' } })}
              placeholder="you@company.com" className={`w-full pl-10 px-4 py-2.5 text-sm transition-all duration-200 outline-none focus:bg-[rgba(255,255,255,0.1)] focus:border-[rgba(255,255,255,0.3)] placeholder-[rgba(255,255,255,0.35)] ${errors.email ? 'input-error' : ''}`}
              style={{ background: 'rgba(255, 255, 255, 0.06)', backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', color: '#fff', '--autofill-color': '#fff' }} />
          </div>
          {errors.email && <p className="text-xs mt-1" style={{ color: '#a3a3a0' }}>{errors.email.message}</p>}
        </div>

        <div>
          <label className="label uppercase" style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', letterSpacing: '0.06em' }}>PASSWORD</label>
          <div className="relative mt-1.5">
            <HiLockClosed className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <input {...register('password', {
              required: 'Required',
              minLength: { value: 8, message: 'Min 8 characters' },
              pattern: {
                value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/,
                message: 'Must include uppercase, lowercase, number and one of @ $ ! % * ? &'
              }
            })}
              type={showPw ? 'text' : 'password'} placeholder="e.g. Admin@1234"
              className={`w-full pl-10 pr-10 px-4 py-2.5 text-sm transition-all duration-200 outline-none focus:bg-[rgba(255,255,255,0.1)] focus:border-[rgba(255,255,255,0.3)] placeholder-[rgba(255,255,255,0.35)] ${errors.password ? 'input-error' : ''}`}
              style={{ background: 'rgba(255, 255, 255, 0.06)', backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', color: '#fff', '--autofill-color': '#fff' }} />
            <button type="button" onClick={() => setShowPw(p => !p)} className="absolute right-3.5 top-1/2 -translate-y-1/2 hover:opacity-80" style={{ color: 'rgba(255,255,255,0.5)' }}>
              {showPw ? <HiEyeOff className="w-4 h-4" /> : <HiEye className="w-4 h-4" />}
            </button>
          </div>
          {errors.password
            ? <p className="text-xs mt-1" style={{ color: '#a3a3a0' }}>{errors.password.message}</p>
            : <p className="text-xs mt-1" style={{ color: '#a3a3a0', opacity: 0.8 }}>Min 8 chars · uppercase · lowercase · digit · special char from @ $ ! % * ? &</p>
          }
        </div>

        <div>
          <label className="label uppercase" style={{ color: 'rgba(255,255,255,0.5)', fontSize: '11px', letterSpacing: '0.06em' }}>CONFIRM PASSWORD</label>
          <div className="relative mt-1.5">
            <HiLockClosed className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <input {...register('confirm', { required: 'Required', validate: v => v === pw || 'Passwords do not match' })}
              type="password" placeholder="••••••••"
              className={`w-full pl-10 px-4 py-2.5 text-sm transition-all duration-200 outline-none focus:bg-[rgba(255,255,255,0.1)] focus:border-[rgba(255,255,255,0.3)] placeholder-[rgba(255,255,255,0.35)] ${errors.confirm ? 'input-error' : ''}`}
              style={{ background: 'rgba(255, 255, 255, 0.06)', backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', color: '#fff', '--autofill-color': '#fff' }} />
          </div>
          {errors.confirm && <p className="text-xs mt-1" style={{ color: '#a3a3a0' }}>{errors.confirm.message}</p>}
        </div>

        <motion.button 
          type="submit" 
          disabled={loading} 
          whileTap={{ scale: 0.98 }} 
          className="w-full py-3 text-base mt-4 transition-colors disabled:opacity-50"
          style={{ background: '#f5f5f5', color: '#0a0a0a', borderRadius: '10px', fontWeight: 500, backdropFilter: 'none', WebkitBackdropFilter: 'none' }}
        >
          {loading ? <div className="flex items-center justify-center gap-2"><Spinner size="sm" /> Creating account…</div> : 'Create account'}
        </motion.button>
      </form>

      <p className="text-center text-sm mt-6" style={{ color: '#a3a3a0' }}>
        Already have an account?{' '}
        <Link to="/login" className="font-medium hover:underline hover:opacity-100" style={{ color: '#f5f5f5' }}>Sign in</Link>
      </p>
    </div>
  )
}
