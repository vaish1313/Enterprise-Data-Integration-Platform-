import { useState } from 'react'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import {
  HiPlus, HiPencil, HiTrash, HiRefresh, HiSearch,
  HiShieldCheck, HiEye, HiEyeOff,
} from 'react-icons/hi'
import { userApi } from '../api/userApi'
import { useFetch } from '../hooks/useFetch'
import { usePagination } from '../hooks/usePagination'
import { useDebounce } from '../hooks/useDebounce'
import { useAuth } from '../context/AuthContext'
import { DataTable, Pagination } from '../components/Table'
import { Modal, ConfirmModal } from '../components/Modal'
import { fmtDateTime } from '../utils/formatters'

const ROLES = ['ADMIN', 'ANALYST', 'OPERATOR']

function UserForm({ onSubmit, defaultValues, loading, isEdit }) {
  const [showPw, setShowPw] = useState(false)
  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: { enabled: true, ...defaultValues },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">First Name</label>
          <input {...register('firstName')} className="input" placeholder="John" />
        </div>
        <div>
          <label className="label">Last Name</label>
          <input {...register('lastName')} className="input" placeholder="Doe" />
        </div>
        <div>
          <label className="label">Username *</label>
          <input
            {...register('username', { required: 'Required' })}
            className={`input ${errors.username ? 'input-error' : ''}`}
            placeholder="john.doe"
          />
          {errors.username && <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{errors.username.message}</p>}
        </div>
        <div>
          <label className="label">Email *</label>
          <input
            {...register('email', {
              required: 'Required',
              pattern: { value: /\S+@\S+\.\S+/, message: 'Invalid email' },
            })}
            className={`input ${errors.email ? 'input-error' : ''}`}
            placeholder="john@company.com"
          />
          {errors.email && <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{errors.email.message}</p>}
        </div>
        <div>
          <label className="label">Role *</label>
          <select
            {...register('role', { required: 'Required' })}
            className={`input ${errors.role ? 'input-error' : ''}`}
          >
            <option value="">Select role…</option>
            {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
          </select>
          {errors.role && <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{errors.role.message}</p>}
        </div>

        {/* Password only shown on create */}
        {!isEdit && (
          <div>
            <label className="label">Password *</label>
            <div className="relative">
              <input
                {...register('password', {
                  required: 'Required',
                  minLength: { value: 8, message: 'Min 8 chars' },
                })}
                type={showPw ? 'text' : 'password'}
                className={`input pr-10 ${errors.password ? 'input-error' : ''}`}
                placeholder="••••••••"
              />
              <button
                type="button"
                onClick={() => setShowPw(p => !p)}
                className="absolute right-3 top-1/2 -translate-y-1/2 hover:opacity-80" style={{ color: 'var(--text-secondary)' }}
              >
                {showPw ? <HiEyeOff className="w-4 h-4" /> : <HiEye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{errors.password.message}</p>}
          </div>
        )}
      </div>

      {isEdit && (
        <div className="flex items-center gap-2">
          <input
            {...register('enabled')}
            type="checkbox"
            id="enabled"
            className="w-4 h-4" style={{ accentColor: 'var(--text-primary)' }}
          />
          <label htmlFor="enabled" className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            Account enabled
          </label>
        </div>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Saving…' : isEdit ? 'Update User' : 'Create User'}
        </button>
      </div>
    </form>
  )
}

export default function UserManagementPage() {
  const { isAdmin } = useAuth()
  const { page, size, setPage } = usePagination()
  const [search, setSearch]       = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [saving, setSaving]   = useState(false)
  const [deleting, setDeleting] = useState(false)
  const debouncedSearch = useDebounce(search)

  const { data, loading, refetch } = useFetch(
    () => userApi.getAll({
      page,
      size,
      ...(debouncedSearch && { username: debouncedSearch }),
    }),
    [page, size, debouncedSearch]
  )

  const handleCreate = async (values) => {
    setSaving(true)
    try {
      await userApi.create(values)
      toast.success('User created')
      setCreateOpen(false)
      refetch()
    } catch {
      toast.error('Failed to create user')
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = async (values) => {
    setSaving(true)
    try {
      await userApi.update(editTarget.id, values)
      toast.success('User updated')
      setEditTarget(null)
      refetch()
    } catch {
      toast.error('Failed to update user')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    setDeleting(true)
    try {
      await userApi.delete(deleteTarget.id)
      toast.success('User deleted')
      setDeleteTarget(null)
      refetch()
    } catch {
      toast.error('Failed to delete user')
    } finally {
      setDeleting(false)
    }
  }

  const roleBadge = (role) => ({
    ADMIN:    'badge-red',
    ANALYST:  'badge-blue',
    OPERATOR: 'badge-purple',
  }[role] || 'badge-gray')

  const columns = [
    {
      key: 'username', label: 'User',
      render: (v, row) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0" style={{ background: 'var(--text-primary)', color: 'var(--bg-base)' }}>
            {v?.[0]?.toUpperCase()}
          </div>
          <div>
            <p className="font-semibold text-sm" style={{ color: 'var(--text-primary)' }}>{v}</p>
            <p className="text-xs" style={{ color: 'var(--text-secondary)' }}>{row.email}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'firstName', label: 'Name',
      render: (v, row) => `${v || ''} ${row.lastName || ''}`.trim() || '—',
    },
    {
      key: 'role', label: 'Role',
      render: v => (
        <span className={roleBadge(v)}>
          <HiShieldCheck className="w-3 h-3" />{v}
        </span>
      ),
    },
    {
      key: 'enabled', label: 'Status',
      render: v => (
        <span className={v ? 'badge-green' : 'badge-gray'}>
          {v ? 'Active' : 'Disabled'}
        </span>
      ),
    },
    { key: 'createdAt', label: 'Joined', render: v => fmtDateTime(v) },
    {
      key: 'actions', label: '', width: '80px',
      render: (_, row) => isAdmin ? (
        <div className="flex items-center gap-1">
          <button onClick={() => setEditTarget(row)} className="btn-icon" title="Edit">
            <HiPencil className="w-4 h-4" />
          </button>
          <button
            onClick={() => setDeleteTarget(row)}
            className="btn-icon hover:opacity-80" style={{ color: 'var(--text-primary)' }}
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
          <h2 className="page-title">User Management</h2>
          <p className="page-subtitle">{data?.totalElements ?? 0} users registered</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={refetch} className="btn-icon" title="Refresh">
            <HiRefresh className="w-4 h-4" />
          </button>
          {isAdmin && (
            <button onClick={() => setCreateOpen(true)} className="btn-primary">
              <HiPlus className="w-4 h-4" /> New User
            </button>
          )}
        </div>
      </div>

      <div className="glass-card p-4 flex gap-3">
        <div className="relative flex-1">
          <HiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search by username…"
            className="input pl-9"
          />
        </div>
      </div>

      <div className="glass-card p-0 overflow-hidden">
        <DataTable
          columns={columns}
          data={data?.content}
          loading={loading}
          emptyMessage="No users found"
        />
        <div className="px-4 py-3 border-t" style={{ borderColor: 'var(--glass-border)' }}>
          <Pagination page={page} totalPages={data?.totalPages ?? 0} onPageChange={setPage} />
        </div>
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Create User" size="md">
        <UserForm onSubmit={handleCreate} loading={saving} isEdit={false} />
      </Modal>

      <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Edit User" size="md">
        {editTarget && (
          <UserForm
            onSubmit={handleEdit}
            loading={saving}
            defaultValues={editTarget}
            isEdit
          />
        )}
      </Modal>

      <ConfirmModal
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        loading={deleting}
        danger
        title="Delete User"
        message={`Delete user "${deleteTarget?.username}"? This cannot be undone.`}
      />
    </div>
  )
}
