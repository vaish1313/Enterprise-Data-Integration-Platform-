import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import toast from 'react-hot-toast'
import {
  HiRefresh, HiPlay, HiCheckCircle, HiXCircle, HiClock,
  HiChartBar, HiEye,
} from 'react-icons/hi'
import { syncApi } from '../api/syncApi'
import { dataSourceApi } from '../api/dataSourceApi'
import { useFetch } from '../hooks/useFetch'
import { usePagination } from '../hooks/usePagination'
import { useAuth } from '../context/AuthContext'
import { DataTable, Pagination } from '../components/Table'
import { Modal } from '../components/Modal'
import { Spinner } from '../components/Loader'
import { fmtDateTime, fmtNumber, fmtMs, fmtPercent, fmtRelative, statusColor } from '../utils/formatters'

/* ── Stats card ─────────────────────────────────────────────────────────── */
function StatCard({ label, value, icon: Icon, color }) {
  return (
    <div className="glass-card p-4 flex items-center gap-3">
      <div className="p-2.5 rounded-xl" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
        <Icon className="w-5 h-5" style={{ color: 'var(--text-primary)' }} />
      </div>
      <div>
        <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>{label}</p>
        <p className="text-lg font-extrabold" style={{ color: 'var(--text-primary)' }}>{value}</p>
      </div>
    </div>
  )
}

/* ── Run Sync modal ─────────────────────────────────────────────────────── */
function RunSyncModal({ open, onClose, onSuccess }) {
  // Fetch all sources with a large page size so the dropdown is complete
  const { data: sources } = useFetch(() => dataSourceApi.getAll({ page: 0, size: 200 }))
  const [dsId, setDsId]     = useState('')
  const [loading, setLoading] = useState(false)
  const [report, setReport]   = useState(null)

  const handleRun = async () => {
    if (!dsId) return toast.error('Select a data source')
    setLoading(true)
    setReport(null)
    try {
      const result = await syncApi.run(dsId)
      setReport(result)
      toast.success('Sync completed')
      onSuccess()
    } catch {
      toast.error('Sync failed')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => { setReport(null); setDsId(''); onClose() }

  return (
    <Modal open={open} onClose={handleClose} title="Run Synchronization" size="md">
      {!report ? (
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
          <p className="text-xs text-slate-400">
            Fetches all PROCESSED, un-synchronized records for the selected data source,
            validates them, and marks them as synchronized.
          </p>
          <div className="flex justify-end gap-3 pt-2">
            <button onClick={handleClose} className="btn-secondary">Cancel</button>
            <button onClick={handleRun} className="btn-primary" disabled={loading}>
              {loading
                ? <><Spinner size="sm" /> Running…</>
                : <><HiPlay className="w-4 h-4" /> Run Sync</>
              }
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="rounded-xl p-4 border" style={{ background: 'rgba(255,255,255,0.05)', borderColor: 'var(--glass-border)' }}>
            <div className="flex items-center gap-2 mb-2">
              {report.fullySuccessful
                ? <HiCheckCircle className="w-5 h-5" style={{ color: 'var(--text-primary)' }} />
                : <HiXCircle className="w-5 h-5" style={{ color: 'var(--text-secondary)' }} />
              }
              <p className="font-semibold text-sm" style={{ color: 'var(--text-primary)' }}>
                {report.fullySuccessful ? 'Sync Successful' : 'Sync Completed with Issues'}
              </p>
            </div>
            <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>{report.summary}</p>
            {report.recommendation && (
              <p className="text-xs mt-2" style={{ color: 'var(--text-muted)' }}>
                {report.recommendation}
              </p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs">
            {[
              ['Status',    report.job?.status],
              ['Processed', fmtNumber(report.job?.recordsProcessed)],
              ['Failed',    fmtNumber(report.job?.recordsFailed)],
              ['Exec Time', fmtMs(report.job?.executionTimeMs)],
            ].map(([k, v]) => (
              <div key={k} className="rounded-lg p-2.5" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
                <p style={{ color: 'var(--text-muted)' }}>{k}</p>
                <p className="font-semibold" style={{ color: 'var(--text-primary)' }}>{v}</p>
              </div>
            ))}
          </div>

          <div className="flex justify-end">
            <button onClick={handleClose} className="btn-primary">Done</button>
          </div>
        </div>
      )}
    </Modal>
  )
}

/* ── Inline job detail panel ────────────────────────────────────────────── */
function JobDetailPanel({ job }) {
  if (!job) return null
  return (
    <div className="p-5" style={{ background: 'var(--glass-fill)' }}>
      <p className="text-xs font-bold uppercase tracking-wider mb-4" style={{ color: 'var(--text-muted)' }}>
        Job Details
      </p>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
        {[
          ['Validation Passed', fmtNumber(job.validationPassed)],
          ['Validation Failed', fmtNumber(job.validationFailed)],
          ['Records Skipped',   fmtNumber(job.recordsSkipped)],
          ['Completed At',      fmtDateTime(job.completedAt)],
        ].map(([k, v]) => (
          <div key={k} className="rounded-xl p-3" style={{ background: 'var(--glass-fill)', border: '1px solid var(--glass-border)' }}>
            <p className="mb-0.5" style={{ color: 'var(--text-muted)' }}>{k}</p>
            <p className="font-semibold" style={{ color: 'var(--text-primary)' }}>{v}</p>
          </div>
        ))}
      </div>
      {job.errorMessage && (
        <div className="mt-3 rounded-xl p-3" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)' }}>
          <p className="text-xs font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>Error</p>
          <p className="text-xs font-mono" style={{ color: 'var(--text-secondary)' }}>{job.errorMessage}</p>
        </div>
      )}
    </div>
  )
}

export default function SyncPage() {
  const { canWrite } = useAuth()
  const { page, size, setPage } = usePagination()
  const [runOpen, setRunOpen]   = useState(false)
  const [detailId, setDetailId] = useState(null)

  const { data, loading, refetch } = useFetch(
    () => syncApi.getAll({ page, size, direction: 'desc' }),
    [page, size]
  )
  const { data: stats, refetch: refetchStats } = useFetch(syncApi.getStats)

  const handleSuccess = () => { refetch(); refetchStats() }

  // Find the currently expanded job from the loaded page
  const detailJob = data?.content?.find(j => j.id === detailId) ?? null

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
      key: 'status', label: 'Status',
      render: v => <span className={statusColor(v)}>{v}</span>,
    },
    { key: 'totalRecords',     label: 'Total',     render: v => fmtNumber(v) },
    {
      key: 'recordsProcessed', label: 'Synced',
      render: v => <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>{fmtNumber(v)}</span>,
    },
    {
      key: 'recordsFailed', label: 'Failed',
      render: v => v > 0
        ? <span className="font-semibold" style={{ color: 'var(--text-secondary)' }}>{fmtNumber(v)}</span>
        : <span style={{ color: 'var(--text-muted)' }}>0</span>,
    },
    { key: 'executionTimeMs', label: 'Exec Time', render: v => fmtMs(v) },
    { key: 'triggeredBy',     label: 'Triggered By' },
    { key: 'startedAt',       label: 'Started',    render: v => fmtDateTime(v) },
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
          <h2 className="page-title">Synchronization</h2>
          <p className="page-subtitle">Move transformed records to target systems</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => { refetch(); refetchStats() }} className="btn-icon" title="Refresh">
            <HiRefresh className="w-4 h-4" />
          </button>
          {canWrite && (
            <button onClick={() => setRunOpen(true)} className="btn-primary">
              <HiPlay className="w-4 h-4" /> Run Sync
            </button>
          )}
        </div>
      </div>

      {/* Stats row */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          <StatCard label="Total Jobs"     value={fmtNumber(stats.totalJobs)}                 icon={HiChartBar}    color="brand" />
          <StatCard label="Completed"      value={fmtNumber(stats.completedJobs)}             icon={HiCheckCircle} color="green" />
          <StatCard label="Failed"         value={fmtNumber(stats.failedJobs)}                icon={HiXCircle}     color="red" />
          <StatCard label="Records Synced" value={fmtNumber(stats.totalRecordsSynchronized)}  icon={HiRefresh}     color="blue" />
          <StatCard label="Pending Sync"   value={fmtNumber(stats.recordsPendingSync)}        icon={HiClock}       color="amber" />
          <StatCard label="Success Rate"   value={fmtPercent(stats.successPercent)}           icon={HiCheckCircle} color="green" />
        </div>
      )}

      {/* Last sync info bar */}
      {stats?.lastSynchronizationTime && (
        <div className="glass-card p-4 flex items-center gap-3 flex-wrap">
          <HiClock className="w-4 h-4 flex-shrink-0" style={{ color: 'var(--text-muted)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            Last successful sync:{' '}
            <strong style={{ color: 'var(--text-primary)' }}>
              {fmtDateTime(stats.lastSynchronizationTime)}
            </strong>
            <span className="ml-2" style={{ color: 'var(--text-muted)' }}>
              ({fmtRelative(stats.lastSynchronizationTime)})
            </span>
          </p>
          <div className="ml-auto text-xs" style={{ color: 'var(--text-muted)' }}>
            Avg: {fmtMs(stats.avgExecutionTimeMs)} · Max: {fmtMs(stats.maxExecutionTimeMs)}
          </div>
        </div>
      )}

      {/* Table */}
      <div className="glass-card p-0 overflow-hidden">
        <DataTable
          columns={columns}
          data={data?.content}
          loading={loading}
          emptyMessage="No sync jobs found"
        />

        {/* Inline expandable detail — extracted to a proper component with stable key */}
        <AnimatePresence>
          {detailId && detailJob && (
            <motion.div
              key={detailId}
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t overflow-hidden" style={{ borderColor: 'var(--glass-border)' }}
            >
              <JobDetailPanel job={detailJob} />
            </motion.div>
          )}
        </AnimatePresence>

        <div className="px-4 py-3 border-t" style={{ borderColor: 'var(--glass-border)' }}>
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </div>
      </div>

      <RunSyncModal
        open={runOpen}
        onClose={() => setRunOpen(false)}
        onSuccess={handleSuccess}
      />
    </div>
  )
}
