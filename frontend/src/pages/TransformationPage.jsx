import { useState } from 'react'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import {
  HiPlus, HiPencil, HiTrash, HiRefresh, HiSearch,
  HiLightningBolt, HiCheckCircle, HiXCircle,
} from 'react-icons/hi'
import { transformationApi } from '../api/transformationApi'
import { dataSourceApi } from '../api/dataSourceApi'
import { useFetch } from '../hooks/useFetch'
import { usePagination } from '../hooks/usePagination'
import { useDebounce } from '../hooks/useDebounce'
import { useAuth } from '../context/AuthContext'
import { DataTable, Pagination } from '../components/Table'
import { Modal, ConfirmModal } from '../components/Modal'
import { fmtDateTime } from '../utils/formatters'

const TYPES = [
  'DIRECT_MAPPING','UPPERCASE','LOWERCASE','TRIM',
  'CONCAT','DEFAULT_VALUE','DATE_FORMAT',
]

function RuleForm({ onSubmit, defaultValues, loading, sources }) {
  const { register, handleSubmit, watch, formState: { errors } } = useForm({
    defaultValues: {
      active: true,          // ← correct RHF default, not native defaultChecked
      executionOrder: 0,
      ...defaultValues,
    },
  })
  const type = watch('transformationType')

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div className="col-span-2">
          <label className="label">Rule Name *</label>
          <input
            {...register('name', { required: 'Required' })}
            className={`input ${errors.name ? 'input-error' : ''}`}
            placeholder="e.g. Uppercase Email"
          />
          {errors.name && <p className="text-xs text-red-400 mt-1">{errors.name.message}</p>}
        </div>

        <div>
          <label className="label">Transformation Type *</label>
          <select
            {...register('transformationType', { required: 'Required' })}
            className={`input ${errors.transformationType ? 'input-error' : ''}`}
          >
            <option value="">Select type…</option>
            {TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
          {errors.transformationType && (
            <p className="text-xs text-red-400 mt-1">{errors.transformationType.message}</p>
          )}
        </div>

        <div>
          <label className="label">Data Source</label>
          <select {...register('dataSourceId')} className="input">
            <option value="">Global (all sources)</option>
            {sources?.content?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="label">Source Field</label>
          <input {...register('sourceField')} className="input" placeholder="e.g. email" />
        </div>

        <div>
          <label className="label">Target Field</label>
          <input {...register('targetField')} className="input" placeholder="e.g. email_upper" />
        </div>

        {(type === 'DEFAULT_VALUE' || type === 'DATE_FORMAT' || type === 'CONCAT') && (
          <div>
            <label className="label">
              {type === 'DATE_FORMAT' ? 'Output Pattern'
               : type === 'CONCAT'   ? 'Separator'
               : 'Default Value'}
            </label>
            <input
              {...register('defaultValue')}
              className="input"
              placeholder={
                type === 'DATE_FORMAT' ? 'yyyy-MM-dd'
                : type === 'CONCAT'   ? ' '
                : 'fallback value'
              }
            />
          </div>
        )}

        {(type === 'CONCAT' || type === 'DATE_FORMAT') && (
          <div>
            <label className="label">
              {type === 'DATE_FORMAT' ? 'Input Pattern' : 'Second Field'}
            </label>
            <input
              {...register('extraConfig')}
              className="input"
              placeholder={type === 'DATE_FORMAT' ? 'dd/MM/yyyy' : 'e.g. lastName'}
            />
          </div>
        )}

        <div>
          <label className="label">Execution Order</label>
          <input
            {...register('executionOrder', { valueAsNumber: true })}
            type="number"
            className="input"
          />
        </div>

        <div>
          <label className="label">Description</label>
          <input {...register('description')} className="input" placeholder="Optional…" />
        </div>
      </div>

      {/* Active toggle — controlled by RHF via defaultValues, not native defaultChecked */}
      <div className="flex items-center gap-2">
        <input
          {...register('active')}
          type="checkbox"
          id="active"
          className="w-4 h-4 accent-brand-600"
        />
        <label htmlFor="active" className="text-sm text-slate-600 dark:text-slate-300">
          Active
        </label>
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Saving…' : 'Save Rule'}
        </button>
      </div>
    </form>
  )
}

export default function TransformationPage() {
  const { canWrite } = useAuth()
  const { page, size, setPage } = usePagination()
  const [search, setSearch]       = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [saving, setSaving]   = useState(false)
  const [deleting, setDeleting] = useState(false)
  const debouncedSearch = useDebounce(search)

  const { data, loading, refetch } = useFetch(
    () => transformationApi.getAll({
      page,
      size,
      ...(debouncedSearch && { name: debouncedSearch }),
    }),
    [page, size, debouncedSearch]
  )

  // Fetch all sources for the dropdown (large page size to avoid truncation)
  const { data: sources } = useFetch(() => dataSourceApi.getAll({ page: 0, size: 200 }))

  const handleCreate = async (values) => {
    setSaving(true)
    try {
      await transformationApi.create(values)
      toast.success('Rule created')
      setCreateOpen(false)
      refetch()
    } catch {
      toast.error('Failed to create rule')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = async (values) => {
    setSaving(true)
    try {
      await transformationApi.update(editTarget.id, values)
      toast.success('Rule updated')
      setEditTarget(null)
      refetch()
    } catch {
      toast.error('Failed to update rule')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    setDeleting(true)
    try {
      await transformationApi.delete(deleteTarget.id)
      toast.success('Rule deleted')
      setDeleteTarget(null)
      refetch()
    } catch {
      toast.error('Failed to delete rule')
    } finally {
      setDeleting(false)
    }
  }

  const columns = [
    {
      key: 'name', label: 'Rule Name',
      render: (v) => (
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-lg bg-amber-500/10 flex items-center justify-center flex-shrink-0">
            <HiLightningBolt className="w-3.5 h-3.5 text-amber-400" />
          </div>
          <span className="font-semibold text-sm text-slate-800 dark:text-white">{v}</span>
        </div>
      ),
    },
    {
      key: 'transformationType', label: 'Type',
      render: v => <span className="badge-purple">{v}</span>,
    },
    {
      key: 'sourceField', label: 'Source Field',
      render: v => v
        ? <code className="text-xs bg-slate-100 dark:bg-surface-700 px-1.5 py-0.5 rounded">{v}</code>
        : '—',
    },
    {
      key: 'targetField', label: 'Target Field',
      render: v => v
        ? <code className="text-xs bg-slate-100 dark:bg-surface-700 px-1.5 py-0.5 rounded">{v}</code>
        : '—',
    },
    { key: 'executionOrder', label: 'Order' },
    {
      key: 'active', label: 'Active',
      render: v => v
        ? <HiCheckCircle className="w-4 h-4 text-emerald-400" />
        : <HiXCircle className="w-4 h-4 text-red-400" />,
    },
    { key: 'createdAt', label: 'Created', render: v => fmtDateTime(v) },
    {
      key: 'actions', label: '', width: '80px',
      render: (_, row) => canWrite ? (
        <div className="flex items-center gap-1">
          <button onClick={() => setEditTarget(row)} className="btn-icon" title="Edit">
            <HiPencil className="w-4 h-4" />
          </button>
          <button
            onClick={() => setDeleteTarget(row)}
            className="btn-icon text-red-400 hover:text-red-500"
            title="Delete"
          >
            <HiTrash className="w-4 h-4" />
          </button>
        </div>
      ) : null,
    },
  ]

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="page-header">
        <div>
          <h2 className="page-title">Transformation Rules</h2>
          <p className="page-subtitle">{data?.totalElements ?? 0} rules defined</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={refetch} className="btn-icon" title="Refresh">
            <HiRefresh className="w-4 h-4" />
          </button>
          {canWrite && (
            <button onClick={() => setCreateOpen(true)} className="btn-primary">
              <HiPlus className="w-4 h-4" /> New Rule
            </button>
          )}
        </div>
      </div>

      <div className="card p-4 flex gap-3">
        <div className="relative flex-1">
          <HiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search rules by name…"
            className="input pl-9"
          />
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <DataTable
          columns={columns}
          data={data?.content}
          loading={loading}
          emptyMessage="No transformation rules found"
        />
        <div className="px-4 py-3 border-t border-slate-200 dark:border-white/10">
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </div>
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="New Transformation Rule" size="lg">
        <RuleForm onSubmit={handleCreate} loading={saving} sources={sources} />
      </Modal>

      <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Edit Transformation Rule" size="lg">
        {editTarget && (
          <RuleForm
            onSubmit={handleEdit}
            loading={saving}
            defaultValues={editTarget}
            sources={sources}
          />
        )}
      </Modal>

      <ConfirmModal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        danger
        title="Delete Rule"
        message={`Delete rule "${deleteTarget?.name}"? This cannot be undone.`}
      />
    </div>
  )
}
