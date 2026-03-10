import { useState, useCallback } from 'react'

export function useFilters(initialFilters = {}) {
  const [filters, setFiltersState] = useState({
    sortBy: 'featured',
    sizes: [],
    colors: [],
    collections: [],
    fabrics: [],
    ...initialFilters,
  })

  const setFilter = useCallback((key, value) => {
    setFiltersState((prev) => ({ ...prev, [key]: value }))
  }, [])

  const toggleArrayFilter = useCallback((key, value) => {
    setFiltersState((prev) => {
      const current = prev[key] || []
      const exists = current.includes(value)
      return {
        ...prev,
        [key]: exists ? current.filter((v) => v !== value) : [...current, value],
      }
    })
  }, [])

  const clearFilters = useCallback(() => {
    setFiltersState({ sortBy: 'featured', sizes: [], colors: [], collections: [], fabrics: [] })
  }, [])

  const hasActiveFilters =
    filters.sizes.length > 0 ||
    filters.colors.length > 0 ||
    filters.collections.length > 0 ||
    filters.fabrics.length > 0

  return { filters, setFilter, toggleArrayFilter, clearFilters, hasActiveFilters }
}