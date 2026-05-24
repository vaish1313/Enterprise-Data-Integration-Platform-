import { format, formatDistanceToNow, parseISO } from 'date-fns'

export const fmtDate = (d) => {
  if (!d) return '—'
  try { return format(typeof d === 'string' ? parseISO(d) : d, 'MMM d, yyyy') }
  catch { return d }
}

export const fmtDateTime = (d) => {
  if (!d) return '—'
  try { return format(typeof d === 'string' ? parseISO(d) : d, 'MMM d, yyyy HH:mm') }
  catch { return d }
}

export const fmtRelative = (d) => {
  if (!d) return '—'
  try { return formatDistanceToNow(typeof d === 'string' ? parseISO(d) : d, { addSuffix: true }) }
  catch { return d }
}

export const fmtMs = (ms) => {
  if (ms == null) return '—'
  if (ms < 1000)  return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}m`
}

export const fmtNumber = (n) => {
  if (n == null) return '—'
  return new Intl.NumberFormat().format(n)
}

export const fmtPercent = (n) => {
  if (n == null) return '—'
  return `${Number(n).toFixed(1)}%`
}

export const statusColor = (status) => {
  switch (status?.toUpperCase()) {
    case 'COMPLETED': case 'ACTIVE': case 'SUCCESS': return 'badge-green'
    case 'FAILED':    case 'ERROR':                  return 'badge-red'
    case 'RUNNING':                                  return 'badge-blue'
    case 'PENDING':                                  return 'badge-yellow'
    case 'PARTIAL':                                  return 'badge-yellow'
    case 'INACTIVE':                                 return 'badge-gray'
    default:                                         return 'badge-gray'
  }
}
