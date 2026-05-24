import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import toast from 'react-hot-toast'
import {
  HiDownload, HiRefresh, HiSearch, HiEye, HiUpload,
  HiCheckCircle, HiXCircle, HiClock,
} from 'react-icons/hi'
import { ingestionApi } from '../api/ingestionApi'
import { dataSourceApi } from '../api/dataSourceApi'
import { useFetch } from '../hooks/useFetch'
import { usePagination } from '../hooks/usePagination'
import { useDebounce } from '../hooks/useDebounce'
import { useAuth } from '../context/AuthContext'
import { DataTable, Pagination } from '../components/Table'
import { Modal } from '../components/Modal'
import { Spinner } from '../components/Loader'
import { fmtDateTime, fmtNumber, statusColor } from '../utils/formatters'

/* ── Job detail panel ───────────────────────────────────────────────────── */
function JobDetail({ jobId }) {
  const { data: job, loading } = useFetch(() => ingestionApi.getById(jobId), [jobId])

  if (loading) return <div className="flex justify-center py-8"><Spinner /></div>
  if (!job)    return null

  const stats = [
    { label: 'Total Records',  value: fmtNumber(job.totalRecords),     icon: HiDownload,    color: 'text-brand-400' },
    { label: 'Processed',      value: fmtNumber(job.recordsProcessed), icon: HiCheckCircle, color: 'text-emerald-400' },
    { label: 'Failed',         value: fmtNumber(job.recordsFailed),    icon: HiXCircle,     color: 'text-red-400' },
    { label: 'Started',        value: fmtDateTime(job.startedAt),      icon: HiClock,       color: 'text-blue-400' },
  ]

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-3">
        {stats.map(s => (
          <div key={s.label} className="bg-white dark:bg-surface-800 rounded-xl p-3 flex items-center gap-3">
            <s.icon className={`w-5 h-5 ${s.color} flex-shrink-0`} />
            <div>
              <p className="text-xs text-slate-400">{s.label}</p>
              <p className="text-sm font-bold text-slate-800 dark:text-white">{s.value}</p>
            </div>
          </div>
        ))}
      </div>

      {job.errorMessage && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-3">
          <p className="text-xs font-semibold text-red-600 dark:text-red-400 mb-1">Error</p>
          <p className="text-xs text-red-700 dark:text-red-300 font-mono">{job.errorMessage}</p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-2 text-xs">
        {[
          ['Job ID',       job.id],
          ['Data Source',  job.dataSourceId],
          ['Type',         job.ingestionType],
          ['File',         job.fileName || '—'],
          ['Triggered By', job.triggeredBy || '—'],
          ['Completed',    fmtDateTime(job.completedAt)],
        ].map(([k, v]) => (
          <div key={k} className="bg-white dark:bg-surface-800 rounded-lg p-2.5">
            <p className="text-slate-400 mb-0.5">{k}</p>
            <p className="font-medium text-slate-700 dark:text-slate-200 truncate font-mono text-[11px]">{v}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

/* ── CSV Upload modal ───────────────────────────────────────────────────── */
function UploadModal({ open, onClose, onSuccess }) {
  // Fetch all sources with a large page size so the dropdown is complete
  const { data: sources } = useFetch(() => dataSourceApi.getAll({ page: 0, size: 200 }))
  const [dsId, setDsId]   = useState('')
  const [file, setFile]   = useState(null)
  const [loading, setLoading] = useState(false)

  const handleUpload = async () => {
    if (!dsId) return toast.error('Select a data source')
    if (!file) return toast.error('Select a CSV file')
    setLoading(true)
    try {
      await ingestionApi.uploadCsv(dsId, file)
      toast.success('CSV uploaded — ingestion started')
      onSuccess()
      onClose()
      setDsId('')
      setFile(null)
    } catch {
      toast.error('Upload failed')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => { setDsId(''); setFile(null); onClose() }

  return (
    <Modal open={open} onClose={handleClose} title="Upload CSV" size="sm">
      <div className="space-y-4">
        <div>
          <label className="label">Data Source *</label>
          <select value={dsId} onChange={e => setDsId(e.target.value)} className="input">
            <option value="">Select data source…</option>
            {sources?.content?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">CSV File *</label>
          <input
            type="file"
            accept=".csv"
            onChange={e => setFile(e.target.files[0])}
            className="input file:mr-3 file:py-1 file:px-3 file:rounded-lg file:border-0
                       file:text-xs file:font-semibold file:bg-brand-600 file:text-white
                       hover:file:bg-brand-700 cursor-pointer"
          />
        </div>
        {file && (
          <p className="text-xs text-slate-400">
            Selected: {file.name} ({(file.size / 1024).toFixed(1)} KB)
          </p>
        )}
        <div className="flex justify-end gap-3 pt-2">
          <button onClick={handleClose} className="btn-secondary">Cancel</button>
          <button onClick={handleUpload} className="btn-primary" disabled={loading}>
            {loading
              ? <><Spinner size="sm" /> Uploading…</>
              : <><HiUpload className="w-4 h-4" /> Upload</>
            }
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function IngestionPage() {
  const { canWrite } = useAuth()
  const { page, size, setPage } = usePagination()
  const [search, setSearch]       = useState('')
  const [statusFilter, setStatus] = useState('')
  const [detailId, setDetailId]   = useState(null)
  const [uploadOpen, setUploadOpen] = useState(false)
  const debouncedSearch = useDebounce(search)

  const { data, loading, refetch } = useFetch(
    () => ingestionApi.getAll({
      page,
      size,
      ...(statusFilter    && { status: statusFilter }),
      ...(debouncedSearch && { search: debouncedSearch }),
    }),
    [page, size, statusFilter, debouncedSearch]
  )

  const columns = [
    {
      key: 'id', label: 'Job ID',
      render: v => <span className="font-mono text-xs text-slate-400">{v?.slice(0, 8)}…</span>,
    },
    {
      key: 'dataSourceId', label: 'Data Source',
      render: v => <span className="font-mono text-xs text-slate-400">{v?.slice(0, 8)}…</span>,
    },
    {
      key: 'ingestionType', label: 'Type',
      render: v => <span className="badge-blue">{v}</span>,
    },
    {
      key: 'status', label: 'Status',
      render: v => <span className={statusColor(v)}>{v}</span>,
    },
    { key: 'totalRecords',     label: 'Total',     render: v => fmtNumber(v) },
    {
      key: 'recordsProcessed', label: 'Processed',
      render: v => <span className="text-emerald-500 font-semibold">{fmtNumber(v)}</span>,
    },
    {
      key: 'recordsFailed', label: 'Failed',
      render: v => v > 0
        ? <span className="text-red-400 font-semibold">{fmtNumber(v)}</span>
        : <span className="text-slate-400">0</span>,
    },
    { key: 'startedAt', label: 'Started', render: v => fmtDateTime(v) },
    {
      key: 'actions', label: '', width: '60px',
      render: (_, row) => (
        <button
          onClick={() => setDetailId(detailId === row.id ? null : row.id)}
          className="btn-icon"
          title="View details"
        >
          <HiEye className="w-4 h-4" />
        </button>
      ),
    },
  ]

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Ingestion Jobs</h2>
          <p className="page-subtitle">{data?.totalElements ?? 0} total jobs</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={refetch} className="btn-icon" title="Refresh">
            <HiRefresh className="w-4 h-4" />
          </button>
          {canWrite && (
            <button onClick={() => setUploadOpen(true)} className="btn-primary">
              <HiUpload className="w-4 h-4" /> Upload CSV
            </button>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="card p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <HiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search jobs…"
            className="input pl-9"
          />
        </div>
        <select
          value={statusFilter}
          onChange={e => { setStatus(e.target.value); setPage(0) }}
          className="input w-44"
        >
          <option value="">All statuses</option>
          {['PENDING','RUNNING','COMPLETED','FAILED','PARTIAL'].map(s => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      {/* Table + inline detail */}
      <div className="card p-0 overflow-hidden">
        <DataTable
          columns={columns}
          data={data?.content}
          loading={loading}
          emptyMessage="No ingestion jobs found"
        />

        {/* Inline expandable detail */}
        <AnimatePresence>
          {detailId && (
            <motion.div
              key={detailId}
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-200 dark:border-white/10 overflow-hidden"
            >
              <div className="p-5 bg-slate-50 dark:bg-surface-900/50">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4">
                  Job Details
                </p>
                <JobDetail jobId={detailId} />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="px-4 py-3 border-t border-slate-200 dark:border-white/10">
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </div>
      </div>

      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onSuccess={refetch}
      />
    </div>
  )
}
