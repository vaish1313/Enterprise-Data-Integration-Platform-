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
import { TruncatedId } from '../components/TruncatedId'
import { fmtDateTime, fmtNumber, statusColor } from '../utils/formatters'

/* ── Job detail panel ───────────────────────────────────────────────────── */
function JobDetail({ jobId }) {
  const { data: job, loading } = useFetch(() => ingestionApi.getById(jobId), [jobId])

  if (loading) return <div className="flex justify-center py-8"><Spinner /></div>
  if (!job)    return null

  const stats = [
    { label: 'Total Records',  value: fmtNumber(job.totalRecords),     icon: HiDownload },
    { label: 'Processed',      value: fmtNumber(job.recordsProcessed), icon: HiCheckCircle },
    { label: 'Failed',         value: fmtNumber(job.recordsFailed),    icon: HiXCircle },
    { label: 'Started',        value: fmtDateTime(job.startedAt),      icon: HiClock },
  ]

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-3">
        {stats.map(s => (
          <div key={s.label} className="rounded-xl p-3 flex items-center gap-3" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
            <s.icon className={`w-5 h-5 flex-shrink-0`} style={{ color: 'var(--text-primary)' }} />
            <div>
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{s.label}</p>
              <p className="text-sm font-bold" style={{ color: 'var(--text-primary)' }}>{s.value}</p>
            </div>
          </div>
        ))}
      </div>

      {job.errorMessage && (
        <div className="rounded-xl p-3" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)' }}>
          <p className="text-xs font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>Error</p>
          <p className="text-xs font-mono" style={{ color: 'var(--text-secondary)' }}>{job.errorMessage}</p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-2 text-xs">
        {[
          ['Job ID',       <TruncatedId value={job.id} />],
          ['Data Source',  <TruncatedId value={job.dataSourceId} />],
          ['Type',         job.ingestionType],
          ['File',         job.fileName || '—'],
          ['Triggered By', job.triggeredBy || '—'],
          ['Completed',    fmtDateTime(job.completedAt)],
        ].map(([k, v]) => (
          <div key={k} className="rounded-lg p-2.5" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
            <p className="mb-0.5" style={{ color: 'var(--text-muted)' }}>{k}</p>
            <p className="font-medium truncate font-mono text-[11px]" style={{ color: 'var(--text-primary)' }}>{v}</p>
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
                       file:text-xs file:font-semibold file:bg-white file:text-black
                       hover:file:opacity-80 cursor-pointer"
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
      render: v => <TruncatedId value={v} />,
    },
    {
      key: 'dataSourceId', label: 'Data Source',
      render: v => <TruncatedId value={v} />,
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
      render: v => <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>{fmtNumber(v)}</span>,
    },
    {
      key: 'recordsFailed', label: 'Failed',
      render: v => v > 0
        ? <span className="font-semibold" style={{ color: 'var(--text-secondary)' }}>{fmtNumber(v)}</span>
        : <span style={{ color: 'var(--text-muted)' }}>0</span>,
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
      <div className="glass-card p-4 flex flex-wrap gap-3">
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
      <div className="glass-card p-0 overflow-hidden">
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
              className="border-t overflow-hidden" style={{ borderColor: 'var(--glass-border)' }}
            >
              <div className="p-5" style={{ background: 'var(--glass-fill)' }}>
                <p className="text-xs font-bold uppercase tracking-wider mb-4" style={{ color: 'var(--text-muted)' }}>
                  Job Details
                </p>
                <JobDetail jobId={detailId} />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="px-4 py-3 border-t" style={{ borderColor: 'var(--glass-border)' }}>
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
