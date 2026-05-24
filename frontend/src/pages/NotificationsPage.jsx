import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  HiBell, HiCheckCircle, HiXCircle, HiInformationCircle,
  HiExclamation, HiTrash, HiCheck,
} from 'react-icons/hi'
import { fmtRelative } from '../utils/formatters'

/* ── Mock notifications (in a real app these come from a WebSocket / API) */
const MOCK = [
  { id: 1, type: 'success', title: 'Sync Completed',       body: 'Data source "CRM Export" synced 12,450 records successfully.',  time: new Date(Date.now() - 2 * 60000),   read: false },
  { id: 2, type: 'error',   title: 'Ingestion Failed',     body: 'CSV upload for "Sales Q2" failed: invalid column format.',       time: new Date(Date.now() - 15 * 60000),  read: false },
  { id: 3, type: 'info',    title: 'Scheduler Running',    body: 'Scheduled sync started for 3 active data sources.',              time: new Date(Date.now() - 60 * 60000),  read: true  },
  { id: 4, type: 'warning', title: 'Sync Partial',         body: '42 records failed validation during sync of "ERP Connector".',   time: new Date(Date.now() - 3 * 3600000), read: true  },
  { id: 5, type: 'success', title: 'Rule Applied',         body: 'Transformation rule "Uppercase Email" applied to 8,200 records.',time: new Date(Date.now() - 5 * 3600000), read: true  },
  { id: 6, type: 'info',    title: 'New User Registered',  body: 'User "alice.smith" registered with ANALYST role.',               time: new Date(Date.now() - 86400000),    read: true  },
  { id: 7, type: 'error',   title: 'Sync Failed',          body: 'Sync job for "Legacy DB" failed: connection timeout.',           time: new Date(Date.now() - 2 * 86400000),read: true  },
]

const ICON = {
  success: { icon: HiCheckCircle,      bg: 'bg-emerald-500/10', text: 'text-emerald-400' },
  error:   { icon: HiXCircle,          bg: 'bg-red-500/10',     text: 'text-red-400' },
  info:    { icon: HiInformationCircle, bg: 'bg-blue-500/10',   text: 'text-blue-400' },
  warning: { icon: HiExclamation,      bg: 'bg-amber-500/10',   text: 'text-amber-400' },
}

export default function NotificationsPage() {
  const [items, setItems] = useState(MOCK)
  const [filter, setFilter] = useState('all')

  const unread = items.filter(n => !n.read).length

  const markAll  = () => setItems(i => i.map(n => ({ ...n, read: true })))
  const markOne  = (id) => setItems(i => i.map(n => n.id === id ? { ...n, read: true } : n))
  const remove   = (id) => setItems(i => i.filter(n => n.id !== id))
  const clearAll = () => setItems([])

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
            <button onClick={clearAll} className="btn-ghost text-xs text-red-400 hover:text-red-500">
              <HiTrash className="w-3.5 h-3.5" /> Clear all
            </button>
          )}
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-1 p-1 bg-slate-100 dark:bg-surface-800 rounded-xl w-fit">
        {['all', 'unread', 'read'].map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold capitalize transition-all ${
              filter === f
                ? 'bg-white dark:bg-surface-700 text-slate-800 dark:text-white shadow-sm'
                : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
            }`}
          >
            {f}
            {f === 'unread' && unread > 0 && (
              <span className="ml-1.5 bg-brand-500 text-white text-[10px] px-1.5 py-0.5 rounded-full">
                {unread}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="space-y-2">
        <AnimatePresence>
          {filtered.length === 0 ? (
            <motion.div
              key="empty"
              initial={{ opacity: 0 }} animate={{ opacity: 1 }}
              className="card p-12 flex flex-col items-center gap-3 text-center"
            >
              <HiBell className="w-10 h-10 text-slate-300 dark:text-slate-600" />
              <p className="text-slate-500 dark:text-slate-400 text-sm">No notifications here</p>
            </motion.div>
          ) : (
            filtered.map((n, i) => {
              const { icon: Icon, bg, text } = ICON[n.type]
              return (
                <motion.div
                  key={n.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: 40, height: 0 }}
                  transition={{ delay: i * 0.03 }}
                  className={`card p-4 flex items-start gap-3 transition-all ${
                    !n.read ? 'border-l-4 border-l-brand-500' : ''
                  }`}
                >
                  <div className={`p-2 rounded-xl ${bg} flex-shrink-0 mt-0.5`}>
                    <Icon className={`w-4 h-4 ${text}`} />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <p className={`text-sm font-semibold ${!n.read ? 'text-slate-900 dark:text-white' : 'text-slate-600 dark:text-slate-300'}`}>
                        {n.title}
                        {!n.read && <span className="ml-2 w-1.5 h-1.5 rounded-full bg-brand-500 inline-block align-middle" />}
                      </p>
                      <span className="text-[11px] text-slate-400 flex-shrink-0">{fmtRelative(n.time)}</span>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 leading-relaxed">{n.body}</p>
                  </div>

                  <div className="flex items-center gap-1 flex-shrink-0">
                    {!n.read && (
                      <button onClick={() => markOne(n.id)} className="btn-icon" title="Mark as read">
                        <HiCheck className="w-3.5 h-3.5" />
                      </button>
                    )}
                    <button onClick={() => remove(n.id)} className="btn-icon text-red-400 hover:text-red-500" title="Dismiss">
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
