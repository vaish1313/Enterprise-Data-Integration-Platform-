import { HiClipboardCopy } from 'react-icons/hi'
import toast from 'react-hot-toast'

export function TruncatedId({ value }) {
  if (!value) return <span style={{ color: 'var(--text-muted)' }}>—</span>

  const handleCopy = (e) => {
    e.stopPropagation() // Prevent clicking from triggering row actions (e.g. accordion)
    navigator.clipboard.writeText(value)
    toast.success('Copied!')
  }

  return (
    <div className="flex items-center gap-2 group">
      <span className="font-mono text-[11px] md:text-xs text-slate-400 cursor-help" title={value}>
        {value.slice(0, 8)}…
      </span>
      <button
        onClick={handleCopy}
        className="opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded hover:bg-[var(--glass-fill)]"
        title="Copy to clipboard"
      >
        <HiClipboardCopy className="w-3.5 h-3.5 text-slate-400 hover:text-[var(--text-primary)] transition-colors" />
      </button>
    </div>
  )
}
