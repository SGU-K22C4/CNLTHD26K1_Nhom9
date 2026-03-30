import { useEffect, useMemo, useState } from 'react'
import { productService } from '../services/productService'

function normalise(str = '') {
  return str.toLowerCase().trim()
}

export function useProducts({ query = '', filters = {}, gender = '' } = {}) {
  const [allProducts, setAllProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    const loadProducts = async () => {
      setLoading(true)
      setError('')
      try {
        const pageSize = 100
        const firstPage = await productService.getAll({
          search: query || undefined,
          page: 0,
          size: pageSize,
          sortBy: 'createdAt',
          sortDir: 'desc',
        })

        let mergedItems = [...(firstPage.items || [])]
        const totalPages = Number(firstPage.totalPages) > 0 ? Number(firstPage.totalPages) : 1

        for (let page = 1; page < totalPages; page += 1) {
          const nextPage = await productService.getAll({
            search: query || undefined,
            page,
            size: pageSize,
            sortBy: 'createdAt',
            sortDir: 'desc',
          })
          mergedItems = mergedItems.concat(nextPage.items || [])
        }

        const uniqueById = Array.from(new Map(mergedItems.map((item) => [item.id, item])).values())
        if (!isMounted) return
        setAllProducts(uniqueById)
      } catch (err) {
        if (!isMounted) return
        setAllProducts([])
        setError(err?.response?.data?.message || err?.message || 'Failed to load products')
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadProducts()

    return () => {
      isMounted = false
    }
  }, [query])

  /* ── Tính toán available filters từ dữ liệu thực tế ──────── */
  const availableFilters = useMemo(() => {
    const sizesSet = new Set()
    const colorLabelsSet = new Set()
    const collectionsSet = new Set()
    const fabricsSet = new Set()

    allProducts.forEach((p) => {
      (p.sizes || []).forEach((s) => sizesSet.add(s))
      ;(p.colorLabels || []).forEach((c) => colorLabelsSet.add(c))
      if (p.collection) collectionsSet.add(p.collection)
      if (p.fabric && p.fabric !== 'N/A' && p.fabric !== 'Khác') fabricsSet.add(p.fabric)
    })

    const SIZE_ORDER = ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL']
    const sortedSizes = [...sizesSet].sort(
      (a, b) => (SIZE_ORDER.indexOf(a) === -1 ? 999 : SIZE_ORDER.indexOf(a))
              - (SIZE_ORDER.indexOf(b) === -1 ? 999 : SIZE_ORDER.indexOf(b))
    )

    return {
      sizes: sortedSizes,
      colorLabels: [...colorLabelsSet].sort(),
      collections: [...collectionsSet].sort(),
      fabrics: [...fabricsSet].sort(),
    }
  }, [allProducts])

  const filteredProducts = useMemo(() => {
    let result = [...allProducts]

    if (gender) {
      result = result.filter((p) => String(p.categoryGender || '').toUpperCase() === gender)
    }

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

    // Color filter — dùng colorLabels (tên tiếng Việt) thay vì hex
    if (filters.colors?.length) {
      result = result.filter((p) => (p.colorLabels || []).some((c) => filters.colors.includes(c)))
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
        result.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
        break
      default:
        break
    }

    return result
  }, [allProducts, query, filters, gender])

  const pageSize = Number(filters.pageSize) > 0 ? Number(filters.pageSize) : 12
  const currentPage = Number(filters.page) >= 0 ? Number(filters.page) : 0

  const total = filteredProducts.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const safePage = Math.min(currentPage, totalPages - 1)

  const products = useMemo(() => {
    const start = safePage * pageSize
    return filteredProducts.slice(start, start + pageSize)
  }, [filteredProducts, safePage, pageSize])

  return {
    products,
    loading,
    total,
    totalPages,
    currentPage: safePage,
    pageSize,
    error,
    availableFilters,
  }
}