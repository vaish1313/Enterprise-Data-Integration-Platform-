import { useState } from 'react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import {
  HiPlus, HiSearch, HiPencil, HiTrash, HiDatabase,
  HiRefresh, HiCheckCircle, HiXCircle,
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

function SourceForm({ onSubmit, defaultValues, loading }) {
  const { register, handleSubmit, formState: { errors } } = useForm({ defaultValues })
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <label className="label">Name *</label>
        <input {...register('name', { required: 'Name is required' })}
          className={`input ${errors.name ? 'input-error' : ''}`} placeholder="My Data Source" />
        {errors.name && <p className="text-xs text-red-400 mt-1">{errors.name.message}</p>}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Source Type *</label>
          <select {...register('sourceType', { required: 'Required' })} className={`input ${errors.sourceType ? 'input-error' : ''}`}>
            <option value="">Select type…</option>
            {SOURCE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
          {errors.sourceType && <p className="text-xs text-red-400 mt-1">{errors.sourceType.message}</p>}
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

export default function DataSourcePage() {
  const { canWrite } = useAuth()
  const { page, size, setPage } = usePagination()
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const debouncedSearch = useDebounce(search)

  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

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

  const columns = [
    {
      key: 'name', label: 'Name',
      render: (v, row) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-brand-500/10 flex items-center justify-center flex-shrink-0">
            <HiDatabase className="w-4 h-4 text-brand-400" />
          </div>
          <div>
            <p className="font-semibold text-slate-800 dark:text-white text-sm">{v}</p>
            <p className="text-xs text-slate-400">{row.description || '—'}</p>
          </div>
        </div>
      ),
    },
    { key: 'sourceType', label: 'Type',
      render: v => <span className="badge-blue">{v}</span> },
    { key: 'status', label: 'Status',
      render: v => <span className={statusColor(v)}>{v}</span> },
    { key: 'createdBy', label: 'Created By' },
    { key: 'createdAt', label: 'Created',
      render: v => v ? new Date(v).toLocaleDateString() : '—' },
    {
      key: 'actions', label: '', width: '100px',
      render: (_, row) => canWrite ? (
        <div className="flex items-center gap-1">
          <button onClick={() => setEditTarget(row)} className="btn-icon" title="Edit">
            <HiPencil className="w-4 h-4" />
          </button>
          <button onClick={() => setDeleteTarget(row)} className="btn-icon text-red-400 hover:text-red-500" title="Delete">
            <HiTrash className="w-4 h-4" />
          </button>
        </div>
      ) : null,
    },
  ]

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

      {/* Filters */}
      <div className="card p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <HiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search by name…" className="input pl-9" />
        </div>
        <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value); setPage(0) }} className="input w-40">
          <option value="">All types</option>
          {SOURCE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0) }} className="input w-40">
          <option value="">All statuses</option>
          {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <DataTable columns={columns} data={data?.content} loading={loading} emptyMessage="No data sources found" />
        <div className="px-4 py-3 border-t border-slate-200 dark:border-white/10">
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
    </div>
  )
}
