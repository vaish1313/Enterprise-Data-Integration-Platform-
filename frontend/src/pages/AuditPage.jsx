import { useState } from 'react'
import toast from 'react-hot-toast'
import {
  HiRefresh, HiSearch, HiDownload, HiTrash,
} from 'react-icons/hi'
import { auditApi } from '../api/auditApi'
import { useFetch } from '../hooks/useFetch'
import { usePagination } from '../hooks/usePagination'
import { useDebounce } from '../hooks/useDebounce'
import { useAuth } from '../context/AuthContext'
import { DataTable, Pagination } from '../components/Table'
import { ConfirmModal } from '../components/Modal'
import { fmtDateTime, fmtRelative } from '../utils/formatters'

const ACTIONS = [
  'USER_LOGIN','USER_LOGOUT','USER_REGISTER',
  'CREATE_DATA_SOURCE','UPDATE_DATA_SOURCE','DELETE_DATA_SOURCE',
  'INGEST_CSV','INGESTION_STARTED','INGESTION_COMPLETED','INGESTION_FAILED',
  'TRANSFORMATION_EXECUTED','CREATE_RULE','UPDATE_RULE','DELETE_RULE',
  'SYNC_STARTED','SYNC_COMPLETED','SYNC_FAILED',
  'CREATE_USER','UPDATE_USER','DELETE_USER',
]

export default function AuditPage() {
  const { isAdmin, isAnalyst } = useAuth()
  const { page, size, setPage } = usePagination()

  // Search / filter state
  const [search, setSearch]       = useState('')
  const [actionFilter, setAction] = useState('')
  const [statusFilter, setStatus] = useState('')
  const debouncedSearch = useDebounce(search)

  const [purgeOpen, setPurgeOpen] = useState(false)
  const [purging, setPurging]     = useState(false)

  // Pass all active filters to the API
  const { data, loading, refetch } = useFetch(
    () => auditApi.getAll({
      page,
      size,
      ...(actionFilter  && { action: actionFilter }),
      ...(statusFilter  && { status: statusFilter }),
      ...(debouncedSearch && { username: debouncedSearch }),
    }),
    [page, size, actionFilter, statusFilter, debouncedSearch]
  )

  const handleExport = async () => {
    try {
      const blob = await auditApi.exportCsv()
      const url  = URL.createObjectURL(blob)
      const a    = document.createElement('a')
      a.href     = url
      a.download = `audit-export-${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      toast.success('Audit log exported')
    } catch {
      toast.error('Export failed')
    }
  }

  const handlePurge = async () => {
    setPurging(true)
    try {
      await auditApi.purge(90)
      toast.success('Audit logs older than 90 days purged')
      setPurgeOpen(false)
      refetch()
    } catch {
      toast.error('Purge failed')
    } finally {
      setPurging(false)
    }
  }

  const columns = [
    {
      key: 'action', label: 'Action',
      render: v => (
        <span className="font-mono text-xs px-2 py-0.5 rounded-md" style={{ background: 'var(--glass-fill)', color: 'var(--text-primary)', border: '1px solid var(--glass-border)' }}>
          {v}
        </span>
      ),
    },
    {
      key: 'username', label: 'User',
      render: v => (
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold flex-shrink-0" style={{ background: 'var(--text-primary)', color: 'var(--bg-base)' }}>
            {v?.[0]?.toUpperCase()}
          </div>
          <span className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>{v}</span>
        </div>
      ),
    },
    {
      key: 'status', label: 'Status',
      render: v => (
        <span className={`badge ${
          v === 'SUCCESS' ? 'badge-green' :
          v === 'FAILED'  ? 'badge-red'   : 'badge-yellow'
        }`}>
          {v}
        </span>
      ),
    },
    {
      key: 'details', label: 'Details',
      render: v => (
        <span className="text-xs max-w-xs truncate block" style={{ color: 'var(--text-secondary)' }}>
          {v || '—'}
        </span>
      ),
    },
    {
      key: 'ipAddress', label: 'IP',
      render: v => <span className="font-mono text-xs" style={{ color: 'var(--text-muted)' }}>{v || '—'}</span>,
    },
    {
      key: 'timestamp', label: 'Time',
      render: v => (
        <div>
          <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>{fmtDateTime(v)}</p>
          <p className="text-[10px]" style={{ color: 'var(--text-muted)' }}>{fmtRelative(v)}</p>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Audit Logs</h2>
          <p className="page-subtitle">{data?.totalElements ?? 0} total events</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={refetch} className="btn-icon" title="Refresh">
            <HiRefresh className="w-4 h-4" />
          </button>
          {(isAdmin || isAnalyst) && (
            <button onClick={handleExport} className="btn-secondary">
              <HiDownload className="w-4 h-4" /> Export CSV
            </button>
          )}
          {isAdmin && (
            <button onClick={() => setPurgeOpen(true)} className="btn-danger">
              <HiTrash className="w-4 h-4" /> Purge Old
            </button>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="glass-card p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <HiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search by username…"
            className="input pl-9"
          />
        </div>
        <select
          value={actionFilter}
          onChange={e => { setAction(e.target.value); setPage(0) }}
          className="input w-52"
        >
          <option value="">All actions</option>
          {ACTIONS.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
        <select
          value={statusFilter}
          onChange={e => { setStatus(e.target.value); setPage(0) }}
          className="input w-36"
        >
          <option value="">All statuses</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="FAILED">FAILED</option>
          <option value="PARTIAL">PARTIAL</option>
        </select>
      </div>

      {/* Table */}
      <div className="glass-card p-0 overflow-hidden">
        <DataTable
          columns={columns}
          data={data?.content}
          loading={loading}
          emptyMessage="No audit events found"
        />
        <div className="px-4 py-3 border-t" style={{ borderColor: 'var(--glass-border)' }}>
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </div>
      </div>

      <ConfirmModal
        open={purgeOpen}
        onClose={() => setPurgeOpen(false)}
        onConfirm={handlePurge}
        loading={purging}
        danger
        title="Purge Audit Logs"
        message="This will permanently delete all audit log entries older than 90 days. This action cannot be undone."
      />
    </div>
  )
}
