import { useState, useEffect, useCallback } from 'react'

/**
 * Generic data-fetching hook.
 * @param {Function} fetchFn  — async function that returns data
 * @param {Array}    deps     — dependency array (re-fetches when changed)
 * @param {boolean}  skip     — skip the fetch entirely
 */
export function useFetch(fetchFn, deps = [], skip = false) {
  const [data,    setData]    = useState(null)
  const [loading, setLoading] = useState(!skip)
  const [error,   setError]   = useState(null)

  const execute = useCallback(async () => {
    if (skip) return
    setLoading(true)
    setError(null)
    try {
      const result = await fetchFn()
      setData(result)
    } catch (err) {
      setError(err?.response?.data?.error || err.message || 'Unknown error')
    } finally {
      setLoading(false)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, skip])

  useEffect(() => { execute() }, [execute])

  return { data, loading, error, refetch: execute }
}
