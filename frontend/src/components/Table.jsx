import { SkeletonRow } from './Loader'

export function DataTable({ columns, data, loading, emptyMessage = 'No records found', skeletonRows = 5 }) {
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead>
          <tr>
            {columns.map(col => (
              <th key={col.key} style={col.width ? { width: col.width } : {}}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            Array.from({ length: skeletonRows }).map((_, i) => (
              <SkeletonRow key={i} cols={columns.length} />
            ))
          ) : !data || data.length === 0 ? (
            /* Handles both null/undefined data (error state) and empty array */
            <tr>
              <td colSpan={columns.length} className="px-4 py-12 text-center text-sm" style={{ color: 'var(--text-muted)' }}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row, i) => (
              <tr key={row.id || i}>
                {columns.map(col => (
                  <td key={col.key}>
                    {col.render ? col.render(row[col.key], row) : (row[col.key] ?? '—')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

/**
 * Sliding-window pagination — always shows up to 7 page buttons
 * centred around the current page.
 */
export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  // Build a sliding window of up to 7 page numbers
  const WINDOW = 7
  let start = Math.max(0, page - Math.floor(WINDOW / 2))
  let end   = start + WINDOW
  if (end > totalPages) {
    end   = totalPages
    start = Math.max(0, end - WINDOW)
  }
  const pages = Array.from({ length: end - start }, (_, i) => start + i)

  return (
    <div className="flex items-center justify-between mt-4 text-sm flex-wrap gap-2">
      <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>
        Page {page + 1} of {totalPages}
      </span>
      <div className="flex gap-1 flex-wrap">
        {/* First page shortcut */}
        {start > 0 && (
          <>
            <button onClick={() => onPageChange(0)}
              className="px-2.5 py-1.5 rounded-lg text-xs font-semibold btn-secondary">1</button>
            {start > 1 && <span className="px-1 py-1.5 text-xs text-slate-400">…</span>}
          </>
        )}

        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="btn-secondary px-3 py-1.5 text-xs disabled:opacity-40"
        >
          ← Prev
        </button>

        {pages.map(p => (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              p === page ? 'shadow-sm' : 'btn-secondary'
            }`}
            style={p === page ? { background: 'var(--text-primary)', color: 'var(--bg-base)' } : {}}
          >
            {p + 1}
          </button>
        ))}

        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="btn-secondary px-3 py-1.5 text-xs disabled:opacity-40"
        >
          Next →
        </button>

        {/* Last page shortcut */}
        {end < totalPages && (
          <>
            {end < totalPages - 1 && <span className="px-1 py-1.5 text-xs text-slate-400">…</span>}
            <button onClick={() => onPageChange(totalPages - 1)}
              className="px-2.5 py-1.5 rounded-lg text-xs font-semibold btn-secondary">{totalPages}</button>
          </>
        )}
      </div>
    </div>
  )
}
