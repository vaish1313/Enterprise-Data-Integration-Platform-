/** Truncate a string to maxLen characters */
export const truncate = (str, maxLen = 40) =>
  str && str.length > maxLen ? str.slice(0, maxLen) + '…' : str

/** Download a Blob as a file */
export const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a   = document.createElement('a')
  a.href     = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

/** Build query string from object, skipping null/undefined/empty */
export const buildQuery = (params) =>
  Object.entries(params)
    .filter(([, v]) => v != null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&')
