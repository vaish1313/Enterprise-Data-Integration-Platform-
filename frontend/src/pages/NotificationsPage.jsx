import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  HiBell, HiCheckCircle, HiXCircle, HiInformationCircle,
  HiExclamation, HiTrash, HiCheck,
} from 'react-icons/hi'
import { fmtRelative } from '../utils/formatters'
import { notificationsApi } from '../api/notificationsApi'
import toast from 'react-hot-toast'

const ICON = {
  SUCCESS: { icon: HiCheckCircle },
  ERROR:   { icon: HiXCircle },
  INFO:    { icon: HiInformationCircle },
  WARNING: { icon: HiExclamation },
}

export default function NotificationsPage() {
  const [items, setItems] = useState([])
  const [filter, setFilter] = useState('all')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    notificationsApi.getAll()
      .then(data => setItems(data.content)) // Data is paginated, we grab .content
      .catch(() => toast.error('Failed to load notifications'))
      .finally(() => setLoading(false))
  }, [])

  const unread = items.filter(n => !n.read).length

  const markAll = async () => {
    try {
      await notificationsApi.markAllAsRead()
      setItems(i => i.map(n => ({ ...n, read: true })))
    } catch {
      toast.error('Failed to mark all as read')
    }
  }

  const markOne = async (id) => {
    try {
      await notificationsApi.markAsRead(id)
      setItems(i => i.map(n => n.id === id ? { ...n, read: true } : n))
    } catch {
      toast.error('Failed to mark as read')
    }
  }

  const remove = async (id) => {
    try {
      await notificationsApi.delete(id)
      setItems(i => i.filter(n => n.id !== id))
    } catch {
      toast.error('Failed to delete notification')
    }
  }

  const clearAll = async () => {
    try {
      await notificationsApi.deleteAll()
      setItems([])
    } catch {
      toast.error('Failed to clear notifications')
    }
  }

  const filtered = items.filter(n => {
    if (filter === 'unread') return !n.read
    if (filter === 'read')   return n.read
    return true
  })

  return (
    <div className="space-y-5 animate-fade-in max-w-2xl mx-auto">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Notifications</h2>
          <p className="page-subtitle">
            {unread > 0 ? `${unread} unread notification${unread > 1 ? 's' : ''}` : 'All caught up'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {unread > 0 && (
            <button onClick={markAll} className="btn-secondary text-xs">
              <HiCheck className="w-3.5 h-3.5" /> Mark all read
            </button>
          )}
          {items.length > 0 && (
            <button onClick={clearAll} className="btn-ghost text-xs hover:opacity-80" style={{ color: 'var(--text-primary)' }}>
              <HiTrash className="w-3.5 h-3.5" /> Clear all
            </button>
          )}
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-1 p-1 rounded-xl w-fit glass-card" style={{ padding: '4px' }}>
        {['all', 'unread', 'read'].map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold capitalize transition-all ${
              filter === f ? 'shadow-sm' : 'hover:opacity-80'
            }`}
            style={{
              background: filter === f ? 'var(--text-primary)' : 'transparent',
              color: filter === f ? 'var(--bg-base)' : 'var(--text-secondary)'
            }}
          >
            {f}
            {f === 'unread' && unread > 0 && (
              <span className="ml-1.5 text-[10px] px-1.5 py-0.5 rounded-full" style={{ background: filter === f ? 'var(--bg-base)' : 'var(--text-primary)', color: filter === f ? 'var(--text-primary)' : 'var(--bg-base)' }}>
                {unread}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="space-y-2">
        <AnimatePresence>
          {loading ? (
            <div className="p-8 text-center text-sm" style={{ color: 'var(--text-muted)' }}>Loading...</div>
          ) : filtered.length === 0 ? (
            <motion.div
              key="empty"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }}
              className="glass-card p-12 flex flex-col items-center gap-3 text-center"
            >
              <HiBell className="w-10 h-10" style={{ color: 'var(--text-muted)' }} />
              <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>No notifications here</p>
            </motion.div>
          ) : (
            filtered.map((n, i) => {
              const { icon: Icon } = ICON[n.type?.toUpperCase()] || ICON.INFO
              return (
                <motion.div
                  key={n.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: 40, height: 0 }}
                  transition={{ delay: i * 0.03 }}
                  className={`glass-card p-4 flex items-start gap-3 transition-all ${
                    !n.read ? '!border-l-4' : ''
                  }`}
                  style={{ borderLeftColor: !n.read ? 'var(--text-primary)' : 'var(--glass-border)' }}
                >
                  <div className="p-2 rounded-xl flex-shrink-0 mt-0.5" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
                    <Icon className="w-4 h-4" style={{ color: 'var(--text-primary)' }} />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-sm font-semibold" style={{ color: !n.read ? 'var(--text-primary)' : 'var(--text-secondary)' }}>
                        {n.title}
                        {!n.read && <span className="ml-2 w-1.5 h-1.5 rounded-full inline-block align-middle" style={{ background: 'var(--text-primary)' }} />}
                      </p>
                      <span className="text-[11px] flex-shrink-0" style={{ color: 'var(--text-muted)' }}>{fmtRelative(n.time)}</span>
                    </div>
                    <p className="text-xs mt-0.5 leading-relaxed" style={{ color: 'var(--text-secondary)' }}>{n.body}</p>
                  </div>

                  <div className="flex items-center gap-1 flex-shrink-0">
                    {!n.read && (
                      <button onClick={() => markOne(n.id)} className="btn-icon" title="Mark as read">
                        <HiCheck className="w-3.5 h-3.5" />
                      </button>
                    )}
                    <button onClick={() => remove(n.id)} className="btn-icon hover:opacity-80" style={{ color: 'var(--text-primary)' }} title="Dismiss">
                      <HiTrash className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </motion.div>
              )
            })
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}
