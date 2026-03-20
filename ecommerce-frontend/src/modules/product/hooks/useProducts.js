import { useMemo } from 'react'
import { MOCK_PRODUCTS } from '../data/mockProducts'

function normalise(str = '') {
  return str.toLowerCase().trim()
}

export function useProducts({ query = '', filters = {} } = {}) {
  const products = useMemo(() => {
    let result = [...MOCK_PRODUCTS]

    // Text search
    if (query) {
      const q = normalise(query)
      result = result.filter(
        (p) =>
          normalise(p.name).includes(q) ||
          normalise(p.category).includes(q) ||
          normalise(p.collection).includes(q) ||
          normalise(p.fabric).includes(q),
      )
    }

    // Size filter
    if (filters.sizes?.length) {
      result = result.filter((p) => p.sizes.some((s) => filters.sizes.includes(s)))
    }

    // Color filter
    if (filters.colors?.length) {
      result = result.filter((p) => p.colors.some((c) => filters.colors.includes(c)))
    }

    // Collection filter
    if (filters.collections?.length) {
      result = result.filter((p) => filters.collections.includes(p.collection))
    }

    // Fabric filter
    if (filters.fabrics?.length) {
      result = result.filter((p) => filters.fabrics.includes(p.fabric))
    }

    // Sort
    switch (filters.sortBy) {
      case 'price-asc':
        result.sort((a, b) => a.price - b.price)
        break
      case 'price-desc':
        result.sort((a, b) => b.price - a.price)
        break
      case 'newest':
        result = result.filter((p) => p.isNew).concat(result.filter((p) => !p.isNew))
        break
      default:
        break
    }

    return result
  }, [query, filters])

  return { products, loading: false, total: products.length }
}