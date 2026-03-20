import axios from 'axios'
import { API_CONFIG } from '../../../config/api.config'

const api = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout,
})

const COLOR_TO_HEX = {
  black: '#1C1C1C',
  white: '#FFFFFF',
  red: '#C0392B',
  blue: '#2E86DE',
  green: '#5A6D57',
  yellow: '#F1C40F',
  gray: '#C0C0C0',
  grey: '#C0C0C0',
  pink: '#F78FB3',
  purple: '#9B59B6',
  brown: '#8B7355',
  orange: '#E67E22',
  beige: '#D2B48C',
  navy: '#1F3A93',
}

function toColorHex(colorName = '') {
  const normalized = String(colorName).toLowerCase().trim()
  return COLOR_TO_HEX[normalized] || '#A8A8A8'
}

function toImageList(product) {
  const images = (product.variants || [])
    .flatMap((variant) => variant.images || [])
    .sort((a, b) => {
      if (a.primary && !b.primary) return -1
      if (!a.primary && b.primary) return 1
      return (a.sortOrder || 0) - (b.sortOrder || 0)
    })
    .map((img) => img.imageUrl)
    .filter(Boolean)

  return [...new Set(images)]
}

function normalizeProduct(product) {
  const variants = product.variants || []
  const firstVariant = variants[0]
  const prices = variants
    .map((v) => Number(v.price))
    .filter((value) => Number.isFinite(value))

  const colors = [...new Set(variants.map((v) => v.colorName).filter(Boolean))]
  const sizes = [...new Set(variants.flatMap((v) => (v.sizes || []).map((s) => s.sizeName).filter(Boolean)))]
  const images = toImageList(product)
  const createdAt = product.createdAt ? new Date(product.createdAt) : null
  const now = Date.now()
  const isNew = createdAt ? now - createdAt.getTime() <= 1000 * 60 * 60 * 24 * 30 : false

  return {
    id: product.id,
    name: product.name,
    description: product.description,
    categoryId: product.categoryId,
    category: product.categoryName || 'Collection',
    categoryName: product.categoryName,
    categoryGender: product.categoryGender,
    variants,
    price: prices.length ? Math.min(...prices) : 0,
    isNew,
    image: images[0] || '',
    images,
    colors: colors.map(toColorHex),
    colorLabels: colors,
    sizes,
    collection: product.categoryName || 'Collection',
    fabric: firstVariant?.compositionDetail || 'N/A',
    createdAt: product.createdAt,
    updatedAt: product.updatedAt,
  }
}

export const productService = {
  getAll: async (params = {}) => {
    const response = await api.get('/api/v1/products', {
      params: {
        page: 0,
        size: 100,
        sortBy: 'createdAt',
        sortDir: 'desc',
        ...params,
      },
    })

    const pageData = response.data || {}
    const items = Array.isArray(pageData.content) ? pageData.content.map(normalizeProduct) : []

    return {
      items,
      totalElements: pageData.totalElements ?? items.length,
      totalPages: pageData.totalPages ?? 1,
    }
  },

  getById: async (id) => {
    const response = await api.get(`/api/v1/products/${id}`)
    if (!response.data) return null
    return normalizeProduct(response.data)
  },
}