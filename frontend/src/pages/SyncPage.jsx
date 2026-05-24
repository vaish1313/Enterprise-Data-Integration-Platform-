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
  const colors = {
    green: 'bg-emerald-500/10 text-emerald-400',
    red:   'bg-red-500/10 text-red-400',
    blue:  'bg-blue-500/10 text-blue-400',
    amber: 'bg-amber-500/10 text-amber-400',
    brand: 'bg-brand-500/10 text-brand-400',
  }
  return (
    <div className="card p-4 flex items-center gap-3">
      <div className={`p-2.5 rounded-xl ${colors[color]}`}>
        <Icon className="w-5 h-5" />
      </div>
      <div>
        <p className="text-xs text-slate-400">{label}</p>
        <p className="text-lg font-extrabold text-slate-900 dark:text-white">{value}</p>
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
          <div className={`rounded-xl p-4 border ${
            report.fullySuccessful
              ? 'bg-emerald-50 dark:bg-emerald-900/20 border-emerald-200 dark:border-emerald-800'
              : 'bg-amber-50 dark:bg-amber-900/20 border-amber-200 dark:border-amber-800'
          }`}>
            <div className="flex items-center gap-2 mb-2">
              {report.fullySuccessful
                ? <HiCheckCircle className="w-5 h-5 text-emerald-500" />
                : <HiXCircle className="w-5 h-5 text-amber-500" />
              }
              <p className="font-semibold text-sm text-slate-800 dark:text-white">
                {report.fullySuccessful ? 'Sync Successful' : 'Sync Completed with Issues'}
              </p>
            </div>
            <p className="text-xs text-slate-600 dark:text-slate-300">{report.summary}</p>
            {report.recommendation && (
              <p className="text-xs text-amber-600 dark:text-amber-400 mt-2">
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
              <div key={k} className="bg-slate-50 dark:bg-surface-900 rounded-lg p-2.5">
                <p className="text-slate-400">{k}</p>
                <p className="font-semibold text-slate-700 dark:text-slate-200">{v}</p>
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
    <div className="p-5 bg-slate-50 dark:bg-surface-900/50">
      <p className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4">
        Job Details
      </p>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
        {[
          ['Validation Passed', fmtNumber(job.validationPassed)],
          ['Validation Failed', fmtNumber(job.validationFailed)],
          ['Records Skipped',   fmtNumber(job.recordsSkipped)],
          ['Completed At',      fmtDateTime(job.completedAt)],
        ].map(([k, v]) => (
          <div key={k} className="bg-white dark:bg-surface-800 rounded-xl p-3">
            <p className="text-slate-400 mb-0.5">{k}</p>
            <p className="font-semibold text-slate-700 dark:text-slate-200">{v}</p>
          </div>
        ))}
      </div>
      {job.errorMessage && (
        <div className="mt-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-3">
          <p className="text-xs font-semibold text-red-600 dark:text-red-400 mb-1">Error</p>
          <p className="text-xs font-mono text-red-700 dark:text-red-300">{job.errorMessage}</p>
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
      render: v => <span className="text-emerald-500 font-semibold">{fmtNumber(v)}</span>,
    },
    {
      key: 'recordsFailed', label: 'Failed',
      render: v => v > 0
        ? <span className="text-red-400 font-semibold">{fmtNumber(v)}</span>
        : <span className="text-slate-400">0</span>,
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
        <div className="card p-4 flex items-center gap-3 flex-wrap">
          <HiClock className="w-4 h-4 text-slate-400 flex-shrink-0" />
          <p className="text-sm text-slate-600 dark:text-slate-300">
            Last successful sync:{' '}
            <strong className="text-slate-800 dark:text-white">
              {fmtDateTime(stats.lastSynchronizationTime)}
            </strong>
            <span className="text-slate-400 ml-2">
              ({fmtRelative(stats.lastSynchronizationTime)})
            </span>
          </p>
          <div className="ml-auto text-xs text-slate-400">
            Avg: {fmtMs(stats.avgExecutionTimeMs)} · Max: {fmtMs(stats.maxExecutionTimeMs)}
          </div>
        </div>
      )}

      {/* Table */}
      <div className="card p-0 overflow-hidden">
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
              className="border-t border-slate-200 dark:border-white/10 overflow-hidden"
            >
              <JobDetailPanel job={detailJob} />
            </motion.div>
          )}
        </AnimatePresence>

        <div className="px-4 py-3 border-t border-slate-200 dark:border-white/10">
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
