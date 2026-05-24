import { useState } from 'react'

export function usePagination(initialSize = 20) {
  const [page, setPage]   = useState(0)
  const [size]            = useState(initialSize)

  const reset = () => setPage(0)

  return { page, size, setPage, reset }
}
