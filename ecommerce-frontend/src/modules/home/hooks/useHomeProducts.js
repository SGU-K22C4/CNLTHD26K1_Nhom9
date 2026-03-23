import { useEffect, useState } from 'react'
import { productService } from '../../product/services/productService'

const CACHE_TTL_MS = 2 * 60 * 1000

let cachedData = null
let cachedAt = 0
let inflightPromise = null

function isCacheValid() {
  return cachedData && Date.now() - cachedAt < CACHE_TTL_MS
}

async function fetchAllProducts() {
  const pageSize = 100
  const firstPage = await productService.getAll({
    page: 0,
    size: pageSize,
    sortBy: 'createdAt',
    sortDir: 'desc',
  })

  let merged = [...(firstPage.items || [])]
  const totalPages = Number(firstPage.totalPages) > 0 ? Number(firstPage.totalPages) : 1

  for (let page = 1; page < totalPages; page += 1) {
    const nextPage = await productService.getAll({
      page,
      size: pageSize,
      sortBy: 'createdAt',
      sortDir: 'desc',
    })
    merged = merged.concat(nextPage.items || [])
  }

  const uniqueById = Array.from(new Map(merged.map((item) => [item.id, item])).values())
  return uniqueById
}

async function getHomeData() {
  if (isCacheValid()) return cachedData

  if (!inflightPromise) {
    inflightPromise = fetchAllProducts()
      .then((products) => {
        const males = products.filter((p) => String(p.categoryGender || '').toUpperCase() === 'MALE')
        const females = products.filter((p) => String(p.categoryGender || '').toUpperCase() === 'FEMALE')

        const data = {
          products,
          bestSellers: products.slice(0, 8),
          featuredProducts: (products.filter((item) => item.isNew).slice(0, 4) || []).length
            ? products.filter((item) => item.isNew).slice(0, 4)
            : products.slice(0, 4),
          collections: [
            {
              id: 'female',
              name: 'Women',
              src: females[0]?.image || '',
              height: 420,
              count: females.length,
              to: '/collection/women',
            },
            {
              id: 'male',
              name: 'Men',
              src: males[0]?.image || '',
              height: 420,
              count: males.length,
              to: '/collection/men',
            },
          ],
        }

        cachedData = data
        cachedAt = Date.now()
        return data
      })
      .finally(() => {
        inflightPromise = null
      })
  }

  return inflightPromise
}

export function useHomeProducts() {
  const [data, setData] = useState(() => cachedData || {
    products: [],
    bestSellers: [],
    featuredProducts: [],
    collections: [],
  })
  const [loading, setLoading] = useState(!isCacheValid())
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    const load = async () => {
      if (isCacheValid()) {
        setData(cachedData)
        setLoading(false)
        return
      }

      setLoading(true)
      setError('')
      try {
        const result = await getHomeData()
        if (!isMounted) return
        setData(result)
      } catch (err) {
        if (!isMounted) return
        setError(err?.response?.data?.message || err?.message || 'Failed to load homepage products')
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    load()

    return () => {
      isMounted = false
    }
  }, [])

  return {
    ...data,
    loading,
    error,
  }
}
