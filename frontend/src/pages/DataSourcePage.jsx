import { useState } from 'react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import {
  HiPlus, HiSearch, HiPencil, HiTrash, HiDatabase,
  HiRefresh, HiCheckCircle, HiXCircle, HiShieldExclamation,
} from 'react-icons/hi'
import { dataSourceApi } from '../api/dataSourceApi'
import { useFetch } from '../hooks/useFetch'
import { useDebounce } from '../hooks/useDebounce'
import { usePagination } from '../hooks/usePagination'
import { useAuth } from '../context/AuthContext'
import { DataTable, Pagination } from '../components/Table'
import { Modal, ConfirmModal } from '../components/Modal'
import { statusColor } from '../utils/formatters'

const SOURCE_TYPES = ['CSV', 'REST_API', 'DATABASE']
const STATUSES     = ['ACTIVE', 'INACTIVE', 'ERROR']

// ─────────────────────────────────────────────────────────────────────────────
// Circuit Breaker Badge
// Renders a coloured dot + label for the circuit_state field.
// Green = CLOSED (healthy), Yellow = HALF_OPEN (testing), Red = OPEN (suspended)
// ─────────────────────────────────────────────────────────────────────────────
function CircuitBadge({ circuitState, consecutiveFailureCount }) {
  if (!circuitState || circuitState === 'CLOSED') {
    return (
      <span
        id="circuit-badge-closed"
        className="inline-flex items-center gap-1.5 text-xs font-medium"
        style={{ color: 'var(--text-secondary)' }}
        title={`Circuit: CLOSED — ${consecutiveFailureCount} failure(s) recorded`}
      >
        <span
          className="w-2 h-2 rounded-full flex-shrink-0"
          style={{ background: '#22c55e', boxShadow: '0 0 4px #22c55e66' }}
        />
        CLOSED
      </span>
    )
  }

  if (circuitState === 'HALF_OPEN') {
    return (
      <span
        id="circuit-badge-half-open"
        className="inline-flex items-center gap-1.5 text-xs font-semibold animate-pulse"
        style={{ color: '#f59e0b' }}
        title="Circuit: HALF_OPEN — one test attempt in progress"
      >
        <span
          className="w-2 h-2 rounded-full flex-shrink-0"
          style={{ background: '#f59e0b', boxShadow: '0 0 6px #f59e0b88' }}
        />
        HALF_OPEN
      </span>
    )
  }

  if (circuitState === 'OPEN') {
    return (
      <span
        id="circuit-badge-open"
        className="inline-flex items-center gap-1.5 text-xs font-semibold"
        style={{ color: '#ef4444' }}
        title={`Circuit: OPEN — suspended after ${consecutiveFailureCount} failures`}
      >
        <span
          className="w-2 h-2 rounded-full flex-shrink-0"
          style={{ background: '#ef4444', boxShadow: '0 0 6px #ef444488' }}
        />
        OPEN
      </span>
    )
  }

  return null
}

// ─────────────────────────────────────────────────────────────────────────────
// Source Form (create / edit)
// ─────────────────────────────────────────────────────────────────────────────
function SourceForm({ onSubmit, defaultValues, loading }) {
  const { register, handleSubmit, formState: { errors } } = useForm({ defaultValues })
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <label className="label">Name *</label>
        <input {...register('name', { required: 'Name is required' })}
          className={`input ${errors.name ? 'input-error' : ''}`} placeholder="My Data Source" />
        {errors.name && <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{errors.name.message}</p>}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Source Type *</label>
          <select {...register('sourceType', { required: 'Required' })} className={`input ${errors.sourceType ? 'input-error' : ''}`}>
            <option value="">Select type…</option>
            {SOURCE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
          {errors.sourceType && <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{errors.sourceType.message}</p>}
        </div>
        <div>
          <label className="label">Status</label>
          <select {...register('status')} className="input">
            {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
      </div>

      <div>
        <label className="label">Description</label>
        <textarea {...register('description')} rows={2}
          className="input resize-none" placeholder="Optional description…" />
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Saving…' : 'Save'}
        </button>
      </div>
    </form>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reset Circuit Confirm Modal
// Shown when admin clicks "Reset Circuit" — explains what the action does
// ─────────────────────────────────────────────────────────────────────────────
function ResetCircuitModal({ open, onClose, onConfirm, loading, source }) {
  if (!source) return null
  return (
    <Modal open={open} onClose={onClose} title="Reset Circuit Breaker">
      <div className="space-y-4">
        {/* Warning banner */}
        <div
          className="flex items-start gap-3 rounded-lg p-3"
          style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.25)' }}
        >
          <HiShieldExclamation className="w-5 h-5 flex-shrink-0 mt-0.5" style={{ color: '#ef4444' }} />
          <div>
            <p className="text-sm font-semibold" style={{ color: '#ef4444' }}>
              Admin override — use only after fixing the underlying issue
            </p>
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-secondary)' }}>
              Resetting the circuit before the root cause is resolved will result
              in repeated failures re-tripping it immediately.
            </p>
          </div>
        </div>

        {/* Source info */}
        <div className="space-y-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
          <div className="flex justify-between">
            <span>Data source</span>
            <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>{source.name}</span>
          </div>
          <div className="flex justify-between">
            <span>Current circuit state</span>
            <CircuitBadge circuitState={source.circuitState} consecutiveFailureCount={source.consecutiveFailureCount} />
          </div>
          <div className="flex justify-between">
            <span>Consecutive failures</span>
            <span className="font-mono font-semibold" style={{ color: '#ef4444' }}>
              {source.consecutiveFailureCount ?? 0}
            </span>
          </div>
          {source.suspendedUntil && (
            <div className="flex justify-between">
              <span>Suspended until</span>
              <span className="font-mono text-xs">{new Date(source.suspendedUntil).toLocaleTimeString()}</span>
            </div>
          )}
        </div>

        {/* What will happen */}
        <div
          className="rounded-lg p-3 text-xs space-y-1"
          style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)', color: 'var(--text-secondary)' }}
        >
          <p className="font-semibold" style={{ color: 'var(--text-primary)' }}>This action will:</p>
          <p>• Set circuit_state → <span className="text-green-400 font-semibold">CLOSED</span></p>
          <p>• Reset consecutive failure count → <span className="font-semibold">0</span></p>
          <p>• Clear suspension window</p>
          <p>• Restore source status → <span className="text-green-400 font-semibold">ACTIVE</span></p>
          <p>• Write a CIRCUIT_BREAKER_RESET audit log entry</p>
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <button onClick={onClose} className="btn-secondary" disabled={loading}>Cancel</button>
          <button
            id="btn-confirm-reset-circuit"
            onClick={onConfirm}
            disabled={loading}
            className="btn-primary"
            style={{ background: 'linear-gradient(135deg, #ef4444, #b91c1c)' }}
          >
            {loading ? 'Resetting…' : 'Reset Circuit'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Page
// ─────────────────────────────────────────────────────────────────────────────
export default function DataSourcePage() {
  const { canWrite, isAdmin } = useAuth()
  const { page, size, setPage } = usePagination()
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const debouncedSearch = useDebounce(search)

  const [createOpen, setCreateOpen]         = useState(false)
  const [editTarget, setEditTarget]         = useState(null)
  const [deleteTarget, setDeleteTarget]     = useState(null)
  const [resetTarget, setResetTarget]       = useState(null)   // circuit reset target
  const [saving, setSaving]                 = useState(false)
  const [deleting, setDeleting]             = useState(false)
  const [resetting, setResetting]           = useState(false)

  const { data, loading, refetch } = useFetch(
    () => dataSourceApi.getAll({ page, size, name: debouncedSearch || undefined, sourceType: typeFilter || undefined, status: statusFilter || undefined }),
    [page, size, debouncedSearch, typeFilter, statusFilter]
  )

  const handleCreate = async (values) => {
    setSaving(true)
    try {
      await dataSourceApi.create(values)
      toast.success('Data source created')
      setCreateOpen(false)
      refetch()
    } catch { toast.error('Failed to create') }
    finally { setSaving(false) }
  }

  const handleEdit = async (values) => {
    setSaving(true)
    try {
      await dataSourceApi.update(editTarget.id, values)
      toast.success('Data source updated')
      setEditTarget(null)
      refetch()
    } catch { toast.error('Failed to update') }
    finally { setSaving(false) }
  }

  const handleDelete = async () => {
    setDeleting(true)
    try {
      await dataSourceApi.delete(deleteTarget.id)
      toast.success('Data source deleted')
      setDeleteTarget(null)
      refetch()
    } catch { toast.error('Failed to delete') }
    finally { setDeleting(false) }
  }

  const handleResetCircuit = async () => {
    setResetting(true)
    try {
      await dataSourceApi.resetCircuit(resetTarget.id)
      toast.success(`Circuit reset for "${resetTarget.name}" — source is now ACTIVE`)
      setResetTarget(null)
      refetch()
    } catch { toast.error('Failed to reset circuit breaker') }
    finally { setResetting(false) }
  }

  const columns = [
    {
      key: 'name', label: 'Name',
      render: (v, row) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
            <HiDatabase className="w-4 h-4" style={{ color: 'var(--text-primary)' }} />
          </div>
          <div>
            <p className="font-semibold text-sm" style={{ color: 'var(--text-primary)' }}>{v}</p>
            <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>{row.description || '—'}</p>
          </div>
        </div>
      ),
    },
    { key: 'sourceType', label: 'Type',
      render: v => <span className="badge-blue">{v}</span> },
    {
      key: 'status', label: 'Status',
      render: v => <span className={statusColor(v)}>{v}</span>,
    },
    {
      // Circuit state column — shows coloured dot + label, visible to all roles
      key: 'circuitState', label: 'Circuit',
      render: (v, row) => (
        <CircuitBadge
          circuitState={v}
          consecutiveFailureCount={row.consecutiveFailureCount}
        />
      ),
    },
    { key: 'createdBy', label: 'Created By' },
    { key: 'createdAt', label: 'Created',
      render: v => v ? new Date(v).toLocaleDateString() : '—' },
    {
      key: 'actions', label: '', width: '120px',
      render: (_, row) => (
        <div className="flex items-center gap-1">
          {/* Edit — ADMIN / ANALYST */}
          {canWrite && (
            <button
              id={`btn-edit-source-${row.id}`}
              onClick={() => setEditTarget(row)}
              className="btn-icon"
              title="Edit"
            >
              <HiPencil className="w-4 h-4" />
            </button>
          )}

          {/* Delete — ADMIN only */}
          {isAdmin && (
            <button
              id={`btn-delete-source-${row.id}`}
              onClick={() => setDeleteTarget(row)}
              className="btn-icon hover:opacity-80"
              style={{ color: 'var(--text-primary)' }}
              title="Delete"
            >
              <HiTrash className="w-4 h-4" />
            </button>
          )}

          {/* Reset Circuit — ADMIN only, visible only when circuit is not CLOSED */}
          {isAdmin && row.circuitState && row.circuitState !== 'CLOSED' && (
            <motion.button
              id={`btn-reset-circuit-${row.id}`}
              onClick={() => setResetTarget(row)}
              className="btn-icon"
              title={`Reset circuit breaker (currently ${row.circuitState})`}
              style={{ color: '#ef4444' }}
              whileHover={{ scale: 1.15 }}
              whileTap={{ scale: 0.92 }}
              animate={{ opacity: [1, 0.6, 1] }}
              transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
            >
              <HiShieldExclamation className="w-4 h-4" />
            </motion.button>
          )}
        </div>
      ),
    },
  ]

  // Count OPEN + HALF_OPEN sources for the header warning banner
  const openCount = data?.content?.filter(s => s.circuitState === 'OPEN').length ?? 0
  const degradedCount = data?.content?.filter(s => s.circuitState === 'HALF_OPEN').length ?? 0

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Data Sources</h2>
          <p className="page-subtitle">{data?.totalElements ?? 0} sources registered</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={refetch} className="btn-icon" title="Refresh"><HiRefresh className="w-4 h-4" /></button>
          {canWrite && (
            <button onClick={() => setCreateOpen(true)} className="btn-primary">
              <HiPlus className="w-4 h-4" /> New Source
            </button>
          )}
        </div>
      </div>

      {/* Circuit Breaker Warning Banner — shown to admins when any circuit is open */}
      {isAdmin && (openCount > 0 || degradedCount > 0) && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-start gap-3 rounded-xl px-4 py-3"
          style={{
            background: openCount > 0
              ? 'linear-gradient(135deg, rgba(239,68,68,0.10), rgba(239,68,68,0.04))'
              : 'linear-gradient(135deg, rgba(245,158,11,0.10), rgba(245,158,11,0.04))',
            border: `1px solid ${openCount > 0 ? 'rgba(239,68,68,0.30)' : 'rgba(245,158,11,0.30)'}`,
          }}
        >
          <HiShieldExclamation
            className="w-5 h-5 flex-shrink-0 mt-0.5"
            style={{ color: openCount > 0 ? '#ef4444' : '#f59e0b' }}
          />
          <div className="flex-1 text-sm">
            <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>
              Circuit Breaker Alert&nbsp;
            </span>
            <span style={{ color: 'var(--text-secondary)' }}>
              {openCount > 0 && <>{openCount} source{openCount > 1 ? 's' : ''} <span style={{ color: '#ef4444', fontWeight: 600 }}>SUSPENDED</span> (scheduler skipping){degradedCount > 0 ? ', ' : ''}</>}
              {degradedCount > 0 && <>{degradedCount} source{degradedCount > 1 ? 's' : ''} in <span style={{ color: '#f59e0b', fontWeight: 600 }}>HALF_OPEN</span> recovery test</>}
              . Click the <HiShieldExclamation className="inline w-3.5 h-3.5" style={{ color: '#ef4444' }} /> icon to reset after fixing the issue.
            </span>
          </div>
        </motion.div>
      )}

      {/* Filters */}
      <div className="glass-card p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <HiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search by name…" className="input pl-9" />
        </div>
        <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value); setPage(0) }} className="input w-40">
          <option value="">All types</option>
          {SOURCE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0) }} className="input w-44">
          <option value="">All statuses</option>
          {[...STATUSES, 'DEGRADED', 'SUSPENDED'].map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {/* Table */}
      <div className="glass-card p-0 overflow-hidden">
        <DataTable columns={columns} data={data?.content} loading={loading} emptyMessage="No data sources found" />
        <div className="px-4 py-3 border-t" style={{ borderColor: 'var(--glass-border)' }}>
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </div>
      </div>

      {/* Create modal */}
      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="New Data Source">
        <SourceForm onSubmit={handleCreate} loading={saving} defaultValues={{ status: 'INACTIVE' }} />
      </Modal>

      {/* Edit modal */}
      <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Edit Data Source">
        {editTarget && <SourceForm onSubmit={handleEdit} loading={saving} defaultValues={editTarget} />}
      </Modal>

      {/* Delete confirm */}
      <ConfirmModal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        danger
        title="Delete Data Source"
        message={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
      />

      {/* Reset Circuit modal — admin only */}
      <ResetCircuitModal
        open={!!resetTarget}
        onClose={() => setResetTarget(null)}
        onConfirm={handleResetCircuit}
        loading={resetting}
        source={resetTarget}
      />
    </div>
  )
}
